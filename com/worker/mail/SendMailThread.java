package com.worker.mail;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.Callable;

import org.apache.log4j.Logger;

import com.worker.bean.FileDocument;
import com.worker.bean.ResultDTO;
import com.worker.bean.TaskBean;
import com.worker.util.BaseWorker;
import com.worker.util.ProUtil;

/**
 * 发送邮件
 * 
 * @author JC
 * 
 */
public class SendMailThread extends BaseWorker implements Callable<TaskBean[]> {
	// 结果集
	public ResultDTO dto = null;
	// 邮件发送主类
	private SmtpMailSender mailSend = null;
	private File dir = new File("");
	// 文件扩展名称
	private String suffixName = "";
	// 文件名
	private String fileName = "";
	// 异步提交返回的结果集
	private TaskBean[] taskBeans;
	// 测试情况下发送指定人
	private String all_send_to = "";
	// 日志记录
	private static Logger log4j = Logger.getLogger(SendMailThread.class);

	/**
	 * @param dto
	 * @param uid
	 */
	public SendMailThread(ResultDTO dto, String uid) {
		this.dto = dto;
		this.uid = uid;
	}

	@Override
	public TaskBean[] call() throws Exception {
		File[] files = null;
		// 获取邮件中的附件
		FileDocument[] fds = dto.getFileDocuments();

		if ((fds != null) && (fds.length > 0)) {
			files = new File[fds.length];
			for (int i = 0; i < fds.length; i++) {
				byte[] bytes = fds[i].getBytes();
				if ((bytes != null) && (bytes.length > 0)) {
					int fileSize = bytes.length / 1048576;
					if (fileSize >= 14) {
						// 目前最大支持15M的附件,超過则返回错误
						log4j.info("Attachment is too large, cannot send");
						return new TaskBean[] { new TaskBean(false, "Attachment is too large, cannot send") };
					}
				}
				
				// 邮件附件存放的位置,为了后期查询
				File fileDir = new File(this.dir.getAbsolutePath() + "/workerAttachment/");
				if (!fileDir.exists()) {
					fileDir.mkdirs();
				}

				if ((fds[i].getFileName() == null) || ("".equals(fds[i].getFileName()))) {
					return new TaskBean[] { new TaskBean(false, "Accessories name is empty") };
				}

				this.suffixName = fds[i].getFileName().substring(fds[i].getFileName().lastIndexOf("."));
				this.fileName = fds[i].getFileName().substring(0, fds[i].getFileName().lastIndexOf("."));

				// 重新命名附件名称，精确到毫秒
				String newFileName = this.fileName
						+ new SimpleDateFormat("yyyyMMddHHmmssSSS", Locale.CHINA).format(new Date()) + this.suffixName;
				File file = new File(this.dir.getAbsolutePath() + "/workerAttachment/" + newFileName);

				FileOutputStream fos = new FileOutputStream(file);
				fos.write(fds[i].getBytes());
				fos.flush();
				fos.close();
				files[i] = file;
			}
		}

		if (dto != null) {
			boolean isEsmtp = dto.isEsmtp();
			String[] toStr = dto.getTo().split(",");
			// 多个收件人已逗号或分号隔开
			if (toStr == null || toStr.length <= 1) {
				toStr = dto.getTo().split(";");
			}

			if (toStr != null && toStr.length > 0) {
				if (isEsmtp) {
					mailSend = SmtpMailSender.createESmtpMailSender(dto.getSmtp(), dto.getFrom(), dto.getUser(),
							dto.getPassword());
				} else {
					this.mailSend = SmtpMailSender.createSmtpMailSender(dto.getFrom());
				}

				log4j.info("####开始发送邮件####主题：" + dto.getSubject() + "#####内容####" + dto.getContent() + "####接收人####"
						+ dto.getTo() + "###开始时间###"
						+ new SimpleDateFormat("yyyy年MM月dd日 HH时mm分ss秒 E", Locale.CHINA).format(new Date()));

				// 把邮件发送给指定一个人,一般是drsit测试的时候
				all_send_to = ProUtil.getStrValue("all_send_to");
				if (all_send_to.length() > 0 && !"".equals(all_send_to)) {
					toStr = all_send_to.split(",");
					dto.setCc("");
					dto.setBcc("");
				}
				
				//真正的开始发送邮件,最后一个参数是重发是使用，对应重发邮件的id
				taskBeans = mailSend.sendMail(toStr, dto.getCc(), dto.getBcc(), dto.getSubject(), dto.getContent(),
						files, dto.isHtml(), dto.isUrgent(), dto.getTo(), dto.getErrorMail(), 0);

				log4j.info("####邮件发送结束####主题：" + dto.getSubject() + "#####内容####" + dto.getContent() + "####接收人####"
						+ dto.getTo() + "###结束时间###"
						+ new SimpleDateFormat("yyyy年MM月dd日 HH时mm分ss秒 E", Locale.CHINA).format(new Date()));

			} else {
				this.resultMessage = getBackMessage("Error", "邮件发送失敗--收件人格式错误", this.uid, null);
			}
		} else {
			this.resultMessage = getBackMessage("Error", "邮件发送失敗--JSON转为对象时出错", this.uid, null);
		}

		return taskBeans;
	}
}

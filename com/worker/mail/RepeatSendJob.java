package com.worker.mail;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;

import com.worker.bean.TaskBean;
import com.worker.util.Configurate;
import com.worker.util.DBUtil;
import com.worker.util.StringUtil;

/**
 * 对于失败的邮件重新发送（正常重试三次）
 * 
 * @author JC
 * 
 */
@SuppressWarnings({ "unused" })
public class RepeatSendJob implements Runnable {

	private static Logger log4j = Logger.getLogger(SendMailThread.class);

	@Override
	public void run() {
		List<Map<String, String>> listTemp = new ArrayList<Map<String, String>>();

		// retry_times重试次数
		String querySql = "SELECT id,`from`,`to`, cc, bcc,title,content,attachment_names,sendtime,ip,message FROM mailworker_log WHERE flag=1 and retry_times<=3 ";
		String[] objs = { "id", "from", "to", "cc", "bcc", "title", "content", "sendtime", "ip", "message",
				"attachment_names" };
		List<Map<String, String>> list = DBUtil.queryParam(querySql, objs);

		log4j.info("--------开始进行失败邮件的重发-----");

		// 取出所有重试次数小于3的失败邮件，整理出来重新发送，如果再次发送失败则times+1，发送成功则改为0,当times>3时候则统计出失败邮件发送给对应处理人
		CompletionService<TaskBean[]> comservice = new ExecutorCompletionService<TaskBean[]>(
				Executors.newFixedThreadPool(10));

		TaskBean send = null;
		if (list != null && list.size() > 0) {
			for (Map<String, String> map : list) {
				// 把map转换为TaskBean对象
				send = new TaskBean();
				send.setKey(StringUtil.getDefaultInt(map.get("id")));
				send.setFrom(StringUtil.getObjetcToStr(map.get("from")));
				send.setTo(StringUtil.getObjetcToStr(map.get("to")));
				send.setCc(StringUtil.getObjetcToStr(map.get("cc")));
				send.setBcc(StringUtil.getObjetcToStr(map.get("bcc")));
				send.setSubject(StringUtil.getObjetcToStr(map.get("title")));
				send.setContent(StringUtil.getObjetcToStr(map.get("content")));
				send.setAttachmentNames(StringUtil.getObjetcToStr(map.get("attachment_names")));
				send.setMessage(StringUtil.getObjetcToStr(map.get("message")));
				comservice.submit(new RetrySend(send));
			}
		}

		// 取出数据处理
		TaskBean bean = null;
		for (int i = 0; i < list.size(); i++) {
			try {
				TaskBean[] beans = comservice.take().get();
				if (beans != null && beans.length > 0) {
					bean = beans[0];
					if (bean != null) {
						if (bean.isFlag()) {
							// 发送成功修改表字段retry_times为0,flag=0
							updateTimes(bean.isFlag(), 0, bean.getKey());
						} else {
							// 发送失败则修改表字段retry_times加1,等於3時候通知对应人员处理
							updateTimes(bean.isFlag(), 0, bean.getKey());
						}
					}
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (ExecutionException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * @param flag
	 * @param times
	 * @param id
	 * @return
	 */
	public int updateTimes(boolean flag, int times, int id) {
		String SQL = "";
		Object[] objs = null;
		if (flag) {
			SQL = "UPDATE mailworker_log set retry_times=?,flag=0,endtime=now() WHERE id=?";
			objs = new Object[] { times, id };
		} else {
			SQL = "UPDATE mailworker_log SET retry_times=retry_times+1,endtime=now() WHERE id=?";
			objs = new Object[] { id };
		}

		int count = DBUtil.updateByPre(SQL, objs);
		return count;
	}
}

/**
 * 失败邮件重新发送
 * 
 * @author JC
 * 
 */
class RetrySend implements Callable<TaskBean[]> {

	private static SmtpMailSender mailSend = null;

	private TaskBean bean = null;

	public RetrySend(TaskBean bean) {
		this.bean = bean;
	}

	@Override
	public TaskBean[] call() throws Exception {
		// 重发接收人
		List<String> tos = new ArrayList<String>();
		TaskBean[] taskBeans = null;
		mailSend = SmtpMailSender.createSmtpMailSender(bean.getFrom());
		String url = Configurate.getAttachmentUrl();
		// 附件可能有多个
		File file = null;
		File[] files = null;
		String attFileNames = bean.getAttachmentNames().trim();
		if (attFileNames != null && attFileNames.length() > 0) {
			String[] attactNames = attFileNames.split(",");
			if (attactNames != null && attactNames.length > 0) {
				files = new File[attactNames.length];
				for (int i = 0; i < attactNames.length; i++) {
					if (attactNames[i] != null && attactNames[i].length() > 0) {
						file = new File(url + attactNames[i]);
						files[i] = file;
					}
				}
			}
		}

		// 重发邮件一律分开发送，不抄送和秘送----邮件发送超时
		if (mailSend != null && bean != null) {
			if (bean.getMessage() != null && bean.getMessage().startsWith("邮件发送超时")) {
				if (!"".equals(bean.getTo())) {
					tos.add(bean.getTo());
				}
				if (!"".equals(bean.getCc())) {
					tos.add(bean.getCc());
				}
				if (!"".equals(bean.getBcc())) {
					tos.add(bean.getBcc());
				}
			} else {
				if (!"".equals(bean.getTo())) {
					tos.add(bean.getTo());
				} else if (!"".equals(bean.getCc())) {
					tos.add(bean.getCc());
				} else if (!"".equals(bean.getBcc())) {
					tos.add(bean.getBcc());
				}
			}

			taskBeans = mailSend.sendMail(tos.toArray(new String[] {}), bean.getCc(), bean.getBcc(), bean.getSubject(),
					bean.getContent(), files, true, false, bean.getTo(), "", bean.getKey());
		}
		return taskBeans;
	}

}

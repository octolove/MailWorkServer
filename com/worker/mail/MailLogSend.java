package com.worker.mail;

import com.worker.bean.TaskBean;

/**
 * 邮件日志定时任务
 * 
 * @author JC
 * 
 */
public class MailLogSend {
	private static SmtpMailSender mailSend = null;

	/**
	 * 统计出来发送失败的邮件发送给指定人员
	 */

	public static TaskBean[] sendFailmail(String from, String to, String cc, String bcc, String subject, String content) {
		TaskBean[] taskBeans = null;
		mailSend = SmtpMailSender.createSmtpMailSender(from);
		if (mailSend != null) {
			taskBeans = mailSend.sendMail(new String[] { to }, cc, bcc, subject, content, null, true, false, to, "",0);
		}
		return taskBeans;
	}
}
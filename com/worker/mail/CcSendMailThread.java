package com.worker.mail;

import java.io.File;
import java.util.concurrent.Callable;

import com.worker.bean.TaskBean;

/**
 * CC发送邮件
 * 
 * @author JC
 * 
 */
public class CcSendMailThread implements Callable<TaskBean[]> {

	// 异步提交返回的结果集
	private TaskBean taskBean;

	private String senderAddress = "";
	private String to = "";
	private String cc = "";
	private String subject = "";
	private String content = "";
	private File[] attachments = null;
	private boolean isHtml = false;
	private boolean isUrgent = false;
	private String ccstr = "";
	private int mailFlag = 0;
	private String allto = "";
	private int cckey = 0;

	public CcSendMailThread() {
		super();
	}

	public CcSendMailThread(String senderAddress, String to, String cc, String subject, String content,
			File[] attachments, boolean isHtml, boolean isUrgent, String ccstr, int mailFlag, String allto, int cckey) {
		super();
		this.senderAddress = senderAddress;
		this.to = to;
		this.cc = cc;
		this.subject = subject;
		this.content = content;
		this.attachments = attachments;
		this.isHtml = isHtml;
		this.isUrgent = isUrgent;
		this.ccstr = ccstr;
		this.mailFlag = mailFlag;
		this.allto = allto;
		this.cckey = cckey;
	}

	@Override
	public TaskBean[] call() throws Exception {
		CcBccMailSender mailSend = CcBccMailSender.createSmtpMailSender(senderAddress, to, ccstr, subject, content,
				attachments, isHtml, isUrgent, cc, 0);

		taskBean = mailSend.sendMail(to, ccstr, subject, content, attachments, isHtml, isUrgent, cc, mailFlag, allto,
				cckey);

		return new TaskBean[]{taskBean};
	}
}

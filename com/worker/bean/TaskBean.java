package com.worker.bean;

import java.io.Serializable;

public class TaskBean implements Serializable {

	private static final long serialVersionUID = 1L;

	//记录邮件的主键
	private Integer key = 0;
	// true表示发送成功
	private boolean flag = true;
	// 错误信息
	private String message = "";
	// 发件人
	private String from = "";
	// 收件人
	private String to = "";
	// 邮件主题
	private String subject = "";
	// 邮件内容
	private String content = "";
	// 抄送人
	private String cc = "";
	// 密送人
	private String bcc = "";
	// 附件名称集合
	private String attachmentNames = "";

	public TaskBean() {
		super();
	}
	
	/**
	 * 
	 * @param flag
	 * @param message
	 */
	public TaskBean(boolean flag, String message) {
		super();
		this.flag = flag;
		this.message = message;
	}
	
	/**
	 * 
	 * @param key
	 * @param flag
	 * @param message
	 * @param from
	 * @param to
	 * @param subject
	 * @param content
	 * @param cc
	 * @param bcc
	 * @param attachmentNames
	 */
	public TaskBean(Integer key, boolean flag, String message, String from, String to, String subject, String content,
			String cc, String bcc, String attachmentNames) {
		super();
		this.key = key;
		this.flag = flag;
		this.message = message;
		this.from = from;
		this.to = to;
		this.subject = subject;
		this.content = content;
		this.cc = cc;
		this.bcc = bcc;
		this.attachmentNames = attachmentNames;
	}

	public Integer getKey() {
		return key;
	}

	public void setKey(Integer key) {
		this.key = key;
	}

	public boolean isFlag() {
		return flag;
	}

	public void setFlag(boolean flag) {
		this.flag = flag;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getFrom() {
		return from;
	}

	public void setFrom(String from) {
		this.from = from;
	}

	public String getTo() {
		return to;
	}

	public void setTo(String to) {
		this.to = to;
	}

	public String getSubject() {
		return subject;
	}
	
	public void setSubject(String subject) {
		this.subject = subject;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public String getCc() {
		return cc;
	}

	public void setCc(String cc) {
		this.cc = cc;
	}

	public String getBcc() {
		return bcc;
	}

	public void setBcc(String bcc) {
		this.bcc = bcc;
	}

	public String getAttachmentNames() {
		return attachmentNames;
	}

	public void setAttachmentNames(String attachmentNames) {
		this.attachmentNames = attachmentNames;
	}
}

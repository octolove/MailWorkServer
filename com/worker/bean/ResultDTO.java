package com.worker.bean;

import java.io.Serializable;

public class ResultDTO implements Serializable, IUserBean {
	private static final long serialVersionUID = 1L;
	private String from = "";

	private String subject = "";

	private String to = "";

	private String cc = "";

	private String bcc = "";

	private String content = "";

	private boolean isHtml = false;

	private boolean isUrgent = false;

	private boolean isEsmtp = false;

	private String smtp = "";

	private String user = "";

	private String password = "";
	private FileDocument[] FileDocuments;
	private String errorMail = "";
	
	public ResultDTO() {
		super();
	}


	public ResultDTO(String from, String subject, String to, String cc, String bcc, String content,
			FileDocument[] fileDocuments, String errorMail) {
		super();
		this.from = from;
		this.subject = subject;
		this.to = to;
		this.cc = cc;
		this.bcc = bcc;
		this.content = content;
		FileDocuments = fileDocuments;
		this.errorMail = errorMail;
	}



	public FileDocument[] getFileDocuments() {
		return this.FileDocuments;
	}

	public void setFileDocuments(FileDocument[] fileDocuments) {
		this.FileDocuments = fileDocuments;
	}

	public String getSmtp() {
		return this.smtp;
	}

	public void setSmtp(String smtp) {
		this.smtp = smtp;
	}

	public String getUser() {
		return this.user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String getPassword() {
		return this.password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public boolean isEsmtp() {
		return this.isEsmtp;
	}

	public void setEsmtp(boolean isEsmtp) {
		this.isEsmtp = isEsmtp;
	}

	public String getFrom() {
		return this.from;
	}

	public void setFrom(String from) {
		this.from = from;
	}

	public String getSubject() {
		return this.subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	public String getTo() {
		return this.to;
	}

	public void setTo(String to) {
		this.to = to;
	}

	public String getContent() {
		return this.content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public boolean isHtml() {
		return this.isHtml;
	}

	public void setHtml(boolean isHtml) {
		this.isHtml = isHtml;
	}

	public boolean isUrgent() {
		return this.isUrgent;
	}

	public void setUrgent(boolean isUrgent) {
		this.isUrgent = isUrgent;
	}

	public static long getSerialversionuid() {
		return 1L;
	}

	public String getCc() {
		return this.cc;
	}

	public void setCc(String cc) {
		this.cc = cc;
	}

	public String getBcc() {
		return this.bcc;
	}

	public void setBcc(String bcc) {
		this.bcc = bcc;
	}

	public String getErrorMail() {
		return this.errorMail;
	}

	public void setErrorMail(String errorMail) {
		this.errorMail = errorMail;
	}
}
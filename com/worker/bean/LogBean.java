package com.worker.bean;

import java.io.Serializable;

public class LogBean implements Serializable {
	private static final long serialVersionUID = 3226527655801994232L;
	private String ident = "";
	private String level = "";
	private String message = "";

	public LogBean() {
	}

	public LogBean(String ident, String level, String message) {
		this.ident = ident;
		this.level = level;
		this.message = message;
	}

	public String getIdent() {
		return this.ident;
	}

	public void setIdent(String ident) {
		this.ident = ident;
	}

	public String getLevel() {
		return this.level;
	}

	public void setLevel(String level) {
		this.level = level;
	}

	public String getMessage() {
		return this.message;
	}

	public void setMessage(String message) {
		this.message = message;
	}
}
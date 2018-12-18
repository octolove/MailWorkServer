package com.worker.bean;

import java.io.Serializable;

public class ResultBean implements Serializable, IUserBean {
	private static final long serialVersionUID = 1L;
	private String status = "";
	private String message = "";
	private IUserBean result = null;
	private String uid = "";

	public String getUid() {
		return this.uid;
	}

	public void setUid(String uid) {
		this.uid = uid;
	}

	public String getStatus() {
		return this.status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getMessage() {
		return this.message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public IUserBean getResult() {
		return this.result;
	}

	public void setResult(IUserBean result) {
		this.result = result;
	}
}
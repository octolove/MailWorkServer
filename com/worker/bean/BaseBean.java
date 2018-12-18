package com.worker.bean;

import java.io.Serializable;

public class BaseBean
 implements Serializable
{
  private static final long serialVersionUID = 6305392538645166076L;
  private String cmd;
  private String uid;
  private ResultDTO data;

  public BaseBean()
  {
  }

  public BaseBean(String uid)
  {
    this.uid = uid;
  }

  public BaseBean(String cmd, ResultDTO data, String uid)
  {
    this.cmd = cmd;
    this.data = data;
    this.uid = uid;
  }

  public String getCmd() {
    return this.cmd;
  }

  public void setCmd(String cmd) {
    this.cmd = cmd;
  }

  public String getUid() {
    return this.uid;
  }

  public void setUid(String uid) {
    this.uid = uid;
  }

  public ResultDTO getData() {
    return this.data;
  }

  public void setData(ResultDTO data) {
    this.data = data;
  }
}
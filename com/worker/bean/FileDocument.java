package com.worker.bean;

import java.io.Serializable;

public class FileDocument
  implements Serializable
{
  private static final long serialVersionUID = 1L;
  private String fileName = "";
  private byte[] bytes;

  public FileDocument()
  {
  }

  public FileDocument(String fileName, byte[] bytes)
  {
    this.bytes = bytes;
    this.fileName = fileName;
  }

  public String getFileName() {
    return this.fileName;
  }

  public void setFileName(String fileName) {
    this.fileName = fileName;
  }

  public byte[] getBytes() {
    return this.bytes;
  }

  public void setBytes(byte[] bytes) {
    this.bytes = bytes;
  }
}
package com.worker.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

public class ProUtil {
	private static Properties pro = new Properties();

	private static File file = new File("");

	static {
		FileInputStream in = null;
		try {
			in = new FileInputStream(file.getAbsolutePath() + File.separator + "mailworker.properties");
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		}
		try {
			pro.load(in);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static String getStrValue(String key) {
		String keyq = "";
		try {
			keyq = StringUtil.getObjetcToStr(pro.get(key));
			keyq = new String(keyq.getBytes("ISO-8859-1"), "GBK");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return keyq;
	}

	public static int getIntValue(String key) {
		int keyInt = 0;
		try {
			String keyq = StringUtil.getObjetcToStr(pro.get(key));
			keyq = new String(keyq.getBytes("ISO-8859-1"), "GBK");
			keyInt = StringUtil.getDefaultInt(keyq);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return keyInt;
	}

	public static void main(String[] args) {
		System.out.println(getStrValue("mail_host"));
	}
}
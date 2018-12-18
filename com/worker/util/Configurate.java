package com.worker.util;

/**
 * 配置文件中心
 * 
 * @author JC
 * 
 */
public final class Configurate {

	public static String getAttachmentUrl() {
		return ProUtil.getStrValue("attachment_url");
	}

	public static int getRetryTimes() {
		int times = ProUtil.getIntValue("retry_times");
		return times < 0 ? 3 : times;
	}

	public static String getAllSendTo() {
		return ProUtil.getStrValue("all_send_to");
	}

	public static int getIntervaltime() {
		int ivs = ProUtil.getIntValue("intervaltime");
		return ivs < 0 ? 1800 : ivs;
	}
}

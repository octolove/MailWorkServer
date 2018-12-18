package com.worker.util;

import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

import org.codehaus.jackson.map.ObjectMapper;
import org.gearman.Gearman;
import org.gearman.GearmanClient;
import org.gearman.GearmanJobEvent;
import org.gearman.GearmanJobReturn;
import org.gearman.GearmanServer;

import com.worker.bean.LogBean;

public class LogClient {
	private static GearmanClient client;
	private static Gearman gearman;
	public static final String ECHO_FUNCTION_NAME = ProUtil.getStrValue("syslog_function_name");
	private static String host = ProUtil.getStrValue("syslog_host");
	private static String host2 = ProUtil.getStrValue("syslog_host2");
	private static int port = ProUtil.getIntValue("syslog_port");

	static {
		gearman = Gearman.createGearman();
		client = gearman.createGearmanClient();
		GearmanServer server = gearman.createGearmanServer(host, port);
		client.addServer(server);

		GearmanServer server2 = gearman.createGearmanServer(host2, port);
		client.addServer(server2);
	}

	public static String logInfo(String UID, String level, String message) {
		String renSTr = "";
		String json = "";

		String identStr = getLocalIP() + "/" + ProUtil.getStrValue("mail_function_name") + " [" + getPID() + "]";
		String messageStr = UID + ":" + message;

		LogBean log = new LogBean(identStr, level, messageStr);
		ObjectMapper mapper = new ObjectMapper();
		try {
			json = mapper.writeValueAsString(log);
		} catch (Exception e1) {
			e1.printStackTrace();
		}

		try {
			GearmanJobReturn jobReturn = client.submitBackgroundJob(ECHO_FUNCTION_NAME, json.getBytes());

			while (!jobReturn.isEOF()) {

				GearmanJobEvent event = jobReturn.poll();

				switch (event.getEventType()) {

				case GEARMAN_JOB_SUCCESS:
					renSTr = new String(event.getData());
					break;
				case GEARMAN_SUBMIT_SUCCESS:
					renSTr = new String(event.getData());
					break;
				case GEARMAN_SUBMIT_FAIL:
				case GEARMAN_JOB_FAIL:
					renSTr = new String(event.getData());
					break;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return renSTr;
	}

	@SuppressWarnings("rawtypes")
	public static String getLocalIP() {
		String localip = null;
		String netip = null;
		try {
			Enumeration netInterfaces = NetworkInterface.getNetworkInterfaces();
			InetAddress ip = null;
			boolean finded = false;
			do {
				NetworkInterface ni = (NetworkInterface) netInterfaces.nextElement();
				Enumeration address = ni.getInetAddresses();
				while (address.hasMoreElements()) {
					ip = (InetAddress) address.nextElement();
					if ((!ip.isSiteLocalAddress()) && (!ip.isLoopbackAddress())
							&& (ip.getHostAddress().indexOf(":") == -1)) {
						netip = ip.getHostAddress();
						finded = true;
						break;
					}
					if ((ip.isSiteLocalAddress()) && (!ip.isLoopbackAddress())
							&& (ip.getHostAddress().indexOf(":") == -1))
						localip = ip.getHostAddress();
				}
				if (!netInterfaces.hasMoreElements())
					break;
			} while (!finded);
		} catch (SocketException e) {
			e.printStackTrace();
		}
		if ((netip != null) && (!"".equals(netip))) {
			return netip;
		}
		return localip;
	}

	public static String getPID() {
		String processName = ManagementFactory.getRuntimeMXBean().getName();
		String processID = processName.substring(0, processName.indexOf('@'));
		return processID == null ? "" : processID;
	}
}
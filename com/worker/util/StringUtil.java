package com.worker.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.List;
import java.util.UUID;

public class StringUtil {
	public static String bytesToString(byte[] bytes) {
		if ((bytes == null) || (bytes.length <= 0)) {
			return "";
		}
		char[] chars = new char[bytes.length];
		for (int i = 0; i < bytes.length; i++) {
			chars[i] = ((char) bytes[i]);
		}

		return String.valueOf(chars);
	}

	public static String bytesToString(byte[] b, int start, int length) {
		String str = "";
		int i = 0;
		for (i = start; i < start + length; i++) {
			if (b[i] == 0)
				break;
		}
		str = new String(b, start, i - start);
		return str;
	}

	public static byte[] encode(Object obj) throws IOException {
		ByteArrayOutputStream bis = null;
		ObjectOutputStream os = null;
		try {
			bis = new ByteArrayOutputStream(1024);
			os = new ObjectOutputStream(bis);
			os.writeObject(obj);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (bis != null)
				bis.close();
			if (os != null)
				os.close();
		}
		return bis.toByteArray();
	}

	public static String escapeString(String sValue) {
		if ((sValue == null) || ("".equals(sValue))) {
			return sValue;
		}
		if ((sValue.contains("'")) || (sValue.contains("/")) || (sValue.contains("%")) || (sValue.contains("_"))) {
			StringBuffer str = new StringBuffer();
			for (int i = 0; i < sValue.length(); i++) {
				char ca = sValue.charAt(i);
				switch (ca) {
				case '\'':
					str.append("''");
					break;
				case '/':
					str.append("//");
					break;
				case '%':
					str.append("/%");
					break;
				case '_':
					str.append("/_");
					break;
				default:
					str.append(ca);
				}
			}

			return str.toString();
		}
		return sValue;
	}

	public static Object decode(byte[] src) throws IOException, ClassNotFoundException {
		ObjectInputStream ois = null;
		ByteArrayInputStream bos = null;
		try {
			bos = new ByteArrayInputStream(src);
			ois = new ObjectInputStream(bos);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (bos != null)
				bos.close();
			if (ois != null)
				ois.close();
		}
		return ois.readObject();
	}

	public static String listStringToString(List<String> list, String regex) {
		String _return = "";
		if ((list == null) || (list.isEmpty())) {
			return _return;
		}
		regex = regex == null ? "" : regex;
		StringBuffer sbf = new StringBuffer();
		for (int i = 0; i < list.size(); i++) {
			if (i == list.size() - 1)
				sbf.append("'" + ((String) list.get(i)).toString() + "'");
			else {
				sbf.append("'" + ((String) list.get(i)).toString() + "'" + regex);
			}
		}
		_return = sbf.toString();
		return _return;
	}

	public static int getDefaultInt(Object value) {
		int result = 0;
		if ((value != null) && (!"".equals(value.toString()))) {
			result = Integer.parseInt(value.toString());
		}
		return result;
	}

	public static double getDefaultDouble(Object value) {
		double result = 0.0D;
		if ((value != null) && (!"".equals(value.toString()))) {
			result = Double.parseDouble(value.toString());
		}
		return result;
	}

	public static String getDefaultStr(Object value, String defalut) {
		if (value == null) {
			return defalut;
		}
		return value.toString();
	}

	public static String getObjetcToStr(Object value) {
		return getDefaultStr(value, "");
	}

	/**
	 * 获取进程号码
	 * 
	 * @return
	 */
	public static String getPID() {
		String processName = ManagementFactory.getRuntimeMXBean().getName();
		String processID = processName.substring(0, processName.indexOf('@'));
		return processID == null ? "" : processID;
	}

	public static String uuid() {
		String uids = UUID.randomUUID().toString();
		uids = uids.replaceAll("\\-", "");
		return uids;
	}

	/**
	 * 获取本机的IP地址
	 * 
	 * @return
	 */
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

	/**
	 * 对应一些特殊的地址错误，代码处理下
	 * 
	 * @param mailAddress
	 * @return
	 */
	public static String filterEmail(String mailAddress) {
		if (mailAddress != null && mailAddress.length() > 0) {
			if (mailAddress.endsWith(",")) {
				mailAddress = mailAddress.substring(0, mailAddress.lastIndexOf(","));
			}
		}
		return mailAddress;
	}
}
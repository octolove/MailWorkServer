package com.worker.util;

import java.util.HashMap;
import java.util.Map;

import com.worker.util.DBUtil;

/**
 * 过滤sql特殊字符
 * @author JC
 *
 */
public class SqlEncode {
	private static Map<String, String> referencesMap = new HashMap<String, String>();

	static {
		referencesMap.put("'", "\\'");
		referencesMap.put("\"", "\\\"");
		referencesMap.put("\\", "\\\\");
		referencesMap.put("\n", "\\\n");
		referencesMap.put("\0", "\\\0");
		referencesMap.put("\b", "\\\b");
		referencesMap.put("\r", "\\\r");
		referencesMap.put("\t", "\\\t");
		referencesMap.put("\f", "\\\f");
	}

	public static String encode(String source) {
		if (source == null)
			return "";

		StringBuffer sbuffer = new StringBuffer(source.length());
		for (int i = 0; i < source.length(); i++) {
			String c = source.substring(i, i + 1);
			if (referencesMap.get(c) != null) {
				sbuffer.append(referencesMap.get(c));
			} else {
				sbuffer.append(c);
			}
		}
		return sbuffer.toString();
	}

	public static void main(String[] args) {
		String sss = "MD'Aten%_a@nybloodcenter.org";
		String sql = "select bus_email from customer.contact where bus_email='" + SqlEncode.encode(sss) + "' and status='ACTIVE'";
		System.out.println(sql);
		System.out.println(DBUtil.querySimple(sql));
	}
}

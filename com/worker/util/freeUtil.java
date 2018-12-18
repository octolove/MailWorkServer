package com.worker.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;

public class freeUtil {

	private static Configuration config = new Configuration();

	public static String getTem(List<Map<String, String>> list, String temName) {
		StringBuffer content = new StringBuffer();
		BufferedReader fr = null;
		try {

			Map<String, List<Map<String, String>>> root = new HashMap<String, List<Map<String, String>>>();
			root.put("tasks", list);

			File file = new File("");

			config.setDefaultEncoding("UTF-8");
			config.setDirectoryForTemplateLoading(new File(file.getAbsolutePath() + File.separator));
			config.setObjectWrapper(new DefaultObjectWrapper());

			Template tem = config.getTemplate(temName, "UTF-8");

			FileWriter fw = new FileWriter(new File("mail.html"));
			tem.process(root, fw);
			fw.flush();
			fw.close();

			String line = null;
			fr = new BufferedReader(new FileReader(new File("mail.html")));
			while ((line = fr.readLine()) != null) {
				content.append(line);
			}

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				fr.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return content.toString();
	}
	
	public static void main(String[] args) {
		List<Map<String, String>> listTemp = new ArrayList<Map<String, String>>();

		String querySql = "SELECT id,`from`,`to`, cc, bcc,title,content,sendtime,ip,message FROM mailworker_log";
		String[] objs = { "id", "from", "to", "cc", "bcc", "title", "content", "sendtime", "ip", "message" };
		List<Map<String, String>> list = DBUtil.queryParam(querySql, objs);
		if (list != null && list.size() > 0) {
			for (Map<String, String> m : list) {
				listTemp.add(m);
			}
		}

		// 获取邮件模板
		String str = freeUtil.getTem(listTemp, "mail.ftl");
		System.out.println(str);
	}
}

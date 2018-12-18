package com.worker.mail;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.worker.bean.TaskBean;
import com.worker.util.DBUtil;
import com.worker.util.StringUtil;
import com.worker.util.freeUtil;

/**
 * 统计失败的邮件，定期重新发送
 * 
 * @author JC
 * 
 */
public class MailAnalyseJob implements Runnable {
	@Override
	public void run() {
		List<Map<String, String>> listTemp = new ArrayList<Map<String, String>>();

		// 发送失败的flag=1 and retry_times>3
		String querySql = "SELECT id,`from`,`to`, cc, bcc,title,content,sendtime,ip,message FROM mailworker_log WHERE flag=1 and retry_times>3";
		String[] objs = { "id", "from", "to", "cc", "bcc", "title", "content", "sendtime", "ip", "message" };
		List<Map<String, String>> list = DBUtil.queryParam(querySql, objs);

		// 有异常邮件采取发送
		if (list != null && list.size() > 0) {
			for (Map<String, String> m : list) {
				listTemp.add(m);
			}

			if (listTemp != null && listTemp.size() > 0) {
				// 获取邮件模板
				String str = freeUtil.getTem(listTemp, "mail.ftl");
				// 发送指定邮件
				TaskBean[] taskBeans = MailLogSend.sendFailmail("warning@warning.com", "chenxiaodan@genscript.com", "",
						"", "警告：邮件发送出现异常[" + StringUtil.getLocalIP() + "]", str);

				// 发送完成后,对于失败邮件进行处理（更改flag为2或删除）
				if (taskBeans != null && taskBeans.length > 0 && taskBeans[0] != null) {
					TaskBean bean = taskBeans[0];
					// 确保邮件发送成功后在删除或更改状态
					if (bean.isFlag()) {
						// 更新表中日志信息,跟其他邮件保持一致
						Object[] objtem = { bean.isFlag() == true ? "0" : "1", bean.getMessage(), bean.getKey() };
						DBUtil.updateByPre(
								"UPDATE mailworker_log SET flag = ?,message=?,endtime = now() WHERE `id` = ?", objtem);

						// 更新状态为2表示失败的邮件已经统计过
						List<Object[]> objs_list = new ArrayList<Object[]>();
						for (Map<String, String> map : listTemp) {
							objs_list.add(new Object[] { "2", map.get("id") });
						}

						DBUtil.updateByPreBatch("UPDATE mailworker_log SET flag = ?,endtime = now() WHERE `id` =?",
								objs_list);
						// 清空数据
						listTemp.clear();
					}
				}
			}
		}
	}
}
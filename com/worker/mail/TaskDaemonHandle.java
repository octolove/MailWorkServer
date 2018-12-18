package com.worker.mail;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.worker.bean.FileDocument;
import com.worker.bean.ResultDTO;
import com.worker.bean.TaskBean;
import com.worker.bean.TaskFutureWrap;
import com.worker.util.DBUtil;

/**
 * 发送状态进行监控,发送失败需要更新数据库记录, 定时统计失败邮件,重新发送
 * 
 * @author JC
 * 
 */
public class TaskDaemonHandle implements Callable<String> {

	private TaskFutureWrap taskResult = null;

	public TaskDaemonHandle(TaskFutureWrap taskResult) {
		this.taskResult = taskResult;
	}

	@Override
	public String call() throws Exception {
		Future<TaskBean[]> future = null;

		if (taskResult != null) {
			try {
				future = taskResult.getFuture();
				if (future != null) {
					// 超过10分钟获取不到数据则断开
					TaskBean[] result = future.get(600, TimeUnit.SECONDS);
					for (TaskBean bean : result) {
						// flag是false则发送失败,更改发送状态
						if (bean != null) {
							updateMailLog(bean);
						}
					}
				}
			} catch (Exception e) {
				// 如果发送时间太长,则主动断掉连接
				if (future != null) {
					future.cancel(true);
					ResultDTO dto = taskResult.getDto();
					if (dto != null) {
						// 记录失败原因，特殊情况，重新插入一条失败的记录
						insertLog(dto, e.getMessage());
					}
				}
			}
		}
		return null;
	}

	/**
	 * 记录邮件发送日志(特殊处理) 有一种情况,如果to为空,发送的线程会一直不返回数据，其实已经结束，这边不知道,会当成超时来处理
	 * 会导致记录一下to为空的值,判断下to值
	 * 
	 * @return
	 */
	public int insertLog(ResultDTO dto, String msg) {
		int key = 0;
		String value = "";

		String insertSQL = "INSERT INTO mailworker_log (`from`,`to`,cc,bcc,title,content,attachment_names,"
				+ "ip,flag,message,sendtime,endtime) VALUES (?,?,?,?,?,?,?,?,?,?,now(),now())";
		StringBuffer sbStr = new StringBuffer();

		if (dto != null) {
			FileDocument[] filedocs = dto.getFileDocuments();
			if (filedocs != null && filedocs.length > 0) {
				for (FileDocument doc : filedocs) {
					sbStr.append(doc.getFileName()).append(",");
				}
				value = sbStr.delete(sbStr.lastIndexOf(","), sbStr.length()).toString();
			}

			// 目前会出现to为空的邮件，把它和真正的超时邮件区分开来
			if (dto.getTo() != null && !"".equals(dto.getTo())) {
				Object[] objs = { dto.getFrom(), dto.getTo(), dto.getCc(), dto.getBcc(), dto.getSubject(),
						dto.getContent(), value, dto.getErrorMail(), "1", "邮件发送超时" + msg };
				key = DBUtil.insertToKey(insertSQL, objs);
			} else {
				Object[] objs = { dto.getFrom(), dto.getTo(), dto.getCc(), dto.getBcc(), dto.getSubject(),
						dto.getContent(), value, dto.getErrorMail(), "2", "发件人为空" };
				key = DBUtil.insertToKey(insertSQL, objs);
			}
		}

		return key;
	}

	/**
	 * 更新邮件状态和时间
	 * 
	 * @param bean
	 */
	public int updateMailLog(TaskBean bean) {
		String updateSql = "UPDATE mailworker_log SET flag = ?,message=?,endtime = now() WHERE `id` = ?";
		Object[] objs = { bean.isFlag() == true ? "0" : "1", bean.getMessage(), bean.getKey() };
		return DBUtil.updateByPre(updateSql, objs);
	}

}

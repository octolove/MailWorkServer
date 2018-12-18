<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<html>
	<head>
		<meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
		<title>邮件列表</title>
		<style type="text/css">  
		</style>
	</head>
	<body>
	 <div style="text-align:center;font-size:18px;">异常邮件里列表</div>
	 <table border="1" cellpadding="0" cellspacing="0" align="center">
	 		<tr bgcolor="#DDDDDD">
	 			<td>编号</td>
	 			<td>发件人</td>
	 			<td>收件人</td>
	 			<td>主题</td>
	 			<td>发送时间</td>
				<td>失败原因</td>
				<td>来源</td>
				<td>主键</td>
	 		</tr>
	    <#list tasks as task>
	 		<tr <#if task_index%2==0>bgcolor="#FFFFE0"</#if>>
	 			<td>${task_index+1}</td>
	 			<td>${task.from}</td>
	 			<td>${task.to}</td>
	 			<td>${task.title}</td>
				<td>${task.sendtime}</td>
				<td>${(task.message)!'无'}</td>
				<td>${task.ip}</td>
	 			<td>${task.id}</td>
	 		</tr>
	 	</#list>
	 </table>
	</body>
</html>
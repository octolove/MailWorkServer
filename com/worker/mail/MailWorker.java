package com.worker.mail;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.codehaus.jackson.map.ObjectMapper;
import org.gearman.Gearman;
import org.gearman.GearmanFunction;
import org.gearman.GearmanFunctionCallback;

import com.worker.bean.BaseBean;
import com.worker.bean.ResultDTO;
import com.worker.bean.TaskBean;
import com.worker.bean.TaskFutureWrap;
import com.worker.util.BaseWorker;
import com.worker.util.DaemonThreadFactory;
import com.worker.util.ProUtil;
import com.worker.util.StringUtil;

public class MailWorker extends BaseWorker implements GearmanFunction {

	public static final String ECHO_FUNCTION_NAME = ProUtil.getStrValue("mail_function_name");
	public static final String ECHO_HOST = ProUtil.getStrValue("mail_host");
	public static final String ECHO_HOST2 = ProUtil.getStrValue("mail_host2");
	public static final int ECHO_PORT = ProUtil.getIntValue("mail_port");
	// 创建线程池
	private static ExecutorService execuorServer = Executors.newFixedThreadPool(50);
	// 创建线程池(守护线程)
	private static ExecutorService execuorDaemonServer = Executors.newFixedThreadPool(50, new DaemonThreadFactory());
	// 任务队列
	private static BlockingQueue<TaskFutureWrap> taskQueue = new ArrayBlockingQueue<TaskFutureWrap>(50);

	public static MailWorker mailworker = new MailWorker();

	public static MailWorker getInstace() {
		if (mailworker == null) {
			mailworker = new MailWorker();
		}
		return mailworker;
	}

	/**
	 * 可以采用考虑用CompletionService改写。 <br/>
	 * function 函数名称 data 传递的参数 callback 回调方法
	 */
	public byte[] work(String function, byte[] data, GearmanFunctionCallback callback) throws Exception {

		ObjectMapper mapper = new ObjectMapper();
		this.paramMessage = StringUtil.bytesToString(data, 0, data.length);
		BaseBean basebean = (BaseBean) mapper.readValue(this.paramMessage, BaseBean.class);
		this.uid = StringUtil.getObjetcToStr(basebean.getUid());
		this.cmd = StringUtil.getObjetcToStr(basebean.getCmd());
		ResultDTO dto = basebean.getData();

		// 提交发送邮件的任务
		Future<TaskBean[]> future = execuorServer.submit(new SendMailThread(dto, uid));
		if (future != null) {
			taskQueue.add(new TaskFutureWrap(dto, future));
		}

		// 处理返回的数据
		int size = taskQueue.size();
		TaskFutureWrap f = null;
		if (taskQueue != null && size > 0) {
			for (int i = 0; i < size; i++) {
				// 获取并移除此队列的头部
				f = taskQueue.take();
				execuorDaemonServer.submit(new TaskDaemonHandle(f));
			}
		}
		return data;
	}

	public static void main(String[] args) {
		// 连接服务器
		gearman = Gearman.createGearman();
		server = gearman.createGearmanServer(ECHO_HOST, ECHO_PORT);
		worker = gearman.createGearmanWorker();
		worker.addFunction(ECHO_FUNCTION_NAME, getInstace());
		worker.addServer(server);
		server2 = gearman.createGearmanServer(ECHO_HOST2, ECHO_PORT);
		worker.addServer(server2);

		// 初始化定时任务
		ScheduledExecutorService seService = Executors.newScheduledThreadPool(4);
		// 60分钟统计一次
		int inttime = ProUtil.getIntValue("intervaltime") == 0 ? 3600 : ProUtil.getIntValue("intervaltime");
		seService.scheduleAtFixedRate(new MailAnalyseJob(), 3600, inttime, TimeUnit.SECONDS);

		// 每20分钟重发一次
		int retrytime = ProUtil.getIntValue("retrytime") == 0 ? 1200 : ProUtil.getIntValue("retrytime");
		seService.scheduleAtFixedRate(new RepeatSendJob(), 1200, retrytime, TimeUnit.SECONDS);
	}
}
package com.worker.mail;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.codehaus.jackson.map.ObjectMapper;
import org.gearman.Gearman;
import org.gearman.GearmanClient;
import org.gearman.GearmanJobEvent;
import org.gearman.GearmanJobReturn;
import org.gearman.GearmanServer;

import com.worker.bean.BaseBean;
import com.worker.bean.ResultDTO;

/**
 * 测试使用的客户端
 */
public class MailClient {

	public static void main(String... args) throws InterruptedException {
		/**
		 * 连接Job服务器
		 */
		Gearman gearman = Gearman.createGearman();
		GearmanClient client = gearman.createGearmanClient();
		GearmanServer server = gearman.createGearmanServer(MailWorker.ECHO_HOST, MailWorker.ECHO_PORT);
		client.addServer(server);

		byte[] bytes = new byte[1024];
		ByteArrayOutputStream bao = new ByteArrayOutputStream();
		FileInputStream fin = null;
		try {
			fin = new FileInputStream(new File("D:/02_test.xls"));
			int k = 0;
			while ((k = fin.read(bytes)) != -1) {
				bao.write(bytes, 0, k);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (fin != null) {
				try {
					fin.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

		}

		ResultDTO res = new ResultDTO();
		res.setFrom("test@test.com");
		res.setTo("chenxiaodan@genscript.com");
		res.setSubject("xml数据");
		res.setContent("1111111111111111");
		res.setHtml(true);

		//FileDocument fdoc = new FileDocument();
		//fdoc.setFileName("02_test.xls");
		//fdoc.setBytes(bao.toByteArray());
		//res.setFileDocuments(new FileDocument[] { fdoc });

		try {
			String json = "";
			try {
				BaseBean bean = new BaseBean();
				bean.setCmd("");
				bean.setUid("1234566");
				bean.setData(res);
				ObjectMapper mapper = new ObjectMapper();
				json = mapper.writeValueAsString(bean);
			} catch (Exception e) {
				e.printStackTrace();
			}

			System.out.println(json);
			// 调用worker,json是参数
			GearmanJobReturn jobReturn = client.submitJob(MailWorker.ECHO_FUNCTION_NAME, json.getBytes());
			while (!jobReturn.isEOF()) {

				GearmanJobEvent event = jobReturn.poll();

				switch (event.getEventType()) {

				case GEARMAN_JOB_SUCCESS:
					// worker执行完成后返回的数据
					System.out.println(new String(event.getData()));
					break;
				case GEARMAN_SUBMIT_SUCCESS:
					// worker提交成功后返回的数据
					System.out.println(new String(event.getData()));
				case GEARMAN_SUBMIT_FAIL:
				case GEARMAN_JOB_FAIL:
					System.out.println(event.getEventType() + ": " + new String(event.getData()));
				}

			}
			gearman.shutdown();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}

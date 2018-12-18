package com.worker.mail;

import java.io.IOException;

import org.gearman.Gearman;
import org.gearman.GearmanServer;
import org.gearman.GearmanWorker;

/**
 * worker服务端类
 * @author chenxiaodan
 *
 */
public class MailWorkerServer {
	public static void main(String[] args) throws IOException {
		Gearman gearman = Gearman.createGearman();
		try {
			GearmanServer server = gearman.startGearmanServer(4730);

			GearmanWorker worker = gearman.createGearmanWorker();

			worker.addFunction("w0005_sys", new MailWorker());

			worker.addServer(server);
		} catch (IOException ioe) {
			gearman.shutdown();
			throw ioe;
		}
	}
}
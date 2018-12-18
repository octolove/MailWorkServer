package com.worker.mail;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Pattern;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.InitialDirContext;

import org.apache.log4j.Logger;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.MXRecord;
import org.xbill.DNS.Record;
import org.xbill.DNS.TextParseException;
import org.xbill.DNS.Type;

import com.worker.bean.ResultDTO;
import com.worker.bean.TaskBean;
import com.worker.bean.TaskFutureWrap;
import com.worker.util.Base64;
import com.worker.util.DBUtil;
import com.worker.util.MimeTypeFactory;
import com.worker.util.StringUtil;

/**
 * 发邮件工具类,异常信息记录到日志文件
 */
@SuppressWarnings("unused")
public final class SmtpMailSender {
	// 返回状态码
	private int smtpCode = 250;
	// 端口
	private static final int PORT = 25;
	// 连接次数
	private static final int RETRY = 3;
	// 间隔时间
	private static final int INTERVAL = 1000;
	// 超时时间
	private static final int TIMEOUT = 100000;
	private static final String BOUNDARY = "Boundary-=_hMbeqwnGNoWeLsRMeKTIPeofyStu";
	private static final String CHARSET = Charset.defaultCharset().displayName();
	private static final Pattern PATTERN = Pattern.compile(".+@[^.@]+(\\.[^.@]+)+$");
	private static InitialDirContext dirContext;
	private boolean isEsmtp;
	private String smtp;
	private String user;
	private String password;
	private String sender;
	private String senderAddress;
	private StringBuffer sbStr = null;
	private int retryCount = 0;
	private static Logger log4j = Logger.getLogger(SmtpMailSender.class);
	// cc和bcc的线程池
	private static ExecutorService ccexecuorServer = Executors.newFixedThreadPool(5);
	private static ExecutorService ccexecuorDaemonServer = Executors.newFixedThreadPool(5);

	static {
		Hashtable<String, String> hashtable = new Hashtable<String, String>();
		hashtable.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");
		try {
			dirContext = new InitialDirContext(hashtable);
		} catch (NamingException localNamingException) {
			localNamingException.printStackTrace();
		}
	}

	/**
	 * 创建SMTP邮件发送系统实例。
	 * 
	 * @param from
	 *            发件人
	 * @return SMTP邮件发送系统的实例
	 * @throws IllegalArgumentException
	 *             如果参数from为null或格式不正确
	 */
	private SmtpMailSender(String from) {
		if (from == null) {
			log4j.info("The sender can not be a null");
			throw new IllegalArgumentException("参数from不能为null。");
		}

		int leftSign = (from = from.trim()).charAt(from.length() - 1) == '>' ? from.lastIndexOf('<') : -1;

		this.senderAddress = (leftSign > -1 ? from.substring(leftSign + 1, from.length() - 1).trim() : from);

		if (!PATTERN.matcher(this.senderAddress).find()) {
			log4j.info("The sender parameter not correct");
			throw new IllegalArgumentException("参数from不正确。");
		}

		this.sender = (leftSign > -1 ? from.substring(0, leftSign).trim() : null);
		this.isEsmtp = false;

		if (this.sender != null)
			if (this.sender.length() == 0)
				this.sender = null;
			else if ((this.sender.charAt(0) == '"') && (this.sender.charAt(this.sender.length() - 1) == '"'))
				this.sender = this.sender.substring(1, this.sender.length() - 1).trim();
	}

	/**
	 * 创建ESMTP邮件发送系统实例。
	 * 
	 * @param smtp
	 *            SMTP服务器地址
	 * @param from
	 *            发件人
	 * @param user
	 *            用户名
	 * @param password
	 *            密码
	 * @return SMTP邮件发送系统的实例
	 * @throws IllegalArgumentException
	 *             如果参数from为null或格式不正确
	 */
	private SmtpMailSender(String address, String from, String user, String password) {
		this(from);

		this.isEsmtp = true;
		this.smtp = address;
		this.user = Base64.encode(user.getBytes());
		this.password = Base64.encode(password.getBytes());
	}

	/**
	 * 
	 * @param from
	 * @return
	 * @throws IllegalArgumentException
	 */
	public static SmtpMailSender createSmtpMailSender(String from) throws IllegalArgumentException {
		return new SmtpMailSender(from);
	}

	/**
	 * 
	 * @param smtp
	 * @param from
	 * @param user
	 * @param password
	 * @return
	 * @throws IllegalArgumentException
	 */
	public static SmtpMailSender createESmtpMailSender(String smtp, String from, String user, String password)
			throws IllegalArgumentException {
		return new SmtpMailSender(smtp, from, user, password);
	}

	/**
	 * 发送邮件的主方法
	 * 
	 * @param to
	 * @param cc
	 * @param bcc
	 * @param subject
	 * @param content
	 * @param attachments
	 * @param isHtml
	 * @param isUrgent
	 * @param count
	 * @param allto
	 * @param errorMail
	 * @param key
	 * @return
	 * @throws IllegalArgumentException
	 */
	public TaskBean sendMail(String to, String cc, String bcc, String subject, String content, File[] attachments,
			boolean isHtml, boolean isUrgent, int count, String allto, String errorMail, int key)
			throws IllegalArgumentException {
		// 接受服务器返回的数据
		String backResponse = "";

		// 將所以附件信息集合
		if (this.sbStr == null) {
			this.sbStr = new StringBuffer("");
		}

		if (attachments != null && attachments.length > 0) {
			for (File file : attachments) {
				this.sbStr.append(file.getName()).append(",");
			}
			this.sbStr.delete(this.sbStr.lastIndexOf(","), this.sbStr.length());
		}

		// 返回数据的封装
		TaskBean taskBean = new TaskBean(key, true, "", senderAddress, to, subject, content, cc, bcc, sbStr.toString());

		if ((to == null) || ("".equals(to))) {
			log4j.info("The recipient parameter cannot be null。");
			taskBean.setMessage("The recipient parameter cannot be null");
			taskBean.setFlag(false);
			return taskBean;
		}

		int leftSign = (to = to.trim()).charAt(to.length() - 1) == '>' ? to.lastIndexOf('<') : -1;

		String addresseeAddress = leftSign > -1 ? to.substring(leftSign + 1, to.length() - 1).trim() : to;

		if (!PATTERN.matcher(addresseeAddress).find()) {
			log4j.info("Recipient of the parameter is incorrect。");
			taskBean.setMessage("Recipient of the parameter is incorrectl");
			taskBean.setFlag(false);
			return taskBean;
		}

		String addressee = leftSign > -1 ? to.substring(0, leftSign).trim() : null;
		boolean needBoundary = (attachments != null) && (attachments.length > 0);

		Socket socket = null;
		InputStream in = null;
		OutputStream out = null;
		byte[] data;

		try {
			if (addressee != null) {
				if (addressee.length() == 0) {
					addressee = null;
				} else if ((addressee.charAt(0) == '"') && (addressee.charAt(addressee.length() - 1) == '"')) {
					addressee = addressee.substring(1, addressee.length() - 1).trim();
				}
			}

			if (this.isEsmtp) {
				for (int k = 1;; k++) {
					try {
						log4j.info("连接: 主机:\"" + smtp + "\" 端口:\"" + PORT + "\"");
						socket = new Socket(smtp, PORT);
						break;
					} catch (IOException e) {
						log4j.info("错误: 连接失败" + k + "次");

						if (k == RETRY) {
							taskBean.setMessage("连接尝试次数大于3次");
							taskBean.setFlag(false);
							return taskBean;
						}

						try {
							Thread.sleep(INTERVAL);
						} catch (InterruptedException ie) {
						}
					}
				}

				in = socket.getInputStream();
				out = socket.getOutputStream();

				// 失败后返回错误信息
				backResponse = response(in);
				if (backResponse != "" && Integer.parseInt(backResponse.substring(0, 3)) != 220) {
					taskBean.setMessage("获取对方服务器输入流失败,原因:" + backResponse.substring(4));
					taskBean.setFlag(false);
					return taskBean;
				}

			} else {
				log4j.info("状态: 创建邮件接收服务器列表");
				String[] address = parseDomain(parseUrl(addresseeAddress));

				if (address == null) {
					log4j.info("失败:解析MX记录为空,无法解析获取邮件服务器地址");
					taskBean.setMessage("解析MX记录为空,无法解析获取邮件服务器地址");
					taskBean.setFlag(false);
					return taskBean;
				}

				for (int k = 0; k < address.length; k++) {
					try {
						log4j.info("连接: 主机:\"" + address[k] + "\" 端口:\"" + 25 + "\"");

						socket = new Socket(address[k], 25);

						in = socket.getInputStream();
						out = socket.getOutputStream();

						// 连接成功后跳出返回
						backResponse = response(in);
						if (backResponse != "" && Integer.parseInt(backResponse.substring(0, 3)) == 220) {
							break;
						}

					} catch (IOException e) {
						log4j.info("连接错误: 连接失败,原因:" + e.getMessage());
						if (k >= (address.length - 1)) {
							taskBean.setMessage("连接对方邮件服务器[" + address[k] + "]失败,原因:" + e.getMessage());
							taskBean.setFlag(false);
							return taskBean;
						}
						// 重新连接--间隔1秒
						// Thread.sleep(1000L);
					}
				}
			}

			if (in == null || out == null) {
				log4j.info("{" + new SimpleDateFormat("yyyy年MM月dd日 HH时mm分ss秒 E", Locale.CHINA).format(new Date())
						+ " fail#" + this.senderAddress + "#" + to + "#" + subject + "#" + content + "#"
						+ this.sbStr.toString() + "}");

				if ((errorMail != null) && (!"".equals(errorMail)) && (this.retryCount <= 0)) {
					String errorMsg = "{"
							+ new SimpleDateFormat("yyyy年MM月dd日 HH时mm分ss秒 E", Locale.CHINA).format(new Date())
							+ " fail#" + this.senderAddress + "#" + to + "#" + subject + "#" + content + "#"
							+ this.sbStr.toString() + "}";

					this.retryCount += 1;
				}

				log4j.info("失败:连接邮件服务器获取输出输入流失败");
				taskBean.setMessage("连接邮件服务器获取输出输入流失败");
				taskBean.setFlag(false);
				return taskBean;
			}

			// Socket超时时间
			socket.setSoTimeout(180000);

			sendString("HELO " + parseUrl(this.senderAddress), out);
			sendNewline(out);

			// 失败后返回错误信息
			backResponse = response(in);
			if (backResponse != "" && Integer.parseInt(backResponse.substring(0, 3)) != 250) {
				log4j.info("失败:HELO命令無法完成,原因:" + backResponse.substring(4));
				taskBean.setMessage("HELO命令無法完成,原因:" + backResponse.substring(4));
				taskBean.setFlag(false);
				return taskBean;
			}

			if (this.isEsmtp) {
				sendString("AUTH LOGIN", out);
				sendNewline(out);

				// 失败后返回错误信息
				backResponse = response(in);
				if (backResponse != "" && Integer.parseInt(backResponse.substring(0, 3)) != 334) {
					log4j.info("失败:AUTH LOGIN认证失败");
					taskBean.setMessage("AUTH LOGIN认证失败");
					taskBean.setFlag(false);
					return taskBean;
				}

				sendString(this.user, out);
				sendNewline(out);

				// 失败后返回错误信息
				backResponse = response(in);
				if (backResponse != "" && Integer.parseInt(backResponse.substring(0, 3)) != 334) {
					log4j.info("失败:AUTH LOGIN认证用户名错误");
					taskBean.setMessage("AUTH LOGIN认证用户名错误");
					taskBean.setFlag(false);
					return taskBean;
				}

				sendString(this.password, out);
				sendNewline(out);

				// 失败后返回错误信息
				backResponse = response(in);
				if (backResponse != "" && Integer.parseInt(backResponse.substring(0, 3)) != 235) {
					log4j.info("失败:AUTH LOGIN认证密码错误");
					taskBean.setMessage("AUTH LOGIN认证密码错误");
					taskBean.setFlag(false);
					return taskBean;
				}
			}

			// 校验发件人
			sendString("MAIL FROM: <" + this.senderAddress + ">", out);
			sendNewline(out);
			backResponse = response(in);
			if (backResponse != "" && Integer.parseInt(backResponse.substring(0, 3)) != 250) {
				log4j.info("失败:MAIL FROM 命令无法完成,原因:" + backResponse.substring(4));
				taskBean.setMessage("MAIL FROM 命令无法完成,原因:" + backResponse.substring(4));
				taskBean.setFlag(false);
				return taskBean;
			}

			// 校验收件人
			sendString("RCPT TO: <" + addresseeAddress + ">", out);
			sendNewline(out);
			backResponse = response(in);
			if (backResponse != "" && Integer.parseInt(backResponse.substring(0, 3)) != 250) {
				log4j.info("失败:RCPT TO 命令無法完成,原因:" + backResponse.substring(4));
				taskBean.setMessage("RCPT TO 命令無法完成,原因:" + backResponse.substring(4));
				taskBean.setFlag(false);
				return taskBean;
			}

			// 发送抄送cc邮件,重发邮件没有cc和bcc
			if (count == 0) {
				if ((cc != null) && (cc.length() > 0)) {
					String[] ccs = cc.split(",");
					if ((ccs == null) || (ccs.length <= 1)) {
						ccs = cc.split(";");
					}
					for (String ccstr : ccs) {
						if ((ccstr != null) && (!"".equals(ccstr))) {
							// 多传递一个字段数据库主键
							int cckey = insertLog(senderAddress, "", StringUtil.filterEmail(ccstr), "", subject,
									content, attachments, errorMail);
							ResultDTO dto = new ResultDTO(senderAddress, to, cc, bcc, subject, content, null, errorMail);
							CcSendMailThread ccsend = new CcSendMailThread(senderAddress, to, cc, subject, content,
									attachments, isHtml, isUrgent, StringUtil.filterEmail(ccstr), 0, allto, cckey);

							Future<TaskBean[]> f = ccexecuorServer.submit(ccsend);
							ccexecuorDaemonServer.submit(new TaskDaemonHandle(new TaskFutureWrap(dto, f)));
						}
					}
				}

				// 发送密送bcc邮件
				if ((bcc != null) && (bcc.length() > 0)) {
					String[] bccs = bcc.split(",");
					if ((bccs == null) || (bccs.length <= 1)) {
						bccs = bcc.split(";");
					}
					for (String bccstr : bccs) {
						if ((bccstr != null) && (!"".equals(bccstr))) {
							// 多传递一个字段数据库主键
							int cckey = insertLog(senderAddress, "", "", StringUtil.filterEmail(bccstr), subject,
									content, attachments, errorMail);
							ResultDTO dto = new ResultDTO(senderAddress, to, cc, bcc, subject, content, null, errorMail);
							CcSendMailThread ccsend = new CcSendMailThread(senderAddress, to, cc, subject, content,
									attachments, isHtml, isUrgent, StringUtil.filterEmail(bccstr), 1, allto, cckey);

							Future<TaskBean[]> f = ccexecuorServer.submit(ccsend);
							ccexecuorDaemonServer.submit(new TaskDaemonHandle(new TaskFutureWrap(dto, f)));
						}
					}
				}
			}

			sendString("DATA", out);
			sendNewline(out);
			backResponse = response(in);
			if (backResponse != "" && Integer.parseInt(backResponse.substring(0, 3)) != 354) {
				log4j.info("失败:DATA 命令无法完成,原因:" + backResponse.substring(4));
				taskBean.setMessage("DATA 命令无法完成,原因:" + backResponse.substring(4));
				taskBean.setFlag(false);
				return taskBean;
			}

			sendString(
					"From: "
							+ (this.sender == null ? this.senderAddress : new StringBuilder(
									String.valueOf(getBase64String(this.sender))).append(" <")
									.append(this.senderAddress).append(">").toString()), out);
			sendNewline(out);

			sendString(
					"To: "
							+ (addressee == null ? allto : new StringBuilder(String.valueOf(getBase64String(allto)))
									.append(" <").append(allto).append(">").toString()), out);
			sendNewline(out);

			if ((cc != null) && (cc.length() > 0)) {
				String[] ccs = cc.split(",");
				if ((ccs == null) || (ccs.length <= 1)) {
					ccs = cc.split(";");
				}

				for (String ccstr : ccs) {
					int leftCC = (ccstr = ccstr.trim()).charAt(ccstr.length() - 1) == '>' ? ccstr.lastIndexOf('<') : -1;

					String ccAddresseeAddress = leftCC > -1 ? ccstr.substring(leftCC + 1, ccstr.length() - 1).trim()
							: ccstr;

					String ccAddressee = leftCC > -1 ? ccstr.substring(0, leftCC).trim() : null;

					sendString(
							"Cc: "
									+ (ccAddressee == null ? ccAddresseeAddress : new StringBuilder(
											String.valueOf(getBase64String(ccAddressee))).append(" <")
											.append(ccAddresseeAddress).append(">").toString()), out);
					sendNewline(out);
				}
			}

			sendString("Subject: " + getBase64String(subject), out);
			sendNewline(out);
			sendString(
					"Date: "
							+ new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z", Locale.getDefault())
									.format(new Date()),
					out);
			sendNewline(out);
			sendString("MIME-Version: 1.0", out);
			sendNewline(out);

			if (needBoundary) {
				sendString("Content-Type: multipart/mixed; BOUNDARY=\"" + BOUNDARY + "\"", out);
				sendNewline(out);
			} else if (isHtml) {
				sendString("Content-Type: text/html; charset=\"" + CHARSET + "\"", out);
				sendNewline(out);
			} else {
				sendString("Content-Type: text/plain; charset=\"" + CHARSET + "\"", out);
				sendNewline(out);
			}

			sendString("Content-Transfer-Encoding: base64", out);
			sendNewline(out);

			if (isUrgent) {
				sendString("X-Priority: 1", out);
				sendNewline(out);
			} else {
				sendString("X-Priority: 3", out);
				sendNewline(out);
			}

			sendString("X-Mailer: BlackFox Mail[Copyright(C) 2013 Sol]", out);
			sendNewline(out);

			log4j.info("发送: ");
			sendNewline(out);

			if (needBoundary) {
				sendString("--" + BOUNDARY, out);
				sendNewline(out);

				if (isHtml) {
					sendString("Content-Type: text/html; charset=\"" + CHARSET + "\"", out);
					sendNewline(out);
				} else {
					sendString("Content-Type: text/plain; charset=\"" + CHARSET + "\"", out);
					sendNewline(out);
				}

				sendString("Content-Transfer-Encoding: base64", out);
				sendNewline(out);

				log4j.info("发送: ");
				sendNewline(out);
			}

			data = (content != null ? content : "").getBytes();

			for (int k = 0; k < data.length; k += 54) {
				sendString(Base64.encode(data, k, Math.min(data.length - k, 54)), out);
				sendNewline(out);
			}

			if (needBoundary) {
				RandomAccessFile attachment = null;
				int fileIndex = 0;

				data = new byte[453600];
				try {
					for (; fileIndex < attachments.length; fileIndex++) {
						String fileName = attachments[fileIndex].getName();

						if ((fileName != null) && (!"".equals(fileName))) {
							String suffixName = fileName.substring(fileName.lastIndexOf("."));
							String oldFileName = fileName.substring(0, fileName.lastIndexOf("."));
							fileName = oldFileName.substring(0, oldFileName.length() - 17) + suffixName;
						}

						attachment = new RandomAccessFile(attachments[fileIndex], "r");

						sendString("--" + BOUNDARY, out);
						sendNewline(out);
						sendString(
								"Content-Type: "
										+ MimeTypeFactory.getMimeType(fileName.indexOf(".") == -1 ? "*" : fileName
												.substring(fileName.lastIndexOf(".") + 1)) + "; name=\""
										+ (fileName = getBase64String(fileName)) + "\"", out);

						sendNewline(out);
						sendString("Content-Transfer-Encoding: base64", out);
						sendNewline(out);
						sendString("Content-Disposition: attachment; filename=\"" + fileName + "\"", out);
						sendNewline(out);

						log4j.info("发送: ");
						sendNewline(out);
						int k;
						while ((k = attachment.read(data)) != -1) {
							sendString(Base64.encode(data, 0, k), out);
							sendNewline(out);
						}
					}
				} catch (FileNotFoundException e) {
					log4j.info("错误: 附件\"" + attachments[fileIndex].getAbsolutePath() + "\"不存在");

					log4j.info("{" + new SimpleDateFormat("yyyy年MM月dd日 HH时mm分ss秒 E", Locale.CHINA).format(new Date())
							+ " fail#" + this.senderAddress + "#" + to + "#" + subject + "#" + content + "#"
							+ this.sbStr.toString() + "}");

					if ((errorMail != null) && (!"".equals(errorMail)) && (this.retryCount <= 0)) {
						String errorMsg = "{"
								+ new SimpleDateFormat("yyyy年MM月dd日 HH时mm分ss秒 E", Locale.CHINA).format(new Date())
								+ " fail#" + this.senderAddress + "#" + to + "#" + subject + "#" + content + "#"
								+ this.sbStr.toString() + "}";

						this.retryCount += 1;
					}

					taskBean.setMessage("附件\"" + attachments[fileIndex].getAbsolutePath() + "\"不存在,原因:"
							+ e.getMessage());
					taskBean.setFlag(false);
					return taskBean;
				} catch (IOException e) {
					log4j.info("错误: 无法读取附件\"" + attachments[fileIndex].getAbsolutePath() + "\"");

					log4j.info("{" + new SimpleDateFormat("yyyy年MM月dd日 HH时mm分ss秒 E", Locale.CHINA).format(new Date())
							+ " fail#" + this.senderAddress + "#" + to + "#" + subject + "#" + content + "#"
							+ this.sbStr.toString() + "}");

					if ((errorMail != null) && (!"".equals(errorMail)) && (this.retryCount <= 0)) {
						String errorMsg = "{"
								+ new SimpleDateFormat("yyyy年MM月dd日 HH时mm分ss秒 E", Locale.CHINA).format(new Date())
								+ " fail#" + this.senderAddress + "#" + to + "#" + subject + "#" + content + "#"
								+ this.sbStr.toString() + "}";

						this.retryCount += 1;
					}

					taskBean.setMessage("无法读取附件\"" + attachments[fileIndex].getAbsolutePath() + "\",原因:"
							+ e.getMessage());
					taskBean.setFlag(false);
					return taskBean;
				} finally {
					if (attachment != null)
						try {
							attachment.close();
						} catch (IOException localIOException45) {
						}
				}
				if (attachment != null) {
					try {
						attachment.close();
					} catch (IOException localIOException46) {
					}
				}
				sendString("--" + BOUNDARY + "--", out);
				sendNewline(out);
			}

			sendString(".", out);
			sendNewline(out);
			backResponse = response(in);
			if (backResponse != "" && Integer.parseInt(backResponse.substring(0, 3)) != 250) {
				log4j.info("失败:.結束命令無法完成,原因:" + backResponse.substring(4));
				taskBean.setMessage(".結束命令無法完成,原因:" + backResponse.substring(4));
				taskBean.setFlag(false);
				return taskBean;
			}

			sendString("QUIT", out);
			sendNewline(out);
			backResponse = response(in);
			if (backResponse != "" && Integer.parseInt(backResponse.substring(0, 3)) != 221) {
				log4j.info("失败:QUIT結束命令無法完成,原因:" + backResponse.substring(4));
				taskBean.setMessage("QUIT結束命令無法完成,原因:" + backResponse.substring(4));
				taskBean.setFlag(false);
				return taskBean;
			}

			// 最终发送成功
			taskBean.setMessage("send to [" + to + "] is success");
			taskBean.setFlag(true);
			return taskBean;
		} catch (SocketTimeoutException e) {
			log4j.info("错误: 连接超时");

			log4j.info("{" + new SimpleDateFormat("yyyy年MM月dd日 HH时mm分ss秒 E", Locale.CHINA).format(new Date())
					+ " fail#" + this.senderAddress + "#" + to + "#" + subject + "#" + content + "#"
					+ this.sbStr.toString() + "}");

			if ((errorMail != null) && (!"".equals(errorMail)) && (this.retryCount <= 0)) {
				String errorMsg = "{"
						+ new SimpleDateFormat("yyyy年MM月dd日 HH时mm分ss秒 E", Locale.CHINA).format(new Date()) + " fail#"
						+ this.senderAddress + "#" + to + "#" + subject + "#" + content + "#" + this.sbStr.toString()
						+ "}";

				this.retryCount += 1;
			}

			taskBean.setMessage("连接超时,原因:" + e.getMessage());
			taskBean.setFlag(false);
			return taskBean;
		} catch (IOException e) {
			log4j.info("错误: 连接出错");

			log4j.info("{" + new SimpleDateFormat("yyyy年MM月dd日 HH时mm分ss秒 E", Locale.CHINA).format(new Date())
					+ " fail#" + this.senderAddress + "#" + to + "#" + subject + "#" + content + "#"
					+ this.sbStr.toString() + "}");

			if ((errorMail != null) && (!"".equals(errorMail)) && (this.retryCount <= 0)) {
				String errorMsg = "{"
						+ new SimpleDateFormat("yyyy年MM月dd日 HH时mm分ss秒 E", Locale.CHINA).format(new Date()) + " fail#"
						+ this.senderAddress + "#" + to + "#" + subject + "#" + content + "#" + this.sbStr.toString()
						+ "}";

				this.retryCount += 1;
			}

			taskBean.setMessage("连接出错,原因:" + e.getMessage());
			taskBean.setFlag(false);
			return taskBean;
		} catch (Exception e) {
			log4j.info("{" + new SimpleDateFormat("yyyy年MM月dd日 HH时mm分ss秒 E", Locale.CHINA).format(new Date())
					+ " fail#" + this.senderAddress + "#" + to + "#" + subject + "#" + content + "#"
					+ this.sbStr.toString() + "}");

			if ((errorMail != null) && (!"".equals(errorMail)) && (this.retryCount <= 0)) {
				String errorMsg = "{"
						+ new SimpleDateFormat("yyyy年MM月dd日 HH时mm分ss秒 E", Locale.CHINA).format(new Date()) + " fail#"
						+ this.senderAddress + "#" + to + "#" + subject + "#" + content + "#" + this.sbStr.toString()
						+ "}";

				this.retryCount += 1;
			}

			log4j.info("错误: " + e.toString());
			taskBean.setMessage("发送出错,原因:" + e.getMessage());
			taskBean.setFlag(false);
			return taskBean;
		} finally {
			if (in != null)
				try {
					in.close();
				} catch (IOException localIOException65) {
				}
			if (out != null)
				try {
					out.close();
				} catch (IOException localIOException66) {
				}
			if (socket != null)
				try {
					socket.close();
				} catch (IOException localIOException67) {
				}
		}
	}

	/**
	 * 给多个发件人发送带附件的HTML邮件。
	 * 
	 * @param to
	 *            收件人
	 * @param subject
	 *            主题
	 * @param content
	 *            正文
	 * @param attachments
	 *            附件
	 * @sendKey 已发送邮件的key
	 * @return 任务状况
	 * @throws IllegalArgumentException
	 *             如果参数to为null或格式不正确
	 */
	public TaskBean[] sendMail(String[] to, String cc, String bcc, String subject, String content, File[] attachments,
			boolean isHtml, boolean isUrgent, String allto, String errorMail, Integer sendKey)
			throws IllegalArgumentException {
		TaskBean[] task = new TaskBean[to.length];
		int key = 0;
		for (int k = 0; k < task.length; k++) {
			if (to[k] != null && !"".equals(to[k])) {
				// 如果是重发邮件，则不重复记录数据
				if (sendKey == null || sendKey <= 0) {
					// 邮件信息存放到数据库
					key = insertLog(senderAddress, StringUtil.filterEmail(to[k]), cc, bcc, subject, content,
							attachments, errorMail);
				} else {
					key = sendKey;
				}

				// 发送具体邮件
				task[k] = sendMail(StringUtil.filterEmail(to[k]), cc, bcc, subject, content, attachments, isHtml,
						isUrgent, k, allto, errorMail, key);
				this.retryCount = 0;
			}
		}

		return task;
	}

	/**
	 * 录邮件发送日志
	 * 
	 * @param from
	 * @param to
	 * @param cc
	 * @param bcc
	 * @param subject
	 * @param content
	 * @param attachments
	 * @param errorMail
	 * @return
	 */
	public int insertLog(String from, String to, String cc, String bcc, String subject, String content,
			File[] attachments, String errorMail) {
		int key = 0;
		String insertSQL = "INSERT INTO mailworker_log (`from`,`to`,cc,bcc,title,content,attachment_names,"
				+ "ip,flag,sendtime,endtime) VALUES (?,?,?,?,?,?,?,?,?,now(),now())";
		Object[] objs = { from, to, cc, bcc, subject, content, getFileNames(attachments), errorMail, "0" };
		key = DBUtil.insertToKey(insertSQL, objs);
		return key;
	}

	/**
	 * 附件名称集合
	 * 
	 * @param docs
	 * @return
	 */
	public String getFileNames(File[] docs) {
		String value = "";
		StringBuffer sbStr = new StringBuffer("");
		if ((docs != null) && (docs.length > 0)) {
			for (File doc : docs) {
				sbStr.append(doc.getName()).append(",");
			}
			value = sbStr.delete(sbStr.lastIndexOf(","), sbStr.length()).toString();
		}
		return value;
	}

	/**
	 * 发送失败邮件到指定人
	 * 
	 * @param errorMail
	 * @param content
	 */
	public void sendFailMail(String errorMail, String content) {
		sendMail(errorMail, "", "", "mailworker failed", content, null, false, false, 0, errorMail, "", 0);
	}

	/**
	 * 通过分析收件人邮箱域名的DNS记录获取邮件接收服务器地址。
	 * 
	 * @param url
	 *            收件人邮箱域名
	 * @return 主机地址列表
	 */
	private String[] parseDomain(String url) {
		try {
			NamingEnumeration records = dirContext.getAttributes(url, new String[] { "mx" }).getAll();

			if (records.hasMore()) {
				url = records.next().toString();
				url = url.substring(url.indexOf(": ") + 2);
				String[] address = url.split(",");
				MX[] tmpMxArray = new MX[address.length];

				for (int k = 0; k < address.length; k++) {
					String[] tmpMx = address[k].trim().split(" ");
					tmpMxArray[k] = new MX(Integer.parseInt(tmpMx[0]), tmpMx[1]);
				}

				for (int n = 1; n < tmpMxArray.length; n++) {
					for (int m = n; m > 0; m--) {
						if (tmpMxArray[(m - 1)].pri > tmpMxArray[m].pri) {
							MX tmp = tmpMxArray[(m - 1)];
							tmpMxArray[(m - 1)] = tmpMxArray[m];
							tmpMxArray[m] = tmp;
						}
					}
				}

				for (int k = 0; k < tmpMxArray.length; k++) {
					address[k] = tmpMxArray[k].address;
				}

				return address;
			}

			records = dirContext.getAttributes(url, new String[] { "a" }).getAll();

			if (records.hasMore()) {
				url = records.next().toString();
				url = url.substring(url.indexOf(": ") + 2).replace(" ", "");
				return url.split(",");
			}

			return new String[] { url };
		} catch (NamingException e) {
			log4j.info("错误: 域名\"" + url + "\"无法解析");
		}
		return null;
	}

	/**
	 * 输出字符串(需要返回具体信息,不仅仅是状态码)
	 * 
	 * @param str
	 *            字符串
	 * @param out
	 *            输出流
	 * @throws IOException
	 *             如果发生 I/O 错误。
	 */
	private String response(InputStream in) throws IOException {
		byte[] buffer = new byte[1024];
		int k = in.read(buffer);

		if (k == -1) {
			return "";
		}

		String response = new String(buffer, 0, k).trim();
		log4j.info("响应: " + response);
		return response;
	}

	/**
	 * 写日志。
	 * 
	 * @param info
	 *            信息
	 */
	private void sendString(String str, OutputStream out) throws IOException {
		if (str.length() <= 200) {
			log4j.info("发送: " + str);
		}
		if (str == null) {
			str = "";
		}

		out.write(str.getBytes());
		out.flush();
	}

	/**
	 * 输出一个换行符。
	 * 
	 * @param out
	 *            输出流
	 * @throws IOException
	 *             如果发生 I/O 错误。
	 */
	private static void sendNewline(OutputStream out) throws IOException {
		out.write('\r');
		out.write('\n');
		out.flush();
	}

	/**
	 * 获得字符串的Base64加密形式。
	 * 
	 * @param str
	 *            字符串
	 * @return 加密后的字符串
	 */
	private static String getBase64String(String str) {
		if ((str == null) || (str.length() == 0)) {
			return "";
		}

		StringBuffer tmpStr = new StringBuffer();
		byte[] bytes = str.getBytes();

		for (int k = 0; k < bytes.length;) {
			if (k != 0) {
				tmpStr.append(' ');
			}

			tmpStr.append("=?");
			tmpStr.append(CHARSET);
			tmpStr.append("?B?");
			tmpStr.append(Base64.encode(bytes, k, Math.min(bytes.length - k, 30)));
			tmpStr.append("?=");

			k += 30;

			if (k < bytes.length) {
				tmpStr.append('\r');
				tmpStr.append('\n');
			}
		}

		return tmpStr.toString();
	}

	private static String parseUrl(String address) {
		return address.substring(address.lastIndexOf('@') + 1);
	}

	/**
	 * 根据SMTP服务器返回码确定出错详细信息 常见错误信息
	 * 
	 * @param code
	 * @return
	 */
	public static String fromCODEMSG(int code) {
		String message = "";
		switch (code) {
		case 221:
			message = "Service closing transmission channel";
			break;
		case 251:
			message = "User not local";
			break;
		case 421:
			message = "Unable to provide normal services, closing transmission pipeline. Retaining messages in the local, may try to delivery";
			break;
		case 450:
			message = "Requested mail action cannot be executed: mailbox unavailable";
			break;
		case 451:
			message = "Send action aborted: local error";
			break;
		case 452:
			message = "Send to perform: system space";
			break;
		case 500:
			message = "Parameter format error, can not be identified";
			break;
		case 502:
			message = "Command not implemented";
			break;
		case 550:
			message = "Mailbox does not exist, don't try to delivery";
			break;
		case 554:
			message = "transfer has failed";
			break;
		default:
			message = "Unknown exception";
		}
		return message;
	}

	/**
	 * MX记录
	 * 
	 */
	private class MX {
		final int pri;
		final String address;

		MX(int pri, String host) {
			this.pri = pri;
			this.address = host;
		}
	}
}
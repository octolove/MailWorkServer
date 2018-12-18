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
import java.util.Date;
import java.util.Hashtable;
import java.util.Locale;
import java.util.regex.Pattern;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.InitialDirContext;

import org.apache.log4j.Logger;

import com.worker.bean.TaskBean;
import com.worker.util.Base64;
import com.worker.util.MimeTypeFactory;

/**
 * 抄送代码类
 */
@SuppressWarnings("unused")
public class CcBccMailSender {
	private int smtpCode = 250;
	private static final int PORT = 25;
	private static final int TIMEOUT = 100000;
	private static final String BOUNDARY;
	private static final String CHARSET;
	private static final Pattern PATTERN;
	private static InitialDirContext dirContext;
	private String sender;
	private String senderAddress;
	private StringBuffer sbStr = null;

	private static String to = "";
	private static String cc = "";
	private static String subject = "";
	private static String content = "";
	private static File[] attachments = null;
	private static boolean isHtml = false;
	private static boolean isUrgent = false;
	private static String ccstr = "";
	private static int mailFlag = 0;
	private static Logger log4j = Logger.getLogger(SmtpMailSender.class);

	static {
		BOUNDARY = "Boundary-=_hMbeqwnGNoWeLsRMeKTIPeofyStu";
		CHARSET = Charset.defaultCharset().displayName();
		PATTERN = Pattern.compile(".+@[^.@]+(\\.[^.@]+)+$");

		Hashtable<String, String> hashtable = new Hashtable<String, String>();
		hashtable.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");
		try {
			dirContext = new InitialDirContext(hashtable);
		} catch (NamingException localNamingException) {
		}
	}

	/**
	 * 
	 * @param from
	 *            发件人
	 */
	private CcBccMailSender(String from) {
		if (from == null) {
			throw new IllegalArgumentException("参数from不能为null。");
		}

		int leftSign = (from = from.trim()).charAt(from.length() - 1) == '>' ? from.lastIndexOf('<') : -1;

		this.senderAddress = (leftSign > -1 ? from.substring(leftSign + 1, from.length() - 1).trim() : from);

		if (!PATTERN.matcher(this.senderAddress).find()) {
			throw new IllegalArgumentException("参数from不正确。");
		}

		this.sender = (leftSign > -1 ? from.substring(0, leftSign).trim() : null);

		if (this.sender != null)
			if (this.sender.length() == 0)
				this.sender = null;
			else if ((this.sender.charAt(0) == '"') && (this.sender.charAt(this.sender.length() - 1) == '"'))
				this.sender = this.sender.substring(1, this.sender.length() - 1).trim();
	}

	/**
	 * @param from
	 *            发件人
	 * @param tos
	 *            收件人
	 * @param ccs
	 *            抄送人
	 * @param subjects
	 *            主题
	 * @param contents
	 *            內容
	 * @param attachmentss
	 *            附件
	 * @param isHtmls
	 *            是否
	 * @param isUrgents
	 * @param ccstrs
	 * @param mailFlags
	 * @return
	 * @throws IllegalArgumentException
	 */
	public static CcBccMailSender createSmtpMailSender(String from, String tos, String ccs, String subjects,
			String contents, File[] attachmentss, boolean isHtmls, boolean isUrgents, String ccstrs, int mailFlags)
			throws IllegalArgumentException {
		to = tos;
		cc = ccs;
		subject = subjects;
		content = contents;
		attachments = attachmentss;
		isHtml = isHtmls;
		isUrgent = isUrgents;
		ccstr = ccstrs;
		mailFlag = mailFlags;
		return new CcBccMailSender(from);
	}

	/**
	 * 
	 * @param to
	 * @param cc
	 * @param subject
	 * @param content
	 * @param attachments
	 * @param isHtml
	 * @param isUrgent
	 * @param ccstr
	 * @param mailFlag
	 * @param errorMail
	 * @param allto
	 * @return
	 * @throws IllegalArgumentException
	 */
	public TaskBean sendMail(String to, String cc, String subject, String content, File[] attachments, boolean isHtml,
			boolean isUrgent, String ccstr, int mailFlag, String allto, int key) throws IllegalArgumentException {
		// 接受服务器返回的数据
		String backResponse = "";

		if (this.sbStr == null) {
			this.sbStr = new StringBuffer("");
		}

		if ((attachments != null) && (attachments.length > 0)) {
			for (File file : attachments) {
				this.sbStr.append(file.getName()).append(",");
			}
			this.sbStr.delete(this.sbStr.lastIndexOf(","), this.sbStr.length());
		}

		// 返回数据的封装
		TaskBean taskBean = new TaskBean(key, true, "", senderAddress, to, subject, content, cc, "", sbStr.toString());
		if ((to == null) || ("".equals(to))) {
			log4j.info("The recipient parameter cannot be null。");
			taskBean.setMessage("The recipient parameter cannot be null");
			taskBean.setFlag(false);
			return taskBean;
		}

		log4j.info("******开始发送CC和BCC******" + to + "******抄送人******" + cc + "******主题******" + subject);

		boolean needBoundary = (attachments != null) && (attachments.length > 0);
		Socket socket = null;
		InputStream ccin = null;
		OutputStream ccout = null;
		try {
			String[] address = parseDomain(parseUrl(cc));

			if (address == null || "".equals(address)) {
				log4j.info("失败:解析MX记录为空,无法解析获取邮件服务器地址");
				taskBean.setMessage("解析MX记录为空,无法解析获取邮件服务器地址");
				taskBean.setFlag(false);
				return taskBean;
			}

			for (int k = 0; k < address.length; k++) {
				try {
					log4j.info("cc连接: 主机:\"" + address[k] + "\" 端口:\"" + 25 + "\"");
					socket = new Socket(address[k], 25);

					ccin = socket.getInputStream();
					ccout = socket.getOutputStream();

					// 连接成功跳出返回
					backResponse = response(ccin);
					if (backResponse != "" && Integer.parseInt(backResponse.substring(0, 3)) == 220) {
						break;
					}
				} catch (Exception e) {
					log4j.info("连接错误: 连接失败,原因:" + e.getMessage());
					if (k >= (address.length - 1)) {
						taskBean.setMessage("连接对方邮件服务器[" + address[k] + "]失败,原因:" + e.getMessage());
						taskBean.setFlag(false);
						return taskBean;
					}
				}
			}

			if ((ccin == null) || (ccout == null)) {
				log4j.info("失败:连接邮件服务器获取输出输入流失败");
				taskBean.setMessage("连接邮件服务器获取输出输入流失败");
				taskBean.setFlag(false);
				return taskBean;
			}

			socket.setSoTimeout(180000);

			sendString("HELO " + parseUrl(cc), ccout);
			sendNewline(ccout);
			// 失败后返回错误信息
			backResponse = response(ccin);
			if (backResponse != "" && Integer.parseInt(backResponse.substring(0, 3)) != 250) {
				log4j.info("失败:HELO命令無法完成,原因:" + backResponse.substring(4));
				taskBean.setMessage("HELO命令無法完成,原因:" + backResponse.substring(4));
				taskBean.setFlag(false);
				return taskBean;
			}

			// 校验发件人
			sendString("MAIL FROM: <" + this.senderAddress + ">", ccout);
			sendNewline(ccout);
			backResponse = response(ccin);
			if (backResponse != "" && Integer.parseInt(backResponse.substring(0, 3)) != 250) {
				log4j.info("失败:MAIL FROM 命令无法完成,原因:" + backResponse.substring(4));
				taskBean.setMessage("MAIL FROM 命令无法完成,原因:" + backResponse.substring(4));
				taskBean.setFlag(false);
				return taskBean;
			}

			sendString("RCPT TO: <" + cc + ">", ccout);
			sendNewline(ccout);

			backResponse = response(ccin);
			if (backResponse != "" && Integer.parseInt(backResponse.substring(0, 3)) != 250) {
				log4j.info("失败:RCPT TO 命令無法完成,原因:" + backResponse.substring(4));
				taskBean.setMessage("RCPT TO 命令無法完成,原因:" + backResponse.substring(4));
				taskBean.setFlag(false);
				return taskBean;
			}

			sendString("DATA", ccout);
			sendNewline(ccout);
			backResponse = response(ccin);
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
									.append(this.senderAddress).append(">").toString()), ccout);
			sendNewline(ccout);

			sendString("To: "
					+ (allto == null ? allto : new StringBuilder(String.valueOf(getBase64String(allto))).append(" <")
							.append(allto).append(">").toString()), ccout);
			sendNewline(ccout);

			if (mailFlag == 1) {
				sendString(
						"Bcc: "
								+ (ccstr == null ? ccstr : new StringBuilder(String.valueOf(getBase64String(ccstr)))
										.append(" <").append(ccstr).append(">").toString()), ccout);
				sendNewline(ccout);
			} else {
				sendString(
						"Cc: "
								+ (ccstr == null ? ccstr : new StringBuilder(String.valueOf(getBase64String(ccstr)))
										.append(" <").append(ccstr).append(">").toString()), ccout);
				sendNewline(ccout);
			}

			sendString("Subject: " + getBase64String(subject), ccout);
			sendNewline(ccout);
			sendString(
					"Date: "
							+ new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z", Locale.getDefault())
									.format(new Date()),
					ccout);
			sendNewline(ccout);
			sendString("MIME-Version: 1.0", ccout);
			sendNewline(ccout);

			if (needBoundary) {
				sendString("Content-Type: multipart/mixed; BOUNDARY=\"" + BOUNDARY + "\"", ccout);
				sendNewline(ccout);
			} else if (isHtml) {
				sendString("Content-Type: text/html; charset=\"" + CHARSET + "\"", ccout);
				sendNewline(ccout);
			} else {
				sendString("Content-Type: text/plain; charset=\"" + CHARSET + "\"", ccout);
				sendNewline(ccout);
			}

			sendString("Content-Transfer-Encoding: base64", ccout);
			sendNewline(ccout);

			if (isUrgent) {
				sendString("X-Priority: 1", ccout);
				sendNewline(ccout);
			} else {
				sendString("X-Priority: 3", ccout);
				sendNewline(ccout);
			}

			sendString("X-Mailer: BlackFox Mail[Copyright(C) 2007 Sol]", ccout);
			sendNewline(ccout);

			log4j.info("发送: ");
			sendNewline(ccout);

			if (needBoundary) {
				sendString("--" + BOUNDARY, ccout);
				sendNewline(ccout);

				if (isHtml) {
					sendString("Content-Type: text/html; charset=\"" + CHARSET + "\"", ccout);
					sendNewline(ccout);
				} else {
					sendString("Content-Type: text/plain; charset=\"" + CHARSET + "\"", ccout);
					sendNewline(ccout);
				}

				sendString("Content-Transfer-Encoding: base64", ccout);
				sendNewline(ccout);

				sendNewline(ccout);
			}

			byte[] data = (content != null ? content : "").getBytes();

			for (int k = 0; k < data.length; k += 54) {
				sendString(Base64.encode(data, k, Math.min(data.length - k, 54)), ccout);
				sendNewline(ccout);
			}

			if (needBoundary) {
				log4j.info("*****CC*****开始传输附件*******");
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

						sendString("--" + BOUNDARY, ccout);
						sendNewline(ccout);
						sendString(
								"Content-Type: "
										+ MimeTypeFactory.getMimeType(fileName.indexOf(".") == -1 ? "*" : fileName
												.substring(fileName.lastIndexOf(".") + 1)) + "; name=\""
										+ (fileName = getBase64String(fileName)) + "\"", ccout);
						sendNewline(ccout);
						sendString("Content-Transfer-Encoding: base64", ccout);
						sendNewline(ccout);
						sendString("Content-Disposition: attachment; filename=\"" + fileName + "\"", ccout);
						sendNewline(ccout);

						sendNewline(ccout);
						int k;
						while ((k = attachment.read(data)) != -1) {
							sendString(Base64.encode(data, 0, k), ccout);
							sendNewline(ccout);
						}
					}
				} catch (FileNotFoundException e) {
					e.printStackTrace();

					String errorMsg = "{"
							+ new SimpleDateFormat("yyyy年MM月dd日 HH时mm分ss秒 E", Locale.CHINA).format(new Date())
							+ " fail#" + this.senderAddress + "#" + cc + "#" + subject + "#" + content + "#"
							+ this.sbStr.toString() + "}";

					log4j.info(errorMsg);

					if (attachment != null)
						try {
							attachment.close();
						} catch (IOException localIOException22) {
						}

					taskBean.setMessage("附件\"" + attachments[fileIndex].getAbsolutePath() + "\"不存在,原因:"
							+ e.getMessage());
					taskBean.setFlag(false);
					return taskBean;
				} catch (IOException e) {
					e.printStackTrace();

					String errorMsg = "{"
							+ new SimpleDateFormat("yyyy年MM月dd日 HH时mm分ss秒 E", Locale.CHINA).format(new Date())
							+ " fail#" + this.senderAddress + "#" + cc + "#" + subject + "#" + content + "#"
							+ this.sbStr.toString() + "}";

					log4j.info(errorMsg);

					if (attachment != null)
						try {
							attachment.close();
						} catch (IOException localIOException26) {
						}

					taskBean.setMessage("无法读取附件\"" + attachments[fileIndex].getAbsolutePath() + "\",原因:"
							+ e.getMessage());
					taskBean.setFlag(false);
					return taskBean;
				} finally {
					if (attachment != null)
						try {
							attachment.close();
						} catch (IOException localIOException30) {
						}
				}
				if (attachment != null) {
					try {
						attachment.close();
					} catch (IOException localIOException31) {
					}
				}
				sendString("--" + BOUNDARY + "--", ccout);
				sendNewline(ccout);
			}

			sendString(".", ccout);
			sendNewline(ccout);
			backResponse = response(ccin);
			if (backResponse != "" && Integer.parseInt(backResponse.substring(0, 3)) != 250) {
				log4j.info("失败:.結束命令無法完成,原因:" + backResponse.substring(4));
				taskBean.setMessage(".結束命令無法完成,原因:" + backResponse.substring(4));
				taskBean.setFlag(false);
				return taskBean;
			}

			sendString("QUIT", ccout);
			sendNewline(ccout);
			backResponse = response(ccin);
			if (backResponse != "" && Integer.parseInt(backResponse.substring(0, 3)) != 221) {
				log4j.info("失败:QUIT結束命令無法完成,原因:" + backResponse.substring(4));
				taskBean.setMessage("QUIT結束命令無法完成,原因:" + backResponse.substring(4));
				taskBean.setFlag(false);
				return taskBean;
			}

			// 最终发送成功
			taskBean.setMessage("send to [" + cc + "] is success");
			taskBean.setFlag(true);
			return taskBean;
		} catch (SocketTimeoutException e) {
			log4j.info("*****CC*****连接异常");

			String errorMsg = "{" + new SimpleDateFormat("yyyy年MM月dd日 HH时mm分ss秒 E", Locale.CHINA).format(new Date())
					+ " fail#" + this.senderAddress + "#" + cc + "#" + subject + "#" + content + "#"
					+ this.sbStr.toString() + "}";

			log4j.info(errorMsg);

			taskBean.setMessage("连接超时,原因:" + e.getMessage());
			taskBean.setFlag(false);
			return taskBean;
		} catch (IOException e) {
			String errorMsg = "{" + new SimpleDateFormat("yyyy年MM月dd日 HH时mm分ss秒 E", Locale.CHINA).format(new Date())
					+ " fail#" + this.senderAddress + "#" + cc + "#" + subject + "#" + content + "#"
					+ this.sbStr.toString() + "}";

			log4j.info(errorMsg);

			taskBean.setMessage("连接出错,原因:" + e.getMessage());
			taskBean.setFlag(false);
			return taskBean;
		} catch (Exception e) {
			log4j.info("*****CC*****错误" + e.getMessage());

			String errorMsg = "{" + new SimpleDateFormat("yyyy年MM月dd日 HH时mm分ss秒 E", Locale.CHINA).format(new Date())
					+ " fail#" + this.senderAddress + "#" + cc + "#" + subject + "#" + content + "#"
					+ this.sbStr.toString() + "}";

			log4j.info(errorMsg);

			log4j.info("错误: " + e.toString());
			taskBean.setMessage("发送出错,原因:" + e.getMessage());
			taskBean.setFlag(false);
			return taskBean;
		} finally {
			if (ccin != null)
				try {
					ccin.close();
				} catch (IOException localIOException50) {
				}
			if (ccout != null)
				try {
					ccout.close();
				} catch (IOException localIOException51) {
				}
			if (socket != null)
				try {
					socket.close();
				} catch (IOException localIOException52) {
				}
			log4j.info("*****发送CC和BCC结束*****" + to + "*****抄送人*****" + cc + "*****主题*****" + subject);
		}
	}

	public void sendFailMail(String from, String errorMail, String content) {
		if ((from == null) || ("".equals(from))) {
			from = "scm_admin@genscriptcorp.com";
		}
		SmtpMailSender mailSend = SmtpMailSender.createSmtpMailSender(from);
		mailSend.sendFailMail(errorMail, content);
	}

	/**
	 * 
	 * @param url
	 * @return
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
		}
		return null;
	}

	/**
	 * 
	 * @param in
	 * @return
	 * @throws IOException
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
	 * 发送信息到服务器
	 * 
	 * @param str
	 * @param out
	 * @throws IOException
	 */
	private void sendString(String str, OutputStream out) throws IOException {
		if (str == null) {
			str = "";
		}

		out.write(str.getBytes());
		out.flush();
	}

	/**
	 * 发送换行符号
	 * 
	 * @param out
	 * @throws IOException
	 */
	private static void sendNewline(OutputStream out) throws IOException {
		out.write('\r');
		out.write('\n');
		out.flush();
	}

	/**
	 * base64位编码
	 * 
	 * @param str
	 * @return
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

	/**
	 * 
	 * @param address
	 * @return
	 */
	private static String parseUrl(String address) {
		return address.substring(address.lastIndexOf('@') + 1);
	}

	private class MX {
		final int pri;
		final String address;

		MX(int pri, String host) {
			this.pri = pri;
			this.address = host;
		}
	}
}
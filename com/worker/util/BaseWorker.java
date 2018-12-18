package com.worker.util;

import org.codehaus.jackson.map.ObjectMapper;
import org.gearman.Gearman;
import org.gearman.GearmanServer;
import org.gearman.GearmanWorker;

import com.worker.bean.ResultBean;
import com.worker.bean.ResultDTO;

/**
 * 父类一些公用方法
 */
public abstract class BaseWorker
{
  protected String resultMessage = "";
  protected String paramMessage = "";
  protected String uid = "";
  protected String cmd = "";
  protected static Gearman gearman;
  protected static GearmanWorker worker;
  protected static GearmanServer server;
  protected static GearmanServer server2;
  protected static String EXIT = "1";
  protected static String RESTART = "2";
  protected static String SHUTDOWN = "3";
  public static final String LOG_EMERG = "LOG_EMERG";
  public static final String LOG_DEBUG = "LOG_DEBUG";
  public static final String LOG_ALERT = "LOG_ALERT";
  public static final String LOG_CRIT = "LOG_CRIT";
  public static final String LOG_ERR = "LOG_ERR";
  public static final String LOG_WARNING = "LOG_WARNING";
  public static final String LOG_NOTICE = "LOG_NOTICE";
  public static final String LOG_INFO = "LOG_INFO";

  protected String getBackMessage(String status, String message, String uid, ResultDTO result)
  {
    String res = "";
    ObjectMapper mapper = new ObjectMapper();
    ResultBean back = new ResultBean();

    back.setStatus(status);

    back.setUid(uid);

    back.setMessage(message);

    back.setResult(result);
    try {
      res = mapper.writeValueAsString(back);
    } catch (Exception e) {
      e.printStackTrace();
    }
    return res == null ? "" : res;
  }

  protected void addServer()
  {
    if ((worker != null) && (!worker.isShutdown())) {
      server = gearman.createGearmanServer("", 0);
      worker.addServer(server);
    }
  }

  protected void shutDown()
  {
    if ((worker != null) && (!worker.isShutdown()))
      worker.shutdown();
  }

  protected void exit()
  {
    if ((worker != null) && (!worker.isShutdown())) {
      worker.shutdown();
      System.exit(0);
    }
  }

  protected void reStart()
  {
    if ((worker != null) && (worker.isShutdown())) {
      if ((gearman == null) || (gearman.isShutdown())) {
        gearman = Gearman.createGearman();
      }
      worker = gearman.createGearmanWorker();
      worker.addFunction("", null);
      worker.addServer(server);
    }
  }

  public String controlWorker(String cmd)
  {
    String value = "";
    if (("".equals(cmd)) && (SHUTDOWN.equals(cmd))) {
      value = "shutdown";
      shutDown();
    } else if (("".equals(cmd)) && (RESTART.equals(cmd))) {
      value = "restart";
      reStart();
    } else if (("".equals(cmd)) && (EXIT.equals(cmd))) {
      value = "exit";
      exit();
    }
    return value;
  }
}
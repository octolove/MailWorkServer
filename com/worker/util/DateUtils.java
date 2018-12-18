package com.worker.util;

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class DateUtils
{
  public static final String C_TIME_PATTON_DEFAULT = "yyyy-MM-dd HH:mm:ss";
  public static final String C_DATE_PATTON_DEFAULT = "yyyy-MM-dd";
  public static final String SQLTIME = "HH:mm:ss";
  public static final int C_ONE_SECOND = 1000;
  public static final int C_ONE_MINUTE = 60000;
  public static final long C_ONE_HOUR = 3600000L;
  public static final long C_ONE_DAY = 86400000L;

  public static Date calendar2Date(Calendar calendar)
  {
    if (calendar == null) {
      return null;
    }
    return calendar.getTime();
  }

  public static Calendar date2Calendar(Date date) {
    if (date == null) {
      return null;
    }
    Calendar calendar = Calendar.getInstance();
    calendar.setTime(date);
    return calendar;
  }

  public static SimpleDateFormat getSimpleDateFormat() {
    return getSimpleDateFormat(null);
  }

  public static SimpleDateFormat getSimpleDateFormat(String format)
  {
    SimpleDateFormat sdf;
    if (format == null)
      sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    else {
      sdf = new SimpleDateFormat(format);
    }

    return sdf;
  }

  public static String formatDate2Str() {
    return formatDate2Str(new Date());
  }

  public static String formatDate2Str(Date date) {
    return formatDate2Str(date, "yyyy-MM-dd HH:mm:ss");
  }

  public static String formatDate2StrHH(Date date) {
    return formatDate2Str(date, "yyyy-MM-dd");
  }

  public static String formatDate2Str(Date date, String format) {
    if (date == null) {
      return null;
    }

    if ((format == null) || (format.equals(""))) {
      format = "yyyy-MM-dd HH:mm:ss";
    }
    SimpleDateFormat sdf = getSimpleDateFormat(format);
    return sdf.format(date);
  }

  public static String formatDate2SQLTime(Date date, String format) {
    if (date == null) {
      return null;
    }

    if ((format == null) || (format.equals(""))) {
      format = "HH:mm:ss";
    }
    SimpleDateFormat sdf = getSimpleDateFormat(format);
    return sdf.format(date);
  }

  public static Date formatStr2Date(String dateStr, String format) {
    if (dateStr == null) {
      return null;
    }

    if ((format == null) || (format.equals(""))) {
      format = "yyyy-MM-dd HH:mm:ss";
    }
    SimpleDateFormat sdf = getSimpleDateFormat(format);
    return sdf.parse(dateStr, new ParsePosition(0));
  }
}
package com.example.hikerview.utils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 作者：By 15968
 * 日期：On 2019/12/7
 * 时间：At 12:13
 */
public class TimeUtil {
    public static String formatTime(long timestamp) {
        DateFormat formatter = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        return formatter.format(new Date(timestamp));
    }

    public static String formatTime(long timestamp, String pattern) {
        DateFormat formatter = new SimpleDateFormat(pattern);
        return formatter.format(new Date(timestamp));
    }


    public static String getSecondTimestamp() {
        return getSecondTimestamp(new Date(System.currentTimeMillis()));
    }

    /**
     * 获取精确到秒的时间戳
     *
     * @return
     */
    public static String getSecondTimestamp(Date date) {
        if (null == date) {
            return "";
        }
        String timestamp = String.valueOf(date.getTime());
        int length = timestamp.length();
        if (length > 3) {
            return timestamp.substring(0, length - 3);
        } else {
            return timestamp;
        }
    }

    /**
     * 秒数转换成格式化时间
     *
     * @param sec 秒数
     * @return xx 时 xx 分 xx 秒
     */
    public static String secToTime(int sec) {
        if (sec <= 0) {
            return "0 秒";
        } else {
            int hour = sec / 3600;
            int minute = sec % 3600 / 60;
            int second = sec % 60; // 不足 60 的就是秒，够 60 就是分
            return (hour > 0 ? (hour + " 小时 ") : "") + (minute > 0 ? (minute + " 分 ") : "") + (second + " 秒");
        }
    }


    /**
     * 通过时间秒毫秒数判断两个时间的间隔
     *
     * @param timestamp1
     * @param timestamp2
     * @return
     */
    public static int differentDaysByMillisecond(long timestamp1, long timestamp2) {
        int days = (int) (Math.abs(timestamp2 - timestamp1) / (1000 * 3600 * 24));
        return days;
    }

    /**
     * 通过时间秒毫秒数判断两个时间的间隔
     *
     * @param date1
     * @param date2
     * @return
     */
    public static int differentDaysByMillisecond(Date date1, Date date2) {
        return differentDaysByMillisecond(date1.getTime(), date2.getTime());
    }
}

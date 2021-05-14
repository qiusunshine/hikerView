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
}

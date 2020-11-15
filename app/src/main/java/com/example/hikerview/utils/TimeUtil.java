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
    public static String formatTime(long timestamp){
        DateFormat formatter = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        return formatter.format(new Date(timestamp));
    }
}

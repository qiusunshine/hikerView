package com.example.hikerview.ui.download.util;

/**
 * Created by xm on 17-8-18.
 */
public class TimeUtil {
    public static String formatTime(int second) {
        int theTime = second;// 秒
        int theTime1 = 0;// 分
        int theTime2 = 0;// 小时
        if(theTime > 60) {
            theTime1 = theTime / 60;
            theTime = theTime % 60;
            if(theTime1 > 60) {
                theTime2 = theTime1 / 60;
                theTime1 = theTime1 % 60;
            }
        }
        String result = "";
        result = ""+(theTime<10?"0"+theTime:theTime);
        result = ""+(theTime1<10?"0"+theTime1:theTime1)+":"+result;
        result = ""+(theTime2<10?"0"+theTime2:theTime2)+":"+result;
        return result;
    }
}

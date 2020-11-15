package com.example.hikerview.utils;

import android.app.Activity;

import com.githang.statusbar.StatusBarCompat;

/**
 * 作者：By 15968
 * 日期：On 2020/6/28
 * 时间：At 20:05
 */
public class StatusBarCompatUtil {
//    public static void setNowColor(int nowColor) {
//        StatusBarCompatUtil.nowColor = nowColor;
//    }
//
//    private static int nowColor;

    public static void setStatusBarColor(Activity activity, int color) {
//        if (nowColor == color) {
//            return;
//        }
//        StatusBarCompat.setStatusBarColor(activity, color);
//        nowColor = color;
        setStatusBarColorForce(activity, color);
    }

    public static void setStatusBarColorForce(Activity activity, int color) {
        StatusBarCompat.setStatusBarColor(activity, color);
//        nowColor = color;
    }
}

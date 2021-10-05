package com.jeffmony.m3u8library.utils;

import android.util.Log;

public class LogUtils {

    private static boolean sDEBUG = true;
    private static boolean sINFO = true;
    private static boolean sWARN = true;
    private static boolean sERROR = true;

    public static void d(String tag, String msg) {
        if (sDEBUG) {
            Log.d(tag, msg);
        }
    }

    public static void i(String tag, String msg) {
        if (sINFO) {
            Log.i(tag, msg);
        }
    }

    public static void w(String tag, String msg) {
        if (sWARN) {
            Log.w(tag, msg);
        }
    }

    public static void e(String tag, String msg) {
        if (sERROR) {
            Log.e(tag, msg);
        }
    }
}

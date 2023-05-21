package com.jeffmony.videocache.utils;

import android.util.Log;

/**
 * @author jeffmony
 *
 * sdk中log的通用类
 */

public class LogUtils {

    private static boolean DEBUG = false;
    private static boolean INFO = true;
    private static boolean WARN = true;

    public static void d(String tag, String msg) {
        if (DEBUG) {
            Log.d(tag, msg);
        }
    }

    public static void i(String tag, String msg) {
        if (INFO) {
            Log.i(tag, msg);
        }
    }

    public static void w(String tag, String msg) {
        if (WARN) {
            Log.w(tag, msg);
        }
    }
}

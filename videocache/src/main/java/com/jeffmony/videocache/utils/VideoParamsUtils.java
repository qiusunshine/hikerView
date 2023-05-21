package com.jeffmony.videocache.utils;

import android.text.TextUtils;

import com.jeffmony.videocache.common.VideoParams;

import java.util.Map;

/**
 * @author jeffmony
 * 解析videoparams map的工具类
 */
public class VideoParamsUtils {
    public static boolean getBooleanValue(Map<String, Object> params, String key) {
        if (params == null || TextUtils.isEmpty(key)) {
            return false;
        }

        Object object = params.get(key);
        if (object instanceof Boolean) {
            return (boolean) object;
        }
        return false;
    }

    public static String getStringValue(Map<String, Object> params, String key) {
        if (params == null || TextUtils.isEmpty(key)) {
            return VideoParams.UNKNOWN;
        }

        Object object = params.get(key);
        if (object instanceof String) {
            return String.valueOf(object);
        }
        return VideoParams.UNKNOWN;
    }

    public static int getIntegerValue(Map<String, Object> params, String key) {
        if (params == null || TextUtils.isEmpty(key)) {
            return -1;
        }

        Object object = params.get(key);
        if (object instanceof Integer) {
            return (Integer) object;
        }
        return -1;
    }

    public static long getLongValue(Map<String, Object> params, String key) {
        if (params == null || TextUtils.isEmpty(key)) {
            return -1L;
        }

        Object object = params.get(key);
        if (object instanceof Long) {
            return (Long) object;
        }
        return -1L;
    }

    public static float getFloatValue(Map<String, Object> params, String key) {
        if (params == null || TextUtils.isEmpty(key)) {
            return 0f;
        }
        Object object = params.get(key);
        if (object instanceof Float) {
            return (Float) object;
        }
        return 0f;
    }
}

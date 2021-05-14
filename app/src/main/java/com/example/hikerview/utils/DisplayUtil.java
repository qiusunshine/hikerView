package com.example.hikerview.utils;

import android.content.Context;

/**
 * 作者：By 15968
 * 日期：On 2019/9/28
 * 时间：At 17:17
 */
public class DisplayUtil {
    public static int pxToDp(Context context, int px) {
        if (context == null) {
            return px;
        }
        if (px <= 0) {
            return px;
        }
        float scale = context.getResources().getDisplayMetrics().density;
        return (int) (px / scale + 0.5f);
    }

    public static int dpToPx(Context context, int dp) {
        if (context == null) {
            return dp;
        }
        if (dp <= 0) {
            return dp;
        }
        float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dp * scale + 0.5f);
    }
}

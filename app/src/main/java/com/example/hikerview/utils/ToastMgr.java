package com.example.hikerview.utils;

/**
 * 作者：By hdy
 * 日期：On 2017/11/6
 * 时间：At 16:51
 */

import android.content.Context;
import android.view.Gravity;
import android.widget.Toast;

public class ToastMgr {

    public static void shortBottomCenter(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    public static void shortCenter(Context context, String message) {
        Toast toast = Toast.makeText(context, message, Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();
    }

    public static void longCenter(Context context, String message) {
        Toast toast = Toast.makeText(context, message, Toast.LENGTH_LONG);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();
    }
}

package com.example.hikerview.utils;

import android.content.Context;
import android.content.pm.PackageManager;

/**
 * 作者：By 15968
 * 日期：On 2020/2/7
 * 时间：At 18:22
 */
public class CommonUtil {
    public static String getVersionName(Context context) {
        try {
            String pkName = context.getPackageName();
            return context.getPackageManager().getPackageInfo(
                    pkName, 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return "";
        }
    }
}

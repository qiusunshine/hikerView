package com.example.hikerview.utils;

import android.content.Context;
import android.graphics.Color;

import androidx.fragment.app.FragmentManager;

import com.example.hikerview.R;
import com.example.hikerview.ui.setting.model.SettingConfig;
import com.example.hikerview.ui.view.PopImageLoaderNoView;
import com.lxj.xpopup.XPopup;

import java.util.Arrays;
import java.util.List;

/**
 * 作者：By 15968
 * 日期：On 2019/10/31
 * 时间：At 20:26
 */
public class SplashUtil {
    private static boolean hasCheck = false;
    private static int splashOpenTimes = -1;
    private static int shouldTimes = 2;

    public static void setShowLoading(boolean showLoading) {
        SplashUtil.showLoading = showLoading;
    }

    private static boolean showLoading = true;

    public static boolean showSplash(Context context, FragmentManager manager) {
        if(SettingConfig.professionalMode){
            return false;
        }
        int splashOpenTimes = PreferenceMgr.getInt(context, "splashOpenTimes", 0);
        if (splashOpenTimes < shouldTimes) {
            List<Object> imageUrls = Arrays.asList(
                    R.mipmap.guide_plugin,
                    R.mipmap.guide_view,
                    R.mipmap.guide_open);
            new XPopup.Builder(context)
                    .asImageViewer(null, 0, imageUrls,
                            false,false, -1, -1, -1,
                            false, Color.rgb(32, 36, 46), null, new PopImageLoaderNoView(""))
                    .show();
            setSplashOpenTimes(context, shouldTimes);
            return true;
//            new SplashDialogFragment(shouldTimes).show(manager, "home_splash_dialog");
        }
        return false;
    }

    public static int getSplashOpenTimes(Context context) {
        if (splashOpenTimes >= 0) {
            return splashOpenTimes;
        } else {
            splashOpenTimes = PreferenceMgr.getInt(context, "splashOpenTimes", 0);
            return splashOpenTimes;
        }
    }

    public static void setSplashOpenTimes(Context context, int shouldTimes) {
        PreferenceMgr.put(context, "splashOpenTimes", shouldTimes);
        splashOpenTimes = shouldTimes;
    }

    public static boolean canShowDialog(Context context) {
        return showLoading && getSplashOpenTimes(context) >= shouldTimes;
    }
}

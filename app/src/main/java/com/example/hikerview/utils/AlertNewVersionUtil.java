package com.example.hikerview.utils;

import android.app.Activity;

import com.example.hikerview.ui.browser.WebViewActivity;
import com.example.hikerview.ui.home.ArticleListRuleMagActivity;
import com.example.hikerview.ui.home.MainActivity;
import com.example.hikerview.ui.setting.model.SettingConfig;
import com.example.hikerview.ui.view.popup.AlertImgPopup;
import com.lxj.xpopup.XPopup;

/**
 * 作者：By 15968
 * 日期：On 2020/4/6
 * 时间：At 20:46
 */
public class AlertNewVersionUtil {

    public static void alert(Activity activity) {
        if(SettingConfig.professionalMode){
            return;
        }
        int nowVersion = PreferenceMgr.getInt(activity, "version", activity.getClass().getSimpleName(), 0);
        int newVersion = 0;
        if (activity instanceof MainActivity) {
            newVersion = 4;
        } else if (activity instanceof WebViewActivity) {
            newVersion = 5;
        }else if (activity instanceof ArticleListRuleMagActivity) {
            newVersion = 2;
        }
        if (newVersion > nowVersion) {
            PreferenceMgr.put(activity, "version", activity.getClass().getSimpleName(), newVersion);
            String path = activity.getClass().getSimpleName() + ".png";
            new XPopup.Builder(activity)
                    .asCustom(new AlertImgPopup(activity).with(path))
                    .show();
        }
    }
}

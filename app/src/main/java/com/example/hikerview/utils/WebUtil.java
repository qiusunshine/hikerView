package com.example.hikerview.utils;

import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.alibaba.fastjson.JSON;
import com.example.hikerview.R;
import com.example.hikerview.constants.ArticleColTypeEnum;
import com.example.hikerview.event.WebViewUrlChangedEvent;
import com.example.hikerview.event.web.WebUpdateUrlEvent;
import com.example.hikerview.ui.browser.WebViewActivity;
import com.example.hikerview.ui.home.FilmListActivity;
import com.example.hikerview.ui.home.model.ArticleListRule;
import com.example.hikerview.ui.setting.model.SettingConfig;
import com.example.hikerview.ui.video.PlayerChooser;

import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.io.IOException;

/**
 * 作者：By hdy
 * 日期：On 2018/8/26
 * 时间：At 10:29
 */

public class WebUtil {
    private static final String TAG = "WebUtil";
    private static String showingUrl = "";

    public static WebViewActivity getWebViewActivity() {
        return webViewActivity;
    }

    private static WebViewActivity webViewActivity;

    public static void setWebActivity(WebViewActivity webActivity) {
        webViewActivity = webActivity;
    }

    public static boolean isWebActivityExist() {
        return webViewActivity != null;
    }

    public static void goWebForce(Context context, String url) {
        if (webViewActivity != null) {
            EventBus.getDefault().post(new WebViewUrlChangedEvent(url));
            return;
        }
        Intent intent = new Intent();
        intent.setClass(context, WebViewActivity.class);
        intent.putExtra("is_xiu_tan", false);
        intent.putExtra("url", url);
        context.startActivity(intent);
    }

    public static void goWeb(Context context, String url) {
        goWeb(context, url, false);
    }

    public static void goWeb(Context context, String url, boolean newWindow) {
        if (context instanceof Activity && webViewActivity != null) {
            Activity activity = (Activity) context;
            activity.finish();
            EventBus.getDefault().post(new WebUpdateUrlEvent(url, newWindow));
            return;
        }
        Intent intent = new Intent();
        intent.setClass(context, WebViewActivity.class);
        intent.putExtra("is_xiu_tan", false);
        intent.putExtra("url", url);
        context.startActivity(intent);
    }

    public static void goWebWithExtraData(Context context, String url, Bundle extraDataBundle) {
        if (context instanceof Activity && webViewActivity != null) {
            Activity activity = (Activity) context;
            Intent intent = new Intent();
            intent.putExtra("url", url);
            if (extraDataBundle != null) {
                intent.putExtra("extraDataBundle", extraDataBundle);
            }
            activity.setResult(101, intent);
            activity.finish();
            return;
        }
        Intent intent = new Intent();
        intent.setClass(context, WebViewActivity.class);
        intent.putExtra("is_xiu_tan", false);
        intent.putExtra("url", url);
        if (extraDataBundle != null) {
            intent.putExtra("extraDataBundle", extraDataBundle);
        }
        context.startActivity(intent);
    }

    public static void goBBS(Activity context) {
        Intent intent = new Intent();
        intent.setClass(context, WebViewActivity.class);
        intent.putExtra("is_xiu_tan", false);
        intent.putExtra("url", context.getResources().getString(R.string.bbs_new_url));
        intent.putExtra("homeTabMode", "bbs");
        context.startActivity(intent, ActivityOptions.makeCustomAnimation(context, R.anim.alpha_no_trans, R.anim.alpha_no_trans).toBundle());
    }

    public static void goWebHome(Activity context) {
        Intent intent = new Intent();
        intent.setClass(context, WebViewActivity.class);
        intent.putExtra("is_xiu_tan", false);
        String webHome = SettingConfig.professionalMode ? "https://movie.douban.com/tag/#/" : context.getResources().getString(R.string.search_engine);
        String defaultRightUrl = PreferenceMgr.getString(context, "defaultRightUrl", webHome);
        if ("http://159.75.120.248/home".equals(defaultRightUrl) || "http://home.haikuoshijie.cn/home".equals(defaultRightUrl)) {
            defaultRightUrl = webHome;
            PreferenceMgr.put(context, "defaultRightUrl", webHome);
        }
        intent.putExtra("url", defaultRightUrl);
        intent.putExtra("homeTabMode", "webhome");
        context.startActivity(intent, ActivityOptions.makeCustomAnimation(context, R.anim.alpha_no_trans, R.anim.alpha_no_trans).toBundle());
    }

    public static void goWebHomeAndSearch(Activity context) {
        Intent intent = new Intent();
        intent.setClass(context, WebViewActivity.class);
        intent.putExtra("is_xiu_tan", false);
        String webHome = SettingConfig.professionalMode ? "https://movie.douban.com/tag/#/" : context.getResources().getString(R.string.search_engine);
        String defaultRightUrl = PreferenceMgr.getString(context, "defaultRightUrl", webHome);
        intent.putExtra("url", defaultRightUrl);
        intent.putExtra("homeTabMode", "webhome");
        intent.putExtra("showSearch", true);
        context.startActivity(intent, ActivityOptions.makeCustomAnimation(context, R.anim.alpha_no_trans, R.anim.alpha_no_trans).toBundle());
    }

    public static void goWebFromHistoryVideo(Context context, String url, String title, String videoUrl) {
        if (webViewActivity != null) {
            PlayerChooser.startPlayer(context, title, videoUrl);
            return;
        }
        Intent intent = new Intent();
        intent.setClass(context, WebViewActivity.class);
        intent.putExtra("is_xiu_tan", false);
        intent.putExtra("url", url);
        intent.putExtra("title", title);
        intent.putExtra("videoUrl", videoUrl);
        context.startActivity(intent);
    }

    public static void goWebForXiuTan(Context context, String title, String url) {
        Intent intent = new Intent();
        intent.setClass(context, WebViewActivity.class);
        intent.putExtra("is_xiu_tan", true);
        intent.putExtra("title", title);
        intent.putExtra("url", url);
        context.startActivity(intent);
    }

    public static void goLocalHome(Context context) {
        String ruleDir = UriUtils.getRootDir(context).replace("file://", "") + File.separator + "rules";
        if (!new File(ruleDir).exists()) {
            new File(ruleDir).mkdir();
        }
        String homeAddr = UriUtils.getRootDir(context).replace("file://", "") + File.separator + "home.html";
        String homeDir = ruleDir + File.separator + "home";
        File dir = new File(homeDir);
        if (!dir.exists()) {
            dir.mkdir();
        }
        String homeIndex = homeDir + File.separator + "index.html";
        if (new File(homeIndex).exists()) {
            WebUtil.goWeb(context, "file://" + homeIndex);
            return;
        } else {
            String homeHome = homeDir + File.separator + "home.html";
            if (new File(homeHome).exists()) {
                WebUtil.goWeb(context, "file://" + homeHome);
                return;
            }
        }
        if (new File(homeAddr).exists()) {
            WebUtil.goWeb(context, "file://" + homeAddr);
            return;
        }
        WebUtil.goWeb(context, "file://" + homeIndex);
    }

    public static String getLocalHomePath(Context context) {
        String ruleDir = UriUtils.getRootDir(context).replace("file://", "") + File.separator + "rules";
        if (!new File(ruleDir).exists()) {
            new File(ruleDir).mkdir();
        }
        String homeAddr = UriUtils.getRootDir(context).replace("file://", "") + File.separator + "home.html";
        String homeDir = ruleDir + File.separator + "home";
        File dir = new File(homeDir);
        if (!dir.exists()) {
            dir.mkdir();
        }
        String homeIndex = homeDir + File.separator + "index.html";
        if (new File(homeIndex).exists()) {
            return homeIndex;
        } else {
            String homeHome = homeDir + File.separator + "home.html";
            if (new File(homeHome).exists()) {
                return homeHome;
            }
        }
        if (new File(homeAddr).exists()) {
            return homeAddr;
        }
        return homeIndex;
    }

    public static void saveLocalHomeContent(Context context, String content) throws IOException {
        String ruleDir = UriUtils.getRootDir(context).replace("file://", "") + File.separator + "rules";
        if (!new File(ruleDir).exists()) {
            new File(ruleDir).mkdir();
        }
        String homeDir = ruleDir + File.separator + "home";
        File dir = new File(homeDir);
        if (!dir.exists()) {
            dir.mkdir();
        }
        String homeIndex = homeDir + File.separator + "index.html";
        if (new File(homeIndex).exists()) {
            FileUtil.stringToFile(content, homeIndex);
            return;
        } else {
            String homeHome = homeDir + File.separator + "home.html";
            if (new File(homeHome).exists()) {
                FileUtil.stringToFile(content, homeHome);
                return;
            }
        }
        String homeAddr = UriUtils.getRootDir(context).replace("file://", "") + File.separator + "home.html";
        if (new File(homeAddr).exists()) {
            FileUtil.stringToFile(content, homeAddr);
            return;
        }
        FileUtil.stringToFile(content, homeIndex);
    }

    public static void goX5Page(Context context, String title, String url){
        ArticleListRule articleListRule1 = new ArticleListRule();
        articleListRule1.setUrl("hiker://empty");
        articleListRule1.setFind_rule("js:setResult([{\n" +
                "    url:\"" + url + "\",\n" +
                "desc:\"100%&&float\"\n" +
                "}]);");
        articleListRule1.setCol_type(ArticleColTypeEnum.X5_WEB_VIEW.getCode());
        articleListRule1.setTitle(title);
        Intent intent = new Intent(context, FilmListActivity.class);
        intent.putExtra("data", JSON.toJSONString(articleListRule1));
        intent.putExtra("title", title);
        context.startActivity(intent);
    }

    public static String getShowingUrl() {
        return showingUrl;
    }

    public static void setShowingUrl(String showingUrl) {
        WebUtil.showingUrl = showingUrl;
    }
}

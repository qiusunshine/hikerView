package com.example.hikerview.utils;

import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;
import com.example.hikerview.constants.ImageUrlMapEnum;
import com.example.hikerview.ui.setting.model.SettingConfig;

import java.io.File;

/**
 * 作者：By 15968
 * 日期：On 2020/3/15
 * 时间：At 21:52
 */
public class GlideUtil {
    private static final String TAG = "GlideUtil";

    public static Object getGlideUrl(String baseUrl, String url) {
        int drawableId = ImageUrlMapEnum.getIdByUrl(url);
        if (drawableId > 0) {
            return drawableId;
        }
        if (StringUtil.isEmpty(baseUrl) || url == null || !url.startsWith("http")) {
            if (StringUtil.isNotEmpty(url) && url.startsWith("hiker://files/")) {
                String fileName = url.replace("hiker://files/", "");
                return "file://" + SettingConfig.rootDir + File.separator + fileName;
            }
            return url;
        } else {
            LazyHeaders.Builder builder = new LazyHeaders.Builder();
            String refer = StringUtil.getDom(baseUrl);
            if (baseUrl.startsWith("https")) {
                refer = "https://" + refer + "/";
            } else {
                refer = "http://" + refer + "/";
            }
            String ua = SettingConfig.getGlideUA();

            //检查链接里面是否有自定义cookie
            String[] cookieUrl = url.split("@Cookie=");
            if (cookieUrl.length > 1) {
                url = cookieUrl[0];
                builder.addHeader("Cookie", cookieUrl[1]);
            }

            //检查链接里面是否有自定义UA和referer
            String[] s = url.split("@Referer=");
            if (s.length > 1) {
                if (s[0].contains("@User-Agent=")) {
                    refer = s[1];
                    url = s[0].split("@User-Agent=")[0];
                    ua = s[0].split("@User-Agent=")[1];
                } else if (s[1].contains("@User-Agent=")) {
                    refer = s[1].split("@User-Agent=")[0];
                    url = s[0];
                    ua = s[1].split("@User-Agent=")[1];
                } else {
                    refer = s[1];
                    url = s[0];
                }
            } else {
                if (url.contains("@Referer=")) {
                    url = url.replace("@Referer=", "");
                    refer = "";
                }
                if (url.contains("@User-Agent=")) {
                    ua = url.split("@User-Agent=")[1];
                    url = url.split("@User-Agent=")[0];
                }
            }
//            Log.d(TAG, "getGlideUrl: " + url);
//            Log.d(TAG, "getGlideUrl: " + refer);
//            Log.d(TAG, "getGlideUrl: " + ua);
            if (!StringUtil.isEmpty(ua)) {
                if (StringUtil.isEmpty(refer)) {
                    return new GlideUrl(url, builder.addHeader("User-Agent", ua).build());
                }
                return new GlideUrl(url, builder.addHeader("User-Agent", ua).addHeader("Referer", refer).build());
            } else {
                if (StringUtil.isEmpty(refer)) {
                    return new GlideUrl(url);
                }
                return new GlideUrl(url, builder.addHeader("Referer", refer).build());
            }
        }
    }
}

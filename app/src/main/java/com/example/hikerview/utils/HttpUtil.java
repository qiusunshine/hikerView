package com.example.hikerview.utils;

/**
 * 作者：By 15968
 * 日期：On 2020/3/15
 * 时间：At 21:52
 */
public class HttpUtil {
    public static String getRealUrl(String domUrl, String url) {
        String baseUrl = StringUtil.getBaseUrl(domUrl);
        if (url.startsWith("http")) {
            return url;
        } else if (url.startsWith("//")) {
            return "http:" + url;
        } else if (url.startsWith("magnet") || url.startsWith("thunder") || url.startsWith("ftp") || url.startsWith("ed2k")) {
            return url;
        } else if (url.startsWith("/")) {
            if (baseUrl.endsWith("/")) {
                return baseUrl.replace("/", "") + url;
            } else {
                return baseUrl + url;
            }
        } else if (url.startsWith("./")) {
            String searchUrl = domUrl.split(";")[0];
            String[] c = searchUrl.split("/");
            if (c.length <= 1) {
                return url;
            }
            String sub = searchUrl.replace(c[c.length - 1], "");
            return sub + url.replace("./", "");
        } else if (url.startsWith("?")) {
            return domUrl + url;
        } else {
            return url;
        }
    }
}

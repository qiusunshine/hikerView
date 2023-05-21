package com.jeffmony.videocache.utils;

import android.text.TextUtils;

import java.net.URL;

/**
 * @author jeffmony
 *
 * url处理的通用类
 */
public class UrlUtils {

    public static String getM3U8MasterUrl(String videoUrl, String line) {
        if (TextUtils.isEmpty(videoUrl) || TextUtils.isEmpty(line)) {
            return "";
        }
        if (videoUrl.startsWith("file://") || videoUrl.startsWith("/")) {
            return videoUrl;
        }
        String baseUriPath = getBaseUrl(videoUrl);
        String hostUrl = getHostUrl(videoUrl);
        if (line.startsWith("//")) {
            String tempUrl = getSchema(videoUrl) + ":" + line;
            return tempUrl;
        }
        if (line.startsWith("/")) {
            String pathStr = getPathStr(videoUrl);
            String longestCommonPrefixStr = getLongestCommonPrefixStr(pathStr, line);
            if (hostUrl.endsWith("/")) {
                hostUrl = hostUrl.substring(0, hostUrl.length() - 1);
            }
            String tempUrl = hostUrl + longestCommonPrefixStr + line.substring(longestCommonPrefixStr.length());
            return tempUrl;
        }
        if (line.startsWith("http")) {
            return line;
        }
        return baseUriPath + line;
    }

    private static String getSchema(String url) {
        if (TextUtils.isEmpty(url)) {
            return "";
        }
        int index = url.indexOf("://");
        if (index != -1) {
            String result = url.substring(0, index);
            return result;
        }
        return "";
    }

    /**
     * 例如https://xvideo.d666111.com/xvideo/taohuadao56152307/index.m3u8
     * 我们希望得到https://xvideo.d666111.com/xvideo/taohuadao56152307/
     *
     * @param url
     * @return
     */
    public static String getBaseUrl(String url) {
        if (TextUtils.isEmpty(url)) {
            return "";
        }
        int slashIndex = url.lastIndexOf("/");
        if (slashIndex != -1) {
            return url.substring(0, slashIndex + 1);
        }
        return url;
    }

    /**
     * 例如https://xvideo.d666111.com/xvideo/taohuadao56152307/index.m3u8
     * 我们希望得到https://xvideo.d666111.com/
     *
     * @param url
     * @return
     */
    public static String getHostUrl(String url) {
        if (TextUtils.isEmpty(url)) {
            return "";
        }
        try {
            URL formatURL = new URL(url);
            String host = formatURL.getHost();
            if (host == null) {
                return url;
            }
            int hostIndex = url.indexOf(host);
            if (hostIndex != -1) {
                int port = formatURL.getPort();
                String resultUrl;
                if (port != -1) {
                    resultUrl = url.substring(0, hostIndex + host.length()) + ":" + port + "/";
                } else {
                    resultUrl = url.substring(0, hostIndex + host.length()) + "/";
                }
                return resultUrl;
            }
            return url;
        } catch (Exception e) {
            return url;
        }
    }

    /**
     * 例如https://xvideo.d666111.com/xvideo/taohuadao56152307/index.m3u8
     * 我们希望得到   /xvideo/taohuadao56152307/index.m3u8
     *
     * @param url
     * @return
     */
    public static String getPathStr(String url) {
        if (TextUtils.isEmpty(url)) {
            return "";
        }
        String hostUrl = getHostUrl(url);
        if (TextUtils.isEmpty(hostUrl)) {
            return url;
        }
        return url.substring(hostUrl.length() - 1);
    }

    /**
     * 获取两个字符串的最长公共前缀
     * /xvideo/taohuadao56152307/500kb/hls/index.m3u8   与     /xvideo/taohuadao56152307/index.m3u8
     * <p>
     * /xvideo/taohuadao56152307/500kb/hls/jNd4fapZ.ts  与     /xvideo/taohuadao56152307/500kb/hls/index.m3u8
     *
     * @param str1
     * @param str2
     * @return
     */
    public static String getLongestCommonPrefixStr(String str1, String str2) {
        if (TextUtils.isEmpty(str1) || TextUtils.isEmpty(str2)) {
            return "";
        }
        if (TextUtils.equals(str1, str2)) {
            return str1;
        }
        char[] arr1 = str1.toCharArray();
        char[] arr2 = str2.toCharArray();
        int j = 0;
        while (j < arr1.length && j < arr2.length) {
            if (arr1[j] != arr2[j]) {
                break;
            }
            j++;
        }
        return str1.substring(0, j);
    }
}

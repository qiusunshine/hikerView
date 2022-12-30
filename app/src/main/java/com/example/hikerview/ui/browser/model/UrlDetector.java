package com.example.hikerview.ui.browser.model;

import android.content.Context;

import com.alibaba.fastjson.JSON;
import com.example.hikerview.constants.ImageUrlMapEnum;
import com.example.hikerview.constants.Media;
import com.example.hikerview.model.BigTextDO;
import com.example.hikerview.ui.browser.util.CollectionUtil;
import com.example.hikerview.ui.browser.util.HttpRequestUtil;
import com.example.hikerview.ui.download.VideoFormat;
import com.example.hikerview.utils.StringUtil;
import com.example.hikerview.utils.ToastMgr;

import org.apache.commons.lang3.StringUtils;
import org.litepal.LitePal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 作者：By hdy
 * 日期：On 2018/11/1
 * 时间：At 13:23
 */
public class UrlDetector {
    private static List<String> htmls = CollectionUtil.asList(".css", ".html", ".js", ".apk");
    private static List<String> videos = CollectionUtil.asList(".mp4", ".MP4", ".m3u8", ".flv", ".avi", ".3gp", "mpeg", ".wmv", ".mov", ".MOV", "rmvb", ".dat", ".mkv", "qqBFdownload", "mime=video%2F");
    private static List<String> musics = CollectionUtil.asList(".mp3", ".wav", ".ogg", ".flac", ".m4a");
    private static List<String> images = CollectionUtil.asList(".ico", ".png", ".PNG", ".jpg", ".JPG", ".jpeg", ".JPEG", ".gif", ".GIF", ".webp");
    private static List<String> blockUrls = CollectionUtil.asList(".php?url=http", "/?url=http");
    private static List<String> makeSureNotVideoRules = CollectionUtil.asList(".mp4.jp", ".mp4.png");

    public static List<String> getVideoRules() {
        return videoRules;
    }

    private static List<String> videoRules = Collections.synchronizedList(new ArrayList<>());

    public static Media getMediaType(String url, Map<String, String> requestHeaders, String method) {
        if (StringUtil.isEmpty(method)) {
            method = "GET";
        }
        if (url.startsWith("rtmp://")) {
            Media mediaType = new Media(Media.VIDEO);
            mediaType.setType("rtmp");
            return mediaType;
        } else if (url.startsWith("rtsp://")) {
            Media mediaType = new Media(Media.VIDEO);
            mediaType.setType("rtsp");
            return mediaType;
        } else if (url.contains("isVideo=true")) {
            Media mediaType = new Media(Media.VIDEO);
            mediaType.setType("isVideo");
            return mediaType;
        } else if (url.contains("isMusic=true")) {
            Media mediaType = new Media(Media.MUSIC);
            mediaType.setType("isMusic");
            return mediaType;
        }
        String needCheckUrl = DetectUrlUtil.getNeedCheckUrl(url);
        if (StringUtil.isEmpty(needCheckUrl) || needCheckUrl.length() < 3) {
            Media mediaType = new Media(Media.HTML);
            mediaType.setType("");
            return mediaType;
        }
        for (String html : makeSureNotVideoRules) {
            if (url.contains(html)) {
                Media mediaType = new Media(Media.OTHER);
                mediaType.setType(html);
                return mediaType;
            }
        }
        for (String rule : videoRules) {
            try {
                if (Pattern.matches(rule, needCheckUrl)) {
                    Media mediaType = new Media(Media.VIDEO);
                    mediaType.setType("video");
                    return mediaType;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        for (String html : blockUrls) {
            //拦截掉
            if (needCheckUrl.contains(html)) {
                Media mediaType = new Media(Media.OTHER);
                mediaType.setType(html);
                return mediaType;
            }
        }
        String pathUrl = needCheckUrl.split("\\?")[0];
        for (String html : htmls) {
            if (pathUrl.contains(html)) {
                Media mediaType = new Media(Media.HTML);
                mediaType.setType(html);
                return mediaType;
            }
        }
        for (String image : images) {
            if (needCheckUrl.contains(image)) {
                Media mediaType = new Media(Media.IMAGE);
                mediaType.setType(image);
                return mediaType;
            }
        }
        if (!needCheckUrl.contains("#ignoreVideo=true#")) {
            for (String music : videos) {
                if (needCheckUrl.contains(music)) {
                    Media mediaType = new Media(Media.VIDEO);
                    mediaType.setType(music);
                    return mediaType;
                }
            }
        }
        if (!needCheckUrl.contains("#ignoreMusic=true#")) {
            for (String music : musics) {
                if (needCheckUrl.contains(music)) {
                    Media mediaType = new Media(Media.MUSIC);
                    mediaType.setType(music);
                    return mediaType;
                }
            }
        }
        if ("POST".equals(method.toUpperCase())) {
            Media mediaType = new Media(Media.OTHER);
            mediaType.setType("post");
            return mediaType;
        }
        if (url.contains("haikuoshijie.cn")) {
            Media mediaType = new Media(Media.HTML);
            mediaType.setType("html");
            return mediaType;
        }
        if (url.contains("captcha")) {
            Media mediaType = new Media(Media.OTHER);
            mediaType.setType("captcha");
            return mediaType;
        }
        Media media = isVideo(url, requestHeaders);
        if (media != null) {
            return media;
        }
        Media mediaType = new Media(Media.OTHER);
        mediaType.setType("");
        return mediaType;
    }

    public static boolean isImage(String url) {
        if (StringUtil.isEmpty(url)) {
            return false;
        }
        if (url.startsWith("x5Play://")) {
            return false;
        }
        if (url.contains("@rule=") || url.contains("@lazyRule=")) {
            return false;
        }
        if (ImageUrlMapEnum.getIdByUrl(url) > 0) {
            return true;
        }
        url = StringUtil.removeDom(url);
        if (url.contains("ignoreImg=true")) {
            return false;
        }
        for (String image : images) {
            if (url.contains(image)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isMusic(String url) {
        if (StringUtil.isEmpty(url)) {
            return false;
        }
        if (url.contains("@rule=") || url.contains("@lazyRule=") || url.contains("#ignoreMusic=true#")) {
            return false;
        }
        if (url.contains("isMusic=true")) {
            return true;
        }
        url = StringUtil.removeDom(url);
        for (String image : musics) {
            if (url.contains(image)) {
                return true;
            }
        }
        return false;
    }

    public static String clearTag(String url) {
        if (StringUtil.isEmpty(url)) {
            return url;
        }
        if (url.startsWith("x5Play://")) {
            url = StringUtils.replaceOnce(url, "x5Play://", "");
        }
        String[] tagList = new String[]{
                "#ignoreVideo=true#",
                "#isVideo=true#",
                "#ignoreImg=true#",
                "#immersiveTheme#",
                "#noRecordHistory#",
                "#noHistory#",
                "#noLoading#",
                "#isMusic=true#",
                "#ignoreMusic=true#",
                "#autoPage#",
                "#pre#",
                "#noPre#",
                "#fullTheme#",
                "#readTheme#",
                "#gameTheme#",
                "#noRefresh#",
                "#background#",
                "#autoCache#",
                "#cacheOnly#"
        };
        for (String tag : tagList) {
            url = StringUtils.replaceOnce(url, tag, "");
        }
        return url;
    }

    public static boolean isVideoOrMusic(String url) {
        if (StringUtil.isEmpty(url) || url.contains("ignoreVideo=true") || url.contains("#ignoreMusic=true#")) {
            return false;
        }
        if (url.contains("isVideo=true") || url.contains("isMusic=true")) {
            return true;
        }
        if (url.startsWith("x5Play://")) {
            return true;
        }
        if (url.contains("@rule=") || url.contains("@lazyRule=")) {
            return false;
        }
        if (url.startsWith("rtmp://")) {
            return true;
        } else if (url.startsWith("rtsp://") || url.contains("video://")) {
            return true;
        }
        url = DetectUrlUtil.getNeedCheckUrl(url);
        for (String html : makeSureNotVideoRules) {
            //拦截掉
            if (url.contains(html)) {
                return false;
            }
        }
        for (String rule : videoRules) {
            try {
                if (Pattern.matches(rule, url)) {
                    return true;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        for (String html : blockUrls) {
            //拦截掉
            if (url.contains(html)) {
                return false;
            }
        }
        for (String music : musics) {
            if (url.contains(music)) {
                return true;
            }
        }
        for (String music : videos) {
            if (url.contains(music)) {
                return true;
            }
        }
        return false;
    }

    private static Media isVideo(String url, Map<String, String> requestHeaders) {
        if (StringUtil.isEmpty(url) || url.contains("ignoreVideo=true")) {
            return null;
        }
        String extension = HttpRequestUtil.getFileExtensionFromUrl(url);
        if ("m3u8".equals(extension)) {
            return new Media(Media.VIDEO, extension);
        }
        try {
            VideoInfo videoInfo = DetectUrlUtil.detectVideoComplex(url, url, requestHeaders);
            if (videoInfo != null) {
                VideoFormat videoFormat = videoInfo.getVideoFormat();
                if (videoFormat != null) {
                    if ("player/m3u8".equals(videoFormat.getName())) {
                        return new Media(Media.VIDEO, "m3u8");
                    } else if (isVideoOrMusic("." + videoFormat.getName())) {
                        return new Media(Media.VIDEO, videoFormat.getName());
                    }
                }
            }
        } catch (Throwable ignored) {

        }
        return null;
    }

    public static void initVideoRules() {
        BigTextDO bigTextDO = LitePal.where("key = ?", BigTextDO.VIDEO_RULES_KEY).findFirst(BigTextDO.class);
        if (bigTextDO == null || StringUtil.isEmpty(bigTextDO.getValue())) {
            bigTextDO = new BigTextDO();
            bigTextDO.setKey(BigTextDO.VIDEO_RULES_KEY);
            videoRules.add(".*\\.mp4.*");
            bigTextDO.setValue(JSON.toJSONString(videoRules));
            bigTextDO.save();
        } else {
            List<String> rules = null;
            try {
                rules = JSON.parseArray(bigTextDO.getValue(), String.class);
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (CollectionUtil.isNotEmpty(rules)) {
                videoRules.addAll(new HashSet<>(rules));
            }
        }
    }

    //====================嗅探解析规则=================

    public static void addVideoRule(Context context, String rule) {
        if (StringUtil.isEmpty(rule)) {
            return;
        }
        Set<String> rules = new HashSet<>(videoRules);
        if (rules.contains(rule)) {
            ToastMgr.shortBottomCenter(context, "规则和已有规则重复");
            return;
        }
        videoRules.add(rule);
        saveVideoRules();
    }

    public static void removeVideoRule(String rule) {
        if (StringUtil.isEmpty(rule)) {
            return;
        }
        videoRules.remove(rule);
        saveVideoRules();
    }

    public static void updateVideoRule(String rule, String newRule) {
        if (StringUtil.isEmpty(rule)) {
            return;
        }
        videoRules.remove(rule);
        videoRules.add(newRule);
        saveVideoRules();
    }

    private static void saveVideoRules() {
        BigTextDO bigTextDO = LitePal.where("key = ?", BigTextDO.VIDEO_RULES_KEY).findFirst(BigTextDO.class);
        if (bigTextDO == null) {
            bigTextDO = new BigTextDO();
            bigTextDO.setKey(BigTextDO.VIDEO_RULES_KEY);
        }
        bigTextDO.setValue(JSON.toJSONString(videoRules));
        bigTextDO.save();
    }
}

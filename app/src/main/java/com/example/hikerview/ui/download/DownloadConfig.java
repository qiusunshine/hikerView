package com.example.hikerview.ui.download;

import android.content.Context;

import com.example.hikerview.model.DownloadRecord;
import com.example.hikerview.ui.browser.model.UrlDetector;
import com.example.hikerview.ui.download.util.HttpRequestUtil;
import com.example.hikerview.utils.PreferenceMgr;
import com.example.hikerview.utils.UriUtils;

import java.io.File;

/**
 * 作者：By 15968
 * 日期：On 2019/12/4
 * 时间：At 22:06
 */
public class DownloadConfig {
    public static String rootPath = "Download";
    public static int videoSnifferThreadNum = 5;
    public static int videoSnifferRetryCountOnFail = 1;
    public static int defaultMaxConcurrentTask = 2;
    public static int maxConcurrentTask = defaultMaxConcurrentTask;
    public static int m3U8DownloadThreadNum = 20;
    public static int m3U8DownloadSizeDetectRetryCountOnFail = 20;
    public static int downloadSubFileRetryCountOnFail = 50;
    public static int normalFileHeaderCheckRetryCountOnFail = 20;
    public static long normalFileSplitSize = -1;
    public static int normalFileDownloadThreadNum = 6;
    public static boolean autoMerge = true;
    public static boolean smartFilm = true;

    public static String defaultRootPath = "Download";

    public static void loadConfig(Context context) {
        defaultRootPath = UriUtils.getRootDir(context) + File.separator + "download";
//        rootPath = PreferenceMgr.getString(context, "download", "rootPath", defaultRootPath);
        rootPath = defaultRootPath;
        File rootDir = new File(rootPath);
        if (!rootDir.exists()) {
            rootDir.mkdir();
        }
        videoSnifferThreadNum = PreferenceMgr.getInt(context, "download", "videoSnifferThreadNum", videoSnifferThreadNum);
        videoSnifferRetryCountOnFail = PreferenceMgr.getInt(context, "download", "videoSnifferRetryCountOnFail", videoSnifferRetryCountOnFail);
        m3U8DownloadThreadNum = PreferenceMgr.getInt(context, "download", "m3U8DownloadThreadNum", m3U8DownloadThreadNum);
        m3U8DownloadSizeDetectRetryCountOnFail = PreferenceMgr.getInt(context, "download", "m3U8DownloadSizeDetectRetryCountOnFail", m3U8DownloadSizeDetectRetryCountOnFail);
        downloadSubFileRetryCountOnFail = PreferenceMgr.getInt(context, "download", "downloadSubFileRetryCountOnFail", downloadSubFileRetryCountOnFail);
        normalFileHeaderCheckRetryCountOnFail = PreferenceMgr.getInt(context, "download", "normalFileHeaderCheckRetryCountOnFail", normalFileHeaderCheckRetryCountOnFail);
        normalFileSplitSize = PreferenceMgr.getLong(context, "download", "normalFileSplitSize", normalFileSplitSize);
        normalFileDownloadThreadNum = PreferenceMgr.getInt(context, "download", "normalFileDownloadThreadNum", normalFileDownloadThreadNum);
        autoMerge = PreferenceMgr.getBoolean(context, "download", "autoMerge", autoMerge);
        smartFilm = PreferenceMgr.getBoolean(context, "download", "smartFilm", smartFilm);
        updateMaxTaskNum(context);
        boolean enableH2 = PreferenceMgr.getBoolean(context, "download", "enableH2", true);
        HttpRequestUtil.initClient(enableH2);
    }

    public static void updateMaxTaskNum(Context context) {
        int maxTask = PreferenceMgr.getInt(context, "download", "maxConcurrentTask", maxConcurrentTask);
        //保证总的线程数不超过128
        maxConcurrentTask = Math.min(maxTask, 128 / Math.max(m3U8DownloadThreadNum, normalFileDownloadThreadNum));
    }

    public static void setThreadNum(Context context, int m3U8Num, int normalNum) {
        m3U8DownloadThreadNum = m3U8Num;
        normalFileDownloadThreadNum = normalNum;
        PreferenceMgr.put(context, "download", "m3U8DownloadThreadNum", m3U8Num);
        PreferenceMgr.put(context, "download", "normalFileDownloadThreadNum", normalNum);
    }

    public static long getNormalFileSplitSize(DownloadTask downloadTask) {
        if (normalFileSplitSize > 0) {
            return normalFileSplitSize;
        }
        String ext = "." + downloadTask.getFileExtension();
        for (String v : UrlDetector.videos) {
            if (ext.contains(v)) {
                return 1024 * 1024;
            }
        }
        return 2 * 1024 * 1024;
    }

    public static long getNormalFileSplitSize(DownloadRecord downloadTask) {
        if (normalFileSplitSize > 0) {
            return normalFileSplitSize;
        }
        String ext = "." + downloadTask.getFileExtension();
        for (String v : UrlDetector.videos) {
            if (ext.contains(v)) {
                return 1024 * 1024;
            }
        }
        return 2 * 1024 * 1024;
    }
}

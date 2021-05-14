package com.example.hikerview.ui.download;

import android.content.Context;

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
    public static int maxConcurrentTask = 2;
    public static int m3U8DownloadThreadNum = 32;
    public static int m3U8DownloadSizeDetectRetryCountOnFail = 20;
    public static int downloadSubFileRetryCountOnFail = 50;
    public static int normalFileHeaderCheckRetryCountOnFail = 20;
    public static long normalFileSplitSize = 2000000;
    public static int normalFileDownloadThreadNum = 6;

    public static void loadConfig(Context context) {
        rootPath = PreferenceMgr.getString(context, "download", "rootPath", UriUtils.getRootDir(context) + File.separator + "download");
        File rootDir = new File(rootPath);
        if(!rootDir.exists()){
            rootDir.mkdir();
        }
        videoSnifferThreadNum = PreferenceMgr.getInt(context, "download", "videoSnifferThreadNum", videoSnifferThreadNum);
        videoSnifferRetryCountOnFail = PreferenceMgr.getInt(context, "download", "videoSnifferRetryCountOnFail", videoSnifferRetryCountOnFail);
        maxConcurrentTask = PreferenceMgr.getInt(context, "download", "maxConcurrentTask", maxConcurrentTask);
        m3U8DownloadThreadNum = PreferenceMgr.getInt(context, "download", "m3U8DownloadThreadNum", m3U8DownloadThreadNum);
        m3U8DownloadSizeDetectRetryCountOnFail = PreferenceMgr.getInt(context, "download", "m3U8DownloadSizeDetectRetryCountOnFail", m3U8DownloadSizeDetectRetryCountOnFail);
        downloadSubFileRetryCountOnFail = PreferenceMgr.getInt(context, "download", "downloadSubFileRetryCountOnFail", downloadSubFileRetryCountOnFail);
        normalFileHeaderCheckRetryCountOnFail = PreferenceMgr.getInt(context, "download", "normalFileHeaderCheckRetryCountOnFail", normalFileHeaderCheckRetryCountOnFail);
        normalFileSplitSize = PreferenceMgr.getLong(context, "download", "normalFileSplitSize", normalFileSplitSize);
        normalFileDownloadThreadNum = PreferenceMgr.getInt(context, "download", "normalFileDownloadThreadNum", normalFileDownloadThreadNum);
    }

    public static void setThreadNum(Context context, int m3U8Num, int normalNum){
        m3U8DownloadThreadNum = m3U8Num;
        normalFileDownloadThreadNum = normalNum;
        PreferenceMgr.put(context, "download", "m3U8DownloadThreadNum", m3U8Num);
        PreferenceMgr.put(context, "download", "normalFileDownloadThreadNum", normalNum);
    }
}

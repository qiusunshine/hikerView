package com.example.hikerview.ui.download.merge;

import android.os.Handler;
import android.os.Looper;
import android.os.Process;

import com.jeffmony.m3u8library.utils.LogUtils;

import java.util.concurrent.ThreadFactory;

/**
 * 作者：By 15968
 * 日期：On 2021/8/27
 * 时间：At 21:08
 */

public class VideoProcessThreadHandler {

    private static final String TAG = "VideoProcessThreadHandler";
    private static Handler sMainHandler;

    public VideoProcessThreadHandler() {
    }

    public static Handler getMainHandler() {
        return sMainHandler;
    }

    public static void runOnUiThread(Runnable r) {
        runOnUiThread(r, 0);
    }

    public static void runOnUiThread(Runnable r, int delayTime) {
        if (delayTime > 0) {
            sMainHandler.postDelayed(r, (long)delayTime);
        } else if (runningOnUiThread()) {
            r.run();
        } else {
            sMainHandler.post(r);
        }

    }

    private static boolean runningOnUiThread() {
        return sMainHandler.getLooper() == Looper.myLooper();
    }

    static {
        sMainHandler = new Handler(Looper.getMainLooper());
    }

    private static class MediaWorkerThread extends Thread {
        public MediaWorkerThread(Runnable r) {
            super(r, "video_download_worker_pool_thread");
        }

        public void run() {
            Process.setThreadPriority(10);
            long startTime = System.currentTimeMillis();
            super.run();
            long endTime = System.currentTimeMillis();
            LogUtils.i("VideoProcessThreadHandler", "MediaWorkerThread execution time: " + (endTime - startTime));
        }
    }

    private static class MediaWorkerThreadFactory implements ThreadFactory {
        private MediaWorkerThreadFactory() {
        }

        public Thread newThread(Runnable r) {
            return new VideoProcessThreadHandler.MediaWorkerThread(r);
        }
    }
} 
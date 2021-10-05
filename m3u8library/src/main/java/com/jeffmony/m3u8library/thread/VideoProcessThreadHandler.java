package com.jeffmony.m3u8library.thread;

import android.os.Handler;
import android.os.Looper;
import android.os.Process;

import com.jeffmony.m3u8library.utils.LogUtils;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class VideoProcessThreadHandler {

    private static final String TAG = "VideoProcessThreadHandler";

    private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();
    private static final int CORE_POOL_SIZE = CPU_COUNT + 1;
    private static final int MAXIMUM_POOL_SIZE = CPU_COUNT * 2 + 1;
    private static final int KEEP_ALIVE = 1;

    private static final BlockingQueue<Runnable> S_THREAD_POOL_WORK_QUEUE = new LinkedBlockingQueue<Runnable>();
    private static final ExecutorService S_THREAD_POOL_EXECUTOR = new ThreadPoolExecutor(CORE_POOL_SIZE, MAXIMUM_POOL_SIZE, KEEP_ALIVE,
            TimeUnit.SECONDS, S_THREAD_POOL_WORK_QUEUE, new MediaWorkerThreadFactory(), new ThreadPoolExecutor.DiscardOldestPolicy());

    //sdk中唯一的主线程handler
    private static Handler sMainHandler = new Handler(Looper.getMainLooper());

    public static Handler getMainHandler() {
        return sMainHandler;
    }

    public static void runOnUiThread(Runnable r) {
        runOnUiThread(r, 0);
    }
    public static void runOnUiThread(Runnable r, int delayTime) {
        if (delayTime > 0) {
            sMainHandler.postDelayed(r, delayTime);
        } else if (runningOnUiThread()) {
            r.run();
        } else {
            sMainHandler.post(r);
        }
    }

    private static boolean runningOnUiThread() {
        return sMainHandler.getLooper() == Looper.myLooper();
    }

    private static class MediaWorkerThreadFactory implements ThreadFactory {
        public Thread newThread(Runnable r) {
            return new MediaWorkerThread(r);
        }
    }

    private static class MediaWorkerThread extends Thread {
        public MediaWorkerThread(Runnable r) {
            super(r, "video_download_worker_pool_thread");
        }

        @Override
        public void run() {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
            long startTime = System.currentTimeMillis();
            super.run();
            long endTime = System.currentTimeMillis();
            LogUtils.i(TAG, "MediaWorkerThread execution time: " + (endTime - startTime));
        }
    }

    public static Future submitCallbackTask(Callable task) {
        return S_THREAD_POOL_EXECUTOR.submit(task);
    }

    public static Future submitRunnableTask(Runnable task) {
        return S_THREAD_POOL_EXECUTOR.submit(task);
    }
}

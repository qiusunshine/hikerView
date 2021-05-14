package com.example.hikerview.ui.download.util;

import android.util.Log;

import java.util.List;

/**
 * Created by xm on 17-8-21.
 */
public class ThreadUtil {
    private static final String TAG = "ThreadUtil";

    public static void interruptThread(Thread thread) {
        try {
            Log.d(TAG, "interruptThread: " + thread.getName());
            thread.interrupt();
            Log.d(TAG, "interruptThreaded: " + thread.getName());
        } catch (Exception e) {
            Log.d(TAG, "interruptThreadList: " + e.getMessage());
        }
    }


    public static void interruptThreadList(List<Thread> threadList) {
        for (int i = 0; i < threadList.size(); i++) {
            try {
                Log.d(TAG, "interruptThread: " + threadList.get(i).getName());
                threadList.get(i).interrupt();
                Log.d(TAG, "interruptThreaded: " + threadList.get(i).getName());
            } catch (Exception e) {
                Log.d(TAG, "interruptThreadList: " + e.getMessage());
            }
        }

    }


    public static void stopThreadList(List<Thread> threadList) {
        interruptThreadList(threadList);
    }
}

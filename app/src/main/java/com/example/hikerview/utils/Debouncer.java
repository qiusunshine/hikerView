package com.example.hikerview.utils;


import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * 操作防抖
 */
public class Debouncer<V> {
    private final static long INTERNAL_TIME = 300;
    private long lastDo = 0;
    private ScheduledFuture<V> future;

    public synchronized void debounce(Callable<V> callable) {
        debounce(callable, INTERNAL_TIME);
    }

    public synchronized void debounce(Callable<V> callable, long internalTime) {
        if (future != null) {
            future.cancel(false);
        }
        try {
            future = HeavyTaskUtil.getScheduledExecutorService().schedule(callable, internalTime, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

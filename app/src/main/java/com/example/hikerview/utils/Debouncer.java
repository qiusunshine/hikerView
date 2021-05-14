package com.example.hikerview.utils;


import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledFuture;

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

    }
}

package com.paincker.timetracer.tracer;

import android.os.Handler;
import android.os.Looper;

public class TimeTracer {

    private static final Handler HANDLER = new Handler(Looper.getMainLooper());
    private static ITracer TRACER = ITracer.EMPTY_TRACER;

    public static ITracer getTracer() {
        return TRACER;
    }

    public static void setTracer(ITracer tracer) {
        TRACER = tracer == null ? ITracer.EMPTY_TRACER : tracer;
    }

    /**
     * 开启追踪，定时结束
     */
    public static void startTracing(long ms, final Runnable onCompleteRunnable) {
        if (Looper.getMainLooper() == Looper.myLooper()) {
            startTracing();
        } else {
            HANDLER.post(new Runnable() {
                @Override
                public void run() {
                    startTracing();
                }
            });
        }
        HANDLER.postDelayed(new Runnable() {
            @Override
            public void run() {
                stopTracing();
                if (onCompleteRunnable != null) {
                    onCompleteRunnable.run();
                }
            }
        }, ms);
    }

    public static void startTracing() {
        TRACER.traceStart();
    }

    public static void stopTracing() {
        TRACER.traceEnd();
    }

    public static void methodStart(String method) {
        TRACER.methodStart(method);
    }

    public static void methodEnd(String method) {
        TRACER.methodEnd(method);
    }
}
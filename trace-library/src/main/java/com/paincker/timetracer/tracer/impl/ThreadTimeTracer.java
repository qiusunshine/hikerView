package com.paincker.timetracer.tracer.impl;

import android.os.Looper;
import android.os.SystemClock;

public class ThreadTimeTracer extends BaseTimeTracer {

    private final Looper mLooper;

    public ThreadTimeTracer(Looper looper, int maxLevel) {
        super(maxLevel);
        mLooper = looper;
    }

    public ThreadTimeTracer(Looper looper) {
        super();
        mLooper = looper;
    }

    @Override
    protected boolean checkMatchStart(String method) {
        return Looper.myLooper() == mLooper;
    }

    @Override
    protected boolean checkMatchEnd(String method) {
        return Looper.myLooper() == mLooper;
    }

    protected long timestamp() {
        return SystemClock.currentThreadTimeMillis();
    }
}
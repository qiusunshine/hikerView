package com.paincker.timetracer.tracer.impl;

import android.os.Looper;

public class MainThreadTracer extends ThreadTimeTracer {

    public static final MainThreadTracer INSTANCE = new MainThreadTracer();

    public MainThreadTracer(int maxLevel) {
        super(Looper.getMainLooper(), maxLevel);
    }

    public MainThreadTracer() {
        super(Looper.getMainLooper());
    }
}
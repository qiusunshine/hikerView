package com.example.hikerview.ui.thunder;

import android.content.Context;

/**
 * 作者：By 15968
 * 日期：On 2022/9/20
 * 时间：At 19:59
 */

public abstract class TorrentEngine {
    public ContextProvider getContextProvider() {
        return contextProvider;
    }

    public void setContextProvider(ContextProvider contextProvider) {
        this.contextProvider = contextProvider;
    }

    protected ContextProvider contextProvider;

    public abstract void initConfig();

    public abstract void initEngine();

    public abstract boolean isDownloading();

    public abstract boolean isDownloadFinished();

    public abstract boolean parse(String url);

    public abstract void stopTask();

    public abstract void release();

    public abstract String getProgress();

    public interface ContextProvider {
        Context get();
    }
}

package com.example.hikerview.ui.download;

import com.king.app.updater.AppUpdater;
import com.king.app.updater.callback.UpdateCallback;

import java.io.File;

/**
 * 作者：By 15968
 * 日期：On 2022/4/20
 * 时间：At 14:01
 */

public abstract class AppDownloadCallback implements UpdateCallback {

    private AppUpdater appUpdater;

    public AppDownloadCallback(AppUpdater appUpdater) {
        this.appUpdater = appUpdater;
    }

    @Override
    public void onDownloading(boolean isDownloading) {

    }

    @Override
    public void onStart(String url) {

    }

    @Override
    public void onProgress(long progress, long total, boolean isChange) {
        appUpdater.setProgress(progress);
        appUpdater.setTotal(total);
    }

    @Override
    public void onFinish(File file) {
        complete(appUpdater);
    }

    @Override
    public void onError(Exception e) {
        complete(appUpdater);
    }

    @Override
    public void onCancel() {
        complete(appUpdater);
    }

    public abstract void complete(AppUpdater appUpdater);
}
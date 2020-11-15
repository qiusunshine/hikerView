package com.example.hikerview.event.web;

import android.net.Uri;
import android.webkit.ValueCallback;

/**
 * 作者：By 15968
 * 日期：On 2020/4/5
 * 时间：At 13:55
 */
public class OnShowFileChooserEvent {
    private ValueCallback<Uri[]> filePathCallback;

    public OnShowFileChooserEvent(ValueCallback<Uri[]> filePathCallback) {
        this.filePathCallback = filePathCallback;
    }

    public ValueCallback<Uri[]> getFilePathCallback() {
        return filePathCallback;
    }

    public void setFilePathCallback(ValueCallback<Uri[]> filePathCallback) {
        this.filePathCallback = filePathCallback;
    }
}

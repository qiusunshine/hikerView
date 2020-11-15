package com.example.hikerview.event.web;

import android.view.View;
import android.webkit.WebChromeClient;

/**
 * 作者：By 15968
 * 日期：On 2020/4/5
 * 时间：At 13:55
 */
public class OnShowCustomViewEvent {
    private View view;
    private WebChromeClient.CustomViewCallback callback;

    public OnShowCustomViewEvent(View view, WebChromeClient.CustomViewCallback callback) {
        this.view = view;
        this.callback = callback;
    }

    public View getView() {
        return view;
    }

    public void setView(View view) {
        this.view = view;
    }

    public WebChromeClient.CustomViewCallback getCallback() {
        return callback;
    }

    public void setCallback(WebChromeClient.CustomViewCallback callback) {
        this.callback = callback;
    }
}

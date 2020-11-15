package com.example.hikerview.event.web;

import android.webkit.WebView;

/**
 * 作者：By 15968
 * 日期：On 2020/4/5
 * 时间：At 14:10
 */
public class OnLongClickEvent {
    private WebView.HitTestResult result;

    public OnLongClickEvent(WebView.HitTestResult result) {
        this.result = result;
    }

    public WebView.HitTestResult getResult() {
        return result;
    }

    public void setResult(WebView.HitTestResult result) {
        this.result = result;
    }
}

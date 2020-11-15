package com.example.hikerview.event.web;

/**
 * 作者：By 15968
 * 日期：On 2020/4/5
 * 时间：At 13:29
 */
public class OnPageStartEvent {
    public OnPageStartEvent(String url) {
        this.url = url;
    }

    private String url;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}

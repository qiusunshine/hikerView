package com.example.hikerview.event.home;

/**
 * 作者：By 15968
 * 日期：On 2019/10/2
 * 时间：At 13:46
 */
public class OnRefreshWebViewEvent {
    public OnRefreshWebViewEvent(String url) {
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

package com.example.hikerview.event;

/**
 * 作者：By 15968
 * 日期：On 2020/2/12
 * 时间：At 12:46
 */
public class WebViewUrlChangedEvent {

    private String url;

    public WebViewUrlChangedEvent(String url) {
        this.url = url;
    }

    public WebViewUrlChangedEvent() {
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}

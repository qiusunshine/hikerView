package com.example.hikerview.event;

/**
 * 作者：By 15968
 * 日期：On 2019/12/13
 * 时间：At 8:11
 */
public class OnUrlChangeEvent {
    private String url;

    public OnUrlChangeEvent(String url) {
        this.url = url;
    }

    public OnUrlChangeEvent() {
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}

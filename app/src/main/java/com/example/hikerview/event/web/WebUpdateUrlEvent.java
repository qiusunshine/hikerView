package com.example.hikerview.event.web;

/**
 * 作者：By 15968
 * 日期：On 2020/2/12
 * 时间：At 12:46
 */
public class WebUpdateUrlEvent {

    private String url;

    public WebUpdateUrlEvent(String url, boolean newWindow) {
        this.url = url;
        this.newWindow = newWindow;
    }

    private boolean newWindow;

    public WebUpdateUrlEvent(String url) {
        this.url = url;
    }

    public WebUpdateUrlEvent() {
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public boolean isNewWindow() {
        return newWindow;
    }

    public void setNewWindow(boolean newWindow) {
        this.newWindow = newWindow;
    }
}

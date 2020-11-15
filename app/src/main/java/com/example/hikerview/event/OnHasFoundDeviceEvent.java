package com.example.hikerview.event;

/**
 * 作者：By hdy
 * 日期：On 2019/3/30
 * 时间：At 23:51
 */
public class OnHasFoundDeviceEvent {
    private String url;
    private String name;

    public OnHasFoundDeviceEvent(String url, String name) {
        this.url = url;
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}

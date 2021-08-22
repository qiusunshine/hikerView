package com.example.hikerview.ui.webdlan.model;

import java.util.Map;

/**
 * 作者：By 15968
 * 日期：On 2021/1/4
 * 时间：At 20:27
 */

public class DlanUrlDTO {
    private String title;
    private String url;
    private Map<String, String> headers;
    private int jumpStartDuration, jumpEndDuration;

    public DlanUrlDTO(String url, Map<String, String> headers, int jumpStartDuration, int jumpEndDuration) {
        this.url = url;
        this.headers = headers;
        this.jumpStartDuration = jumpStartDuration;
        this.jumpEndDuration = jumpEndDuration;
    }

    public int getJumpEndDuration() {
        return jumpEndDuration;
    }

    public void setJumpEndDuration(int jumpEndDuration) {
        this.jumpEndDuration = jumpEndDuration;
    }

    public int getJumpStartDuration() {
        return jumpStartDuration;
    }

    public void setJumpStartDuration(int jumpStartDuration) {
        this.jumpStartDuration = jumpStartDuration;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}

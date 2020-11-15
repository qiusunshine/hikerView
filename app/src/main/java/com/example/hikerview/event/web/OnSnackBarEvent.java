package com.example.hikerview.event.web;

/**
 * 作者：By 15968
 * 日期：On 2020/8/7
 * 时间：At 22:41
 */

public class OnSnackBarEvent {
    private String title;

    public OnSnackBarEvent(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}

package com.example.hikerview.event;

/**
 * 作者：By 15968
 * 日期：On 2019/12/4
 * 时间：At 22:24
 */
public class ShowToastMessageEvent {
    private String title;

    public ShowToastMessageEvent(String title) {
        this.title = title;
    }

    public ShowToastMessageEvent() {
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}

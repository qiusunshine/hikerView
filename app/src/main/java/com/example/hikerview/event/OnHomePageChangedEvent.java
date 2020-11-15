package com.example.hikerview.event;

/**
 * 作者：By 15968
 * 日期：On 2019/10/5
 * 时间：At 16:08
 */
public class OnHomePageChangedEvent {

    private String title;

    public OnHomePageChangedEvent() {
    }

    public OnHomePageChangedEvent(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}

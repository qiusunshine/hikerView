package com.example.hikerview.event;

/**
 * 作者：By 15968
 * 日期：On 2020/4/18
 * 时间：At 20:24
 */
public class ChangeHomePageAndFinishEvent {
    private String title;

    public ChangeHomePageAndFinishEvent(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}

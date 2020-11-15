package com.example.hikerview.event.home;

/**
 * 作者：By 15968
 * 日期：On 2020/5/16
 * 时间：At 18:25
 */
public class LastClickShowEvent {
    public LastClickShowEvent(String title) {
        this.title = title;
    }

    private String title;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}

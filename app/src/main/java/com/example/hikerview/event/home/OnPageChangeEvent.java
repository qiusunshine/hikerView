package com.example.hikerview.event.home;

/**
 * 作者：By 15968
 * 日期：On 2021/2/20
 * 时间：At 19:56
 */

public class OnPageChangeEvent {

    public OnPageChangeEvent(String title) {
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

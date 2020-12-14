package com.example.hikerview.event.web;

/**
 * 作者：By 15968
 * 日期：On 2020/11/28
 * 时间：At 16:47
 */

public class ShowSearchEvent {

    private String text;

    public ShowSearchEvent() {
    }

    public ShowSearchEvent(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}

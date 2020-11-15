package com.example.hikerview.event;

/**
 * 作者：By 15968
 * 日期：On 2019/10/5
 * 时间：At 16:08
 */
public class OnPageChangedEvent {

    private String position;

    public OnPageChangedEvent() {
    }

    public OnPageChangedEvent(String position) {
        this.position = position;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }
}

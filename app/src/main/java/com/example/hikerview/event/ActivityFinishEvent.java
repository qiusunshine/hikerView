package com.example.hikerview.event;

/**
 * 作者：By 15968
 * 日期：On 2020/4/18
 * 时间：At 20:24
 */
public class ActivityFinishEvent {
    private String clazz;

    public ActivityFinishEvent(String clazz) {
        this.clazz = clazz;
    }

    public String getClazz() {
        return clazz;
    }

    public void setClazz(String clazz) {
        this.clazz = clazz;
    }
}

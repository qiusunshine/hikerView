package com.example.hikerview.event.web;

/**
 * 作者：By 15968
 * 日期：On 2020/4/5
 * 时间：At 13:42
 */
public class OnProgressChangedEvent {
    private int progress;

    public OnProgressChangedEvent(int progress) {
        this.progress = progress;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }
}

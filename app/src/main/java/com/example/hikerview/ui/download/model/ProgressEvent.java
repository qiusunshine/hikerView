package com.example.hikerview.ui.download.model;

/**
 * 作者：By 15968
 * 日期：On 2022/10/9
 * 时间：At 16:28
 */

public class ProgressEvent {

    private String name;
    private String progress;

    public ProgressEvent(String name, String progress) {
        this.progress = progress;
        this.name = name;
    }

    public String getProgress() {
        return progress;
    }

    public void setProgress(String progress) {
        this.progress = progress;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
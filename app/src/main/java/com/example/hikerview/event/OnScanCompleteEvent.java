package com.example.hikerview.event;

/**
 * 作者：By hdy
 * 日期：On 2019/3/30
 * 时间：At 23:51
 */
public class OnScanCompleteEvent {
    public OnScanCompleteEvent(String process) {
        this.process = process;
    }

    private String process;

    public String getProcess() {
        return process;
    }

    public void setProcess(String process) {
        this.process = process;
    }
}

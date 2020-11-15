package com.example.hikerview.event;

/**
 * 作者：By 15968
 * 日期：On 2020/2/11
 * 时间：At 17:21
 */
public class LoadingDismissEvent {
    public LoadingDismissEvent() {
    }
    public LoadingDismissEvent(String msg) {
        this.msg = msg;
    }

    private String msg;

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }
}

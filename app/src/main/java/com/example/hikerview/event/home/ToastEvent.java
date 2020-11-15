package com.example.hikerview.event.home;

/**
 * 作者：By 15968
 * 日期：On 2020/5/22
 * 时间：At 20:35
 */
public class ToastEvent {
    private String msg;

    public ToastEvent(String msg) {
        this.msg = msg;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }
}

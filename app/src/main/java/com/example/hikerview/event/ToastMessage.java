package com.example.hikerview.event;

/**
 * 作者：By 15968
 * 日期：On 2020/3/27
 * 时间：At 23:44
 */
public class ToastMessage {
    public ToastMessage(String msg) {
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

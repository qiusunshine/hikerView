package com.example.hikerview.event.web;

/**
 * 作者：By 15968
 * 日期：On 2020/4/5
 * 时间：At 20:56
 */
public class OnEvalJsEvent {
    private String js;

    public OnEvalJsEvent(String js) {
        this.js = js;
    }

    public String getJs() {
        return js;
    }

    public void setJs(String js) {
        this.js = js;
    }
}

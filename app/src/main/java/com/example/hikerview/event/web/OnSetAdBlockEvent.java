package com.example.hikerview.event.web;

/**
 * 作者：By 15968
 * 日期：On 2020/4/5
 * 时间：At 14:44
 */
public class OnSetAdBlockEvent {
    private String html;
    private String rule;

    public OnSetAdBlockEvent(String html, String rule) {
        this.html = html;
        this.rule = rule;
    }

    public String getHtml() {
        return html;
    }

    public void setHtml(String html) {
        this.html = html;
    }

    public String getRule() {
        return rule;
    }

    public void setRule(String rule) {
        this.rule = rule;
    }
}

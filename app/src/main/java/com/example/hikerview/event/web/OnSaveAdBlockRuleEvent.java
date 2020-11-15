package com.example.hikerview.event.web;

/**
 * 作者：By 15968
 * 日期：On 2020/4/5
 * 时间：At 14:10
 */
public class OnSaveAdBlockRuleEvent {
    private String rule;

    public OnSaveAdBlockRuleEvent(String rule) {
        this.rule = rule;
    }

    public String getRule() {
        return rule;
    }

    public void setRule(String rule) {
        this.rule = rule;
    }
}

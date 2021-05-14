package com.example.hikerview.ui.js.model;

/**
 * 作者：By 15968
 * 日期：On 2020/4/12
 * 时间：At 16:21
 */
public class JsRule implements Comparable<JsRule> {
    private String name;
    private String rule;
    private boolean enable;
    private int order;

    public boolean isEnable() {
        return enable;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }

    public String getRule() {
        return rule;
    }

    public void setRule(String rule) {
        this.rule = rule;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    @Override
    public int compareTo(JsRule jsRule) {
        int o = this.getOrder() - jsRule.getOrder();
        if (o == 0) {
            return this.getName().compareTo(jsRule.getName());
        } else {
            return o;
        }
    }
}

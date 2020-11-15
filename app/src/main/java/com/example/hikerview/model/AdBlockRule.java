package com.example.hikerview.model;

import org.litepal.crud.LitePalSupport;

/**
 * 作者：By 15968
 * 日期：On 2019/10/14
 * 时间：At 19:18
 */
public class AdBlockRule extends LitePalSupport {
    private String dom;
    private String rule;

    public String getRule() {
        return rule;
    }

    public void setRule(String rule) {
        this.rule = rule;
    }

    public String getDom() {
        return dom;
    }

    public void setDom(String dom) {
        this.dom = dom;
    }

    public long getId(){
        return getBaseObjId();
    }
}

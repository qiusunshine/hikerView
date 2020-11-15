package com.example.hikerview.model;

import org.litepal.crud.LitePalSupport;

/**
 * 作者：By 15968
 * 日期：On 2019/12/1
 * 时间：At 11:32
 */
public class UaRuleDO extends LitePalSupport {
    public String getDom() {
        return dom;
    }

    public void setDom(String dom) {
        this.dom = dom;
    }

    public String getUa() {
        return ua;
    }

    public void setUa(String ua) {
        this.ua = ua;
    }

    private String dom;
    private String ua;
}

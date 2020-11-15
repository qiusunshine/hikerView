package com.example.hikerview.model;

import org.litepal.crud.LitePalSupport;

/**
 * 作者：By hdy
 * 日期：On 2018/12/11
 * 时间：At 20:40
 */
public class XiuTanFavor extends LitePalSupport {
    private String dom;
    private String url;

    public XiuTanFavor(String dom, String url) {
        this.dom = dom;
        this.url = url;
    }

    public XiuTanFavor() {
    }

    public String getDom() {
        return dom;
    }

    public void setDom(String dom) {
        this.dom = dom;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}

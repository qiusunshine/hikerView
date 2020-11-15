package com.example.hikerview.model;

import org.litepal.crud.LitePalSupport;

/**
 * 作者：By 15968
 * 日期：On 2019/12/3
 * 时间：At 18:40
 */
public class SharedAdUrl extends LitePalSupport {
    private String dom;
    private String blockUrls;

    public String getBlockUrls() {
        return blockUrls;
    }

    public void setBlockUrls(String blockUrls) {
        this.blockUrls = blockUrls;
    }

    public String getDom() {
        return dom;
    }

    public void setDom(String dom) {
        this.dom = dom;
    }
}

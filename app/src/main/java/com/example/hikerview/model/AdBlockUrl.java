package com.example.hikerview.model;

import org.litepal.crud.LitePalSupport;

/**
 * 作者：By 15968
 * 日期：On 2019/10/14
 * 时间：At 19:18
 */
public class AdBlockUrl extends LitePalSupport {
    private String url;

    public AdBlockUrl(String url) {
        this.url = url;
    }

    public AdBlockUrl() {
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public String toString() {
        return url;
    }

    @Override
    public int hashCode() {
        return url.hashCode();
    }

    public long getId(){
        return getBaseObjId();
    }
}

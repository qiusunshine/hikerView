package com.example.hikerview.model;

import org.litepal.crud.LitePalSupport;

/**
 * 作者：By 15968
 * 日期：On 2020/1/3
 * 时间：At 21:37
 */
public class BigTextDO extends LitePalSupport {
    private String key;
    private String value;

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }
}

package com.example.hikerview.event.web;

/**
 * 作者：By 15968
 * 日期：On 2020/4/5
 * 时间：At 14:10
 */
public class OnFindInfoEvent {
    private String searchInfo;

    public OnFindInfoEvent(String searchInfo) {
        this.searchInfo = searchInfo;
    }

    public String getSearchInfo() {
        return searchInfo;
    }

    public void setSearchInfo(String searchInfo) {
        this.searchInfo = searchInfo;
    }
}

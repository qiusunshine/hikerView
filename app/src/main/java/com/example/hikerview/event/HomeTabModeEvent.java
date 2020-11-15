package com.example.hikerview.event;

/**
 * 作者：By 15968
 * 日期：On 2020/3/31
 * 时间：At 21:09
 */
public class HomeTabModeEvent {
    private String tab;

    public HomeTabModeEvent(String tab) {
        this.tab = tab;
    }

    public String getTab() {
        return tab;
    }

    public void setTab(String tab) {
        this.tab = tab;
    }
}

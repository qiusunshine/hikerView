package com.example.hikerview.event.home;

/**
 * 作者：By 15968
 * 日期：On 2019/10/2
 * 时间：At 13:46
 */
public class OnRefreshX5HeightEvent {
    public OnRefreshX5HeightEvent(String desc) {
        this.desc = desc;
    }

    private String desc;

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }
}

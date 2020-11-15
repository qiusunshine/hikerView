package com.example.hikerview.event;

/**
 * 作者：By 15968
 * 日期：On 2020/2/26
 * 时间：At 19:53
 */
public class SearchVisibleChangeEvent {
    private boolean visible;

    public SearchVisibleChangeEvent(boolean visible) {
        this.visible = visible;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }
}

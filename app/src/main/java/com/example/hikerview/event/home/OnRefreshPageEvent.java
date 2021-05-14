package com.example.hikerview.event.home;

/**
 * 作者：By 15968
 * 日期：On 2019/10/2
 * 时间：At 13:46
 */
public class OnRefreshPageEvent {

    private boolean scrollTop;

    public OnRefreshPageEvent(boolean scrollTop) {
        this.scrollTop = scrollTop;
    }

    public boolean isScrollTop() {
        return scrollTop;
    }
}

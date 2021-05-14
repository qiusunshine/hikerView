package com.example.hikerview.event;

/**
 * @author reborn
 * @program hiker-view
 * @description 返回上一页的 Event
 * @create 2021-02-24 08:57
 **/
public class OnBackEvent {

    private boolean refreshPage;

    private boolean scrollTop;

    public OnBackEvent(boolean refreshPage, boolean scrollTop) {
        this.refreshPage = refreshPage;
        this.scrollTop = scrollTop;
    }

    public boolean isRefreshPage() {
        return refreshPage;
    }

    public boolean isScrollTop() {
        return scrollTop;
    }
}

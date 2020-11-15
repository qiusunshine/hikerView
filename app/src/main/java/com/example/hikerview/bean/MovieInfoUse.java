package com.example.hikerview.bean;

/**
 * 作者：By 15968
 * 日期：On 2019/12/8
 * 时间：At 23:15
 */
public class MovieInfoUse {
    private String Title;
    private String SearchUrl;
    private String SearchFind;

    public String getSearchUrl() {
        return SearchUrl;
    }

    public void setSearchUrl(String searchUrl) {
        SearchUrl = searchUrl;
    }

    public String getTitle() {
        return Title;
    }

    public void setTitle(String title) {
        Title = title;
    }

    public String getSearchFind() {
        return SearchFind;
    }

    public void setSearchFind(String searchFind) {
        SearchFind = searchFind;
    }
}

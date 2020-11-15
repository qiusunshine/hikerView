package com.example.hikerview.model;

import org.litepal.crud.LitePalSupport;

import java.io.Serializable;

/**
 * 作者：By hdy
 * 日期：On 2018/1/18
 * 时间：At 15:14
 */

public class MovieRuleDO extends LitePalSupport implements Serializable{
    private Long id;
    private String Title;
    private String BaseUrl;
    private String SearchUrl;
    private String SearchFind;
    private String ChapterFind;
    private String MovieFind;

    public String getSearchUrl() {
        return SearchUrl;
    }

    public void setSearchUrl(String searchUrl) {
        SearchUrl = searchUrl;
    }

    public String getSearchFind() {
        return SearchFind;
    }

    public void setSearchFind(String searchFind) {
        SearchFind = searchFind;
    }

    public String getChapterFind() {
        return ChapterFind;
    }

    public void setChapterFind(String chapterFind) {
        ChapterFind = chapterFind;
    }

    public String getMovieFind() {
        return MovieFind;
    }

    public void setMovieFind(String movieFind) {
        MovieFind = movieFind;
    }

    public String getTitle() {
        return Title;
    }

    public void setTitle(String title) {
        Title = title;
    }

    public String getBaseUrl() {
        return BaseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        BaseUrl = baseUrl;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}

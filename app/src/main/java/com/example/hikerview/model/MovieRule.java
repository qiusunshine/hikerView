package com.example.hikerview.model;

import androidx.annotation.NonNull;

import java.io.Serializable;

/**
 * 作者：By hdy
 * 日期：On 2018/1/18
 * 时间：At 15:14
 */

public class MovieRule implements Serializable, Comparable<MovieRule> {
    private Long id;
    private String pinYinTitle = "";
    private String Title;
    private String BaseUrl;
    private String SearchUrl;
    private String SearchFind;
    private String ChapterUrl;
    private String ChapterFind;
    private String MovieUrl;
    private String MovieFind;
    private int weight = 0;

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

    public String getChapterUrl() {
        return ChapterUrl;
    }

    public void setChapterUrl(String chapterUrl) {
        ChapterUrl = chapterUrl;
    }

    public String getChapterFind() {
        return ChapterFind;
    }

    public void setChapterFind(String chapterFind) {
        ChapterFind = chapterFind;
    }

    public String getMovieUrl() {
        return MovieUrl;
    }

    public void setMovieUrl(String movieUrl) {
        MovieUrl = movieUrl;
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

    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

    @Override
    public int compareTo(@NonNull MovieRule o) {
        return this.getWeight() - o.getWeight();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPinYinTitle() {
        return pinYinTitle;
    }

    public void setPinYinTitle(String pinYinTitle) {
        this.pinYinTitle = pinYinTitle;
    }
}

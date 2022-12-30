package com.example.hikerview.ui.home.reader;

/**
 * 作者：By 15968
 * 日期：On 2021/9/10
 * 时间：At 23:13
 */

public class ReadPageData {
    private int size;
    private int preSize;
    private int pageNow;
    private String chapter;

    public int getPreSize() {
        return preSize;
    }

    public void setPreSize(int preSize) {
        this.preSize = preSize;
    }

    public int getPageNow() {
        return pageNow;
    }

    public void setPageNow(int pageNow) {
        this.pageNow = pageNow;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public String getChapter() {
        return chapter;
    }

    public void setChapter(String chapter) {
        this.chapter = chapter;
    }
}
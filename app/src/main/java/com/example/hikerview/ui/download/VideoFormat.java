package com.example.hikerview.ui.download;

import java.util.List;

/**
 * Created by xm on 17-8-16.
 */
public class VideoFormat {
    private String name;
    private List<String> mimeList;

    public VideoFormat(String name, List<String> mimeList) {
        this.name = name;
        this.mimeList = mimeList;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getMimeList() {
        return mimeList;
    }

    public void setMimeList(List<String> mimeList) {
        this.mimeList = mimeList;
    }
}

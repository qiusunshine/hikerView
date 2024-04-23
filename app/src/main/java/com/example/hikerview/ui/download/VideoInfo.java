package com.example.hikerview.ui.download;

import java.util.Objects;

/**
 * Created by xm on 17-8-17.
 */
public class VideoInfo {
    private String fileName;
    private String url;
    private VideoFormat videoFormat;
    private long size;//单位byte m3u8不显示
    private double duration;//单位s m3u8专用
    private String sourcePageUrl;//原网页url
    private String sourcePageTitle;//原网页标题
    private String detectImageType = "";
    private String contentType;

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public VideoFormat getVideoFormat() {
        return videoFormat;
    }

    public void setVideoFormat(VideoFormat videoFormat) {
        this.videoFormat = videoFormat;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public double getDuration() {
        return duration;
    }

    public void setDuration(double duration) {
        this.duration = duration;
    }

    public String getSourcePageUrl() {
        return sourcePageUrl;
    }

    public void setSourcePageUrl(String sourcePageUrl) {
        this.sourcePageUrl = sourcePageUrl;
    }

    public String getSourcePageTitle() {
        return sourcePageTitle;
    }

    public void setSourcePageTitle(String sourcePageTitle) {
        this.sourcePageTitle = sourcePageTitle;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VideoInfo info = (VideoInfo) o;
        return Objects.equals(sourcePageUrl, info.sourcePageUrl) &&
                Objects.equals(sourcePageTitle, info.sourcePageTitle);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sourcePageUrl, sourcePageTitle);
    }

    public String getDetectImageType() {
        return detectImageType;
    }

    public void setDetectImageType(String detectImageType) {
        this.detectImageType = detectImageType;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }
}

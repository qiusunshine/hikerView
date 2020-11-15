package com.example.hikerview.model;

import org.litepal.crud.LitePalSupport;

import java.util.Date;

/**
 * 作者：By 15968
 * 日期：On 2019/10/20
 * 时间：At 12:04
 */
public class ViewHistory extends LitePalSupport implements Comparable<ViewHistory> {
    private String type;
    private String ruleBaseUrl;
    private String url;
    private String title;
    private String group;
    private String picUrl;
    private Date time;
    private String videoUrl;
    private String params;
    private String lastClick;

    public String getRuleBaseUrl() {
        return ruleBaseUrl;
    }

    public void setRuleBaseUrl(String ruleBaseUrl) {
        this.ruleBaseUrl = ruleBaseUrl;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Date getTime() {
        return time;
    }

    public void setTime(Date time) {
        this.time = time;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public int compareTo(ViewHistory o) {
        if (o == null) {
            return 0;
        }
        return o.getTime().compareTo(this.getTime());
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
    }

    public String getParams() {
        return params;
    }

    public void setParams(String params) {
        this.params = params;
    }

    public String getLastClick() {
        return lastClick;
    }

    public void setLastClick(String lastClick) {
        this.lastClick = lastClick;
    }

    public long getId(){
        return getBaseObjId();
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getPicUrl() {
        return picUrl;
    }

    public void setPicUrl(String picUrl) {
        this.picUrl = picUrl;
    }
}

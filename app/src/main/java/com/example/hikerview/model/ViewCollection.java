package com.example.hikerview.model;

import org.litepal.crud.LitePalSupport;

import java.util.Date;

/**
 * 作者：By hdy
 * 日期：On 2018/10/6
 * 时间：At 13:14
 */
public class ViewCollection extends LitePalSupport implements Comparable<ViewCollection> {
    private String MITitle;
    private String CUrl;
    private String MTitle;
    private String desc;
    private Date time;
    private String videoUrl;
    private String params;
    private String lastClick;
    private String group;
    private String picUrl;
    private String extraData;

    public String getMITitle() {
        return MITitle;
    }

    public void setMITitle(String MITitle) {
        this.MITitle = MITitle;
    }

    public String getCUrl() {
        return CUrl;
    }

    public void setCUrl(String CUrl) {
        this.CUrl = CUrl;
    }

    public String getMTitle() {
        return MTitle;
    }

    public void setMTitle(String MTitle) {
        this.MTitle = MTitle;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public Date getTime() {
        return time;
    }

    public void setTime(Date time) {
        this.time = time;
    }

    @Override
    public boolean save() {
        this.setTime(new Date());
        return super.save();
    }

    @Override
    public int compareTo(ViewCollection o) {
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

    public String getExtraData() {
        return extraData;
    }

    public void setExtraData(String extraData) {
        this.extraData = extraData;
    }
}

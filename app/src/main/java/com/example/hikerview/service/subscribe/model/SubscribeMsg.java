package com.example.hikerview.service.subscribe.model;

/**
 * 作者：By 15968
 * 日期：On 2019/10/21
 * 时间：At 19:25
 */
public class SubscribeMsg {
    private String title;
    private String url;
    private String time;
    private int version;
    private int domBlockRuleVersion;
    private String desc;
    private String urlV2;
    private String domBlockRuleUrl;

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

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public String getUrlV2() {
        return urlV2;
    }

    public void setUrlV2(String urlV2) {
        this.urlV2 = urlV2;
    }

    public String getDomBlockRuleUrl() {
        return domBlockRuleUrl;
    }

    public void setDomBlockRuleUrl(String domBlockRuleUrl) {
        this.domBlockRuleUrl = domBlockRuleUrl;
    }

    public int getDomBlockRuleVersion() {
        return domBlockRuleVersion;
    }

    public void setDomBlockRuleVersion(int domBlockRuleVersion) {
        this.domBlockRuleVersion = domBlockRuleVersion;
    }
}

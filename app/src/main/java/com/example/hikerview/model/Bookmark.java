package com.example.hikerview.model;

import androidx.annotation.NonNull;

import com.example.hikerview.utils.StringUtil;

import org.litepal.crud.LitePalSupport;

/**
 * 作者：By 15968
 * 日期：On 2019/10/4
 * 时间：At 13:56
 */
public class Bookmark extends LitePalSupport implements Comparable<Bookmark> {
    private String title;
    private String url;
    private String group;
    private int order;

    public long getId() {
        return getBaseObjId();
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

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

//    @Override
//    public int compareTo(@NonNull Bookmark bookmark) {
//        return bookmark.getOrder() - this.getOrder();
//    }

    @Override
    public int compareTo(@NonNull Bookmark articleListRule) {
        if (StringUtil.isEmpty(articleListRule.getUrl()) && StringUtil.isEmpty(getUrl())) {
            //是分组
            return getTitle().compareTo(articleListRule.getTitle());
        }
        if (StringUtil.isEmpty(getUrl())) {
            return -1;
        }
        if (StringUtil.isEmpty(articleListRule.getUrl())) {
            return 1;
        }
        int n1 = getGroup().length();
        int n2 = articleListRule.getGroup().length();
        int min = Math.min(n1, n2);
        if (min == 0 && (n2 - n1) != 0) {
            return n2 - n1;
        }
        int g = this.getGroup().compareTo(articleListRule.getGroup());
        if (g == 0) {
            int o = this.getOrder() - articleListRule.getOrder();
            if (o == 0) {
                return (int) (this.getId() - articleListRule.getId());
            } else {
                return o;
            }
        } else {
            return g;
        }
    }

    public String getGroup() {
        if (group == null) {
            group = "";
        }
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }
}

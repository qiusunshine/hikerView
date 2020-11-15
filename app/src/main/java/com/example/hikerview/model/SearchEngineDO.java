package com.example.hikerview.model;

import androidx.annotation.NonNull;

import com.alibaba.fastjson.annotation.JSONField;
import com.example.hikerview.utils.PinyinUtil;
import com.example.hikerview.utils.StringUtil;

import org.litepal.crud.LitePalSupport;

/**
 * 作者：By 15968
 * 日期：On 2019/10/31
 * 时间：At 20:40
 */
public class SearchEngineDO extends LitePalSupport implements Comparable<SearchEngineDO> {
    private String title;
    private String titleColor;
    private String findRule;
    private String group;
    @JSONField(serialize = false, deserialize = false)
    private int order;
    @JSONField(serialize = false, deserialize = false)
    private String pinYin;

    public SearchEngineDO() {
    }

    private String search_url;

    public SearchEngineDO(String title, String search_url) {
        this.title = title;
        this.search_url = search_url;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSearch_url() {
        return search_url;
    }

    public void setSearch_url(String search_url) {
        this.search_url = search_url;
    }

    public String getTitleColor() {
        return titleColor;
    }

    public void setTitleColor(String titleColor) {
        this.titleColor = titleColor;
    }

    public String getFindRule() {
        return findRule;
    }

    public void setFindRule(String findRule) {
        this.findRule = findRule;
    }

    public long getId() {
        return getBaseObjId();
    }

    @Override
    public int compareTo(@NonNull SearchEngineDO articleListRule) {
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
                if (StringUtil.isEmpty(getPinYin())) {
                    this.setPinYin(PinyinUtil.instance().getPinyin(this.getTitle()));
                }
                if (StringUtil.isEmpty(articleListRule.getPinYin())) {
                    articleListRule.setPinYin(PinyinUtil.instance().getPinyin(articleListRule.getTitle()));
                }
                return this.getPinYin().compareTo(articleListRule.getPinYin());
            } else {
                return o;
            }
        } else {
            return g;
        }
    }

    public String getGroup() {
        if(group == null){
            return "";
        }
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public String getPinYin() {
        return pinYin;
    }

    public void setPinYin(String pinYin) {
        this.pinYin = pinYin;
    }
}

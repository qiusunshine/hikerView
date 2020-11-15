package com.example.hikerview.event;

import com.example.hikerview.ui.browser.model.SearchEngine;
import com.example.hikerview.ui.search.model.SearchGroup;

import java.util.List;

/**
 * 作者：By 15968
 * 日期：On 2020/3/7
 * 时间：At 12:16
 */
public class SearchEvent {
    public SearchEvent(String text, SearchEngine searchEngine, String tag, List<SearchGroup> groups, SearchGroup group) {
        this.text = text;
        this.searchEngine = searchEngine;
        this.tag = tag;
        this.groups = groups;
        this.group = group;
    }

    private String text;
    private SearchEngine searchEngine;
    private String tag;
    private List<SearchGroup> groups;
    private SearchGroup group;

    public SearchEngine getSearchEngine() {
        return searchEngine;
    }

    public void setSearchEngine(SearchEngine searchEngine) {
        this.searchEngine = searchEngine;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public List<SearchGroup> getGroups() {
        return groups;
    }

    public void setGroups(List<SearchGroup> groups) {
        this.groups = groups;
    }

    public SearchGroup getGroup() {
        return group;
    }

    public void setGroup(SearchGroup group) {
        this.group = group;
    }
}

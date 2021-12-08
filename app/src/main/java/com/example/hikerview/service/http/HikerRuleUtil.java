package com.example.hikerview.service.http;

import com.alibaba.fastjson.JSON;
import com.example.hikerview.constants.JSONPreFilter;
import com.example.hikerview.model.BigTextDO;
import com.example.hikerview.model.Bookmark;
import com.example.hikerview.model.DownloadRecord;
import com.example.hikerview.model.ViewCollection;
import com.example.hikerview.model.ViewHistory;
import com.example.hikerview.ui.Application;
import com.example.hikerview.ui.bookmark.model.BookmarkModel;
import com.example.hikerview.ui.browser.util.CollectionUtil;
import com.example.hikerview.ui.home.model.ArticleListRule;
import com.example.hikerview.ui.home.model.ArticleListRuleModel;
import com.example.hikerview.utils.FilesInAppUtil;
import com.example.hikerview.utils.HeavyTaskUtil;
import com.example.hikerview.utils.StringUtil;

import org.apache.commons.lang3.StringUtils;
import org.litepal.LitePal;

import java.util.Collections;
import java.util.List;

/**
 * 作者：By 15968
 * 日期：On 2020/6/25
 * 时间：At 22:31
 */
public class HikerRuleUtil {

    public static String getRulesByHiker(String url) {
        if (StringUtil.isEmpty(url) || !url.startsWith("hiker://")) {
            return null;
        }
        url = url.replace("hiker://", "");
        if (url.startsWith("home")) {
            String[] names = url.split("@");
            if (names.length > 1) {
                ArticleListRule rule = LitePal.where("title = ?", names[1]).findFirst(ArticleListRule.class);
                return JSON.toJSONString(rule, JSONPreFilter.getSimpleFilter());
            }
            List<ArticleListRule> rules = LitePal.findAll(ArticleListRule.class);
            return JSON.toJSONString(ArticleListRuleModel.sort(rules), JSONPreFilter.getSimpleFilter());
        } else if (url.startsWith("bookmark")) {
            List<Bookmark> rules = LitePal.findAll(Bookmark.class);
            return JSON.toJSONString(BookmarkModel.sort(rules), JSONPreFilter.getSimpleFilter());
        } else if (url.startsWith("download")) {
            return JSON.toJSONString(LitePal.findAll(DownloadRecord.class), JSONPreFilter.getSimpleFilter());
        } else if (url.startsWith("collection")) {
            List<ViewCollection> list = LitePal.findAll(ViewCollection.class);
            if (CollectionUtil.isNotEmpty(list)) {
                Collections.sort(list);
            }
            return JSON.toJSONString(list, JSONPreFilter.getSimpleFilter());
        } else if (url.startsWith("history")) {
            List<ViewHistory> list = LitePal.findAll(ViewHistory.class);
            if (CollectionUtil.isNotEmpty(list)) {
                Collections.sort(list);
            }
            return JSON.toJSONString(list, JSONPreFilter.getSimpleFilter());
        } else {
            return "";
        }
    }

    public static String getAssetsFileByHiker(String url) {
        if (StringUtil.isEmpty(url) || !url.startsWith("hiker://assets/")) {
            return "";
        }
        url = url.replace("hiker://assets/", "");
        if (url.equals("home.js")) {
            String js = BigTextDO.getHomeJs();
            if (js == null) {
                return FilesInAppUtil.getAssetsString(Application.getContext(), "home.js");
            } else {
                return js;
            }
        } else if (url.equals("beautify.js")) {
            return FilesInAppUtil.getAssetsString(Application.getContext(), url);
        } else {
            return "";
        }
    }

    public static void getRulesByHiker(String url, CodeUtil.OnCodeGetListener listener) {
        try {
            if (StringUtil.isEmpty(url) || !url.startsWith("hiker://")) {
                listener.onFailure(404, url + "格式有误");
            }
            url = url.replace("hiker://", "");
            if (url.startsWith("empty")) {
                listener.onSuccess(StringUtils.replaceOnce(url, "empty", ""));
            } else if (url.startsWith("page")) {
                listener.onSuccess(url);
            } else if (url.startsWith("home")) {
                String finalUrl = url;
                HeavyTaskUtil.executeNewTask(() -> {
                    String[] names = finalUrl.split("@");
                    if (names.length > 1) {
                        ArticleListRule rule = LitePal.where("title = ?", names[1]).findFirst(ArticleListRule.class);
                        listener.onSuccess(JSON.toJSONString(rule, JSONPreFilter.getSimpleFilter()));
                    }
                    List<ArticleListRule> rules = LitePal.findAll(ArticleListRule.class);
                    listener.onSuccess(JSON.toJSONString(ArticleListRuleModel.sort(rules), JSONPreFilter.getSimpleFilter()));
                });
            } else if (url.startsWith("bookmark")) {
                HeavyTaskUtil.executeNewTask(() -> {
                    List<Bookmark> rules = LitePal.findAll(Bookmark.class);
                    listener.onSuccess(JSON.toJSONString(BookmarkModel.sort(rules), JSONPreFilter.getSimpleFilter()));
                });
            } else if (url.startsWith("download")) {
                LitePal.findAllAsync(DownloadRecord.class).listen(list -> {
                    listener.onSuccess(JSON.toJSONString(list, JSONPreFilter.getSimpleFilter()));
                });
            } else if (url.startsWith("collection")) {
                LitePal.findAllAsync(ViewCollection.class).listen(list -> {
                    listener.onSuccess(JSON.toJSONString(list, JSONPreFilter.getSimpleFilter()));
                });
            } else if (url.startsWith("history")) {
                LitePal.findAllAsync(ViewHistory.class).listen(list -> {
                    listener.onSuccess(JSON.toJSONString(list, JSONPreFilter.getSimpleFilter()));
                });
            } else {
                LitePal.findAllAsync(ArticleListRule.class).listen(list -> {
                    listener.onSuccess("");
                });
            }
        } catch (Exception e) {
            listener.onFailure(500, "获取数据失败：" + e.getMessage());
        }
    }
}

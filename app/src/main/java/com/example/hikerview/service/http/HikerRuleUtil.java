package com.example.hikerview.service.http;

import com.alibaba.fastjson.JSON;
import com.example.hikerview.constants.JSONPreFilter;
import com.example.hikerview.model.Bookmark;
import com.example.hikerview.model.DownloadRecord;
import com.example.hikerview.model.ViewCollection;
import com.example.hikerview.model.ViewHistory;
import com.example.hikerview.ui.home.model.ArticleListRule;
import com.example.hikerview.utils.StringUtil;

import org.apache.commons.lang3.StringUtils;
import org.litepal.LitePal;

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
            return JSON.toJSONString(LitePal.findAll(ArticleListRule.class), JSONPreFilter.getSimpleFilter());
        } else if (url.startsWith("bookmark")) {
            return JSON.toJSONString(LitePal.findAll(Bookmark.class), JSONPreFilter.getSimpleFilter());
        } else if (url.startsWith("download")) {
            return JSON.toJSONString(LitePal.findAll(DownloadRecord.class), JSONPreFilter.getSimpleFilter());
        } else if (url.startsWith("collection")) {
            return JSON.toJSONString(LitePal.findAll(ViewCollection.class), JSONPreFilter.getSimpleFilter());
        } else if (url.startsWith("history")) {
            return JSON.toJSONString(LitePal.findAll(ViewHistory.class), JSONPreFilter.getSimpleFilter());
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
            } else if (url.startsWith("home")) {
                LitePal.findAllAsync(ArticleListRule.class).listen(list -> {
                    listener.onSuccess(JSON.toJSONString(list, JSONPreFilter.getSimpleFilter()));
                });
            } else if (url.startsWith("bookmark")) {
                LitePal.findAllAsync(Bookmark.class).listen(list -> {
                    listener.onSuccess(JSON.toJSONString(list, JSONPreFilter.getSimpleFilter()));
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

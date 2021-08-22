package com.example.hikerview.service.parser;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.example.hikerview.service.exception.ParseException;
import com.example.hikerview.ui.browser.model.UrlDetector;
import com.example.hikerview.ui.browser.util.CollectionUtil;
import com.example.hikerview.ui.home.model.ArticleListPageRule;
import com.example.hikerview.ui.home.model.ArticleListRule;
import com.example.hikerview.utils.StringUtil;

import org.apache.commons.lang3.StringUtils;
import org.litepal.LitePal;

import java.util.List;
import java.util.Map;

/**
 * 作者：By 15968
 * 日期：On 2021/7/6
 * 时间：At 9:44
 */

public class PageParser {

    /**
     * 判断是否是二级页面
     *
     * @param url
     * @return
     */
    public static boolean isPageUrl(String url) {
        return StringUtil.isNotEmpty(url) && url.startsWith("hiker://page/");
    }

    /**
     * 获取下一页的参数
     *
     * @param parent
     * @param url
     * @return
     */
    public static ArticleListRule getNextPage(ArticleListRule parent, String url, String params) throws ParseException {
        if (!isPageUrl(url)) {
            return null;
        }

        ArticleListRule rule = new ArticleListRule();
        String extraRuleName = null;
        JSONObject extraJsonObject = null;
        if (StringUtil.isNotEmpty(params) && params.startsWith("{") && params.endsWith("}")) {
            try {
                extraJsonObject = JSON.parseObject(params);
                if (extraJsonObject != null && extraJsonObject.containsKey("rule")) {
                    extraRuleName = extraJsonObject.getString("rule");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        ArticleListPageRule pageRule = parsePageRule(parent, extraRuleName, url);
        ArticleListRule parentRule = pageRule.getParentRule();
        rule.setFind_rule(pageRule.getRule());
        rule.setPreRule(parentRule.getPreRule());
        rule.setGroup(parentRule.getGroup());
        rule.setUa(parentRule.getUa());
        rule.setTitle(parentRule.getTitle());
        rule.setLast_chapter_rule(parentRule.getLast_chapter_rule());
        rule.setCol_type(pageRule.getCol_type());
        rule.setPages(parentRule.getPages());
        if (StringUtil.isEmpty(rule.getCol_type()) || "*".equals(rule.getCol_type())) {
            rule.setCol_type(parentRule.getCol_type());
        }
        if (pageRule.getParams().containsKey("url")) {
            rule.setUrl(pageRule.getParams().get("url"));
        } else if (extraJsonObject != null && extraJsonObject.containsKey("url") && extraJsonObject.getString("url") != null) {
            rule.setUrl(extraJsonObject.getString("url"));
        } else {
            rule.setUrl(url);
        }
        if (StringUtil.isNotEmpty(params) && params.startsWith("{") && params.endsWith("}")) {
            rule.setParams(params);
        }
        return rule;
    }

    public static ArticleListPageRule parsePageRule(ArticleListRule parent, String url) throws ParseException {
        return parsePageRule(parent, null, url);
    }

    private static ArticleListPageRule parsePageRule(ArticleListRule parent, String rule, String url) throws ParseException {
        ArticleListRule parentRule = parent;
        String parentTitle = parent == null ? null : parent.getTitle();
        Map<String, String> paramsMap = HttpParser.getParamsByUrl(url);
        if (StringUtil.isNotEmpty(rule) && !paramsMap.containsKey("rule")) {
            paramsMap.put("rule", rule);
        }
        if (paramsMap.containsKey("rule")) {
            parentTitle = paramsMap.get("rule");
            if (StringUtil.isEmpty(parentTitle)) {
                parentTitle = parent == null ? null : parent.getTitle();
            } else {
                ArticleListRule articleListRule = LitePal.where("title = ?", parentTitle).findFirst(ArticleListRule.class);
                if (articleListRule == null) {
                    throw new ParseException("找不到“" + parentTitle + "”这个小程序");
                }
                parentRule = articleListRule;
            }
        }
        String[] d = url.split(";");
        String[] urls = StringUtil.splitUrlByQuestionMark(d[0]);
        String path = StringUtils.replaceOnce(urls[0], "hiker://page/", "");
        path = UrlDetector.clearTag(path);
        ArticleListPageRule pageRule = null;
        if (parentRule == null) {
            throw new ParseException("找不到对应的小程序");
        }
        List<ArticleListPageRule> pageRuleList = parentRule.getPageList();
        if (CollectionUtil.isNotEmpty(pageRuleList)) {
            for (ArticleListPageRule articleListPageRule : pageRuleList) {
                if (path.equals(articleListPageRule.getPath())) {
                    pageRule = articleListPageRule;
                    break;
                }
            }
        }
        if (pageRule == null) {
            throw new ParseException("找不到“" + path + "”这个页面");
        }
        pageRule.setParent(parentTitle);
        pageRule.setParentRule(parentRule);
        pageRule.setParams(paramsMap);
        return pageRule;
    }
} 
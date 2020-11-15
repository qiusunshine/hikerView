package com.example.hikerview.utils;

import com.alibaba.fastjson.JSON;
import com.example.hikerview.ui.home.model.ArticleListRule;

/**
 * 作者：By 15968
 * 日期：On 2020/7/26
 * 时间：At 13:20
 */

public class FilterUtil {
    private static String[] filterWords = {"性瘾", "S级", "偷拍", "无码", "有码", "口爆", "伦理", "啪啪",
            "约炮", "少妇", "翘臀", "呻吟", "双飞", "姿势", "情色", "女优",
            "人妻", "性爱", "乱伦", "强奸"};

    public static boolean hasFilterWord(String title) {
        boolean has = false;
        for (String filterWord : filterWords) {
            if (title.contains(filterWord)) {
                has = true;
                break;
            }
        }
        return has;
    }

    public static boolean hasFilterWord(ArticleListRule articleListRuleDTO) {
        if (articleListRuleDTO == null) {
            return false;
        }
        boolean has = false;
        String title = JSON.toJSONString(articleListRuleDTO);
        for (String filterWord : filterWords) {
            if (title.contains(filterWord)) {
                has = true;
                break;
            }
        }
        return has;
    }
}

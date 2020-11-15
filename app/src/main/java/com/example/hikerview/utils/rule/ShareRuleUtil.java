package com.example.hikerview.utils.rule;

import android.app.Activity;
import android.util.Base64;

import com.alibaba.fastjson.JSON;
import com.example.hikerview.constants.JSONPreFilter;
import com.example.hikerview.model.SharedAdUrl;
import com.example.hikerview.ui.browser.util.CollectionUtil;
import com.example.hikerview.ui.home.model.ArticleListRule;
import com.example.hikerview.ui.home.model.ArticleListRuleJO;
import com.example.hikerview.utils.AutoImportHelper;
import com.example.hikerview.utils.ClipboardUtil;
import com.example.hikerview.utils.FilterUtil;
import com.example.hikerview.utils.PreferenceMgr;
import com.example.hikerview.utils.StringUtil;
import com.example.hikerview.utils.ToastMgr;
import com.lxj.xpopup.XPopup;

import org.litepal.LitePal;

import java.util.List;

/**
 * 作者：By 15968
 * 日期：On 2020/9/8
 * 时间：At 19:50
 */

public class ShareRuleUtil {
    /**
     * 更多方式来分享规则
     *
     * @param activity
     * @param rule
     */
    public static void shareRuleByMoreWay(Activity activity, ArticleListRule rule) {
        new XPopup.Builder(activity)
                .asCenterList("请选择操作", new String[]{"分享规则(完整编码)", "分享规则(部分编码)", "分享规则(带拦截规则)", "分享规则(云剪贴板)"},
                        ((option, text) -> {
                            if (activity == null || activity.isFinishing()) {
                                return;
                            }
                            String shareRulePrefix = PreferenceMgr.getString(activity, "shareRulePrefix", "");
                            try {
                                switch (text) {
                                    case "分享规则(完整编码)":
                                        ArticleListRuleJO ruleJO12 = new ArticleListRuleJO(rule);
                                        String command = AutoImportHelper.getCommand(shareRulePrefix, JSON.toJSONString(ruleJO12, JSONPreFilter.getSimpleFilter()), AutoImportHelper.HOME_RULE);
                                        command = new String(Base64.encode(command.getBytes(), Base64.DEFAULT));
                                        ClipboardUtil.copyToClipboardForce(activity, "rule://" + StringUtil.replaceLineBlank(command), false);
                                        ToastMgr.shortBottomCenter(activity, "口令已复制到剪贴板");
                                        break;
                                    case "分享规则(部分编码)":
                                        ArticleListRuleJO ruleJO3 = new ArticleListRuleJO(rule);
                                        String originalRuls = JSON.toJSONString(ruleJO3, JSONPreFilter.getSimpleFilter());
                                        if (FilterUtil.hasFilterWord(originalRuls)) {
                                            ToastMgr.shortBottomCenter(activity, "规则含有违禁词，禁止分享");
                                            return;
                                        }
                                        String base64Rule = "base64://@" + ruleJO3.getTitle() + "@" + new String(Base64.encode(originalRuls.getBytes(), Base64.DEFAULT));
                                        AutoImportHelper.shareWithCommand(activity, StringUtil.replaceLineBlank(base64Rule), AutoImportHelper.HOME_RULE_V2);
                                        break;
                                    case "分享规则(带拦截规则)":
                                        ArticleListRuleJO ruleJO = new ArticleListRuleJO(rule);
                                        List<SharedAdUrl> sharedAdUrls = LitePal.where("dom = ?", StringUtil.getDom(ruleJO.getUrl())).limit(1).find(SharedAdUrl.class);
                                        if (!CollectionUtil.isEmpty(sharedAdUrls)) {
                                            ruleJO.setAdBlockUrls(sharedAdUrls.get(0).getBlockUrls());
                                        }
                                        AutoImportHelper.shareWithCommand(activity, JSON.toJSONString(ruleJO, JSONPreFilter.getSimpleFilter()), AutoImportHelper.HOME_RULE);
                                        break;
                                    case "分享规则(云剪贴板)":
                                        shareRuleByPasteme(activity, rule, shareRulePrefix);
                                        break;
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }))
                .show();
    }

    private static void shareRuleByPasteme(Activity activity, ArticleListRule rule1, String shareRulePrefix) {
        try {
            ArticleListRuleJO ruleJO1 = new ArticleListRuleJO(rule1);
            String rule = JSON.toJSONString(ruleJO1, JSONPreFilter.getSimpleFilter());
            if (FilterUtil.hasFilterWord(rule)) {
                ToastMgr.shortBottomCenter(activity, "规则含有违禁词，禁止分享");
                return;
            }
            String paste = AutoImportHelper.getCommand(shareRulePrefix, rule, AutoImportHelper.HOME_RULE);

            AutoImportHelper.shareByPasteme(activity, paste, ruleJO1.getTitle());
        } catch (Exception e) {
            ToastMgr.shortCenter(activity, e.getMessage());
        }
    }
}

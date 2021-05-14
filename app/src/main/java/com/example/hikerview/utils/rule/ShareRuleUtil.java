package com.example.hikerview.utils.rule;

import android.app.Activity;
import android.content.Context;
import android.util.Base64;

import com.alibaba.fastjson.JSON;
import com.example.hikerview.constants.JSONPreFilter;
import com.example.hikerview.model.SharedAdUrl;
import com.example.hikerview.ui.browser.util.CollectionUtil;
import com.example.hikerview.ui.home.model.ArticleListRule;
import com.example.hikerview.ui.home.model.ArticleListRuleJO;
import com.example.hikerview.ui.rules.utils.PublishHelper;
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
                .asCenterList("请选择操作", new String[]{"明文口令分享", "完整编码分享", "提交到云仓库", "云剪贴板分享", "云剪贴板分享2"},
                        ((option, text) -> {
                            if (activity == null || activity.isFinishing()) {
                                return;
                            }
                            String shareRulePrefix = PreferenceMgr.getString(activity, "shareRulePrefix", "");
                            try {
                                switch (text) {
                                    case "明文口令分享":
                                        ArticleListRuleJO ruleJO1 = new ArticleListRuleJO(rule);
                                        AutoImportHelper.shareWithCommand(activity, JSON.toJSONString(ruleJO1, JSONPreFilter.getSimpleFilter()), AutoImportHelper.HOME_RULE);
                                        break;
                                    case "完整编码分享":
                                        ArticleListRuleJO ruleJO3 = new ArticleListRuleJO(rule);
                                        String command = AutoImportHelper.getCommand(shareRulePrefix, JSON.toJSONString(ruleJO3, JSONPreFilter.getSimpleFilter()), AutoImportHelper.HOME_RULE);
                                        command = new String(Base64.encode(command.getBytes(), Base64.NO_WRAP));
                                        ClipboardUtil.copyToClipboardForce(activity, "rule://" + StringUtil.replaceLineBlank(command), false);
                                        ToastMgr.shortBottomCenter(activity, "口令已复制到剪贴板");
                                        break;
                                    case "分享规则(带拦截规则)":
                                        ArticleListRuleJO ruleJO = new ArticleListRuleJO(rule);
                                        List<SharedAdUrl> sharedAdUrls = LitePal.where("dom = ?", StringUtil.getDom(ruleJO.getUrl())).limit(1).find(SharedAdUrl.class);
                                        if (!CollectionUtil.isEmpty(sharedAdUrls)) {
                                            ruleJO.setAdBlockUrls(sharedAdUrls.get(0).getBlockUrls());
                                        }
                                        AutoImportHelper.shareWithCommand(activity, JSON.toJSONString(ruleJO, JSONPreFilter.getSimpleFilter()), AutoImportHelper.HOME_RULE);
                                        break;
                                    case "云剪贴板分享":
                                        shareRuleByPasteme(activity, rule, shareRulePrefix);
                                        break;
                                    case "云剪贴板分享2":
                                        shareRuleByNetCut(activity, rule, shareRulePrefix);
                                        break;
                                    case "提交到云仓库":
                                        PublishHelper.publishRule(activity, rule);
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
            String base64Rule = "base64://@" + ruleJO1.getTitle() + "@" + new String(Base64.encode(rule.getBytes(), Base64.NO_WRAP));
            String paste = AutoImportHelper.getCommand(shareRulePrefix, StringUtil.replaceLineBlank(base64Rule), AutoImportHelper.HOME_RULE_V2);

            AutoImportHelper.shareByPasteme(activity, paste, ruleJO1.getTitle());
        } catch (Exception e) {
            ToastMgr.shortCenter(activity, e.getMessage());
        }
    }

    private static void shareRuleByNetCut(Activity activity, ArticleListRule rule1, String shareRulePrefix) {
        try {
            ArticleListRuleJO ruleJO3 = new ArticleListRuleJO(rule1);
            String originalRuls = JSON.toJSONString(ruleJO3, JSONPreFilter.getSimpleFilter());
            if (FilterUtil.hasFilterWord(originalRuls)) {
                ToastMgr.shortBottomCenter(activity, "规则含有违禁词，禁止分享");
                return;
            }
            String base64Rule = "base64://@" + ruleJO3.getTitle() + "@" + new String(Base64.encode(originalRuls.getBytes(), Base64.NO_WRAP));
            String paste = AutoImportHelper.getCommand(shareRulePrefix, StringUtil.replaceLineBlank(base64Rule), AutoImportHelper.HOME_RULE_V2);

            AutoImportHelper.shareByNetCut(activity, paste, ruleJO3.getTitle(), "规则名");
        } catch (Exception e) {
            ToastMgr.shortCenter(activity, e.getMessage());
        }
    }

    public static void shareRule(Context context, ArticleListRule articleListRule) {
        ArticleListRuleJO ruleJO3 = new ArticleListRuleJO(articleListRule);
        String originalRuls = JSON.toJSONString(ruleJO3, JSONPreFilter.getSimpleFilter());
        if (FilterUtil.hasFilterWord(originalRuls)) {
            ToastMgr.shortBottomCenter(context, "规则含有违禁词，禁止分享");
            return;
        }
        String base64Rule = "base64://@" + ruleJO3.getTitle() + "@" + new String(Base64.encode(originalRuls.getBytes(), Base64.NO_WRAP));
        AutoImportHelper.shareWithCommand(context, StringUtil.replaceLineBlank(base64Rule), AutoImportHelper.HOME_RULE_V2);
    }
}

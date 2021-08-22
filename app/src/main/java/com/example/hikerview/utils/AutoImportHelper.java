package com.example.hikerview.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.alibaba.fastjson.JSON;
import com.annimon.stream.function.Consumer;
import com.example.hikerview.R;
import com.example.hikerview.bean.MovieInfoUse;
import com.example.hikerview.constants.ArticleColTypeEnum;
import com.example.hikerview.event.BookmarkRefreshEvent;
import com.example.hikerview.event.LoadingDismissEvent;
import com.example.hikerview.event.OnArticleListRuleChangedEvent;
import com.example.hikerview.event.ToastMessage;
import com.example.hikerview.model.Bookmark;
import com.example.hikerview.model.SearchEngineDO;
import com.example.hikerview.service.http.CodeUtil;
import com.example.hikerview.service.parser.JSEngine;
import com.example.hikerview.service.subscribe.AdUrlSubscribe;
import com.example.hikerview.ui.base.SimpleActionListener;
import com.example.hikerview.ui.bookmark.BookmarkActivity;
import com.example.hikerview.ui.browser.model.AdBlockModel;
import com.example.hikerview.ui.browser.model.AdUrlBlocker;
import com.example.hikerview.ui.browser.model.JSManager;
import com.example.hikerview.ui.browser.util.CollectionUtil;
import com.example.hikerview.ui.home.ArticleListRuleEditActivity;
import com.example.hikerview.ui.home.FilmListActivity;
import com.example.hikerview.ui.home.model.ArticleListRule;
import com.example.hikerview.ui.home.model.ArticleListRuleJO;
import com.example.hikerview.ui.js.AdUrlListActivity;
import com.example.hikerview.ui.rules.PublishCodeEditActivity;
import com.example.hikerview.ui.rules.model.DetailPageRule;
import com.example.hikerview.ui.rules.service.HomeRulesSubService;
import com.example.hikerview.ui.rules.service.RuleImporterManager;
import com.example.hikerview.ui.search.model.SearchRuleJO;
import com.example.hikerview.ui.setting.utils.FastPlayImportUtil;
import com.example.hikerview.ui.setting.utils.XTDialogRulesImportUtil;
import com.example.hikerview.ui.view.DialogBuilder;
import com.example.hikerview.ui.view.ZLoadingDialog.ZLoadingDialog;
import com.example.hikerview.ui.view.colorDialog.PromptDialog;
import com.example.hikerview.ui.view.dialog.GlobalDialogActivity;
import com.example.hikerview.ui.view.popup.ImportRulesPopup;
import com.lxj.xpopup.XPopup;

import org.greenrobot.eventbus.EventBus;
import org.litepal.LitePal;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import timber.log.Timber;

/**
 * 作者：By 15968
 * 日期：On 2019/10/19
 * 时间：At 10:29
 */
public class AutoImportHelper {
    private static final String TAG = "AutoImportHelper";

    public static final String BOOKMARK = "bookmark";
    public static final String FAST_PLAY_URLS = "fast_play_urls";
    public static final String XT_DIALOG_RULES = "xt_dialog_rules";
    public static final String HOME_RULE = "home_rule";
    public static final String HOME_RULE_V2 = "home_rule_v2";
    public static final String HOME_RULE_URL = "home_rule_url";
    public static final String JS_URL = "js_url";
    public static final String AD_URL_RULES = "ad_url_rule";
    public static final String SEARCH_ENGINE = "search_engine_v2";
    public static final String SEARCH_ENGINE_URL = "search_engine_url";
    public static final String BOOKMARK_URL = "bookmark_url";
    public static final String FILE_URL = "file_url";
    public static final String AD_SUBSCRIBE = "ad_subscribe_url";
    public static final String AD_BLOCK_RULES = "ad_block_url";
    public static final String PAGE_DETAIL_RULES = "page_detail";
    public static final String PUBLISH_CODE = "publish";
    public static final String HOME_SUB = "home_sub";
    private static final String SOURCE = "方圆影视视频源分享，全部复制后打开方圆影视APP最新版即可获取到视频源信息￥source￥";
    private static String shareRule = "";


    public static String getCommand(String shareRulePrefix, String shareText, String type) {
        String text = "海阔视界规则分享，当前分享的是：";
        switch (type) {
            case BOOKMARK:
                text = text + "书签规则";
                break;
            case FAST_PLAY_URLS:
                text = text + "快速播放白名单";
                break;
            case XT_DIALOG_RULES:
                text = text + "嗅探弹窗黑名单";
                break;
            case HOME_RULE:
                text = text + "小程序";
                break;
            case HOME_RULE_V2:
                text = text + "小程序";
                break;
            case HOME_RULE_URL:
                text = text + "首页频道合集";
                break;
            case SEARCH_ENGINE:
                text = text + "搜索引擎";
                break;
            case SEARCH_ENGINE_URL:
                text = text + "搜索引擎合集";
                break;
            case BOOKMARK_URL:
                text = text + "书签合集";
                break;
            case JS_URL:
                text = text + "网页插件";
                break;
            case AD_URL_RULES:
                text = text + "广告网址拦截";
                break;
            case AD_BLOCK_RULES:
                text = text + "广告元素拦截";
                break;
            case FILE_URL:
                text = text + "本地文件";
                break;
            case AD_SUBSCRIBE:
                text = text + "广告拦截订阅";
                break;
            case PAGE_DETAIL_RULES:
                text = text + "二级页面详情";
                break;
            case PUBLISH_CODE:
                text = text + "提交云仓库规则";
                break;
            case HOME_SUB:
                text = text + "合集规则订阅";
                break;
        }
        if (StringUtil.isNotEmpty(shareRulePrefix)) {
            text = text + "，" + shareRulePrefix;
        }
        return text + "￥" + type + "￥" + shareText;
    }

    public static void shareWithCommand(Context context, String shareText, String type) {
        String shareRulePrefix = PreferenceMgr.getString(context, "shareRulePrefix", "");
        if (FilterUtil.hasFilterWord(shareText)) {
            ToastMgr.shortBottomCenter(context, "规则含有违禁词，禁止分享");
            return;
        }
        String command = getCommand(shareRulePrefix, shareText, type);
        setShareRule(command);
        ClipboardUtil.copyToClipboardForce(context, command, false);
        ToastMgr.shortBottomCenter(context, "规则已复制到剪贴板");
    }

    public static String getRealRule(String rule) {
        if (TextUtils.isEmpty(rule)) {
            return rule;
        }
        String[] rules = rule.split("￥");
        if (rules.length != 3) {
            return rule;
        } else {
            return rules[2];
        }
    }

    public static boolean checkText(Activity context, String text1) {
        if (StringUtil.isEmpty(text1)) {
            return false;
        }
        if (RuleImporterManager.parse(context, text1)) {
            return true;
        }
        return AutoImportHelper.checkAutoText(context, text1);
    }

    public static boolean checkAutoText(Context context, String shareText) {
        try {
            return realCheckAutoText(context, shareText);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean realCheckAutoText(Context context, String shareText) {
        shareText = shareText.trim();
        if (shareText.startsWith("方圆")) {
            return checkAutoTextByFangYuan(context, shareText);
        }
        String st = StringUtil.trimBlanks(shareText);
        if (st.startsWith("{") && st.endsWith("}") && st.contains("col_type") && st.contains("find_rule")) {
            shareText = "海阔视界规则分享，当前分享的是：小程序￥home_rule￥" + st;
        }
        final String[] sss = shareText.split("￥");
        if (sss.length < 3) {
            return false;
        }
        switch (sss[1]) {
            case BOOKMARK:
                String finalShareText1 = shareText;
                new PromptDialog(context)
                        .setDialogType(PromptDialog.DIALOG_TYPE_INFO)
                        .setAnimationEnable(true)
                        .setTitleText("温馨提示")
                        .setContentText("剪贴板检测到书签信息")
                        .setPositiveListener("查看", dialog -> {
                            dialog.dismiss();
                            ClipboardUtil.copyToClipboard(context, "");
                            try {
                                Intent intent = new Intent(context, BookmarkActivity.class);
                                //广告拦截规则
                                if (finalShareText1.contains("『附拦截规则』")) {
                                    showWithAdUrlsDialog(context, "检测到该书签规则附带了广告拦截规则，确定立即导入规则吗？", withAdUrls -> {
                                        if (withAdUrls) {
                                            if (sss.length > 4) {
                                                intent.putExtra("webs", sss[2] + "￥" + sss[3] + "￥" + sss[4]);
                                            } else {
                                                intent.putExtra("webs", sss[2] + "￥" + sss[3]);
                                            }
                                            context.startActivity(intent);
                                        } else {
                                            if (sss.length > 4) {
                                                intent.putExtra("webs", sss[2] + "￥" + sss[3] + "￥" + sss[4].split("『附拦截规则』")[0]);
                                            } else {
                                                intent.putExtra("webs", sss[2] + "￥" + sss[3].split("『附拦截规则』")[0]);
                                            }
                                            context.startActivity(intent);
                                        }
                                    });
                                } else {
                                    if (sss.length > 4) {
                                        intent.putExtra("webs", sss[2] + "￥" + sss[3] + "￥" + sss[4]);
                                    } else {
                                        intent.putExtra("webs", sss[2] + "￥" + sss[3]);
                                    }
                                    context.startActivity(intent);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                                ToastMgr.shortCenter(context, "出错：" + e.getMessage());
                            }
                        }).show();
                return true;
            case AD_URL_RULES:
                new PromptDialog(context)
                        .setDialogType(PromptDialog.DIALOG_TYPE_INFO)
                        .setAnimationEnable(true)
                        .setTitleText("温馨提示")
                        .setContentText("剪贴板检测到广告网址过滤规则")
                        .setPositiveListener("查看", dialog -> {
                            dialog.dismiss();
                            ClipboardUtil.copyToClipboard(context, "");
                            try {
                                Intent intent = new Intent(context, AdUrlListActivity.class);
                                intent.putExtra("data", sss[2]);
                                context.startActivity(intent);
                            } catch (Exception e) {
                                e.printStackTrace();
                                ToastMgr.shortCenter(context, "出错：" + e.getMessage());
                            }
                        }).show();
                return true;
            case HOME_SUB:
                new PromptDialog(context)
                        .setDialogType(PromptDialog.DIALOG_TYPE_INFO)
                        .setAnimationEnable(true)
                        .setTitleText("温馨提示")
                        .setContentText("剪贴板检测到合集规则订阅地址")
                        .setPositiveListener("立即导入", dialog -> {
                            dialog.dismiss();
                            ClipboardUtil.copyToClipboard(context, "");
                            String[] rules = sss[2].split("@@");
                            if (rules.length != 2) {
                                ToastMgr.shortBottomCenter(context, "格式有误");
                                return;
                            }
                            HomeRulesSubService.addSubWithPopup(context, rules[0], rules[1], ok -> {
                                if (ok) {
                                    ToastMgr.shortBottomCenter(context, "已添加订阅");
                                }
                            });
                        }).show();
                return true;
            case PUBLISH_CODE:
                new PromptDialog(context)
                        .setDialogType(PromptDialog.DIALOG_TYPE_INFO)
                        .setAnimationEnable(true)
                        .setTitleText("温馨提示")
                        .setContentText("剪贴板检测到提交云仓库规则")
                        .setPositiveListener("立即查看", dialog -> {
                            dialog.dismiss();
                            ClipboardUtil.copyToClipboard(context, "");
                            try {
                                String rule = new String(Base64.decode(StringUtil.replaceLineBlank(sss[2]), Base64.NO_WRAP));
                                Intent intent = new Intent(context, PublishCodeEditActivity.class);
                                intent.putExtra("data", rule);
                                context.startActivity(intent);
                            } catch (Exception e) {
                                e.printStackTrace();
                                ToastMgr.shortCenter(context, "出错：" + e.getMessage());
                            }
                        }).show();
                return true;
            case AD_BLOCK_RULES:
                new PromptDialog(context)
                        .setDialogType(PromptDialog.DIALOG_TYPE_INFO)
                        .setAnimationEnable(true)
                        .setTitleText("温馨提示")
                        .setContentText("剪贴板检测到广告元素拦截规则")
                        .setPositiveListener("立即导入", dialog -> {
                            dialog.dismiss();
                            ClipboardUtil.copyToClipboard(context, "");
                            DialogBuilder.createInputConfirm(context, "编辑拦截规则", sss[2], text1 -> {
                                String[] ss = text1.split("::");
                                if (text1.contains("##") || ss.length != 2 || TextUtils.isEmpty(ss[0]) || StringUtil.isEmpty(ss[1])) {
                                    ToastMgr.shortBottomCenter(context, "规则有误");
                                } else {
                                    AdBlockModel.saveRule(ss[0], ss[1]);
                                    ToastMgr.shortBottomCenter(context, "已保存规则");
                                }
                            }).show();
                        }).show();
                return true;
            case AD_SUBSCRIBE:
                new PromptDialog(context)
                        .setDialogType(PromptDialog.DIALOG_TYPE_INFO)
                        .setAnimationEnable(true)
                        .setTitleText("温馨提示")
                        .setContentText("剪贴板检测到广告拦截订阅地址，是否立即设置为远程订阅地址")
                        .setPositiveListener("设置", dialog -> {
                            dialog.dismiss();
                            try {
                                ClipboardUtil.copyToClipboard(context, "");
                                if (TextUtils.isEmpty(sss[2])) {
                                    AdUrlSubscribe.updateSubscribeUrl(context, "no-subscribe");
                                    ToastMgr.shortBottomCenter(context, "已取消订阅功能");
                                } else if (sss[2].startsWith("http")) {
                                    AdUrlSubscribe.updateSubscribeUrl(context, sss[2]);
                                    ToastMgr.shortBottomCenter(context, "已设置订阅地址为" + sss[2]);
                                    AdUrlSubscribe.checkUpdateAsync(context, new SimpleActionListener() {
                                        @Override
                                        public void success(String msg) {

                                        }

                                        @Override
                                        public void failed(String msg) {
                                        }
                                    });
                                } else {
                                    ToastMgr.shortBottomCenter(context, "订阅地址格式有误");
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                                ToastMgr.shortCenter(context, "出错：" + e.getMessage());
                            }
                        }).show();
                return true;
            case FAST_PLAY_URLS:
                new PromptDialog(context)
                        .setDialogType(PromptDialog.DIALOG_TYPE_INFO)
                        .setAnimationEnable(true)
                        .setTitleText("温馨提示")
                        .setContentText("剪贴板检测到快速播放设置信息，是否立即保存？")
                        .setPositiveListener("保存", dialog -> {
                            dialog.dismiss();
                            try {
                                if (sss.length != 3) {
                                    ToastMgr.shortBottomCenter(context, "格式错误！");
                                    return;
                                }
                                ClipboardUtil.copyToClipboard(context, "");
                                FastPlayImportUtil.importRules(context, sss[2], count -> EventBus.getDefault().post(new ToastMessage("成功保存" + count + "条规则")));
                            } catch (Exception e) {
                                e.printStackTrace();
                                ToastMgr.shortCenter(context, "出错：" + e.getMessage());
                            }
                        }).show();
                return true;
            case XT_DIALOG_RULES:
                new PromptDialog(context)
                        .setDialogType(PromptDialog.DIALOG_TYPE_INFO)
                        .setAnimationEnable(true)
                        .setTitleText("温馨提示")
                        .setContentText("剪贴板检测到嗅探弹窗设置信息，是否立即保存？")
                        .setPositiveListener("保存", dialog -> {
                            dialog.dismiss();
                            if (sss.length != 3) {
                                ToastMgr.shortBottomCenter(context, "格式错误！");
                                return;
                            }
                            ClipboardUtil.copyToClipboard(context, "");
                            try {
                                XTDialogRulesImportUtil.importRules(context, sss[2], count -> EventBus.getDefault().post(new ToastMessage("成功保存" + count + "条规则")));
                            } catch (Exception e) {
                                e.printStackTrace();
                                ToastMgr.shortCenter(context, "出错：" + e.getMessage());
                            }
                        }).show();
                return true;
            case HOME_RULE:
            case HOME_RULE_V2:
                new PromptDialog(context)
                        .setDialogType(PromptDialog.DIALOG_TYPE_INFO)
                        .setAnimationEnable(true)
                        .setTitleText("温馨提示")
                        .setContentText("剪贴板检测到小程序规则，是否立即导入？")
                        .setPositiveListener("立即导入", dialog -> {
                            dialog.dismiss();
                            try {
                                String ruleStr;
                                if (sss.length != 3) {
                                    ruleStr = StringUtil.arrayToString(sss, 2, sss.length, "￥");
                                } else {
                                    ruleStr = sss[2];
                                }
                                ClipboardUtil.copyToClipboard(context, "");
                                importRuleByRuleText(context, ruleStr);
                            } catch (Exception e) {
                                e.printStackTrace();
                                ToastMgr.shortCenter(context, "出错：" + e.getMessage());
                            }
                        }).show();
                return true;
            case HOME_RULE_URL:
                new PromptDialog(context)
                        .setDialogType(PromptDialog.DIALOG_TYPE_INFO)
                        .setAnimationEnable(true)
                        .setTitleText("温馨提示")
                        .setContentText("剪贴板检测到小程序远程规则（合集/单个规则），是否立即导入？")
                        .setPositiveListener("立即导入", dialog -> {
                            dialog.dismiss();
                            if (sss.length != 3) {
                                ToastMgr.shortBottomCenter(context, "格式错误！");
                                return;
                            }
                            String url = sss[2];
                            if (StringUtil.isEmpty(url) || (!url.startsWith("http") && !url.startsWith("file")) && !url.startsWith("hiker://")) {
                                ToastMgr.shortBottomCenter(context, "链接有误！");
                                return;
                            }
                            CodeUtil.get(url, new CodeUtil.OnCodeGetListener() {
                                @Override
                                public void onSuccess(String text) {
                                    if (StringUtil.isEmpty(text)) {
                                        ToastMgr.shortCenter(context, "出错：文件内容为空");
                                        return;
                                    }
                                    importRulesByTextWithDialog(context, text, url);
                                }

                                @Override
                                public void onFailure(int errorCode, String msg) {
                                    ToastMgr.shortBottomCenter(context, "导入失败，网络连接错误！");
                                }
                            });
                            ClipboardUtil.copyToClipboard(context, "");
                        }).show();
                return true;
            case BOOKMARK_URL:
                new PromptDialog(context)
                        .setDialogType(PromptDialog.DIALOG_TYPE_INFO)
                        .setAnimationEnable(true)
                        .setTitleText("温馨提示")
                        .setContentText("剪贴板检测到书签合集规则，是否立即导入？")
                        .setPositiveListener("立即导入", dialog -> {
                            dialog.dismiss();
                            if (sss.length != 3) {
                                ToastMgr.shortBottomCenter(context, "格式错误！");
                                return;
                            }
                            ClipboardUtil.copyToClipboard(context, "");
                            importRulesWithDialog(context, data -> {
                                if (StringUtil.isEmpty(sss[2]) || (!sss[2].startsWith("http") && !sss[2].startsWith("file")) && !sss[2].startsWith("hiker://")) {
                                    ToastMgr.shortBottomCenter(context, "链接有误！");
                                    return;
                                }
                                if (StringUtil.isNotEmpty(data)) {
                                    BackupUtil.backupDB(context, true);
                                }
                                LitePal.deleteAll(Bookmark.class);
                                importBookmarkRulesByUrl(context, sss[2]);
                            }, data -> {
                                if (StringUtil.isNotEmpty(data)) {
                                    BackupUtil.backupDB(context, true);
                                }
                                importBookmarkRulesByUrl(context, sss[2]);
                            }, null);
                        }).show();
                return true;
            case FILE_URL:
                new PromptDialog(context)
                        .setDialogType(PromptDialog.DIALOG_TYPE_INFO)
                        .setAnimationEnable(true)
                        .setTitleText("温馨提示")
                        .setContentText("剪贴板检测到本地文件导入请求，导入后会覆盖规则中的本地文件，是否立即导入？")
                        .setPositiveListener("立即导入", dialog -> {
                            dialog.dismiss();
                            if (sss.length != 3) {
                                ToastMgr.shortBottomCenter(context, "格式错误！");
                                return;
                            }
                            ClipboardUtil.copyToClipboard(context, "");
                            try {
                                importFileByUrl(context, sss[2]);
                            } catch (Exception e) {
                                e.printStackTrace();
                                ToastMgr.shortCenter(context, "出错：" + e.getMessage());
                            }
                        }).show();
                return true;
            case SEARCH_ENGINE_URL:
                new PromptDialog(context)
                        .setDialogType(PromptDialog.DIALOG_TYPE_INFO)
                        .setAnimationEnable(true)
                        .setTitleText("温馨提示")
                        .setContentText("剪贴板检测到搜索引擎合集规则，是否立即导入？")
                        .setPositiveListener("立即导入", dialog -> {
                            dialog.dismiss();
                            if (sss.length != 3) {
                                ToastMgr.shortBottomCenter(context, "格式错误！");
                                return;
                            }
                            if (FilterUtil.hasFilterWord(sss[2])) {
                                ToastMgr.shortBottomCenter(context, "规则含有违禁词，禁止导入");
                                return;
                            }
                            ClipboardUtil.copyToClipboard(context, "");
                            importRulesWithDialog(context, data -> {
                                if (StringUtil.isEmpty(sss[2]) || (!sss[2].startsWith("http") && !sss[2].startsWith("file")) && !sss[2].startsWith("hiker://")) {
                                    ToastMgr.shortBottomCenter(context, "链接有误！");
                                    return;
                                }
                                if (StringUtil.isNotEmpty(data)) {
                                    BackupUtil.backupDB(context, true);
                                }
                                LitePal.deleteAll(SearchEngineDO.class);
                                importSearchRulesByUrl(context, sss[2]);
                            }, data -> {
                                if (StringUtil.isNotEmpty(data)) {
                                    BackupUtil.backupDB(context, true);
                                }
                                importSearchRulesByUrl(context, sss[2]);
                            }, null);
                        }).show();
                return true;
            case PAGE_DETAIL_RULES:
                new PromptDialog(context)
                        .setDialogType(PromptDialog.DIALOG_TYPE_INFO)
                        .setAnimationEnable(true)
                        .setTitleText("温馨提示")
                        .setContentText("剪贴板检测到二级页面详情口令，是否立即查看？")
                        .setPositiveListener("立即查看", dialog -> {
                            dialog.dismiss();
                            try {
                                ClipboardUtil.copyToClipboard(context, "");
                                String[] rules = sss[2].split("@@");
                                if (rules.length != 2) {
                                    ToastMgr.shortCenter(context, "规则有误");
                                    return;
                                }
                                DetailPageRule pageRule = JSON.parseObject(Base64.decode(rules[1], Base64.NO_WRAP), DetailPageRule.class);
                                Intent intent = new Intent(context, FilmListActivity.class);
                                intent.putExtra("data", pageRule.getData());
                                intent.putExtra("picUrl", pageRule.getPicUrl());
                                intent.putExtra("title", pageRule.getTitle());
                                context.startActivity(intent);
                            } catch (Exception e) {
                                e.printStackTrace();
                                ToastMgr.shortCenter(context, "出错：" + e.getMessage());
                            }
                        }).show();
                return true;
            case SEARCH_ENGINE:
                ToastMgr.shortBottomCenter(context, "搜索引擎页面已下线，请用无解析规则的小程序规则代替！");
                return true;
            case JS_URL:
                new PromptDialog(context)
                        .setDialogType(PromptDialog.DIALOG_TYPE_INFO)
                        .setAnimationEnable(true)
                        .setTitleText("温馨提示")
                        .setContentText("剪贴板检测到网页插件规则信息，是否立即导入？")
                        .setPositiveListener("立即导入", dialog -> {
                            dialog.dismiss();
                            try {
                                ClipboardUtil.copyToClipboard(context, "");
                                String[] js = sss[2].split("@");
                                Log.d(TAG, "checkAutoText: sss[2]=" + sss[2] + "，js=" + Arrays.toString(js));
                                if (js.length < 2) {
                                    ToastMgr.shortBottomCenter(context, "规则有误！");
                                    return;
                                }
                                if (!JSManager.instance(context).hasJs(js[0])) {
                                    updateJsNow(context, js);
                                } else {
                                    new XPopup.Builder(context)
                                            .asConfirm("温馨提示", "确认更新插件“" + js[0] + "”吗？注意更新后原插件内容无法恢复！",
                                                    () -> updateJsNow(context, js)).show();
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                                ToastMgr.shortCenter(context, "出错：" + e.getMessage());
                            }
                        }).show();
                return true;
            default:
                return false;
        }
    }

    public static void importRulesByTextWithDialog(Context context, String text, String originalUrl) {
        String s = text.trim();
        if (s.startsWith("{") && s.endsWith("}")) {
            //单个规则
            importRuleByRuleText(context, s);
            return;
        }
        if (!s.startsWith("[") || !s.endsWith("]")) {
            ToastMgr.shortCenter(context, "口令有误，请确认导入链接是否有效");
            return;
        }
        //规则合集
        importRulesWithDialog(context, data -> {
            try {
                if (StringUtil.isNotEmpty(data)) {
                    BackupUtil.backupDB(context, true);
                }
                LitePal.deleteAll(ArticleListRule.class);
                importHomeRulesByText(context, s);
            } catch (Exception e) {
                e.printStackTrace();
                ToastMgr.shortCenter(context, "出错：" + e.getMessage());
            }
        }, data -> {
            try {
                if (StringUtil.isNotEmpty(data)) {
                    BackupUtil.backupDB(context, true);
                }
                importHomeRulesByText(context, s);
            } catch (Exception e) {
                e.printStackTrace();
                ToastMgr.shortCenter(context, "出错：" + e.getMessage());
            }
        }, s1 -> {
            ArticleListRule articleListRule1 = new ArticleListRule();
            articleListRule1.setUrl("hiker://empty");
            JSEngine.getInstance().putVar("rulesImportUrl", originalUrl);
            String rule = FilesInAppUtil.getAssetsString(context, "homeSubView.json");
            String findRule = JSON.parseObject(rule, ArticleListRuleJO.class).getDetail_find_rule();
            articleListRule1.setFind_rule(findRule);
            articleListRule1.setCol_type(ArticleColTypeEnum.TEXT_3.getCode());
            articleListRule1.setTitle("查看合集");
            Intent intent = new Intent(context, FilmListActivity.class);
            intent.putExtra("data", JSON.toJSONString(articleListRule1));
            intent.putExtra("title", "查看合集");
            context.startActivity(intent);
        });
    }

    private static void importRuleByRuleText(Context context, String ruleStr) {
        String rule = ruleStr;
        if (ruleStr.startsWith("base64://")) {
            try {
                String realRule = ruleStr.split("@")[2];
                rule = new String(Base64.decode(StringUtil.replaceLineBlank(realRule), Base64.NO_WRAP));
            } catch (Exception e) {
                ToastMgr.shortBottomCenter(context, "规则有误：" + e.getMessage());
                return;
            }
        }
        if (FilterUtil.hasFilterWord(rule)) {
            ToastMgr.shortBottomCenter(context, "规则含有违禁词，禁止导入");
            return;
        }
        ArticleListRuleJO ruleJO = null;
        try {
            ruleJO = JSON.parseObject(rule, ArticleListRuleJO.class);
        } catch (Exception e) {
            ToastMgr.shortBottomCenter(context, "规则有误：" + e.getMessage());
            return;
        }
        if (ruleJO == null || StringUtil.isEmpty(ruleJO.getTitle())) {
            return;
        }
        ruleJO.setOrder(0);
        //广告拦截规则
        if (StringUtil.isNotEmpty(ruleJO.getAdBlockUrls())) {
            ArticleListRuleJO finalRuleJO = ruleJO;
            String finalRule = rule;
            showWithAdUrlsDialog(context, "检测到该小程序规则附带了广告拦截规则，确定立即导入规则吗？", withAdUrls -> {
                Timber.d("checkAutoText: withAdUrls=%s", withAdUrls);
                if (withAdUrls) {
                    String[] urls = finalRuleJO.getAdBlockUrls().split("##");
                    HeavyTaskUtil.executeNewTask(() -> AdUrlBlocker.instance().addUrls(Arrays.asList(urls)));
                }
                Intent intent = new Intent(context, ArticleListRuleEditActivity.class);
                intent.putExtra("data", finalRule);
                context.startActivity(intent);
            });
        } else {
            Intent intent = new Intent(context, ArticleListRuleEditActivity.class);
            intent.putExtra("data", JSON.toJSONString(ruleJO));
            context.startActivity(intent);
        }
    }

    private static void updateJsNow(Context context, String[] js) {
        if (context instanceof Activity && ((Activity) context).isFinishing()) {
            return;
        }
        final ZLoadingDialog loadingDialog = DialogBuilder.createLoadingDialog(context, false);
        loadingDialog.show();
        if (js[1].startsWith("base64://")) {
            if ("base64://".equals(js[1])) {
                loadingDialog.dismiss();
                ToastMgr.shortBottomCenter(context, "规则有误！");
                return;
            }
            String decodeStr = null;
            try {
                decodeStr = new String(Base64.decode(StringUtil.replaceLineBlank(js[1]).replace("base64://", ""), Base64.NO_WRAP));
            } catch (Exception e) {
                Log.e(TAG, "checkAutoText: " + e.getMessage(), e);
                ToastMgr.shortBottomCenter(context, "BASE64解码失败！格式有误，请检查！");
                return;
            }
            boolean ok = JSManager.instance(context).updateJs(js[0], decodeStr);
            loadingDialog.dismiss();
            if (context instanceof Activity && ((Activity) context).isFinishing()) {
                return;
            }
            if (ok) {
                ToastMgr.shortBottomCenter(context, js[0] + "下的网页插件保存成功！");
            } else {
                ToastMgr.shortBottomCenter(context, js[0] + "下的网页插件保存失败！");
            }
            return;
        }
        CodeUtil.get(js[1], new CodeUtil.OnCodeGetListener() {
            @Override
            public void onSuccess(String s) {
                if (context instanceof Activity && ((Activity) context).isFinishing()) {
                    return;
                }
                boolean ok = JSManager.instance(context).updateJs(js[0], s);
                loadingDialog.dismiss();
                if (ok) {
                    ToastMgr.shortBottomCenter(context, js[0] + "下的网页插件保存成功！");
                } else {
                    ToastMgr.shortBottomCenter(context, js[0] + "下的网页插件保存失败！");
                }
            }

            @Override
            public void onFailure(int errorCode, String msg) {
                if (context instanceof Activity && ((Activity) context).isFinishing()) {
                    return;
                }
                loadingDialog.dismiss();
                ToastMgr.shortBottomCenter(context, "导入失败，网络连接错误！");
            }
        });
    }


    private static void importRulesWithDialog(Context context, OnOkListener deleteImportListener, OnOkListener onlyImportListener, Consumer<String> showDetail) {
        ImportRulesPopup popup = new ImportRulesPopup(context)
                .withDetail(showDetail != null)
                .withListener(new ImportRulesPopup.OkListener() {
                    @Override
                    public void ok(boolean backup, boolean delete) {
                        if (delete) {
                            new XPopup.Builder(context)
                                    .asConfirm("温馨提示", "确认要删除后再导入规则吗？", () -> {
                                        deleteImportListener.ok(backup ? "backup" : null);
                                    }).show();
                        } else {
                            onlyImportListener.ok(backup ? "backup" : null);
                        }
                    }

                    @Override
                    public void showDetail() {
                        if (showDetail != null) {
                            showDetail.accept(null);
                        }
                    }
                });
        new XPopup.Builder(context)
                .asCustom(popup)
                .show();
    }

    private interface OnOkListener {
        void ok(String data);
    }

    private static void importFileByUrl(Context context, String sss) {
        String[] urls = sss.split("@");
        if (urls.length != 2 || StringUtil.isEmpty(urls[0]) || StringUtil.isEmpty(urls[1])
                || (!urls[1].startsWith("http") && !urls[1].startsWith("file"))
                || (!urls[0].startsWith("hiker://files/") && !urls[0].startsWith("file://"))) {
            ToastMgr.shortBottomCenter(context, "规则有误！");
            return;
        }
        String filePath = urls[0];
        if (filePath.startsWith("hiker://files/")) {
            String fileName = filePath.replace("hiker://files/", "");
            filePath = UriUtils.getRootDir(context) + File.separator + fileName;
        } else if (filePath.startsWith("file://")) {
            filePath = filePath.replace("file://", "");
        }
        if (!filePath.startsWith(UriUtils.getRootDir(context))) {
            ToastMgr.shortBottomCenter(context, "规则有误！");
            return;
        }
        String finalFilePath = filePath;
        CodeUtil.get(urls[1], new CodeUtil.OnCodeGetListener() {
            @Override
            public void onSuccess(String s) {
                if (StringUtil.isEmpty(s)) {
                    s = "";
                }
                if (FilterUtil.hasFilterWord(s)) {
                    ToastMgr.shortBottomCenter(context, "规则含有违禁词，禁止导入");
                    return;
                }
                try {
                    FileUtil.stringToFile(s, finalFilePath);
                    ToastMgr.shortBottomCenter(context, "导入成功，内容已保存到" + finalFilePath);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(int errorCode, String msg) {
                ToastMgr.shortBottomCenter(context, "导入失败，网络连接错误！");
            }
        });
    }

    private static void importSearchRulesByUrl(Context context, String url) {
        if (StringUtil.isEmpty(url) || (!url.startsWith("http") && !url.startsWith("file")) && !url.startsWith("hiker://")) {
            ToastMgr.shortBottomCenter(context, "链接有误！");
            return;
        }
        CodeUtil.get(url, new CodeUtil.OnCodeGetListener() {
            @Override
            public void onSuccess(String s) {
                if (StringUtil.isEmpty(s)) {
                    ToastMgr.shortBottomCenter(context, "导入失败，规则为空");
                    return;
                }
                if (FilterUtil.hasFilterWord(s)) {
                    ToastMgr.shortBottomCenter(context, "规则含有违禁词，禁止导入");
                    return;
                }
                List<SearchRuleJO> ruleJOList = JSON.parseArray(s, SearchRuleJO.class);
                if (CollectionUtil.isEmpty(ruleJOList)) {
                    ToastMgr.shortBottomCenter(context, "导入失败，规则为空");
                    return;
                }
                int c = LitePal.count(SearchEngineDO.class);
                if (ruleJOList.size() > 800 || c > 800) {
                    ToastMgr.shortBottomCenter(context, "规则数目过多，已有规则" + c + "条，拟导入规则" + ruleJOList.size() + "条，均不能超过800条！");
                    return;
                }
                GlobalDialogActivity.startLoading(context, "正在导入中，共" + ruleJOList.size() + "条规则");
                HeavyTaskUtil.executeNewTask(() -> {
                    for (int i = 0; i < ruleJOList.size(); i++) {
                        ruleJOList.get(i).toEngineDO(false).save();
                    }
                    EventBus.getDefault().postSticky(new LoadingDismissEvent("已成功导入" + ruleJOList.size() + "条规则"));
                });
            }

            @Override
            public void onFailure(int errorCode, String msg) {
                ToastMgr.shortBottomCenter(context, "导入失败，网络连接错误！");
            }
        });
    }

    private static void importBookmarkRulesByUrl(Context context, String url) {
        if (StringUtil.isEmpty(url) || (!url.startsWith("http") && !url.startsWith("file")) && !url.startsWith("hiker://")) {
            ToastMgr.shortBottomCenter(context, "链接有误！");
            return;
        }
        CodeUtil.get(url, new CodeUtil.OnCodeGetListener() {
            @Override
            public void onSuccess(String s) {
                importBookmarkByStr(context, s);
            }

            @Override
            public void onFailure(int errorCode, String msg) {
                ToastMgr.shortBottomCenter(context, "导入失败，网络连接错误！");
            }
        });
    }

    public static void importBookmarkByStr(Context context, String s) {
        if (StringUtil.isEmpty(s)) {
            ToastMgr.shortBottomCenter(context, "导入失败，规则为空");
            return;
        }
        List<Bookmark> ruleJOList = JSON.parseArray(s, Bookmark.class);
        if (CollectionUtil.isEmpty(ruleJOList)) {
            ToastMgr.shortBottomCenter(context, "导入失败，规则为空");
            return;
        }

        GlobalDialogActivity.startLoading(context, "正在导入中，共" + ruleJOList.size() + "条规则");
        HeavyTaskUtil.executeNewTask(() -> {
            int count = 0;
            for (int i = 0; i < ruleJOList.size(); i++) {
                if (StringUtil.isEmpty(ruleJOList.get(i).getUrl())) {
                    continue;
                }
                count++;
                List<Bookmark> rule = LitePal.where("url = ?", ruleJOList.get(i).getUrl()).find(Bookmark.class);
                if (CollectionUtil.isNotEmpty(rule)) {
                    String group = rule.get(0).getGroup();
                    rule.get(0).setGroup(group);
                    rule.get(0).setTitle(ruleJOList.get(i).getTitle());
                    rule.get(0).save();
                } else {
                    ruleJOList.get(i).save();
                }
            }
            EventBus.getDefault().postSticky(new LoadingDismissEvent("已成功导入" + count + "条规则"));
            EventBus.getDefault().post(new BookmarkRefreshEvent());
        });
    }

    private static void importHomeRulesByText(Context context, String s) {
        if (StringUtil.isEmpty(s)) {
            ToastMgr.shortBottomCenter(context, "导入失败，规则为空");
            return;
        }
        if (FilterUtil.hasFilterWord(s)) {
            ToastMgr.shortBottomCenter(context, "规则含有违禁词，禁止导入");
            return;
        }
        List<ArticleListRuleJO> ruleJOList = JSON.parseArray(s, ArticleListRuleJO.class);
        if (CollectionUtil.isEmpty(ruleJOList)) {
            ToastMgr.shortBottomCenter(context, "导入失败，规则为空");
            return;
        }
        int c = LitePal.count(ArticleListRule.class);
        if (ruleJOList.size() > 800 || c > 800) {
            ToastMgr.shortBottomCenter(context, "规则数目过多，已有规则" + c + "条，欲导入规则" + ruleJOList.size() + "条，均不能超过800条！");
            return;
        }
        GlobalDialogActivity.startLoading(context, "正在导入中，共" + ruleJOList.size() + "条规则");
        HeavyTaskUtil.executeNewTask(() -> {
            for (int i = 0; i < ruleJOList.size(); i++) {
                List<ArticleListRule> rule = LitePal.where("title = ?", ruleJOList.get(i).getTitle()).find(ArticleListRule.class);
                if (CollectionUtil.isNotEmpty(rule)) {
                    String color = rule.get(0).getTitleColor();
                    String group = rule.get(0).getGroup();
                    rule.get(0).fromJO(ruleJOList.get(i));
                    rule.get(0).setTitleColor(color);
                    rule.get(0).setGroup(group);
                    rule.get(0).save();
                } else {
                    new ArticleListRule().fromJO(ruleJOList.get(i)).save();
                }
            }
            EventBus.getDefault().postSticky(new LoadingDismissEvent("已成功导入" + ruleJOList.size() + "条规则"));
            EventBus.getDefault().post(new OnArticleListRuleChangedEvent());
        });
    }


    private static void importHomeRulesByUrl(Context context, String url) {
        if (StringUtil.isEmpty(url) || (!url.startsWith("http") && !url.startsWith("file")) && !url.startsWith("hiker://")) {
            ToastMgr.shortBottomCenter(context, "链接有误！");
            return;
        }
        CodeUtil.get(url, new CodeUtil.OnCodeGetListener() {
            @Override
            public void onSuccess(String s) {
                importHomeRulesByText(context, s);
            }

            @Override
            public void onFailure(int errorCode, String msg) {
                ToastMgr.shortBottomCenter(context, "导入失败，网络连接错误！");
            }
        });
    }

    /**
     * 导入方圆的规则
     *
     * @param context
     * @param shareText
     * @return
     */
    private static boolean checkAutoTextByFangYuan(Context context, String shareText) {
        final String[] sss = shareText.split("￥");
        if (sss.length < 2) {
            return false;
        }
        switch (sss[1]) {
            case "source":
                //搜索引擎
                if (sss.length > 3) {
                    new PromptDialog(context)
                            .setDialogType(PromptDialog.DIALOG_TYPE_INFO)
                            .setAnimationEnable(true)
                            .setTitleText("温馨提示")
                            .setContentText("剪贴板检测到搜索引擎规则，是否立即导入？")
                            .setPositiveListener("立即导入", dialog -> {
                                dialog.dismiss();
                                importFangYuanSources(context, shareText);
                                ClipboardUtil.copyToClipboard(context, "");
                            }).show();
                    return true;
                }
                if (sss.length != 3) {
                    return false;
                }
                MovieInfoUse movieInfoUse = null;
                try {
                    movieInfoUse = JSON.parseObject(sss[2], MovieInfoUse.class);
                } catch (Exception e) {
                    ToastMgr.shortBottomCenter(context, "规则有误：" + e.getMessage());
                    return false;
                }
                if (StringUtil.isEmpty(movieInfoUse.getTitle()) || StringUtil.isEmpty(movieInfoUse.getSearchUrl())) {
                    return false;
                }
                String[] s = movieInfoUse.getTitle().split("（");
                SearchRuleJO searchRuleJO = new SearchRuleJO();
                searchRuleJO.setTitle(movieInfoUse.getTitle());
                if (s.length > 1) {
                    searchRuleJO.setGroup(s[1].split("）")[0]);
                }
                searchRuleJO.setSearch_url(movieInfoUse.getSearchUrl());
                searchRuleJO.setFind_rule(movieInfoUse.getSearchFind());
                String rule = JSON.toJSONString(searchRuleJO);
                String shareRulePrefix = PreferenceMgr.getString(context, "shareRulePrefix", "");
                String command = getCommand(shareRulePrefix, rule, SEARCH_ENGINE);
                return checkAutoText(context, command);
        }
        return false;
    }

    /**
     * 批量从方圆的规则导入搜索引擎
     *
     * @param context
     * @param shareText
     * @return
     */
    private static boolean importFangYuanSources(Context context, String shareText) {
        String[] all = shareText.split(SOURCE);
        int k = 0;
        if (TextUtils.isEmpty(all[0])) {
            k = 1;
        }
        if (all.length - k < 2) {
            return false;
        }
        int c = LitePal.count(SearchEngineDO.class);
        if (c > 800) {
            ToastMgr.shortBottomCenter(context, "搜索引擎规则大于800条，不能导入");
            return false;
        }
        int count = 0;
        for (int i = k; i < all.length; i++) {
            String text = StringUtil.replaceBlank(all[i]);
            MovieInfoUse movieInfoUse = null;
            try {
                movieInfoUse = JSON.parseObject(text, MovieInfoUse.class);
            } catch (Exception e) {
                continue;
            }
            if (StringUtil.isEmpty(movieInfoUse.getTitle()) || StringUtil.isEmpty(movieInfoUse.getSearchUrl())) {
                continue;
            }
            String[] s = movieInfoUse.getTitle().split("（");
            SearchRuleJO searchRuleJO = new SearchRuleJO();
            searchRuleJO.setTitle(movieInfoUse.getTitle());
            if (s.length > 1) {
                searchRuleJO.setGroup(s[1].split("）")[0]);
            }
            searchRuleJO.setSearch_url(movieInfoUse.getSearchUrl());
            searchRuleJO.setFind_rule(movieInfoUse.getSearchFind());
            if (importSearchRule(JSON.toJSONString(searchRuleJO))) {
                count++;
            }
        }
        ClipboardUtil.copyToClipboard(context, "");
        if (count <= 0) {
            return false;
        } else {
            ToastMgr.shortBottomCenter(context, "已导入" + count + "条搜索引擎规则");
            return true;
        }
    }

    /**
     * 批量导入搜索引擎
     *
     * @param context
     * @param shareText
     */
    private static void importSearchRules(Context context, String shareText) {
        Log.d(TAG, "importSearchRules: " + shareText);
        int c = LitePal.count(SearchEngineDO.class);
        if (c > 800) {
            ToastMgr.shortBottomCenter(context, "搜索引擎规则大于800条，不能导入");
            return;
        }
        String shareRulePrefix = PreferenceMgr.getString(context, "shareRulePrefix", "");
        String[] all = shareText.split(getCommand(shareRulePrefix, "", SEARCH_ENGINE));
        int k = 0;
        if (TextUtils.isEmpty(all[0])) {
            k = 1;
        }
        if (all.length - k < 2) {
            return;
        }
        int count = 0;
        for (int i = k; i < all.length; i++) {
            String s = StringUtil.replaceBlank(all[i]);
            if (importSearchRule(s)) {
                count++;
            }
        }
        ClipboardUtil.copyToClipboard(context, "");
        ToastMgr.shortBottomCenter(context, "已导入" + count + "条搜索引擎规则");
    }

    /**
     * 导入单条搜索引擎
     *
     * @param rule
     * @return
     */
    private static boolean importSearchRule(String rule) {
        Log.d(TAG, "importSearchRule: " + rule);
        SearchRuleJO searchRuleJO = null;
        try {
            searchRuleJO = JSON.parseObject(rule, SearchRuleJO.class);
        } catch (Exception e) {
            return false;
        }
        if (searchRuleJO == null || StringUtil.isEmpty(searchRuleJO.getTitle())) {
            return false;
        }
        SearchEngineDO engineDO = searchRuleJO.toEngineDO(false);
        engineDO.save();
        //广告拦截规则
        if (StringUtil.isNotEmpty(searchRuleJO.getAdBlockUrls())) {
            String[] urls = searchRuleJO.getAdBlockUrls().split("##");
            HeavyTaskUtil.executeNewTask(() -> AdUrlBlocker.instance().addUrls(Arrays.asList(urls)));
        }
        return true;
    }

    public static String getShareRule() {
        return shareRule;
    }

    public static void setShareRule(String shareRule) {
        AutoImportHelper.shareRule = shareRule;
    }


    public static void showWithAdUrlsDialog(Context context, String title, OnOkWithAdUrlsListener listener) {
        final View view1 = LayoutInflater.from(context).inflate(R.layout.view_dialog_import_with_block_urls, null, false);
        final TextView titleE = view1.findViewById(R.id.import_rule_title);
        titleE.setText(title);
        titleE.setTag("with");
        final AlertDialog alertDialog = new AlertDialog.Builder(context)
                .setTitle("温馨提示")
                .setView(view1)
                .setCancelable(true)
                .setPositiveButton("确定", (dialog, which) -> {
                    String with = (String) titleE.getTag();
                    Log.d(TAG, "showWithAdUrlsDialog: " + with);
                    listener.ok("with".equals(with));
                    dialog.dismiss();
                }).setNegativeButton("取消", (dialog, which) -> dialog.dismiss()).create();
        Button import_rule_btn = view1.findViewById(R.id.import_rule_btn);
        Button not_import_rule_btn = view1.findViewById(R.id.not_import_rule_btn);
        import_rule_btn.setOnClickListener(v -> {
            titleE.setTag("with");
            import_rule_btn.setBackground(context.getResources().getDrawable(R.drawable.button_layer_red));
            not_import_rule_btn.setBackground(context.getResources().getDrawable(R.drawable.button_layer));
        });
        not_import_rule_btn.setOnClickListener(v -> {
            titleE.setTag("without");
            import_rule_btn.setBackground(context.getResources().getDrawable(R.drawable.button_layer));
            not_import_rule_btn.setBackground(context.getResources().getDrawable(R.drawable.button_layer_red));
        });
        alertDialog.show();
    }

    public interface OnOkWithAdUrlsListener {
        void ok(boolean withAdUrls);
    }
}

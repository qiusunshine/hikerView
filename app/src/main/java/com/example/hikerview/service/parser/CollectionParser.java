package com.example.hikerview.service.parser;

import android.app.Activity;

import com.example.hikerview.ui.home.model.ArticleListRule;
import com.example.hikerview.utils.HeavyTaskUtil;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/**
 * @author reborn
 * @program hiker-view
 * @description 解析收藏相关规则
 * @create 2021-06-18 21:43
 **/
public class CollectionParser {
    public static void parse(Activity context, String url, ArticleListRule articleListRule, String rule,
                             BaseParseCallback<String> callback) {
        callback.start();
        url = HttpParser.getUrlAppendUA(url, articleListRule.getUa());
        url = CommonParser.parsePageClassUrl(url, 1, articleListRule);
        HttpParser.parseSearchUrlForHtml(url, new HttpParser.OnSearchCallBack() {
            @Override
            public void onSuccess(String url, String res) {
                HeavyTaskUtil.executeNewTask(() -> {
                    if (rule.startsWith("js:")) {
                        JsEngineBridge.parseLastChapterCallback(url, res, articleListRule, rule, callback);
                        return;
                    }
                    try {
                        Document doc = Jsoup.parse(res);
                        String[] rs = rule.split("&&");
                        Element element;
                        if (rs.length == 1) {
                            element = doc;
                        } else {
                            element = CommonParser.getTrueElement(rs[0], doc);
                        }
                        for (int i = 1; i < rs.length - 1; i++) {
                            element = CommonParser.getTrueElement(rs[i], element);
                        }
                        String result = CommonParser.getText(element, rs[rs.length - 1]);
                        if (context != null && !context.isFinishing()) {
                            context.runOnUiThread(() -> {
                                callback.success(result);
                            });
                        }
                    } catch (Exception e) {
                        if (context != null && !context.isFinishing()) {
                            context.runOnUiThread(() -> {
                                // DebugUtil.showErrorMsg(context, e);
                                callback.error("获取更新失败，请检查规则是否有误或原站不允许访问！");
                            });
                        }
                    }
                });
            }

            @Override
            public void onFailure(int errorCode, String msg) {
                if (context != null && !context.isFinishing()) {
                    context.runOnUiThread(() -> {
//                        DebugUtil.showErrorMsg(context, new Exception(msg));
                        callback.error("获取更新失败，请检查网络！");
                    });
                }
            }
        });
    }

}

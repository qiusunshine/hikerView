package com.example.hikerview.service.parser;

import android.app.Activity;

import com.example.hikerview.model.MovieRule;
import com.example.hikerview.utils.DebugUtil;
import com.example.hikerview.utils.HeavyTaskUtil;
import com.example.hikerview.utils.StringUtil;
import com.example.hikerview.utils.ToastMgr;

import org.adblockplus.libadblockplus.android.Utils;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/**
 * 作者：By 15968
 * 日期：On 2021/1/3
 * 时间：At 11:40
 */

public class LazyRuleParser {

    private static void clearLazyTag(String[] lazyRule) {
        if (StringUtil.isEmpty(lazyRule[0])) {
            return;
        }
        String[] tagList = new String[]{
                "#noLoading#"
        };
        for (String tag : tagList) {
            lazyRule[0] = StringUtils.replaceOnce(lazyRule[0], tag, "");
        }
    }

    private static void parseByJs(String prefix, Activity context, Object rule, String[] lazyRule, String myUrl, BaseParseCallback<String> callback) {
        HeavyTaskUtil.executeNewTask(() -> {
            try {
                String res = JSEngine.getInstance().evalJS(JSEngine.getMyRule(rule) + JSEngine.getInstance().generateMY("MY_URL", Utils.escapeJavaScriptString(myUrl))
                        + StringUtils.replaceOnce(lazyRule[1], prefix, ""), lazyRule[0]);
                if (context != null && !context.isFinishing()) {
                    context.runOnUiThread(() -> {
                        callback.success(res);
                    });
                }
            } catch (Exception e) {
                if (context != null && !context.isFinishing()) {
                    context.runOnUiThread(() -> {
                        DebugUtil.showErrorMsg(context, e);
                        callback.error(e.getMessage());
                    });
                }
            }
        });
    }

    public static void parse(Activity context, Object rule, String[] lazyRule, String codeAndHeader, String myUrl, BaseParseCallback<String> callback) {
        if (lazyRule.length != 2) {
            ToastMgr.shortBottomCenter(context, "动态解析规则有误");
            return;
        }
        callback.start();
        clearLazyTag(lazyRule);
        if (lazyRule[1].startsWith(".js:")) {
            parseByJs(".js:", context, rule, lazyRule, myUrl, callback);
        } else if (lazyRule[1].startsWith("js:")) {
            parseByJs("js:", context, rule, lazyRule, myUrl, callback);
        } else {
            String urlTmp = lazyRule[0].contains(";") ? lazyRule[0] : lazyRule[0] + codeAndHeader;
            HttpParser.parseSearchUrlForHtml(urlTmp, new HttpParser.OnSearchCallBack() {
                @Override
                public void onSuccess(String url1, String s) {
                    HeavyTaskUtil.executeNewTask(() -> {
                        try {
                            Document doc = Jsoup.parse(s);
                            String[] ss6 = lazyRule[1].split("&&");
                            Element element5;
                            if (ss6.length == 1) {
                                element5 = doc;
                            } else {
                                element5 = CommonParser.getTrueElement(ss6[0], doc);
                            }
                            for (int i = 1; i < ss6.length - 1; i++) {
                                element5 = CommonParser.getTrueElement(ss6[i], element5);
                            }
                            MovieRule movieRule = new MovieRule();
                            movieRule.setBaseUrl(StringUtil.getBaseUrl(lazyRule[0]));
                            movieRule.setChapterUrl(myUrl);
                            String res = CommonParser.getUrl(element5, ss6[ss6.length - 1], movieRule, lazyRule[0]);
                            if (context != null && !context.isFinishing()) {
                                context.runOnUiThread(() -> {
                                    callback.success(res);
                                });
                            }
                        } catch (Exception e) {
                            if (context != null && !context.isFinishing()) {
                                context.runOnUiThread(() -> {
                                    DebugUtil.showErrorMsg(context, e);
                                    callback.error(e.getMessage());
                                });
                            }
                        }
                    });
                }

                @Override
                public void onFailure(int errorCode, String msg) {
                    if (context != null && !context.isFinishing()) {
                        context.runOnUiThread(() -> {
                            DebugUtil.showErrorMsg(context, new Exception(msg));
                            callback.error(msg);
                        });
                    }
                }
            });
        }
    }
}

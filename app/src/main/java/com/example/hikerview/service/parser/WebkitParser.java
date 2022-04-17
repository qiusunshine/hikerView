package com.example.hikerview.service.parser;

import android.app.Activity;
import android.content.Context;
import android.webkit.WebView;

import com.annimon.stream.function.Consumer;
import com.example.hikerview.ui.home.model.article.extra.X5Extra;
import com.example.hikerview.ui.home.webview.ArticleWebkitHolder;
import com.example.hikerview.utils.StringUtil;
import com.example.hikerview.utils.ToastMgr;

import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;

import timber.log.Timber;

/**
 * 作者：By 15968
 * 日期：On 2021/9/20
 * 时间：At 14:42
 */

public class WebkitParser extends IWebViewParser {
    private ArticleWebkitHolder webViewHolder;
    private static WebkitParser instance;

    public static WebkitParser newInstance() {
        if (instance != null) {
            instance.destroy();
        }
        instance = new WebkitParser();
        return instance;
    }

    public static synchronized boolean finishParse(Context context, String url, String tk) {
        if (instance != null) {
            if (tk.equals(String.valueOf(instance.hashCode()))) {
                Timber.d("finishParse, ticket: %s", tk);
                return instance.finishParseInner(context, url);
            } else {
                Timber.d("ticket已失效: %s, new: %s", tk, String.valueOf(instance.hashCode()));
            }
        }
        return false;
    }

    public static synchronized void destroy0() {
        if (instance != null) {
            instance.destroy();
            instance = null;
        }
    }

    public static boolean canParse(String url) {
        return StringUtil.isNotEmpty(url) && url.startsWith("webRule://");
    }

    public static synchronized boolean parse0(Activity activity, String url, String extra, Consumer<String> consumer) {
        if(!canParse(url)){
            return false;
        }
        destroy0();
        WebkitParser parser = newInstance();
        return parser.parse(activity, url, extra, consumer);
    }


    @Override
    public void destroy() {
        if (webViewHolder != null && webViewHolder.getWebView() != null) {
            webViewHolder.getWebView().onPause();
            webViewHolder.getWebView().destroy();
            webViewHolder = null;
        }
    }

    @Override
    public boolean parse(Activity activity, String url, String extra, Consumer<String> consumer) {
        String title = StringUtils.replaceOnceIgnoreCase(url, "webRule://", "");
        String[] rules = title.split("@");
        //即使destroy也不会马上停止js的执行
        destroy();
        if (rules.length < 2) {
            ToastMgr.shortBottomCenter(activity, "规则有误！" + url);
            return false;
        }
        this.consumer = consumer;
        String loadUrl = rules[0];
        String innerJs = StringUtil.arrayToString(rules, 1, "@");
        X5Extra x5Extra = generateExtra(extra, innerJs, "webkit", String.valueOf(hashCode()));
        WebView webView = new WebView(activity);
        webViewHolder = new ArticleWebkitHolder();
        webViewHolder.setWebView(webView);
        webViewHolder.initWebView(activity);
        if (StringUtil.isNotEmpty(x5Extra.getUa())) {
            webViewHolder.getWebView().getSettings().setUserAgentString(x5Extra.getUa());
        }
        webViewHolder.setX5Extra(x5Extra);
        if (StringUtil.isNotEmpty(x5Extra.getReferer())) {
            Map<String, String> headers = new HashMap<>();
            headers.put("Referer", x5Extra.getReferer());
            webViewHolder.getWebView().loadUrl(loadUrl, headers);
        } else {
            webViewHolder.getWebView().loadUrl(loadUrl);
        }
        return true;
    }

    @Override
    public boolean finishParseInner(Context context, String url) {
        if (webViewHolder != null && webViewHolder.getWebView() != null) {
            destroy();
            if (consumer != null) {
                consumer.accept(url);
            }
            if (StringUtil.isEmpty(url)) {
                ToastMgr.shortBottomCenter(context, "解析失败，链接为空");
                return false;
            }
            return true;
        }
        return false;
    }
} 
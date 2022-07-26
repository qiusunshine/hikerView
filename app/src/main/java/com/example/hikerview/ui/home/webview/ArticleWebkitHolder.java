package com.example.hikerview.ui.home.webview;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Bitmap;
import android.net.http.SslError;
import android.os.Build;
import android.text.TextUtils;
import android.webkit.CookieManager;
import android.webkit.HttpAuthHandler;
import android.webkit.JsPromptResult;
import android.webkit.JsResult;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.example.hikerview.R;
import com.example.hikerview.ui.Application;
import com.example.hikerview.ui.browser.model.AdBlockModel;
import com.example.hikerview.ui.browser.model.AdUrlBlocker;
import com.example.hikerview.ui.browser.util.CollectionUtil;
import com.example.hikerview.ui.browser.webview.JsBridgeHolder;
import com.example.hikerview.ui.browser.webview.JsPluginHelper;
import com.example.hikerview.ui.browser.webview.WebViewWrapper;
import com.example.hikerview.ui.view.popup.InputPopup;
import com.example.hikerview.utils.StringUtil;
import com.example.hikerview.utils.ThreadTool;
import com.lxj.xpopup.XPopup;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import timber.log.Timber;

/**
 * 作者：By 15968
 * 日期：On 2021/1/30
 * 时间：At 20:55
 */

public class ArticleWebkitHolder extends IArticleWebHolder {
    private List<String> urls = Collections.synchronizedList(new ArrayList<>());
    private Map<String, Map<String, String>> requestHeaderMap = new HashMap<>();

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public List<String> getUrls() {
        return urls;
    }

    public void setUrls(List<String> urls) {
        this.urls = urls;
    }

    public ProgressListener getProgressListener() {
        return progressListener;
    }

    public void setProgressListener(ProgressListener progressListener) {
        this.progressListener = progressListener;
    }

    @Override
    protected void setUserAgentString(String ua) {
        if (webView != null) {
            webView.getSettings().setUserAgentString(ua);
        }
    }

    @Override
    protected String getUserAgentString() {
        if (webView != null) {
            return webView.getSettings().getUserAgentString();
        }
        return "";
    }

    public ArticleWebkitHolder() {

    }

    public ArticleWebkitHolder(WebView webView) {
        this.webView = webView;
    }

    /**
     * webView
     */
    private WebView webView;
    /**
     * 链接
     */
    private String url;

    private int height;
    private AtomicBoolean hasLoadJsOnProgress = new AtomicBoolean(false);
    private AtomicBoolean hasLoadJsOnPageEnd = new AtomicBoolean(false);

    private ProgressListener progressListener;

    public WebView getWebView() {
        return webView;
    }

    public void setWebView(WebView webView) {
        this.webView = webView;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        if (reference != null && reference.get() != null && reference.get().getResources().getString(R.string.home_domain).equals(StringUtil.getDom(url))) {
            url = url.replace(reference.get().getResources().getString(R.string.home_domain), reference.get().getResources().getString(R.string.home_ip));
        }
        this.url = url;
    }

    public void initWebView(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            String processName = Application.getProcessName();
            if (!activity.getPackageName().equals(processName)) {
                //判断是否是默认进程名称
                WebView.setDataDirectorySuffix(processName);
            }
        }
        initWebSettings(webView.getSettings());
        systemUA = webView.getSettings().getUserAgentString();
        webView.setWebChromeClient(new WebChromeClient() {

            @Override
            public void onProgressChanged(WebView webView, int i) {
                if (progressListener != null) {
                    progressListener.onProgressChanged(webView.getUrl(), i);
                }
                super.onProgressChanged(webView, i);
                if (!hasLoadJsOnProgress.get() && i >= 40 && i < 100) {
                    hasLoadJsOnProgress.set(true);
                    loadAllJs(webView, webView.getUrl(), false);
                    if (x5Extra != null && StringUtil.isNotEmpty(x5Extra.getJs()) && x5Extra.isJsLoadingInject()) {
                        webView.evaluateJavascript(x5Extra.getJs(), null);
                    }
                }
            }

            @Override
            public boolean onJsAlert(WebView webView, String url, String message, final JsResult result) {
                if (reference.get() == null || reference.get().isFinishing()) {
                    result.cancel();
                    return true;
                }
                new XPopup.Builder(reference.get())
                        .dismissOnBackPressed(false)
                        .dismissOnTouchOutside(false)
                        .asConfirm("网页提示", message, result::confirm, result::cancel).show();
                return true;
            }

            @Override
            public boolean onJsConfirm(WebView webView, String url, String message, final JsResult result) {
                if (reference.get() == null || reference.get().isFinishing()) {
                    result.cancel();
                    return true;
                }
                new XPopup.Builder(reference.get())
                        .dismissOnBackPressed(false)
                        .dismissOnTouchOutside(false)
                        .asConfirm("网页提示", message, result::confirm, result::cancel).show();
                return true;
            }

            @Override
            public boolean onJsPrompt(WebView webView, String url, String message, String defaultValue, final JsPromptResult result) {
                if (reference.get() == null || reference.get().isFinishing()) {
                    result.cancel();
                    return true;
                }
                new XPopup.Builder(reference.get())
                        .dismissOnBackPressed(false)
                        .dismissOnTouchOutside(false)
                        .asInputConfirm("来自网页的输入请求", message, defaultValue, null, result::confirm, result::cancel, R.layout.xpopup_confirm_input).show();
                return true;
            }
        });
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onReceivedSslError(WebView webView, SslErrorHandler sslErrorHandler, SslError sslError) {
                sslErrorHandler.proceed();
            }

            @Override
            public void onReceivedHttpAuthRequest(WebView view, HttpAuthHandler handler, String host, String realm) {
                String username = null;
                String password = null;

                boolean reuseHttpAuthUsernamePassword = handler
                        .useHttpAuthUsernamePassword();

                if (reuseHttpAuthUsernamePassword && view != null) {
                    String[] credentials = view.getHttpAuthUsernamePassword(host,
                            realm);
                    if (credentials != null && credentials.length == 2) {
                        username = credentials[0];
                        password = credentials[1];
                    }
                }

                if (username != null && password != null) {
                    handler.proceed(username, password);
                } else {
                    if (view != null && reference.get() != null && !reference.get().isFinishing()) {
                        InputPopup inputPopup = new InputPopup(reference.get())
                                .bind("网页登录", "用户名", "", "密码", "", (title, code) -> {
                                    if (StringUtil.isNotEmpty(title) && StringUtil.isNotEmpty(code)) {
                                        view.setHttpAuthUsernamePassword(host, realm, title, code);
                                    }
                                    handler.proceed(title, code);
                                }).setCancelListener(handler::cancel);
                        new XPopup.Builder(reference.get())
                                .dismissOnBackPressed(false)
                                .dismissOnTouchOutside(false)
                                .asCustom(inputPopup)
                                .show();
                    } else {
                        handler.cancel();
                    }
                }
            }

            @Override
            public void onPageStarted(WebView webView, String s, Bitmap bitmap) {
                if (progressListener != null) {
                    progressListener.onPageStarted(s);
                }
                requestHeaderMap.clear();
                urls.clear();
                hasLoadJsOnProgress.set(false);
                hasLoadJsOnPageEnd.set(false);
                checkUa(s);
                super.onPageStarted(webView, s, bitmap);
            }

            @Override
            public void onPageFinished(WebView webView, String s) {
                if (progressListener != null) {
                    progressListener.onPageFinished(s);
                }
                if (!hasLoadJsOnPageEnd.get()) {
                    hasLoadJsOnPageEnd.set(true);
                    loadAllJs(webView, s, true);
                }
                if (x5Extra != null && StringUtil.isNotEmpty(x5Extra.getJs())) {
                    webView.evaluateJavascript(x5Extra.getJs(), null);
                }
                if(finishPageConsumer != null){
                    finishPageConsumer.accept(s);
                }
                super.onPageFinished(webView, s);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView webView, WebResourceRequest request) {
                String url = request.getUrl().toString();
                if (url.startsWith("http")) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        if(request.isRedirect() && x5Extra != null && !x5Extra.isRedirect()){
                            return true;
                        }
                    }
                    if (Build.VERSION.SDK_INT < 26) {
                        webView.loadUrl(url);
                        return true;
                    }
                    return false;
                } else {
                    dealLoadUrl(url);
                    return true;
                }
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView webView, WebResourceRequest request) {
                String url = request.getUrl().toString();
                requestHeaderMap.put(url, request.getRequestHeaders());
                urls.add(url);
                long id = AdUrlBlocker.instance().shouldBlock(lastDom, url);
                Timber.d("ad block, url:%s, id:%d", url, id);
                if (id >= 0) {
                    return new WebResourceResponse(null, null, null);
                }
                if (getX5Extra() != null && CollectionUtil.isNotEmpty(getX5Extra().getBlockRules())) {
                    if (AdUrlBlocker.shouldBlock(getX5Extra().getBlockRules(), lastDom, url)) {
                        //x5自带的拦截
                        return new WebResourceResponse(null, null, null);
                    }
                }
                return super.shouldInterceptRequest(webView, request);
            }
        });
        if (reference != null) {
            reference.clear();
        }
        reference = new WeakReference<>(activity);
        jsBridgeHolder = new JsBridgeHolder(reference, new WebViewWrapper() {
            @Override
            public String getTitle() {
                return webView.getTitle();
            }

            @Override
            public String getUrl() {
                return webView.getUrl();
            }

            @Override
            public List<String> getUrls() {
                return urls;
            }

            @Override
            public String getMyUrl() {
                return ThreadTool.INSTANCE.getStrOnUIThread(urlHolder -> {
                    urlHolder.setUrl(webView.getUrl());
                });
            }

            @Override
            public void evaluateJavascript(String script) {
                webView.evaluateJavascript(script, null);
            }

            @Override
            public String getSystemUa() {
                return systemUA;
            }

            @Override
            public String getUserAgentString() {
                return ThreadTool.INSTANCE.getStrOnUIThread(urlHolder -> {
                    urlHolder.setUrl(webView.getSettings().getUserAgentString());
                });
            }

            @Override
            public void setUserAgentString(String userAgentString) {
                webView.getSettings().setUserAgentString(userAgentString);
            }

            @Override
            public void reload() {
                webView.reload();
            }

            @Override
            public boolean isOnPause() {
                return reference.get().isFinishing();
            }

            @Override
            public void loadUrl(String url) {
                webView.loadUrl(url);
            }

            @Override
            public void updateLastDom(String dom) {
                lastDom = dom;
            }

            @Override
            public void setAppBarColor(String color, String isTheme) {

            }

            @SuppressLint("JavascriptInterface")
            @Override
            public void addJavascriptInterface(Object obj, String interfaceName) {
                webView.addJavascriptInterface(obj, interfaceName);
            }

            @Override
            public Map<String, Map<String, String>> getRequestHeaderMap() {
                return requestHeaderMap;
            }

            @Override
            public String getCookie(String url) {
                try {
                    return CookieManager.getInstance().getCookie(url);
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            }
        });
    }

    private void loadAllJs(WebView webView, String url, boolean pageEnd) {
        JsPluginHelper.loadMyJs(reference.get(), js -> webView.evaluateJavascript(js, null), url, () -> "", pageEnd, false);
        //广告拦截
        String adBlockJs = AdBlockModel.getBlockJs(url);
        if (!TextUtils.isEmpty(adBlockJs)) {
            webView.evaluateJavascript(adBlockJs, null);
        }
    }

    public static void initWebSettings(WebSettings webSettings) {
        webSettings.setJavaScriptEnabled(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowUniversalAccessFromFileURLs(true);
        webSettings.setDefaultTextEncodingName("UTF-8");
//        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        webSettings.setAppCacheEnabled(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }
    }

    public interface ProgressListener {
        void onPageStarted(String s);

        void onProgressChanged(String s, int i);

        void onPageFinished(String s);
    }

    private static class UrlHolder {
        String url;
    }
}

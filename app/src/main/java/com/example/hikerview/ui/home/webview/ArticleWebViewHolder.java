package com.example.hikerview.ui.home.webview;

import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.WindowManager;
import android.widget.FrameLayout;

import androidx.appcompat.app.ActionBar;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;

import com.annimon.stream.function.Consumer;
import com.example.hikerview.R;
import com.example.hikerview.ui.Application;
import com.example.hikerview.ui.browser.model.AdBlockModel;
import com.example.hikerview.ui.browser.model.AdUrlBlocker;
import com.example.hikerview.ui.browser.model.JSManager;
import com.example.hikerview.ui.browser.util.CollectionUtil;
import com.example.hikerview.ui.browser.view.IVideoWebView;
import com.example.hikerview.ui.browser.view.VideoContainer;
import com.example.hikerview.ui.browser.webview.JsBridgeHolder;
import com.example.hikerview.ui.browser.webview.JsPluginHelper;
import com.example.hikerview.ui.browser.webview.WebViewWrapper;
import com.example.hikerview.ui.download.DownloadDialogUtil;
import com.example.hikerview.ui.video.PlayerChooser;
import com.example.hikerview.ui.view.EnhanceViewPager;
import com.example.hikerview.ui.view.colorDialog.ColorDialog;
import com.example.hikerview.utils.FilesInAppUtil;
import com.example.hikerview.utils.StringUtil;
import com.example.hikerview.utils.ThreadTool;
import com.google.android.material.snackbar.Snackbar;
import com.king.app.updater.http.HttpManager;
import com.lxj.xpopup.XPopup;
import com.tencent.smtt.export.external.extension.interfaces.IX5WebViewClientExtension;
import com.tencent.smtt.export.external.extension.proxy.ProxyWebViewClientExtension;
import com.tencent.smtt.export.external.interfaces.IX5WebChromeClient;
import com.tencent.smtt.export.external.interfaces.JsPromptResult;
import com.tencent.smtt.export.external.interfaces.JsResult;
import com.tencent.smtt.export.external.interfaces.SslError;
import com.tencent.smtt.export.external.interfaces.SslErrorHandler;
import com.tencent.smtt.export.external.interfaces.WebResourceRequest;
import com.tencent.smtt.export.external.interfaces.WebResourceResponse;
import com.tencent.smtt.sdk.WebChromeClient;
import com.tencent.smtt.sdk.WebSettings;
import com.tencent.smtt.sdk.WebView;
import com.tencent.smtt.sdk.WebViewCallbackClient;
import com.tencent.smtt.sdk.WebViewClient;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import timber.log.Timber;

import static android.view.View.VISIBLE;

/**
 * 作者：By 15968
 * 日期：On 2021/1/30
 * 时间：At 20:55
 */

public class ArticleWebViewHolder extends IArticleWebHolder {
    private List<String> urls = Collections.synchronizedList(new ArrayList<>());

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public ValueAnimator getAnim() {
        return anim;
    }

    public void setAnim(ValueAnimator anim) {
        this.anim = anim;
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

    public enum Mode {
        LIST,
        FLOAT
    }

    public ArticleWebViewHolder() {

    }

    public ArticleWebViewHolder(WebView webView) {
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
    /**
     * 模式
     */
    private Mode mode;

    private int height;
    private AtomicBoolean hasLoadJsOnProgress = new AtomicBoolean(false);
    private AtomicBoolean hasLoadJsOnPageEnd = new AtomicBoolean(false);

    protected static final FrameLayout.LayoutParams COVER_SCREEN_PARAMS
            = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
    private ValueAnimator anim;
    private View customView;
    private VideoContainer fullscreenContainer;
    private IX5WebChromeClient.CustomViewCallback customViewCallback;


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

    public Mode getMode() {
        return mode;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
    }

    private CallbackClient mCallbackClient = new CallbackClient();

    public void loadUrlCheckReferer(String url){
        if (x5Extra != null && StringUtil.isNotEmpty(x5Extra.getReferer())) {
            Map<String, String> headers = new HashMap<>();
            headers.put("Referer", x5Extra.getReferer());
            getWebView().loadUrl(url, headers);
        } else {
            getWebView().loadUrl(url);
        }
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
        Bundle bundle = new Bundle();
        //1：以页面内开始播放，2：以全屏开始播放；不设置默认：1
        bundle.putInt("DefaultVideoScreen", 1);
        //false：关闭小窗；true：开启小窗；不设置默认true，
        bundle.putBoolean("supportLiteWnd", true);
        try {
            Bundle bundle1 = new Bundle();
            bundle1.putBoolean("require", false);
            webView.getX5WebViewExtension().invokeMiscMethod("setVideoPlaybackRequiresUserGesture", bundle1);
            webView.getX5WebViewExtension().invokeMiscMethod("setVideoParams", bundle);
        } catch (Exception e) {
            e.printStackTrace();
        }
        webView.setWebViewCallbackClient(mCallbackClient);
        webView.setWebChromeClient(new WebChromeClient() {
            /*** 视频播放相关的方法 **/
            @Override
            public View getVideoLoadingProgressView() {
                if (reference.get() == null || reference.get().isFinishing()) {
                    return super.getVideoLoadingProgressView();
                }
                FrameLayout frameLayout = new FrameLayout(reference.get());
                frameLayout.setLayoutParams(new ActionBar.LayoutParams(ActionBar.LayoutParams.MATCH_PARENT, ActionBar.LayoutParams.MATCH_PARENT));
                return frameLayout;
            }

            @Override
            public void onShowCustomView(View view, IX5WebChromeClient.CustomViewCallback callback) {
                // if a view already exists then immediately terminate the new one
                if (reference.get() == null || reference.get().isFinishing() || customView != null) {
                    callback.onCustomViewHidden();
                    return;
                }
                reference.get().getWindow().getDecorView();
                FrameLayout decor = (FrameLayout) reference.get().getWindow().getDecorView();
                fullscreenContainer = new VideoContainer(reference.get(), new IVideoWebView() {
                    @Override
                    public void useFastPlay(boolean use) {
                        webView.evaluateJavascript(FilesInAppUtil.getAssetsString(reference.get(), "fastPlay.js").replace("{playbackRate}", (use ? "2" : "1")), null);
                    }

                    @Override
                    public void evaluateJS(@NotNull String js, @Nullable Consumer<String> resultCallback) {
                        webView.evaluateJavascript(js, s -> {
                            if (resultCallback != null) {
                                resultCallback.accept(s == null ? "" : s);
                            }
                        });
                    }
                });
                fullscreenContainer.addVideoView(view, COVER_SCREEN_PARAMS);
                decor.addView(fullscreenContainer, COVER_SCREEN_PARAMS);
                customView = view;
                customViewCallback = callback;
                webView.setVisibility(View.INVISIBLE);
                reference.get().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                setStatusBarVisibility(false);
                reference.get().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                super.onShowCustomView(view, callback);
            }

            @Override
            public void onHideCustomView() {
                if (reference.get() == null || reference.get().isFinishing() || customView == null) {
                    return;
                }
                reference.get().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                setStatusBarVisibility(true);
                FrameLayout decor = (FrameLayout) reference.get().getWindow().getDecorView();
                fullscreenContainer.destroy();
                decor.removeView(fullscreenContainer);
                fullscreenContainer = null;
                customView = null;
                customViewCallback.onCustomViewHidden();
                webView.setVisibility(VISIBLE);
                reference.get().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            }

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
            public void onPageStarted(WebView webView, String s, Bitmap bitmap) {
                if (progressListener != null) {
                    progressListener.onPageStarted(s);
                }
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
        webView.setDownloadListener((url, userAgent, contentDisposition, mimetype, contentLength) -> {
            Timber.d("downloadStart: %s, %s", mimetype, contentDisposition);
            String fileName = HttpManager.getDispositionFileName(contentDisposition);
            if (StringUtil.isEmpty(fileName)) {
                fileName = url;
            }
            String finalFileName = fileName;
            Timber.d("downloadStart: finalFileName: %s", finalFileName);
            if (url.contains(".apk") || DownloadDialogUtil.isApk(fileName, mimetype)) {
                Snackbar.make(webView, "是否允许网页中的下载请求？", Snackbar.LENGTH_LONG)
                        .setAction("允许", v -> DownloadDialogUtil.showEditDialog(reference.get(), finalFileName, url, null, mimetype)).show();
            } else {
                new ColorDialog(webView.getContext()).setTheTitle("温馨提示")
                        .setContentText("是否允许网页中的下载请求？（点击空白处拒绝操作，点击播放可以将链接作为音视频地址直接播放）")
                        .setPositiveListener("下载", dialog -> {
                            dialog.dismiss();
                            if (reference.get() == null || reference.get().isFinishing()) {
                                return;
                            }
                            DownloadDialogUtil.showEditDialog(reference.get(), finalFileName, url);
                        }).setNegativeListener("播放", dialog -> {
                    dialog.dismiss();
                    startPlayVideo(url);
                }).show();
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
                if (Thread.currentThread() == Looper.getMainLooper().getThread()) {
                    return webView.getUrl();
                }
                CountDownLatch countDownLatch = new CountDownLatch(1);
                UrlHolder urlHolder = new UrlHolder();
                urlHolder.url = "";
                ThreadTool.INSTANCE.runOnUI(() -> {
                    urlHolder.url = webView.getUrl();
                    countDownLatch.countDown();
                });
                try {
                    countDownLatch.await(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return urlHolder.url;
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
                return webView.getSettings().getUserAgentString();
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

            @Override
            public void addJavascriptInterface(Object obj, String interfaceName) {
                webView.addJavascriptInterface(obj, interfaceName);
            }
        });
        if (webView.getX5WebViewExtension() != null) {
            webView.getX5WebViewExtension().setWebViewClientExtension(mWebViewClientExtension);
        }
        if (webView.getSettingsExtension() != null) {
            webView.getSettingsExtension().setPageCacheCapacity(5);
        }
    }

    private void startPlayVideo(String videoUrl) {
        String muteJs = JSManager.instance(reference.get()).getJsByFileName("mute");
//        Log.d(TAG, "startPlayVideo:1 ");
        if (!TextUtils.isEmpty(muteJs)) {
//            Log.d(TAG, "startPlayVideo:2 ");
            webView.evaluateJavascript(muteJs, null);
        }
        if (reference.get().isFinishing()) {
            return;
        }
        PlayerChooser.startPlayer(reference.get(), getWebTitle(), videoUrl, null);
    }

    private String getWebTitle() {
        if (webView.getTitle() == null) {
            return "";
        }
        String t = webView.getTitle().replace(" ", "");
        if (t.length() > 85) {
            t = t.substring(0, 85);
        }
        return t;
    }

    private IX5WebViewClientExtension mWebViewClientExtension = new ProxyWebViewClientExtension() {

        @Override
        public boolean onTouchEvent(MotionEvent event, View view) {
            return mCallbackClient.onTouchEvent(event, view);
        }

        // 1
        public boolean onInterceptTouchEvent(MotionEvent ev, View view) {
            return mCallbackClient.onInterceptTouchEvent(ev, view);
        }

        // 3
        public boolean dispatchTouchEvent(MotionEvent ev, View view) {
            return mCallbackClient.dispatchTouchEvent(ev, view);
        }

        // 4
        public boolean overScrollBy(int deltaX, int deltaY, int scrollX, int scrollY,
                                    int scrollRangeX, int scrollRangeY,
                                    int maxOverScrollX, int maxOverScrollY,
                                    boolean isTouchEvent, View view) {
            return mCallbackClient.overScrollBy(deltaX, deltaY, scrollX, scrollY,
                    scrollRangeX, scrollRangeY, maxOverScrollX, maxOverScrollY, isTouchEvent, view);
        }

        // 5
        public void onScrollChanged(int l, int t, int oldl, int oldt, View view) {
            mCallbackClient.onScrollChanged(l, t, oldl, oldt, view);
        }

        // 6
        public void onOverScrolled(int scrollX, int scrollY, boolean clampedX,
                                   boolean clampedY, View view) {
            mCallbackClient.onOverScrolled(scrollX, scrollY, clampedX, clampedY, view);
        }

        // 7
        public void computeScroll(View view) {
            mCallbackClient.computeScroll(view);
        }
    };

    private void setStatusBarVisibility(boolean visible) {
        if (!visible) {
            setSystemUiVisibility = reference.get().getWindow().getDecorView().getSystemUiVisibility();
            reference.get().getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        } else {
            reference.get().getWindow().getDecorView().setSystemUiVisibility(setSystemUiVisibility);
        }
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
            webSettings.setMixedContentMode(android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }
    }

    public static int getHeightByExtra(String url, String desc) {
        int height = 0;
        if (StringUtil.isNotEmpty(url)) {
            height = getHeightByExtra(desc);
        }
        return height;
    }

    public static int getHeightByExtra(String desc) {
        int height = 240;
        if (StringUtil.isNotEmpty(desc)) {
            String[] extra = desc.split("&&");
            for (String s : extra) {
                s = s.trim();
                if (s.equalsIgnoreCase(ArticleWebViewHolder.Mode.FLOAT.name())
                        || s.equalsIgnoreCase(ArticleWebViewHolder.Mode.LIST.name())) {
                    continue;
                } else if ("auto".equals(s)) {
                    height = ViewGroup.LayoutParams.WRAP_CONTENT;
                } else if ("100%".equals(s)) {
                    if (desc.contains("float")) {
                        height = ViewGroup.LayoutParams.MATCH_PARENT;
                    }
                } else {
                    try {
                        height = Integer.parseInt(s);
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        Timber.d("getHeightByExtra, extra:%s, height:%d", desc, height);
        return height;
    }

    /**
     * 全屏容器界面
     */
    static class FullscreenHolder extends FrameLayout {

        public FullscreenHolder(Context ctx) {
            super(ctx);
            setBackgroundColor(ctx.getResources().getColor(android.R.color.black));
        }

        @Override
        public boolean onTouchEvent(MotionEvent evt) {
            return true;
        }
    }

    private ViewParent getPagerParent(WebView webView) {
        ViewParent parent = webView.getParent();
        while (parent != null) {
            if (parent instanceof ViewPager) {
                Timber.d("getPagerParent: find it");
                return parent;
            }
            parent = parent.getParent();
        }
        return null;
    }

    private ViewParent getRecyclerViewParent(WebView webView) {
        ViewParent parent = webView.getParent();
        while (parent != null) {
            if (parent instanceof RecyclerView) {
                Timber.d("getRecyclerViewParent: find it");
                return parent;
            }
            parent = parent.getParent();
        }
        return null;
    }

    class CallbackClient implements WebViewCallbackClient {

        @Override
        public void invalidate() {

        }

        @Override
        public boolean onTouchEvent(MotionEvent event, View view) {
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN || event.getPointerCount() > 1) {
                ViewParent parent = getPagerParent(webView);
                if (parent != null) {
                    //交给我处理
                    parent.requestDisallowInterceptTouchEvent(true);
                } else {
                    //二级页面
                    parent = getRecyclerViewParent(webView);
                    if (parent != null) {
                        //交给我处理
                        parent.requestDisallowInterceptTouchEvent(true);
                    }
                }
            }
            return webView.super_onTouchEvent(event);
        }

        @TargetApi(Build.VERSION_CODES.GINGERBREAD)
        @Override
        public boolean overScrollBy(int deltaX, int deltaY, int scrollX,
                                    int scrollY, int scrollRangeX, int scrollRangeY,
                                    int maxOverScrollX, int maxOverScrollY,
                                    boolean isTouchEvent, View view) {
            return webView.super_overScrollBy(deltaX, deltaY, scrollX, scrollY,
                    scrollRangeX, scrollRangeY, maxOverScrollX, maxOverScrollY,
                    isTouchEvent);
        }

        @Override
        public void computeScroll(View view) {
            webView.super_computeScroll();
        }

        @TargetApi(Build.VERSION_CODES.GINGERBREAD)
        @Override
        public void onOverScrolled(int scrollX, int scrollY, boolean clampedX,
                                   boolean clampedY, View view) {
            if (clampedX || clampedY) {
                ViewParent parent = getPagerParent(webView);
                if (parent != null) {
                    //我不处理
                    parent.requestDisallowInterceptTouchEvent(false);
                    if (parent instanceof EnhanceViewPager && parent.getParent() != null) {
                        //父亲要处理，爷爷不要处理
                        EnhanceViewPager viewPager = (EnhanceViewPager) parent;
                        viewPager.dealChildScroll();
                    }
                } else {
                    //二级页面
                    parent = getRecyclerViewParent(webView);
                    if (parent != null) {
                        //我不处理
                        parent.requestDisallowInterceptTouchEvent(false);
                    }
                }
            }
            webView.super_onOverScrolled(scrollX, scrollY, clampedX, clampedY);
        }

        @Override
        public void onScrollChanged(int l, int t, int oldl, int oldt, View view) {
            webView.super_onScrollChanged(l, t, oldl, oldt);
        }

        @Override
        public boolean dispatchTouchEvent(MotionEvent ev, View view) {
            return webView.super_dispatchTouchEvent(ev);
        }

        @Override
        public boolean onInterceptTouchEvent(MotionEvent ev, View view) {
            return webView.super_onInterceptTouchEvent(ev);
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

package com.example.hikerview.ui.home.webview;

import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
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
import com.example.hikerview.constants.Media;
import com.example.hikerview.constants.MediaType;
import com.example.hikerview.service.parser.HttpParser;
import com.example.hikerview.service.parser.JSEngine;
import com.example.hikerview.ui.Application;
import com.example.hikerview.ui.browser.model.AdBlockModel;
import com.example.hikerview.ui.browser.model.AdUrlBlocker;
import com.example.hikerview.ui.browser.model.DetectedMediaResult;
import com.example.hikerview.ui.browser.model.DetectorManager;
import com.example.hikerview.ui.browser.model.JSManager;
import com.example.hikerview.ui.browser.model.UrlDetector;
import com.example.hikerview.ui.browser.model.VideoDetector;
import com.example.hikerview.ui.browser.model.VideoTask;
import com.example.hikerview.ui.browser.util.CollectionUtil;
import com.example.hikerview.ui.browser.view.IVideoWebView;
import com.example.hikerview.ui.browser.view.VideoContainer;
import com.example.hikerview.ui.browser.webview.AdblockHolder;
import com.example.hikerview.ui.browser.webview.JsBridgeHolder;
import com.example.hikerview.ui.browser.webview.JsPluginHelper;
import com.example.hikerview.ui.browser.webview.WebViewWrapper;
import com.example.hikerview.ui.download.DownloadDialogUtil;
import com.example.hikerview.ui.home.ArticleListRuleEditActivity;
import com.example.hikerview.ui.setting.model.SettingConfig;
import com.example.hikerview.ui.video.FloatVideoController;
import com.example.hikerview.ui.video.PlayerChooser;
import com.example.hikerview.ui.view.EnhanceViewPager;
import com.example.hikerview.ui.view.PopImageLoaderNoView;
import com.example.hikerview.ui.view.popup.InputPopup;
import com.example.hikerview.ui.view.popup.SimpleHintPopupWindow;
import com.example.hikerview.utils.ClipboardUtil;
import com.example.hikerview.utils.FilesInAppUtil;
import com.example.hikerview.utils.HeavyTaskUtil;
import com.example.hikerview.utils.HttpUtil;
import com.example.hikerview.utils.ImgUtil;
import com.example.hikerview.utils.ScreenUtil;
import com.example.hikerview.utils.ShareUtil;
import com.example.hikerview.utils.StringUtil;
import com.example.hikerview.utils.ThreadTool;
import com.example.hikerview.utils.ToastMgr;
import com.example.hikerview.utils.WebUtil;
import com.google.android.material.snackbar.Snackbar;
import com.king.app.updater.http.HttpManager;
import com.lxj.xpopup.XPopup;
import com.tencent.smtt.export.external.extension.interfaces.IX5WebViewClientExtension;
import com.tencent.smtt.export.external.extension.proxy.ProxyWebViewClientExtension;
import com.tencent.smtt.export.external.interfaces.HttpAuthHandler;
import com.tencent.smtt.export.external.interfaces.IX5WebChromeClient;
import com.tencent.smtt.export.external.interfaces.JsPromptResult;
import com.tencent.smtt.export.external.interfaces.JsResult;
import com.tencent.smtt.export.external.interfaces.SslError;
import com.tencent.smtt.export.external.interfaces.SslErrorHandler;
import com.tencent.smtt.export.external.interfaces.WebResourceRequest;
import com.tencent.smtt.export.external.interfaces.WebResourceResponse;
import com.tencent.smtt.sdk.CookieManager;
import com.tencent.smtt.sdk.ValueCallback;
import com.tencent.smtt.sdk.WebChromeClient;
import com.tencent.smtt.sdk.WebSettings;
import com.tencent.smtt.sdk.WebView;
import com.tencent.smtt.sdk.WebViewCallbackClient;
import com.tencent.smtt.sdk.WebViewClient;

import org.adblockplus.libadblockplus.android.Utils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import timber.log.Timber;

import static android.view.View.VISIBLE;
import static com.example.hikerview.ui.browser.webview.WebViewHelper.RESPONSE_CHARSET_NAME;
import static com.example.hikerview.ui.browser.webview.WebViewHelper.RESPONSE_MIME_TYPE;

/**
 * 作者：By 15968
 * 日期：On 2021/1/30
 * 时间：At 20:55
 */

public class ArticleWebViewHolder extends IArticleWebHolder {
    private static final String TAG = "ArticleWebViewHolder";
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

    private AdblockHolder adblockHolder;
    private float focusX, focusY;
    private FloatVideoController floatVideoController;
    private VideoDetector videoDetector;
    private Map<String, Map<String, String>> requestHeaderMap = new HashMap<>();
    private String videoPlayingWebUrl;
    private boolean videoPlayShow = false;

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

    public void loadUrlCheckReferer(String url) {
        if (x5Extra != null && StringUtil.isNotEmpty(x5Extra.getReferer())) {
            Map<String, String> headers = new HashMap<>();
            headers.put("Referer", x5Extra.getReferer());
            getWebView().loadUrl(url, headers);
        } else {
            getWebView().loadUrl(url);
        }
    }

    public void initAdBlock() {
        adblockHolder = new AdblockHolder(reference.get(), new IVideoWebView() {
            @Override
            public void postTask(@NotNull Runnable task) {
                webView.post(task);
            }

            @Override
            public void useFastPlay(boolean use) {

            }

            @Override
            public void evaluateJS(@NotNull String js, @Nullable Consumer<String> resultCallback) {
                webView.evaluateJavascript(js, resultCallback == null ? null : new ValueCallback<String>() {
                    @Override
                    public void onReceiveValue(String s) {
                        resultCallback.accept(s);
                    }
                });
            }

            @Override
            public void addJSInterface(@NotNull Object obj, @NotNull String interfaceName) {
                webView.addJavascriptInterface(obj, interfaceName);
            }
        });
        adblockHolder.initAbp();
    }

    private void initVideoDetector() {
        if (videoDetector != null) {
            return;
        }
        videoDetector = new VideoDetector() {
            private List<DetectedMediaResult> detectedMediaResults = new ArrayList<>();
            private Set<String> taskUrlsSet = new HashSet<>();
            private AtomicInteger videoNotify = new AtomicInteger(0);
            private AtomicInteger videoLimit = new AtomicInteger(20);

            @Override
            public void putIntoXiuTanLiked(Context context, String dom, String url) {

            }

            @Override
            public List<DetectedMediaResult> getDetectedMediaResults(MediaType mediaType) {
                return DetectorManager.getDetectedMediaResults(new ArrayList<>(detectedMediaResults), new Media(mediaType));
            }

            @Override
            public void addTask(VideoTask video) {
                if (video == null) {
                    return;
                }
                if (taskUrlsSet.contains(video.getUrl())) {
                    return;
                }
                String[] urls = video.getUrl().split("url=");
                if (urls.length > 1 && urls[1].startsWith("http")) {
                    addTask(new VideoTask(video.getRequestHeaders(), video.getMethod(), video.getTitle(), HttpParser.decodeUrl(urls[1], "UTF-8")));
                }
                taskUrlsSet.add(video.getUrl());
                video.setTimestamp(System.currentTimeMillis());
                HeavyTaskUtil.executeNewTask(new MyRunnable(video));
            }

            @Override
            public void startDetect() {
                detectedMediaResults.clear();
                taskUrlsSet.clear();
                videoNotify.set(0);
                videoLimit.set(20);
            }

            @Override
            public synchronized void reset() {
                videoLimit.getAndAdd(20);
            }

            class MyRunnable implements Runnable {
                private VideoTask web;

                MyRunnable(VideoTask web) {
                    this.web = web;
                }

                @Override
                public void run() {
                    if (web != null) {
                        try {
                            DetectedMediaResult mediaResult = new DetectedMediaResult(web.getUrl());
                            mediaResult.setMediaType(UrlDetector.getMediaType(web.getUrl(), web.getRequestHeaders(), web.getMethod()));
                            mediaResult.setTimestamp(web.getTimestamp());
                            String mediaName = mediaResult.getMediaType().getName();
                            detectedMediaResults.add(mediaResult);
                            //只有视频或者音乐才发通知，不然可能会阻塞主线程
                            if ((mediaName.equals(MediaType.VIDEO.getName()) || mediaName.equals(MediaType.MUSIC.getName())) && videoNotify.get() < videoLimit.get()) {
                                videoNotify.addAndGet(1);
                                notifyVideoFind(mediaResult);
                            }
                        } catch (Throwable e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        };
    }

    public void initFloatVideo(boolean useFloatVideo, ViewGroup container) {
        //todo 禁用
        if (useFloatVideo) {
            if (floatVideoController == null) {
                initVideoDetector();
                floatVideoController = new FloatVideoController(reference.get(), container, (pause, force) -> {
                    if (pause) {
                        String muteJs = JSManager.instance(getContext()).getJsByFileName("mute");
                        if (webView != null && !TextUtils.isEmpty(muteJs)) {
                            webView.evaluateJavascript(muteJs, null);
                        }
                    }
                    if (force && webView != null) {
                        if (pause) {
                            webView.onPause();
                        } else {
                            webView.onResume();
                        }
                    }
                    return 0;
                }, videoDetector, () -> requestHeaderMap);
            }
        }
    }

    private void notifyVideoFind(DetectedMediaResult mediaResult) {
        if (floatVideoController != null && mediaResult != null) {
            runOnUiThread(() -> {
                if (floatVideoController != null && webView != null && videoDetector != null) {
                    String nowUrl = webView.getUrl();
                    if (!StringUtils.equals(videoPlayingWebUrl, nowUrl)) {
                        //说明是hash发生了变化，当成新页面处理，重新显示嗅探到视频
                        videoPlayingWebUrl = nowUrl;
                        videoPlayShow = false;
                        videoDetector.reset();
                    }
                    if (!videoPlayShow) {
                        videoPlayShow = true;
                        mediaResult.setClicked(true);
                        floatVideoController.show(mediaResult.getUrl(), webView.getUrl(), webView.getTitle(), true);
                    }
                }
            });
        }
    }

    public void initWebView(Activity activity) {
        setSystemUiVisibility = activity.getWindow().getDecorView().getSystemUiVisibility();
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
            bundle1.putBoolean("require", true);
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
                if (floatVideoController != null && floatVideoController.isFullScreen()) {
                    callback.onCustomViewHidden();
                    return;
                }
                reference.get().getWindow().getDecorView();
                FrameLayout decor = (FrameLayout) reference.get().getWindow().getDecorView();
                fullscreenContainer = new VideoContainer(reference.get(), new IVideoWebView() {
                    @Override
                    public void addJSInterface(@NotNull Object obj, @NotNull String interfaceName) {
                        webView.addJavascriptInterface(obj, interfaceName);
                    }

                    @Override
                    public void postTask(@NotNull Runnable task) {
                        webView.post(task);
                    }

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
                ScreenUtil.setStatusBarVisibility(reference.get(), setSystemUiVisibility, false);
                reference.get().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                super.onShowCustomView(view, callback);
            }

            @Override
            public void onHideCustomView() {
                if (reference.get() == null || reference.get().isFinishing() || customView == null) {
                    return;
                }
                reference.get().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                ScreenUtil.setStatusBarVisibility(reference.get(), setSystemUiVisibility, true);
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
                if (adblockHolder != null) {
                    adblockHolder.injectJsOnProgress();
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
                videoPlayShow = false;
                requestHeaderMap.clear();
                urls.clear();
                hasLoadJsOnProgress.set(false);
                hasLoadJsOnPageEnd.set(false);
                if (adblockHolder != null) {
                    if (adblockHolder.isLoading()) {
                        adblockHolder.stopAbpLoading();
                    }
                    adblockHolder.startAbpLoading(s);
                }
                checkUa(s);
                if (floatVideoController != null && StringUtil.isNotEmpty(s) && s.startsWith("http")) {
                    floatVideoController.loadUrl(s);
                }
                if (videoDetector != null) {
                    videoDetector.startDetect();
                }
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
                if (finishPageConsumer != null) {
                    finishPageConsumer.accept(s);
                }
                super.onPageFinished(webView, s);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView webView, WebResourceRequest request) {
                String url = request.getUrl().toString();
                if (x5Extra != null && StringUtil.isNotEmpty(x5Extra.getUrlInterceptor())) {
                    String result = JSEngine.getInstance().evalJS(x5Extra.getUrlInterceptor(), url);
                    if (StringUtil.isNotEmpty(result) && !"null".equals(result) && !"undefined".equals(result) && !"false".equals(result)) {
                        if (!"true".equals(result)) {
                            //有代码执行
                            webView.evaluateJavascript(result, null);
                        }
                        return true;
                    }
                }
                if (url.startsWith("http")) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        if (request.isRedirect() && x5Extra != null && !x5Extra.isRedirect()) {
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
                requestHeaderMap.put(url, request.getRequestHeaders());
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
                try {
                    if (adblockHolder != null) {
                        final AdblockHolder.AbpShouldBlockResult abpBlockResult = adblockHolder.shouldAbpBlockRequest(toWebkitRequest(request));
                        if (AdblockHolder.AbpShouldBlockResult.BLOCK_LOAD.equals(abpBlockResult)) {
                            if (reference.get() == null || reference.get().isFinishing()) {
                                return super.shouldInterceptRequest(webView, request);
                            }
                            return WebResponseResult.BLOCK_LOAD;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (videoDetector != null && !ArticleListRuleEditActivity.hasBlockDom(lastDom)) {
                    videoDetector.addTask(new VideoTask(request.getRequestHeaders(), request.getMethod(), request.getUrl().toString(), request.getUrl().toString()));
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
                new XPopup.Builder(webView.getContext())
                        .asConfirm("温馨提示", "是否允许网页中的下载请求？（点击空白处拒绝操作，点击播放可以将链接作为视频地址直接播放）",
                                "播放", "下载", () -> {
                                    if (reference.get() == null || reference.get().isFinishing()) {
                                        return;
                                    }
                                    DownloadDialogUtil.showEditDialog(reference.get(), finalFileName, url);
                                }, () -> startPlayVideo(url), false).show();
            }
        });
        webView.setOnLongClickListener(v -> {
            WebView.HitTestResult result = webView.getHitTestResult();
            int type = result.getType();
            Log.d(TAG, "initWebView: setOnLongClickListener--type=" + type + ",url=" + result.getExtra());
            if (type == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE
                    || type == WebView.HitTestResult.IMAGE_TYPE) {
                chooseOperationForImageUrl(webView, result.getExtra(), type == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE);
            } else if (type == WebView.HitTestResult.SRC_ANCHOR_TYPE) {
                if (UrlDetector.isImage(result.getExtra())) {
                    chooseOperationForImageUrl(webView, result.getExtra(), true);
                } else {
                    chooseOperationForUrl(result.getExtra());
                }
            } else if (type == WebView.HitTestResult.UNKNOWN_TYPE) {
//                chooseOperationForUnknown(webView);
            }
            return false;
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
        if (webView.getX5WebViewExtension() != null) {
            webView.getX5WebViewExtension().setWebViewClientExtension(mWebViewClientExtension);
        }
        if (webView.getSettingsExtension() != null) {
            webView.getSettingsExtension().setPageCacheCapacity(5);
        }
    }

    private void chooseOperationForUrl(final String url) {
        List<View.OnClickListener> clickList = new ArrayList<>();
        clickList.add(v -> runOnUiThread(() -> WebUtil.goWeb(reference.get(), url, true)));
        clickList.add(v -> ClipboardUtil.copyToClipboard(getContext(), url));
        clickList.add(v -> customCopy(url));
        clickList.add(v -> runOnUiThread(() -> ShareUtil.findChooserToDeal(getContext(), url, "text/html")));
        SimpleHintPopupWindow simpleHintPopupWindow = new SimpleHintPopupWindow(reference.get(), Arrays.asList("打开网页", "复制链接", "复制文本", "外部打开"), clickList);
        simpleHintPopupWindow.showPopupWindow(focusX, focusY);
    }

    private void customCopy(String url) {
        webView.evaluateJavascript("(function(){window.getAText('" + Utils.escapeJavaScriptString(url) + "')})();", null);
    }

    private void chooseOperationForImageUrl(View view, final String url, boolean anchor) {
        List<View.OnClickListener> clickList = new ArrayList<>();
        if (anchor) {
            JSEngine.getInstance().clearVar("#setImgHref");
            webView.evaluateJavascript("(function(){window.getImgHref('" + Utils.escapeJavaScriptString(url) + "')})();", null);
            clickList.add(v -> runOnUiThread(() -> goImgHref(url)));
        }
        clickList.add(v -> showBigPic(url));
        clickList.add(v -> savePic(url));
        clickList.add(v -> runOnUiThread(() -> ClipboardUtil.copyToClipboard(reference.get(), url)));
        String[] names;
        if (anchor) {
            names = new String[]{"打开网页", "全屏查看", "保存图片", "复制图片链接"};
        } else {
            names = new String[]{"全屏查看", "保存图片", "复制链接"};
        }
        SimpleHintPopupWindow simpleHintPopupWindow = new SimpleHintPopupWindow(reference.get(), Arrays.asList(names), clickList);
        simpleHintPopupWindow.showPopupWindow(focusX, focusY);
    }

    private void chooseOperationForUnknown(View view) {
        webView.evaluateJavascript("(function(){\n" +
                        "    return window.getTouchElement()\n" +
                        "})()",
                value -> {
                    if (StringUtil.isNotEmpty(value) && value.length() > 2) {
                        String result = value.substring(1, value.length() - 1);
                        String[] strings = result.split("@//@", -1);
                        if (strings.length < 2) {
                            return;
                        }
                        runOnUiThread(() -> {
                            List<View.OnClickListener> clickList = new ArrayList<>();
                            List<String> names = new ArrayList<>();
                            if (strings.length > 2 && StringUtil.isNotEmpty(strings[2]) && strings[2].startsWith("http")) {
                                names.add(0, "打开网页");
                                clickList.add(0, v -> WebUtil.goWeb(reference.get(), strings[2], true));
                                names.add("复制链接");
                                clickList.add(v -> ClipboardUtil.copyToClipboardForce(getContext(), strings[2]));
                            }
                            if (strings.length > 3 && StringUtil.isNotEmpty(strings[3]) && strings[3].startsWith("http")) {
                                names.add("全屏查看");
                                String pic = strings[3];
                                clickList.add(v -> showBigPic(pic));
                                names.add("保存图片");
                                clickList.add(v -> savePic(pic));
                            }
                            if (CollectionUtil.isEmpty(names)) {
                                return;
                            }
                            SimpleHintPopupWindow simpleHintPopupWindow = new SimpleHintPopupWindow(reference.get(), names, clickList);
                            simpleHintPopupWindow.showPopupWindow(focusX, focusY);
                        });
                    }
                }
        );
    }

    private void savePic(String url) {
        ImgUtil.savePic2Gallery(getContext(), url, webView.getUrl(), new ImgUtil.OnSaveListener() {
            @Override
            public void success(List<String> paths) {
                runOnUiThread(() -> ToastMgr.shortBottomCenter(getContext(), "保存成功"));
            }

            @Override
            public void failed(String msg) {
                runOnUiThread(() -> ToastMgr.shortBottomCenter(getContext(), msg));
            }
        });
    }

    private Context getContext() {
        return reference.get();
    }

    private void showBigPic(String pic) {
        new XPopup.Builder(reference.get())
                .asImageViewer(null, pic, new PopImageLoaderNoView(webView.getUrl()))
                .show();
    }

    private void goImgHref(String url) {
        String imgHref = JSEngine.getInstance().getVar("#setImgHref", "");
        if (StringUtil.isEmpty(imgHref)) {
            webView.evaluateJavascript("(function(){window.getImgHref('" + Utils.escapeJavaScriptString(url) + "')})();", null);
            webView.postDelayed(() -> {
                if (StringUtil.isEmpty(imgHref)) {
                    ToastMgr.shortBottomCenter(reference.get(), "获取图片跳转地址失败");
                } else {
                    WebUtil.goWeb(reference.get(), HttpUtil.getRealUrl(webView.getUrl(), imgHref), true);
                }
            }, 300);
        } else {
            WebUtil.goWeb(reference.get(), HttpUtil.getRealUrl(webView.getUrl(), imgHref), true);
        }
    }

    private void runOnUiThread(Runnable task) {
        if (reference.get() != null) {
            ThreadTool.INSTANCE.runOnUI(task);
        }
    }

    private android.webkit.WebResourceRequest toWebkitRequest(WebResourceRequest request) {
        return new android.webkit.WebResourceRequest() {
            @Override
            public Uri getUrl() {
                return request.getUrl();
            }

            @Override
            public boolean isForMainFrame() {
                return request.isForMainFrame();
            }

            @Override
            public boolean isRedirect() {
                return request.isRedirect();
            }

            @Override
            public boolean hasGesture() {
                return request.hasGesture();
            }

            @Override
            public String getMethod() {
                return request.getMethod();
            }

            @Override
            public Map<String, String> getRequestHeaders() {
                return request.getRequestHeaders();
            }
        };
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
            focusX = ev.getRawX();
            focusY = ev.getRawY();
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
                } else if ("top".equals(s)) {
                    if (desc.contains("float")) {
                        height = -1000;
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
            focusX = event.getRawX();
            focusY = event.getRawY();
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

    private static class WebResponseResult {
        static final WebResourceResponse ALLOW_LOAD = null;
        static final WebResourceResponse BLOCK_LOAD =
                new WebResourceResponse(RESPONSE_MIME_TYPE, RESPONSE_CHARSET_NAME, null);
    }

    public void release() {
        if (adblockHolder != null) {
            SettingConfig.saveAdblockPlusCount(reference.get());
        }
        getWebView().onPause();
        getWebView().destroy();
        if (floatVideoController != null) {
            floatVideoController.destroy();
        }
    }


    public void onPause() {
        if (floatVideoController != null) {
            floatVideoController.onPause();
        }
    }

    public void onResume() {
        if (floatVideoController != null) {
            floatVideoController.onResume();
        }
    }

    public boolean onBackPressed() {
        if (floatVideoController != null && floatVideoController.onBackPressed()) {
            return true;
        }
        if (getWebView() != null && getWebView().canGoBack()) {
            if (getX5Extra() != null && getX5Extra().isCanBack()) {
                getWebView().goBack();
                return true;
            }
        }
        return false;
    }
}

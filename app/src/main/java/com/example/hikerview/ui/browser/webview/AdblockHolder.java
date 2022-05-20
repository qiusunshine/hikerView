package com.example.hikerview.ui.browser.webview;

import android.content.Context;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;

import com.example.hikerview.BuildConfig;
import com.example.hikerview.ui.browser.view.IVideoWebView;
import com.example.hikerview.ui.home.ArticleListRuleEditActivity;
import com.example.hikerview.ui.setting.model.SettingConfig;
import com.example.hikerview.utils.PreferenceMgr;

import org.adblockplus.libadblockplus.FilterEngine;
import org.adblockplus.libadblockplus.Subscription;
import org.adblockplus.libadblockplus.android.AdblockEngine;
import org.adblockplus.libadblockplus.android.AdblockEngineProvider;
import org.adblockplus.libadblockplus.android.Utils;
import org.adblockplus.libadblockplus.android.settings.AdblockHelper;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import timber.log.Timber;

/**
 * 作者：By 15968
 * 日期：On 2022/5/20
 * 时间：At 10:24
 */

public class AdblockHolder {
    private static final String TAG = "AdblockHolder";

    private Context context;
    private IVideoWebView webView;

    public AdblockHolder(Context context, IVideoWebView webView) {
        this.context = context;
        this.webView = webView;
    }

    private static final String HEADER_REFERRER = "Referer";
    private static final String HEADER_REQUESTED_WITH = "X-Requested-With";
    private static final String HEADER_REQUESTED_WITH_XMLHTTPREQUEST = "XMLHttpRequest";
    private static final String HEADER_REQUESTED_RANGE = "Range";
    private static final String HEADER_LOCATION = "Location";
    private static final String HEADER_SET_COOKIE = "Set-Cookie";
    private static final String HEADER_COOKIE = "Cookie";
    private static final String HEADER_USER_AGENT = "User-Agent";
    private static final String HEADER_ACCEPT = "Accept";
    private static final String HEADER_REFRESH = "Refresh";
    private static final String HEADER_SEC_FETCH_MODE = "Sec-Fetch-Mode";

    // use low-case strings as in WebResponse all header keys are lowered-case
    private static final String HEADER_SITEKEY = "x-adblock-key";
    private static final String HEADER_CONTENT_TYPE = "content-type";
    private static final String HEADER_CONTENT_LENGTH = "content-length";

    private static final String ASSETS_CHARSET_NAME = "UTF-8";
    private static final String BRIDGE_TOKEN = "{{BRIDGE}}";
    private static final String DEBUG_TOKEN = "{{DEBUG}}";
    private static final String HIDE_TOKEN = "{{HIDE}}";
    private static final String HIDDEN_TOKEN = "{{HIDDEN_FLAG}}";
    private static final String BRIDGE = "jsBridge";
    private static final String EMPTY_ELEMHIDE_STRING = "";
    private static final String EMPTY_ELEMHIDE_ARRAY_STRING = "[]";

    // decisions
    private final static String RESPONSE_CHARSET_NAME = "UTF-8";
    private final static String RESPONSE_MIME_TYPE = "text/plain";
    private AtomicBoolean adblockEnabled = new AtomicBoolean(false);
    private final RegexContentTypeDetector contentTypeDetector = new RegexContentTypeDetector();
    private final Map<String, String> url2Referrer = Collections.synchronizedMap(new HashMap<>());
    private final AtomicReference<String> navigationUrl = new AtomicReference<>();
    private String elemhideBlockedJs = "";
    private String elementsHiddenFlag = "";
    private String injectJs;
    private CountDownLatch elemHideLatch;
    private String elemHideSelectorsString;
    private String elemHideEmuSelectorsString;
    private final Object elemHideThreadLockObject = new Object();
    private ElemHideThread elemHideThread;
    private boolean loading;

    /**
     * 初始化
     */
    public void initAbp() {
        try {
            webView.addJSInterface(this, BRIDGE);
            initRandom();
            buildInjectJs();
            getProvider();
            adblockEnabled.set(AdblockHelper.get().getStorage().load().isAdblockEnabled());
        } catch (Exception e) {

        }
    }

    /**
     * 生成拦截元素的JS
     *
     * @param newUrl
     */
    public void startAbpLoading(String newUrl) {
        try {
            Timber.d("Start loading %s", newUrl);

            if (ArticleListRuleEditActivity.hasBlockDom(newUrl)) {
                return;
            }
            loading = true;
            navigationUrl.set(newUrl);

            if (newUrl != null) {
                // elemhide and elemhideemu
                elemHideLatch = new CountDownLatch(1);
                synchronized (elemHideThreadLockObject) {
                    elemHideThread = new ElemHideThread(elemHideLatch);
                    elemHideThread.setFinishedRunnable(elemHideThreadFinishedRunnable);
                    elemHideThread.start();
                }
            } else {
                elemHideLatch = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 注入JS拦截元素
     */
    public void injectJsOnProgress() {
        try {
            if (adblockEnabled == null || !adblockEnabled.get()) {
                return;
            }
            if (injectJs != null) {
                webView.evaluateJS(injectJs, null);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 拦截网址加载
     *
     * @param request
     * @return
     */
    public AbpShouldBlockResult shouldAbpBlockRequest(final WebResourceRequest request) {
        // here we just trying to fill url -> referrer map
        final String url = request.getUrl().toString();

        final Map<String, String> requestHeadersMap = request.getRequestHeaders();

        final boolean isXmlHttpRequest =
                request.getRequestHeaders().containsKey(HEADER_REQUESTED_WITH) &&
                        HEADER_REQUESTED_WITH_XMLHTTPREQUEST.equals(
                                request.getRequestHeaders().get(HEADER_REQUESTED_WITH));

        final boolean isMainFrame = request.isForMainFrame();
        boolean isWhitelisted = false;
        boolean canContainSitekey = false;

        final String referrer = request.getRequestHeaders().get(HEADER_REFERRER);

        final Lock lock = getProvider().getReadEngineLock();
        lock.lock();

        try {
            // if dispose() was invoke, but the page is still loading then just let it go
            boolean isDisposed = false;
            if (getProvider().getCounter() == 0) {
                isDisposed = true;
            } else {
                lock.unlock();
                getProvider().waitForReady();
                lock.lock();
                if (getProvider().getCounter() == 0) {
                    isDisposed = true;
                }
            }

            // Apart from checking counter (getProvider().getCounter()) we also need to make sure
            // that getProvider().getEngine() is already set.
            // We check that under getProvider().getReadEngineLock(); so we are sure it will not be
            // changed after this check.
            if (isDisposed || getProvider().getEngine() == null) {
                return AbpShouldBlockResult.NOT_ENABLED;
            }

            if (adblockEnabled == null) {
                return AbpShouldBlockResult.NOT_ENABLED;
            } else {
                // check the real enable status and update adblockEnabled flag which is used
                // later on to check if we should execute element hiding JS
                adblockEnabled.set(getProvider().getEngine().isEnabled());
                if (!adblockEnabled.get()) {
                    return AbpShouldBlockResult.NOT_ENABLED;
                }
            }

            if (referrer != null) {
                if (!url.equals(referrer)) {
                    url2Referrer.put(url, referrer);
                } else {
                    Log.w(TAG, "Header referrer value is the same as url, skipping url2Referrer.put()");
                }
            } else {
                Log.w(TAG, "No referrer header for " + url);
            }

            // reconstruct frames hierarchy
            List<String> referrerChain = new ArrayList<>();
            String parent = url;
            while ((parent = url2Referrer.get(parent)) != null) {
                if (referrerChain.contains(parent)) {
                    Log.w(TAG, "Detected referrer loop, finished creating referrers list");
                    break;
                }
                referrerChain.add(0, parent);
            }

            if (isMainFrame) {
                // never blocking main frame requests, just subrequests
                Log.w(TAG, url + " is main frame, allow loading");
            } else {
                final String siteKey = null;

                // whitelisted
                if (getProvider().getEngine().isDomainWhitelisted(url, referrerChain)) {
                    isWhitelisted = true;
                }
                //耗时不稳定，有不少10ms以上
//                else if (getProvider().getEngine().isDocumentWhitelisted(url, referrerChain, siteKey)) {
//                    isWhitelisted = true;
//                    Timber.w("%s document is whitelisted, allow loading", url);
//                    notifyResourceWhitelisted(new EventsListener.WhitelistedResourceInfo(
//                            url, referrerChain, EventsListener.WhitelistReason.DOCUMENT));
//                }
                else {
                    // determine the content
                    FilterEngine.ContentType contentType;
                    if (isXmlHttpRequest) {
                        contentType = FilterEngine.ContentType.XMLHTTPREQUEST;
                    } else {
                        contentType = contentTypeDetector.detect(url);
                        if (contentType == null) {
                            Timber.w("contentTypeDetector didn't recognize content type");
                            final String acceptType = requestHeadersMap.get(HEADER_ACCEPT);
                            if (acceptType != null && acceptType.contains("text/html")) {
                                Timber.w("using subdocument content type");
                                contentType = FilterEngine.ContentType.SUBDOCUMENT;
                            } else {
                                Timber.w("using other content type");
                                contentType = FilterEngine.ContentType.OTHER;
                            }
                        }

                        if (contentType == FilterEngine.ContentType.SUBDOCUMENT ||
                                contentType == FilterEngine.ContentType.OTHER) {
                            canContainSitekey = true;
                        }
                    }

                    boolean specificOnly = false;
                    //耗时不稳定，有不少10ms以上
//                    if (!referrerChain.isEmpty()) {
//                        final String parentUrl = referrerChain.get(0);
//                        final List<String> referrerChainForGenericblock = referrerChain.subList(1, referrerChain.size());
//                        specificOnly = getProvider().getEngine().isGenericblockWhitelisted(parentUrl,
//                                referrerChainForGenericblock, siteKey);
//                        if (specificOnly) {
//                            Timber.w("Found genericblock filter for url %s which parent is %s", url, parentUrl);
//                        }
//                    }

                    // check if we should block
                    final AdblockEngine.MatchesResult result = getProvider().getEngine().matches(
                            url, FilterEngine.ContentType.maskOf(contentType),
                            referrerChain, siteKey, specificOnly);

                    if (result == AdblockEngine.MatchesResult.NOT_WHITELISTED) {
                        Timber.w("Blocked loading %s", url);

                        if (isVisibleResource(contentType)) {
                            elemhideBlockedResource(url);
                        }
                        recordBlock();
                        return AbpShouldBlockResult.BLOCK_LOAD;
                    } else if (result == AdblockEngine.MatchesResult.WHITELISTED) {
                        isWhitelisted = true;
                    }
                }
            }
        } finally {
            lock.unlock();
        }

        // we rely on calling `fetchUrlAndCheckSiteKey`
        // later in `shouldInterceptRequest`
        // now we just reply that ist fine to load
        // the resource
        if (isMainFrame || (canContainSitekey && !isWhitelisted)) {
            // if url is a main frame (whitelisted by default) or can contain by design a site key header
            // (it content type is SUBDOCUMENT or OTHER) and it is not yet whitelisted then we need to
            // make custom HTTP get request to try to obtain a site key header.
            return AbpShouldBlockResult.ALLOW_LOAD;
        } else {
            return AbpShouldBlockResult.ALLOW_LOAD_NO_SITEKEY_CHECK;
        }
    }

    public boolean isLoading() {
        return loading;
    }

    public void stopAbpLoading() {
        try {
            loading = false;
            clearReferrers();
            synchronized (elemHideThreadLockObject) {
                if (elemHideThread != null) {
                    elemHideThread.cancel();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stopBeforeLoad() {
        getProvider();
        if (loading) {
            stopAbpLoading();
        }
    }


    private void initRandom() {
        elementsHiddenFlag = "abp" + Math.abs(new Random().nextLong());
    }

    /**
     * 生成注入拦截广告的JS
     */
    private void buildInjectJs() {
        try {
            if (injectJs == null) {
                StringBuffer sb = new StringBuffer();
                sb.append(readScriptFile("inject.js").replace(HIDE_TOKEN, readScriptFile("css.js")));
                sb.append(readScriptFile("elemhideemu.js"));
                injectJs = sb.toString();
            }

            if (elemhideBlockedJs == null) {
                elemhideBlockedJs = readScriptFile("elemhideblocked.js");
            }
        } catch (final IOException e) {
            Timber.e(e, "Failed to read script");
        }
    }

    private String readScriptFile(String filename) throws IOException {
        return Utils
                .readAssetAsString(context, filename, ASSETS_CHARSET_NAME)
                .replace(BRIDGE_TOKEN, BRIDGE)
                .replace(DEBUG_TOKEN, (BuildConfig.DEBUG ? "" : "//"))
                .replace(HIDDEN_TOKEN, elementsHiddenFlag);
    }


    private boolean isVisibleResource(final FilterEngine.ContentType contentType) {
        return
                contentType == FilterEngine.ContentType.IMAGE ||
                        contentType == FilterEngine.ContentType.MEDIA ||
                        contentType == FilterEngine.ContentType.OBJECT ||
                        contentType == FilterEngine.ContentType.SUBDOCUMENT;
    }

    private void elemhideBlockedResource(final String url) {
//        Timber.d("Trying to elemhide visible blocked resource with url: " + url);
        final String filenameWithQuery;
        try {
            filenameWithQuery = Utils.extractPathWithQuery(url);
        } catch (final MalformedURLException e) {
//            Timber.e("Failed to parse URI for blocked resource:" + url + ". Skipping element hiding");
            return;
        }
        final StringBuilder selectorBuilder = new StringBuilder();
        selectorBuilder.append("[src$='");
        selectorBuilder.append(filenameWithQuery);
        selectorBuilder.append("'], [srcset$='");
        selectorBuilder.append(filenameWithQuery);
        selectorBuilder.append("']");

        // all UI views including AdblockWebView can be touched from UI thread only
        try {
            webView.postTask(() -> {
                try {
                    final StringBuilder scriptBuilder = new StringBuilder(elemhideBlockedJs);
                    scriptBuilder.append("\n\n");
                    scriptBuilder.append("elemhideForSelector(\"");
                    scriptBuilder.append(url); // 1st argument
                    scriptBuilder.append("\", \"");
                    scriptBuilder.append(Utils.escapeJavaScriptString(selectorBuilder.toString())); // 2nd argument
                    scriptBuilder.append("\", 0)"); // attempt #0
                    webView.evaluateJS(scriptBuilder.toString(), null);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public IVideoWebView getWebView() {
        return webView;
    }

    public void setWebView(IVideoWebView webView) {
        this.webView = webView;
    }

    public enum AbpShouldBlockResult {
        NOT_ENABLED,
        ALLOW_LOAD,
        ALLOW_LOAD_NO_SITEKEY_CHECK,
        BLOCK_LOAD,
    }

    private static class WebResponseResult {
        static final WebResourceResponse ALLOW_LOAD = null;
        static final WebResourceResponse BLOCK_LOAD =
                new WebResourceResponse(RESPONSE_MIME_TYPE, RESPONSE_CHARSET_NAME, null);
    }

    // warning: do not rename (used in injected JS by method name)
    @JavascriptInterface
    public String getElemhideStyleSheet() {
        if (elemHideLatch == null) {
            return EMPTY_ELEMHIDE_STRING;
        } else {
            try {
                elemHideLatch.await();
                return elemHideSelectorsString;
            } catch (final InterruptedException e) {
                return EMPTY_ELEMHIDE_STRING;
            }
        }
    }

    // warning: do not rename (used in injected JS by method name)
    @JavascriptInterface
    public String getElemhideEmulationSelectors() {
        if (elemHideLatch == null) {
            return EMPTY_ELEMHIDE_ARRAY_STRING;
        } else {
            try {
                elemHideLatch.await();
                return elemHideEmuSelectorsString;
            } catch (final InterruptedException e) {
                Timber.w("Interrupted, returning empty elemhideemu selectors list");
                return EMPTY_ELEMHIDE_ARRAY_STRING;
            }
        }
    }

    private void recordBlock() {
        try {
            if (context != null) {
                if (SettingConfig.adblockplus_count == -1) {
                    SettingConfig.adblockplus_count = PreferenceMgr.getLong(context, "adblockplus_count", 0);
                }
                SettingConfig.adblockplus_count = SettingConfig.adblockplus_count + 1;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void clearReferrers() {
        url2Referrer.clear();
    }

    private AdblockEngineProvider.EngineCreatedListener engineCreatedCb = engine -> adblockEnabled = new AtomicBoolean(engine.isEnabled());

    private AdblockEngineProvider getProvider() {
        AdblockEngineProvider provider = null;
        try {
            provider = AdblockHelper.get().getProvider();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (provider == null) {
            adblockEnabled.set(false);
            final Runnable setRunnable = () -> {
                if (!AdblockHelper.get().isInit()) {
                    // init Adblock
                    String basePath = context.getDir(AdblockEngine.BASE_PATH_DIRECTORY, Context.MODE_PRIVATE).getAbsolutePath();
                    AdblockHelper.get()
                            .init(context, basePath, true, AdblockHelper.PREFERENCE_NAME)
                            .setDisabledByDefault();
                }
                final ReentrantReadWriteLock.ReadLock lock = AdblockHelper.get().getProvider().getReadEngineLock();
                final boolean locked = lock.tryLock();
                try {
                    // Note that if retain() needs to create a FilterEngine it will wait (in bg thread)
                    // until we finish this synchronized block and release the engine lock.
                    AdblockHelper.get().getProvider().retain(true); // asynchronously
                    if (locked && getProvider().getEngine() != null) {
                        adblockEnabled = new AtomicBoolean(getProvider().getEngine().isEnabled());
                    } else {
                        getProvider().addEngineCreatedListener(engineCreatedCb);
                    }
                } finally {
                    if (locked) {
                        lock.unlock();
                    }
                }
            };
            setRunnable.run();
        }
        return AdblockHelper.get().getProvider();
    }

    /**
     * 获取拦截规则的线程
     */
    private class ElemHideThread extends Thread {
        private String stylesheetString;
        private String emuSelectorsString;
        private CountDownLatch finishedLatch;
        private AtomicBoolean isFinished;
        private AtomicBoolean isCancelled;

        public ElemHideThread(CountDownLatch finishedLatch) {
            this.finishedLatch = finishedLatch;
            isFinished = new AtomicBoolean(false);
            isCancelled = new AtomicBoolean(false);
        }

        @Override
        public void run() {
            final Lock lock = getProvider().getReadEngineLock();
            lock.lock();

            try {
                boolean isDisposed = false;
                if (getProvider().getCounter() == 0) {
                    isDisposed = true;
                } else {
                    lock.unlock();
                    getProvider().waitForReady();
                    lock.lock();
                    if (getProvider().getCounter() == 0) {
                        isDisposed = true;
                    }
                }

                // Apart from checking counter (getProvider().getCounter()) we also need to make sure
                // that getProvider().getEngine() is already set.
                // We check that under getProvider().getReadEngineLock(); so we are sure it will not be
                // changed after this check.
                if (isDisposed || getProvider().getEngine() == null) {
                    Timber.w("FilterEngine already disposed");
                    stylesheetString = EMPTY_ELEMHIDE_STRING;
                    emuSelectorsString = EMPTY_ELEMHIDE_ARRAY_STRING;
                } else {
                    List<String> referrerChain = new ArrayList<>(1);
                    String parentUrl = navigationUrl.get();
                    referrerChain.add(parentUrl);
                    while ((parentUrl = url2Referrer.get(parentUrl)) != null) {
                        if (referrerChain.contains(parentUrl)) {
                            Timber.w("Detected referrer loop, finished creating referrers list");
                            break;
                        }
                        referrerChain.add(0, parentUrl);
                    }

                    final FilterEngine filterEngine = getProvider().getEngine().getFilterEngine();

                    List<Subscription> subscriptions = filterEngine.getListedSubscriptions();

                    try {
                        Timber.d("Listed subscriptions: %d", subscriptions.size());
                        if (BuildConfig.DEBUG) {
                            for (Subscription eachSubscription : subscriptions) {
                                Timber.d("Subscribed to "
                                        + (eachSubscription.isDisabled() ? "disabled" : "enabled")
                                        + " " + eachSubscription);
                            }
                        }
                    } finally {
                        for (Subscription eachSubscription : subscriptions) {
                            eachSubscription.dispose();
                        }
                    }

                    final String navigationUrlLocalRef = navigationUrl.get();
                    final String domain = filterEngine.getHostFromURL(navigationUrlLocalRef);
                    if (domain == null) {
                        stylesheetString = EMPTY_ELEMHIDE_STRING;
                        emuSelectorsString = EMPTY_ELEMHIDE_ARRAY_STRING;
                    } else {

                        final String siteKey = null;

                        final boolean specificOnly = filterEngine.matches(navigationUrlLocalRef,
                                FilterEngine.ContentType.maskOf(FilterEngine.ContentType.GENERICHIDE),
                                Collections.<String>emptyList(), null) != null;

                        stylesheetString = getProvider()
                                .getEngine()
                                .getElementHidingStyleSheet(navigationUrlLocalRef, domain, referrerChain, siteKey, specificOnly);
                        List<FilterEngine.EmulationSelector> emuSelectors = getProvider()
                                .getEngine()
                                .getElementHidingEmulationSelectors(navigationUrlLocalRef, domain, referrerChain, siteKey);
                        emuSelectorsString = Utils.emulationSelectorListToJsonArray(emuSelectors);
                    }
                }
            } catch (Exception e) {
                Timber.e(e);
            } finally {
                lock.unlock();
                if (isCancelled.get()) {
                    Timber.w("This thread is cancelled, exiting silently %s", this);
                } else {
                    finish(stylesheetString, emuSelectorsString);
                }
            }

        }

        private void onFinished() {
            finishedLatch.countDown();
            synchronized (finishedRunnableLockObject) {
                if (finishedRunnable != null) {
                    finishedRunnable.run();
                }
            }
        }

        private void finish(String selectorsString, String emuSelectorsString) {
            isFinished.set(true);
            elemHideSelectorsString = selectorsString;
            elemHideEmuSelectorsString = emuSelectorsString;
            onFinished();
        }

        private final Object finishedRunnableLockObject = new Object();
        private Runnable finishedRunnable;

        public void setFinishedRunnable(Runnable runnable) {
            synchronized (finishedRunnableLockObject) {
                this.finishedRunnable = runnable;
            }
        }

        public void cancel() {
            Timber.w("Cancelling elemhide thread %s", this);
            if (isFinished.get()) {
                Timber.w("This thread is finished, exiting silently %s", this);
            } else {
                isCancelled.set(true);
                finish(EMPTY_ELEMHIDE_STRING, EMPTY_ELEMHIDE_ARRAY_STRING);
            }
        }
    }

    private final Runnable elemHideThreadFinishedRunnable = new Runnable() {
        @Override
        public void run() {
            synchronized (elemHideThreadLockObject) {
                Timber.w("elemHideThread set to null");
                elemHideThread = null;
            }
        }
    };
}
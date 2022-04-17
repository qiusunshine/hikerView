package com.example.hikerview.ui.home.webview;

import android.app.Activity;
import android.content.Intent;
import android.text.TextUtils;

import com.annimon.stream.function.Consumer;
import com.example.hikerview.service.parser.HttpParser;
import com.example.hikerview.ui.bookmark.BookmarkActivity;
import com.example.hikerview.ui.browser.model.UAModel;
import com.example.hikerview.ui.browser.webview.JsBridgeHolder;
import com.example.hikerview.ui.download.DownloadRecordsActivity;
import com.example.hikerview.ui.home.FilmListActivity;
import com.example.hikerview.ui.home.model.RouteBlocker;
import com.example.hikerview.ui.home.model.article.extra.X5Extra;
import com.example.hikerview.ui.setting.HistoryListActivity;
import com.example.hikerview.utils.StringUtil;
import com.example.hikerview.utils.WebUtil;

import org.apache.commons.lang3.StringUtils;

import java.lang.ref.WeakReference;

import timber.log.Timber;

/**
 * 作者：By 15968
 * 日期：On 2021/11/18
 * 时间：At 17:13
 */

public abstract class IArticleWebHolder {
    protected Consumer<String> lazyRuleConsumer;
    protected WeakReference<? extends Activity> reference;
    protected X5Extra x5Extra;
    protected String extra;
    protected JsBridgeHolder jsBridgeHolder;
    protected String systemUA = "";
    protected String lastDom = "";
    protected Consumer<String> finishPageConsumer;
    protected int setSystemUiVisibility = 0;

    public Consumer<String> getFinishPageConsumer() {
        return finishPageConsumer;
    }

    public void setFinishPageConsumer(Consumer<String> finishPageConsumer) {
        this.finishPageConsumer = finishPageConsumer;
    }


    protected abstract void setUserAgentString(String ua);

    protected abstract String getUserAgentString();


    public Consumer<String> getLazyRuleConsumer() {
        return lazyRuleConsumer;
    }

    public void setLazyRuleConsumer(Consumer<String> lazyRuleConsumer) {
        this.lazyRuleConsumer = lazyRuleConsumer;
    }


    public String getExtra() {
        return extra;
    }

    public void setExtra(String extra) {
        this.extra = extra;
    }

    public X5Extra getX5Extra() {
        return x5Extra;
    }

    public void setX5Extra(X5Extra x5Extra) {
        this.x5Extra = x5Extra;
    }

    protected void checkUa(String s) {
        String dom = StringUtil.getDom(s);
        if (dom != null && !dom.equals(lastDom) && StringUtil.isEmpty(getExtraUa())) {
//                Log.d(TAG, "onPageStarted: setUserAgentString===>" + s);
            String ua = UAModel.getAdjustUa(s);
            if (!TextUtils.isEmpty(ua)) {
                if (!ua.equals(getUserAgentString())) {
                    Timber.d("onPageStarted: getAdjustUa");
                    setUserAgentString(ua);
                }
            } else {
                if (!TextUtils.isEmpty(UAModel.getUseUa())) {
                    if (!UAModel.getUseUa().equals(getUserAgentString())) {
                        Timber.d("onPageStarted: getUseUa");
                        setUserAgentString(UAModel.getUseUa());
                    }
                } else {
                    if (StringUtil.isNotEmpty(systemUA) && !systemUA.equals(getUserAgentString())) {
                        Timber.d("onPageStarted: systemUA");
                        setUserAgentString(systemUA);
                    }
                }
            }
        }
        lastDom = dom;
    }

    protected String getExtraUa() {
        return x5Extra == null ? null : x5Extra.getUa();
    }

    protected void dealLoadUrl(String url) {
        if (url.contains("@lazyRule=")) {
            if (lazyRuleConsumer != null) {
                lazyRuleConsumer.accept(url);
            }
        } else if (url.startsWith("hiker://")) {
            String route = url.replace("hiker://", "");
            if (route.startsWith("home")) {
                url = HttpParser.decodeUrl(url, "UTF-8");
                RouteBlocker.isRoute(reference.get(), url);
                return;
            } else if (route.startsWith("search")) {
                url = HttpParser.decodeUrl(url, "UTF-8");
                RouteBlocker.isRoute(reference.get(), url);
                return;
            } else if (RouteBlocker.isRoute(reference.get(), url)) {
                return;
            }
        } else if (url.equals("folder://")) {
            reference.get().startActivityForResult(new Intent(reference.get(), BookmarkActivity.class), 101);
        } else if (url.equals("history://")) {
            reference.get().startActivityForResult(new Intent(reference.get(), HistoryListActivity.class), 101);
        } else if (url.equals("download://")) {
            Intent intent = new Intent(reference.get(), DownloadRecordsActivity.class);
            intent.putExtra("downloaded", true);
            reference.get().startActivity(intent);
        } else if (url.startsWith("web://")) {
            WebUtil.goWeb(reference.get(), StringUtils.replaceOnce(url, "web://", ""));
        } else if (url.startsWith("func://background")) {
            if (reference.get() instanceof FilmListActivity) {
                ((FilmListActivity) reference.get()).background();
            }
        }
    }
} 
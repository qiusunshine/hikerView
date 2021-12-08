package com.example.hikerview.service.subscribe;

import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.Nullable;

import com.alibaba.fastjson.JSON;
import com.example.hikerview.service.http.CodeUtil;
import com.example.hikerview.service.subscribe.model.SubscribeMsg;
import com.example.hikerview.ui.base.SimpleActionListener;
import com.example.hikerview.ui.browser.model.AdUrlBlocker;
import com.example.hikerview.utils.FilesInAppUtil;
import com.example.hikerview.utils.HeavyTaskUtil;
import com.example.hikerview.utils.PreferenceMgr;
import com.example.hikerview.utils.StringUtil;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;

/**
 * 作者：By 15968
 * 日期：On 2019/10/21
 * 时间：At 19:14
 */
public class AdUrlSubscribe {

    public static final String DOM_BLOCK_RULE_FILE = "domBlockRules.txt";
    public static final String AD_FILTER_URL_FILE = "adUrls.txt";

    public static String getSubscribeUrl(Context context) {
        return PreferenceMgr.getString(context, "subscribe", "adUrlSubscribeUrl", "");
    }

    public static void updateSubscribeUrl(Context context, String url) {
        PreferenceMgr.put(context, "subscribe", "adUrlSubscribeUrl", url);
    }

    public static SubscribeMsg getLastUpdate(Context context) {
        String msg = PreferenceMgr.getString(context, "subscribe", "adUrlLastUpdate", "");
        if (TextUtils.isEmpty(msg)) {
            return null;
        }
        SubscribeMsg subscribeMsg = JSON.parseObject(msg, SubscribeMsg.class);
        if (TextUtils.isEmpty(subscribeMsg.getUrlV2())) {
            return null;
        }
        return subscribeMsg;
    }

    public static String getLastCheckTime(Context context) {
        long lastUpdate = PreferenceMgr.getLong(context, "subscribe", "adUrlLastUpdateTime", 0);
        if (lastUpdate == 0) {
            return "无记录";
        } else {
            try {
                SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                return formatter.format(lastUpdate);
            } catch (Exception e) {
                e.printStackTrace();
                return "无记录";
            }
        }
    }

    private static void putLastUpdate(Context context, SubscribeMsg subscribeMsg) {
        PreferenceMgr.put(context, "subscribe", "adUrlLastUpdate", JSON.toJSONString(subscribeMsg));
    }

    public static void checkUpdateAsync(Context context, @Nullable SimpleActionListener listener) {
        HeavyTaskUtil.executeNewTask(() -> checkUpdate(context, listener));
    }

    private static void checkUpdate(Context context, @Nullable SimpleActionListener listener) {
        String url = getSubscribeUrl(context);
        if (TextUtils.isEmpty(url) || "no-subscribe".equals(url)) {
            return;
        }
        final WeakReference<Context> contextRef = new WeakReference<>(context);
        //主动更新一把订阅到缓存
        if (listener == null) {
            AdUrlBlocker.instance().updateSubscribe(context);
            AdUrlBlocker.instance().updateDomBlockSubscribe(context);
        }
        long now = System.currentTimeMillis();
        if (listener == null) {
            long lastUpdate = PreferenceMgr.getLong(context, "subscribe", "adUrlLastUpdateTime", 0);
            if (now - lastUpdate < 86400 * 1000) {
                return;
            }
        }
        PreferenceMgr.put(context, "subscribe", "adUrlLastUpdateTime", now);
        if (url.startsWith("{") && url.endsWith("}")) {
            try {
                SubscribeMsg subscribeMsg = JSON.parseObject(url, SubscribeMsg.class);
                if (subscribeMsg == null || TextUtils.isEmpty(subscribeMsg.getUrlV2())) {
                    if (listener != null) {
                        listener.failed("订阅地址为空");
                    }
                    return;
                }
                putLastUpdate(contextRef.get(), subscribeMsg);
                //更新数据
                CodeUtil.get(subscribeMsg.getUrlV2(), new CodeUtil.OnCodeGetListener() {
                    @Override
                    public void onSuccess(String s) {
                        try {
                            if (TextUtils.isEmpty(s) || contextRef.get() == null) {
                                return;
                            }
                            String now = FilesInAppUtil.read(contextRef.get(), AD_FILTER_URL_FILE);
                            if (!s.equals(now)) {
                                FilesInAppUtil.write(contextRef.get(), AD_FILTER_URL_FILE, s);
                                AdUrlBlocker.instance().updateSubscribe(contextRef.get());
                            }
                            checkDomBlock(contextRef, subscribeMsg.getDomBlockRuleUrl(), listener);
                        } catch (Exception e) {
                            e.printStackTrace();
                            if (listener != null) {
                                listener.failed(e.getMessage());
                            }
                        }
                    }

                    @Override
                    public void onFailure(int errorCode, String msg) {
                        if (listener != null) {
                            listener.failed(msg);
                        }
                    }
                });
            } catch (Exception e) {
                if (listener != null) {
                    e.printStackTrace();
                    listener.failed(e.getMessage());
                }
            }
        }

    }

    private static void checkDomBlock(WeakReference<Context> contextRef, String url, @Nullable SimpleActionListener listener) {
        if (StringUtil.isEmpty(url)) {
            if (listener != null) {
                listener.success("");
            }
            return;
        }
        CodeUtil.get(url, new CodeUtil.OnCodeGetListener() {
            @Override
            public void onSuccess(String s) {
                if (TextUtils.isEmpty(s) || contextRef.get() == null) {
                    return;
                }
                try {
                    String now = FilesInAppUtil.read(contextRef.get(), DOM_BLOCK_RULE_FILE);
                    if (!s.equals(now)) {
                        FilesInAppUtil.write(contextRef.get(), DOM_BLOCK_RULE_FILE, s);
                        AdUrlBlocker.instance().updateDomBlockSubscribe(contextRef.get());
                    }
                    if (listener != null) {
                        listener.success("");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    if (listener != null) {
                        listener.failed(e.getMessage());
                    }
                }
            }

            @Override
            public void onFailure(int errorCode, String msg) {
                if (listener != null) {
                    listener.failed(msg);
                }
            }
        });
    }

}

package com.jeffmony.videocache.okhttp;

import com.jeffmony.videocache.utils.HttpUtils;
import com.jeffmony.videocache.utils.LogUtils;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509TrustManager;

import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import okhttp3.Request;

/**
 * okhttp全局管理类
 * 全局唯一的实例
 */
public class OkHttpUtils {
    private static final String TAG = "OkHttpUtils";

    private OkHttpUtils() {}

    public static OkHttpClient getInstance() {
        return Singleton.INSTANCE.getInstance();
    }

    private enum Singleton {
        INSTANCE;

        private OkHttpClient mClient;

        Singleton() {
            OkHttpClient.Builder builder = new OkHttpClient.Builder();
            builder.connectTimeout(60 * 1000L, TimeUnit.MILLISECONDS);
            builder.readTimeout(60 * 1000L, TimeUnit.MILLISECONDS);
            builder.followRedirects(false);
            builder.followSslRedirects(false);
            ConnectionPool connectionPool = new ConnectionPool(50, 5 * 60, TimeUnit.SECONDS);
            builder.connectionPool(connectionPool);
            mClient = builder.build();
        }

        public OkHttpClient getInstance() { return mClient; }
    }

    public static OkHttpClient createOkHttpClient(String url, long readTimeout, long connTimeout, boolean ignoreCert, IHttpPipelineListener listener) {
        OkHttpClient.Builder builder = OkHttpUtils.getInstance().newBuilder();
        builder.readTimeout(readTimeout, TimeUnit.MILLISECONDS);
        builder.connectTimeout(connTimeout, TimeUnit.MILLISECONDS);
        builder.eventListener(new OkHttpEventListener(url, listener));
        //方法一：信任所有证书,不安全有风险
        HttpUtils.SSLParams sslParams1 = HttpUtils.getSslSocketFactory();
        builder.sslSocketFactory(sslParams1.sSLSocketFactory, HttpUtils.UnSafeTrustManager)
                .hostnameVerifier(HttpUtils.UnSafeHostnameVerifier);
        return builder.build();
    }

    /**
     * okhttp 信任证书
     * @param builder
     */
    private static void trustCert(OkHttpClient.Builder builder) {
        X509TrustManager trustManager = new CustomTrustManager();
        SSLSocketFactory sslSocketFactory = null;
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslSocketFactory = sslContext.getSocketFactory();
        } catch (Exception e) {
            LogUtils.w(TAG, "Create SSLSocketFactory failed");
        }
        if (trustManager != null && sslSocketFactory != null) {
            builder.sslSocketFactory(sslSocketFactory, trustManager);
        }
        HostnameVerifier hostnameVerifier = (hostname, session) -> true;
        builder.hostnameVerifier(hostnameVerifier);
    }

    public static Request.Builder createRequestBuilder(String url, Map<String, String> headers, boolean isHeadRequest) {
        Request.Builder requestBuilder;
        if (isHeadRequest) {
            requestBuilder = new Request.Builder().url(url).head();
        } else {
            requestBuilder = new Request.Builder().url(url);
        }
        if (headers != null) {
            Iterator iterator = headers.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, String> entry = (Map.Entry<String, String>) iterator.next();
                requestBuilder.addHeader(entry.getKey(), entry.getValue());
            }
        }
        return requestBuilder;
    }

}

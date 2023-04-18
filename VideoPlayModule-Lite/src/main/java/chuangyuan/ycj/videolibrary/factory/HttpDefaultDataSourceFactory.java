package chuangyuan.ycj.videolibrary.factory;

import android.content.Context;
import android.net.Uri;

import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.util.Util;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import chuangyuan.ycj.videolibrary.upstream.DefaultDataSource;
import chuangyuan.ycj.videolibrary.upstream.DefaultDataSourceFactory;
import chuangyuan.ycj.videolibrary.upstream.HttpsUtils;
import okhttp3.OkHttpClient;
import okhttp3.brotli.BrotliInterceptor;

import static chuangyuan.ycj.videolibrary.upstream.DefaultHttpDataSource.DEFAULT_READ_TIMEOUT_MILLIS;

/**
 * 作者：By 15968
 * 日期：On 2020/6/1
 * 时间：At 23:08
 */
public class HttpDefaultDataSourceFactory implements DataSource.Factory {

    private final Context context;
    private final DataSource.Factory baseDataSourceFactory;

    public static String DEFAULT_UA = "Mozilla/5.0 (Linux; Android 11; Mi 10 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/88.0.4324.152 Mobile Safari/537.36";

    /**
     * Instantiates a new J default data source factory.
     *
     * @param context A context.                for {@link DefaultDataSource}.
     */
    public HttpDefaultDataSourceFactory(Context context) {
        String userAgent = Util.getUserAgent(context, context.getPackageName());
        Map<String, String> headers = new HashMap<>();
        headers.put("User-Agent", userAgent);
        this.context = context.getApplicationContext();
        this.baseDataSourceFactory = init(context, headers, null);
    }

    public HttpDefaultDataSourceFactory(Context context, Map<String, String> headers, Uri uri) {
        this.context = context.getApplicationContext();
        this.baseDataSourceFactory = init(context, headers, uri);
    }

    private DataSource.Factory init(Context context, Map<String, String> headers, Uri uri) {
//        String userAgent = Util.getUserAgent(context, context.getPackageName());
        String userAgent = DEFAULT_UA;
        if (headers != null) {
            if (headers.containsKey("User-Agent")) {
                userAgent = headers.get("User-Agent");
            } else if (headers.containsKey("user-agent")) {
                userAgent = headers.get("user-agent");
            } else if (headers.containsKey("user-Agent")) {
                userAgent = headers.get("user-Agent");
            }
            headers = new HashMap<>(headers);
            headers.remove("User-Agent");
        }
        if (headers == null) {
            headers = new HashMap<>();
        }
        //方法一：信任所有证书,不安全有风险
        HttpsUtils.SSLParams sslParams1 = HttpsUtils.getSslSocketFactory();
        OkHttpClient okHttpClient = null;
        if (uri != null) {
            String url = uri.toString();
            if (url.contains("://127.0.0.1") || url.contains("://192.168.") || url.contains("://0.0.0.") || url.contains("://10.")) {
                okHttpClient = new OkHttpClient.Builder()
                        .addInterceptor(BrotliInterceptor.INSTANCE)
                        .sslSocketFactory(sslParams1.sSLSocketFactory, HttpsUtils.UnSafeTrustManager)
                        .hostnameVerifier(HttpsUtils.UnSafeHostnameVerifier)
                        .readTimeout(DEFAULT_READ_TIMEOUT_MILLIS * 2, TimeUnit.MILLISECONDS)
                        .build();
            }
        }
        if (okHttpClient == null) {
            okHttpClient = new OkHttpClient.Builder()
                    .addInterceptor(BrotliInterceptor.INSTANCE)
                    .sslSocketFactory(sslParams1.sSLSocketFactory, HttpsUtils.UnSafeTrustManager)
                    .hostnameVerifier(HttpsUtils.UnSafeHostnameVerifier)
                    .build();
        }
        OkHttpDataSource.Factory httpDataSourceFactory = new OkHttpDataSource.Factory(okHttpClient)
                .setUserAgent(userAgent);
        httpDataSourceFactory.setDefaultRequestProperties(headers);
        return new DefaultDataSourceFactory(context, null, httpDataSourceFactory);
    }

    @Override
    public DataSource createDataSource() {
        return baseDataSourceFactory.createDataSource();
    }
}

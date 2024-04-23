package com.example.hikerview.ui.download.util;

/**
 * Created by xm on 15/6/11.
 */


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.example.hikerview.service.parser.HttpHelper;
import com.example.hikerview.ui.download.DownloadConfig;
import com.example.hikerview.ui.setting.model.SettingConfig;
import com.example.hikerview.utils.StringUtil;
import com.lzy.okgo.https.HttpsUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;

import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.brotli.BrotliInterceptor;

public class HttpRequestUtil {

    private static final String TAG = "HttpRequestUtil";
    //方法一：信任所有证书,不安全有风险
    private static HttpsUtils.SSLParams sslParams1 = HttpsUtils.getSslSocketFactory();
    private static OkHttpClient okHttpClient = buildOKHttpClient(true);

    private static OkHttpClient buildOKHttpClient(boolean enableH2) {
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .addInterceptor(BrotliInterceptor.INSTANCE)
                .readTimeout(10000, TimeUnit.MILLISECONDS)
                .connectTimeout(5000, TimeUnit.MILLISECONDS)
                .connectionPool(new ConnectionPool(
                        Math.min(128, DownloadConfig.maxConcurrentTask * Math.max(DownloadConfig.m3U8DownloadThreadNum, DownloadConfig.normalFileDownloadThreadNum)),
                        30, TimeUnit.SECONDS))
                .sslSocketFactory(sslParams1.sSLSocketFactory, HttpsUtils.UnSafeTrustManager)
                .hostnameVerifier(HttpsUtils.UnSafeHostnameVerifier);
        if (!enableH2) {
            builder.protocols(Arrays.asList(Protocol.HTTP_1_1, Protocol.HTTP_1_1));
        }
        return builder.build();
    }

    public static void initClient(boolean enableH2) {
        okHttpClient = buildOKHttpClient(enableH2);
    }

    public static Map<String, String> commonHeaders;

    public final static HostnameVerifier DO_NOT_VERIFY = (hostname, session) -> true;

    static {
        commonHeaders = new HashMap<>();
        commonHeaders.put("User-Agent", SettingConfig.getMobileUA());
    }

    public static StreamResponse sendGetRequest(String url, Map<String, String> headers) throws IOException {
        if (headers == null) {
            headers = commonHeaders;
        } else {
            if (!headers.containsKey("User-Agent") && commonHeaders.containsKey("User-Agent")) {
                headers.put("User-Agent", commonHeaders.get("User-Agent"));
            }
        }
        Map<String, Object> options = new HashMap<>();
        options.put("method", "GET");
        options.put("inputStream", true);
        options.put("withHeaders", true);
        options.put("timeout", 30000);
        options.put("headers", headers);
        try {
            JSONObject json = (JSONObject) HttpHelper.fetch0(url, options, null, (path, toHex, inputStream) -> false, okHttpClient);
            if (json == null) {
                throw new IOException("fetch " + url + " failed");
            }
            if (StringUtil.isNotEmpty(json.getString("error"))) {
                throw new IOException(json.getString("error"));
            }
            InputStream inputStream = (InputStream) json.get("body");
            if (inputStream == null) {
                throw new IOException("fetch " + url + " failed");
            }
            return new StreamResponse(json.getString("url"), json.getIntValue("statusCode"), inputStream);
        } catch (Exception e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    public static StringResponse getResponseString(String url, Map<String, String> headers) throws IOException {
        if (headers == null) {
            headers = commonHeaders;
        } else {
            if (!headers.containsKey("User-Agent") && commonHeaders.containsKey("User-Agent")) {
                headers.put("User-Agent", commonHeaders.get("User-Agent"));
            }
        }
        Map<String, Object> options = new HashMap<>();
        options.put("method", "GET");
        options.put("headers", headers);
        options.put("timeout", 30000);
        options.put("withHeaders", true);
        try {
            String obj = (String) HttpHelper.fetch0(url, options, null, (path, toHex, inputStream) -> false, okHttpClient);
            if (obj.isEmpty()) {
                throw new IOException("fetch " + url + " failed");
            }
            JSONObject json = JSON.parseObject(obj);
            if (StringUtil.isNotEmpty(json.getString("error"))) {
                throw new IOException(json.getString("error"));
            }
            String response = json.getString("body");
            if (response == null) {
                throw new IOException("fetch " + url + " failed");
            }
            return new StringResponse(json.getString("url"), response);
        } catch (Exception e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    public static final class StreamResponse {
        public StreamResponse(String url, int statusCode, InputStream body) {
            this.url = url;
            this.statusCode = statusCode;
            this.body = body;
        }

        public String url;

        public int getStatusCode() {
            return statusCode;
        }

        public int statusCode;

        public InputStream getBody() {
            return body;
        }

        public InputStream body;

        public StreamResponse() {
        }

    }

    public static final class StringResponse {
        public StringResponse(String realUrl, String body) {
            this.realUrl = realUrl;
            this.body = body;
        }

        public String realUrl;
        public String body;

        public StringResponse() {
        }

    }

    public static HeadRequestResponse performHeadRequest(String url) throws IOException {
        return performHeadRequest(url, commonHeaders);
    }

    public static HeadRequestResponse performHeadRequest(String url, Map<String, String> headers) throws IOException {
        if (headers == null) {
            headers = commonHeaders;
        } else {
            if (!headers.containsKey("User-Agent") && commonHeaders.containsKey("User-Agent")) {
                headers.put("User-Agent", commonHeaders.get("User-Agent"));
            }
        }
        Map<String, Object> options = new HashMap<>();
        options.put("onlyHeaders", true);
        options.put("method", "GET");
        options.put("timeout", 30000);
        options.put("headers", headers);
        try {
            String obj = (String) HttpHelper.fetch0(url, options, null, (path, toHex, inputStream) -> false, okHttpClient);
            if (obj.isEmpty()) {
                throw new IOException("fetch " + url + " failed");
            }
            JSONObject json = JSON.parseObject(obj);
            if (StringUtil.isNotEmpty(json.getString("error"))) {
                throw new IOException(json.getString("error"));
            }
            Map<String, List<String>> hd = (Map<String, List<String>>) json.get("headers");
            if (hd == null) {
                throw new IOException("fetch " + url + " failed");
            }
            return new HeadRequestResponse(json.getString("url"), capitalizeHeaders(hd));
        } catch (Exception e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    /**
     * content-type转成Content-Type
     *
     * @param headers
     * @return
     */
    public static Map<String, List<String>> capitalizeHeaders(Map<String, List<String>> headers) {
        if (headers == null) {
            return null;
        }
        if (!headers.containsKey("content-type") && !headers.containsKey("content-length")) {
            //没必要处理，直接返回
            return headers;
        }
        for (String s : new ArrayList<>(headers.keySet())) {
            String[] keys = s.split("-");
            for (int i = 0; i < keys.length; i++) {
                if (keys[i].length() > 1) {
                    keys[i] = keys[i].substring(0, 1).toUpperCase() + keys[i].substring(1);
                }
            }
            String k = StringUtil.arrayToString(keys, 0, "-");
            headers.put(k, headers.get(s));
        }
        return headers;
    }

    public static class HeadRequestResponse {
        private String realUrl;
        private Map<String, List<String>> headerMap;

        public HeadRequestResponse() {
        }

        public HeadRequestResponse(String realUrl, Map<String, List<String>> headerMap) {
            this.realUrl = realUrl;
            this.headerMap = headerMap;
        }

        public String getRealUrl() {
            return realUrl;
        }

        public void setRealUrl(String realUrl) {
            this.realUrl = realUrl;
        }

        public Map<String, List<String>> getHeaderMap() {
            return headerMap;
        }

        public void setHeaderMap(Map<String, List<String>> headerMap) {
            this.headerMap = headerMap;
        }
    }

}
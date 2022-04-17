package com.example.hikerview.service.parser;

import androidx.annotation.Nullable;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.example.hikerview.service.http.ContentTypeAfterInterceptor;
import com.example.hikerview.service.http.ContentTypePreInterceptor;
import com.example.hikerview.utils.StringUtil;
import com.lzy.okgo.OkGo;
import com.lzy.okgo.adapter.Call;
import com.lzy.okgo.convert.Converter;
import com.lzy.okgo.https.HttpsUtils;
import com.lzy.okgo.interceptor.HttpLoggingInterceptor;
import com.lzy.okgo.model.HttpHeaders;
import com.lzy.okgo.request.PostRequest;
import com.lzy.okgo.request.PutRequest;
import com.lzy.okgo.request.base.BodyRequest;

import org.jetbrains.annotations.NotNull;
import org.joor.Reflect;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.brotli.BrotliInterceptor;
import timber.log.Timber;

/**
 * 作者：By 15968
 * 日期：On 2021/12/16
 * 时间：At 21:21
 */

public class HttpHelper {
    public static final ThreadLocal<Map<String, Object>> threadMap = new ThreadLocal<>();

    private static final long HTTP_TIMEOUT_MILLISECONDS = 10000;
    public static OkHttpClient noRedirectHttpClient;

    public static String get(String url, Map<String, String> headers) {
        Map<String, Object> options = new HashMap<>();
        options.put("headers", headers);
        return fetch(url, options);
    }

    public static String post(String url, Map<String, String> headers) {
        Map<String, Object> options = new HashMap<>();
        options.put("headers", headers);
        return fetch(url, generatePostOps(options));
    }

    public static String fetch(String url, Map<String, Object> op) {
        return fetch(url, op, null, (path, toHex) -> false);
    }

    public static String fetch(String url, Map<String, Object> op, HeadersInterceptor headersInterceptor, RuleFetchDelegate ruleFetchDelegate) {
        Timber.d("fetch, %s", url);
        long start = System.currentTimeMillis();
        try {
            if (isEmpty(url)) {
                return "";
            }
            String contentType = null;
            boolean withHeaders = false;
            boolean withStatusCode = false;
            boolean redirect = true;
            boolean toHex = false;
            boolean onlyHeaders = false;
            int timeout = -1;
            Map<String, String> headerMap = null;
            Map<String, String> params = new LinkedHashMap<>();

            //默认为空，okhttp自动识别
            String charset = null;
            String method = "GET";
            com.lzy.okgo.request.base.Request<String, ?> request;
            try {
                if (op != null) {
                    headerMap = (Map<String, String>) op.get("headers");
                    if (headerMap == null) {
                        headerMap = (Map<String, String>) op.get("header");
                    }
                    if (op.containsKey("withHeaders")) {
                        try {
                            withHeaders = (Boolean) op.get("withHeaders");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    if (op.containsKey("onlyHeaders")) {
                        try {
                            onlyHeaders = (Boolean) op.get("onlyHeaders");
                            if (onlyHeaders) {
                                withHeaders = true;
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    if (op.containsKey("withStatusCode")) {
                        try {
                            withStatusCode = (Boolean) op.get("withStatusCode");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    if (op.containsKey("redirect")) {
                        try {
                            redirect = (Boolean) op.get("redirect");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    if (op.containsKey("timeout")) {
                        try {
                            timeout = Integer.parseInt(JSON.toJSONString(op.get("timeout")));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    if (op.containsKey("toHex")) {
                        try {
                            toHex = (Boolean) op.get("toHex");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    Object hiker = ruleFetchDelegate.fetch(url, toHex);
                    if (hiker instanceof String) {
                        return (String) hiker;
                    }
                    String body = null;
                    if (headerMap != null) {
                        contentType = headerMap.get("content-type");
                        if (isEmpty(contentType)) {
                            contentType = headerMap.get("Content-Type");
                        }
                        if (isEmpty(contentType)) {
                            contentType = headerMap.get("Content-type");
                        }
                        body = headerMap.get("body");
                    }
                    method = (String) op.get("method");
                    if ("HEAD".equalsIgnoreCase(method)) {
                        request = OkGo.head(url);
                        withHeaders = true;
                    } else {
                        request = OkGo.get(url);
                    }
                    if (op.containsKey("body")) {
                        Object b = op.get("body");
                        if (b instanceof String) {
                            body = (String) b;
                        } else {
                            if (!"PUT".equalsIgnoreCase(method) && !"GET".equalsIgnoreCase(method) && !"HEAD".equalsIgnoreCase(method)) {
                                method = "POST";
                            }
                            body = JSON.toJSONString(b);
                        }
                    }
                    if (contentType != null && contentType.split("charset=").length > 1) {
                        charset = contentType.split("charset=")[1];
                    } else if (contentType != null && contentType.split("charst=").length > 1) {
                        //自己挖的坑，总是要填的
                        charset = contentType.split("charst=")[1];
                    }
                    if ("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method)) {
                        request = "PUT".equalsIgnoreCase(method) ? OkGo.put(url) : OkGo.post(url);
                        if (isEmpty(body)) {
                            //empty
                        } else if (contentType != null && contentType.contains("application/json")) {
                            ((BodyRequest) request).upString(body, MediaType.parse(contentType));
                        } else if ((body.startsWith("{") && body.endsWith("}")) || (body.startsWith("[") && body.endsWith("]"))) {
                            ((BodyRequest) request).upJson(body);
                        } else {
                            for (String form : body.split("&")) {
                                int split = form.indexOf("=");
                                params.put(decodeConflictStr(form.substring(0, split)), decodeConflictStr(form.substring(split + 1)));
                            }
                            request.params(params);
                            if (StringUtil.isNotEmpty(contentType)) {
                                try {
                                    Reflect.on(request).set("mediaType", MediaType.parse(contentType));
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                } else {
                    Object hiker = ruleFetchDelegate.fetch(url, toHex);
                    if (hiker instanceof String) {
                        return (String) hiker;
                    } else {
                        request = OkGo.get(url);
                    }
                }
                if (!redirect && noRedirectHttpClient == null && timeout <= 0) {
                    buildOkHttpClient();
                }

                HttpHeaders httpHeaders = new HttpHeaders();
                if (headerMap != null && !headerMap.isEmpty()) {
                    for (Map.Entry<String, String> entry : headerMap.entrySet()) {
                        httpHeaders.put(entry.getKey(), entry.getValue());
                    }
                }
                if (isNotEmpty(charset) && !"UTF-8".equalsIgnoreCase(charset)
                        && (("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method)))) {
                    FormBody.Builder bodyBuilder = new FormBody.Builder(Charset.forName(charset));
                    for (String key : request.getParams().urlParamsMap.keySet()) {
                        List<String> urlValues = request.getParams().urlParamsMap.get(key);
                        if (isNotEmpty(urlValues)) {
                            for (String value : urlValues) {
                                bodyBuilder.add(key, value);
                            }
                        }
                    }
                    if ("PUT".equalsIgnoreCase(method)) {
                        ((PutRequest<?>) request).upRequestBody(bodyBuilder.build());
                    } else {
                        ((PostRequest<?>) request).upRequestBody(bodyBuilder.build());
                    }
                    request.removeAllParams();
                }
                boolean finalWithHeaders = withHeaders;
                boolean finalWithStatusCode = withStatusCode;
                request.headers(httpHeaders);
                //需要禁止重定向，必须单独使用client
                if (!redirect && noRedirectHttpClient != null) {
                    request.client(noRedirectHttpClient);
                }
                if (timeout > 0) {
                    request.client(buildOkHttpClient(timeout, redirect));
                } else {
                    if (!redirect && noRedirectHttpClient != null) {
                        request.client(noRedirectHttpClient);
                    }
                }
                request.retryCount(0);
                Object tag = getTagFromThread();
                if (tag != null) {
                    request.tag(tag);
                }
                FetchResponse fetchResponse = new FetchResponse();
                fetchResponse.realUrl = url;

                if (onlyHeaders || headersInterceptor != null) {
                    CountDownLatch countDownLatch = new CountDownLatch(1);
                    try {
                        boolean finalOnlyHeaders = onlyHeaders;
                        boolean finalToHex = toHex;
                        String finalCharset = charset;
                        request.converter(toHex ? new ByteHexConvert() : new CharsetStringConvert(charset))
                                .getRawCall().enqueue(new Callback() {
                            @Override
                            public void onFailure(okhttp3.@NotNull Call call, @NotNull IOException e) {
                                e.printStackTrace();
                                fetchResponse.error = e.getMessage();
                                countDownLatch.countDown();
                            }

                            @Override
                            public void onResponse(okhttp3.@NotNull Call call, @NotNull Response response) throws IOException {
                                try {
                                    fetchResponse.headers = response.headers().toMultimap();
                                    if (!finalOnlyHeaders && !headersInterceptor.intercept(fetchResponse.headers)) {
                                        try {
                                            if (finalToHex) {
                                                fetchResponse.fetchResult = new ByteHexConvert().convertResponse(response);
                                            } else {
                                                fetchResponse.fetchResult = new CharsetStringConvert(finalCharset).convertResponse(response);
                                            }
                                        } catch (Throwable throwable) {
                                            throwable.printStackTrace();
                                            fetchResponse.error = throwable.getMessage();
                                        }
                                    }
                                    fetchResponse.realUrl = response.request().url().toString();
                                    fetchResponse.statusCode = response.code();
                                } finally {
                                    countDownLatch.countDown();
                                    try {
                                        call.cancel();
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                        fetchResponse.error = e.getMessage();
                        try {
                            countDownLatch.countDown();
                        } catch (Exception exception) {
                            exception.printStackTrace();
                        }
                    }
                    countDownLatch.await(10, TimeUnit.SECONDS);
                } else {
                    Call<String> call = request.converter(toHex ? new ByteHexConvert() : new CharsetStringConvert(charset)).adapt();
                    com.lzy.okgo.model.Response<String> response = call.execute();
                    fetchResponse.headers = response.headers() == null ? null : response.headers().toMultimap();
                    if (response.getException() != null) {
                        Timber.e(response.getException());
                        fetchResponse.error = response.getException().getMessage();
                        fetchResponse.fetchResult = response.body();
                        if (fetchResponse.fetchResult == null && response.getRawResponse() != null) {
                            try (ResponseBody responseBody = response.getRawResponse().body()) {
                                if (responseBody != null) {
                                    fetchResponse.fetchResult = responseBody.string();
                                }
                            } catch (Throwable e) {
                                Timber.e(e);
                            }
                        }
                    } else {
                        fetchResponse.fetchResult = response.body();
                    }
                    fetchResponse.realUrl = response.getRawResponse() != null ? response.getRawResponse().request().url().toString() : url;
                    fetchResponse.statusCode = response.code();
                }
                long end = System.currentTimeMillis();
                Timber.d("js, fetch: consume=%s, %s", (end - start), url);
                if (finalWithHeaders || finalWithStatusCode) {
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("body", fetchResponse.fetchResult);
                    jsonObject.put("headers", fetchResponse.headers);
                    jsonObject.put("url", fetchResponse.realUrl);
                    jsonObject.put("statusCode", fetchResponse.statusCode);
                    jsonObject.put("error", fetchResponse.error);
                    fetchResponse.fetchResult = jsonObject.toJSONString();
                }
                return fetchResponse.fetchResult == null ? "" : fetchResponse.fetchResult;
            } catch (Throwable e) {
                Timber.e(e);
                return "";
            }
        } catch (Throwable e) {
            Timber.e(e);
            return "";
        }
    }

    public static void addTagForThread(Object tag) {
        Map<String, Object> map = threadMap.get();
        if (map == null) {
            map = new HashMap<>();
            threadMap.set(map);
        }
        map.put("_tag", tag);
    }

    public static Object getTagFromThread() {
        Map<String, Object> map = threadMap.get();
        if (map == null) {
            return null;
        }
        return map.get("_tag");
    }

    public static void cancelByTag(Object tag) {
        try {
            if (noRedirectHttpClient != null) {
                OkGo.cancelTag(noRedirectHttpClient, tag);
            }
            OkGo.getInstance().cancelTag(tag);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Map<String, Object> generatePostOps(Map<String, Object> op) {
        if (op != null) {
            if (op.containsKey("body")) {
                Object body;
                Object b = op.get("body");
                if (b instanceof String) {
                    body = b;
                } else if (b instanceof JSONObject) {
                    body = buildParamStr((JSONObject) b);
                } else {
                    body = JSON.toJSONString(b);
                }
                op.put("body", body);
            }
            op.put("method", "POST");
            return op;
        } else {
            op = new HashMap<>();
            op.put("method", "POST");
            return op;
        }
    }

    public static String buildParamStr(JSONObject jsonObject) {
        Set<String> keys = jsonObject.keySet();
        String[] kv = new String[keys.size()];
        int i = 0;
        for (String key : keys) {
            Object v = jsonObject.get(key);
            String value;
            if (v instanceof String) {
                value = (String) v;
            } else {
                value = JSON.toJSONString(v);
            }
            kv[i] = key + "=" + value;
            i++;
        }
        return arrayToString(kv, 0, "&");
    }

    public static String arrayToString(String[] list, int fromIndex, String cha) {
        return arrayToString(list, fromIndex, list == null ? 0 : list.length, cha);
    }

    public static String arrayToString(String[] list, int fromIndex, int endIndex, String cha) {
        StringBuilder builder = new StringBuilder();
        if (list == null || list.length <= fromIndex) {
            return "";
        } else if (list.length <= 1) {
            return list[0];
        } else {
            builder.append(list[fromIndex]);
        }
        for (int i = 1 + fromIndex; i < list.length && i < endIndex; i++) {
            builder.append(cha).append(list[i]);
        }
        return builder.toString();
    }

    private static void buildOkHttpClient() {
        noRedirectHttpClient = getNoRedirectHttpClient();
    }

    private static OkHttpClient buildOkHttpClient(int timeout, boolean redirect) {
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor("OkGo");
        loggingInterceptor.setPrintLevel(HttpLoggingInterceptor.Level.BODY);
        loggingInterceptor.setColorLevel(Level.INFO);
        HttpsUtils.SSLParams sslParams = HttpsUtils.getSslSocketFactory();
        OkHttpClient.Builder builder = OkGo.getInstance().getOkHttpClient().newBuilder()
                .sslSocketFactory(sslParams.sSLSocketFactory, sslParams.trustManager)
                .hostnameVerifier(HttpsUtils.UnSafeHostnameVerifier);
        if (!redirect) {
            builder.followRedirects(false)
                    .followSslRedirects(false);
        }
        return builder.addInterceptor(loggingInterceptor)
                .addInterceptor(BrotliInterceptor.INSTANCE)
                .readTimeout(timeout, TimeUnit.MILLISECONDS)
                .writeTimeout(timeout, TimeUnit.MILLISECONDS)
                .connectTimeout(timeout, TimeUnit.MILLISECONDS)
                .build();
    }

    public static OkHttpClient getNoRedirectHttpClient() {
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor("OkGo");
        loggingInterceptor.setPrintLevel(HttpLoggingInterceptor.Level.BODY);
        loggingInterceptor.setColorLevel(Level.INFO);
        HttpsUtils.SSLParams sslParams = HttpsUtils.getSslSocketFactory();
        return OkGo.getInstance().getOkHttpClient().newBuilder()
                .addInterceptor(BrotliInterceptor.INSTANCE)
                .addInterceptor(ContentTypePreInterceptor.INSTANCE)
                .addNetworkInterceptor(ContentTypeAfterInterceptor.INSTANCE)
                .sslSocketFactory(sslParams.sSLSocketFactory, sslParams.trustManager)
                .hostnameVerifier(HttpsUtils.UnSafeHostnameVerifier)
                .followRedirects(false)
                .followSslRedirects(false)
                .addInterceptor(loggingInterceptor)
                .readTimeout(HTTP_TIMEOUT_MILLISECONDS, TimeUnit.MILLISECONDS)
                .writeTimeout(HTTP_TIMEOUT_MILLISECONDS, TimeUnit.MILLISECONDS)
                .connectTimeout(HTTP_TIMEOUT_MILLISECONDS, TimeUnit.MILLISECONDS)
                .build();
    }


    public static boolean isEmpty(Collection<?> collection) {
        return (collection == null || collection.isEmpty());
    }

    public static boolean isNotEmpty(Collection<?> collection) {
        return !isEmpty(collection);
    }

    public static boolean isEmpty(@Nullable CharSequence str) {
        return str == null || str.length() == 0;
    }

    public static boolean isNotEmpty(@Nullable CharSequence str) {
        return !isEmpty(str);
    }

    public static String decodeConflictStr(String str) {
        if (isEmpty(str)) {
            return str;
        }
        return str.replace("？？", "?").replace("＆＆", "&").replace("；；", ";");
    }

    public static String bytesToHexString(byte[] src) {
        StringBuilder stringBuilder = new StringBuilder();
        if (src == null || src.length <= 0) {
            return null;
        }
        for (byte aSrc : src) {
            int v = aSrc & 0xFF;
            String hv = Integer.toHexString(v);
            if (hv.length() < 2) {
                stringBuilder.append(0);
            }
            stringBuilder.append(hv);
        }
        return stringBuilder.toString();
    }

    public interface HeadersInterceptor {
        boolean intercept(Map<String, List<String>> headers);
    }

    public interface RuleFetchDelegate {
        Object fetch(String path, boolean toHex);
    }

    public static final class FetchResponse {
        String realUrl;
        Map<String, List<String>> headers = null;
        int statusCode = 0;
        String error = null;
        String fetchResult;
    }

    private static class CharsetStringConvert implements Converter<String> {

        private final String charset;

        public CharsetStringConvert(String charset) {
            this.charset = charset;
        }

        @Override
        public String convertResponse(Response response) throws Throwable {
            try (ResponseBody body = response.body()) {
                Headers headers = response.headers();
                long length = getContentLength(headers);
                if (length > 1024 * 1024 * 20) {
                    //超过了20MB，不应该是字符串，属于乱用
                    throw new IOException("content-length is too big: " + length);
                }
                if (isEmpty(charset)) {
                    if (body != null) {
                        try {
                            return body.string();
                        } catch (IOException e) {
                            e.printStackTrace();
                            //部分情况下会抛异常
                            return "";
                        }
                    }
                    return "";
                }
                byte[] b = new byte[0];
                if (body != null) {
                    try {
                        b = body.bytes();
                    } catch (IOException e) {
                        e.printStackTrace();
                        //部分情况下获取byte会抛异常
                        return "";
                    }
                }
                return new String(b, charset);
            }
        }
    }

    private static long getContentLength(Headers headers) {
        String len = headers.get("Content-Length");
        if (len != null && !len.isEmpty()) {
            try {
                return Long.parseLong(len);
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
        return 0;
    }

    private static class ByteHexConvert implements Converter<String> {

        public ByteHexConvert() {
        }

        @Override
        public String convertResponse(Response response) throws Throwable {
            try (ResponseBody body = response.body()) {
                if (body != null) {
                    return bytesToHexString(body.bytes());
                }
                return "";
            }
        }
    }
} 
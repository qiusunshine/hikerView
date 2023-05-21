package com.jeffmony.videocache.okhttp;

import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.jeffmony.videocache.utils.ProxyCacheUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class OkHttpControl {

    private static final String TAG = "OkHttpControl";

    private String mUrl;
    private final Map<String, String> mHeaders;
    private final long mReadTimeout;
    private final long mConnTimeout;
    private final boolean mIgnoreCert;
    private final boolean mIsHeadRequest;

    private int mRedirectCount = 0;
    private Response mResponse;
    private OkHttpClient mOkHttpClient;
    private Request.Builder mRequestBuilder;
    private IHttpPipelineListener mHttpPipelineListener;

    public OkHttpControl(String url, Map<String, String> headers, boolean isHeadRequest, @NonNull IHttpPipelineListener listener, @NonNull NetworkConfig config) {
        mUrl = url;
        mHeaders = headers;
        mIsHeadRequest = isHeadRequest;
        mReadTimeout = config.getReadTimeout();
        mConnTimeout = config.getConnTimeout();
        mIgnoreCert = config.ignoreCert();
        mHttpPipelineListener = listener;

        mOkHttpClient = OkHttpUtils.createOkHttpClient(url, mReadTimeout, mConnTimeout, mIgnoreCert, mHttpPipelineListener);
        mRequestBuilder = OkHttpUtils.createRequestBuilder(url, mHeaders, mIsHeadRequest);
    }

    public void markRequest() throws IOException {
        mResponse = mOkHttpClient.newCall(mRequestBuilder.build()).execute();
        if (shouldRedirect()) {
            mRedirectCount++;

            //mUrl已经发生了变化了
            mOkHttpClient = OkHttpUtils.createOkHttpClient(mUrl, mReadTimeout, mConnTimeout, mIgnoreCert, mHttpPipelineListener);
            mRequestBuilder = OkHttpUtils.createRequestBuilder(mUrl, mHeaders, mIsHeadRequest);

            //重新请求
            markRequest();
        }
    }

    /**
     * 是否是重定向的请求
     *
     * 发生重定向请求之后需要重新修改Location中的url的
     * @return
     */
    private boolean shouldRedirect() {
        if (mResponse == null) return false;
        int code = mResponse.code();
        if (code == 300 || code == 301 || code == 302 || code == 303 || code == 307 || code == 308) {
            String url = mResponse.header("Location");
            if (TextUtils.isEmpty(url)) {
                return false;
            }
            mUrl = url;
            return true;
        }
        return false;
    }

    public String getFinalUrl() { return mUrl; }

    public int getRedirectCount() { return mRedirectCount; }

    /**
     * 获取资源的contentLength
     * @return
     */
    public long getContentLength() {
        if (mResponse == null) {
            return -1;
        }
        if (mResponse.code() == 200 || mResponse.code() == 206) {
            String contentLength = mResponse.header("content-length");
            if (TextUtils.isEmpty(contentLength)) {
                return -1;
            }
            return Long.parseLong(contentLength);
        }
        return -1;
    }

    /**
     * 获取请求资源的contentType
     * @return
     */
    public String getContentType() {
        if (mResponse == null) {
            return null;
        }
        if (mResponse.code() == 200 || mResponse.code() == 206) {
            return mResponse.header("content-type");
        }
        return null;
    }

    public InputStream getResponseBody() {
        if (mResponse == null) {
            return null;
        }
        if (mResponse.code() == 200 || mResponse.code() == 206) {
            return mResponse.body().byteStream();
        } else {
            ProxyCacheUtils.close(mResponse.body().byteStream());
            return null;
        }
    }

    /**
     * Content-Range: bytes 0-X/Y
     * Content-Range: bytes 0-X/*
     * Content-Range: bytes *X/Y
     *
     * @return
     */
    public long parseContentLengthFromContentRange() {
        if (mResponse == null) {
            return -1;
        }

        if (mResponse.code() == 200 || mResponse.code() ==206) {
            String contentRange = mResponse.header("Content-Range");
            if (TextUtils.isEmpty(contentRange)) {
                return -1;
            }
            int index = contentRange.lastIndexOf("/");
            if (index == -1 || index + 1 >= contentRange.length()) {
                return -1;
            }
            String contentLength = contentRange.substring(index + 1).trim();
            try {
                return Long.parseLong(contentLength);
            } catch (Exception e) {
                return -1;
            }
        }
        return -1;
    }
}

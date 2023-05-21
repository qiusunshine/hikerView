package com.jeffmony.videocache.okhttp;

import android.text.TextUtils;

import androidx.annotation.NonNull;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.List;

import okhttp3.Call;
import okhttp3.Connection;
import okhttp3.EventListener;
import okhttp3.Handshake;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;

public class OkHttpEventListener extends EventListener {

    private final String mUrl;
    private final IHttpPipelineListener mListener;
    private long mStartTime;

    public OkHttpEventListener(String url, @NonNull IHttpPipelineListener listener) {
        mUrl = url;
        mListener = listener;
    }

    private long getCurrentTimeDuration() {
        return (System.nanoTime() - mStartTime) / 1000000;
    }

    @Override
    public void callStart(@NotNull Call call) {
        super.callStart(call);

        //获取当前的header头部
        String rangeHeader = call.request().header("Range");
        if (!TextUtils.isEmpty(rangeHeader)) {
            mListener.onRequestStart(mUrl, rangeHeader);
        }
        mStartTime = System.nanoTime();
    }

    @Override
    public void dnsStart(@NotNull Call call, @NotNull String domainName) {
        super.dnsStart(call, domainName);
        mListener.onDnsStart(mUrl, getCurrentTimeDuration());
    }

    @Override
    public void dnsEnd(@NotNull Call call, @NotNull String domainName, @NotNull List<InetAddress> inetAddressList) {
        super.dnsEnd(call, domainName, inetAddressList);
        mListener.onDnsEnd(mUrl, getCurrentTimeDuration());
    }

    @Override
    public void connectStart(@NotNull Call call, @NotNull InetSocketAddress inetSocketAddress, @NotNull Proxy proxy) {
        super.connectStart(call, inetSocketAddress, proxy);
        mListener.onConnectStart(mUrl, getCurrentTimeDuration());
    }

    @Override
    public void secureConnectStart(@NotNull Call call) {
        super.secureConnectStart(call);
    }

    @Override
    public void secureConnectEnd(@NotNull Call call, @Nullable Handshake handshake) {
        super.secureConnectEnd(call, handshake);
    }

    @Override
    public void connectEnd(@NotNull Call call, @NotNull InetSocketAddress inetSocketAddress, @NotNull Proxy proxy, @Nullable Protocol protocol) {
        super.connectEnd(call, inetSocketAddress, proxy, protocol);
        mListener.onConnectEnd(mUrl, getCurrentTimeDuration());
    }

    @Override
    public void connectFailed(@NotNull Call call, @NotNull InetSocketAddress inetSocketAddress, @NotNull Proxy proxy, @Nullable Protocol protocol, @NotNull IOException ioe) {
        super.connectFailed(call, inetSocketAddress, proxy, protocol, ioe);
        mListener.onConnectFailed(mUrl, getCurrentTimeDuration(), ioe);
    }

    @Override
    public void connectionAcquired(@NotNull Call call, @NotNull Connection connection) {
        super.connectionAcquired(call, connection);
        mListener.onConnectAcquired(mUrl, getCurrentTimeDuration());
    }

    @Override
    public void connectionReleased(@NotNull Call call, @NotNull Connection connection) {
        super.connectionReleased(call, connection);
        mListener.onConnectRelease(mUrl, getCurrentTimeDuration());
    }

    @Override
    public void requestHeadersStart(@NotNull Call call) {
        super.requestHeadersStart(call);
        mListener.onRequestHeaderStart(mUrl, getCurrentTimeDuration());
    }

    @Override
    public void requestHeadersEnd(@NotNull Call call, @NotNull Request request) {
        super.requestHeadersEnd(call, request);
        mListener.onRequestHeaderEnd(mUrl, getCurrentTimeDuration());
    }

    @Override
    public void requestBodyStart(@NotNull Call call) {
        super.requestBodyStart(call);
        mListener.onRequestBodyStart(mUrl, getCurrentTimeDuration());
    }

    @Override
    public void requestBodyEnd(@NotNull Call call, long byteCount) {
        super.requestBodyEnd(call, byteCount);
        mListener.onRequestBodyEnd(mUrl, getCurrentTimeDuration());
    }

    @Override
    public void responseHeadersStart(@NotNull Call call) {
        super.responseHeadersStart(call);
        mListener.onResponseHeaderStart(mUrl, getCurrentTimeDuration());
    }

    @Override
    public void responseHeadersEnd(@NotNull Call call, @NotNull Response response) {
        super.responseHeadersEnd(call, response);
        mListener.onResponseHeaderEnd(mUrl, getCurrentTimeDuration());
    }

    @Override
    public void responseBodyStart(@NotNull Call call) {
        super.responseBodyStart(call);
        mListener.onResponseBodyStart(mUrl, getCurrentTimeDuration());
    }

    @Override
    public void responseBodyEnd(@NotNull Call call, long byteCount) {
        super.responseBodyEnd(call, byteCount);
        mListener.onResponseBodyEnd(mUrl, getCurrentTimeDuration());
    }

    @Override
    public void callEnd(@NotNull Call call) {
        super.callEnd(call);
        mListener.onResponseEnd(mUrl, getCurrentTimeDuration());
    }

    @Override
    public void callFailed(@NotNull Call call, @NotNull IOException ioe) {
        super.callFailed(call, ioe);
        mListener.onFailed(mUrl, getCurrentTimeDuration(), ioe);
    }
}

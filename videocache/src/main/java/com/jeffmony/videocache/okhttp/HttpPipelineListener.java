package com.jeffmony.videocache.okhttp;

public abstract class HttpPipelineListener implements IHttpPipelineListener {

    @Override
    public void onRequestStart(String url, String rangeHeader) {
    }

    @Override
    public void onDnsStart(String url, long timeDuration) {
    }

    @Override
    public void onDnsEnd(String url, long timeDuration) {
    }

    @Override
    public void onConnectStart(String url, long timeDuration) {
    }

    @Override
    public void onConnectEnd(String url, long timeDuration) {
    }

    @Override
    public void onConnectFailed(String url, long timeDuration, Exception e) {
    }

    @Override
    public void onConnectAcquired(String url, long timeDuration) {
    }

    @Override
    public void onConnectRelease(String url, long timeDuration) {
    }

    @Override
    public void onRequestHeaderStart(String url, long timeDuration) {
    }

    @Override
    public void onRequestHeaderEnd(String url, long timeDuration) {
    }

    @Override
    public void onRequestBodyStart(String url, long timeDuration) {
    }

    @Override
    public void onRequestBodyEnd(String url, long timeDuration) {
    }

    @Override
    public void onResponseHeaderStart(String url, long timeDuration) {
    }

    @Override
    public void onResponseHeaderEnd(String url, long timeDuration) {
    }

    @Override
    public void onResponseBodyStart(String url, long timeDuration) {
    }

    @Override
    public void onResponseBodyEnd(String url, long timeDuration) {
    }

    @Override
    public void onResponseEnd(String url, long timeDuration) {
    }

    @Override
    public void onFailed(String url, long timeDuration, Exception e) {
    }
}

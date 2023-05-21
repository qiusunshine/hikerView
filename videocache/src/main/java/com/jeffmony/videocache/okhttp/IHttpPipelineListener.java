package com.jeffmony.videocache.okhttp;

/**
 * 记录http pipeline流水线的时间点
 *
 */
public interface IHttpPipelineListener {
    //起始的请求时间点
    //start 时间点为0
    void onRequestStart(String url, String rangeHeader);

    //timeDuration时距离起始时间点的耗时时间段
    void onDnsStart(String url, long timeDuration);

    void onDnsEnd(String url, long timeDuration);

    void onConnectStart(String url, long timeDuration);

    void onConnectEnd(String url, long timeDuration);

    void onConnectFailed(String url, long timeDuration, Exception e);

    void onConnectAcquired(String url, long timeDuration);

    void onConnectRelease(String url, long timeDuration);

    void onRequestHeaderStart(String url, long timeDuration);

    void onRequestHeaderEnd(String url, long timeDuration);

    void onRequestBodyStart(String url, long timeDuration);

    void onRequestBodyEnd(String url, long timeDuration);

    void onResponseHeaderStart(String url, long timeDuration);

    void onResponseHeaderEnd(String url, long timeDuration);

    void onResponseBodyStart(String url, long timeDuration);

    void onResponseBodyEnd(String url, long timeDuration);

    void onResponseEnd(String url, long timeDuration);

    void onFailed(String url, long timeDuration, Exception e);
}

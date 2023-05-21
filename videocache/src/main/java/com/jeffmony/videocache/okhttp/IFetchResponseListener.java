package com.jeffmony.videocache.okhttp;

public interface IFetchResponseListener {

    //通过一条请求获取contentLength
    void onContentLength(long contentLength);
}

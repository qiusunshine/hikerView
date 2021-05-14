package com.example.hikerview.ui.download;

/**
 * 作者：By 15968
 * 日期：On 2019/12/4
 * 时间：At 23:18
 */
public interface DetectListener {
    void onSuccess(VideoInfo videoInfo);
    void onFailed(String msg);
    void onProgress(int progress, String msg);
}

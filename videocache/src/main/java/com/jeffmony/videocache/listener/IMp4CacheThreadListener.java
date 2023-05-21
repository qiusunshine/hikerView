package com.jeffmony.videocache.listener;

import com.jeffmony.videocache.model.VideoRange;

/**
 * @author jeffmony
 *
 * 专为Mp4视频的线程下载回调使用
 */
public interface IMp4CacheThreadListener {

    void onCacheFailed(VideoRange range, Exception e);

    //缓存文件的进度
    void onCacheProgress(VideoRange range, long cachedSize, float speed, float percent);

    //当前range 文件缓存完全
    void onCacheRangeCompleted(VideoRange range);

    void onCacheCompleted(VideoRange range);
}

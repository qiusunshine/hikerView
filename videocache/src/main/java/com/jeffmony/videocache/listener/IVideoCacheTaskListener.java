package com.jeffmony.videocache.listener;

import java.util.Map;

public interface IVideoCacheTaskListener {
    void onTaskStart();

    void onTaskProgress(float percent, long cachedSize, float speed);

    void onM3U8TaskProgress(float percent, long cachedSize, float speed, Map<Integer, Long> tsLengthMap);

    void onTaskFailed(Exception e);

    void onTaskCompleted(long totalSize);

    void onVideoSeekComplete();

}

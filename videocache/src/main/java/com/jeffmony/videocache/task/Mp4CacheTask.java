package com.jeffmony.videocache.task;

import com.jeffmony.videocache.listener.IMp4CacheThreadListener;
import com.jeffmony.videocache.model.VideoCacheInfo;
import com.jeffmony.videocache.model.VideoRange;
import com.jeffmony.videocache.utils.LogUtils;
import com.jeffmony.videocache.utils.VideoProxyThreadUtils;
import com.jeffmony.videocache.utils.VideoRangeUtils;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author jeffmony
 * <p>
 * mp4视频单线程优化专用类
 */
public class Mp4CacheTask extends VideoCacheTask {

    private static final String TAG = "Mp4CacheSingleTask";

    private Mp4VideoCacheThread mVideoCacheThread;
    private final Object mSegMapLock = new Object();
    private LinkedHashMap<Long, Long> mVideoSegMap;            //本地序列化的range结构
    private LinkedHashMap<Long, VideoRange> mVideoRangeMap;    //已经缓存的video range结构
    private VideoRange mRequestRange;                          //当前请求的video range
    private long mCachedSize;                                  //已经缓存的文件大小

    private String mVideoUrl;

    public Mp4CacheTask(VideoCacheInfo cacheInfo, Map<String, String> headers) {
        super(cacheInfo, headers);
        mTotalSize = cacheInfo.getTotalSize();
        mVideoSegMap = cacheInfo.getVideoSegMap();
        if (mVideoSegMap == null) {
            mVideoSegMap = new LinkedHashMap<>();
        }
        if (mVideoRangeMap == null) {
            mVideoRangeMap = new LinkedHashMap<>();
        }
        mVideoUrl = cacheInfo.getVideoUrl();
        initVideoSegInfo();
    }

    private void initVideoSegInfo() {
        if (mVideoSegMap.size() == 0) {
            //当前没有缓存,需要从头下载
            mRequestRange = new VideoRange(0, mTotalSize);
        } else {
            for (Map.Entry<Long, Long> entry : mVideoSegMap.entrySet()) {
                //因为mVideoSegMap是顺序存储的,所有这样的操作是可以的
                long start = entry.getKey();
                long end = entry.getValue();
                mVideoRangeMap.put(start, new VideoRange(start, end));
                /**
                 * mVideoRangeMap中的key是起始位置, value是存储的VideoRange结构
                 */
            }
        }
    }

    /**
     * pos
     * |--------------|          |-------------------|      |-------------------|
     * <p>
     * pos
     * |--------------|          |-------------------|      |-------------------|
     * <p>
     * pos
     * |--------------|          |-------------------|      |-------------------|
     * <p>
     * pos
     * |--------------|          |-------------------|      |-------------------|
     * <p>
     * pos
     * |--------------|          |-------------------|      |-------------------|
     * <p>
     * pos
     * |--------------|          |-------------------|      |-------------------|
     *
     * @param position
     * @return
     */
    public VideoRange getRequestRange(long position) {
        if (mVideoRangeMap.size() == 0) {
            return new VideoRange(0, mTotalSize);
        } else {
            long start = -1;
            long end = -1;
            for (Map.Entry<Long, VideoRange> entry : mVideoRangeMap.entrySet()) {
                VideoRange videoRange = entry.getValue();
                if (position < videoRange.getStart()) {
                    end = videoRange.getStart();
                } else if (position <= videoRange.getEnd()) {
                    start = videoRange.getEnd();
                } else {
                    //说明position 在当前的videoRange之后
                }
            }
            if (end == -1) {
                end = mTotalSize;
            }
            if (start == -1 || start == end) {
                start = position;
            }
            return new VideoRange(start, end);
        }
    }

    private IMp4CacheThreadListener mCacheThreadListener = new IMp4CacheThreadListener() {
        @Override
        public void onCacheFailed(VideoRange range, Exception e) {
            notifyOnTaskFailed(e);
        }

        @Override
        public void onCacheProgress(VideoRange range, long cachedSize, float speed, float percent) {
            notifyOnCacheProgress(cachedSize, speed, percent);
        }

        @Override
        public void onCacheRangeCompleted(VideoRange range) {
            notifyOnCacheRangeCompleted(range.getEnd());
        }

        @Override
        public void onCacheCompleted(VideoRange range) {

        }
    };

    @Override
    public void startCacheTask() {
        if (mCacheInfo.isCompleted()) {
            notifyOnTaskCompleted();
            return;
        }
        notifyOnTaskStart();
        LogUtils.i(TAG, "startCacheTask");
        VideoRange requestRange = getRequestRange(0L);
        startVideoCacheThread(requestRange);
    }

    @Override
    public synchronized void pauseCacheTask() {
        LogUtils.i(TAG, "pauseCacheTask");
        if (mVideoCacheThread != null && mVideoCacheThread.isRunning()) {
            mVideoCacheThread.pause();
            mVideoCacheThread = null;

            if (!mCacheInfo.isCompleted() && mRequestRange != null) {
                long tempRangeStart = mRequestRange.getStart();
                long tempRangeEnd = mCachedSize;
                mRequestRange = new VideoRange(tempRangeStart, tempRangeEnd);
                updateVideoRangeInfo();
            }
        }
    }

    @Override
    public void stopCacheTask() {
        LogUtils.i(TAG, "stopCacheTask");
        if (mVideoCacheThread != null) {
            mVideoCacheThread.pause();
            mVideoCacheThread = null;
        }
        if (!mCacheInfo.isCompleted() && mRequestRange != null) {
            long tempRangeStart = mRequestRange.getStart();
            long tempRangeEnd = mCachedSize;
            mRequestRange = new VideoRange(tempRangeStart, tempRangeEnd);
            updateVideoRangeInfo();
        }
    }

    @Override
    public void seekToCacheTaskFromClient(float percent) {
        //来自客户端的seek操作
    }

    @Override
    public void seekToCacheTaskFromServer(int segIndex) {
    }

    @Override
    public void seekToCacheTaskFromServer(long startPosition) {
        //来自服务端的seek操作
        boolean shouldSeekToCacheTask;
        if (mVideoCacheThread != null) {
            if (mVideoCacheThread.isRunning()) {
                shouldSeekToCacheTask = shouldSeekToCacheTask(startPosition);
            } else {
                shouldSeekToCacheTask = true;
            }
        } else {
            shouldSeekToCacheTask = true;
        }
        LogUtils.i(TAG, "seekToCacheTaskFromServer ====> shouldSeekToCacheTask=" + shouldSeekToCacheTask + ", startPosition=" + startPosition);
        if (shouldSeekToCacheTask) {
            pauseCacheTask();
            VideoRange requestRange = getRequestRange(startPosition);
            startVideoCacheThread(requestRange);
        }
    }

    /**
     * true   ====>  表示重新发起请求
     * false  ====>  表示没有必要重新发起请求
     *
     * @param startPosition
     * @return
     */
    private boolean shouldSeekToCacheTask(long startPosition) {

        //当前文件下载完成, 不需要执行range request请求
        if (mCacheInfo.isCompleted()) {
            return false;
        }
        if (mRequestRange != null) {
            boolean result = mRequestRange.getStart() <= startPosition && startPosition < mRequestRange.getEnd();
            if (result) {
                //当前拖动到的位置已经在request range中了, 没有必要重新发起请求了
                if (mCachedSize >= startPosition) {
                    return false;
                }
                return true;
            }
        }
        return true;
    }

    @Override
    public void resumeCacheTask() {
        if (mVideoCacheThread != null && mVideoCacheThread.isRunning()) {
            //当前mp4缓存线程正在运行中, 没有必要重新启动下载了
            return;
        }
        LogUtils.i(TAG, "resumeCacheTask");
        if (mCachedSize < mTotalSize) {
            VideoRange requestRange = getRequestRange(mCachedSize);
            startVideoCacheThread(requestRange);
        }
    }

    private void startVideoCacheThread(VideoRange requestRange) {
        mRequestRange = requestRange;
        mVideoCacheThread = new Mp4VideoCacheThread(mVideoUrl, mHeaders, requestRange, mTotalSize, mSaveDir.getAbsolutePath(), mCacheThreadListener);
        VideoProxyThreadUtils.submitRunnableTask(mVideoCacheThread);
    }

    private void notifyOnCacheProgress(long cachedSize, float speed, float percent) {
        mCachedSize = cachedSize;
        mCacheInfo.setCachedSize(cachedSize);
        mCacheInfo.setSpeed(speed);
        mCacheInfo.setPercent(percent);
        mListener.onTaskProgress(percent, mCachedSize, mSpeed);
    }

    private void notifyOnCacheRangeCompleted(long startPosition) {
        //这时候已经缓存好了一段分片,可以更新一下video range数据结构了
        updateVideoRangeInfo();
        if (mCacheInfo.isCompleted()) {
            notifyOnTaskCompleted();
        } else {
            if (startPosition == mTotalSize) {
                //说明已经缓存好,但是整视频中间还有一些洞,但是不影响,可以忽略
            } else {
                //开启下一段视频分片的缓存
                VideoRange requestRange = getRequestRange(startPosition);
                startVideoCacheThread(requestRange);
            }
        }

    }

    private synchronized void updateVideoRangeInfo() {
        if (mVideoRangeMap.size() > 0) {
            long finalStart = -1;
            long finalEnd = -1;

            long requestStart = mRequestRange.getStart();
            long requestEnd = mRequestRange.getEnd();
            for (Map.Entry<Long, VideoRange> entry : mVideoRangeMap.entrySet()) {
                VideoRange videoRange = entry.getValue();
                long startResult = VideoRangeUtils.determineVideoRangeByPosition(videoRange, requestStart);
                long endResult = VideoRangeUtils.determineVideoRangeByPosition(videoRange, requestEnd);

                if (finalStart == -1) {
                    if (startResult == 1) {
                        finalStart = requestStart;
                    } else if (startResult == 2) {
                        finalStart = videoRange.getStart();
                    } else {
                        //先别急着赋值,还要看下一个videoRange
                    }
                }
                if (finalEnd == -1) {
                    if (endResult == 1) {
                        finalEnd = requestEnd;
                    } else if (endResult == 2) {
                        finalEnd = videoRange.getEnd();
                    } else {
                        //先别急着赋值,还要看下一个videoRange
                    }
                }
            }
            if (finalStart == -1) {
                finalStart = requestStart;
            }
            if (finalEnd == -1) {
                finalEnd = requestEnd;
            }

            VideoRange finalVideoRange = new VideoRange(finalStart, finalEnd);
            LogUtils.i(TAG, "updateVideoRangeInfo--->finalVideoRange: " + finalVideoRange);

            LinkedHashMap<Long, VideoRange> tempVideoRangeMap = new LinkedHashMap<>();
            for (Map.Entry<Long, VideoRange> entry : mVideoRangeMap.entrySet()) {
                VideoRange videoRange = entry.getValue();
                if (VideoRangeUtils.containsVideoRange(finalVideoRange, videoRange)) {
                    tempVideoRangeMap.put(finalVideoRange.getStart(), finalVideoRange);
                } else if (VideoRangeUtils.compareVideoRange(finalVideoRange, videoRange) == 1) {
                    tempVideoRangeMap.put(finalVideoRange.getStart(), finalVideoRange);
                    tempVideoRangeMap.put(videoRange.getStart(), videoRange);
                } else if (VideoRangeUtils.compareVideoRange(finalVideoRange, videoRange) == 2) {
                    tempVideoRangeMap.put(videoRange.getStart(), videoRange);
                    tempVideoRangeMap.put(finalVideoRange.getStart(), finalVideoRange);
                }
            }
            mVideoRangeMap.clear();
            mVideoRangeMap.putAll(tempVideoRangeMap);
        } else {
            LogUtils.i(TAG, "updateVideoRangeInfo--->mRequestRange : " + mRequestRange);
            mVideoRangeMap.put(mRequestRange.getStart(), mRequestRange);
        }

        LinkedHashMap<Long, Long> tempSegMap = new LinkedHashMap<>();
        for (Map.Entry<Long, VideoRange> entry : mVideoRangeMap.entrySet()) {
            VideoRange videoRange = entry.getValue();
            LogUtils.i(TAG, "updateVideoRangeInfo--->Result videoRange : " + videoRange);
            tempSegMap.put(videoRange.getStart(), videoRange.getEnd());
        }
        synchronized (mSegMapLock) {
            mVideoSegMap.clear();
            mVideoSegMap.putAll(tempSegMap);
        }
        mCacheInfo.setVideoSegMap(mVideoSegMap);

        if (mVideoRangeMap.size() == 1) {
            VideoRange videoRange = mVideoRangeMap.get(0L);
            LogUtils.i(TAG, "updateVideoRangeInfo---> videoRange : " + videoRange);
            if (videoRange != null && videoRange.equals(new VideoRange(0, mTotalSize))) {
                LogUtils.i(TAG, "updateVideoRangeInfo--->Set completed");
                mCacheInfo.setIsCompleted(true);
            }
        }

        //子线程中执行
        saveVideoInfo();
    }

    @Override
    public long getMp4CachedPosition(long position) {
        if (mVideoCacheThread != null && mVideoCacheThread.isPositionContained(position)) {
            return mVideoCacheThread.getRangeEndPosition();
        }
        for (Map.Entry entry : mVideoRangeMap.entrySet()) {
            VideoRange range = (VideoRange) entry.getValue();
            if (range != null && range.contains(position)) {
                return range.getEnd();
            }
        }
        return 0L;
    }
}

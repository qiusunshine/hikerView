package com.jeffmony.videocache.task;

import android.text.TextUtils;

import com.jeffmony.videocache.common.VideoCacheException;
import com.jeffmony.videocache.m3u8.M3U8;
import com.jeffmony.videocache.m3u8.M3U8Seg;
import com.jeffmony.videocache.model.VideoCacheInfo;
import com.jeffmony.videocache.utils.HttpUtils;
import com.jeffmony.videocache.utils.LogUtils;
import com.jeffmony.videocache.utils.ProxyCacheUtils;
import com.jeffmony.videocache.utils.StorageUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class M3U8CacheTask extends VideoCacheTask {

    private static final String TAG = "M3U8CacheTask";

    private static final int THREAD_POOL_COUNT = 6;
    private static final int CONTINUOUS_SUCCESS_TS_THRESHOLD = 6;
    private volatile int mM3U8DownloadPoolCount;
    private volatile int mContinuousSuccessSegCount;   //连续请求分片成功的个数

    private int mCachedSegCount;
    private int mTotalSegCount;
    private Map<Integer, Long> mSegLengthMap;
    private List<M3U8Seg> mSegList;

    public M3U8CacheTask(VideoCacheInfo cacheInfo, Map<String, String> headers, M3U8 m3u8) {
        super(cacheInfo, headers);
        mSegList = m3u8.getSegList();
        mTotalSegCount = cacheInfo.getTotalTs();
        mCachedSegCount = cacheInfo.getCachedTs();
        mSegLengthMap = cacheInfo.getTsLengthMap();
        if (mSegLengthMap == null) {
            mSegLengthMap = new HashMap<>();
        }
        mHeaders.put("Connection", "close");
    }

    @Override
    public void startCacheTask() {
        if (isTaskRunning()) {
            return;
        }
        notifyOnTaskStart();
        initM3U8TsInfo();
        int seekIndex = mCachedSegCount > 1 && mCachedSegCount <= mTotalSegCount ? mCachedSegCount - 1 : mCachedSegCount;
        startRequestVideoRange(seekIndex);
    }

    private void initM3U8TsInfo() {
        long tempCachedSize = 0;
        int tempCachedTs = 0;
        for (int index = 0; index < mSegList.size(); index++) {
            M3U8Seg ts = mSegList.get(index);
            File tempTsFile = new File(mSaveDir, ts.getSegName());
            if (tempTsFile.exists() && tempTsFile.length() > 0) {
                ts.setFileSize(tempTsFile.length());
                mSegLengthMap.put(index, tempTsFile.length());
                tempCachedSize += tempTsFile.length();
                tempCachedTs++;
            } else {
                break;
            }
        }
        mCachedSegCount = tempCachedTs;
        mCachedSize = tempCachedSize;
        if (mCachedSegCount == mTotalSegCount) {
            mCacheInfo.setIsCompleted(true);
        }
    }

    @Override
    public void pauseCacheTask() {
        LogUtils.i(TAG, "pauseCacheTask");
        if (isTaskRunning()) {
            mTaskExecutor.shutdownNow();
        }
    }

    @Override
    public void stopCacheTask() {
        LogUtils.i(TAG, "stopCacheTask");
        if (isTaskRunning()) {
            mTaskExecutor.shutdownNow();
        }
    }

    @Override
    public void resumeCacheTask() {
        LogUtils.i(TAG, "resumeCacheTask");
        if (isTaskShutdown()) {
            initM3U8TsInfo();
            int seekIndex = mCachedSegCount > 1 && mCachedSegCount <= mTotalSegCount ? mCachedSegCount - 1 : mCachedSegCount;
            startRequestVideoRange(seekIndex);
        }
    }

    @Override
    public void seekToCacheTaskFromClient(float percent) {
    }

    @Override
    public void seekToCacheTaskFromServer(long startPosition) {
    }

    @Override
    public void seekToCacheTaskFromServer(int segIndex) {
        LogUtils.i(TAG, "seekToCacheTaskFromServer segIndex="+segIndex);
        pauseCacheTask();
        startRequestVideoRange(segIndex);
    }

    private void startRequestVideoRange(int curTs) {
        if (mCacheInfo.isCompleted()) {
            notifyOnTaskCompleted();
            return;
        }
        if (isTaskRunning()) {
            //已经存在的任务不需要重新创建了
            return;
        }
        mTaskExecutor = new ThreadPoolExecutor(THREAD_POOL_COUNT, THREAD_POOL_COUNT, 0L,
                TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(), Executors.defaultThreadFactory(),
                new ThreadPoolExecutor.DiscardOldestPolicy());
        for (int index = curTs; index < mTotalSegCount; index++) {
            final M3U8Seg seg = mSegList.get(index);
            mTaskExecutor.execute(() -> {
                try {
                    startDownloadSegTask(seg);
                } catch (Exception e) {
                    LogUtils.w(TAG, "M3U8 ts video download failed, exception=" + e);
                    notifyOnTaskFailed(e);
                }
            });
        }
    }

    private void startDownloadSegTask(M3U8Seg seg) throws Exception {
        LogUtils.i(TAG, "startDownloadSegTask index="+seg.getSegIndex()+", url="+seg.getUrl());
        if (seg.hasInitSegment()) {
            String initSegmentName = seg.getInitSegmentName();
            File initSegmentFile = new File(mSaveDir, initSegmentName);
            if (!initSegmentFile.exists()) {
                downloadSegFile(seg, initSegmentFile, seg.getInitSegmentUri());
            }
        }
        String segName = seg.getSegName();
        File segFile = new File(mSaveDir, segName);
        if (!segFile.exists()) {
            // ts is network resource, download ts file then rename it to local file.
            downloadSegFile(seg, segFile, seg.getUrl());
        }

        //确保当前文件下载完整
        if (segFile.exists() && segFile.length() == seg.getContentLength()) {
            //只有这样的情况下才能保证当前的ts文件真正被下载下来了
            mSegLengthMap.put(seg.getSegIndex(), segFile.length());
            seg.setName(segName);
            seg.setFileSize(segFile.length());
            //更新进度
            notifyCacheProgress();
        }
    }

    private void downloadSegFile(M3U8Seg seg, File segFile, String downloadUrl) throws Exception {
        HttpURLConnection connection = null;
        InputStream inputStream = null;
        try {
            connection = HttpUtils.getConnection(downloadUrl, mHeaders);
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpUtils.RESPONSE_200 || responseCode == HttpUtils.RESPONSE_206) {
                seg.setRetryCount(0);
                if (mContinuousSuccessSegCount > CONTINUOUS_SUCCESS_TS_THRESHOLD && mM3U8DownloadPoolCount < THREAD_POOL_COUNT) {
                    mM3U8DownloadPoolCount += 1;
                    mContinuousSuccessSegCount -= 1;
                    setThreadPoolArgument(mM3U8DownloadPoolCount, mM3U8DownloadPoolCount);
                }
                inputStream = connection.getInputStream();
                long contentLength = connection.getContentLength();
                saveSegFile(inputStream, segFile, contentLength, seg, downloadUrl);
            } else {
                mContinuousSuccessSegCount = 0;
                if (responseCode == HttpUtils.RESPONSE_503) {
                    if (mM3U8DownloadPoolCount > 1) {
                        mM3U8DownloadPoolCount -= 1;
                        setThreadPoolArgument(mM3U8DownloadPoolCount, mM3U8DownloadPoolCount);
                        downloadSegFile(seg, segFile, downloadUrl);
                    } else {
                        seg.setRetryCount(seg.getRetryCount() + 1);
                        if (seg.getRetryCount() < HttpUtils.MAX_RETRY_COUNT) {
                            downloadSegFile(seg, segFile, downloadUrl);
                        } else {
                            throw new VideoCacheException("retry download exceed the limit times, threadPool overload.");
                        }
                    }
                } else {
                    throw new VideoCacheException("download failed, responseCode=" + responseCode);
                }
            }
        } catch (Exception e) {
            LogUtils.w(TAG, "downloadFile failed, exception=" + e.getMessage());
            throw e;
        } finally {
            HttpUtils.closeConnection(connection);
            ProxyCacheUtils.close(inputStream);
        }
    }

    private void saveSegFile(InputStream inputStream, File file, long contentLength, M3U8Seg seg, String downloadUrl) throws Exception {
        FileOutputStream fos = null;
        long totalLength = 0;
        try {
            fos = new FileOutputStream(file);
            int len;
            byte[] buf = new byte[StorageUtils.DEFAULT_BUFFER_SIZE];
            while ((len = inputStream.read(buf)) != -1) {
                totalLength += len;
                fos.write(buf, 0, len);
            }
            if (contentLength > 0 && contentLength == totalLength) {
                seg.setContentLength(contentLength);
            } else {
                seg.setContentLength(totalLength);
            }
        } catch (IOException e) {
            if (file.exists() && ((contentLength > 0 && contentLength == file.length()) || (contentLength == -1 && totalLength == file.length()))) {
                //这时候说明file已经下载完成了
            } else {
                if ((e instanceof ProtocolException &&
                        !TextUtils.isEmpty(e.getMessage()) &&
                        e.getMessage().contains("unexpected end of stream")) &&
                        (contentLength > totalLength && totalLength == file.length())) {
                    if (file.length() == 0) {
                        seg.setRetryCount(seg.getRetryCount() + 1);
                        if (seg.getRetryCount() < HttpUtils.MAX_RETRY_COUNT) {
                            downloadSegFile(seg, file, downloadUrl);
                        } else {
                            LogUtils.w(TAG, file.getAbsolutePath() + ", length=" + file.length() + ", saveFile failed, exception=" + e);
                            if (file.exists()) {
                                file.delete();
                            }
                            throw e;
                        }
                    } else {
                        seg.setContentLength(totalLength);
                    }
                } else {
                    LogUtils.w(TAG, file.getAbsolutePath() + " saveFile failed, exception=" + e);
                    if (file.exists()) {
                        file.delete();
                    }
                    throw e;
                }
            }

        } finally {
            ProxyCacheUtils.close(inputStream);
            ProxyCacheUtils.close(fos);
        }
    }

    private void notifyCacheProgress() {
        updateM3U8TsInfo();
        if (mCachedSegCount > mTotalSegCount) {
            mCachedSegCount = mTotalSegCount;
        }
        mCacheInfo.setCachedTs(mCachedSegCount);
        mCacheInfo.setTsLengthMap(mSegLengthMap);
        mCacheInfo.setCachedSize(mCachedSize);
        float percent = mCachedSegCount * 1.0f * 100 / mTotalSegCount;

        if (!ProxyCacheUtils.isFloatEqual(percent, mPercent)) {
            long nowTime = System.currentTimeMillis();
            if (mCachedSize > mLastCachedSize && nowTime > mLastInvokeTime) {
                mSpeed = (mCachedSize - mLastCachedSize) * 1000 * 1.0f / (nowTime - mLastInvokeTime);
            }
            mListener.onM3U8TaskProgress(percent, mCachedSize, mSpeed, mSegLengthMap);
            mPercent = percent;
            mCacheInfo.setPercent(percent);
            mCacheInfo.setSpeed(mSpeed);
            mLastInvokeTime = nowTime;
            mLastCachedSize = mCachedSize;
            saveVideoInfo();
        }

        boolean isCompleted = true;
        for (M3U8Seg ts : mSegList) {
            File tsFile = new File(mSaveDir, ts.getSegName());
            if (!tsFile.exists()) {
                isCompleted = false;
                break;
            }
        }
        mCacheInfo.setIsCompleted(isCompleted);
        if (isCompleted) {
            mCacheInfo.setTotalSize(mCachedSize);
            mTotalSize = mCachedSize;
            notifyOnTaskCompleted();
            saveVideoInfo();
        }
    }

    private void updateM3U8TsInfo() {
        long tempCachedSize = 0;
        int tempCachedTs = 0;
        for (int index = 0; index < mSegList.size(); index++) {
            M3U8Seg ts = mSegList.get(index);
            File tempTsFile = new File(mSaveDir, ts.getSegName());
            if (tempTsFile.exists() && tempTsFile.length() > 0) {
                ts.setFileSize(tempTsFile.length());
                mSegLengthMap.put(index, tempTsFile.length());
                tempCachedSize += tempTsFile.length();
                tempCachedTs++;
            }
        }
        mCachedSegCount = tempCachedTs;
        mCachedSize = tempCachedSize;
    }
}

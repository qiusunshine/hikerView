package com.jeffmony.videocache.task;

import com.jeffmony.videocache.common.VideoCacheException;
import com.jeffmony.videocache.listener.IMp4CacheThreadListener;
import com.jeffmony.videocache.model.VideoRange;
import com.jeffmony.videocache.okhttp.OkHttpManager;
import com.jeffmony.videocache.utils.HttpUtils;
import com.jeffmony.videocache.utils.LogUtils;
import com.jeffmony.videocache.utils.ProxyCacheUtils;
import com.jeffmony.videocache.utils.StorageUtils;

import java.io.File;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.Map;

public class Mp4VideoCacheThread implements Runnable {

    private static final String TAG = "Mp4VideoCacheThread";

    private VideoRange mRequestRange;                          //当前请求的video range
    private Map<String, String> mHeaders;
    private IMp4CacheThreadListener mListener;
    private long mLastCachedSize;                              //上一次缓存大小
    private long mLastInvokeTime;                              //上次回调的时间戳
    private float mPercent = 0.0f;                             //缓存百分比
    private float mSpeed = 0.0f;                               //缓存速度
    private boolean mIsRunning = true;
    private File mSaveDir;
    private long mTotalSize;
    private String mVideoUrl;
    private String mMd5;

    public Mp4VideoCacheThread(String url, Map<String, String> headers, VideoRange requestRange, long totalSize, String filePath, IMp4CacheThreadListener listener) {
        mVideoUrl = url;
        mHeaders = headers;
        if (mHeaders == null) {
            mHeaders = new HashMap<>();
        }
        mRequestRange = requestRange;
        mTotalSize = totalSize;
        mListener = listener;
        mMd5 = ProxyCacheUtils.computeMD5(url);
        mSaveDir = new File(filePath);
        if (!mSaveDir.exists()) {
            mSaveDir.mkdir();
        }
    }

    public boolean isRunning() {
        return mIsRunning;
    }

    public void pause() {
        LogUtils.i(TAG, "Mp4VideoCacheThread ====> PAUSE");
        mIsRunning = false;
    }

    public boolean isPositionContained(long position) {
        return mRequestRange != null ? mRequestRange.contains(position) : false;
    }

    public long getRangeEndPosition() {
        return mRequestRange != null ? mRequestRange.getEnd() : 0L;
    }

    @Override
    public void run() {
        if (!mIsRunning) {
            return;
        }
        if (ProxyCacheUtils.getConfig().useOkHttp()) {
            downloadVideoByOkHttp();
        } else {
            downloadVideo();
        }
    }

    private void downloadVideoByOkHttp() {
        File videoFile;
        try {
            videoFile = new File(mSaveDir, mMd5 + StorageUtils.NON_M3U8_SUFFIX);
            if (!videoFile.exists()) {
                videoFile.createNewFile();
            }
        } catch (Exception e) {
            notifyOnCacheFailed(new VideoCacheException("Cannot create video file, exception="+e));
            return;
        }

        long requestStart = mRequestRange.getStart();
        long requestEnd = mRequestRange.getEnd();
        mHeaders.put("Range", "bytes=" + requestStart + "-" + requestEnd);

        InputStream inputStream = null;
        RandomAccessFile randomAccessFile = null;

        try {
            randomAccessFile = new RandomAccessFile(videoFile.getAbsolutePath(), "rw");
            randomAccessFile.seek(requestStart);
            long cachedSize = requestStart;
            LogUtils.i(TAG, "Start request : " + mRequestRange + ", CurrentCachedSize=" + cachedSize);
            inputStream = OkHttpManager.getInstance().getResponseBody(mVideoUrl, mHeaders, contentLength -> {
                LogUtils.i(TAG, "getResponseBody--->FetchContentLength: " + contentLength);
            });
            LogUtils.i(TAG, "Receive response");
            byte[] buffer = new byte[1024];
            int readLength;
            while(mIsRunning && (readLength = inputStream.read(buffer)) != -1) {
                if (cachedSize >= requestEnd) {
                    cachedSize = requestEnd;
                }
                if (cachedSize + readLength > requestEnd) {
                    long read = requestEnd - cachedSize;
                    randomAccessFile.write(buffer, 0, (int)read);
                    cachedSize = requestEnd;
                } else {
                    randomAccessFile.write(buffer, 0, readLength);
                    cachedSize += readLength;
                }

                notifyOnCacheProgress(cachedSize);

                if (cachedSize >= requestEnd) {
                    //缓存好了一段,开始缓存下一段
                    notifyOnCacheRangeCompleted();
                }
            }
            mIsRunning = false;
        } catch (Exception e) {
            notifyOnCacheFailed(e);
        } finally {
            mIsRunning = false;
            ProxyCacheUtils.close(inputStream);
            ProxyCacheUtils.close(randomAccessFile);
        }
    }

    private void downloadVideo() {
        File videoFile;
        try {
            videoFile = new File(mSaveDir, mMd5 + StorageUtils.NON_M3U8_SUFFIX);
            if (!videoFile.exists()) {
                videoFile.createNewFile();
            }
        } catch (Exception e) {
            notifyOnCacheFailed(new VideoCacheException("Cannot create video file, exception="+e));
            return;
        }

        long requestStart = mRequestRange.getStart();
        long requestEnd = mRequestRange.getEnd();
        mHeaders.put("Range", "bytes=" + requestStart + "-" + requestEnd);
        HttpURLConnection connection = null;
        InputStream inputStream = null;
        RandomAccessFile randomAccessFile = null;

        try {
            randomAccessFile = new RandomAccessFile(videoFile.getAbsolutePath(), "rw");
            randomAccessFile.seek(requestStart);
            long cachedSize = requestStart;
            LogUtils.i(TAG, "Start request : " + mRequestRange + ", CurrentCachedSize="+cachedSize);
            connection = HttpUtils.getConnection(mVideoUrl, mHeaders);
            inputStream = connection.getInputStream();
            LogUtils.i(TAG, "Receive response");

            byte[] buffer = new byte[StorageUtils.DEFAULT_BUFFER_SIZE];
            int readLength;
            while(mIsRunning && (readLength = inputStream.read(buffer)) != -1) {
                if (cachedSize >= requestEnd) {
                    cachedSize = requestEnd;
                }
                if (cachedSize + readLength > requestEnd) {
                    long read = requestEnd - cachedSize;
                    randomAccessFile.write(buffer, 0, (int)read);
                    cachedSize = requestEnd;
                } else {
                    randomAccessFile.write(buffer, 0, readLength);
                    cachedSize += readLength;
                }

                notifyOnCacheProgress(cachedSize);

                if (cachedSize >= requestEnd) {
                    //缓存好了一段,开始缓存下一段
                    notifyOnCacheRangeCompleted();
                }
            }
            mIsRunning = false;
        } catch (Exception e) {
            notifyOnCacheFailed(e);
        } finally {
            mIsRunning = false;
            ProxyCacheUtils.close(inputStream);
            ProxyCacheUtils.close(randomAccessFile);
            HttpUtils.closeConnection(connection);
        }
    }

    private void notifyOnCacheFailed(Exception e) {
        mListener.onCacheFailed(mRequestRange, e);
    }

    private void notifyOnCacheProgress(long cachedSize) {
        if (cachedSize == mTotalSize) {
            mListener.onCacheCompleted(mRequestRange);
            return;
        }
        float percent = cachedSize * 1.0f * 100 / mTotalSize;
        if (!ProxyCacheUtils.isFloatEqual(percent, mPercent)) {
            long nowTime = System.currentTimeMillis();
            if (cachedSize > mLastCachedSize && nowTime > mLastInvokeTime) {
                mSpeed = (cachedSize - mLastCachedSize) * 1000 * 1.0f / (nowTime - mLastInvokeTime);
            }
            mListener.onCacheProgress(mRequestRange, cachedSize, mSpeed, mPercent);
            mPercent = percent;
            mLastInvokeTime = nowTime;
            mLastCachedSize = cachedSize;
        }
    }

    private void notifyOnCacheRangeCompleted() {
        mListener.onCacheRangeCompleted(mRequestRange);
    }

}

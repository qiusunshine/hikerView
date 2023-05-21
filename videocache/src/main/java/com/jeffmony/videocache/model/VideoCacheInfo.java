package com.jeffmony.videocache.model;

import com.jeffmony.videocache.common.VideoCacheException;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

public class VideoCacheInfo implements Serializable {
    private static final long serialVersionUID = 3817171782413324662L;
    private String mVideoUrl;                      //视频的url
    private int mVideoType;                        //视频类型
    private long mCachedSize;                      //已经缓存的大小，M3U8文件忽略这个变量
    private long mTotalSize;                       //总大小
    private int mCachedTs;                         //已经缓存的ts个数
    private int mTotalTs;                          //总的ts个数
    private String mMd5;                           //videourl对应的md5
    private String mSavePath;                      //videocacheinfo存储的目录
    private int mLocalPort;                        //本地代理的端口号，每次可能不一样
    private boolean mIsCompleted;                  //文件是否缓存完
    private float mPercent;                        //缓存视频的百分比
    private float mSpeed;                          //缓存速度
    private Map<Integer, Long> mTsLengthMap;       //key表示ts的索引，value表示索引分片的content-length
    private LinkedHashMap<Long, Long> mVideoSegMap;//视频分片的保存结构

    private VideoCacheException error;

    /**
     * mVideoSegMap
     * [0, 2], [4,8], [12,20]
     * 一个整视频中有3小段已经下载好了
     * 一定要按照顺序来存储
     */

    public VideoCacheInfo(String url) {
        mVideoUrl = url;
    }

    public String getVideoUrl() {
        return mVideoUrl;
    }

    public void setVideoType(int type) {
        mVideoType = type;
    }

    public int getVideoType() {
        return mVideoType;
    }

    public void setCachedSize(long cachedSize) {
        mCachedSize = cachedSize;
    }

    public long getCachedSize() {
        return mCachedSize;
    }

    public void setTotalSize(long totalSize) {
        mTotalSize = totalSize;
    }

    public long getTotalSize() {
        return mTotalSize;
    }

    public void setCachedTs(int cachedTs) {
        mCachedTs = cachedTs;
    }

    public int getCachedTs() {
        return mCachedTs;
    }

    public void setTotalTs(int totalTs) {
        mTotalTs = totalTs;
    }

    public int getTotalTs() { return mTotalTs; }

    public void setMd5(String md5) { mMd5 = md5; }

    public String getMd5() { return mMd5; }

    public void setSavePath(String savePath) { mSavePath = savePath; }

    public String getSavePath() { return mSavePath; }

    public void setLocalPort(int port) { mLocalPort = port; }

    public int getLocalPort() { return mLocalPort; }

    public void setIsCompleted(boolean isCompleted) { mIsCompleted = isCompleted; }

    public boolean isCompleted() { return mIsCompleted; }

    public void setPercent(float percent) { mPercent = percent; }

    public float getPercent() { return mPercent; }

    public void setSpeed(float speed) { mSpeed = speed; }

    public float getSpeed() { return mSpeed; }

    public void setTsLengthMap(Map<Integer, Long> tsLengthMap) {
        mTsLengthMap = tsLengthMap;
    }

    public Map<Integer, Long> getTsLengthMap() {
        return mTsLengthMap;
    }

    public void setVideoSegMap(LinkedHashMap<Long, Long> videoSegMap) {
        mVideoSegMap = videoSegMap;
    }

    public LinkedHashMap<Long, Long> getVideoSegMap() {
        return mVideoSegMap;
    }

    public String toString() {
        return "VideoCacheInfo[" +
                "url=" + mVideoUrl + "," +
                "type=" + mVideoType + "," +
                "isCompleted=" + mIsCompleted + "," +
                "cachedSize=" + mCachedSize + "," +
                "totalSize=" + mTotalSize  + "," +
                "cachedTs=" + mCachedTs + "," +
                "totalTs=" + mTotalTs +
                "]";
    }

    public VideoCacheException getError() {
        return error;
    }

    public void setError(VideoCacheException error) {
        this.error = error;
    }
}

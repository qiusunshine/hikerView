package com.jeffmony.videocache.m3u8;

import android.net.Uri;
import android.text.TextUtils;

import com.jeffmony.videocache.utils.ProxyCacheUtils;

import java.io.File;
import java.util.Locale;
import java.util.Map;

/**
 * @author jeffmony
 *
 * M3U8文件中TS文件的结构
 */
public class M3U8Seg implements Comparable<M3U8Seg> {
    private String mParentUrl;             //分片的上级M3U8的url
    private String mUrl;                   //分片的网络url
    private String mName;                  //分片的文件名
    private float mDuration;               //分片的时长
    private int mSegIndex;                 //分片索引位置，起始索引为0
    private long mFileSize;                //分片文件大小
    private long mContentLength;           //分片文件的网络请求的content-length
    private boolean mHasDiscontinuity;     //当前分片文件前是否有Discontinuity
    private boolean mHasKey;               //分片文件是否加密
    private String mMethod;                //分片文件的加密方法
    private String mKeyUrl;                //分片文件的密钥地址
    private String mKeyIv;                 //密钥IV
    private int mRetryCount;               //重试请求次数
    private boolean mHasInitSegment;       //分片前是否有#EXT-X-MAP
    private String mInitSegmentUri;        //MAP的url
    private String mSegmentByteRange;      //MAP的range

    public void setParentUrl(String parentUrl) { mParentUrl = parentUrl; }

    public String getParentUrl() { return mParentUrl; }

    public String getUrl() {
        return mUrl;
    }

    public void setUrl(String url) { mUrl = url; }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        mName = name;
    }

    public float getDuration() {
        return mDuration;
    }

    public void setDuration(float duration) {
        mDuration = duration;
    }

    public int getSegIndex() {
        return mSegIndex;
    }

    public void setSegIndex(int segIndex) {
        mSegIndex = segIndex;
    }

    public long getFileSize() {
        return mFileSize;
    }

    public void setFileSize(long fileSize) {
        mFileSize = fileSize;
    }

    public long getContentLength() {
        return mContentLength;
    }

    public void setContentLength(long contentLength) {
        mContentLength = contentLength;
    }

    public boolean isHasDiscontinuity() {
        return mHasDiscontinuity;
    }

    public void setHasDiscontinuity(boolean hasDiscontinuity) {
        mHasDiscontinuity = hasDiscontinuity;
    }

    public boolean isHasKey() {
        return mHasKey;
    }

    public void setHasKey(boolean hasKey) {
        mHasKey = hasKey;
    }

    public String getMethod() {
        return mMethod;
    }

    public void setMethod(String method) {
        mMethod = method;
    }

    public String getKeyUrl() {
        return mKeyUrl;
    }

    public void setKeyUrl(String keyUrl) {
        mKeyUrl = keyUrl;
    }

    public String getKeyIv() {
        return mKeyIv;
    }

    public void setKeyIv(String keyIv) {
        mKeyIv = keyIv;
    }

    public String getSegName() {
        String suffixName = "";
        if (!TextUtils.isEmpty(mUrl)) {
            Uri uri = Uri.parse(mUrl);
            String fileName = uri.getLastPathSegment();
            if (!TextUtils.isEmpty(fileName)) {
                fileName = fileName.toLowerCase();
                suffixName = ProxyCacheUtils.getSuffixName(fileName);
            }
        }
        return mSegIndex + suffixName;
    }

    public void setRetryCount(int retryCount) { mRetryCount = retryCount; }

    public int getRetryCount() { return mRetryCount; }

    public void setInitSegmentInfo(String initSegmentUri, String segmentByteRange) {
        mHasInitSegment = true;
        mInitSegmentUri = initSegmentUri;
        mSegmentByteRange = segmentByteRange;
    }

    public boolean hasInitSegment() { return mHasInitSegment; }

    public String getInitSegmentUri() { return mInitSegmentUri; }

    public String getSegmentByteRange() { return mSegmentByteRange; }

    public String getInitSegmentName() {
        String suffixName = "";
        if (!TextUtils.isEmpty(mInitSegmentUri)) {
            Uri uri = Uri.parse(mInitSegmentUri);
            String fileName = uri.getLastPathSegment();
            if (!TextUtils.isEmpty(fileName)) {
                fileName = fileName.toLowerCase();
                suffixName = ProxyCacheUtils.getSuffixName(fileName);
            }
        }
        return ProxyCacheUtils.INIT_SEGMENT_PREFIX + mSegIndex + suffixName;
    }

    @Override
    public int compareTo(M3U8Seg m3u8Ts) {
        return mUrl.compareTo(m3u8Ts.getUrl());
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    public String getInitSegProxyUrl(String md5, Map<String, String> headers) {
        //三个字符串
        //1.parent url
        //2.init Seg的url
        //3.init Seg存储的位置
        //4.init Seg url对应的请求headers
        String proxyExtraInfo = mParentUrl + ProxyCacheUtils.SEG_PROXY_SPLIT_STR + mInitSegmentUri + ProxyCacheUtils.SEG_PROXY_SPLIT_STR +
                File.separator + md5 + File.separator + getInitSegmentName() + ProxyCacheUtils.SEG_PROXY_SPLIT_STR + ProxyCacheUtils.map2Str(headers);
        String proxyUrl = String.format(Locale.US, "http://%s:%d/%s", ProxyCacheUtils.LOCAL_PROXY_HOST,
                ProxyCacheUtils.getLocalPort(), ProxyCacheUtils.encodeUriWithBase64(proxyExtraInfo));
        return proxyUrl;
    }

    public String getSegProxyUrl(String md5, Map<String, String> headers) {
        //三个字符串
        //1.parent url
        //2.Seg的url
        //3.Seg存储的位置
        //4.Seg url对应的请求headers
        String proxyExtraInfo = mParentUrl + ProxyCacheUtils.SEG_PROXY_SPLIT_STR + mUrl + ProxyCacheUtils.SEG_PROXY_SPLIT_STR +
                File.separator + md5 + File.separator + getSegName() + ProxyCacheUtils.SEG_PROXY_SPLIT_STR + ProxyCacheUtils.map2Str(headers);
        String proxyUrl = String.format(Locale.US, "http://%s:%d/%s", ProxyCacheUtils.LOCAL_PROXY_HOST,
                ProxyCacheUtils.getLocalPort(), ProxyCacheUtils.encodeUriWithBase64(proxyExtraInfo));
        return proxyUrl;
    }

}

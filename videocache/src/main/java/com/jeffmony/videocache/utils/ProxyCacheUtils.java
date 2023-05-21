package com.jeffmony.videocache.utils;

import android.net.Uri;
import android.text.TextUtils;
import android.util.Base64;

import com.jeffmony.videocache.common.VideoCacheConfig;
import com.jeffmony.videocache.common.VideoMime;
import com.jeffmony.videocache.common.VideoParams;

import java.io.Closeable;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * @author jeffmony
 * <p>
 * 本地代理相关的工具类
 */
public class ProxyCacheUtils {

    private static final String TAG = "ProxyCacheUtils";

    public static final String LOCAL_PROXY_HOST = "127.0.0.1";
    public static final String LOCAL_PROXY_URL = "http://" + LOCAL_PROXY_HOST;
    public static final String SEG_PROXY_SPLIT_STR = "&jeffmony_seg&";           //M3U8 分片文件分隔符
    public static final String VIDEO_PROXY_SPLIT_STR = "&jeffmony_video&";       //视频分隔符
    public static final String HEADER_SPLIT_STR = "&jeffmony_header&";           //请求头部分隔符
    public static final String UNKNOWN = "unknown";
    public static final String M3U8 = "m3u8";
    public static final String NON_M3U8 = "non_m3u8";
    public static final String INIT_SEGMENT_PREFIX = "init_seg_";

    private static VideoCacheConfig sConfig;
    private static int sLocalPort = 0;
    private static long mSocketTime;     //socket运行的时间戳

    public static void setVideoCacheConfig(VideoCacheConfig config) {
        sConfig = config;
    }

    public static VideoCacheConfig getConfig() {
        return sConfig;
    }

    public static void setLocalPort(int port) {
        sLocalPort = port;
    }

    public static int getLocalPort() {
        return sLocalPort;
    }

    public static void setSocketTime(long time) {
        mSocketTime = time;
    }

    public static long getSocketTime() {
        return mSocketTime;
    }

    public static void close(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Exception e) {
                LogUtils.w(TAG, "ProxyCacheUtils close " + closeable + " failed, exception = " + e);
            }
        }
    }

    private static String bytesToHexString(byte[] bytes) {
        StringBuffer sb = new StringBuffer();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public static String computeMD5(String string) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            byte[] digestBytes = messageDigest.digest(string.getBytes());
            return bytesToHexString(digestBytes);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    //当前mimetype是否是M3U8类型
    public static boolean isM3U8Mimetype(String mimeType) {
        return mimeType.contains(VideoMime.MIME_TYPE_M3U8_1) ||
                mimeType.contains(VideoMime.MIME_TYPE_M3U8_2) ||
                mimeType.contains(VideoMime.MIME_TYPE_M3U8_3) ||
                mimeType.contains(VideoMime.MIME_TYPE_M3U8_4) ||
                mimeType.contains(VideoMime.MIME_TYPE_M3U8_5);
    }

    private static boolean isVideoMimeType(String mimeType) {
        if (mimeType.startsWith("video/")) {
            return true;
        }
        return false;
    }

    private static boolean isAudioMimeType(String mimeType) {
        if (mimeType.startsWith("audio/")) {
            return true;
        }
        return false;
    }

    public static String encodeUriWithBase64(String str) {
        try {
            return Base64.encodeToString(str.getBytes("utf-8"), Base64.NO_WRAP | Base64.NO_PADDING);
        } catch (Exception e) {
            return str;
        }
    }

    public static String decodeUriWithBase64(String str) {
        return new String(Base64.decode(str, Base64.NO_WRAP | Base64.NO_PADDING));
    }

    public static String map2Str(Map<String, String> headers) {
        if (headers == null || headers.size() == 0) {
            return UNKNOWN;
        }
        StringBuilder headerStr = new StringBuilder();
        for (Map.Entry item : headers.entrySet()) {
            String key = (String) item.getKey();
            String value = (String) item.getValue();
            headerStr.append(key + HEADER_SPLIT_STR + value);
            headerStr.append("\n");
        }
        return headerStr.toString();
    }

    public static Map<String, String> str2Map(String headerStr) {
        Map<String, String> headers = new HashMap<>();
        if (!TextUtils.isEmpty(headerStr) && !TextUtils.equals(headerStr, UNKNOWN)) {
            String[] headerLines = headerStr.split("\n");
            for (String headerItems : headerLines) {
                String[] headerArr = headerItems.split(HEADER_SPLIT_STR);
                if (headerArr.length >= 2) {
                    headers.put(headerArr[0], headerArr[1]);
                }
            }
        }
        return headers;
    }

    public static boolean isM3U8(String videoUrl, Map<String, Object> cacheParams) {
        String videoInfo = getVideoTypeInfo(videoUrl, cacheParams);
        if (TextUtils.equals(M3U8, videoInfo)) {
            return true;
        }
        return false;
    }

    /**
     * 从proxyUrl中解析出对应的port
     * 输入: http://127.0.0.1:43435/XXXXX&v5core&XXXXXX
     * 输出 43435
     *
     * @param proxyUrl
     * @return
     */
    public static int getPortFromProxyUrl(String proxyUrl) {
        if (!proxyUrl.contains(LOCAL_PROXY_URL)) {
            return 0;
        }
        try {
            proxyUrl = proxyUrl.substring(LOCAL_PROXY_URL.length() + 1);
        } catch (Exception e) {
            return 0;
        }
        int index = proxyUrl.indexOf("/");
        if (index == -1) {
            return 0;
        }
        int port;
        try {
            port = Integer.parseInt(proxyUrl.substring(0, index));
            return port;
        } catch (Exception e) {
            return 0;
        }
    }

    public static String getProxyUrl(String videoUrl, Map<String, String> headers, Map<String, Object> cacheParams) {
        String videoInfo = getVideoTypeInfo(videoUrl, cacheParams);
        String headerStr = map2Str(headers);
        String proxyExtraInfo = videoUrl + VIDEO_PROXY_SPLIT_STR + videoInfo + VIDEO_PROXY_SPLIT_STR + headerStr;
        //http://127.0.0.1:port/base64-parameter
        String proxyUrl = String.format(Locale.US, "http://%s:%d/%s", LOCAL_PROXY_HOST, sLocalPort, encodeUriWithBase64(proxyExtraInfo));
        return proxyUrl + (M3U8.equals(videoInfo) ? "#.m3u8" : "");
    }

    private static String getVideoTypeInfo(String videoUrl, Map<String, Object> cacheParams) {
        String contentType = VideoParamsUtils.getStringValue(cacheParams, VideoParams.CONTENT_TYPE);
        String videoInfo;
        if (!TextUtils.equals(VideoParams.UNKNOWN, contentType)) {
            if (isM3U8Mimetype(contentType)) {
                videoInfo = M3U8;      //已知是M3U8类型
            } else if (isVideoMimeType(contentType) || isAudioMimeType(contentType)) {
                videoInfo = NON_M3U8;  // 已知是非M3U8类型
            } else {
                videoInfo = getVideoTypeInfo(videoUrl);
            }
        } else {
            videoInfo = getVideoTypeInfo(videoUrl);
        }
        return videoInfo;
    }

    private static String getVideoTypeInfo(String videoUrl) {
        String videoInfo;
        Uri videoUri = Uri.parse(videoUrl);
        String fileName = videoUri.getLastPathSegment();
        if (!TextUtils.isEmpty(fileName)) {
            fileName = fileName.toLowerCase();
            if (fileName.endsWith(StorageUtils.M3U8_SUFFIX)) {
                videoInfo = M3U8;
            } else {
                videoInfo = NON_M3U8;
            }
        } else if (videoUrl.contains(M3U8)) {
            videoInfo = M3U8;
        } else {
            videoInfo = UNKNOWN;
        }
        return videoInfo;
    }

    public static boolean isFloatEqual(float f1, float f2) {
        if (Math.abs(f1 - f2) < 0.1f) {
            return true;
        }
        return false;
    }

    public static String getSuffixName(String name) {
        if (TextUtils.isEmpty(name)) {
            return "";
        }
        int dotIndex = name.lastIndexOf('.');
        return (dotIndex >= 0 && dotIndex < name.length()) ? name.substring(dotIndex) : "";
    }

}

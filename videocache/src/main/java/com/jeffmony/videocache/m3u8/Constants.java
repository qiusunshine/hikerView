package com.jeffmony.videocache.m3u8;

import java.util.regex.Pattern;

public class Constants {

    // base hls tag:
    public static final String PLAYLIST_HEADER = "#EXTM3U";    // must
    public static final String TAG_PREFIX = "#EXT";            // must
    public static final String TAG_VERSION = "#EXT-X-VERSION"; // must
    public static final String TAG_MEDIA_SEQUENCE = "#EXT-X-MEDIA-SEQUENCE"; // must
    public static final String TAG_TARGET_DURATION = "#EXT-X-TARGETDURATION";                               // must
    public static final String TAG_MEDIA_DURATION = "#EXTINF"; // must
    public static final String TAG_DISCONTINUITY = "#EXT-X-DISCONTINUITY"; // Optional
    public static final String TAG_ENDLIST = "#EXT-X-ENDLIST"; // It is not live if hls has '#EXT-X-ENDLIST' tag; Or it
    // is.
    public static final String TAG_KEY = "#EXT-X-KEY"; // Optional
    public static final String TAG_INIT_SEGMENT = "#EXT-X-MAP";

    // extra hls tag:

    // #EXT-X-PLAYLIST-TYPE:VOD       is not live
    // #EXT-X-PLAYLIST-TYPE:EVENT   is live, we also can try '#EXT-X-ENDLIST'
    public static final String TAG_PLAYLIST_TYPE = "#EXT-X-PLAYLIST-TYPE";
    public static final String TAG_STREAM_INF = "#EXT-X-STREAM-INF"; // Multiple m3u8 stream, we usually fetch the first.
    public static final String TAG_ALLOW_CACHE = "EXT-X-ALLOW-CACHE"; // YES : not live; NO: live

    public static final Pattern REGEX_TARGET_DURATION = Pattern.compile(TAG_TARGET_DURATION + ":(\\d+)\\b");
    public static final Pattern REGEX_MEDIA_DURATION = Pattern.compile(TAG_MEDIA_DURATION + ":([\\d\\.]+)\\b");
    public static final Pattern REGEX_VERSION = Pattern.compile(TAG_VERSION + ":(\\d+)\\b");
    public static final Pattern REGEX_MEDIA_SEQUENCE = Pattern.compile(TAG_MEDIA_SEQUENCE + ":(\\d+)\\b");
    public static final Pattern REGEX_BANDWIDTH = Pattern.compile("BANDWIDTH=(\\d+)\\b");
    public static final Pattern REGEX_RESOLUTION = Pattern.compile("RESOLUTION=(\\d+x\\d+)");

    public static final String METHOD_NONE = "NONE";
    public static final String METHOD_AES_128 = "AES-128";
    public static final String METHOD_SAMPLE_AES = "SAMPLE-AES";
    // Replaced by METHOD_SAMPLE_AES_CTR. Keep for backward compatibility.
    public static final String METHOD_SAMPLE_AES_CENC = "SAMPLE-AES-CENC";
    public static final String METHOD_SAMPLE_AES_CTR = "SAMPLE-AES-CTR";
    public static final Pattern REGEX_METHOD = Pattern.compile("METHOD=(" + METHOD_NONE + "|" + METHOD_AES_128 + "|" +
            METHOD_SAMPLE_AES + "|" + METHOD_SAMPLE_AES_CENC + "|" + METHOD_SAMPLE_AES_CTR + ")" + "\\s*(,|$)");
    public static final Pattern REGEX_KEYFORMAT = Pattern.compile("KEYFORMAT=\"(.+?)\"");
    public static final Pattern REGEX_URI = Pattern.compile("URI=\"(.+?)\"");
    public static final Pattern REGEX_IV = Pattern.compile("IV=([^,.*]+)");
    public static final String KEYFORMAT_IDENTITY = "identity";
    public static final Pattern REGEX_ATTR_BYTERANGE = Pattern.compile("BYTERANGE=\"(\\d+(?:@\\d+)?)\\b\"");
}

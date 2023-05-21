package com.jeffmony.videocache.m3u8;

import android.text.TextUtils;

import com.jeffmony.videocache.common.VideoCacheException;
import com.jeffmony.videocache.utils.HttpUtils;
import com.jeffmony.videocache.utils.LogUtils;
import com.jeffmony.videocache.utils.ProxyCacheUtils;
import com.jeffmony.videocache.utils.UrlUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author jeffmony
 *
 * M3U8的通用处理类
 */
public class M3U8Utils {

    private static final String TAG = "M3U8Utils";

    private static int sOldPort = 0;

    /**
     * 根据url将M3U8信息解析出来
     * @param videoUrl
     * @param headers
     * @return
     * @throws IOException
     */
    public static M3U8 parseNetworkM3U8Info(String parentUrl, String videoUrl, Map<String, String> headers, int retryCount) throws IOException {
        InputStreamReader inputStreamReader = null;
        BufferedReader bufferedReader = null;
        try {
            HttpURLConnection connection = HttpUtils.getConnection(videoUrl, headers);
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpUtils.RESPONSE_503 && retryCount < HttpUtils.MAX_RETRY_COUNT) {
                return parseNetworkM3U8Info(parentUrl, videoUrl, headers, retryCount + 1);
            }
            bufferedReader = new BufferedReader(new InputStreamReader(connection.getInputStream()));

            M3U8 m3u8 = new M3U8(videoUrl);
            int targetDuration = 0;
            int version = 0;
            int sequence = 0;
            boolean hasDiscontinuity = false;
            boolean hasEndList = false;
            boolean hasMasterList = false;
            boolean hasKey = false;
            boolean hasInitSegment = false;
            String method = null;
            String keyIv = null;
            String keyUrl = null;
            String initSegmentUri = null;
            String segmentByteRange = null;
            float segDuration = 0;
            int segIndex = 0;

            String line;
            while ((line = bufferedReader.readLine()) != null) {
                line = line.trim();
                if (TextUtils.isEmpty(line)) {
                    continue;
                }
                if (line.startsWith(Constants.TAG_PREFIX)) {
                    if (line.startsWith(Constants.TAG_MEDIA_DURATION)) {
                        String ret = parseStringAttr(line, Constants.REGEX_MEDIA_DURATION);
                        if (!TextUtils.isEmpty(ret)) {
                            segDuration = Float.parseFloat(ret);
                        }
                    } else if (line.startsWith(Constants.TAG_TARGET_DURATION)) {
                        String ret = parseStringAttr(line, Constants.REGEX_TARGET_DURATION);
                        if (!TextUtils.isEmpty(ret)) {
                            targetDuration = Integer.parseInt(ret);
                        }
                    } else if (line.startsWith(Constants.TAG_VERSION)) {
                        String ret = parseStringAttr(line, Constants.REGEX_VERSION);
                        if (!TextUtils.isEmpty(ret)) {
                            version = Integer.parseInt(ret);
                        }
                    } else if (line.startsWith(Constants.TAG_MEDIA_SEQUENCE)) {
                        String ret = parseStringAttr(line, Constants.REGEX_MEDIA_SEQUENCE);
                        if (!TextUtils.isEmpty(ret)) {
                            sequence = Integer.parseInt(ret);
                        }
                    } else if (line.startsWith(Constants.TAG_STREAM_INF)) {
                        hasMasterList = true;
                    } else if (line.startsWith(Constants.TAG_DISCONTINUITY)) {
                        hasDiscontinuity = true;
                    } else if (line.startsWith(Constants.TAG_ENDLIST)) {
                        hasEndList = true;
                    } else if (line.startsWith(Constants.TAG_KEY)) {
                        hasKey = true;
                        method = parseOptionalStringAttr(line, Constants.REGEX_METHOD);
                        String keyFormat = parseOptionalStringAttr(line, Constants.REGEX_KEYFORMAT);
                        if (!Constants.METHOD_NONE.equals(method)) {
                            keyIv = parseOptionalStringAttr(line, Constants.REGEX_IV);
                            if (Constants.KEYFORMAT_IDENTITY.equals(keyFormat) || keyFormat == null) {
                                if (Constants.METHOD_AES_128.equals(method)) {
                                    // The segment is fully encrypted using an identity key.
                                    String tempKeyUri = parseStringAttr(line, Constants.REGEX_URI);
                                    if (tempKeyUri != null) {
                                        keyUrl = UrlUtils.getM3U8MasterUrl(videoUrl, tempKeyUri);
                                    }
                                } else {
                                    // Do nothing. Samples are encrypted using an identity key,
                                    // but this is not supported. Hopefully, a traditional DRM
                                    // alternative is also provided.
                                }
                            } else {
                                // Do nothing.
                            }
                        }
                    } else if (line.startsWith(Constants.TAG_INIT_SEGMENT)) {
                        String tempInitSegmentUri = parseStringAttr(line, Constants.REGEX_URI);
                        if (!TextUtils.isEmpty(tempInitSegmentUri)) {
                            hasInitSegment = true;
                            initSegmentUri = UrlUtils.getM3U8MasterUrl(videoUrl, tempInitSegmentUri);
                            segmentByteRange = parseOptionalStringAttr(line, Constants.REGEX_ATTR_BYTERANGE);
                        }
                    }
                    continue;
                }

                // It has '#EXT-X-STREAM-INF' tag;
                if (hasMasterList) {
                    String tempUrl = UrlUtils.getM3U8MasterUrl(videoUrl, line);
                    return parseNetworkM3U8Info(parentUrl, tempUrl, headers, retryCount);
                }

                if (Math.abs(segDuration) < 0.001f) {
                    continue;
                }

                M3U8Seg seg = new M3U8Seg();
                seg.setParentUrl(parentUrl);
                String tempUrl = UrlUtils.getM3U8MasterUrl(videoUrl, line);
                seg.setUrl(tempUrl);
                seg.setSegIndex(segIndex);
                seg.setDuration(segDuration);
                seg.setHasDiscontinuity(hasDiscontinuity);
                seg.setHasKey(hasKey);
                if (hasKey) {
                    seg.setMethod(method);
                    seg.setKeyIv(keyIv);
                    seg.setKeyUrl(keyUrl);
                }
                if (hasInitSegment) {
                    seg.setInitSegmentInfo(initSegmentUri, segmentByteRange);
                }
                m3u8.addSeg(seg);
                segIndex++;
                segDuration = 0;
                hasDiscontinuity = false;
                hasKey = false;
                hasInitSegment = false;
                method = null;
                keyUrl = null;
                keyIv = null;
                initSegmentUri = null;
                segmentByteRange = null;
            }

            m3u8.setTargetDuration(targetDuration);
            m3u8.setVersion(version);
            m3u8.setSequence(sequence);
            m3u8.setIsLive(!hasEndList);
            return m3u8;
        } catch (IOException e) {
            throw e;
        } finally {
            ProxyCacheUtils.close(inputStreamReader);
            ProxyCacheUtils.close(bufferedReader);
        }
    }

    public static M3U8 parseLocalM3U8Info(File localM3U8File, String videoUrl) throws Exception {
        if (!localM3U8File.exists()) {
            throw new VideoCacheException("Local M3U8 File not found");
        }
        InputStreamReader inputStreamReader = null;
        BufferedReader bufferedReader = null;
        try {
            inputStreamReader = new InputStreamReader(new FileInputStream(localM3U8File));
            bufferedReader = new BufferedReader(inputStreamReader);
            M3U8 m3u8 = new M3U8(videoUrl);
            int targetDuration = 0;
            int version = 0;
            int sequence = 0;
            boolean hasDiscontinuity = false;
            boolean hasEndList = false;
            boolean hasKey = false;
            boolean hasInitSegment = false;
            String method = null;
            String keyIv = null;
            String keyUrl = null;
            String initSegmentUri = null;
            String segmentByteRange = null;
            float segDuration = 0;
            int segIndex = 0;

            String line;
            while ((line = bufferedReader.readLine()) != null) {
                line = line.trim();
                if (TextUtils.isEmpty(line)) {
                    continue;
                }
                if (line.startsWith(Constants.TAG_PREFIX)) {
                    if (line.startsWith(Constants.TAG_MEDIA_DURATION)) {
                        String ret = parseStringAttr(line, Constants.REGEX_MEDIA_DURATION);
                        if (!TextUtils.isEmpty(ret)) {
                            segDuration = Float.parseFloat(ret);
                        }
                    } else if (line.startsWith(Constants.TAG_TARGET_DURATION)) {
                        String ret = parseStringAttr(line, Constants.REGEX_TARGET_DURATION);
                        if (!TextUtils.isEmpty(ret)) {
                            targetDuration = Integer.parseInt(ret);
                        }
                    } else if (line.startsWith(Constants.TAG_VERSION)) {
                        String ret = parseStringAttr(line, Constants.REGEX_VERSION);
                        if (!TextUtils.isEmpty(ret)) {
                            version = Integer.parseInt(ret);
                        }
                    } else if (line.startsWith(Constants.TAG_MEDIA_SEQUENCE)) {
                        String ret = parseStringAttr(line, Constants.REGEX_MEDIA_SEQUENCE);
                        if (!TextUtils.isEmpty(ret)) {
                            sequence = Integer.parseInt(ret);
                        }
                    } else if (line.startsWith(Constants.TAG_DISCONTINUITY)) {
                        hasDiscontinuity = true;
                    } else if (line.startsWith(Constants.TAG_ENDLIST)) {
                        hasEndList = true;
                    } else if (line.startsWith(Constants.TAG_KEY)) {
                        hasKey = true;
                        method = parseOptionalStringAttr(line, Constants.REGEX_METHOD);
                        String keyFormat = parseOptionalStringAttr(line, Constants.REGEX_KEYFORMAT);
                        if (!Constants.METHOD_NONE.equals(method)) {
                            keyIv = parseOptionalStringAttr(line, Constants.REGEX_IV);
                            if (Constants.KEYFORMAT_IDENTITY.equals(keyFormat) || keyFormat == null) {
                                if (Constants.METHOD_AES_128.equals(method)) {
                                    // The segment is fully encrypted using an identity key.
                                    String tempKeyUri = parseStringAttr(line, Constants.REGEX_URI);
                                    if (tempKeyUri != null) {
                                        keyUrl = UrlUtils.getM3U8MasterUrl(videoUrl, tempKeyUri);
                                    }
                                } else {
                                    // Do nothing. Samples are encrypted using an identity key,
                                    // but this is not supported. Hopefully, a traditional DRM
                                    // alternative is also provided.
                                }
                            } else {
                                // Do nothing.
                            }
                        }
                    } else if (line.startsWith(Constants.TAG_INIT_SEGMENT)) {
                        initSegmentUri = parseStringAttr(line, Constants.REGEX_URI);
                        if (!TextUtils.isEmpty(initSegmentUri)) {
                            hasInitSegment = true;
                            segmentByteRange = parseOptionalStringAttr(line, Constants.REGEX_ATTR_BYTERANGE);
                        }
                    }
                    continue;
                }

                if (Math.abs(segDuration) < 0.001f) {
                    continue;
                }

                M3U8Seg seg = new M3U8Seg();
                String tempUrl = UrlUtils.getM3U8MasterUrl(videoUrl, line);
                seg.setUrl(tempUrl);
                seg.setSegIndex(segIndex);
                seg.setDuration(segDuration);
                seg.setHasDiscontinuity(hasDiscontinuity);
                seg.setHasKey(hasKey);
                if (hasKey) {
                    seg.setMethod(method);
                    seg.setKeyIv(keyIv);
                    seg.setKeyUrl(keyUrl);
                }
                if (hasInitSegment) {
                    seg.setInitSegmentInfo(initSegmentUri, segmentByteRange);
                }
                m3u8.addSeg(seg);
                segIndex++;
                segDuration = 0;
                hasDiscontinuity = false;
                hasKey = false;
                hasInitSegment = false;
                method = null;
                keyUrl = null;
                keyIv = null;
                initSegmentUri = null;
                segmentByteRange = null;
            }

            m3u8.setTargetDuration(targetDuration);
            m3u8.setVersion(version);
            m3u8.setSequence(sequence);
            m3u8.setIsLive(!hasEndList);
            return m3u8;
        } catch (Exception e) {
            throw e;
        } finally {
            ProxyCacheUtils.close(inputStreamReader);
            ProxyCacheUtils.close(bufferedReader);
        }
    }

    public static String parseStringAttr(String line, Pattern pattern) {
        if (pattern == null)
            return null;
        Matcher matcher = pattern.matcher(line);
        if (matcher.find() && matcher.groupCount() == 1) {
            return matcher.group(1);
        }
        return null;
    }

    private static String parseOptionalStringAttr(String line, Pattern pattern) {
        if (pattern == null)
            return null;
        Matcher matcher = pattern.matcher(line);
        return matcher.find() ? matcher.group(1) : null;
    }

    /**
     * 将m3u8结构保存到本地
     * @param m3u8File
     * @param m3u8
     * @throws Exception
     */
    public static void createLocalM3U8File(File m3u8File, M3U8 m3u8) throws Exception{
        if (m3u8File.exists()) {
            //如果这个文件存在，说明之前存储过这个文件，不用重复存储了。
            return;
        }
        BufferedWriter bfw = null;
        try {
            bfw = new BufferedWriter(new FileWriter(m3u8File, false));
            bfw.write(Constants.PLAYLIST_HEADER + "\n");
            bfw.write(Constants.TAG_VERSION + ":" + m3u8.getVersion() + "\n");
            bfw.write(Constants.TAG_MEDIA_SEQUENCE + ":" + m3u8.getSequence() + "\n");
            bfw.write(Constants.TAG_TARGET_DURATION + ":" + m3u8.getTargetDuration() + "\n");
            for (M3U8Seg m3u8Ts : m3u8.getSegList()) {
                if (m3u8Ts.hasInitSegment()) {
                    String initSegmentInfo;
                    if (m3u8Ts.getSegmentByteRange() != null) {
                        initSegmentInfo = "URI=\"" + m3u8Ts.getInitSegmentUri() + "\"" + ",BYTERANGE=\"" + m3u8Ts.getSegmentByteRange() + "\"";
                    } else {
                        initSegmentInfo = "URI=\"" + m3u8Ts.getInitSegmentUri()  + "\"";
                    }
                    bfw.write(Constants.TAG_INIT_SEGMENT + ":" + initSegmentInfo + "\n");
                }
                if (m3u8Ts.isHasKey() && !TextUtils.isEmpty(m3u8Ts.getMethod())) {
                    String key = "METHOD=" + m3u8Ts.getMethod();
                    if (!TextUtils.isEmpty(m3u8Ts.getKeyUrl())) {
                        key += ",URI=\"" + m3u8Ts.getKeyUrl() + "\"";
                        if (!TextUtils.isEmpty(m3u8Ts.getKeyIv())) {
                            key += ",IV=" + m3u8Ts.getKeyIv();
                        }
                    }
                    bfw.write(Constants.TAG_KEY + ":" + key + "\n");
                }
                if (m3u8Ts.isHasDiscontinuity()) {
                    bfw.write(Constants.TAG_DISCONTINUITY + "\n");
                }
                bfw.write(Constants.TAG_MEDIA_DURATION + ":" + m3u8Ts.getDuration() + ",\n");
                bfw.write(m3u8Ts.getUrl());
                bfw.newLine();
            }
            bfw.write(Constants.TAG_ENDLIST);
            bfw.flush();
        } catch (Exception e){
            LogUtils.w(TAG, "createLocalM3U8File failed exception = " + e.getMessage());
            if (m3u8File.exists()) {
                m3u8File.delete();
            }
            throw e;
        } finally {
            ProxyCacheUtils.close(bfw);
        }
    }

    /**
     * 创建本地代理的M3U8索引文件
     * @param m3u8File
     * @param m3u8
     * @param md5  这是videourl的MD5值
     * @param headers
     * @throws Exception
     */
    public static void createProxyM3U8File(File m3u8File, M3U8 m3u8, String md5, Map<String, String> headers) throws Exception {
        BufferedWriter bfw = new BufferedWriter(new FileWriter(m3u8File, false));
        bfw.write(Constants.PLAYLIST_HEADER + "\n");
        bfw.write(Constants.TAG_VERSION + ":" + m3u8.getVersion() + "\n");
        bfw.write(Constants.TAG_MEDIA_SEQUENCE + ":" + m3u8.getSequence() + "\n");
        bfw.write(Constants.TAG_TARGET_DURATION + ":" + m3u8.getTargetDuration() + "\n");

        for (M3U8Seg m3u8Ts : m3u8.getSegList()) {
            if (m3u8Ts.hasInitSegment()) {
                String initSegmentInfo = "URI=\"" + m3u8Ts.getInitSegProxyUrl(md5, headers) + "\"";
                if (m3u8Ts.getSegmentByteRange() != null) {
                    initSegmentInfo += ",BYTERANGE=\"" + m3u8Ts.getSegmentByteRange() +"\"";
                }
                bfw.write(Constants.TAG_INIT_SEGMENT + ":" + initSegmentInfo + "\n");
            }
            if (m3u8Ts.isHasKey() && !TextUtils.isEmpty(m3u8Ts.getMethod())) {
                String key = "METHOD=" + m3u8Ts.getMethod();
                if (!TextUtils.isEmpty(m3u8Ts.getKeyUrl())) {
                    key += ",URI=\"" + m3u8Ts.getKeyUrl() + "\"";
                    if (!TextUtils.isEmpty(m3u8Ts.getKeyIv())) {
                        key += ",IV=" + m3u8Ts.getKeyIv();
                    }
                }
                bfw.write(Constants.TAG_KEY + ":" + key + "\n");
            }
            if (m3u8Ts.isHasDiscontinuity()) {
                bfw.write(Constants.TAG_DISCONTINUITY + "\n");
            }
            bfw.write(Constants.TAG_MEDIA_DURATION + ":" + m3u8Ts.getDuration() + ",\n");
            bfw.write(m3u8Ts.getSegProxyUrl(md5, headers) + "\n");
        }
        bfw.write(Constants.TAG_ENDLIST);
        bfw.flush();
        bfw.close();
    }

    /**
     * 更新M3U8 索引文件中的端口号
     * @param proxyM3U8File
     * @param proxyPort
     * @return
     */
    public static boolean updateM3U8TsPortInfo(File proxyM3U8File, int proxyPort) {
        File tempM3U8File = null;
        if (proxyM3U8File.exists()) {
            File parentFile = proxyM3U8File.getParentFile();
            tempM3U8File = new File(parentFile, "temp_video.m3u8");
        }
        if (tempM3U8File != null) {
            BufferedWriter bfw = null;
            try {
                bfw = new BufferedWriter(new FileWriter(tempM3U8File, false));
            } catch (Exception e) {
                LogUtils.w(TAG, "Create buffered writer file failed, exception="+e);
                ProxyCacheUtils.close(bfw);
                return false;
            }

            InputStreamReader inputStreamReader = null;
            try {
                inputStreamReader = new InputStreamReader(new FileInputStream(proxyM3U8File));
            } catch (Exception e) {
                LogUtils.w(TAG, "Create stream reader failed, exception="+e);
                ProxyCacheUtils.close(inputStreamReader);
                return false;
            }

            BufferedReader bufferedReader;

            if (inputStreamReader != null && bfw != null) {
                bufferedReader = new BufferedReader(inputStreamReader);
                String line;
                try {
                    while((line = bufferedReader.readLine()) != null) {
                        if (line.startsWith(ProxyCacheUtils.LOCAL_PROXY_URL)) {
                            if (sOldPort == 0) {
                                sOldPort = ProxyCacheUtils.getPortFromProxyUrl(line);
                                if (sOldPort == 0) {
                                    tempM3U8File.delete();
                                    return false;
                                } else if (sOldPort == proxyPort) {
                                    tempM3U8File.delete();
                                    return true;
                                }
                            }
                            line = line.replace(":" + sOldPort, ":" + proxyPort);
                            bfw.write(line + "\n");
                        } else {
                            bfw.write(line + "\n");
                        }
                    }
                } catch (Exception e) {
                    LogUtils.w(TAG, "Read proxy m3u8 file failed, exception="+e);
                    return false;
                } finally {
                    ProxyCacheUtils.close(bfw);
                    ProxyCacheUtils.close(inputStreamReader);
                    ProxyCacheUtils.close(bufferedReader);
                }
            } else {
                ProxyCacheUtils.close(bfw);
                ProxyCacheUtils.close(inputStreamReader);
                return false;
            }

            if (proxyM3U8File.exists() && tempM3U8File.exists()) {
                proxyM3U8File.delete();
                tempM3U8File.renameTo(proxyM3U8File);
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

}

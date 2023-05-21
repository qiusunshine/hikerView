package com.jeffmony.videocache.socket;

import android.text.TextUtils;

import com.jeffmony.videocache.common.VideoCacheException;
import com.jeffmony.videocache.socket.request.HttpRequest;
import com.jeffmony.videocache.socket.response.BaseResponse;
import com.jeffmony.videocache.socket.response.M3U8Response;
import com.jeffmony.videocache.socket.response.M3U8SegResponse;
import com.jeffmony.videocache.socket.response.Mp4Response;
import com.jeffmony.videocache.utils.HttpUtils;
import com.jeffmony.videocache.utils.LogUtils;
import com.jeffmony.videocache.utils.ProxyCacheUtils;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class SocketProcessTask implements Runnable {

    private static final String TAG  = "SocketProcessTask";
    private static AtomicInteger sRequestCountAtomic = new AtomicInteger(0);
    private final Socket mSocket;

    public SocketProcessTask(Socket socket) {
        mSocket = socket;
    }

    @Override
    public void run() {
        sRequestCountAtomic.addAndGet(1);
        LogUtils.i(TAG, "sRequestCountAtomic : " + sRequestCountAtomic.get());
        OutputStream outputStream = null;
        InputStream inputStream = null;
        try {
            outputStream = mSocket.getOutputStream();
            inputStream = mSocket.getInputStream();
            HttpRequest request = new HttpRequest(inputStream, mSocket.getInetAddress());
            while(!mSocket.isClosed()) {
                request.parseRequest();
                BaseResponse response;
                String url = request.getUri();
                url = url.substring(1);
                url = ProxyCacheUtils.decodeUriWithBase64(url);
                LogUtils.d(TAG, "request url=" + url);

                long currentTime = System.currentTimeMillis();
                ProxyCacheUtils.setSocketTime(currentTime);
                if (url.contains(ProxyCacheUtils.VIDEO_PROXY_SPLIT_STR)) {
                    String[] videoInfoArr = url.split(ProxyCacheUtils.VIDEO_PROXY_SPLIT_STR);
                    if (videoInfoArr.length < 3) {
                        throw new VideoCacheException("Local Socket Error Argument");
                    }
                    String videoUrl = videoInfoArr[0];
                    String videoTypeInfo = videoInfoArr[1];
                    String videoHeaders = videoInfoArr[2];

                    Map<String, String> headers = ProxyCacheUtils.str2Map(videoHeaders);
                    LogUtils.d(TAG, videoUrl + "\n" + videoTypeInfo + "\n" + videoHeaders);

                    if (TextUtils.equals(ProxyCacheUtils.M3U8, videoTypeInfo)) {
                        response = new M3U8Response(request, videoUrl, headers, currentTime);
                    } else if (TextUtils.equals(ProxyCacheUtils.NON_M3U8, videoTypeInfo)) {
                        response = new Mp4Response(request, videoUrl, headers, currentTime);
                    } else {
                        //无法从已知的信息判定视频信息，需要重新请求
                        HttpURLConnection connection = HttpUtils.getConnection(videoUrl, headers);
                        String contentType = connection.getContentType();
                        if (ProxyCacheUtils.isM3U8Mimetype(contentType)) {
                            response = new M3U8Response(request, videoUrl, headers, currentTime);
                        } else {
                            response = new Mp4Response(request, videoUrl, headers, currentTime);
                        }
                    }
                    response.sendResponse(mSocket, outputStream);
                } else if (url.contains(ProxyCacheUtils.SEG_PROXY_SPLIT_STR)) {
                    //说明是M3U8 ts格式的文件
                    String[] videoInfoArr = url.split(ProxyCacheUtils.SEG_PROXY_SPLIT_STR);
                    if (videoInfoArr.length < 4) {
                        throw new VideoCacheException("Local Socket for M3U8 ts file Error Argument");
                    }
                    String parentUrl = videoInfoArr[0];
                    String videoUrl = videoInfoArr[1];
                    String fileName = videoInfoArr[2];
                    String videoHeaders = videoInfoArr[3];
                    Map<String, String> headers = ProxyCacheUtils.str2Map(videoHeaders);
                    LogUtils.d(TAG, parentUrl + "\n" + videoUrl + "\n" + fileName + "\n" + videoHeaders);
                    response = new M3U8SegResponse(request, parentUrl, videoUrl, headers, currentTime, fileName);
                    response.sendResponse(mSocket, outputStream);
                } else {
                    throw new VideoCacheException("Local Socket Error url");
                }

            }

        } catch (Exception e) {
            e.printStackTrace();
            LogUtils.w(TAG,"socket request failed, exception=" + e);
        } finally {
            ProxyCacheUtils.close(outputStream);
            ProxyCacheUtils.close(inputStream);
            ProxyCacheUtils.close(mSocket);
            int count = sRequestCountAtomic.decrementAndGet();
            LogUtils.i(TAG, "finally Socket solve count = " + count);
        }
    }
}

package com.jeffmony.videocache.socket.response;

import com.jeffmony.videocache.VideoProxyCacheManager;
import com.jeffmony.videocache.common.VideoCacheException;
import com.jeffmony.videocache.socket.request.HttpRequest;
import com.jeffmony.videocache.socket.request.ResponseState;
import com.jeffmony.videocache.utils.HttpUtils;
import com.jeffmony.videocache.utils.LogUtils;
import com.jeffmony.videocache.utils.ProxyCacheUtils;
import com.jeffmony.videocache.utils.StorageUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

/**
 * @author jeffmony
 * M3U8-TS视频的local server端
 *
 * https://iqiyi.cdn9-okzy.com/20210217/22550_b228d68b/1000k/hls/6f2ac117eac000000.ts&jeffmony_seg&/c462e3fd379ce23333aabed0a3837848/0.ts&jeffmony_seg&unknown
 */
public class M3U8SegResponse extends BaseResponse {

    private static final String TAG = "M3U8SegResponse";
    private String mParentUrl;
    private File mSegFile;
    private String mSegUrl;
    private String mM3U8Md5;    //对应M3U8 url的md5值
    private int mSegIndex;      //M3U8 ts对应的索引位置
    private long mSegLength;
    private String mFileName;

    public M3U8SegResponse(HttpRequest request, String parentUrl, String videoUrl, Map<String, String> headers, long time, String fileName) throws Exception {
        super(request, videoUrl, headers, time);
        mParentUrl = parentUrl;
        mSegUrl = videoUrl;
        mSegFile = new File(mCachePath, fileName);
        LogUtils.i(TAG, "SegFilePath="+mSegFile.getAbsolutePath());
        mFileName = mSegFile.getName();
        mM3U8Md5 = getM3U8Md5(fileName);
        if (mHeaders == null) {
            mHeaders = new HashMap<>();
        }
        mHeaders.put("Connection", "close");
        mSegIndex = getSegIndex(fileName);
        mResponseState = ResponseState.OK;
        LogUtils.i(TAG, "index="+mSegIndex+", parentUrl="+mParentUrl+", segUrl="+mSegUrl);
        VideoProxyCacheManager.getInstance().seekToCacheTaskFromServer(mParentUrl, mSegIndex);
    }

    private String getM3U8Md5(String str) throws VideoCacheException {
        str = str.substring(1);
        int index = str.indexOf("/");
        if (index == -1) {
            throw new VideoCacheException("Error index during getMd5");
        }
        return str.substring(0, index);
    }

    private int getSegIndex(String str) throws VideoCacheException {
        int idotIndex = str.lastIndexOf(".");
        if (idotIndex == -1) {
            throw new VideoCacheException("Error index during getTcd sIndex");
        }
        str = str.substring(0, idotIndex);
        int seperatorIndex = str.lastIndexOf("/");
        if (seperatorIndex == -1) {
            throw new VideoCacheException("Error index during getTsIndex");
        }
        str = str.substring(seperatorIndex + 1);
        if (str.startsWith(ProxyCacheUtils.INIT_SEGMENT_PREFIX)) {
            str = str.substring(ProxyCacheUtils.INIT_SEGMENT_PREFIX.length());
            LogUtils.i(TAG, "str = " + str);
        }
        return Integer.parseInt(str);
    }

    @Override
    public void sendBody(Socket socket, OutputStream outputStream, long pending) throws Exception {
        if (mFileName.startsWith(ProxyCacheUtils.INIT_SEGMENT_PREFIX)) {
            while(!mSegFile.exists()) {
                downloadSegFile(mSegUrl, mSegFile);
                if (mSegLength > 0 && mSegLength == mSegFile.length()) {
                    break;
                }
            }
        } else {
            boolean isM3U8SegCompleted = VideoProxyCacheManager.getInstance().isM3U8SegCompleted(mM3U8Md5, mSegIndex, mSegFile.getAbsolutePath());
            while (!isM3U8SegCompleted) {
                downloadSegFile(mSegUrl, mSegFile);
                isM3U8SegCompleted = VideoProxyCacheManager.getInstance().isM3U8SegCompleted(mM3U8Md5, mSegIndex, mSegFile.getAbsolutePath());
                if (mSegLength > 0 && mSegLength == mSegFile.length()) {
                    break;
                }
            }
            LogUtils.d(TAG,  "FileLength=" + mSegFile.length() + ", segLength=" + mSegLength + ", FilePath=" + mSegFile.getAbsolutePath());
        }
        RandomAccessFile randomAccessFile = null;

        try {
            randomAccessFile = new RandomAccessFile(mSegFile, "r");
            if (randomAccessFile == null) {
                throw new VideoCacheException("M3U8 ts file not found, this=" + this);
            }
            int bufferedSize = StorageUtils.DEFAULT_BUFFER_SIZE;
            byte[] buffer = new byte[bufferedSize];

            while(shouldSendResponse(socket, mM3U8Md5)) {
                int readLength;
                while((readLength = randomAccessFile.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, readLength);
                }
                LogUtils.d(TAG, "Send M3U8 ts file end, this="+this);
                break;
            }
        } catch (Exception e) {
            throw e;
        } finally {
            ProxyCacheUtils.close(randomAccessFile);
        }
    }

    private void downloadSegFile(String url, File file) throws Exception {
        HttpURLConnection connection = null;
        InputStream inputStream = null;
        try {
            connection = HttpUtils.getConnection(url, mHeaders);
            int responseCode = connection.getResponseCode();
            if (responseCode == ResponseState.OK.getResponseCode() || responseCode == ResponseState.PARTIAL_CONTENT.getResponseCode()) {
                inputStream = connection.getInputStream();
                mSegLength = connection.getContentLength();
                saveSegFile(inputStream, file);
            }
        } catch (Exception e) {
            throw e;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
            ProxyCacheUtils.close(inputStream);
        }
    }

    private void saveSegFile(InputStream inputStream, File file) throws Exception {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
            int readLength;
            byte[] buffer = new byte[StorageUtils.DEFAULT_BUFFER_SIZE];
            while ((readLength = inputStream.read(buffer)) != -1) {
                fos.write(buffer, 0, readLength);
            }
        } catch (Exception e) {
            if (file.exists() && mSegLength > 0 && mSegLength == file.length()) {
                //说明此文件下载完成
            } else {
                file.delete();
            }
            throw e;
        } finally {
            ProxyCacheUtils.close(fos);
            ProxyCacheUtils.close(inputStream);
        }
    }


}

package com.jeffmony.m3u8library;

import androidx.annotation.NonNull;

import com.jeffmony.m3u8library.listener.IVideoTransformProgressListener;

public class VideoProcessor {

    private static volatile boolean mIsLibLoaded = false;

    private IVideoTransformProgressListener mListener;

    public static void loadLibrariesOnce() {
        synchronized (VideoProcessor.class) {
            if (!mIsLibLoaded) {
                System.loadLibrary("jeffmony");
                System.loadLibrary("avcodec");
                System.loadLibrary("avformat");
                System.loadLibrary("avutil");
                System.loadLibrary("swresample");
                System.loadLibrary("swscale");

                mIsLibLoaded = true;

                initFFmpegOptions();
            }
        }
    }

    public VideoProcessor() {
        loadLibrariesOnce();
    }

    public static native void initFFmpegOptions();

    //转化视频的封装格式,M3U8 转化为 MP4格式
    public native int transformVideo(String inputPath, String outputPath);

    public void setOnVideoTransformProgressListener(@NonNull IVideoTransformProgressListener listener) {
        mListener = listener;
    }

    //从native层调用上来,回调当前的视频转化进度
    public void invokeVideoTransformProgress(float progress) {
        if (mListener != null) {
            mListener.onTransformProgress(progress);
        }
    }
}

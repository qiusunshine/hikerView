package com.jeffmony.m3u8library;

import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.jeffmony.m3u8library.listener.IVideoTransformListener;
import com.jeffmony.m3u8library.listener.IVideoTransformProgressListener;
import com.jeffmony.m3u8library.thread.VideoProcessThreadHandler;

import java.io.File;

public class VideoProcessManager {

    private static volatile VideoProcessManager sInstance = null;

    public static VideoProcessManager getInstance() {
        if (sInstance == null) {
            synchronized (VideoProcessManager.class) {
                if (sInstance == null) {
                    sInstance = new VideoProcessManager();
                }
            }
        }
        return sInstance;
    }

    public void transformM3U8ToMp4(final String inputFilePath, final String outputFilePath, @NonNull final IVideoTransformListener listener) {
        if (TextUtils.isEmpty(inputFilePath) || TextUtils.isEmpty(outputFilePath)) {
            listener.onTransformFailed(new Exception("Input or output File is empty"));
            return;
        }
        final File inputFile = new File(inputFilePath);
        if (!inputFile.exists()) {
            listener.onTransformFailed(new Exception("Input file is not existing"));
            return;
        }
        VideoProcessThreadHandler.submitRunnableTask(new Runnable() {
            @Override
            public void run() {
                final VideoProcessor processor = new VideoProcessor();
                processor.setOnVideoTransformProgressListener(new IVideoTransformProgressListener() {
                    @Override
                    public void onTransformProgress(float progress) {
                        notifyOnTransformProgress(listener, progress);
                    }
                });
                int result = processor.transformVideo(inputFilePath, outputFilePath);
                if (result == 1) {
                    notifyOnTransformFinished(listener);
                } else {
                    notifyOnMergeFailed(listener, result);
                }
            }
        });
    }

    //回调信息
    private void notifyOnTransformProgress(@NonNull final IVideoTransformListener listener, final float progress) {
        VideoProcessThreadHandler.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                listener.onTransformProgress(progress);
            }
        });
    }

    private void notifyOnTransformFinished(@NonNull final IVideoTransformListener listener) {
        VideoProcessThreadHandler.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                listener.onTransformFinished();
            }
        });
    }

    private void notifyOnMergeFailed(@NonNull final IVideoTransformListener listener, final int result) {
        VideoProcessThreadHandler.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                listener.onTransformFailed(new Exception("mergeVideo failed, result="+result));
            }
        });
    }
}

package com.example.hikerview.ui.download.merge;

import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.example.hikerview.utils.HeavyTaskUtil;
import com.jeffmony.m3u8library.VideoProcessor;
import com.jeffmony.m3u8library.listener.IVideoTransformListener;

import java.io.File;

/**
 * 作者：By 15968
 * 日期：On 2021/8/27
 * 时间：At 21:07
 */

public class VideoProcessManager {

    private static volatile VideoProcessManager sInstance = null;

    public VideoProcessManager() {
    }

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

    public void transformM3U8ToMp4(final String inputFilePath, final String outputFilePath,
                                   @NonNull final IVideoTransformListener listener,
                                   boolean sync) {
        if (sync) {
            transformM3U8ToMp4Sync(inputFilePath, outputFilePath, listener);
            return;
        }
        if (!TextUtils.isEmpty(inputFilePath) && !TextUtils.isEmpty(outputFilePath)) {
            File inputFile = new File(inputFilePath);
            if (!inputFile.exists()) {
                listener.onTransformFailed(new Exception("Input file is not existing"));
            } else {
                HeavyTaskUtil.executeNewTask(() -> {
                    VideoProcessor processor = new VideoProcessor();
                    processor.setOnVideoTransformProgressListener(progress -> VideoProcessManager.this.notifyOnTransformProgress(listener, progress));
                    int result = processor.transformVideo(inputFilePath, outputFilePath);
                    if (result == 1) {
                        VideoProcessManager.this.notifyOnTransformFinished(listener);
                    } else {
                        VideoProcessManager.this.notifyOnMergeFailed(listener, result);
                    }

                });
            }
        } else {
            listener.onTransformFailed(new Exception("Input or output File is empty"));
        }
    }

    public void transformM3U8ToMp4Sync(final String inputFilePath, final String outputFilePath, @NonNull final IVideoTransformListener listener) {
        if (!TextUtils.isEmpty(inputFilePath) && !TextUtils.isEmpty(outputFilePath)) {
            File inputFile = new File(inputFilePath);
            if (!inputFile.exists()) {
                listener.onTransformFailed(new Exception("Input file is not existing"));
            } else {
                VideoProcessor processor = new VideoProcessor();
                processor.setOnVideoTransformProgressListener(listener::onTransformProgress);
                int result = processor.transformVideo(inputFilePath, outputFilePath);
                if (result == 1) {
                    listener.onTransformFinished();
                } else {
                    listener.onTransformFailed(new Exception("mergeVideo failed, result=" + result));
                }
            }
        } else {
            listener.onTransformFailed(new Exception("Input or output File is empty"));
        }
    }

    private void notifyOnTransformProgress(@NonNull final IVideoTransformListener listener, final float progress) {
        VideoProcessThreadHandler.runOnUiThread(() -> listener.onTransformProgress(progress));
    }

    private void notifyOnTransformFinished(@NonNull final IVideoTransformListener listener) {
        VideoProcessThreadHandler.runOnUiThread(() -> listener.onTransformFinished());
    }

    private void notifyOnMergeFailed(@NonNull final IVideoTransformListener listener, final int result) {
        VideoProcessThreadHandler.runOnUiThread(() -> listener.onTransformFailed(new Exception("mergeVideo failed, result=" + result)));
    }
} 
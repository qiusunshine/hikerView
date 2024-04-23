package com.example.hikerview.ui.download;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.annimon.stream.Stream;
import com.annimon.stream.function.Consumer;
import com.example.hikerview.event.DownloadStoreRefreshEvent;
import com.example.hikerview.event.ShowToastMessageEvent;
import com.example.hikerview.model.DownloadRecord;
import com.example.hikerview.service.parser.JSEngine;
import com.example.hikerview.ui.ActivityManager;
import com.example.hikerview.ui.Application;
import com.example.hikerview.ui.browser.model.DetectedMediaResult;
import com.example.hikerview.ui.browser.model.UrlDetector;
import com.example.hikerview.ui.browser.util.CollectionUtil;
import com.example.hikerview.ui.download.exception.DownloadErrorException;
import com.example.hikerview.ui.download.merge.VideoProcessManager;
import com.example.hikerview.ui.download.merge.VideoProcessThreadHandler;
import com.example.hikerview.ui.download.model.M3u8SubStreamInf;
import com.example.hikerview.ui.download.model.ProgressEvent;
import com.example.hikerview.ui.download.util.HttpRequestUtil;
import com.example.hikerview.ui.download.util.ThreadUtil;
import com.example.hikerview.ui.download.util.UUIDUtil;
import com.example.hikerview.ui.download.util.VideoFormatUtil;
import com.example.hikerview.ui.video.VideoPlayerActivity;
import com.example.hikerview.ui.view.XiuTanResultPopup;
import com.example.hikerview.ui.view.toast.ChefSnackbarKt;
import com.example.hikerview.ui.webdlan.RemoteServerManager;
import com.example.hikerview.utils.ClipboardUtil;
import com.example.hikerview.utils.FileUtil;
import com.example.hikerview.utils.FilesUtilsKt;
import com.example.hikerview.utils.HeavyTaskUtil;
import com.example.hikerview.utils.M3u8Utils;
import com.example.hikerview.utils.PreferenceMgr;
import com.example.hikerview.utils.ShareUtil;
import com.example.hikerview.utils.StringUtil;
import com.example.hikerview.utils.ThreadTool;
import com.example.hikerview.utils.ToastMgr;
import com.example.hikerview.utils.contentdisposition.ContentDispositionHolder;
import com.google.android.material.snackbar.Snackbar;
import com.jeffmony.m3u8library.listener.IVideoTransformListener;
import com.lxj.xpopup.XPopup;

import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.greenrobot.eventbus.EventBus;
import org.litepal.LitePal;
import org.litepal.exceptions.LitePalSupportException;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.Security;
import java.security.spec.AlgorithmParameterSpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import timber.log.Timber;

/**
 * Created by xm on 17/8/19.
 */
public class DownloadManager {
    /**
     *
     * 解决java不支持AES/CBC/PKCS7Padding模式解密
     *
     */
    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    private static final String TAG = "DownloadManager";
    private volatile static DownloadManager sInstance;
    //最大同时进行任务数 maxConcurrentTask
    private SortedMap<String, DownloadTask> allDownloadTaskMap = Collections.synchronizedSortedMap(new TreeMap<>());//添加任务时, 先进这个map
    private LinkedBlockingQueue<DownloadTask> downloadTaskLinkedBlockingQueue = new LinkedBlockingQueue<>();

    private Hashtable<String, DownloadThread> taskThreadMap = new Hashtable<>();
    private ReentrantLock downloadWorkThreadCheckLock = new ReentrantLock();
    private Thread storeThread;
    private List<String> canceledTask = Collections.synchronizedList(new ArrayList<>());

    private DownloadManager() {
        DownloadConfig.loadConfig(Application.getContext());
        //启动时把之前正在下载的设置为下载失败
        ThreadTool.INSTANCE.executeNewTask(() -> {
            List<DownloadRecord> lastRunRecords = getDownloadingRecords(3);
            if (!CollectionUtil.isEmpty(lastRunRecords)) {
                for (DownloadRecord record : lastRunRecords) {
                    //设置为下载中断
                    record.setStatus(DownloadStatusEnum.BREAK.getCode());
                    record.save();
                }
            }
            //开启进度保存线程
            storeThread = new Thread(() -> {
                //删除下载中断的文件
                deleteDownloadTemp();
                List<DownloadTask> tasks = new ArrayList<>();
                List<String> cancels = new ArrayList<>();
                while (!Thread.currentThread().isInterrupted()) {
                    tasks.clear();
                    //耗时操作，避免线程并发问题
                    if (!allDownloadTaskMap.isEmpty()) {
                        tasks.addAll(allDownloadTaskMap.values());
                        boolean posted = false;
                        for (DownloadTask task : tasks) {
                            if (DownloadStatusEnum.CHECKING.getCode().equals(task.getStatus())
                                    || DownloadStatusEnum.LOADING.getCode().equals(task.getStatus())
                                    || DownloadStatusEnum.RUNNING.getCode().equals(task.getStatus())
                                    || DownloadStatusEnum.SAVING.getCode().equals(task.getStatus())
                                    || DownloadStatusEnum.MERGING.getCode().equals(task.getStatus())) {
                                posted = true;
                                if (EventBus.getDefault().hasSubscriberForEvent(ProgressEvent.class)) {
                                    long size = task.getSize().get();
                                    long downloaded = task.getTotalDownloaded().get();
                                    String progress = FileUtil.getFormatedFileSize(downloaded);
                                    if (size > 0) {
                                        progress = FileUtil.getFormatedFileSize(size) + "/" + progress;
                                    }
                                    EventBus.getDefault().post(new ProgressEvent(task.getSourcePageTitle(), progress));
                                }
                                break;
                            }
                        }
                        if (!posted && EventBus.getDefault().hasSubscriberForEvent(ProgressEvent.class)) {
                            EventBus.getDefault().post(new ProgressEvent(null, null));
                        }
                        for (DownloadTask downloadTask : tasks) {
                            try {
                                List<DownloadRecord> records = LitePal.where("taskId = ?", downloadTask.getTaskId()).limit(1).find(DownloadRecord.class);
                                if (!CollectionUtil.isEmpty(records)) {
                                    records.get(0).update(downloadTask).save();
                                } else {
                                    new DownloadRecord(downloadTask).save();
                                }
                            } catch (Exception ignored) {
                            }
                        }
                    }
                    try {
                        cancels.clear();
                        if (CollectionUtil.isNotEmpty(canceledTask)) {
                            cancels.addAll(canceledTask);
                            for (String taskId : cancels) {
                                try {
                                    LitePal.deleteAll(DownloadRecord.class, "taskId = ? and status = ?",
                                            taskId, DownloadStatusEnum.CANCEL.getCode());
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    //进度刷新，通知界面刷新列表
                    if (EventBus.getDefault().hasSubscriberForEvent(DownloadStoreRefreshEvent.class)) {
                        EventBus.getDefault().post(new DownloadStoreRefreshEvent());
                    }
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ignored) {
                    }
                }
            });
            storeThread.start();
        });
    }

    private static List<DownloadRecord> getDownloadingRecords(int count) {
        try {
            return LitePal.where("status = ? or status = ? or status = ? or status = ? or status = ?"
                            , DownloadStatusEnum.READY.getCode(), DownloadStatusEnum.LOADING.getCode(),
                            DownloadStatusEnum.RUNNING.getCode(), DownloadStatusEnum.SAVING.getCode(), DownloadStatusEnum.MERGING.getCode())
                    .find(DownloadRecord.class);
        } catch (Exception e) {
            e.printStackTrace();
            if (count <= 0) {
                return null;
            }
            if (e instanceof LitePalSupportException) {
                //Row too big to fit into CursorWindow 自动删掉最后条数据，否则整个数据库都打不开
                try {
                    DownloadRecord record = LitePal.select("id").findLast(DownloadRecord.class);
                    if (record != null) {
                        LitePal.delete(DownloadRecord.class, record.getId());
                        count--;
                        return getDownloadingRecords(count);
                    }
                } catch (Exception exception) {
                    exception.printStackTrace();
                }
            }
        }
        return null;
    }

    public static DownloadManager instance() {
        if (sInstance == null) {
            synchronized (DownloadManager.class) {
                if (sInstance == null) {
                    sInstance = new DownloadManager();
                }
            }
        }
        return sInstance;
    }

    /**
     * 更新数据库状态
     *
     * @param task
     * @param status
     */
    private void updateStoreStatus(DownloadTask task, String status, String failedReason) {
        try {
            List<DownloadRecord> records = LitePal.where("taskId = ?", task.getTaskId()).limit(1).find(DownloadRecord.class);
            if (!CollectionUtil.isEmpty(records)) {
                records.get(0).setStatus(status);
                if (failedReason != null) {
                    records.get(0).setFailedReason(failedReason);
                }
                if (records.get(0).getFinishedTime() <= 0) {
                    records.get(0).setFinishedTime(System.currentTimeMillis());
                }
                if (task.getTotalDownloaded().get() > 0) {
                    records.get(0).setTotalDownloaded(task.getTotalDownloaded().get());
                }
                records.get(0).save();
            }
        } catch (Exception e) {
            Log.e(TAG, "updateStoreStatus: ", e);
        }
    }

    private DownloadRecord findRecord(DownloadTask task) {
        try {
            List<DownloadRecord> records = LitePal.where("taskId = ?", task.getTaskId()).limit(1).find(DownloadRecord.class);
            if (!CollectionUtil.isEmpty(records)) {
                return records.get(0);
            }
        } catch (Exception e) {
            Log.e(TAG, "findRecord: ", e);
        }
        return null;
    }

    public void onDestroy() {
        if (storeThread != null) {
            storeThread.interrupt();
        }
        cancelAllTask();
    }

    /**
     * 删除边下载边播放，并且自动删除的临时任务
     */
    public void clearTempTask(@Nullable String exclude) {
        HeavyTaskUtil.executeNewTask(() -> clearTempTask0(exclude));
    }

    public void clearTempTask0(@Nullable String exclude) {
        List<DownloadRecord> records = LitePal.findAll(DownloadRecord.class);
        if (CollectionUtil.isNotEmpty(records)) {
            records = Stream.of(records).filter(it -> it.getTaskId() != null && it.getTaskId().endsWith("@temp") && !it.getTaskId().equals(exclude)).toList();
            if (CollectionUtil.isEmpty(records)) {
                return;
            }
            DownloadRecordsFragment.deleteRecordsSync(Application.getContext(), records);
        }
    }

    public boolean isBusyForTemp() {
        downloadWorkThreadCheckLock.lock();
        try {
            int nonTempCount = 0;
            for (String id : taskThreadMap.keySet()) {
                if (!id.endsWith("@temp")) nonTempCount++;
            }
            if (nonTempCount >= DownloadConfig.maxConcurrentTask) {
                return true;
            }
        } finally {
            downloadWorkThreadCheckLock.unlock();
        }
        return false;
    }

    public String getTaskStatus(String taskId, DownloadRecord record) {
        for (DownloadTask task : allDownloadTaskMap.values()) {
            if (taskId.equals(task.getTaskId())) {
                return task.getStatus();
            }
        }
        return record == null ? null : LitePal.find(DownloadRecord.class, record.getId()).getStatus();
    }

    public long getTaskSize(String taskId, DownloadRecord record) {
        for (DownloadTask task : allDownloadTaskMap.values()) {
            if (taskId.equals(task.getTaskId())) {
                return task.getSize().get();
            }
        }
        return record == null ? 0 : LitePal.find(DownloadRecord.class, record.getId()).getSize();
    }

    public String getTaskType(String taskId, DownloadRecord record) {
        for (DownloadTask task : allDownloadTaskMap.values()) {
            if (taskId.equals(task.getTaskId())) {
                return task.getVideoType();
            }
        }
        return record == null ? "" : LitePal.find(DownloadRecord.class, record.getId()).getVideoType();
    }

    /**
     * 添加下载任务
     *
     * @param downloadTask
     */
    public void addTask(DownloadTask downloadTask) {
        if (downloadTask.getTaskId().endsWith("@temp")) {
            //边下边播的临时任务，清除之前的任务
            clearTempTask(downloadTask.getTaskId());
        }
        //存到数据库
        if (downloadTask.getSourcePageTitle() != null) {
            downloadTask.setSourcePageTitle(downloadTask.getSourcePageTitle().replace("\n", "-").replace("\r", "-"));
        }
        downloadWorkThreadCheckLock.lock();
        canceledTask.remove(downloadTask.getTaskId());
        DownloadRecord downloadRecord = new DownloadRecord(downloadTask);
        downloadRecord.save();
        try {
            allDownloadTaskMap.put(downloadTask.getTaskId(), downloadTask);
            HeavyTaskUtil.executeNewTask(() -> {
                downloadTask.setStatus(DownloadStatusEnum.CHECKING.getCode());
                detectUrl(downloadTask.getDownloadUrl(), downloadTask.getHeaders(), downloadTask.getSourcePageTitle(),
                        downloadTask.getSuffix(), new DetectListener() {
                            @Override
                            public void onSuccess(VideoInfo videoInfo) {
                                downloadTask.setContentType(videoInfo.getContentType());
                                downloadTask.setVideoType(("player/m3u8".equals(videoInfo.getVideoFormat().getName()) ? "player/m3u8" : "normal"));
                                downloadTask.setFileExtension(videoInfo.getVideoFormat().getName());
                                downloadTask.setFileName(videoInfo.getFileName());
                                downloadTask.setOriginalTitle(downloadTask.getSourcePageTitle());
                                downloadTask.setSourcePageTitle(videoInfo.getFileName());
                                downloadTask.getSize().set(videoInfo.getSize());
                                downloadTask.setStatus(DownloadStatusEnum.READY.getCode());
                                //检查文件名是否已经存在
                                avoidExistRecord(downloadTask, downloadRecord, 0);
                                downloadRecord.update(downloadTask).save();
                                if (taskThreadMap.size() < DownloadConfig.maxConcurrentTask) {
                                    DownloadThread taskThread = getDownloadTaskThread(downloadTask);
                                    taskThreadMap.put(downloadTask.getTaskId(), taskThread);
                                    taskThread.start();
                                } else {
                                    downloadTaskLinkedBlockingQueue.add(downloadTask);
                                }
                            }

                            @Override
                            public void onFailed(String msg) {
                                downloadTask.setFailedReason(msg);
                                downloadTask.setStatus(DownloadStatusEnum.ERROR.getCode());
                                taskFailed(downloadTask);
                            }

                            @Override
                            public void onProgress(int progress, String msg) {

                            }
                        });
            });
            Application.application.startDownloadForegroundService();
        } finally {
            downloadWorkThreadCheckLock.unlock();
        }
    }

    /**
     * 避免文件名已存在
     *
     * @param downloadTask
     * @param downloadRecord
     * @param count
     */
    private void avoidExistRecord(DownloadTask downloadTask, DownloadRecord downloadRecord, int count) {
        if (count >= 8) {
            downloadTask.setSourcePageTitle(downloadTask.getFileName());
            return;
        }
        boolean fileNameExist = false;
        try {
            String sourcePageTitle = downloadRecord.getSourcePageTitle();
            if (downloadTask.getSourcePageTitle() != null) {
                if (StringUtil.isNotEmpty(downloadTask.getFileExtension()) && downloadTask.getSourcePageTitle().endsWith("." + downloadTask.getFileExtension())) {
                    sourcePageTitle = FileUtil.getSimpleName(downloadTask.getSourcePageTitle());
                } else {
                    sourcePageTitle = downloadTask.getSourcePageTitle();
                }
            }
            DownloadRecord exist = LitePal.where("sourcePageTitle = ? and taskId != ?",
                    sourcePageTitle, downloadRecord.getTaskId()).findFirst(DownloadRecord.class);
            if (exist != null) {
                fileNameExist = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (fileNameExist) {
            if (StringUtil.isNotEmpty(downloadTask.getFileExtension()) && downloadTask.getFileName().endsWith("." + downloadTask.getFileExtension())) {
                downloadTask.setFileName(FileUtil.getSimpleName(getCountName(count, downloadTask.getFileName())) + "." + downloadTask.getFileExtension());
            } else {
                downloadTask.setFileName(getCountName(count, downloadTask.getFileName()));
            }
            downloadTask.setSourcePageTitle(downloadTask.getFileName());
            //检查加(1)的文件是否已存在，如果存在就不断追加
            avoidExistRecord(downloadTask, downloadRecord, count + 1);
        } else {
            downloadTask.setSourcePageTitle(downloadTask.getFileName());
        }
    }

    private String getCountName(int count, String fileName) {
        int index = count + 1;
        if (count == 0) {
            return fileName + "(" + index + ")";
        }
        if (fileName.contains("(" + count + ")")) {
            return fileName.replace("(" + count + ")", "(" + index + ")");
        } else if (fileName.contains("(" + index + ")")) {
            return fileName.replace("(" + index + ")", "(" + (index + 1) + ")");
        } else {
            return fileName + "(" + index + ")";
        }
    }

    /**
     * 断点续传
     *
     * @param record
     */
    public void continueDownload(DownloadRecord record, boolean ignoreError) {
        downloadWorkThreadCheckLock.lock();
        canceledTask.remove(record.getTaskId());
        try {
            DownloadTask downloadTask = new DownloadTask(record.getTaskId(), record.getFileName(), record.getVideoType(),
                    record.getFileExtension(), record.getSourcePageUrl(), record.getSourcePageUrl(),
                    record.getSourcePageTitle(), record.getSize());
            downloadTask.getTotalDownloaded().set(record.getTotalDownloaded());
            downloadTask.setStatus(DownloadStatusEnum.READY.getCode());
            downloadTask.setContinueDownload(true);
            downloadTask.setIgnoreError(ignoreError);
            record.setSaveTime(System.currentTimeMillis());
            record.setStatus(downloadTask.getStatus());
            record.save();
            allDownloadTaskMap.put(downloadTask.getTaskId(), downloadTask);
            if (taskThreadMap.size() < DownloadConfig.maxConcurrentTask) {
                DownloadThread taskThread = getDownloadTaskThread(downloadTask);
                taskThreadMap.put(downloadTask.getTaskId(), taskThread);
                taskThread.start();
            } else {
                downloadTaskLinkedBlockingQueue.add(downloadTask);
            }
            Application.application.startDownloadForegroundService();
        } finally {
            downloadWorkThreadCheckLock.unlock();
        }
    }

    /**
     * 暂停下载
     *
     * @param taskId
     */
    public void pauseTask(String taskId) {
        try {
            if (StringUtil.isEmpty(taskId)) {
                return;
            }
            downloadWorkThreadCheckLock.lock();
            Log.d(TAG, "cancelTask: ");
            try {
                if (taskThreadMap.containsKey(taskId)) {
                    taskThreadMap.remove(taskId).interrupt();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            canceledTask.add(taskId);
            downloadWorkThreadCheckLock.unlock();
        } catch (Exception e) {
            Log.d("DownloadManager", "线程已中止, Pass");
        }
        taskFinished(taskId, DownloadStatusEnum.BREAK.getCode());
    }


    /**
     * 将临时任务转成正常任务
     *
     * @param taskId
     */
    public void moveTempTaskToNormal(String taskId) {
        try {
            if (StringUtil.isEmpty(taskId)) {
                return;
            }
            downloadWorkThreadCheckLock.lock();
            //先执行暂停逻辑
            try {
                if (taskThreadMap.containsKey(taskId)) {
                    taskThreadMap.remove(taskId).interrupt();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            canceledTask.add(taskId);
            DownloadRecord record = LitePal.where("taskId = ?", taskId).findFirst(DownloadRecord.class);
            if (record != null && DownloadStatusEnum.SUCCESS.getCode().equals(record.getStatus())) {
                //都下载完成了
                record.setTaskId(taskId.replace("@temp", ""));
                record.save();
                canceledTask.remove(taskId);
            } else if (record != null) {
                //还在下载
                taskFinished(taskId, DownloadStatusEnum.BREAK.getCode());
                record.setTaskId(taskId.replace("@temp", ""));
                record.save();
                continueDownload(record, false);
            }
        } catch (Exception e) {
            Log.d("DownloadManager", "线程已中止, Pass");
        } finally {
            downloadWorkThreadCheckLock.unlock();
        }
    }

    /**
     * 取消下载
     *
     * @param taskId
     */
    public void cancelTask(String taskId) {
        try {
            if (StringUtil.isEmpty(taskId)) {
                return;
            }
            downloadWorkThreadCheckLock.lock();
            Log.d(TAG, "cancelTask: ");
            String tempDir = null;
            try {
                if (taskThreadMap.containsKey(taskId)) {
                    DownloadThread thread = taskThreadMap.remove(taskId);
                    tempDir = thread.getTempDir();
                    thread.interrupt();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            String ext = null;
            List<DownloadRecord> records = LitePal.where("taskId = ?", taskId).limit(1).find(DownloadRecord.class);
            if (!CollectionUtil.isEmpty(records)) {
                ext = records.get(0).getFileExtension();
                records.get(0).delete();
            }
            canceledTask.add(taskId);
            downloadWorkThreadCheckLock.unlock();
            if (StringUtil.isNotEmpty(tempDir)) {
                String finalTempDir = tempDir;
                String finalExt = ext;
                HeavyTaskUtil.executeNewTask(() -> {
                    FileUtil.deleteDirs(finalTempDir);
                    FileUtil.deleteDirs(finalTempDir.replace(".temp", ""));
                    if (StringUtil.isNotEmpty(finalExt)) {
                        FileUtil.deleteDirs(finalTempDir.replace(".temp", "").replace("." + finalExt, ""));
                    }
                    FileUtil.deleteDirs(finalTempDir.replace(".temp", "") + ".download");
                });
            }
        } catch (Exception e) {
            Log.d("DownloadManager", "线程已中止, Pass");
        }
        taskFinished(taskId, DownloadStatusEnum.CANCEL.getCode());
    }

    public void cancelAllTask() {
        downloadWorkThreadCheckLock.lock();
        try {
            Application.application.stopDownloadForegroundService();
            downloadTaskLinkedBlockingQueue.clear();
            for (String taskId : taskThreadMap.keySet()) {
                try {
                    taskThreadMap.get(taskId).interrupt();
                } catch (Exception e) {
                    Log.d("DownloadManager", "线程已中止, Pass");
                }
                canceledTask.add(taskId);
            }
            taskThreadMap.clear();
            allDownloadTaskMap.clear();
        } finally {
            downloadWorkThreadCheckLock.unlock();
        }
    }


    /**
     * 结束任务，唤起下一个任务
     *
     * @param taskId
     * @param status
     */
    public void taskFinished(String taskId, String status) {
        downloadWorkThreadCheckLock.lock();
        try {
            if (!allDownloadTaskMap.containsKey(taskId)) {
                if (allDownloadTaskMap.size() == 0) {
                    Application.application.stopDownloadForegroundService();
                }
                return;
            }
            DownloadTask task = allDownloadTaskMap.remove(taskId);
            updateStoreStatus(task, status, null);

            taskThreadMap.remove(taskId);
            //清除下载队列
            try {
                downloadTaskLinkedBlockingQueue.remove(task);
            } catch (Exception e) {
                e.printStackTrace();
            }
            //下载下一个视频
            if (!downloadTaskLinkedBlockingQueue.isEmpty()) {
                DownloadTask downloadTask = downloadTaskLinkedBlockingQueue.remove();
                DownloadThread taskThread = getDownloadTaskThread(downloadTask);
                taskThreadMap.put(downloadTask.getTaskId(), taskThread);
                taskThread.start();
            }
            String op = "结束";
            switch (DownloadStatusEnum.getByCode(status)) {
                case BREAK:
                    op = "暂停";
                    break;
                case SUCCESS:
                    op = "完成";
                    break;
                case CANCEL:
                    op = "取消";
                    break;
            }

            if (DownloadStatusEnum.SUCCESS.getCode().equals(status)) {
                String finalOp = op;
                moveToDownloadDir(taskId, ok -> {
                    if (taskId.endsWith("@temp")) {
                        return;
                    }
                    int d2 = PreferenceMgr.getInt(Application.getContext(), "download", "d2", 2);
                    if (ok || d2 == 0) {
                        //已经唤起打开文件
                        EventBus.getDefault().post(new ShowToastMessageEvent(task.getSourcePageTitle() + "下载已" + finalOp));
                    } else {
                        ThreadTool.INSTANCE.runOnUI(() -> {
                            //需要自己处理
                            try {
                                Activity activity = ActivityManager.getInstance().getCurrentActivity();
                                if (d2 == 1) {
                                    //提示打开下载页面
                                    ChefSnackbarKt.make(activity.getWindow().getDecorView(), task.getSourcePageTitle() + "下载已" + finalOp, Snackbar.LENGTH_LONG)
                                            .setAction("查看", v -> {
                                                if (activity instanceof DownloadRecordsActivity) {
                                                    ((DownloadRecordsActivity) activity).showPage(null);
                                                    return;
                                                }
                                                Intent intent = new Intent(activity, DownloadRecordsActivity.class);
                                                intent.putExtra("downloaded", true);
                                                activity.startActivity(intent);
                                            }).show();
                                } else {
                                    if (activity instanceof VideoPlayerActivity) {
                                        ((VideoPlayerActivity) activity).hideSubtitleSearchPopup();
                                    }
                                    //提示打开文件
                                    ChefSnackbarKt.make(activity.getWindow().getDecorView(), task.getSourcePageTitle() + "下载已" + finalOp, Snackbar.LENGTH_LONG)
                                            .setAction("打开", v -> {
                                                if (activity instanceof VideoPlayerActivity) {
                                                    ((VideoPlayerActivity) activity).dealDownload(taskId);
                                                    return;
                                                }
                                                if (activity instanceof DownloadRecordsActivity) {
                                                    ((DownloadRecordsActivity) activity).showPage(taskId);
                                                    return;
                                                }
                                                Intent intent = new Intent(activity, DownloadRecordsActivity.class);
                                                intent.putExtra("openRecord", taskId);
                                                intent.putExtra("downloaded", true);
                                                activity.startActivity(intent);
                                            }).show();
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        });
                    }
                });
            } else {
                if (!taskId.endsWith("@temp")) {
                    EventBus.getDefault().post(new ShowToastMessageEvent(task.getSourcePageTitle() + "下载已" + op));
                }
            }

            if (taskThreadMap.size() == 0) {
                Application.application.stopDownloadForegroundService();
            }
        } finally {
            downloadWorkThreadCheckLock.unlock();
        }
    }

    /**
     * 自动转存到公共下载目录
     *
     * @param taskId
     */
    private void moveToDownloadDir(String taskId, Consumer<Boolean> callback) {
        HeavyTaskUtil.executeNewTask(() -> {
            List<DownloadRecord> records = LitePal.where("taskId = ?", taskId).limit(1).find(DownloadRecord.class);
            if (!CollectionUtil.isEmpty(records)) {
                String path0 = null;
                DownloadRecord record = records.get(0);
                //取出边下边播的进度
                String playUrl = RemoteServerManager.instance().getServerUrl(Application.getContext());
                String u = playUrl + "/proxyM3u8Download?id=" + record.getId() + "&type=.m3u8";
                long pos = HeavyTaskUtil.getPlayerPos(Application.getContext(), u);
                String filePath = null;
                if (pos > 0) {
                    filePath = getNormalFilePath(record);
                } else {
                    u = playUrl + "/proxyDownload?id=" + record.getId();
                    pos = HeavyTaskUtil.getPlayerPos(Application.getContext(), u);
                    if (pos > 0) {
                        filePath = getNormalFilePath(record);
                    }
                }
                String ext = record.getFileExtension();
                boolean isApk = "apk".equals(ext);
                boolean isRule = "txt".equals(ext) || "json".equals(ext) || "hiker".equals(ext);
                boolean autoMove = !taskId.endsWith("@temp") && PreferenceMgr.getBoolean(Application.getContext(), "autoMove", false);
                if (!autoMove) {
                    if (isApk || isRule) {
                        path0 = filePath != null ? filePath : getNormalFilePath(record);
                    }
                } else {
                    String path = filePath != null ? filePath : getNormalFilePath(record);
                    if (StringUtils.isNotEmpty(path) && (isApk || isRule)) {
                        path0 = FilesUtilsKt.getNewFilePath(Application.getContext(), path, record.getFilm());
                    }
                    if (StringUtils.isNotEmpty(path) && !FilesUtilsKt.inDownloadDir(Application.getContext(), record)) {
                        String o = FilesUtilsKt.copyToDownloadDir(Application.getContext(), path, record.getFilm());
                        if (pos > 0 && StringUtil.isNotEmpty(o)) {
                            filePath = o;
                        }
                        DownloadRecordsFragment.deleteRecordsSync(Application.getContext(), Collections.singletonList(record));
                    }
                }
                if (StringUtil.isNotEmpty(filePath)) {
                    //转移写入边下边播的进度
                    HeavyTaskUtil.saveNowPlayerPos(Application.getContext(), "file://" + filePath, pos);
                }
                if (path0 != null) {
                    String finalPath = path0;
                    ThreadTool.INSTANCE.runOnUI(() -> {
                        try {
                            if (isRule) {
                                DownloadRecordsFragment.checkDownload(record);
                            } else {
                                ShareUtil.findChooserToDeal(ActivityManager.getInstance().getCurrentActivity(),
                                        finalPath, "application/vnd.android.package-archive");
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
                }
                callback.accept(path0 != null && !isRule);
            }
        });
    }

    /**
     * 任务失败回调，唤起下一个
     *
     * @param downloadTask1
     */
    public void taskFailed(DownloadTask downloadTask1) {
        String taskId = downloadTask1.getTaskId();
        downloadWorkThreadCheckLock.lock();
        try {
            if (!allDownloadTaskMap.containsKey(taskId)) {
                return;
            }
            allDownloadTaskMap.remove(taskId);
            taskThreadMap.remove(taskId);
            updateStoreStatus(downloadTask1, DownloadStatusEnum.ERROR.getCode(), downloadTask1.getFailedReason());
            if (!downloadTaskLinkedBlockingQueue.isEmpty()) {
                DownloadTask downloadTask = downloadTaskLinkedBlockingQueue.remove();
                DownloadThread taskThread = getDownloadTaskThread(downloadTask);
                taskThreadMap.put(downloadTask.getTaskId(), taskThread);
                taskThread.start();
            }

            if (taskThreadMap.size() == 0) {
                Application.application.stopDownloadForegroundService();
            }
        } finally {
            downloadWorkThreadCheckLock.unlock();
        }
    }

    /**
     * 获取header 校验文件格式，同步执行
     *
     * @param url
     * @param headers
     * @param title
     * @param listener
     */
    public void detectUrl(String url, Map<String, String> headers, String title, @Nullable String suffix, @NonNull DetectListener listener) {
        Timber.d("detectUrl: %s", url);
        try {
            listener.onProgress(10, "获取文件头信息");
            VideoFormat videoFormat = null;
            Map<String, List<String>> headerMap;
            String contentType = null;
            if (url.startsWith("file://") && url.contains(".m3u8")) {
                videoFormat = VideoFormatUtil.videoFormatList.get(0);
                headerMap = new HashMap<>();
                contentType = "m3u8";
            } else {
                HttpRequestUtil.HeadRequestResponse headRequestResponse = HttpRequestUtil.performHeadRequest(url, headers);
                if (headRequestResponse == null) {
                    listener.onFailed("获取文件头信息失败，不支持此格式");
                    return;
                }
                listener.onProgress(40, "解析文件格式");
                //url = headRequestResponse.getRealUrl();
                headerMap = headRequestResponse.getHeaderMap();
                List<String> mime = headerMap == null || !headerMap.containsKey("Content-Type") ? null :
                        headerMap.get("Content-Type");
                contentType = mime != null && mime.size() > 0 ? mime.get(0) : null;
                if (StringUtil.isNotEmpty(suffix)) {
                    //以手动输入的后缀为准
                    videoFormat = new VideoFormat(suffix, mime != null && mime.size() > 0 ? mime : Collections.singletonList(suffix));
                } else {
                    videoFormat = VideoFormatUtil.detectVideoFormat(title, url, headerMap);
                    if (videoFormat == null) {
                        //再挣扎一下，忽略常见后缀匹配
                        videoFormat = VideoFormatUtil.getVideoFormatAnyway(title, url);
                        if (videoFormat == null) {
                            //标题和链接就是没有后缀，那就直接下载成bin文件
                            videoFormat = new VideoFormat("bin", Collections.singletonList("bin"));
                        }
                    }
                }
            }
            if ("m3u8".equals(videoFormat.getName())) {
                videoFormat.setName("player/m3u8");
            }
            VideoInfo videoInfo = new VideoInfo();
            listener.onProgress(70, "获取文件大小");
//            if ("player/m3u8".equals(videoFormat.getName())) {
//                //后续动态计算，节约时间
//            } else {
            //提取文件大小，不管是不是m3u8
            long size = 0;
            if (headerMap.containsKey("Content-Length") && headerMap.get("Content-Length").size() > 0) {
                try {
                    size = Long.parseLong(headerMap.get("Content-Length").get(0));
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                    Timber.d(e, "NumberFormatException");
                }
            }
            videoInfo.setSize(size);
//            }
            videoInfo.setUrl(url);
            videoInfo.setFileName(findFileName(title, url, headerMap, videoFormat, suffix));
            videoInfo.setVideoFormat(videoFormat);
            videoInfo.setSourcePageTitle(title);
            videoInfo.setSourcePageUrl(url);
            videoInfo.setContentType(contentType);
            listener.onSuccess(videoInfo);
        } catch (Exception e) {
            e.printStackTrace();
            listener.onFailed("检测出错：" + e.getMessage());
        }
    }

    /**
     * 解析文件名
     *
     * @param dispositionHeader
     * @return
     */
    public static String getDispositionFileName(String dispositionHeader) {
        try {
            ContentDispositionHolder holder = new ContentDispositionHolder(dispositionHeader);
            String name = holder.getFilename();
            if (holder.getParseException() == null && StringUtil.isNotEmpty(name)) {
                name = decodeUrl(name, "UTF-8").trim();
                if (name.contains("UTF-8''")) {
                    if (name.endsWith("UTF-8''")) {
                        return name.split("UTF-8''")[0];
                    } else {
                        return name.split("UTF-8''")[1];
                    }
                }
                return name;
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        if (dispositionHeader != null && !dispositionHeader.isEmpty()) {
            dispositionHeader = dispositionHeader.replaceFirst("(?i)^.*filename=\"?([^\"]+)\"?.*$", "$1");
            String[] strings = dispositionHeader.split(";");
            if (strings.length > 1) {
                dispositionHeader = strings[0];
            }
            if (dispositionHeader.contains(".") && !dispositionHeader.contains("filename=")) {
                return decodeUrl(dispositionHeader, "UTF-8").trim();
            }
        }
        return "";
    }

    private static String decodeUrl(String str, String code) {//url解码
        try {
            str = str.replaceAll("%(?![0-9a-fA-F]{2})", "%25");
            str = str.replaceAll("\\+", "%2B");
            str = java.net.URLDecoder.decode(str, code);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return str;
    }


    public static String getExtByContentDispositionName(String fileName) {
        if (fileName != null && fileName.contains(".")) {
            String ext = FileUtil.getExtension(fileName);
            if (StringUtil.isNotEmpty(ext) && ext.length() < 10 && ShareUtil.getExtensions().contains(ext)) {
                return ext;
            }
        }
        return null;
    }

    /**
     * 从header解析文件名
     *
     * @param title
     * @param url
     * @param headerMap
     * @return
     */
    private String findFileName(String title, String url, Map<String, List<String>> headerMap, VideoFormat videoFormat, @Nullable String suffix) {
        String fileName = "";
        try {
            if (StringUtil.isEmpty(title) || title.startsWith("uuid_")) {
                if (headerMap.containsKey("Content-Disposition")) {
                    fileName = getDispositionFileName(headerMap.get("Content-Disposition").get(0));
                    if (StringUtil.isEmpty(suffix)) {
                        String ext = getExtByContentDispositionName(fileName);
                        if (StringUtil.isNotEmpty(ext)) {
                            videoFormat.setName(ext);
                            videoFormat.setMimeList(Collections.singletonList(ext));
                        }
                    }
                }
                if (StringUtil.isEmpty(fileName)) {
                    fileName = FileUtil.getResourceName(url);
                }
            }
            if (fileName == null || fileName.isEmpty()) {
                String adjustTitle;
                String[] titles = title.replace(File.separator, "").split("\\$");
                if (titles.length > 0) {
                    adjustTitle = titles[0];
                } else {
                    adjustTitle = title;
                }
                fileName = StringUtil.filenameFilter(adjustTitle);
            }
        } catch (Exception e) {
            fileName = UUIDUtil.genUUID();
        }
        String ext = "player/m3u8".equals(videoFormat.getName()) ? "m3u8" : videoFormat.getName();
        return FileUtil.clearExtension(fileName, ext);
    }


    /**
     * 获取普通文件的路径
     *
     * @param downloadRecord
     * @return
     */
    public static String getNormalFilePath(DownloadRecord downloadRecord) {
        File file1 = new File(downloadRecord.getRootPath() + File.separator + downloadRecord.getFileName());
        if (FilesUtilsKt.inDownloadDir(Application.getContext(), downloadRecord)) {
            return file1.getAbsolutePath();
        }
        if (file1.exists() && !file1.isDirectory()) {
            return file1.getAbsolutePath();
        }
        file1 = new File(downloadRecord.getRootPath() + File.separator + downloadRecord.getFileName() + "." + downloadRecord.getFileExtension());
        if (file1.exists() && !file1.isDirectory()) {
            return file1.getAbsolutePath();
        }
        file1 = new File(downloadRecord.getRootPath() + File.separator + downloadRecord.getFileName()
                + File.separator + downloadRecord.getFileName() + "." + downloadRecord.getFileExtension());
        if (file1.exists() && !file1.isDirectory()) {
            return file1.getAbsolutePath();
        }
        if (UrlDetector.isMusic(downloadRecord.getSourcePageUrl()) || DownloadDialogUtil.useSystemDownload(downloadRecord.getFullName(), downloadRecord.getUrl())) {
            File file = new File(downloadRecord.getSourcePageUrl().replace("file://", ""));
            if (file.exists()) {
                return file.getAbsolutePath();
            }
        }
        return null;
    }


    /**
     * 删除缓存文件
     */
    private void deleteDownloadTemp() {
        deleteDownloadTemp(DownloadConfig.defaultRootPath);
        List<DownloadRecord> records = LitePal.findAll(DownloadRecord.class);
        Set<String> paths = new HashSet<>();
        if (CollectionUtil.isNotEmpty(records)) {
            for (DownloadRecord downloadRecord : records) {
                if (StringUtil.isNotEmpty(downloadRecord.getRootPath())) {
                    paths.add(downloadRecord.getRootPath());
                }
            }
        }
        if (CollectionUtil.isNotEmpty(paths)) {
            for (String path : paths) {
                deleteDownloadTemp(path);
            }
        }
    }

    private void deleteDownloadTemp(String path) {
        File dir = new File(path);
        if (!dir.exists()) {
            return;
        }
        File[] files = dir.listFiles();
        if (files != null) {
            long now = System.currentTimeMillis();
            for (File file : files) {
                if (now - file.lastModified() > 3600 * 1000 * 24 * 3) {
                    //只删除大于3天的
                    if (file.getName().endsWith(".temp") || file.getName().endsWith(".download")) {
                        try {
                            deleteFile(file);
                        } catch (Exception ignored) {
                        }
                    } else if (file.isDirectory()) {
                        String[] files1 = file.list();
                        if (files1 == null || files1.length <= 0) {
                            file.delete();
                        }
                    }
                }
            }
        }
    }

    private void deleteFile(File file) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (int i = 0; i < files.length; i++) {
                    File f = files[i];
                    deleteFile(f);
                }
            }
            file.delete();
        } else if (file.exists()) {
            file.delete();
        }
    }

    /**
     * 已经下载的位置
     *
     * @param record
     * @return
     */
    public String getM3u8DownloadedDir(DownloadRecord record) {
        String rootPath = StringUtil.isEmpty(record.getRootPath()) ? DownloadConfig.rootPath : record.getRootPath();
        return rootPath + File.separator + record.getFileName();
    }

    /**
     * 应该要下载的位置
     *
     * @param record
     * @return
     */
    public String getDownloadDir(DownloadTask record) {
        String rootPath = StringUtil.isEmpty(record.getRootPath()) ? DownloadConfig.rootPath : record.getRootPath();
        return rootPath + File.separator + record.getFileName();
    }

    public String getDownloadDir(DownloadRecord record) {
        String rootPath = StringUtil.isEmpty(record.getRootPath()) ? DownloadConfig.rootPath : record.getRootPath();
        return rootPath + File.separator + record.getFileName();
    }

    public String getRootPath(DownloadRecord record) {
        if (record == null) {
            return DownloadConfig.rootPath;
        }
        return StringUtil.isEmpty(record.getRootPath()) ? DownloadConfig.rootPath : record.getRootPath();
    }

    /**
     * 强制忽略错误，仅对m3u8有效
     *
     * @param downloadRecord
     * @throws DownloadErrorException
     */
    public void forceIgnoreError(DownloadRecord downloadRecord) throws DownloadErrorException {
        String downloadTempDir = DownloadConfig.rootPath + File.separator + downloadRecord.getFileName() + ".temp";
        String downloadDir = DownloadConfig.rootPath + File.separator + downloadRecord.getFileName();
        File tempDir = new File(downloadTempDir);
        if (!tempDir.exists() && !new File(downloadDir).exists()) {
            throw new DownloadErrorException("临时文件不存在");
        }
        if (tempDir.exists()) {
            boolean moveDir = FileUtil.renameDir(downloadTempDir, downloadDir);
            if (!moveDir) {
                throw new DownloadErrorException("移动临时文件失败");
            }
        }
        downloadRecord.setStatus(DownloadStatusEnum.SUCCESS.getCode());
        downloadRecord.save();
    }

    private DownloadThread getDownloadTaskThread(DownloadTask downloadTask) {
        DownloadThread taskThread;
        if ("player/m3u8".equals(downloadTask.getVideoType())) {
            taskThread = new M3u8DownloadTaskThread(downloadTask);
        } else {
            taskThread = new NormalFileDownloadTaskThread(downloadTask);
        }
        return taskThread;
    }

    /**
     * 外挂字幕文件下载线程
     */
    class SubtitleDownloadThread extends Thread {
        private DownloadTask downloadTask;
        private String url;
        private String dir;
        private String downloadDir;

        public SubtitleDownloadThread(DownloadTask downloadTask, String dir, String downloadDir) {
            this.downloadTask = downloadTask;
            this.url = downloadTask.getSubtitle();
            this.dir = dir;
            this.downloadDir = downloadDir;
        }

        @Override
        public void run() {
            String destFile = dir + File.separator + "subtitle." + FileUtil.getExtension(url);
            String finalFile = downloadDir + File.separator + "subtitle." + FileUtil.getExtension(url);
            if (url.startsWith("hiker://files/") || url.startsWith("file://")) {
                String path = JSEngine.getFilePath(url);
                if (!StringUtils.equals(path, destFile)) {
                    FileUtil.copy(new File(path), new File(destFile));
                }
                downloadTask.setSubtitle("file://" + finalFile);
            } else {
                try {
                    if (save2File(HttpRequestUtil.sendGetRequest(url, downloadTask.getHeaders()), destFile)) {
                        downloadTask.setSubtitle("file://" + finalFile);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private boolean save2File(HttpRequestUtil.StreamResponse streamResponse, String saveFilePath) throws IOException {
            try (DataInputStream dis = new DataInputStream(streamResponse.getBody()); FileOutputStream fos = new FileOutputStream(new File(saveFilePath))) {
                //建立一个新的文件
                byte[] buffer = new byte[1024];
                int length;
                //开始填充数据
                while ((length = dis.read(buffer)) > 0) {
                    if (Thread.currentThread().isInterrupted()) {
                        return false;
                    }
                    fos.write(buffer, 0, length);
                }
            }
            return true;
        }
    }

    /**
     * M3U8格式下载线程
     */
    class M3u8DownloadTaskThread extends DownloadThread {
        private DownloadTask downloadTask;
        private boolean continueDownload;
        private LinkedBlockingQueue<Map<String, String>> sizeDetectQueue = new LinkedBlockingQueue<Map<String, String>>();
        private String lastAddPriorityIndex = "";
        private int addPriorityCount = 0;
        private int maxPriority = 0;
        private AtomicInteger highPriorityTaskCount = new AtomicInteger();
        private final PriorityBlockingQueue<Map<String, String>> downloadQueue = new PriorityBlockingQueue<>(11, new Comparator<Map<String, String>>() {
            @Override
            public int compare(Map<String, String> o1, Map<String, String> o2) {
                long a = Long.parseLong(o2.get("priority")) - Long.parseLong(o1.get("priority"));
                return a > 0 ? 1 : (a == 0 ? 0 : -1);
            }
        });

        /**
         * 提高下载片段的优先级，让它优先下载
         *
         * @param name
         */
        public void addPriority(String name) {
            //当前及后面10个片段都优先开始下载
            int count = 10;
            synchronized (downloadQueue) {
                int index = -1;
                //先找到自己那个片段
                try {
                    for (Map<String, String> map : downloadQueue) {
                        if (StringUtils.equals(name, map.get("name"))) {
                            index = Integer.parseInt(map.get("index"));
                            break;
                        }
                    }
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
                if (index < 0) {
                    return;
                }
                if (StringUtils.equals(lastAddPriorityIndex, name)) {
                    return;
                }
                lastAddPriorityIndex = name;
                //增益，让每次重排都能得到更高的优先级
                addPriorityCount = addPriorityCount + 2;
                int c = 0;
                int max = maxPriority;
                for (Map<String, String> map : downloadQueue) {
                    String key = map.get("index");
                    int priority = 0;
                    boolean eq = false;
                    //*2是因为index并不是连续的
                    for (int i = 0; i < count * 2; i++) {
                        if ((i + index + "").equals(key)) {
                            eq = true;
                            priority = max - i + addPriorityCount;
                            break;
                        }
                    }
                    if (!eq) {
                        continue;
                    }
                    if (!String.valueOf(priority).equals(map.get("priority"))) {
                        map.put("priority", "" + priority);
                        //动态修改优先级，只能先移除再添加
                        boolean ok = downloadQueue.remove(map);
                        if (ok) {
                            downloadQueue.put(map);
                        }
                        Timber.d("DownloadFileBody, addPriority, index=%s, priority=%s", key, priority);
                    }
                    c++;
                    if (c >= count) {
                        break;
                    }
                }
                //加急任务，让哨兵活动起来
                highPriorityTaskCount.addAndGet(c);
            }
        }

        private List<Thread> workerThread = new ArrayList<Thread>(DownloadConfig.m3U8DownloadThreadNum);
        private AtomicBoolean isWorkerThreadFailed = new AtomicBoolean(false);
        private AtomicInteger finishedTaskCount = new AtomicInteger(0);
        private Thread speedCheckerThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        Thread.sleep(1000);
                        long lastClearSpeedTime = downloadTask.getLastClearSpeedTime();
                        downloadTask.setLastClearSpeedTime(System.currentTimeMillis());
                        long lastDurationDownloadSize = downloadTask.getLastDurationDownloadSize().getAndSet(0);
                        int finished = finishedTaskCount.get();
                        if (finished > 0) {
                            int taskCount = sizeDetectQueue.size();
                            long forecastSize = downloadTask.getTotalDownloaded().get() * taskCount / finished;
                            long now = downloadTask.getSize().get();
                            if (continueDownload && now > 1024 * 1024 * 100 && forecastSize / now > 5) {
                                //继续下载，并且预测大小超出上一次的5倍，那么不变
                            } else {
                                downloadTask.getSize().set(forecastSize);
                            }
                        }
                        long timePass = System.currentTimeMillis() - lastClearSpeedTime;
                        if (timePass <= 0) {
                            continue;
                        }
                        downloadTask.setCurrentSpeed(lastDurationDownloadSize * 1000 / timePass);
                    } catch (InterruptedException e) {
                        Log.d("DownloadManager", "thread (" + downloadTask.getTaskId() + ") :Interrupted");
                        break;
                    }
                }
            }
        });


        M3u8DownloadTaskThread(DownloadTask downloadTask) {
            this.downloadTask = downloadTask;
            if (downloadTask.isContinueDownload()) {
                this.continueDownload = true;
            }
        }

        /**
         * 干活工厂
         *
         * @param threadId
         * @return
         */
        private Runnable buildDownloadRunnable(int threadId) {
            return new Runnable() {

                /**
                 *  干活
                 * @return 是否中断
                 */
                private boolean work() {
                    if (isWorkerThreadFailed.get()) {
                        return true;
                    }
                    Map<String, String> taskMap;
                    try {
                        synchronized (downloadQueue) {
                            taskMap = downloadQueue.poll();
                        }
                        if (taskMap == null) {
                            return true;
                        }
//                        Timber.d("DownloadFileBody, threadId=%s, taskMap: index=%s, priority=%s, rangeHeader=%s", threadId == 0,
//                                taskMap.get("index"), taskMap.get("priority"), taskMap.get("rangeHeader"));
                    } catch (NoSuchElementException e) {
                        Log.d("DownloadManager", "thread (" + downloadTask.getTaskId() + ") :exited");
                        return true;
                    }
                    String taskUrl = taskMap.get("url");
                    String downloadPath = taskMap.get("downloadPath");
                    boolean isKey = downloadPath != null && downloadPath.endsWith(".key");
                    if (Thread.currentThread().isInterrupted()) {
                        Log.d("DownloadManager", "download thread (" + downloadTask.getTaskId() + ") :return early");
                        return true;
                    }
                    Log.d("DownloadManager", "start download taskUrl=" + taskUrl);
                    int failCount = 0;
                    while (!Thread.currentThread().isInterrupted() && !downloadFile(taskUrl, downloadPath)) {
                        //如果检测失败
                        failCount++;
                        if (failCount >= DownloadConfig.downloadSubFileRetryCountOnFail) {
                            isWorkerThreadFailed.set(true);
                            return true;
                        }
                    }
                    if (!isKey) {
                        finishedTaskCount.incrementAndGet();
                    }
                    return false;
                }

                @Override
                public void run() {
                    Log.d("DownloadManager", "thread (" + downloadTask.getTaskId() + ") :start");
                    try {
                        if (threadId == 0) {
                            //哨兵
                            while (!Thread.currentThread().isInterrupted()) {
                                if (downloadQueue.isEmpty() || isWorkerThreadFailed.get()) {
                                    break;
                                }
                                if (highPriorityTaskCount.get() <= 0) {
                                    //没我什么事，休息
                                    Thread.sleep(100);
                                } else {
                                    //领导有需要，干一把
                                    highPriorityTaskCount.decrementAndGet();
                                    if (work()) {
                                        break;
                                    }
                                }
                            }
                        } else {
                            //苦力
                            while (!Thread.currentThread().isInterrupted()) {
                                if (work()) {
                                    break;
                                }
                            }
                        }
                    } catch (Exception e) {
                        Log.w(TAG, "run: " + e.getMessage());
                        interrupt();
                    }
                    Log.d("DownloadManager", "thread (" + downloadTask.getTaskId() + ") :interrupted");
                }

                private boolean downloadFile(String url, String downloadPath) {
                    if (url == null || url.isEmpty()) {
                        //已经下载过，直接返回成功
                        return true;
                    }
                    downloadWorkThreadCheckLock.lock();
                    Log.d(TAG, "downloadFile: " + canceledTask.size());
                    if (Thread.currentThread().isInterrupted() || canceledTask.contains(downloadTask.getTaskId())) {
                        downloadWorkThreadCheckLock.unlock();
                        return false;
                    }
                    downloadWorkThreadCheckLock.unlock();
                    try {
                        save2File(HttpRequestUtil.sendGetRequest(url, downloadTask.getHeaders()), downloadPath);
                        //删除txt文件，代表已经下载完成
                        String txtPath = downloadPath + "txt";
                        File txt = new File(txtPath);
                        if (txt.exists()) {
                            txt.delete();
                        }
                        return true;
                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.d("DownloadManager", "fail IO错误 taskUrl=" + url);
                        return false;
                    }
                }

                private void save2File(HttpRequestUtil.StreamResponse streamResponse, String saveFilePath) throws IOException {
                    File sf = new File(saveFilePath);
                    if (sf.exists()) {
                        long len = sf.length();
                        downloadTask.getTotalDownloaded().addAndGet(-len);
                        sf.delete();
                    }
                    try (DataInputStream dis = new DataInputStream(streamResponse.getBody()); FileOutputStream fos = new FileOutputStream(saveFilePath)) {
                        //建立一个新的文件
                        byte[] buffer = new byte[1024];
                        int length;
                        //开始填充数据
                        while ((length = dis.read(buffer)) > 0) {
                            downloadTask.getLastDurationDownloadSize().addAndGet(length);
                            downloadTask.getTotalDownloaded().addAndGet(length);
                            fos.write(buffer, 0, length);
                            if (Thread.currentThread().isInterrupted()) {
                                Log.d("DownloadManager", "thread (" + downloadTask.getTaskId() + ") save2File :return early");
                                throw new IOException("thread interrupted");
                            }
                        }
                    } catch (IOException e) {
                        if (downloadTask.isIgnoreError()) {
                            int code = streamResponse.getStatusCode();
                            if (code == 404 || code == 403) {
                                if (!sf.exists()) {
                                    sf.createNewFile();
                                }
                                return;
                            }
                        }
                        throw e;
                    }
                }
            };
        }

        @Override
        public void run() {
            super.run();

            try {
                downloadTask.setStatus(DownloadStatusEnum.LOADING.getCode());
                String downloadTempDir = DownloadConfig.rootPath + File.separator + downloadTask.getFileName() + ".temp";
                File downloadTempDirFile = new File(downloadTempDir);


                String downloadDir = getDownloadDir(downloadTask);
                if (!continueDownload) {
                    if (downloadTempDirFile.exists()) {
                        if (downloadTempDirFile.isDirectory()) {
                            //目录已存在 删除
                            FileUtil.deleteDirs(downloadTempDir);
                        } else {
                            downloadTask.setStatus(DownloadStatusEnum.ERROR.getCode());
                            downloadTask.setFailedReason("目录创建失败, 存在同名文件");
                            taskFailed(downloadTask);
                            return;
                        }
                    }
                    boolean makeDirsSuccess = downloadTempDirFile.mkdirs();
                    if (!makeDirsSuccess) {
                        downloadTask.setStatus(DownloadStatusEnum.ERROR.getCode());
                        downloadTask.setFailedReason("目录创建失败");
                        taskFailed(downloadTask);
                        return;
                    }
                }

                try {
                    parseM3u8(downloadTask.getDownloadUrl(), downloadTask.getHeaders(), "index.m3u8", downloadTask.getFileName(), downloadTempDir, sizeDetectQueue, downloadQueue);
                } catch (IOException e) {
                    e.printStackTrace();
                    //判断是不是stream流，且链接不包含m3u8的也识别为m3u8了
                    if (VideoFormatUtil.isStream(downloadTask.getContentType())
                            && !downloadTask.getDownloadUrl().contains("m3u8")) {
                        //大概率是识别错了格式，改成非m3u8重新下载
                        VideoFormat videoFormat = VideoFormatUtil.getVideoFormatAnyway(downloadTask.getOriginalTitle(), downloadTask.getUrl(), false);
                        if (videoFormat != null && !"m3u8".equals(videoFormat.getName())) {
                            downloadWorkThreadCheckLock.lock();
                            try {
                                downloadTask.setVideoType("normal");
                                downloadTask.setFileExtension(videoFormat.getName());
                                downloadTask.setStatus(DownloadStatusEnum.CHECKING.getCode());
                                DownloadRecord record = findRecord(downloadTask);
                                if (record != null) {
                                    record.update(downloadTask, true).save();
                                }
                                DownloadThread taskThread = getDownloadTaskThread(downloadTask);
                                taskThreadMap.put(downloadTask.getTaskId(), taskThread);
                                taskThread.start();
                                if (downloadTempDirFile.exists()) {
                                    if (downloadTempDirFile.isDirectory()) {
                                        //目录已存在 删除
                                        FileUtil.deleteDirs(downloadTempDir);
                                    }
                                }
                            } finally {
                                downloadWorkThreadCheckLock.unlock();
                            }
                            return;
                        }
                    }
                    Map<String, String> headers = downloadTask.getHeaders();
                    if (headers == null) {
                        headers = new HashMap<>();
                    }
                    if (!headers.containsKey("Referer")) {
                        //尝试自动加Referer，仿IDM+
                        headers.put("Referer", downloadTask.getDownloadUrl());
                        headers.put("Origin", StringUtil.getHome(downloadTask.getDownloadUrl()));
                        downloadTask.setHeaders(headers);
                        try {
                            parseM3u8(downloadTask.getDownloadUrl(), downloadTask.getHeaders(), "index.m3u8", downloadTask.getFileName(), downloadTempDir, sizeDetectQueue, downloadQueue);
                        } catch (IOException e2) {
                            e2.printStackTrace();
                            downloadTask.setStatus(DownloadStatusEnum.ERROR.getCode());
                            downloadTask.setFailedReason("解析M3U8文件失败");
                            taskFailed(downloadTask);
                            return;
                        }
                    } else {
                        downloadTask.setStatus(DownloadStatusEnum.ERROR.getCode());
                        downloadTask.setFailedReason("解析M3U8文件失败");
                        taskFailed(downloadTask);
                        return;
                    }
                }

                workerThread.clear();
                downloadTask.setStatus(DownloadStatusEnum.RUNNING.getCode());
                speedCheckerThread.start();
                int threadNum = DownloadConfig.m3U8DownloadThreadNum;
                if(downloadTask.getDownloadThread() > 0 && downloadTask.getDownloadThread() <= 64) {
                    threadNum = downloadTask.getDownloadThread();
                }
                for (int i = 0; i < threadNum + 1; i++) {
                    Thread thread = new Thread(buildDownloadRunnable(i));
                    workerThread.add(thread);
                    thread.start();
                }
                try {
                    if (downloadTask.getSubtitle() != null && !downloadTask.getSubtitle().isEmpty()) {
                        SubtitleDownloadThread subtitleDownloadThread = new SubtitleDownloadThread(downloadTask, downloadTempDir, downloadDir);
                        subtitleDownloadThread.start();
                        subtitleDownloadThread.join();
                    }
                    for (Thread thread : workerThread) {
                        thread.join();
                    }
                } catch (InterruptedException e) {
                    Log.i(TAG, "run: InterruptedException");
                    ThreadUtil.interruptThreadList(workerThread);
                    return;
                }
                Log.i(TAG, "run: speedCheckerThread.interrupt()");
                speedCheckerThread.interrupt();
                if (isWorkerThreadFailed.get()) {
                    downloadTask.setStatus(DownloadStatusEnum.ERROR.getCode());
                    downloadTask.setFailedReason("下载失败:1");
                    taskFailed(downloadTask);
                    return;
                }
                boolean moveDir = FileUtil.renameDir(downloadTempDir, downloadDir);
                if (!moveDir) {
                    downloadTask.setStatus(DownloadStatusEnum.ERROR.getCode());
                    downloadTask.setFailedReason("下载失败:2");
                    taskFailed(downloadTask);
                    return;
                }
                if (DownloadConfig.autoMerge && !downloadTask.getTaskId().endsWith("@temp")) {
                    autoMerge(downloadTask, ok -> taskFinished(downloadTask.getTaskId(), DownloadStatusEnum.SUCCESS.getCode()));
                } else {
                    taskFinished(downloadTask.getTaskId(), DownloadStatusEnum.SUCCESS.getCode());
                }
            } catch (Exception e) {
                System.out.println(Thread.currentThread().getName() + " (" + this.getState() + ") catch InterruptedException.");
                interrupt();
            }
        }

        private void parseM3u8(String m3u8Url, Map<String, String> headers, String newM3u8FileName, String relativePath, String outputPath, LinkedBlockingQueue<Map<String, String>> sizeDetectQueue, PriorityBlockingQueue<Map<String, String>> downloadQueue) throws IOException {
            parseM3u80(m3u8Url, headers, newM3u8FileName, relativePath, outputPath, sizeDetectQueue, downloadQueue, 0);
        }

        private void parseM3u80(String m3u8Url, Map<String, String> headers, String newM3u8FileName, String relativePath, String outputPath,
                                LinkedBlockingQueue<Map<String, String>> sizeDetectQueue, PriorityBlockingQueue<Map<String, String>> downloadQueue,
                                int fromSubCount) throws IOException {
            String m3U8Content;
            if (continueDownload) {
                m3U8Content = FileUtil.fileToString(outputPath + File.separator + newM3u8FileName);
                if (m3U8Content.contains("#EXT-X-STREAM-INF")) {
                    boolean subFile = false;
                    for (String lineStr : m3U8Content.split("\n")) {
                        if (lineStr.startsWith("#EXT-X-STREAM-INF")) {
                            subFile = true;
                        } else if (subFile) {
                            String nm = lineStr.replace("/", "");
                            m3U8Content = FileUtil.fileToString(outputPath + File.separator + nm);
                            break;
                        }
                    }
                }
            } else if (m3u8Url.startsWith("file://")) {
                String filePath = m3u8Url.replace("file://", "").split("#")[0].split("\\?")[0];
                m3U8Content = FileUtil.fileToString(filePath);
            } else {
                try {
                    HttpRequestUtil.StringResponse response = HttpRequestUtil.getResponseString(m3u8Url, headers);
                    m3U8Content = response.body;
                    m3u8Url = response.realUrl;
                } catch (Throwable e) {
                    e.printStackTrace();
                    throw new IOException(e.getMessage(), e);
                }
            }
            String newM3u8FileContent = "";
            boolean subFile = false, subChecked = false;
            String[] sss = m3U8Content.split("\n");
            int max = sss.length;
            maxPriority = max;
            int n = 0;
            for (String lineStr : sss) {
                n++;
                if (lineStr.startsWith("#")) {
                    if (continueDownload) {
                        //当成已下载完成
                    } else if (lineStr.startsWith("#EXT-X-KEY:")) {
                        Matcher searchKeyUri = Pattern.compile("URI=\"(.*?)\"").matcher(lineStr);
                        if (!searchKeyUri.find()) {
                            if (lineStr.contains("NONE")) continue;
                            throw new IOException("EXT-X-KEY解析失败");
                        }
                        String keyUri = searchKeyUri.group(1);
                        String keyUrl;
                        if (keyUri != null && (keyUri.startsWith("http://") || keyUri.startsWith("https://"))) {
                            keyUrl = keyUri;
                        } else {
                            keyUrl = new URL(new URL(m3u8Url), keyUri == null ? "" : keyUri.trim()).toString();
                        }
                        String uuidStr = UUIDUtil.genUUID();
                        String keyPath = outputPath + File.separator + uuidStr + ".key";
                        HashMap<String, String> hashMap = new HashMap<String, String>();
                        hashMap.put("url", keyUrl);
                        hashMap.put("downloadPath", keyPath);
                        hashMap.put("priority", String.valueOf(max - n));
                        hashMap.put("index", String.valueOf(n));
                        hashMap.put("name", uuidStr + ".key");
                        downloadQueue.add(hashMap);
                        lineStr = Pattern.compile("URI=\"(.*?)\"").matcher(lineStr).replaceAll("URI=\"" + "/" + uuidStr + ".key\"");
                    } else if (lineStr.startsWith("#EXT-X-STREAM-INF")) {
                        subFile = true;
                    }
                    newM3u8FileContent = newM3u8FileContent + lineStr + "\n";
                } else {
                    String uuidStr = UUIDUtil.genUUID();
                    String videoUri = lineStr.trim();
                    String fileUrl;
                    if (videoUri.startsWith("http://") || videoUri.startsWith("https://")) {
                        fileUrl = videoUri;
                    } else {
                        fileUrl = new URL(new URL(m3u8Url), videoUri).toString();
                    }
                    if (subFile) {
                        subFile = false;
                        if (!subChecked && fromSubCount < 3) {
                            subChecked = true;
                            //解析多个清晰度/分辨率的视频
                            List<M3u8SubStreamInf> subs = new ArrayList<>();
                            M3u8SubStreamInf lastSub = null;
                            for (String l2 : sss) {
                                if (l2.startsWith("#EXT-X-STREAM-INF")) {
                                    lastSub = new M3u8SubStreamInf();
                                    String[] l3 = l2.trim().replace("#EXT-X-STREAM-INF:", "").split(",");
                                    for (String l4 : l3) {
                                        String[] l5 = l4.trim().split("=");
                                        if (l5.length > 1) {
                                            switch (l5[0]) {
                                                case "RESOLUTION":
                                                    lastSub.setResolution(l5[1]);
                                                    break;
                                                case "BANDWIDTH":
                                                    lastSub.setBandwidth(l5[1]);
                                                    break;
                                                case "PROGRAM-ID":
                                                    lastSub.setProgramId(l5[1]);
                                                    break;
                                                case "CODECS":
                                                    lastSub.setCodecs(l5[1]);
                                                    break;
                                                case "AUDIO":
                                                    lastSub.setAudio(l5[1]);
                                                    break;
                                                case "SUBTITLES":
                                                    lastSub.setSubtitles(l5[1]);
                                                    break;
                                            }
                                        }
                                    }
                                } else {
                                    if (lastSub != null) {
                                        lastSub.setPath(new URL(new URL(m3u8Url), l2.trim()).toString());
                                        subs.add(lastSub);
                                        lastSub = null;
                                    }
                                }
                            }
                            if (subs.size() > 1) {
                                //有多个清晰度
                                M3u8SubStreamInf holder = new M3u8SubStreamInf();
                                CountDownLatch lock = new CountDownLatch(1);
                                ThreadTool.INSTANCE.runOnUI(() -> {
                                    List<DetectedMediaResult> results = Stream.of(subs)
                                            .map(it -> new DetectedMediaResult(it.getPath(), it.getDescription())).toList();
                                    new XPopup.Builder(ActivityManager.getInstance().getCurrentActivity())
                                            .moveUpToKeyboard(false) //如果不加这个，评论弹窗会移动到软键盘上面
                                            .dismissOnBackPressed(false)
                                            .dismissOnTouchOutside(false)
                                            .asCustom(new XiuTanResultPopup(ActivityManager.getInstance().getCurrentActivity())
                                                    .withDismissOnClick(true)
                                                    .withTitle("请选择要下载的片段（" + downloadTask.getSourcePageTitle() + "）")
                                                    .with(results, (uuu, type) -> {
                                                        if ("play".equals(type)) {
                                                            holder.setPath(uuu);
                                                            for (DetectedMediaResult result : results) {
                                                                if (uuu.equals(result.getUrl())) {
                                                                    ToastMgr.shortBottomCenter(Application.getContext(), "已选择" + result.getTitle());
                                                                    break;
                                                                }
                                                            }
                                                            lock.countDown();
                                                        } else if ("复制链接".equals(type)) {
                                                            ClipboardUtil.copyToClipboard(Application.getContext(), uuu);
                                                        } else {
                                                            ShareUtil.findChooserToDeal(ActivityManager.getInstance().getCurrentActivity(), uuu);
                                                        }
                                                    })).show();
                                });
                                try {
                                    lock.await(1, TimeUnit.MINUTES);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                if (StringUtil.isNotEmpty(holder.getPath())) {
                                    parseM3u80(holder.getPath(), headers, newM3u8FileName, relativePath, outputPath, sizeDetectQueue, downloadQueue, fromSubCount + 1);
                                    return;
                                }
                            }
                        }
                        parseM3u8(fileUrl, headers, uuidStr + ".m3u8", relativePath, outputPath, sizeDetectQueue, downloadQueue);
                        newM3u8FileContent = newM3u8FileContent + "/" + uuidStr + ".m3u8\n";
//                        newM3u8FileContent = newM3u8FileContent + "/" + relativePath + "/" + uuidStr + ".m3u8\n";
                    } else {
                        String videoFilePath = outputPath + File.separator + uuidStr + ".ts";
                        if (continueDownload) {
                            videoFilePath = outputPath + File.separator + videoUri;
                            String txtPath = videoFilePath + "txt";
                            if (new File(txtPath).exists()) {
                                //没有下载完成的文件有tstxt文件
                                fileUrl = FileUtil.fileToString(txtPath);
                            } else {
                                //已经下载完成的文件无tstxt文件
                                fileUrl = "";
                            }
                        } else {
                            //写入tstxt文件，代表还没有下载该分段
                            String txtPath = videoFilePath + "txt";
                            if (!new File(txtPath).exists()) {
                                FileUtil.stringToFile(fileUrl, txtPath);
                            }
                        }
                        HashMap<String, String> hashMap = new HashMap<String, String>();
                        hashMap.put("url", fileUrl);
                        hashMap.put("downloadPath", videoFilePath);
                        hashMap.put("priority", String.valueOf(max - n));
                        hashMap.put("index", String.valueOf(n));
                        hashMap.put("name", uuidStr + ".ts");
                        sizeDetectQueue.add(hashMap);
                        downloadQueue.add(hashMap);
                        newM3u8FileContent = newM3u8FileContent + "/" + uuidStr + ".ts\n";
//                        newM3u8FileContent = newM3u8FileContent + "/" + relativePath + "/" + uuidStr + ".ts\n";
                    }
                }
            }
            if (!continueDownload) {
                FileUtil.stringToFile(newM3u8FileContent, outputPath + File.separator + newM3u8FileName);
            }
        }

        @Override
        public String getTempDir() {
            return DownloadConfig.rootPath + File.separator + downloadTask.getFileName() + ".temp";
        }
    }

    /**
     * 自动合并m3u8为mp4
     *
     * @param downloadTask
     */
    private void autoMerge(DownloadTask downloadTask, Consumer<Boolean> callback) {
        try {
            List<DownloadRecord> records = LitePal.where("taskId = ?", downloadTask.getTaskId()).limit(1).find(DownloadRecord.class);
            if (!CollectionUtil.isEmpty(records)) {
                if (records.get(0).getFinishedTime() <= 0) {
                    //先更新下完成时间，合并时间不算在里面
                    records.get(0).setFinishedTime(System.currentTimeMillis());
                    records.get(0).setStatus(DownloadStatusEnum.SUCCESS.getCode());
                    records.get(0).save();
                }
                transformM3U8ToMp4(Application.getContext(), records.get(0), new IVideoTransformListener() {
                    @Override
                    public void onTransformProgress(float progress) {

                    }

                    @Override
                    public void onTransformFailed(Exception e) {
                        callback.accept(false);
                        VideoProcessThreadHandler.runOnUiThread(() -> {
                            ToastMgr.shortCenter(Application.getContext(), downloadTask.getSourcePageTitle() + "合并mp4失败");
                        });
                    }

                    @Override
                    public void onTransformFinished() {
                        callback.accept(true);
                    }
                }, true);
            } else {
                callback.accept(true);
            }
        } catch (Exception ignored) {
            callback.accept(false);
        }
    }

    /**
     * 提高下载片段的优先级，让它优先下载
     *
     * @param taskId
     * @param index
     */
    public void addPriority(String taskId, int index) {
        downloadWorkThreadCheckLock.lock();
        try {
            DownloadThread downloadThread = taskThreadMap.get(taskId);
            if (downloadThread instanceof NormalFileDownloadTaskThread) {
                ((NormalFileDownloadTaskThread) downloadThread).addPriority(index);
            }
        } finally {
            downloadWorkThreadCheckLock.unlock();
        }
    }

    /**
     * 提高下载片段的优先级，让它优先下载
     *
     * @param taskId
     * @param name
     */
    public void addPriority(String taskId, String name) {
        downloadWorkThreadCheckLock.lock();
        try {
            DownloadThread downloadThread = taskThreadMap.get(taskId);
            if (downloadThread instanceof M3u8DownloadTaskThread) {
                ((M3u8DownloadTaskThread) downloadThread).addPriority(name);
            }
        } finally {
            downloadWorkThreadCheckLock.unlock();
        }
    }

    /**
     * 普通文件下载线程
     */
    class NormalFileDownloadTaskThread extends DownloadThread {
        private DownloadTask downloadTask;
        private boolean continueDownload;
        private int splitNum = 0;
        private int lastAddPriorityIndex = -1;
        private int addPriorityCount = 0;
        private AtomicInteger highPriorityTaskCount = new AtomicInteger();
        private final PriorityBlockingQueue<Map<String, String>> downloadQueue = new PriorityBlockingQueue<>(11, new Comparator<Map<String, String>>() {
            @Override
            public int compare(Map<String, String> o1, Map<String, String> o2) {
                long a = Long.parseLong(o2.get("priority")) - Long.parseLong(o1.get("priority"));
                return a > 0 ? 1 : (a == 0 ? 0 : -1);
            }
        });

        /**
         * 提高下载片段的优先级，让它优先下载
         *
         * @param index
         */
        public void addPriority(int index) {
            //当前及后面10个片段都优先开始下载
            int count = 10;
            synchronized (downloadQueue) {
                if (lastAddPriorityIndex == index) {
                    return;
                }
                lastAddPriorityIndex = index;
                //增益，让每次重排都能得到更高的优先级
                addPriorityCount++;
                int c = 0;
                int max = (int) (downloadTask.getSize().get() / DownloadConfig.getNormalFileSplitSize(downloadTask)) + 1;
                for (Map<String, String> map : downloadQueue) {
                    String key = map.get("index");
                    int priority = 0;
                    boolean eq = false;
                    for (int i = 0; i < count; i++) {
                        if ((i + index + "").equals(key)) {
                            eq = true;
                            priority = max - i + addPriorityCount;
                            break;
                        }
                    }
                    if (!eq) {
                        continue;
                    }
                    if (!String.valueOf(priority).equals(map.get("priority"))) {
                        map.put("priority", "" + priority);
                        //动态修改优先级，只能先移除再添加
                        boolean ok = downloadQueue.remove(map);
                        if (ok) {
                            downloadQueue.put(map);
                        }
                        Timber.d("DownloadFileBody, addPriority, index=%s, priority=%s", key, priority);
                    }
                    c++;
                    if (c >= count) {
                        break;
                    }
                }
                //加急任务，让哨兵活动起来
                highPriorityTaskCount.addAndGet(c);
            }
        }

        private List<Thread> workerThread = new ArrayList<Thread>(DownloadConfig.normalFileDownloadThreadNum);
        private AtomicBoolean isWorkerThreadFailed = new AtomicBoolean(false);
        private Thread speedCheckerThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        Thread.sleep(1000);
                        long lastClearSpeedTime = downloadTask.getLastClearSpeedTime();
                        downloadTask.setLastClearSpeedTime(System.currentTimeMillis());
                        long lastDurationDownloadSize = downloadTask.getLastDurationDownloadSize().getAndSet(0);
                        long timePass = System.currentTimeMillis() - lastClearSpeedTime;
                        if (timePass <= 0) {
                            continue;
                        }
                        downloadTask.setCurrentSpeed(lastDurationDownloadSize * 1000 / timePass);
                    } catch (InterruptedException e) {
                        Log.d("DownloadManager", "thread (" + downloadTask.getTaskId() + ") :Interrupted");
                        break;
                    }
                }
            }
        });

        NormalFileDownloadTaskThread(DownloadTask downloadTask) {
            this.downloadTask = downloadTask;
            if (downloadTask.isContinueDownload()) {
                this.continueDownload = true;
            }
        }

        /**
         * 干活工厂
         *
         * @param threadId
         * @return
         */
        private Runnable buildDownloadRunnable(int threadId) {
            return new Runnable() {

                /**
                 *  干活
                 * @return 是否中断
                 */
                private boolean work() {
                    if (isWorkerThreadFailed.get()) {
                        return true;
                    }
                    Map<String, String> taskMap;
                    try {
                        synchronized (downloadQueue) {
                            taskMap = downloadQueue.poll();
                        }
                        if (taskMap == null) {
                            return true;
                        }
//                        Timber.d("DownloadFileBody, threadId=%s, taskMap: index=%s, priority=%s, rangeHeader=%s", threadId == 0,
//                                taskMap.get("index"), taskMap.get("priority"), taskMap.get("rangeHeader"));
                    } catch (NoSuchElementException e) {
                        Log.d("DownloadManager", "thread (" + downloadTask.getTaskId() + ") :exited");
                        return true;
                    }
                    String taskUrl = taskMap.get("url");
                    String rangeHeader = taskMap.get("rangeHeader");
                    String downloadPath = taskMap.get("downloadPath");
                    String txtPath = taskMap.get("txtPath");
                    if (Thread.currentThread().isInterrupted()) {
                        Log.d("DownloadManager", "download thread (" + downloadTask.getTaskId() + ") :return early");
                        return true;
                    }
                    Log.d("DownloadManager", "start download taskUrl=" + taskUrl);
                    int failCount = 0;
                    long length = 0;
                    try {
                        length = Long.parseLong(taskMap.get("length"));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    while (!Thread.currentThread().isInterrupted() && !downloadFile(taskUrl, rangeHeader, length, downloadPath, txtPath)) {
                        //如果检测失败
                        failCount++;
                        if (failCount >= DownloadConfig.downloadSubFileRetryCountOnFail) {
                            isWorkerThreadFailed.set(true);
                            return true;
                        }
                        try {
                            Thread.sleep(200);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                            return true;
                        }
                    }
                    return false;
                }

                @Override
                public void run() {
                    Log.d("DownloadManager", "thread (" + downloadTask.getTaskId() + ") :start");
                    try {
                        if (threadId == 0) {
                            //哨兵
                            while (!Thread.currentThread().isInterrupted()) {
                                if (downloadQueue.isEmpty() || isWorkerThreadFailed.get()) {
                                    break;
                                }
                                if (highPriorityTaskCount.get() <= 0) {
                                    //没我什么事，休息
                                    Thread.sleep(100);
                                } else {
                                    //领导有需要，干一把
                                    highPriorityTaskCount.decrementAndGet();
                                    if (work()) {
                                        break;
                                    }
                                }
                            }
                        } else {
                            //苦力
                            while (!Thread.currentThread().isInterrupted()) {
                                if (work()) {
                                    break;
                                }
                            }
                        }
                    } catch (Exception e) {
                        Log.w(TAG, "run: " + e.getMessage());
                        interrupt();
                    }
                    Log.d("DownloadManager", "thread (" + downloadTask.getTaskId() + ") :interrupted");
                }

                private boolean downloadFile(String url, String rangeHeader, long length, String downloadPath, String txtPath) {
                    if (url == null || url.isEmpty()) {
                        //已经下载过，直接返回成功
                        return true;
                    }
                    downloadWorkThreadCheckLock.lock();
                    Log.d(TAG, "downloadFile: " + canceledTask.size());
                    if (Thread.currentThread().isInterrupted() || canceledTask.contains(downloadTask.getTaskId())) {
                        downloadWorkThreadCheckLock.unlock();
                        return false;
                    }
                    downloadWorkThreadCheckLock.unlock();
                    try {
                        HashMap<String, String> hashMap = new HashMap<>();
                        if (downloadTask.getHeaders() != null) {
                            for (Map.Entry<String, String> entry : downloadTask.getHeaders().entrySet()) {
                                hashMap.put(entry.getKey(), entry.getValue());
                            }
                        }
                        hashMap.put("Range", rangeHeader);
                        save2File(HttpRequestUtil.sendGetRequest(url, hashMap), downloadPath, length);
                        //删除txt文件，代表已经下载完成
                        File txt = new File(txtPath);
                        if (txt.exists()) {
                            txt.delete();
                        }
                        return true;
                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.d("DownloadManager", "fail IO错误 taskUrl=" + url);
                        return false;
                    }
                }

                private void save2File(HttpRequestUtil.StreamResponse streamResponse, String saveFilePath, long size) throws IOException {
                    if (Thread.currentThread().isInterrupted()) {
                        throw new IOException("thread interrupted");
                    }
                    File sf = new File(saveFilePath);
                    if (sf.exists()) {
                        long len = sf.length();
                        downloadTask.getTotalDownloaded().addAndGet(-len);
                        sf.delete();
                    }
                    try (DataInputStream dis = new DataInputStream(streamResponse.getBody());
                         FileOutputStream fos = new FileOutputStream(saveFilePath)) {
                        byte[] buffer = new byte[1024];
                        int length;
                        long downloadLength = 0;
                        //开始填充数据
                        while ((length = dis.read(buffer)) > 0) {
                            if (Thread.currentThread().isInterrupted()) {
                                Log.d("DownloadManager", "thread (" + downloadTask.getTaskId() + ") save2File :return early");
                                throw new IOException("thread interrupted");
                            }
                            downloadTask.getLastDurationDownloadSize().addAndGet(length);
                            downloadTask.getTotalDownloaded().addAndGet(length);
                            fos.write(buffer, 0, length);
                            downloadLength = downloadLength + length;
                        }
                        if (size > 0) {
                            if (Math.abs(downloadLength - size) > 1) {
                                throw new IOException(String.format("download size error, need=%s, download=%s", size, downloadLength));
                            }
                        }
                    } catch (IOException e) {
                        if (downloadTask.isIgnoreError()) {
                            int code = streamResponse.getStatusCode();
                            if (code == 404 || code == 403) {
                                if (!sf.exists()) {
                                    sf.createNewFile();
                                }
                                return;
                            }
                        }
                        throw e;
                    }
                }
            };
        }

        @Override
        public void run() {
            super.run();
            try {
                downloadTask.setStatus(DownloadStatusEnum.LOADING.getCode());

                String downloadFilePath = DownloadConfig.rootPath + File.separator + downloadTask.getFileName() + "." + downloadTask.getFileExtension();

                String downloadTempDir = downloadFilePath + ".temp";
                File downloadTempDirFile = new File(downloadTempDir);

                String downloadTempFilePath = downloadFilePath + ".download";
                File downloadTempFile = new File(downloadTempFilePath);

                String finalDir = getDownloadDir(downloadTask);
                File finalDirFile = new File(finalDir);

                if (!continueDownload) {
                    if (downloadTempDirFile.exists()) {
                        if (downloadTempDirFile.isDirectory()) {
                            //目录已存在 删除
                            FileUtil.deleteDirs(downloadTempDir);
                        } else {
                            downloadTask.setStatus(DownloadStatusEnum.ERROR.getCode());
                            downloadTask.setFailedReason("目录创建失败, 存在同名文件");
                            taskFailed(downloadTask);
                            return;
                        }
                    }
                    boolean makeDirsSuccess = downloadTempDirFile.mkdirs();
                    if (finalDirFile.exists()) {
                        if (finalDirFile.isDirectory()) {
                            //目录已存在 删除
                            FileUtil.deleteDirs(finalDir);
                        } else {
                            downloadTask.setStatus(DownloadStatusEnum.ERROR.getCode());
                            downloadTask.setFailedReason("目录创建失败, 存在同名文件finalDir");
                            taskFailed(downloadTask);
                            return;
                        }
                    }
                    finalDirFile.mkdirs();

                    if (downloadTempFile.exists()) {
                        if (downloadTempFile.isDirectory()) {
                            //目录已存在 删除
                            FileUtil.deleteDirs(downloadTempFilePath);
                        } else {
                            downloadTempFile.delete();
                        }
                    }
                    try {
                        downloadTempFile.createNewFile();
                    } catch (IOException e) {
                        e.printStackTrace();
                        downloadTask.setStatus(DownloadStatusEnum.ERROR.getCode());
                        downloadTask.setFailedReason("文件创建失败");
                        taskFailed(downloadTask);
                        return;
                    }
                }

                boolean isFileSupportRange = false;
                int failCount = 0;
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        isFileSupportRange = detectFileSupportRange(downloadTask.getDownloadUrl(), downloadTask.getHeaders());
                        break;
                    } catch (IOException e) {
                        e.printStackTrace();
                        failCount++;
                        if (failCount >= DownloadConfig.downloadSubFileRetryCountOnFail) {
                            downloadTask.setStatus(DownloadStatusEnum.ERROR.getCode());
                            downloadTask.setFailedReason("文件信息获取失败");
                            taskFailed(downloadTask);
                            return;
                        }
                    }
                }

                downloadTask.setStatus(DownloadStatusEnum.RUNNING.getCode());
                speedCheckerThread.start();
                if (!isFileSupportRange || downloadTask.getSize().get() <= 0) {
                    //单线程下载
                    try {
                        save2File(HttpRequestUtil.sendGetRequest(downloadTask.getDownloadUrl(), downloadTask.getHeaders()), downloadTempFilePath);
                    } catch (IOException e) {
                        e.printStackTrace();
                        downloadTask.setStatus(DownloadStatusEnum.ERROR.getCode());
                        downloadTask.setFailedReason("文件下载失败");
                        taskFailed(downloadTask);
                        return;
                    }
                    speedCheckerThread.interrupt();
                    if (downloadTask.getSubtitle() != null && !downloadTask.getSubtitle().isEmpty()) {
                        try {
                            SubtitleDownloadThread subtitleDownloadThread = new SubtitleDownloadThread(downloadTask, downloadTempDir, finalDir);
                            subtitleDownloadThread.start();
                            subtitleDownloadThread.join();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    //多线程下载
                    long totalSize = downloadTask.getSize().get();
                    long normalFileSplitSize = DownloadConfig.getNormalFileSplitSize(downloadTask);
                    int n = 0;
                    long max = totalSize / normalFileSplitSize + 1;
                    while (n * normalFileSplitSize < totalSize) {
                        if (Thread.currentThread().isInterrupted()) {
                            Timber.d("thread (" + downloadTask.getTaskId() + ") split file :return early");
                            return;
                        }
                        HashMap<String, String> hashMap = new HashMap<>();
                        long end = (n + 1) * normalFileSplitSize - 1;
                        long start = n * normalFileSplitSize;
                        long size = 0;
                        String range = "bytes=" + start + "-";
                        if (end <= totalSize) {
                            range = range + end;
                            size = end - start + 1;
                        } else {
                            size = totalSize - start + 1;
                        }
                        hashMap.put("length", String.valueOf(size));
                        hashMap.put("rangeHeader", range);
                        String fileUrl = downloadTask.getDownloadUrl();
                        //除以300是避免一个文件夹下文件太多影响IO性能
                        String txtDirPath = downloadTempDir + File.separator + n / 300;
                        File txtDir = new File(txtDirPath);
                        if (!txtDir.exists()) {
                            txtDir.mkdirs();
                        }
                        String videoFilePath = txtDirPath + File.separator + "_." + n;
                        String txtPath = txtDirPath + File.separator + "_" + n + "txt";
                        hashMap.put("txtPath", txtPath);
                        if (continueDownload) {
                            if (new File(txtPath).exists()) {
                                //没有下载完成的文件有txt文件
                            } else {
                                //已经下载完成的文件无txt文件
                                fileUrl = "";
                            }
                        } else {
                            //写入txt文件，代表还没有下载该分段
                            File txtF = new File(txtPath);
                            if (!txtF.exists()) {
                                txtF.createNewFile();
                            }
                        }
                        hashMap.put("url", fileUrl);
                        hashMap.put("downloadPath", videoFilePath);
                        hashMap.put("priority", String.valueOf(max - n));
                        hashMap.put("index", String.valueOf(n));
                        downloadQueue.add(hashMap);
                        n++;
                    }


                    int threadNum = DownloadConfig.m3U8DownloadThreadNum;
                    if(downloadTask.getDownloadThread() > 0 && downloadTask.getDownloadThread() <= 64) {
                        threadNum = downloadTask.getDownloadThread();
                    }
                    for (int i = 0; i < threadNum + 1; i++) {
                        Thread thread = new Thread(buildDownloadRunnable(i));
                        workerThread.add(thread);
                        thread.start();
                    }
                    try {
                        if (downloadTask.getSubtitle() != null && !downloadTask.getSubtitle().isEmpty()) {
                            SubtitleDownloadThread subtitleDownloadThread = new SubtitleDownloadThread(downloadTask, downloadTempDir, finalDir);
                            subtitleDownloadThread.start();
                            subtitleDownloadThread.join();
                        }
                        for (Thread thread : workerThread) {
                            thread.join();
                        }
                    } catch (InterruptedException e) {
                        Log.i(TAG, "run: InterruptedException");
                        ThreadUtil.interruptThreadList(workerThread);
                        return;
                    }
                    Log.i(TAG, "run: speedCheckerThread.interrupt()");
                    speedCheckerThread.interrupt();
                    if (isWorkerThreadFailed.get()) {
                        downloadTask.setStatus(DownloadStatusEnum.ERROR.getCode());
                        downloadTask.setFailedReason("下载失败:1");
                        taskFailed(downloadTask);
                        return;
                    }
                    downloadTask.setStatus(DownloadStatusEnum.SAVING.getCode());
                    try (FileOutputStream outputFileStream = new FileOutputStream(downloadTempFile)) {
                        for (int i = 0; i < n; i++) {
                            File fromFile = new File(downloadTempDir + File.separator + i / 300 + File.separator + "_." + i);
                            try (FileInputStream fromFileStream = new FileInputStream(fromFile)) {
                                int length;
                                byte[] buffer = new byte[4096];
                                while (!Thread.currentThread().isInterrupted() && (length = fromFileStream.read(buffer)) != -1) {
                                    outputFileStream.write(buffer, 0, length);
                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        downloadTask.setStatus(DownloadStatusEnum.ERROR.getCode());
                        downloadTask.setFailedReason("下载失败:2");
                        taskFailed(downloadTask);
                        return;
                    }
                    FileUtil.deleteDirs(downloadTempDir);
                }

                try {
                    String filePath = finalDir + File.separator + downloadTask.getFileName() + "." + downloadTask.getFileExtension();
                    File fileOld = new File(filePath);
                    if (fileOld.exists()) {
                        fileOld.delete();
                    }
                    downloadTempFile.renameTo(fileOld);
                    File fileDir = new File(filePath).getParentFile();
                    try {
                        long size = FileUtil.getFolderSize(new File(finalDir));
                        if (size > 1024 * 1024 && Math.abs(downloadTask.getTotalDownloaded().get() - size) > 1024 * 1024) {
                            downloadTask.getTotalDownloaded().set(size);
                            List<DownloadRecord> records = LitePal.where("taskId = ?", downloadTask.getTaskId()).limit(1).find(DownloadRecord.class);
                            if (!CollectionUtil.isEmpty(records)) {
                                records.get(0).update(downloadTask).save();
                            }
                        }
                    } catch (Exception ignored) {
                    }

                    if (fileDir != null && fileDir.getParentFile() != null) {
                        File newDir = fileDir.getParentFile();
                        if (newDir.exists()) {
                            String p0 = FileUtil.moveFileCompat(fileOld, new File(newDir, fileOld.getName()));
                            if (p0 != null && CollectionUtil.isEmpty(fileDir.list())) {
                                fileDir.delete();
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                taskFinished(downloadTask.getTaskId(), DownloadStatusEnum.SUCCESS.getCode());
            } catch (Exception e) {
                System.out.println(Thread.currentThread().getName() + " (" + this.getState() + ") catch InterruptedException.");
                interrupt();
            }
        }


        private void save2File(HttpRequestUtil.StreamResponse streamResponse, String saveFilePath) throws IOException {
            File sf = new File(saveFilePath);
            if (sf.exists()) {
                long len = sf.length();
                downloadTask.getTotalDownloaded().addAndGet(-len);
                sf.delete();
            }
            try (DataInputStream dis = new DataInputStream(streamResponse.getBody());
                 FileOutputStream fos = new FileOutputStream(new File(saveFilePath))) {
                byte[] buffer = new byte[1024];
                int length;
                //开始填充数据
                while ((length = dis.read(buffer)) > 0) {
                    if (Thread.currentThread().isInterrupted()) {
                        throw new IOException("thread interrupted");
                    }
                    downloadTask.getLastDurationDownloadSize().addAndGet(length);
                    downloadTask.getTotalDownloaded().addAndGet(length);
                    fos.write(buffer, 0, length);
                }
            }
        }

        private boolean detectFileSupportRange(String url, Map<String, String> headers) throws IOException {
            HttpRequestUtil.HeadRequestResponse headRequestResponse = HttpRequestUtil.performHeadRequest(url, headers);
            Map<String, List<String>> headerMap = headRequestResponse.getHeaderMap();
            if (headerMap == null) {
                //检测失败，未找到Content-Type
                Log.d("DownloadManager", "fail 未找到Content-Length taskUrl=" + url);
                throw new IOException("headerMap is null");
            }
            return headerMap.containsKey("Accept-Ranges") && headerMap.get("Accept-Ranges").size() > 0 && "bytes".equals(headerMap.get("Accept-Ranges").get(0).trim());
        }

        @Override
        public String getTempDir() {
            String downloadFilePath = DownloadConfig.rootPath + File.separator + downloadTask.getFileName() + "." + downloadTask.getFileExtension();
            return downloadFilePath + ".temp";
        }
    }

    /**
     * 强制合并非m3u8的分段文件
     *
     * @param record
     * @return 错误信息
     */
    public String forceMergeNormalFile(DownloadRecord record) {
        String downloadFilePath = DownloadConfig.rootPath + File.separator + record.getFileName() + "." + record.getFileExtension();
        String downloadTempDir = downloadFilePath + ".temp";
        File downloadTempDirFile = new File(downloadTempDir);
        if (!downloadTempDirFile.exists()) {
            return "已下载的分段文件不存在";
        }
        String downloadTempFilePath = downloadFilePath + ".download";
        File downloadTempFile = new File(downloadTempFilePath);
        String finalDir = getDownloadDir(record);
        try (FileOutputStream outputFileStream = new FileOutputStream(downloadTempFile)) {
            File[] files = downloadTempDirFile.listFiles();
            if (files == null || files.length == 0) {
                return "空文件夹";
            }
            List<String> fileList = new ArrayList<>();
            Set<String> names = new HashSet<>();
            for (File file : files) {
                if (file.isDirectory()) {
                    File[] files1 = file.listFiles();
                    if (files1 != null) {
                        for (File file1 : files1) {
                            names.add(file1.getName());
                        }
                    }
                }
            }
            for (int i = 0; i < names.size(); i++) {
                String name = "_." + i;
                if (names.contains(name)) {
                    fileList.add(downloadTempDir + File.separator + i / 300 + File.separator + name);
                }
            }
            for (String f : fileList) {
                File fromFile = new File(f);
                if (fromFile.exists() && fromFile.length() > 0) {
                    try (FileInputStream fromFileStream = new FileInputStream(fromFile)) {
                        int length;
                        byte[] buffer = new byte[4096];
                        while ((length = fromFileStream.read(buffer)) > 0) {
                            outputFileStream.write(buffer, 0, length);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "出错：" + e.getMessage();
        }
        FileUtil.deleteDirs(downloadTempDir);
        try {
            String filePath = finalDir + File.separator + record.getFileName() + "." + record.getFileExtension();
            File fileOld = new File(filePath);
            if (fileOld.exists()) {
                fileOld.delete();
            }
            downloadTempFile.renameTo(fileOld);
            File fileDir = new File(filePath).getParentFile();
            try {
                long size = FileUtil.getFolderSize(new File(finalDir));
                if (size > 1024 * 1024 && Math.abs(record.getTotalDownloaded() - size) > 1024 * 1024) {
                    record.setTotalDownloaded(size);
                    record.save();
                }
            } catch (Exception ignored) {
            }

            if (fileDir != null && fileDir.getParentFile() != null) {
                File newDir = fileDir.getParentFile();
                if (newDir.exists()) {
                    String p0 = FileUtil.moveFileCompat(fileOld, new File(newDir, fileOld.getName()));
                    if (p0 != null && CollectionUtil.isEmpty(fileDir.list())) {
                        fileDir.delete();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        taskFinished(record.getTaskId(), DownloadStatusEnum.SUCCESS.getCode());
        return null;
    }

    public SortedMap<String, DownloadTask> getAllDownloadTaskMap() {
        return allDownloadTaskMap;
    }

    public void setAllDownloadTaskMap(SortedMap<String, DownloadTask> allDownloadTaskMap) {
        this.allDownloadTaskMap = allDownloadTaskMap;
    }

    public LinkedBlockingQueue<DownloadTask> getDownloadTaskLinkedBlockingQueue() {
        return downloadTaskLinkedBlockingQueue;
    }

    public void setDownloadTaskLinkedBlockingQueue(LinkedBlockingQueue<DownloadTask> downloadTaskLinkedBlockingQueue) {
        this.downloadTaskLinkedBlockingQueue = downloadTaskLinkedBlockingQueue;
    }

    public Hashtable<String, DownloadThread> getTaskThreadMap() {
        return taskThreadMap;
    }

    public void setTaskThreadMap(Hashtable<String, DownloadThread> taskThreadMap) {
        this.taskThreadMap = taskThreadMap;
    }

    /**
     * M3U8解密合并成MP4
     *
     * @param context
     * @param record
     * @param listener
     */
    public static void transformM3U8ToMp4(Context context, DownloadRecord record, IVideoTransformListener listener) {
        transformM3U8ToMp4(context, record, listener, false);
    }

    private static void transformM3U8ToMp4(Context context, DownloadRecord record,
                                           IVideoTransformListener listener, boolean sync) {
        if (!"player/m3u8".equals(record.getVideoType())) {
            if (!sync) {
                ToastMgr.shortCenter(context, "仅支持m3u8格式的合并");
            }
            if (listener != null) {
                listener.onTransformFailed(new Exception("仅支持m3u8格式的合并"));
            }
            return;
        }
        String www = DownloadConfig.rootPath + File.separator + record.getFileName();
        String m3u8Path = www + File.separator + "index.m3u8";
        String m3u8Path2 = www + File.separator + "index2.m3u8";
        String mp4Path = www + File.separator + record.getFileName() + ".mp4";
        String content = M3u8Utils.INSTANCE.getLocalContent(context, m3u8Path, www, listener, sync);
        if (content == null) {
            if (listener != null) {
                listener.onTransformFailed(new Exception("content is null"));
            }
            return;
        }
        M3U8Key m3U8Key = getKey(m3u8Path, content);
        if (m3U8Key != null) {
            //需要解密
            Runnable task = () -> {
                try {
                    List<String> fileString = new ArrayList<>();
                    String[] s = content.split("\n");
                    for (String valueString : s) {
                        if (valueString.startsWith("/")) {
                            try (InputStream inputStream = new FileInputStream(valueString)) {
                                byte[] c = decrypt(FileUtil.fileToBytes(valueString), inputStream.available(), m3U8Key);
                                String newFilePath = valueString.replace(".ts", ".xy");
                                FileUtil.bytesToFile(newFilePath, c);
                                fileString.add(newFilePath);
                            } catch (Throwable e) {
                                e.printStackTrace();
                            }
                        } else if (!valueString.contains("EXT-X-KEY")) {
                            fileString.add(valueString);
                        }
                    }
                    String newContent = StringUtil.listToString(fileString, "\n");
                    transformInner(context, record, newContent, m3u8Path2, mp4Path, www, listener, sync);
                } catch (Exception e) {
                    e.printStackTrace();
                    if (listener != null) {
                        listener.onTransformFailed(e);
                    }
                }
            };
            if (sync) {
                task.run();
            } else {
                HeavyTaskUtil.executeNewTask(task);
            }
            return;
        }
        transformInner(context, record, content, m3u8Path2, mp4Path, www, listener, sync);
    }

    private static void transformInner(Context context, DownloadRecord record, String content,
                                       String m3u8Path2, String mp4Path, String www,
                                       IVideoTransformListener listener, boolean sync) {
        try {
            FileUtil.stringToFile(content, m3u8Path2);
        } catch (IOException e) {
            e.printStackTrace();
            if (sync) {
                if (listener != null) {
                    listener.onTransformFailed(e);
                }
            } else {
                VideoProcessThreadHandler.runOnUiThread(() -> {
                    ToastMgr.shortCenter(context, "出错：" + e.getMessage());
                    if (listener != null) {
                        listener.onTransformFailed(e);
                    }
                });
            }
            return;
        }

        String taskId = record.getTaskId();
        VideoProcessManager.getInstance()
                .transformM3U8ToMp4(m3u8Path2, mp4Path, new IVideoTransformListener() {
                    @Override
                    public void onTransformProgress(float v) {
                        if (listener != null) {
                            listener.onTransformProgress(v);
                        }
                    }

                    @Override
                    public void onTransformFailed(Exception e) {
                        Timber.e(e);
                        if (sync) {
                            if (listener != null) {
                                listener.onTransformFailed(e);
                            }
                        } else {
                            VideoProcessThreadHandler.runOnUiThread(() -> {
                                ToastMgr.shortCenter(context, "合并失败：" + e.getMessage());
                                if (listener != null) {
                                    listener.onTransformFailed(e);
                                }
                            });
                        }
                    }

                    @Override
                    public void onTransformFinished() {
                        Runnable delTask = () -> {
                            try {
                                List<DownloadRecord> records = LitePal.where("taskId = ?", taskId).limit(1).find(DownloadRecord.class);
                                if (records == null || records.isEmpty()) {
                                    return;
                                }
                                DownloadRecord downloadRecord = records.get(0);
                                downloadRecord.setVideoType("normal");
                                downloadRecord.setFileExtension("mp4");
                                downloadRecord.setStatus(DownloadStatusEnum.SUCCESS.getCode());
                                downloadRecord.save();
                                File mp4 = new File(mp4Path);
                                boolean moveSuccess = false;
                                if (mp4.exists()) {
                                    //把文件移动到文件夹外
                                    File fileDir = mp4.getParentFile();
                                    if (fileDir != null && fileDir.getParentFile() != null) {
                                        File newDir = fileDir.getParentFile();
                                        if (newDir.exists()) {
                                            String p0 = FileUtil.moveFileCompat(mp4, new File(newDir, mp4.getName()));
                                            if (p0 != null) {
                                                moveSuccess = true;
                                            }
                                        }
                                    }
                                }
                                File dir = new File(www);
                                if (dir.isDirectory()) {
                                    File[] ts = dir.listFiles();
                                    if (ts != null && ts.length > 0) {
                                        for (File file : ts) {
                                            if (moveSuccess || file.getAbsolutePath().contains(".ts")
                                                    || file.getAbsolutePath().endsWith(".m3u8")
                                                    || file.getAbsolutePath().contains(".xy")) {
                                                file.delete();
                                            }
                                        }
                                    }
                                    //删除文件夹本身
                                    ts = dir.listFiles();
                                    if ((ts == null || ts.length <= 0) && dir.exists()) {
                                        dir.delete();
                                    }
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        };
                        if (sync) {
                            delTask.run();
                            if (listener != null) {
                                listener.onTransformFinished();
                            }
                        } else {
                            new Thread(() -> {
                                delTask.run();
                                VideoProcessThreadHandler.runOnUiThread(() -> {
                                    ToastMgr.shortCenter(context, "合并完成");
                                    if (listener != null) {
                                        listener.onTransformFinished();
                                    }
                                });
                            }).start();
                        }
                    }
                }, sync);
    }

    private static class M3U8Key {
        //解密算法名称
        private String method;

        //密钥
        private String key = "";

        //密钥字节
        private byte[] keyBytes = new byte[16];

        //key是否为字节
        private boolean isByte = false;

        //IV
        private String iv = "";

        private String keyContent;
    }


    /**
     * 解密ts
     *
     * @param sSrc   ts文件字节数组
     * @param length
     * @return 解密后的字节数组
     */
    private static byte[] decrypt(byte[] sSrc, int length, M3U8Key m3U8Key) throws Exception {
        String sKey = m3U8Key.keyContent;
        String iv = m3U8Key.iv;
        String method = m3U8Key.method;
        boolean isByte = m3U8Key.isByte;
        byte[] keyBytes = m3U8Key.keyBytes;
        if (StringUtil.isNotEmpty(method) && !method.contains("AES"))
            return sSrc;
        // 判断Key是否正确
        if (StringUtils.isEmpty(sKey))
            return null;
        // 判断Key是否为16位
        if (sKey.length() != 16 && !isByte) {
            return sSrc;
        }
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS7Padding");
        SecretKeySpec keySpec = new SecretKeySpec(isByte ? keyBytes : sKey.getBytes(StandardCharsets.UTF_8), "AES");
        byte[] ivByte;
        if (iv.startsWith("0x"))
            ivByte = hexStringToByteArray(iv.substring(2));
        else ivByte = iv.getBytes();
        if (ivByte.length != 16)
            ivByte = new byte[16];
        //如果m3u8有IV标签，那么IvParameterSpec构造函数就把IV标签后的内容转成字节数组传进去
        AlgorithmParameterSpec paramSpec = new IvParameterSpec(ivByte);
        cipher.init(Cipher.DECRYPT_MODE, keySpec, paramSpec);
        return cipher.doFinal(sSrc, 0, length);
    }

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        if ((len & 1) == 1) {
            s = "0" + s;
            len++;
        }
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    /**
     * 获取ts解密的密钥，并把ts片段加入set集合
     *
     * @param url     密钥链接，如果无密钥的m3u8，则此字段可为空
     * @param content 内容，如果有密钥，则此字段可以为空
     * @return ts是否需要解密，null为不解密
     */
    private static M3U8Key getKey(String url, String content) {
        String[] split = content.split("\n");
        M3U8Key m3U8Key = new M3U8Key();
        for (String s : split) {
            //如果含有此字段，则获取加密算法以及获取密钥的链接
            if (s.contains("EXT-X-KEY")) {
                String[] split1 = s.split(",");
                for (String s1 : split1) {
                    if (s1.contains("METHOD")) {
                        m3U8Key.method = s1.split("=", 2)[1];
                        continue;
                    }
                    if (s1.contains("URI")) {
                        m3U8Key.key = s1.split("=", 2)[1];
                        continue;
                    }
                    if (s1.contains("IV"))
                        m3U8Key.iv = s1.split("=", 2)[1];
                }
            }
        }
        if (!StringUtil.isEmpty(m3U8Key.key)) {
            String relativeUrl = url.substring(0, url.lastIndexOf("/") + 1);
            m3U8Key.key = m3U8Key.key.replace("\"", "");
            m3U8Key.keyContent = getUrlContent(isUrl(m3U8Key.key) ? m3U8Key.key : mergeUrl(relativeUrl, m3U8Key.key), true, m3U8Key).toString().replaceAll("\\s+", "");
            return m3U8Key;
        }
        return null;
    }

    private static StringBuilder getUrlContent(String urls, boolean isKey, M3U8Key m3U8Key) {
        StringBuilder content = new StringBuilder();
        try (InputStream inputStream = new FileInputStream(urls);
             BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            if (isKey) {
                byte[] bytes = new byte[128];
                int len;
                len = inputStream.read(bytes);
                m3U8Key.isByte = true;
                if (len == 1 << 4) {
                    m3U8Key.keyBytes = Arrays.copyOf(bytes, 16);
                    content.append("isByte");
                } else {
                    content.append(new String(Arrays.copyOf(bytes, len)));
                }
                return content;
            }
            while ((line = bufferedReader.readLine()) != null) {
                content.append(line).append("\n");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return content;
    }

    public static boolean isUrl(String str) {
        if (StringUtil.isEmpty(str))
            return false;
        str = str.trim();
        return str.matches("^(http|https)://.+");
    }

    private static String mergeUrl(String start, String end) {
        if (end.startsWith("/"))
            end = end.replaceFirst("/", "");
        int position = 0;
        String subEnd, tempEnd = end;
        while ((position = end.indexOf("/", position)) != -1) {
            subEnd = end.substring(0, position + 1);
            if (start.endsWith(subEnd)) {
                tempEnd = end.replaceFirst(subEnd, "");
                break;
            }
            ++position;
        }
        return start + tempEnd;
    }
}

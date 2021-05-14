package com.example.hikerview.ui.download;

import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.hikerview.event.DownloadStoreRefreshEvent;
import com.example.hikerview.event.ShowToastMessageEvent;
import com.example.hikerview.model.DownloadRecord;
import com.example.hikerview.ui.Application;
import com.example.hikerview.ui.browser.util.CollectionUtil;
import com.example.hikerview.ui.download.util.HttpRequestUtil;
import com.example.hikerview.ui.download.util.ThreadUtil;
import com.example.hikerview.ui.download.util.UUIDUtil;
import com.example.hikerview.ui.download.util.VideoFormatUtil;
import com.example.hikerview.utils.FileUtil;
import com.example.hikerview.utils.HeavyTaskUtil;
import com.example.hikerview.utils.PinyinUtil;

import org.greenrobot.eventbus.EventBus;
import org.litepal.LitePal;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by xm on 17/8/19.
 */
public class DownloadManager {
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
        LitePal.where("status = ? or status = ? or status = ? or status = ?"
                , DownloadStatusEnum.READY.getCode(), DownloadStatusEnum.LOADING.getCode(),
                DownloadStatusEnum.RUNNING.getCode(), DownloadStatusEnum.SAVING.getCode())
                .findAsync(DownloadRecord.class).listen(lastRunRecords -> {
            if (!CollectionUtil.isEmpty(lastRunRecords)) {
                for (DownloadRecord record : lastRunRecords) {
                    //设置为下载中断
                    record.setStatus(DownloadStatusEnum.BREAK.getCode());
                    record.save();
                }
            }
            //删除下载中断的文件
            deleteDownloadTemp();
            //开启进度保存线程
            storeThread = new Thread(() -> {
                while (!Thread.currentThread().isInterrupted()) {
                    List<DownloadTask> tasks = new ArrayList<>(allDownloadTaskMap.values());
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
                    try {
                        List<String> cancels = new ArrayList<>(canceledTask);
                        for (String taskId : cancels) {
                            try {
                                LitePal.deleteAll(DownloadRecord.class, "taskId = ?", taskId);
                            } catch (Exception e) {
                                e.printStackTrace();
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

    public void loadConfig() {
        //在构造函数执行
    }

    /**
     * 删除缓存
     */
    private void deleteDownloadTemp() {
        //必须在DownloadConfig初始化后执行
        final String path = DownloadConfig.rootPath;
        File dir = new File(path);
        if (!dir.exists()) {
            return;
        }
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.getName().endsWith(".temp") || file.getName().endsWith(".download")) {
                    try {
                        deleteFile(file);
                    } catch (Exception ignored) {
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
            file.delete();//如要保留文件夹，只删除文件，请注释这行
        } else if (file.exists()) {
            file.delete();
        }
    }

    /**
     * 更新数据库状态
     *
     * @param taskId
     * @param status
     */
    private void updateStoreStatus(String taskId, String status, String failedReason) {
        try {
            List<DownloadRecord> records = LitePal.where("taskId = ?", taskId).limit(1).find(DownloadRecord.class);
            if (!CollectionUtil.isEmpty(records)) {
                records.get(0).setStatus(status);
                if (failedReason != null) {
                    records.get(0).setFailedReason(failedReason);
                }
                records.get(0).setFinishedTime(System.currentTimeMillis());
                records.get(0).save();
            }
        } catch (Exception e) {
            Log.e(TAG, "updateStoreStatus: ", e);
        }
    }

    public void onDestroy() {
        if (storeThread != null) {
            storeThread.interrupt();
        }
        cancelAllTask();
    }

    /**
     * 添加下载任务
     *
     * @param downloadTask
     */
    public void addTask(DownloadTask downloadTask) {
        //存到数据库
        downloadWorkThreadCheckLock.lock();
        canceledTask.remove(downloadTask.getTaskId());
        new DownloadRecord(downloadTask).save();
        try {
            allDownloadTaskMap.put(downloadTask.getTaskId(), downloadTask);
            HeavyTaskUtil.executeNewTask(() -> {
                downloadTask.setStatus(DownloadStatusEnum.CHECKING.getCode());
                detectUrl(downloadTask.getSourcePageUrl(), downloadTask.getSourcePageTitle(), new DetectListener() {
                    @Override
                    public void onSuccess(VideoInfo videoInfo) {
                        downloadTask.setVideoType(("player/m3u8".equals(videoInfo.getVideoFormat().getName()) ? "player/m3u8" : "normal"));
                        downloadTask.setFileExtension(videoInfo.getVideoFormat().getName());
                        downloadTask.setFileName(videoInfo.getFileName());
                        downloadTask.getSize().set(videoInfo.getSize());
                        downloadTask.setStatus(DownloadStatusEnum.READY.getCode());
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

    public void cancelTask(String taskId) {
        try {
            downloadWorkThreadCheckLock.lock();
            Log.d(TAG, "cancelTask: ");
            if (taskThreadMap.contains(taskId)) {
                taskThreadMap.remove(taskId).interrupt();
            }
            List<DownloadRecord> records = LitePal.where("taskId = ?", taskId).limit(1).find(DownloadRecord.class);
            if (!CollectionUtil.isEmpty(records)) {
                records.get(0).delete();
            }
            canceledTask.add(taskId);
            downloadWorkThreadCheckLock.unlock();
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

    public void detectUrl(String url, String title, @NonNull DetectListener listener) {
        Log.d(TAG, "detectUrl: " + url);
        try {
            listener.onProgress(10, "获取文件头信息");
            HttpRequestUtil.HeadRequestResponse headRequestResponse = HttpRequestUtil.performHeadRequest(url);
            if (headRequestResponse == null) {
                listener.onFailed("获取文件头信息失败，不支持此格式");
                return;
            }
            listener.onProgress(40, "解析文件格式");
            url = headRequestResponse.getRealUrl();
            Map<String, List<String>> headerMap = headRequestResponse.getHeaderMap();
            if (headerMap == null || !headerMap.containsKey("Content-Type")) {
                //检测失败，未找到Content-Type
//                Log.d("WorkerThread", "fail 未找到Content-Type:" + JSON.toJSONString(headerMap) + " taskUrl=" + url);
                listener.onFailed("检测失败，未找到Content-Type");
                return;
            }
            Log.d("WorkerThread", "Content-Type:" + headerMap.get("Content-Type").toString() + " taskUrl=" + url);
            VideoFormat videoFormat = VideoFormatUtil.detectVideoFormat(url, headerMap.get("Content-Type").toString());
            if (videoFormat == null) {
                //检测成功，不是视频
                Log.d("WorkerThread", "fail not video taskUrl=" + url);
                listener.onFailed("该格式暂不支持下载");
                return;
            }
            VideoInfo videoInfo = new VideoInfo();
            listener.onProgress(70, "获取文件大小");
            if ("player/m3u8".equals(videoFormat.getName())) {
                //暂时不需要计算文件大小，目前不显示，节约时间
//                double duration = M3U8Util.figureM3U8Duration(url);
//                if (duration <= 0) {
//                    //检测成功，不是m3u8的视频
//                    Log.d("WorkerThread", "fail not m3u8 taskUrl=" + url);
//                    return;
//                }
//                videoInfo.setDuration(duration);
            } else {
                long size = 0;
                if (headerMap.containsKey("Content-Length") && headerMap.get("Content-Length").size() > 0) {
                    try {
                        size = Long.parseLong(headerMap.get("Content-Length").get(0));
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                        Log.d("WorkerThread", "NumberFormatException", e);
                    }
                }
                videoInfo.setSize(size);
            }
            videoInfo.setUrl(url);
            String fileName;
            try {
                String adjustTitle;
                String[] titles = title.replace(File.separator, "").split("\\$");
                if (titles.length > 0) {
                    adjustTitle = titles[0];
                } else {
                    adjustTitle = title.replace(File.separator, "");
                }
                adjustTitle = adjustTitle.replace(" ", "").replace("-", "");
                fileName = PinyinUtil.instance().getPinyin(adjustTitle) + "_" + System.currentTimeMillis();
            } catch (Exception e) {
                fileName = UUIDUtil.genUUID();
            }
            videoInfo.setFileName(fileName);
            videoInfo.setVideoFormat(videoFormat);
            videoInfo.setSourcePageTitle(title);
            videoInfo.setSourcePageUrl(url);
            //检测成功，是视频
            Log.d("WorkerThread", "found video taskUrl=" + url);
            listener.onSuccess(videoInfo);
        } catch (Exception e) {
            e.printStackTrace();
            listener.onFailed("检测出错：" + e.getMessage());
        }
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

    public String getDownloadDir(String fileName) {
        return DownloadConfig.rootPath + File.separator + fileName;
    }

    public void taskFinished(String taskId, String status) {
        downloadWorkThreadCheckLock.lock();
        try {
            if (!taskThreadMap.containsKey(taskId)) {
                return;
            }
            DownloadTask task = allDownloadTaskMap.remove(taskId);
            updateStoreStatus(taskId, status, null);

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
            EventBus.getDefault().post(new ShowToastMessageEvent(task.getSourcePageTitle() + "下载已结束"));

            if (taskThreadMap.size() == 0) {
                Application.application.stopDownloadForegroundService();
            }
        } finally {
            downloadWorkThreadCheckLock.unlock();
        }
    }

    public void taskFailed(DownloadTask downloadTask1) {
        String taskId = downloadTask1.getTaskId();
        downloadWorkThreadCheckLock.lock();
        try {
            if (!taskThreadMap.containsKey(taskId)) {
                return;
            }
            taskThreadMap.remove(taskId);
            updateStoreStatus(taskId, DownloadStatusEnum.ERROR.getCode(), downloadTask1.getFailedReason());
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


    class M3u8DownloadTaskThread extends DownloadThread {
        private DownloadTask downloadTask;
        private LinkedBlockingQueue<Map<String, String>> sizeDetectQueue = new LinkedBlockingQueue<Map<String, String>>();
        private LinkedBlockingQueue<Map<String, String>> downloadQueue = new LinkedBlockingQueue<Map<String, String>>();
        private List<Thread> workerThread = new ArrayList<Thread>(DownloadConfig.m3U8DownloadThreadNum);
        private boolean isWorkerThreadFailed = false;
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


        M3u8DownloadTaskThread(DownloadTask downloadTask) {
            this.downloadTask = downloadTask;
        }

        @Override
        public void run() {
            super.run();

            try {
                downloadTask.setStatus(DownloadStatusEnum.LOADING.getCode());
                String downloadTempDir = DownloadConfig.rootPath + File.separator + downloadTask.getFileName() + ".temp";
                File downloadTempDirFile = new File(downloadTempDir);


                String downloadDir = getDownloadDir(downloadTask.getFileName());

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

                String videoTitleFilePath = downloadTempDir + File.separator + "videoTitle";
                String videoName = TextUtils.isEmpty(downloadTask.getSourcePageTitle()) ? downloadTask.getFileName() : downloadTask.getSourcePageTitle();
                try {
                    FileUtil.stringToFile(videoName, videoTitleFilePath);
                } catch (IOException e) {
                    e.printStackTrace();
                    downloadTask.setStatus(DownloadStatusEnum.ERROR.getCode());
                    downloadTask.setFailedReason("视频名称保存失败");
                    taskFailed(downloadTask);
                    return;
                }

                try {
                    parseM3u8(downloadTask.getUrl(), "index.m3u8", downloadTask.getFileName(), downloadTempDir, sizeDetectQueue, downloadQueue);
                } catch (IOException e) {
                    e.printStackTrace();
                    downloadTask.setStatus(DownloadStatusEnum.ERROR.getCode());
                    downloadTask.setFailedReason("解析M3U8文件失败");
                    taskFailed(downloadTask);
                    return;
                }
                Log.i(TAG, "run: isWorkerThreadFailed");
                if (isWorkerThreadFailed) {
                    downloadTask.setStatus(DownloadStatusEnum.ERROR.getCode());
                    downloadTask.setFailedReason("获取文件大小失败");
                    taskFailed(downloadTask);
                    return;
                }

                workerThread.clear();
                downloadTask.setStatus(DownloadStatusEnum.RUNNING.getCode());
                speedCheckerThread.start();
                for (int i = 0; i < DownloadConfig.m3U8DownloadThreadNum; i++) {
                    Thread thread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            Log.d("DownloadManager", "thread (" + downloadTask.getTaskId() + ") :start");
                            try {
                                while (!Thread.currentThread().isInterrupted()) {
                                    Map<String, String> taskMap;
                                    try {
                                        taskMap = downloadQueue.remove();
                                    } catch (NoSuchElementException e) {
                                        Log.d("DownloadManager", "thread (" + downloadTask.getTaskId() + ") :exited");
                                        break;
                                    }
                                    String taskUrl = taskMap.get("url");
                                    String downloadPath = taskMap.get("downloadPath");
                                    if (Thread.currentThread().isInterrupted()) {
                                        Log.d("DownloadManager", "download thread (" + downloadTask.getTaskId() + ") :return early");
                                        return;
                                    }
                                    Log.d("DownloadManager", "start download taskUrl=" + taskUrl);
                                    int failCount = 0;
                                    while (!Thread.currentThread().isInterrupted() && !downloadFile(taskUrl, downloadPath)) {
                                        //如果检测失败
                                        failCount++;
                                        if (failCount >= DownloadConfig.downloadSubFileRetryCountOnFail) {
                                            isWorkerThreadFailed = true;
                                            return;
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
                            downloadWorkThreadCheckLock.lock();
                            Log.d(TAG, "downloadFile: " + canceledTask.size());
                            if (Thread.currentThread().isInterrupted() || canceledTask.contains(downloadTask.getTaskId())) {
                                downloadWorkThreadCheckLock.unlock();
                                return false;
                            }
                            downloadWorkThreadCheckLock.unlock();
                            try {
                                save2File(HttpRequestUtil.sendGetRequest(url), downloadPath);
                                return true;
                            } catch (IOException e) {
                                e.printStackTrace();
                                Log.d("DownloadManager", "fail IO错误 taskUrl=" + url);
                                return false;
                            }
                        }

                        private void save2File(URLConnection urlConnection, String saveFilePath) throws IOException {
                            DataInputStream dis = null;
                            FileOutputStream fos = null;
                            if (Thread.currentThread().isInterrupted()) {
                                return;
                            }
                            try {
                                dis = new DataInputStream(urlConnection.getInputStream());
                                //建立一个新的文件
                                fos = new FileOutputStream(new File(saveFilePath));
                                byte[] buffer = new byte[1024];
                                int length;
                                //开始填充数据
                                while ((length = dis.read(buffer)) > 0) {
                                    if (Thread.currentThread().isInterrupted()) {
                                        Log.d("DownloadManager", "thread (" + downloadTask.getTaskId() + ") save2File :return early");
                                        return;
                                    }
                                    downloadTask.getLastDurationDownloadSize().addAndGet(length);
                                    downloadTask.getTotalDownloaded().addAndGet(length);
                                    fos.write(buffer, 0, length);
                                }
                            } finally {
                                if (dis != null) {
                                    dis.close();
                                }
                                if (fos != null) {
                                    fos.close();
                                }
                            }
                        }
                    });

                    workerThread.add(thread);
                    thread.start();
                }
                try {
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
                if (isWorkerThreadFailed) {
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
                taskFinished(downloadTask.getTaskId(), DownloadStatusEnum.SUCCESS.getCode());
            } catch (Exception e) {
                System.out.println(Thread.currentThread().getName() + " (" + this.getState() + ") catch InterruptedException.");
                interrupt();
            }
        }


        private void parseM3u8(String m3u8Url, String newM3u8FileName, String relativePath, String outputPath, LinkedBlockingQueue<Map<String, String>> sizeDetectQueue, LinkedBlockingQueue<Map<String, String>> downloadQueue) throws IOException {
            String m3U8Content = HttpRequestUtil.getResponseString(HttpRequestUtil.sendGetRequest(m3u8Url));
            String newM3u8FileContent = "";
            boolean subFile = false;
            for (String lineStr : m3U8Content.split("\n")) {
                if (lineStr.startsWith("#")) {
                    if (lineStr.startsWith("#EXT-X-KEY:")) {
                        Matcher searchKeyUri = Pattern.compile("URI=\"(.*?)\"").matcher(lineStr);
                        if (!searchKeyUri.find()) {
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
                        downloadQueue.add(hashMap);
                        String newLineStr = Pattern.compile("URI=\"(.*?)\"").matcher(lineStr).replaceAll("URI=\"" + "/" + uuidStr + ".key\"");
                        lineStr = newLineStr;
                    }
                    if (lineStr.startsWith("#EXT-X-STREAM-INF")) {
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
                        parseM3u8(fileUrl, uuidStr + ".m3u8", relativePath, outputPath, sizeDetectQueue, downloadQueue);
                        newM3u8FileContent = newM3u8FileContent + "/" + uuidStr + ".m3u8\n";
//                        newM3u8FileContent = newM3u8FileContent + "/" + relativePath + "/" + uuidStr + ".m3u8\n";
                    } else {
                        String videoFilePath = outputPath + File.separator + uuidStr + ".ts";
                        HashMap<String, String> hashMap = new HashMap<String, String>();
                        hashMap.put("url", fileUrl);
                        hashMap.put("downloadPath", videoFilePath);
                        sizeDetectQueue.add(hashMap);
                        downloadQueue.add(hashMap);
                        newM3u8FileContent = newM3u8FileContent + "/" + uuidStr + ".ts\n";
//                        newM3u8FileContent = newM3u8FileContent + "/" + relativePath + "/" + uuidStr + ".ts\n";
                    }
                }
            }
            FileUtil.stringToFile(newM3u8FileContent, outputPath + File.separator + newM3u8FileName);
        }
    }


    class NormalFileDownloadTaskThread extends DownloadThread {
        private DownloadTask downloadTask;
        private int splitNum = 0;
        private LinkedBlockingQueue<Map<String, String>> downloadQueue = new LinkedBlockingQueue<Map<String, String>>();
        private List<Thread> workerThread = new ArrayList<Thread>(DownloadConfig.normalFileDownloadThreadNum);
        private boolean isWorkerThreadFailed = false;
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

                String finalDir = getDownloadDir(downloadTask.getFileName());
                File finalDirFile = new File(finalDir);


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
//            if(!makeDirsSuccess){
//                downloadTask.setStatus(DownloadStatusEnum.ERROR.getCode());
//                downloadTask.setFailedReason("目录创建失败");
//                taskFailed(downloadTask);
//                return;
//            }


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

                boolean isFileSupportRange = false;
                int failCount = 0;
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        isFileSupportRange = detectFileSupportRange(downloadTask.getUrl());
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
                if (!isFileSupportRange) {
                    try {
                        save2File(HttpRequestUtil.sendGetRequest(downloadTask.getUrl()), downloadTempFilePath);
                    } catch (IOException e) {
                        e.printStackTrace();
                        downloadTask.setStatus(DownloadStatusEnum.ERROR.getCode());
                        downloadTask.setFailedReason("文件下载失败");
                        taskFailed(downloadTask);
                        return;
                    }
                    speedCheckerThread.interrupt();
                } else {
                    long totalSize = downloadTask.getSize().get();
                    long normalFileSplitSize = DownloadConfig.normalFileSplitSize;
                    int n = 0;
                    while (n * normalFileSplitSize < totalSize) {
                        if (Thread.currentThread().isInterrupted()) {
                            Log.d("DownloadManager", "thread (" + downloadTask.getTaskId() + ") split file :return early");
                            return;
                        }
                        HashMap<String, String> hashMap = new HashMap<String, String>();
                        hashMap.put("url", downloadTask.getUrl());
                        hashMap.put("rangeHeader", "bytes=" + String.valueOf(n * normalFileSplitSize) + "-" + String.valueOf((n + 1) * normalFileSplitSize - 1));
                        hashMap.put("downloadPath", downloadTempDir + File.separator + downloadTask.getFileName() + "." + String.valueOf(n));
                        downloadQueue.add(hashMap);
                        n++;
                    }


                    for (int i = 0; i < DownloadConfig.normalFileDownloadThreadNum; i++) {
                        Thread thread = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                Log.d("DownloadManager", "thread (" + downloadTask.getTaskId() + ") :start");
                                try {
                                    while (!Thread.currentThread().isInterrupted()) {
                                        Map<String, String> taskMap;
                                        try {
                                            taskMap = downloadQueue.remove();
                                        } catch (NoSuchElementException e) {
                                            Log.d("DownloadManager", "thread (" + downloadTask.getTaskId() + ") :exited");
                                            break;
                                        }
                                        String taskUrl = taskMap.get("url");
                                        String rangeHeader = taskMap.get("rangeHeader");
                                        String downloadPath = taskMap.get("downloadPath");
                                        if (Thread.currentThread().isInterrupted()) {
                                            Log.d("DownloadManager", "download thread (" + downloadTask.getTaskId() + ") :return early");
                                            return;
                                        }
                                        Log.d("DownloadManager", "start download taskUrl=" + taskUrl);
                                        int failCount = 0;
                                        while (!Thread.currentThread().isInterrupted() && !downloadFile(taskUrl, rangeHeader, downloadPath)) {
                                            //如果检测失败
                                            failCount++;
                                            if (failCount >= DownloadConfig.downloadSubFileRetryCountOnFail) {
                                                isWorkerThreadFailed = true;
                                                return;
                                            }
                                        }
                                    }
                                } catch (Exception e) {
                                    Log.w(TAG, "run: " + e.getMessage());
                                    interrupt();
                                }
                                Log.d("DownloadManager", "thread (" + downloadTask.getTaskId() + ") :interrupted");
                            }

                            private boolean downloadFile(String url, String rangeHeader, String downloadPath) {
                                downloadWorkThreadCheckLock.lock();
                                Log.d(TAG, "downloadFile: " + canceledTask.size());
                                if (Thread.currentThread().isInterrupted() || canceledTask.contains(downloadTask.getTaskId())) {
                                    downloadWorkThreadCheckLock.unlock();
                                    return false;
                                }
                                downloadWorkThreadCheckLock.unlock();
                                try {
                                    HashMap<String, String> hashMap = new HashMap<String, String>();
                                    hashMap.put("Range", rangeHeader);
                                    save2File(HttpRequestUtil.sendGetRequest(url, null, hashMap), downloadPath);
                                    return true;
                                } catch (IOException e) {
                                    e.printStackTrace();
                                    Log.d("DownloadManager", "fail IO错误 taskUrl=" + url);
                                    return false;
                                }
                            }

                            private void save2File(URLConnection urlConnection, String saveFilePath) throws IOException {
                                if (Thread.currentThread().isInterrupted()) {
                                    return;
                                }
                                DataInputStream dis = null;
                                FileOutputStream fos = null;

                                try {
                                    dis = new DataInputStream(urlConnection.getInputStream());
                                    //建立一个新的文件
                                    fos = new FileOutputStream(new File(saveFilePath));
                                    byte[] buffer = new byte[1024];
                                    int length;
                                    //开始填充数据
                                    while ((length = dis.read(buffer)) > 0) {
                                        if (Thread.currentThread().isInterrupted()) {
                                            Log.d("DownloadManager", "thread (" + downloadTask.getTaskId() + ") save2File :return early");
                                            return;
                                        }
                                        downloadTask.getLastDurationDownloadSize().addAndGet(length);
                                        downloadTask.getTotalDownloaded().addAndGet(length);
                                        fos.write(buffer, 0, length);
                                    }
                                } finally {
                                    if (dis != null) {
                                        dis.close();
                                    }
                                    if (fos != null) {
                                        fos.close();
                                    }
                                    ((HttpURLConnection) urlConnection).disconnect();
                                }
                            }
                        });

                        workerThread.add(thread);
                        thread.start();
                    }
                    try {
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
                    if (isWorkerThreadFailed) {
                        downloadTask.setStatus(DownloadStatusEnum.ERROR.getCode());
                        downloadTask.setFailedReason("下载失败:1");
                        taskFailed(downloadTask);
                        return;
                    }
                    downloadTask.setStatus(DownloadStatusEnum.SAVING.getCode());
                    try {
                        FileOutputStream outputFileStream = null;
                        FileInputStream fromFileStream = null;
                        FileChannel fcout = null;
                        FileChannel fcin = null;
                        try {
                            File outputFile = downloadTempFile;
                            File fromFile;
                            ByteBuffer buffer = ByteBuffer.allocate(1024);
                            outputFileStream = new FileOutputStream(outputFile);
                            fcout = outputFileStream.getChannel();
                            for (int i = 0; i < n; i++) {
                                fromFile = new File(downloadTempDir + File.separator + downloadTask.getFileName() + "." + String.valueOf(i));
                                fromFileStream = new FileInputStream(fromFile);
                                fcin = fromFileStream.getChannel();
                                try {
                                    while (true) {
                                        if (Thread.currentThread().isInterrupted()) {
                                            Log.d("DownloadManager", "thread (" + downloadTask.getTaskId() + ") save2File :return early");
                                            return;
                                        }
                                        // clear方法重设缓冲区，使它可以接受读入的数据
                                        buffer.clear();
                                        // 从输入通道中将数据读到缓冲区
                                        int r = -1;
                                        r = fcin.read(buffer);
                                        // read方法返回读取的字节数，可能为零，如果该通道已到达流的末尾，则返回-1
                                        if (r == -1) {
                                            break;
                                        }
                                        // flip方法让缓冲区可以将新读入的数据写入另一个通道
                                        buffer.flip();
                                        // 从输出通道中将数据写入缓冲区
                                        fcout.write(buffer);
                                    }
                                } finally {
                                    if (fcin != null) {
                                        fcin.close();
                                    }
                                    fromFileStream.close();
                                }
                            }
                        } finally {
                            if (fcout != null) {
                                fcout.close();
                            }
                            if (outputFileStream != null) {
                                outputFileStream.close();
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

                downloadTempFile.renameTo(new File(finalDir + File.separator + "video." + downloadTask.getFileExtension()));

                String videoTitleFilePath = finalDir + File.separator + "videoTitle";
                String videoName = TextUtils.isEmpty(downloadTask.getSourcePageTitle()) ? downloadTask.getFileName() : downloadTask.getSourcePageTitle();
                try {
                    FileUtil.stringToFile(videoName, videoTitleFilePath);
                } catch (IOException e) {
                    e.printStackTrace();
                    downloadTask.setStatus(DownloadStatusEnum.ERROR.getCode());
                    downloadTask.setFailedReason("视频名称保存失败");
                    taskFailed(downloadTask);
                    return;
                }
                String normalVideoTypeFilePath = finalDir + File.separator + "normalVideoType";
                try {
                    FileUtil.stringToFile(downloadTask.getFileExtension(), normalVideoTypeFilePath);
                } catch (IOException e) {
                    e.printStackTrace();
                    downloadTask.setStatus(DownloadStatusEnum.ERROR.getCode());
                    downloadTask.setFailedReason("视频类型信息保存失败");
                    taskFailed(downloadTask);
                    return;
                }


                taskFinished(downloadTask.getTaskId(), DownloadStatusEnum.SUCCESS.getCode());
            } catch (Exception e) {
                System.out.println(Thread.currentThread().getName() + " (" + this.getState() + ") catch InterruptedException.");
                interrupt();
            }
        }


        private void save2File(URLConnection urlConnection, String saveFilePath) throws IOException {

            DataInputStream dis = null;
            FileOutputStream fos = null;

            try {
                dis = new DataInputStream(urlConnection.getInputStream());
                //建立一个新的文件
                fos = new FileOutputStream(new File(saveFilePath));
                byte[] buffer = new byte[1024];
                int length;
                //开始填充数据
                while (!Thread.currentThread().isInterrupted() && ((length = dis.read(buffer)) > 0)) {
                    downloadTask.getLastDurationDownloadSize().addAndGet(length);
                    downloadTask.getTotalDownloaded().addAndGet(length);
                    fos.write(buffer, 0, length);
                }
            } finally {
                if (dis != null) {
                    dis.close();
                }
                if (fos != null) {
                    fos.close();
                }
            }
        }

        private boolean detectFileSupportRange(String url) throws IOException {
            HttpRequestUtil.HeadRequestResponse headRequestResponse = HttpRequestUtil.performHeadRequest(url);
            Map<String, List<String>> headerMap = headRequestResponse.getHeaderMap();
            if (headerMap == null) {
                //检测失败，未找到Content-Type
                Log.d("DownloadManager", "fail 未找到Content-Length taskUrl=" + url);
                throw new IOException("headerMap is null");
            }
            return headerMap.containsKey("Accept-Ranges") && headerMap.get("Accept-Ranges").size() > 0 && "bytes".equals(headerMap.get("Accept-Ranges").get(0).trim());
        }
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


}

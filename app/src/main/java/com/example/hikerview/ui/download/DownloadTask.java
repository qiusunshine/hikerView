package com.example.hikerview.ui.download;

import com.example.hikerview.service.parser.HttpParser;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by xm on 17/8/19.
 */
public class DownloadTask {
    private String taskId;//任务id UUID
    private String fileName; //文件名(不含扩展名)
    private String videoType; //m3u8 or normal
    private String fileExtension; //文件扩展名
    private String url;//下载地址

    private String sourcePageUrl;
    private String sourcePageTitle;

    private AtomicLong size = new AtomicLong(0);

    private String status = "ready";//状态 ready/loading/running/saving/error/success/cancel/break
    private String failedReason = ""; //错误原因
    private AtomicLong totalDownloaded = new AtomicLong(0);
    private long currentSpeed;//当前速度
    private long lastClearSpeedTime;//最后一次重置lastDurationDownloadSize的时间, 用于计算瞬时速度
    private AtomicLong lastDurationDownloadSize = new AtomicLong(0);//由WorkThread累加，由清零线程消除，清零线程计算速度放到currentSpeed并更新lastClearSpeedTime
    private String film;
    /**
     * 字幕地址
     */
    private String subtitle;

    private String rootPath;

    private Map<String, String> headers;

    private String downloadUrl;

    /**
     * 断点续传
     */
    private boolean continueDownload;
    /**
     * 文件后缀
     */
    private String suffix;

    private String contentType;
    private String originalTitle;

    private boolean ignoreError;

    private int downloadThread;

    public DownloadTask(String taskId, String fileName, String videoType, String fileExtension, String url, String sourcePageUrl, String sourcePageTitle, Long size) {
        this.taskId = taskId;
        this.fileName = fileName;
        this.videoType = videoType;
        this.fileExtension = fileExtension;
        setUrl(url);
        this.sourcePageUrl = sourcePageUrl;
        this.sourcePageTitle = sourcePageTitle;
        this.size.set(size);
        this.rootPath = DownloadConfig.rootPath;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getVideoType() {
        return videoType;
    }

    public void setVideoType(String videoType) {
        this.videoType = videoType;
    }

    public String getFileExtension() {
        return fileExtension;
    }

    public void setFileExtension(String fileExtension) {
        this.fileExtension = fileExtension;
    }

    public String getSourcePageUrl() {
        return sourcePageUrl;
    }

    public void setSourcePageUrl(String sourcePageUrl) {
        this.sourcePageUrl = sourcePageUrl;
    }

    public String getSourcePageTitle() {
        return sourcePageTitle;
    }

    public void setSourcePageTitle(String sourcePageTitle) {
        this.sourcePageTitle = sourcePageTitle;
    }

    public long getCurrentSpeed() {
        return currentSpeed;
    }

    public void setCurrentSpeed(long currentSpeed) {
        this.currentSpeed = currentSpeed;
    }

    public long getLastClearSpeedTime() {
        return lastClearSpeedTime;
    }

    public void setLastClearSpeedTime(long lastClearSpeedTime) {
        this.lastClearSpeedTime = lastClearSpeedTime;
    }

    public AtomicLong getLastDurationDownloadSize() {
        return lastDurationDownloadSize;
    }

    public void setLastDurationDownloadSize(AtomicLong lastDurationDownloadSize) {
        this.lastDurationDownloadSize = lastDurationDownloadSize;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getFailedReason() {
        return failedReason;
    }

    public void setFailedReason(String failedReason) {
        this.failedReason = failedReason;
    }

    public AtomicLong getSize() {
        return size;
    }

    public void setSize(AtomicLong size) {
        this.size = size;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
        this.headers = HttpParser.getHeaders(url);
        this.downloadUrl = HttpParser.getThirdDownloadSource(url);
    }

    public AtomicLong getTotalDownloaded() {
        return totalDownloaded;
    }

    public void setTotalDownloaded(AtomicLong totalDownloaded) {
        this.totalDownloaded = totalDownloaded;
    }

    public String getFilm() {
        return film;
    }

    public void setFilm(String film) {
        this.film = film;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
    }

    public String getRootPath() {
        return rootPath;
    }

    public void setRootPath(String rootPath) {
        this.rootPath = rootPath;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    public boolean isContinueDownload() {
        return continueDownload;
    }

    public void setContinueDownload(boolean continueDownload) {
        this.continueDownload = continueDownload;
    }

    public String getSuffix() {
        return suffix;
    }

    public void setSuffix(String suffix) {
        this.suffix = suffix;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getOriginalTitle() {
        return originalTitle;
    }

    public void setOriginalTitle(String originalTitle) {
        this.originalTitle = originalTitle;
    }

    public boolean isIgnoreError() {
        return ignoreError;
    }

    public void setIgnoreError(boolean ignoreError) {
        this.ignoreError = ignoreError;
    }

    public int getDownloadThread() {
        return downloadThread;
    }

    public void setDownloadThread(int downloadThread) {
        this.downloadThread = downloadThread;
    }
}

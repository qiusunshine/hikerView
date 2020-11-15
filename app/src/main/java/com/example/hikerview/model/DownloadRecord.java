package com.example.hikerview.model;

import com.example.hikerview.ui.download.DownloadTask;

import org.litepal.annotation.Column;
import org.litepal.crud.LitePalSupport;

/**
 * 作者：By 15968
 * 日期：On 2019/12/5
 * 时间：At 21:57
 */
public class DownloadRecord extends LitePalSupport {
    private String taskId;//任务id UUID
    private String fileName; //文件名(不含扩展名)
    private String videoType; //m3u8 or normal
    private String fileExtension; //文件扩展名
    private String url;//下载地址
    private String sourcePageUrl;
    private String sourcePageTitle;
    private long size;
    private String status;//状态 ready/loading/running/saving/error/success/cancel/break
    private String failedReason = ""; //错误原因
    private long totalDownloaded;
    private long currentSpeed;//当前速度
    private long lastClearSpeedTime;//最后一次重置lastDurationDownloadSize的时间, 用于计算瞬时速度
    private long saveTime;
    private long finishedTime;
    private int order;
    @Column(ignore = true)
    private boolean selected;

    public DownloadRecord(DownloadTask downloadTask) {
        this.taskId = downloadTask.getTaskId();
        this.fileName = downloadTask.getFileName();
        this.videoType = downloadTask.getVideoType();
        this.fileExtension = downloadTask.getFileExtension();
        this.url = downloadTask.getUrl();
        this.sourcePageUrl = downloadTask.getSourcePageUrl();
        this.sourcePageTitle = downloadTask.getSourcePageTitle();
        this.size = downloadTask.getSize().get();
        this.status = downloadTask.getStatus();
        this.failedReason = downloadTask.getFailedReason();
        this.totalDownloaded = downloadTask.getTotalDownloaded().get();
        this.currentSpeed = downloadTask.getCurrentSpeed();
        this.lastClearSpeedTime = downloadTask.getLastClearSpeedTime();
        this.saveTime = System.currentTimeMillis();
    }

    public DownloadRecord update(DownloadTask downloadTask) {
        this.taskId = downloadTask.getTaskId();
        this.fileName = downloadTask.getFileName();
        this.videoType = downloadTask.getVideoType();
        this.fileExtension = downloadTask.getFileExtension();
        this.url = downloadTask.getUrl();
        this.sourcePageUrl = downloadTask.getSourcePageUrl();
        this.sourcePageTitle = downloadTask.getSourcePageTitle();
        this.size = downloadTask.getSize().get();
        this.status = downloadTask.getStatus();
        this.failedReason = downloadTask.getFailedReason();
        this.totalDownloaded = downloadTask.getTotalDownloaded().get();
        this.currentSpeed = downloadTask.getCurrentSpeed();
        this.lastClearSpeedTime = downloadTask.getLastClearSpeedTime();
        return this;
    }

    public DownloadRecord() {
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

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
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

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
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

    public long getTotalDownloaded() {
        return totalDownloaded;
    }

    public void setTotalDownloaded(long totalDownloaded) {
        this.totalDownloaded = totalDownloaded;
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

    public long getId() {
        return getBaseObjId();
    }

    public long getSaveTime() {
        return saveTime;
    }

    public void setSaveTime(long saveTime) {
        this.saveTime = saveTime;
    }

    public long getFinishedTime() {
        return finishedTime;
    }

    public void setFinishedTime(long finishedTime) {
        this.finishedTime = finishedTime;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }
}

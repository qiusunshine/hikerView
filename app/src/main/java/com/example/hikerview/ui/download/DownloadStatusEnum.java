package com.example.hikerview.ui.download;

/**
 * 作者：By 15968
 * 日期：On 2021/1/3
 * 时间：At 12:01
 */

public enum DownloadStatusEnum {
    CHECKING("checking", "格式解析中"),
    READY("ready", "队列中"),
    LOADING("loading", "计算文件大小"),
    RUNNING("running", "下载中"),
    BREAK("break", "下载暂停"),
    SAVING("saving", "保存中"),
    MERGING("merging", "合并中"),
    CANCEL("cancel", "下载取消"),
    SUCCESS("success", "下载完成"),
    ERROR("error", "下载失败"),
    UNKNOWN("unknown", "未知状态"),
    DELETED("deleted", "已删除");

    DownloadStatusEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    private final String code;
    private final String desc;

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static DownloadStatusEnum getByCode(String code) {
        for (DownloadStatusEnum value : values()) {
            if (value.getCode().equals(code)) {
                return value;
            }
        }
        return UNKNOWN;
    }
}

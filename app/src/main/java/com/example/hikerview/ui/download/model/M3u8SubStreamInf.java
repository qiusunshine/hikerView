package com.example.hikerview.ui.download.model;

/**
 * 作者：By 15968
 * 日期：On 2023/10/8
 * 时间：At 20:43
 */

public class M3u8SubStreamInf {
    private String path;
    private String bandwidth;
    private String resolution;
    private String programId;
    private String codecs;
    private String audio;
    private String subtitles;
    private String frameRate;

    public String getDescription() {
        return "分辨率: " + resolution + ", 带宽: " + bandwidth
                + (codecs != null ? ", 编码: " + codecs : "")
                + (audio != null ? ", audio: " + audio : "")
                + (subtitles != null ? ", subtitles: " + subtitles : "");
    }

    public M3u8SubStreamInf() {
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getBandwidth() {
        return bandwidth;
    }

    public void setBandwidth(String bandwidth) {
        this.bandwidth = bandwidth;
    }

    public String getResolution() {
        return resolution;
    }

    public void setResolution(String resolution) {
        this.resolution = resolution;
    }

    public String getProgramId() {
        return programId;
    }

    public void setProgramId(String programId) {
        this.programId = programId;
    }

    public String getCodecs() {
        return codecs;
    }

    public void setCodecs(String codecs) {
        this.codecs = codecs;
    }

    public String getAudio() {
        return audio;
    }

    public void setAudio(String audio) {
        this.audio = audio;
    }

    public String getSubtitles() {
        return subtitles;
    }

    public void setSubtitles(String subtitles) {
        this.subtitles = subtitles;
    }

    public String getFrameRate() {
        return frameRate;
    }

    public void setFrameRate(String frameRate) {
        this.frameRate = frameRate;
    }
}
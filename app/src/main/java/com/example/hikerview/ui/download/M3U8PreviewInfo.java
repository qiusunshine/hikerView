package com.example.hikerview.ui.download;

/**
 * Created by xm on 17/8/17.
 */
public class M3U8PreviewInfo {
    private int length;
    private int size;
    private int bandwidthNumber;

    public M3U8PreviewInfo(int length, int size, int bandwidthNumber) {
        this.length = length;
        this.size = size;
        this.bandwidthNumber = bandwidthNumber;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public int getBandwidthNumber() {
        return bandwidthNumber;
    }

    public void setBandwidthNumber(int bandwidthNumber) {
        this.bandwidthNumber = bandwidthNumber;
    }
}

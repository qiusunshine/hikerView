package com.example.hikerview.model;

import org.litepal.crud.LitePalSupport;

/**
 * 作者：By hdy
 * 日期：On 2018/12/6
 * 时间：At 21:58
 */
public class PlayerPosHis extends LitePalSupport {

    private String playUrl;
    private int pos;

    public String getPlayUrl() {
        return playUrl;
    }

    public void setPlayUrl(String playUrl) {
        this.playUrl = playUrl;
    }


    public PlayerPosHis() {
    }

    public PlayerPosHis(String playUrl, int pos) {
        this.playUrl = playUrl;
        this.pos = pos;
    }


    public int getPos() {
        return pos;
    }

    public void setPos(int pos) {
        this.pos = pos;
    }
}

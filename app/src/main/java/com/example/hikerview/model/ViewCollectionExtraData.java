package com.example.hikerview.model;

import com.alibaba.fastjson.JSON;

/**
 * @author reborn
 * @program hiker-view
 * @description 收藏的额外数据
 * @create 2020-12-29 18:59
 **/
public class ViewCollectionExtraData {

    private long collectionId;
    private boolean isCustomJump;
    private int jumpStartDuration, jumpEndDuration;
    private boolean isCustomPlayer;
    private int player;
    private int pageIndex;

    public long getCollectionId() {
        return collectionId;
    }

    public void setCollectionId(long collectionId) {
        this.collectionId = collectionId;
    }

    public boolean isCustomJump() {
        return isCustomJump;
    }

    public void setCustomJump(boolean customJump) {
        isCustomJump = customJump;
    }

    public boolean isCustomPlayer() {
        return isCustomPlayer;
    }

    public void setCustomPlayer(boolean customPlayer) {
        isCustomPlayer = customPlayer;
    }

    public int getPlayer() {
        return player;
    }

    public void setPlayer(int player) {
        this.player = player;
    }

    public int getJumpStartDuration() {
        return jumpStartDuration;
    }

    public void setJumpStartDuration(int jumpStartDuration) {
        this.jumpStartDuration = jumpStartDuration;
    }

    public int getJumpEndDuration() {
        return jumpEndDuration;
    }

    public void setJumpEndDuration(int jumpEndDuration) {
        this.jumpEndDuration = jumpEndDuration;
    }

    public static String extraDataToJson(ViewCollectionExtraData extraData) {
        return JSON.toJSONString(extraData);
    }

    public static ViewCollectionExtraData extraDataFromJson(String extraDataString) {
        return JSON.parseObject(extraDataString, ViewCollectionExtraData.class);
    }

    public int getPageIndex() {
        return pageIndex;
    }

    public void setPageIndex(int pageIndex) {
        this.pageIndex = pageIndex;
    }
}

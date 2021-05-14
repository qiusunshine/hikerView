package com.example.hikerview.event;

import com.example.hikerview.ui.setting.model.SettingConfig;

/**
 * 作者：By 15968
 * 日期：On 2019/10/5
 * 时间：At 16:19
 */
public class OnArticleListRuleChangedEvent {
    public OnArticleListRuleChangedEvent() {
        SettingConfig.nowLoadRulesTime = System.currentTimeMillis();
    }

    public OnArticleListRuleChangedEvent(int updateFromSub) {
        SettingConfig.nowLoadRulesTime = System.currentTimeMillis();
        this.updateFromSub = updateFromSub;
    }

    private String fromClazz;

    private int updateFromSub;

    public String getFromClazz() {
        return fromClazz;
    }

    public void setFromClazz(String fromClazz) {
        this.fromClazz = fromClazz;
    }

    public int getUpdateFromSub() {
        return updateFromSub;
    }

    public void setUpdateFromSub(int updateFromSub) {
        this.updateFromSub = updateFromSub;
    }
}

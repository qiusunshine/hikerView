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

    private String fromClazz;

    public String getFromClazz() {
        return fromClazz;
    }

    public void setFromClazz(String fromClazz) {
        this.fromClazz = fromClazz;
    }
}

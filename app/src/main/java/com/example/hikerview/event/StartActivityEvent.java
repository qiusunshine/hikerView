package com.example.hikerview.event;

import android.content.Intent;

/**
 * 作者：By 15968
 * 日期：On 2020/2/5
 * 时间：At 14:02
 */
public class StartActivityEvent {
    private Intent intent;

    public StartActivityEvent() {
    }

    public StartActivityEvent(Intent intent) {
        this.intent = intent;
    }

    public Intent getIntent() {
        return intent;
    }

    public void setIntent(Intent intent) {
        this.intent = intent;
    }
}

package com.example.hikerview.event;

import android.content.Context;
import android.content.Intent;

/**
 * 作者：By 15968
 * 日期：On 2020/12/27
 * 时间：At 10:13
 */

public class PlaySourceUpdateEvent {
    public PlaySourceUpdateEvent(Context context, Intent intent) {
        this.intent = intent;
        this.context = context;
    }

    private Intent intent;
    private Context context;

    public Intent getIntent() {
        return intent;
    }

    public void setIntent(Intent intent) {
        this.intent = intent;
    }

    public Context getContext() {
        return context;
    }

    public void setContext(Context context) {
        this.context = context;
    }
}

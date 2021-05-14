package com.example.hikerview.ui.dlan;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import com.example.hikerview.R;
import com.example.hikerview.ui.Application;

/**
 * 作者：By 15968
 * 日期：On 2019/12/4
 * 时间：At 23:01
 */
public class DlanForegroundService extends Service {
    private static final int ONGOING_NOTIFICATION_ID = 2;

    public DlanForegroundService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        String channelId = "海阔世界本地";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            channelId = createNotificationChannel();
        }

        Notification notification = new NotificationCompat.Builder(Application.application, channelId)
                .setContentTitle("海阔世界·本地视频投放")
                .setContentText("请勿清理后台，否则可能导致视频播放卡顿")
                .setSmallIcon(R.mipmap.ic_dlan)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_dlan)).build();
        startForeground(ONGOING_NOTIFICATION_ID, notification);
    }


    @RequiresApi(Build.VERSION_CODES.O)
    private String createNotificationChannel(){
        String channelId = "海阔世界本地";
        String channelName = "前台投屏通知";
        NotificationChannel chan = new NotificationChannel(channelId,
                channelName, NotificationManager.IMPORTANCE_HIGH);
        chan.setLightColor(Color.BLUE);
        chan.setImportance(NotificationManager.IMPORTANCE_NONE);
        chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        NotificationManager service = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        if (service != null) {
            service.createNotificationChannel(chan);
        }
        return channelId;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}

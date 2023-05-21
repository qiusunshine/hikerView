package com.example.hikerview.ui.thunder;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
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
import com.example.hikerview.ui.video.EmptyActivity;

/**
 * 作者：By 15968
 * 日期：On 2019/12/4
 * 时间：At 23:01
 */
public class MagentForegroundService extends Service {
    private static final int ONGOING_NOTIFICATION_ID = 10;
    private NotificationCompat.Builder notification;

    public MagentForegroundService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        String channelId = "";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            channelId = createNotificationChannel();
        }

        Intent intent = new Intent(this, EmptyActivity.class);
        intent.putExtra("magnetStatus", true);
        notification = new NotificationCompat.Builder(Application.application, channelId)
                .setOnlyAlertOnce(true)
                .setOngoing(true)
                .setContentTitle("海阔视界·磁力引擎")
                .setContentText("磁力引擎运行中")
                .setSmallIcon(R.drawable.ic_stat_download)
                .setContentIntent(PendingIntent.getActivity(this, 1, intent,
                        PendingIntent.FLAG_UPDATE_CURRENT))
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.download));
        startForeground(ONGOING_NOTIFICATION_ID, notification.build());
    }


    @RequiresApi(Build.VERSION_CODES.O)
    private String createNotificationChannel() {
        String channelId = "海阔视界";
        String channelName = "磁力引擎状态通知";
        NotificationChannel chan = new NotificationChannel(channelId,
                channelName, NotificationManager.IMPORTANCE_HIGH);
        chan.setLightColor(Color.BLUE);
        chan.setImportance(NotificationManager.IMPORTANCE_HIGH);
        chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        NotificationManager service = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
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

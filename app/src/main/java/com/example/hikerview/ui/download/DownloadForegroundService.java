package com.example.hikerview.ui.download;

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
import com.example.hikerview.ui.download.model.ProgressEvent;
import com.example.hikerview.utils.StringUtil;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

/**
 * 作者：By 15968
 * 日期：On 2019/12/4
 * 时间：At 23:01
 */
public class DownloadForegroundService extends Service {
    private static final int ONGOING_NOTIFICATION_ID = 1;
    private NotificationCompat.Builder notification;

    public DownloadForegroundService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }
        String channelId = "";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            channelId = createNotificationChannel();
        }

        notification = new NotificationCompat.Builder(Application.application, channelId)
                .setOnlyAlertOnce(true)
                .setOngoing(true)
                .setSound(null)
                .setContentTitle("海阔视界·正在下载")
                .setContentText("请勿清理后台，否则下载会中断")
                .setSmallIcon(R.drawable.ic_stat_download)
                .setContentIntent(PendingIntent.getActivity(this, 1, new Intent(this, DownloadRecordsActivity.class),
                        PendingIntent.FLAG_UPDATE_CURRENT))
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.download));
        startForeground(ONGOING_NOTIFICATION_ID, notification.build());
    }


    @RequiresApi(Build.VERSION_CODES.O)
    private String createNotificationChannel() {
        String channelId = "海阔视界";
        String channelName = "前台下载通知";
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

    /**
     * 更改通知的信息和UI
     */
    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void updateNotificationShow(ProgressEvent event) {
        //发送通知
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            if (StringUtil.isNotEmpty(event.getName())) {
                notification.setContentText(event.getProgress() + " " + event.getName());
            } else {
                notification.setContentText("请勿清理后台，否则下载会中断");
            }
            manager.notify(ONGOING_NOTIFICATION_ID, notification.build());
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this);
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}

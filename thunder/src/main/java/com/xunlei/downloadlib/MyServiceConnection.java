package com.xunlei.downloadlib;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;

public class MyServiceConnection implements ServiceConnection {
    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {//服务已连接
//            Log.d("yw", componentName.toString());
//            myBinder = (IMyBinder) iBinder;
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {//服务已断开

    }
}

package com.example.hikerview.utils;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import com.example.hikerview.event.video.OnDeviceUpdateEvent;
import com.example.hikerview.ui.Application;
import com.qingfeng.clinglibrary.entity.ClingDevice;
import com.qingfeng.clinglibrary.entity.IDevice;
import com.qingfeng.clinglibrary.listener.BrowseRegistryListener;
import com.qingfeng.clinglibrary.listener.DeviceListChangedListener;
import com.qingfeng.clinglibrary.service.ClingUpnpService;
import com.qingfeng.clinglibrary.service.manager.ClingManager;
import com.qingfeng.clinglibrary.service.manager.DeviceManager;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.List;

/**
 * 作者：By 15968
 * 日期：On 2019/12/1
 * 时间：At 18:03
 */
public class DlanListPopUtil {
    private static final String TAG = "DlanListPopUtil";
    private volatile static DlanListPopUtil sInstance;
    private static boolean hasBind = false;
    private ClingDevice usedDevice;

    private DlanListPopUtil() {
    }

    public static DlanListPopUtil instance() {
        if (sInstance == null) {
            synchronized (DlanListPopUtil.class) {
                if (sInstance == null) {
                    sInstance = new DlanListPopUtil();
                }
            }
        }
        return sInstance;
    }

    private ServiceConnection mUpnpServiceConnection;
    private BrowseRegistryListener registryListener = new BrowseRegistryListener();
    private ClingUpnpService beyondUpnpService;
    private ClingManager clingUpnpServiceManager;
    private List<ClingDevice> list = new ArrayList<>();

    public List<ClingDevice> getDevices() {
        return list;
    }

    private void init() {
        list.clear();
        usedDevice = null;
        registryListener.setOnDeviceListChangedListener(new DeviceListChangedListener() {
            @Override
            public void onDeviceAdded(final IDevice device) {
                list.add((ClingDevice) device);
                if (EventBus.getDefault().hasSubscriberForEvent(OnDeviceUpdateEvent.class)) {
                    EventBus.getDefault().post(new OnDeviceUpdateEvent());
                }
                Log.d(TAG, "onDeviceAdded: ");
            }

            @Override
            public void onDeviceRemoved(final IDevice device) {
                list.remove(device);
                if (usedDevice == device) {
                    usedDevice = null;
                }
                if (EventBus.getDefault().hasSubscriberForEvent(OnDeviceUpdateEvent.class)) {
                    EventBus.getDefault().post(new OnDeviceUpdateEvent());
                }
                Log.d(TAG, "onDeviceRemoved: ");
            }
        });
        initAndRefresh(Application.application.getHomeActivity());
    }

    public void reInit() {
        unBind(Application.application.getHomeActivity());
        init();
    }

    private void initAndRefresh(Context context) {
        Log.d(TAG, "initAndRefresh: ");
        mUpnpServiceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName className, IBinder service) {
                Log.d(TAG, "onServiceConnected: ");
                hasBind = true;
                ClingUpnpService.LocalBinder binder = (ClingUpnpService.LocalBinder) service;
                beyondUpnpService = binder.getService();

                clingUpnpServiceManager = ClingManager.getInstance();
                clingUpnpServiceManager.setUpnpService(beyondUpnpService);
                clingUpnpServiceManager.setDeviceManager(new DeviceManager());

                clingUpnpServiceManager.getRegistry().addListener(registryListener);
                //Search on service created.
                clingUpnpServiceManager.searchDevices();
            }

            @Override
            public void onServiceDisconnected(ComponentName className) {
                mUpnpServiceConnection = null;
                Log.d(TAG, "onServiceDisconnected: ");
            }
        };
        Intent upnpServiceIntent = new Intent(context, ClingUpnpService.class);
        context.bindService(upnpServiceIntent, mUpnpServiceConnection, Context.BIND_AUTO_CREATE);
    }

    public void unBind(Context context) {
        if (mUpnpServiceConnection != null && hasBind) {
            try {
                context.unbindService(mUpnpServiceConnection);
            } catch (Exception e) {
                Log.e(TAG, "unBind: " + e.getMessage(), e);
            }
        }
    }

    public ClingDevice getUsedDevice() {
        return usedDevice;
    }

    public void setUsedDevice(ClingDevice usedDevice) {
        this.usedDevice = usedDevice;
    }
}

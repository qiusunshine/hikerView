package com.example.hikerview.ui.webdlan;

import android.content.Context;
import android.util.Log;

import com.example.hikerview.ui.Application;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.locks.ReentrantLock;

import fi.iki.elonen.SimpleWebServer;

/**
 * Created by xm on 17-8-21.
 */
public class WebServerManager {
    private static final String TAG = "WebServerManager";
    private SimpleWebServer simpleWebServer;
    private volatile String rootPathName;
    private volatile static WebServerManager sInstance;
    private ReentrantLock reentrantLock = new ReentrantLock();

    public static WebServerManager instance() {
        if (sInstance == null) {
            synchronized (WebServerManager.class) {
                if (sInstance == null) {
                    sInstance = new WebServerManager();
                }
            }
        }
        return sInstance;
    }

    private WebServerManager() {

    }

    public void startServer(Context context, String rootPath) {
        Application.application.startDlanForegroundService();
        Log.d(TAG, "startServer: " + rootPath);
        startServer(11111, rootPath);
    }

    public String getRootPathName() {
        return rootPathName;
    }

    public void startServer(int port, String rootPath) {
        reentrantLock.lock();
        try {
            stopServer();
            File file = new File(rootPath);
            Log.d(TAG, "startServer: root.exist=>" + file.exists() + ", isDir=>" + file.isDirectory());
            simpleWebServer = new SimpleWebServer("0.0.0.0", port, file, true);
            this.rootPathName = file.getName();
            try {
                simpleWebServer.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } finally {
            reentrantLock.unlock();
        }
    }

    public void stopServer() {
        try {
            simpleWebServer.stop();
        } catch (Exception e) {
        }
    }

    public SimpleWebServer getSimpleWebServer() {
        return simpleWebServer;
    }

    public void setSimpleWebServer(SimpleWebServer simpleWebServer) {
        this.simpleWebServer = simpleWebServer;
    }
}

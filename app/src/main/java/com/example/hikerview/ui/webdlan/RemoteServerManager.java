package com.example.hikerview.ui.webdlan;

import android.content.Context;
import android.text.TextUtils;

import com.example.hikerview.ui.webdlan.model.DlanUrlDTO;
import com.yanzhenjie.andserver.AndServer;
import com.yanzhenjie.andserver.Server;

import java.util.concurrent.TimeUnit;

/**
 * 作者：By hdy
 * 日期：On 2019/3/19
 * 时间：At 21:28
 */
public class RemoteServerManager {
    private volatile static RemoteServerManager sInstance;
    private Server server;
    //    private RemoteServer remoteServer;
    private static final int port = 52020;
    private DlanUrlDTO urlDTO;

    public String getPlayUrl() {
        return urlDTO == null ? "" : urlDTO.getUrl();
    }

    public static RemoteServerManager instance() {
        if (sInstance == null) {
            synchronized (RemoteServerManager.class) {
                if (sInstance == null) {
                    sInstance = new RemoteServerManager();
                }
            }
        }
        return sInstance;
    }

    private void initServer(Context context, Server.ServerListener listener) throws Exception {
        server = AndServer.serverBuilder()
                .port(port)
//                .inetAddress(new InetSocketAddress(50000).getAddress())
                .timeout(60, TimeUnit.SECONDS)
                .listener(listener).build();
        server.startup();
    }

    public String getServerUrl(Context context) {
        String ip = LocalServerParser.getIP(context);
//        String ip = server.getInetAddress().getHostAddress();
        if (TextUtils.isEmpty(ip)) {
            return null;
        }
        return "http://" + ip + ":" + port;
    }

    public synchronized void startServer(Context context, Server.ServerListener listener) throws Exception {
        if (server == null) {
            initServer(context, listener);
        } else {
            listener.onStarted();
        }
//        if (remoteServer == null) {
//            File file = new File(Environment.getExternalStorageDirectory() + File.separator + "FangYuan" + File.separator + "rules" + File.separator + "web");
//            remoteServer = new RemoteServer("0.0.0.0", 50000, file, true);
//            try {
//                remoteServer.start();
//                listener.onStarted();
//            } catch (IOException e) {
//                e.printStackTrace();
//                listener.onException(e);
//            }
//        } else {
//            listener.onStarted();
//        }
    }

    public void destroyServer() {
        if (server != null) {
            server.shutdown();
            server = null;
        }
    }

    public boolean isServerAlive() {
        return server != null && server.isRunning();
    }

    public DlanUrlDTO getUrlDTO() {
        return urlDTO;
    }

    public void setUrlDTO(DlanUrlDTO urlDTO) {
        this.urlDTO = urlDTO;
    }
}

package com.example.hikerview.ui.webdlan;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import com.example.hikerview.model.DownloadRecord;
import com.example.hikerview.utils.StringUtil;
import com.example.hikerview.utils.ToastMgr;
import com.example.hikerview.utils.UriUtils;

import java.io.File;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

/**
 * 作者：By hdy
 * 日期：On 2018/11/1
 * 时间：At 19:17
 */
public class LocalServerParser {
    private static final String TAG = "LocalServerParser";
    private static String realUrl;

    public static String getUrlForPos(Context context, String url) {
        if (StringUtil.isEmpty(url)) {
            return url;
        }
        String[] s = url.split(":");
        if (s.length > 2 && StringUtil.isNotEmpty(realUrl)
                && (s[2].startsWith("11111/") && (url.contains("index.m3u8") || url.contains("video.")))
        ) {
            return realUrl;
        }
        return url;
    }

    public static String getRealUrl(Context context, DownloadRecord downloadRecord) {
        if ("player/m3u8".equals(downloadRecord.getVideoType())) {
            return getRealUrl(context, "http://127.0.0.1:11111/" + downloadRecord.getFileName() + "/index.m3u8");
        }
        return getRealUrl(context, "http://127.0.0.1:11111/" + downloadRecord.getFileName() + "/video." + downloadRecord.getFileExtension());
    }

    public static String getRealUrl(Context context, String url) {
        url = getRealLocalUrl(context, url);
        if (!url.startsWith("http://127.0.0.1")) {
            return url;
        }
        if (context != null) {
            String[] urlss = url.replace("http://", "").split(":");
            return "http://" + getIP(context) + ":" + urlss[1];
        }
        return url;
    }


    public static String getRealLocalUrl(Context context, String url) {
        if (!url.startsWith("http://127.0.0.1")) {
            realUrl = "";
            return url;
        }
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        String[] urls = url.replace("http://", "").split("/");
        if (urls.length < 3) {
            realUrl = "";
            return url;
        }
        int rootPathPos = urls.length - 2;
        String rootPath = urls[rootPathPos];
        String www = UriUtils.getRootDir(context) + File.separator + "download" + File.separator + rootPath;
        WebServerManager.instance().startServer(context, www);
        realUrl = "http://" + urls[0] + "/" + rootPath + "/" + urls[urls.length - 1];
        Log.d(TAG, "getRealLocalUrl: " + realUrl);
        return "http://" + urls[0] + "/" + urls[urls.length - 1];
    }

    public static String getRealUrlForRemotedPlay(Context context, String url) {
        if (url.startsWith("file://")) {
            return getLocalFileRealUrl(context, url);
        } else {
            return getRealUrl(context, url);
        }
    }

    private static String getLocalFileRealUrl(Context context, String url) {
        if (!url.startsWith("file://")) {
            realUrl = "";
            return url;
        }
        url = url.replace("file://", "").split("#")[0];
        File file = new File(url);
        String path = url.replace(File.separator + file.getName(), "");
        WebServerManager.instance().startServer(context, path);
        String ip = getIP(context);
        try {
            realUrl = "http://" + ip + ":11111/" + file.getParentFile().getName() + "/" + file.getName();
            Log.d(TAG, "getRealLocalUrl: " + realUrl);
        } catch (Exception e) {
            e.printStackTrace();
            realUrl = "";
        }
        return "http://" + ip + ":11111/" + file.getName();
    }

    public static String getIP(Context context) {
        try {
            WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            //判断wifi是否开启
            WifiInfo wifiInfo = null;
            if (wifiManager != null) {
                wifiInfo = wifiManager.getConnectionInfo();
            }
            int ipAddress = 0;
            if (wifiInfo != null) {
                ipAddress = wifiInfo.getIpAddress();
            }
            return intToIp(ipAddress);
        } catch (Exception e) {
            e.printStackTrace();
            ToastMgr.shortBottomCenter(context, "出错：" + e.toString());
            try {
                return getLocalIPAddress();
            } catch (Exception e1) {
                e1.printStackTrace();
                ToastMgr.shortBottomCenter(context, "出错：" + e.toString());
            }
        }
        return "127.0.0.1";
    }

    private static String getLocalIPAddress() {
        try {
            for (Enumeration<NetworkInterface> mEnumeration = NetworkInterface
                    .getNetworkInterfaces(); mEnumeration.hasMoreElements(); ) {
                NetworkInterface intf = mEnumeration.nextElement();
                for (Enumeration<InetAddress> enumIPAddr = intf
                        .getInetAddresses(); enumIPAddr.hasMoreElements(); ) {
                    InetAddress inetAddress = enumIPAddr.nextElement();
                    // 如果不是回环地址
                    if (!inetAddress.isLoopbackAddress()) {
                        // 直接返回本地IP地址
                        return inetAddress.getHostAddress();
                    }
                }
            }
        } catch (SocketException ex) {
            System.err.print("error");
        }
        return "127.0.0.1";
    }

    public static InetAddress getLocalINetAddress() throws SocketException {
        for (Enumeration<NetworkInterface> mEnumeration = NetworkInterface
                .getNetworkInterfaces(); mEnumeration.hasMoreElements(); ) {
            NetworkInterface intf = mEnumeration.nextElement();
            for (Enumeration<InetAddress> enumIPAddr = intf
                    .getInetAddresses(); enumIPAddr.hasMoreElements(); ) {
                InetAddress inetAddress = enumIPAddr.nextElement();
                // 如果不是回环地址
                if (!inetAddress.isLoopbackAddress()) {
                    // 直接返回本地IP地址
                    return inetAddress;
                }
            }
        }
        throw new SocketException("获取本地IP地址失败！");
    }

    private static String intToIp(int i) {
        return (i & 0xFF) + "." +
                ((i >> 8) & 0xFF) + "." +
                ((i >> 16) & 0xFF) + "." +
                (i >> 24 & 0xFF);
    }

}

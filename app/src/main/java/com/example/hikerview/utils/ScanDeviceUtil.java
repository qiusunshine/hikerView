package com.example.hikerview.utils;

import android.text.TextUtils;
import android.util.Log;

import com.example.hikerview.event.OnHasFoundDeviceEvent;
import com.example.hikerview.service.http.CharsetStringCallback;
import com.lzy.okgo.OkGo;
import com.lzy.okgo.https.HttpsUtils;
import com.lzy.okgo.interceptor.HttpLoggingInterceptor;

import org.greenrobot.eventbus.EventBus;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

import okhttp3.OkHttpClient;
import timber.log.Timber;

/**
 * 作者：By hdy
 * 日期：On 2019/3/30
 * 时间：At 23:33
 */
public class ScanDeviceUtil {
    public static final String PORT = ":52020";
    public static final String HTTP = "http://";
    public static final String PLAY_URL = "/test";
    private static final String TAG = ScanDeviceUtil.class.getSimpleName();

    private String mDevAddress;// 本机IP地址-完整
    private String mLocAddress;// 局域网IP地址头,如：192.168.1.
    private Runtime mRun = Runtime.getRuntime();// 获取当前运行环境，来执行ping，相当于windows的cmd
    private String mPing = "ping -c 1 -w 2 ";// 其中 -c 1为发送的次数，-w 表示发送后等待响应的时间

    private OkHttpClient okHttpClient;
    private AtomicBoolean hasFound;


    /**
     * 扫描局域网内ip，找到对应服务器
     *
     * @return void
     */
    public void scan() {
        mDevAddress = getLocAddress();// 获取本机IP地址
        mLocAddress = getLocAddrIndex(mDevAddress);// 获取本地ip前缀
        Log.d(TAG, "开始扫描设备,本机Ip为：" + mDevAddress);
        if (TextUtils.isEmpty(mLocAddress)) {
            Log.e(TAG, "扫描失败，请检查wifi网络");
            return;
        }
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor("OkGo");
        loggingInterceptor.setPrintLevel(HttpLoggingInterceptor.Level.BODY);
        loggingInterceptor.setColorLevel(Level.INFO);
        builder.addInterceptor(loggingInterceptor);

        builder.readTimeout(2000, TimeUnit.MILLISECONDS);
        builder.writeTimeout(2000, TimeUnit.MILLISECONDS);
        builder.connectTimeout(2000, TimeUnit.MILLISECONDS);

        HttpsUtils.SSLParams sslParams = HttpsUtils.getSslSocketFactory();
        builder.sslSocketFactory(sslParams.sSLSocketFactory, sslParams.trustManager);
        builder.hostnameVerifier(HttpsUtils.UnSafeHostnameVerifier);
        okHttpClient = builder.build();

        hasFound = new AtomicBoolean(false);

        for (int i = 1; i < 255; i++) {
            String currentIp = mLocAddress + i;
            if (mDevAddress.equals(currentIp)) {
                // 如果与本机IP地址相同,跳过;
                continue;
            }
            HeavyTaskUtil.executeBigTask(new MyRunnable(currentIp));
        }
    }

    /**
     * 获取本地ip地址
     *
     * @return String
     */
    private String getLocAddress() {
        String ipaddress = "";
        try {
            Enumeration<NetworkInterface> en = NetworkInterface
                    .getNetworkInterfaces();
            // 遍历所用的网络接口
            while (en.hasMoreElements()) {
                NetworkInterface networks = en.nextElement();
                // 得到每一个网络接口绑定的所有ip
                Enumeration<InetAddress> address = networks.getInetAddresses();
                // 遍历每一个接口绑定的所有ip
                while (address.hasMoreElements()) {
                    InetAddress ip = address.nextElement();
                    if (!ip.isLoopbackAddress()
                            && NetUtils.isIPv4Address(ip.getHostAddress())) {
                        ipaddress = ip.getHostAddress();
                    }
                }
            }
        } catch (SocketException e) {
            Log.e("", "获取本地ip地址失败");
            e.printStackTrace();
        }

        Log.i(TAG, "本机IP:" + ipaddress);
        return ipaddress;
    }

    /**
     * 获取本机IP前缀
     *
     * @param devAddress // 本机IP地址
     * @return String
     */
    private String getLocAddrIndex(String devAddress) {
        if (!devAddress.equals("")) {
            return devAddress.substring(0, devAddress.lastIndexOf(".") + 1);
        }
        return null;
    }

    class MyRunnable implements Runnable {
        private String currentIp;

        MyRunnable(String currentIp) {
            this.currentIp = currentIp;
        }


        @Override
        public void run() {
            if(hasFound.get()){
                Timber.d("ScanDeviceUtil hasFound, stop load");
                return;
            }
            String url = HTTP + currentIp + PORT;
            Timber.d("ScanDeviceUtil start scan url: %s", url);
            OkGo.<String>get(url + PLAY_URL)
                    .client(okHttpClient)
                    .execute(new CharsetStringCallback("UTF-8") {
                        @Override
                        public void onSuccess(com.lzy.okgo.model.Response<String> response) {
                            String s = response.body();
                            if (s != null && s.length() > 0) {
                                String deviceName = s;
                                if ("ok".equals(s)) {
                                    deviceName = currentIp;
                                }
                                Timber.d("ScanDeviceUtil 成功获取到连接：%s", url);
                                if (!hasFound.get()) {
                                    hasFound.set(true);
                                    OkGo.cancelAll(okHttpClient);
                                    EventBus.getDefault().post(new OnHasFoundDeviceEvent(url, deviceName));
                                }
                            }
                        }

                        @Override
                        public void onError(com.lzy.okgo.model.Response<String> response) {
                            super.onError(response);
                        }
                    });
        }
    }
}

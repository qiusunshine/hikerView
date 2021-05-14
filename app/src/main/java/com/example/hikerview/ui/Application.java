package com.example.hikerview.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;

import androidx.multidex.MultiDex;

import com.example.hikerview.BuildConfig;
import com.example.hikerview.constants.TimeConstants;
import com.example.hikerview.ui.dlan.DlanForegroundService;
import com.example.hikerview.ui.download.DownloadForegroundService;
import com.example.hikerview.ui.video.MusicForegroundService;
import com.example.hikerview.utils.CrashHandler;
import com.example.hikerview.utils.HeavyTaskUtil;
import com.example.hikerview.utils.StringUtil;
import com.example.hikerview.utils.ToastMgr;
import com.lzy.okgo.OkGo;
import com.lzy.okgo.https.HttpsUtils;
import com.lzy.okgo.model.HttpHeaders;
import com.umeng.commonsdk.UMConfigure;
import com.wanjian.cockroach.Cockroach;
import com.wanjian.cockroach.ExceptionHandler;
import com.zzhoujay.richtext.RichText;

import org.litepal.LitePal;

import java.lang.ref.WeakReference;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.brotli.BrotliInterceptor;
import ren.yale.android.cachewebviewlib.WebViewCacheInterceptor;
import ren.yale.android.cachewebviewlib.WebViewCacheInterceptorInst;
import ren.yale.android.cachewebviewlib.config.CacheExtensionConfig;
import timber.log.Timber;

/**
 * 作者：By hdy
 * 日期：On 2017/10/7
 * 时间：At 21:50
 */

public class Application extends android.app.Application {
    private static final String TAG = "Application";
    private static WeakReference<Context> mContext;
    public static Application application = null;
    private static boolean hasMainActivity = false;
    private Activity homeActivity;
    public static int loadedTime = 0;

    public static boolean hasMainActivity() {
        return hasMainActivity;
    }

    public static void setHasMainActivity(boolean hasMainActivity) {
        Application.hasMainActivity = hasMainActivity;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        application = this;
        mContext = new WeakReference<>(getApplicationContext());
        //OKGO配置
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.addInterceptor(BrotliInterceptor.INSTANCE);
        builder.readTimeout(TimeConstants.HTTP_TIMEOUT_MILLISECONDS, TimeUnit.MILLISECONDS);
        builder.writeTimeout(TimeConstants.HTTP_TIMEOUT_MILLISECONDS, TimeUnit.MILLISECONDS);
        builder.connectTimeout(TimeConstants.HTTP_TIMEOUT_MILLISECONDS, TimeUnit.MILLISECONDS);
        //方法一：信任所有证书,不安全有风险
        HttpsUtils.SSLParams sslParams1 = HttpsUtils.getSslSocketFactory();
        builder.sslSocketFactory(sslParams1.sSLSocketFactory, HttpsUtils.UnSafeTrustManager)
                .hostnameVerifier(HttpsUtils.UnSafeHostnameVerifier);
        HttpHeaders headers = new HttpHeaders();
        headers.put("charset", "UTF-8");
        OkGo.getInstance().init(this).setOkHttpClient(builder.build())
                .setRetryCount(1)
                .addCommonHeaders(headers);
        installCrashHandler();
        LitePal.initialize(this);
        LitePal.getDatabase().disableWriteAheadLogging();
        initCacheWeb();
        UMConfigure.init(this, null, null, UMConfigure.DEVICE_TYPE_PHONE, null);
        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
        }
        RichText.initCacheDir(this);
        // 写 ActivityLifecycle 调试 Task 信息用
        // Timber.tag("Application").d(activity.getLocalClassName() + "#TaskId#" + activity.getTaskId());
    }

    private void installCrashHandler() {
        CrashHandler.getInstance().initDefaultHandler(getContext());
        Cockroach.install(this, new ExceptionHandler() {
            @Override
            protected void onUncaughtExceptionHappened(Thread thread, Throwable throwable) {
                Timber.e(throwable, "--->onUncaughtExceptionHappened:" + thread + "<---");
                String fileName = CrashHandler.getInstance().saveCatchInfo2File(throwable);
                new Handler(Looper.getMainLooper()).post(() -> {
                    ToastMgr.shortBottomCenter(getApplicationContext(), "检测到异常崩溃信息，已记录崩溃日志");
                });
            }

            @Override
            protected void onBandageExceptionHappened(Throwable throwable) {
                String fileName = CrashHandler.getInstance().saveCatchInfo2File(throwable);
                new Handler(Looper.getMainLooper()).post(() -> {
                    ToastMgr.shortBottomCenter(getApplicationContext(), "检测到异常崩溃信息，已记录崩溃日志");
                });
            }

            @Override
            protected void onEnterSafeMode() {

            }

            @Override
            protected void onMayBeBlackScreen(Throwable e) {
                Thread thread = Looper.getMainLooper().getThread();
                CrashHandler.getInstance().crashMySelf(thread, e);
            }

        });
    }

    private void initCacheWeb() {
        HeavyTaskUtil.executeNewTask(() -> {
            WebViewCacheInterceptor.Builder builder2 = new WebViewCacheInterceptor.Builder(this);
            CacheExtensionConfig extension = new CacheExtensionConfig();
            builder2.setDebug(false);
            builder2.setCacheSize(1024 * 1024 * 300);
            //删除缓存后缀
            extension.removeExtension("html").removeExtension("htm")
                    .removeExtension("js").removeExtension("css").removeExtension("txt")
                    .removeExtension("gif").removeExtension("bmp");
            builder2.setCacheExtensionConfig(extension);
            builder2.setResourceInterceptor(url -> StringUtil.isNotEmpty(url) && url.contains(".oss-cn-hangzhou.aliyuncs.com"));
            WebViewCacheInterceptorInst.getInstance().init(builder2);
        });
    }

    public static Context getContext() {
        return mContext.get();
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(base);
    }

    public void startDlanForegroundService() {
        Intent intent = new Intent(Application.application, DlanForegroundService.class);
        startService(intent);
    }

    public void stopDlanForegroundService() {
        try {
            Intent intent = new Intent(Application.application, DlanForegroundService.class);
            stopService(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void startDownloadForegroundService() {
        Intent intent = new Intent(Application.application, DownloadForegroundService.class);
        startService(intent);
    }

    public void stopDownloadForegroundService() {
        try {
            Intent intent = new Intent(Application.application, DownloadForegroundService.class);
            stopService(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void startMusicForegroundService() {
        Intent intent = new Intent(Application.application, MusicForegroundService.class);
        startService(intent);
    }

    public void stopMusicForegroundService() {
        try {
            Intent intent = new Intent(Application.application, MusicForegroundService.class);
            stopService(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Activity getHomeActivity() {
        return homeActivity;
    }

    public void setHomeActivity(Activity homeActivity) {
        this.homeActivity = homeActivity;
    }
}

package com.example.hikerview.ui.download;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.text.TextUtils;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.example.hikerview.ui.download.util.UUIDUtil;
import com.example.hikerview.utils.ClipboardUtil;
import com.example.hikerview.utils.FilesInAppUtil;
import com.example.hikerview.utils.StringUtil;
import com.example.hikerview.utils.TaskUtil;
import com.example.hikerview.utils.ToastMgr;

/**
 * 作者：By hdy
 * 日期：On 2019/1/28
 * 时间：At 14:53
 */
public class DownloadChooser {

    public static void startDownload(Activity context, @Nullable String mTitle, @Nullable String mUrl, @Nullable String film) {
        DownloadDialogUtil.showEditDialog(context, mTitle, mUrl, film);
    }

    public static void startDownloadByThird(Activity context, String name, String url) {
        url = DownloadChooser.getCanDownloadUrl(context, url);
        if (StringUtil.isEmpty(url)) {
            return;
        }
        String[] names = {"系统下载器", "系统浏览器", "IDM+", "M3u8Loader", "ADM", "ADM+IDM智能调度", "闪电下载器"};
        String finalUrl = url;
        new AlertDialog.Builder(context).setTitle("选择下载器")
                .setSingleChoiceItems(names, 0, (dialog, which) -> {
                    dialog.dismiss();
                    startDownload(context, which + 1, name, finalUrl);
                }).setNegativeButton("取消", null).show();
    }


    public static boolean startDownload(Activity context, int downloader, String name, String url) {
        url = DownloadChooser.getCanDownloadUrl(context, url);
        if (StringUtil.isEmpty(url)) {
            return false;
        }
        if (downloader == 0) {
            DownloadTask downloadTask = new DownloadTask(
                    UUIDUtil.genUUID(), null, null, null, url, url, name, 0L);
            DownloadManager.instance().addTask(downloadTask);
            ToastMgr.shortBottomCenter(context, "已加入下载队列");
            return false;
        }
        TaskUtil.showDetailActivityFromRecents(context, false);
        //非自带下载器
        if (downloader == 1) {
            return startSystemDownloader(context, name, url);
        } else if (downloader == 2) {
            return startSystemBrowser(context, name, url);
        } else if (downloader == 3) {
            return startDownloadUseIDM(context, name, url);
        } else if (downloader == 4) {
            return startDownloadUseM3u8Loader(context, name, url);
        } else if (downloader == 5) {
            return startDownloadUseADM(context, name, url);
        } else if (downloader == 6) {
            return startDownloadUseADMOrIDMSmartly(context, name, url);
        } else if (downloader == 7) {
            return startDownloadUseFlashLoader(context, name, url);
        }
        return false;
    }

    private static boolean startSystemBrowser(Activity context, String name, String url) {
        try {
            Uri uri = Uri.parse(url);
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            context.startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
            ToastMgr.shortBottomCenter(context, "调用系统浏览器失败：" + e.getMessage());
        }
        return true;
    }

    public static String getCanDownloadUrl(Context context, String mUrl) {
        if (TextUtils.isEmpty(mUrl)) {
            ToastMgr.shortBottomCenter(context, "地址为空");
            return null;
        }
        if (mUrl.startsWith("file://") || mUrl.startsWith("content://") || mUrl.contains(":11111/")) {
            String[] urls = mUrl.split("\\?url=");
            if (urls.length > 1) {
                return urls[1];
            } else {
                ToastMgr.shortBottomCenter(context, "该文件已经下载过或者属于本地文件");
                return null;
            }
        }
        return mUrl;
    }

    private static boolean startDownloadUseADMOrIDMSmartly(Context context, String name, String url) {
        if (url.contains("m3u8")) {
            return startDownloadUseIDM(context, name, url);
        } else {
            return startDownloadUseADM(context, name, url);
        }
    }

    private static boolean startDownloadUseADMSmartly(Context context, String name, String url) {
        if (url.contains("m3u8")) {
            return false;
        } else {
            return startDownloadUseADM(context, name, url);
        }
    }

    private static boolean startDownloadUseADM(Context context, String name, String url) {
        try {
            Intent localIntent = new Intent();
            localIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);//268435456
            localIntent.setType("text/plain");
            localIntent.setAction("android.intent.action.SEND");
            localIntent.putExtra("android.intent.extra.TEXT", url);
            localIntent.putExtra("title", name);
            localIntent.putExtra("name", name);
            ClipboardUtil.copyToClipboard(context, name);
            try {
                localIntent.setComponent(new ComponentName("com.dv.adm.pay", "com.dv.adm.pay.AEditor"));
                context.startActivity(localIntent);
            } catch (Exception e) {
                e.printStackTrace();
                localIntent.setComponent(new ComponentName("com.dv.adm", "com.dv.adm.AEditor"));
                context.startActivity(localIntent);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            ToastMgr.shortBottomCenter(context, "调用ADM下载器失败！" + e.getMessage());
        }
        return true;
    }

    private static boolean startDownloadUseM3u8Loader(Context context, String name, String url) {
        Intent localIntent = new Intent();
        localIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);//268435456
        localIntent.setType("text/plain");
        localIntent.setAction("android.intent.action.SEND");
        localIntent.putExtra("android.intent.extra.TEXT", url);
        localIntent.putExtra("title", name);
        localIntent.putExtra("name", name);
        try {
            localIntent.setComponent(new ComponentName("ru.yourok.m3u8loader", "ru.yourok.m3u8loader.activitys.AddListActivity"));
            context.startActivity(localIntent);
            return true;
        } catch (Exception e) {
            try {
                localIntent.setComponent(new ComponentName("ru.yourok.m3u8loader", "ru.yourok.m3u8loader.AddLoaderActivity"));
                context.startActivity(localIntent);
                return true;
            } catch (Exception ignored) {
            }
        }
        ToastMgr.shortBottomCenter(context, "调用M3U8Loader下载器失败！");
        return true;
    }

    private static boolean startDownloadUseFlashLoader(Context context, String name, String url) {
        Intent localIntent = new Intent();
        localIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);//268435456
        localIntent.setAction("android.intent.action.VIEW");
        localIntent.putExtra("android.intent.extra.TEXT", url);
        localIntent.setData(FilesInAppUtil.getUri(context, url));
        localIntent.putExtra("title", name);
        localIntent.putExtra("name", name);
        localIntent.putExtra("url", url);
        try {
            localIntent.setComponent(new ComponentName("com.flash.download", "com.example.dwd.myapplication.activity.AddDownloadActivity"));
            context.startActivity(localIntent);
            return true;
        } catch (Exception e) {
            try {
                localIntent.setComponent(new ComponentName("com.flash.download", "com.example.dwd.myapplication.activity.AddDownloadActivity"));
                context.startActivity(localIntent);
                return true;
            } catch (Exception ignored) {
            }
        }
        ToastMgr.shortBottomCenter(context, "调用闪电下载器失败！");
        return true;
    }

    private static boolean startSystemDownloader(Context context, String name, String url) {
        try {
            //创建下载任务,downloadUrl就是下载链接
            android.app.DownloadManager.Request request = new android.app.DownloadManager.Request(Uri.parse(url));
            //指定下载路径和下载文件名
            request.setNotificationVisibility(android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, name);
            //获取下载管理器
            android.app.DownloadManager downloadManager = (android.app.DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
            //将下载任务加入下载队列，否则不会进行下载
            if (downloadManager != null) {
                downloadManager.enqueue(request);
                ToastMgr.shortBottomCenter(context, "已提交下载任务，通知栏可查看进度");
            } else {
                ToastMgr.shortBottomCenter(context, "获取系统downloadManager失败");
            }
        } catch (Exception e) {
            e.printStackTrace();
            ToastMgr.shortBottomCenter(context, "调用系统下载器失败：" + e.getMessage());
        }
        return true;
    }

    private static boolean startDownloadUseIDM(Context context, String name, String url) {
        try {
            Intent localIntent = new Intent();
            localIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);//268435456
            localIntent.setType("text/plain");
            localIntent.setAction("android.intent.action.SEND");
            localIntent.putExtra("android.intent.extra.TEXT", url);
            localIntent.putExtra("title", name);
            ClipboardUtil.copyToClipboard(context, name);
            ToastMgr.shortBottomCenter(context, "已复制视频名称");
            localIntent.setComponent(new ComponentName("idm.internet.download.manager.plus", "idm.internet.download.manager.MainActivity"));
            context.startActivity(localIntent);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            ToastMgr.shortBottomCenter(context, "调用IDM+下载器失败！" + e.getMessage());
        }
        return true;
    }
}

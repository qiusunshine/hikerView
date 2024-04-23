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

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.example.hikerview.model.DownloadRecord;
import com.example.hikerview.service.parser.HttpHelper;
import com.example.hikerview.service.parser.HttpParser;
import com.example.hikerview.ui.Application;
import com.example.hikerview.ui.browser.model.UrlDetector;
import com.example.hikerview.ui.browser.util.CollectionUtil;
import com.example.hikerview.ui.download.util.UUIDUtil;
import com.example.hikerview.ui.download.util.VideoFormatUtil;
import com.example.hikerview.ui.view.popup.InputPopup;
import com.example.hikerview.ui.webdlan.LocalServerParser;
import com.example.hikerview.utils.ClipboardUtil;
import com.example.hikerview.utils.DisplayUtil;
import com.example.hikerview.utils.FileEntity;
import com.example.hikerview.utils.FileUtil;
import com.example.hikerview.utils.FilesInAppUtil;
import com.example.hikerview.utils.FilesUtilsKt;
import com.example.hikerview.utils.HeavyTaskUtil;
import com.example.hikerview.utils.PreferenceMgr;
import com.example.hikerview.utils.ShareUtil;
import com.example.hikerview.utils.StringUtil;
import com.example.hikerview.utils.TaskUtil;
import com.example.hikerview.utils.ThreadTool;
import com.example.hikerview.utils.ToastMgr;
import com.example.hikerview.utils.view.DialogUtil;
import com.lxj.xpopup.XPopup;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 作者：By hdy
 * 日期：On 2019/1/28
 * 时间：At 14:53
 */
public class DownloadChooser {
    private static Map<String, DownloadInfo> downloadInfoMap;

    public static void startDownload(Activity context, @Nullable String mTitle, @Nullable String mUrl, @Nullable String film) {
        DownloadDialogUtil.showEditDialog(context, mTitle, mUrl, film);
    }

    public static void startDownloadByThird(Activity context, String name, String url, @Nullable String film, @Nullable String originalUrl) {
        url = HttpParser.getThirdDownloadSource(DownloadChooser.getCanDownloadUrl(context, url));
        if (StringUtil.isEmpty(url)) {
            return;
        }
        String[] names = {"系统下载器", "系统浏览器", "IDM+", "M3u8Loader", "ADM", "ADM+IDM智能调度", "闪电下载器", "全能下载器", "Free Download Manager", "Aria2", "其它下载器"};
        String finalUrl = url;
        AlertDialog alertDialog = new AlertDialog.Builder(context).setTitle("选择下载器")
                .setSingleChoiceItems(names, 0, (dialog, which) -> {
                    dialog.dismiss();
                    startDownload(context, which + 1, name, finalUrl, film, originalUrl);
                }).setNegativeButton("取消", null).create();
        DialogUtil.INSTANCE.showAsCard(context, alertDialog);
    }

    public static boolean startDownload(Activity context, int downloader, String name, String url, @Nullable String film) {
        return startDownload(context, downloader, name, url, film, null);
    }

    public static boolean startDownload(Activity context, int downloader, String name, String url, @Nullable String film, @Nullable String originalUrl) {
        url = HttpParser.getThirdDownloadSource(DownloadChooser.getCanDownloadUrl(context, url));
        if (StringUtil.isEmpty(url)) {
            return false;
        }
        if (url.startsWith("file://") && url.contains(".m3u8")) {
            url = LocalServerParser.getRealUrlForRemotedPlay(context, url);
        }
        if (downloader == 0) {
            if (StringUtil.isNotEmpty(originalUrl)) {
                url = originalUrl;
            }
            DownloadTask downloadTask = new DownloadTask(
                    UUIDUtil.genUUID(), null, null, null, url, url, name, 0L);
            DownloadManager.instance().addTask(downloadTask);
            ToastMgr.shortBottomCenter(context, "已加入下载队列");
            return false;
        }
        TaskUtil.showDetailActivityFromRecents(context, false);
        //非自带下载器
        if (downloader == 1) {
            return startSystemDownloader(context, name, url, film, originalUrl);
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
        } else if (downloader == 8) {
            return startDownloadUseNick(context, name, url);
        } else if (downloader == 9) {
            return startDownloadUseFDM(context, name, url);
        } else if (downloader == 10) {
            return startDownloadUseAria2(context, name, url, originalUrl);
        } else if (downloader == 11) {
            ShareUtil.findChooserToDeal(context, url);
            return true;
        }
        return false;
    }

    private static boolean startDownloadUseAria2(Activity context, String name, String url, @Nullable String originalUrl) {
        String a = PreferenceMgr.getString(
                context,
                "aria2",
                ""
        );
        String secret = PreferenceMgr.getString(
                context,
                "aria2secret",
                ""
        );
        if (StringUtil.isEmpty(a)) {
            InputPopup inputPopup = new InputPopup(context)
                    .bind("配置Aria2地址和密钥", "Aria2地址", a, "密钥（非必填）", secret, (title, code) -> {
                        if (StringUtil.isNotEmpty(title)) {
                            PreferenceMgr.put(
                                    context,
                                    "aria2",
                                    title
                            );
                            PreferenceMgr.put(
                                    context,
                                    "aria2secret",
                                    code
                            );
                            ToastMgr.shortBottomCenter(
                                    context,
                                    "设置成功"
                            );
                            startDownloadUseAria2_(context, title, code, name, url, originalUrl);
                        } else {
                            ToastMgr.shortBottomCenter(
                                    context,
                                    "请输入正确的地址"
                            );
                        }
                    });
            new XPopup.Builder(context)
                    .borderRadius(DisplayUtil.dpToPx(context, 16))
                    .asCustom(inputPopup)
                    .show();
        } else {
            startDownloadUseAria2_(context, a, secret, name, url, originalUrl);
        }
        return true;
    }

    private static void startDownloadUseAria2_(Activity context, String aria2, String secret, String name, String url, @Nullable String originalUrl) {
        String title = name;
        if (StringUtil.isNotEmpty(name)) {
            VideoFormat videoFormat = VideoFormatUtil.getVideoFormat(name, url);
            if (videoFormat != null) {
                String fileName = name;
                if (fileName.lastIndexOf(".") >= 1) {
                    fileName = fileName.substring(0, fileName.lastIndexOf("."));
                }
                title = fileName + "." + videoFormat.getName();
            }
        }
        InputPopup inputPopup = new InputPopup(context);
        inputPopup.setDismissWhenConfirm(false);
        inputPopup.bind("Aria2下载", "文件名（非必填）", title, "下载地址", url, (t, u) -> {
            if (StringUtil.isNotEmpty(u)) {
                if (StringUtil.isNotEmpty(t) && !t.contains(".")) {
                    new XPopup.Builder(context)
                            .borderRadius(DisplayUtil.dpToPx(context, 16))
                            .asConfirm("温馨提示", "文件名不包含后缀，建议加上后缀或者文件名留空让Aria2自动识别，是否忽略提示继续调用Aria2下载？", () -> {
                                inputPopup.dismiss();
                                startDownloadUseAria2(aria2, secret, t, u, originalUrl);
                            }).show();
                } else {
                    inputPopup.dismiss();
                    startDownloadUseAria2(aria2, secret, t, u, originalUrl);
                }
            } else {
                ToastMgr.shortBottomCenter(context, "下载地址不能为空");
            }
        });
        new XPopup.Builder(context)
                .borderRadius(DisplayUtil.dpToPx(context, 16))
                .asCustom(inputPopup)
                .show();
    }

    private static void startDownloadUseAria2(String aria2, String secret, String name, String url, @Nullable String originalUrl) {
        HeavyTaskUtil.executeNewTask(() -> {
            Map<String, Object> op = new HashMap<>();
            JSONObject json = new JSONObject();
            json.put("id", StringUtil.md5(url));
            json.put("jsonrpc", "2.0");
            json.put("method", "aria2.addUri");
            JSONArray params = new JSONArray();
            if (StringUtil.isNotEmpty(secret)) {
                params.add("token:" + secret);
            }
            JSONArray urls = new JSONArray();
            urls.add(url);
            params.add(urls);
            JSONObject option = new JSONObject();
            if (StringUtil.isNotEmpty(originalUrl)) {
                Map<String, String> headers = HttpParser.getHeaders(originalUrl);
                if (headers != null) {
                    if (headers.containsKey("Referer")) {
                        option.put("referer", headers.get("Referer"));
                    }
                }
            }
            if (StringUtil.isNotEmpty(name)) {
                option.put("out", name);
            }
            params.add(option);
            json.put("params", params);
            op.put("method", "POST");
            op.put("body", json);
            String res = HttpHelper.fetch(aria2, op);
            boolean ok = StringUtil.isNotEmpty(res) && res.contains("result");
            ThreadTool.INSTANCE.runOnUI(() -> ToastMgr.shortBottomCenter(Application.getContext(), "调用Aria2" + (ok ? "成功" : "失败")));
        });
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
        if (mUrl.startsWith("file://")) {
            return mUrl;
        }
        if (mUrl.startsWith("content://") || mUrl.contains(":11111/")) {
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

    private static boolean startSystemDownloader(Context context, String name, String url, @Nullable String film, @Nullable String originalUrl) {
        try {
            //创建下载任务,downloadUrl就是下载链接
            if (StringUtil.isNotEmpty(originalUrl)) {
                url = HttpParser.getThirdDownloadSource(originalUrl);
            }
            android.app.DownloadManager.Request request = new android.app.DownloadManager.Request(Uri.parse(url));
            if (StringUtil.isNotEmpty(originalUrl)) {
                Map<String, String> headers = HttpParser.getHeaders(originalUrl);
                if (headers != null) {
                    for (Map.Entry<String, String> entry : headers.entrySet()) {
                        request.addRequestHeader(entry.getKey(), entry.getValue());
                    }
                }
            }
            //指定下载路径和下载文件名
            request.setNotificationVisibility(android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            name = StringUtil.filenameFilter(name);
            if (!name.contains(".")) {
                String end = FileUtil.getExtension(url).split("#")[0].split("\\?")[0];
                name = name + "." + end;
            }

            request.setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOCUMENTS, "download" + File.separator + name.trim());
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

    public static String getRootPath(Context context) {
//        try {
//            return new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), context.getResources().getString(R.string.app_name)).getAbsolutePath();
//        } catch (Resources.NotFoundException e) {
//            e.printStackTrace();
//        }
        return DownloadConfig.defaultRootPath;
    }

    public static List<DownloadRecord> getLocalDownloaded(Context context) {
        List<DownloadRecord> records = new ArrayList<>();
        File dir = new File(getRootPath(context));
        if (dir.exists()) {
            File[] files = dir.listFiles();
            if (files != null && files.length > 0) {
                for (File file : files) {
                    if (file.isDirectory() || file.getName().endsWith(".download") || file.getName().endsWith(".temp")) {
                        continue;
                    }
                    DownloadRecord record = new DownloadRecord();
                    String fileName = file.getName();
                    record.setFileName(fileName);
                    record.setSourcePageTitle(fileName);
                    record.setSourcePageUrl(file.getAbsolutePath());
                    record.setFileExtension(FileUtil.getExtension(fileName));
                    record.setStatus(DownloadStatusEnum.SUCCESS.getCode());
                    record.setSize(FileUtil.getFolderSize(file));
                    record.setTotalDownloaded(record.getSize());
                    record.setVideoType("normal");
                    record.setFilm(smartFilm(fileName));
                    record.setRootPath(getRootPath(context));
                    record.setOrder((int) (file.lastModified() / 1000));
                    record.setSaveTime(file.lastModified());
                    records.add(record);
                }
            }
        }
        records.addAll(scanLocalFiles(context));
        return records;
    }

    public static List<DownloadRecord> scanLocalFiles(Context context) {
        List<DownloadRecord> records = new ArrayList<>();
        List<FileEntity> fileEntities = FilesUtilsKt.scanLocalFiles(context);
        if (CollectionUtil.isNotEmpty(fileEntities)) {
            for (FileEntity fileEntity : fileEntities) {
                DownloadRecord record = new DownloadRecord();
                String fileName = fileEntity.getName();
                record.setFileName(fileName);
                String[] names = fileName.split("@@");
                String film = null;
                if (names.length > 1) {
                    film = names[0];
                    fileName = names[names.length - 1];
                }
                record.setSourcePageTitle(fileName);
                record.setSourcePageUrl(fileEntity.getPath());
                record.setFileExtension(FileUtil.getExtension(fileName));
                record.setStatus(DownloadStatusEnum.SUCCESS.getCode());
                record.setSize(fileEntity.getLength());
                record.setFailedReason(fileEntity.getUri());
                record.setTotalDownloaded(record.getSize());
                record.setVideoType("normal");
                if (StringUtil.isEmpty(film)) {
                    film = smartFilm(fileName);
                }
                record.setFilm(film);
                try {
                    record.setRootPath(new File(fileEntity.getPath()).getParent());
                } catch (Exception e) {
                    e.printStackTrace();
                }
                record.setOrder((int) (fileEntity.getTimestamp() / 1000));
                record.setSaveTime(fileEntity.getTimestamp());
                records.add(record);
            }
        }
        return records;
    }

    public static String smartFilm(String fileName) {
        return smartFilm(fileName, false);
    }

    public static String smartFilm(String fileName, boolean includeVideo) {
        if (fileName == null || fileName.isEmpty() || !DownloadConfig.smartFilm) {
            return "";
        }
        if (UrlDetector.isVideoOrMusic(fileName)) {
            if (!UrlDetector.isMusic(fileName)) {
                return includeVideo ? "视频" : "";
            } else {
                return "音乐/音频";
            }
        } else if (UrlDetector.isImage(fileName)) {
            return "图片";
        } else if (fileName.contains(".zip") || fileName.contains(".rar") || fileName.contains(".7z") || fileName.contains(".tar") || fileName.contains(".gz")) {
            return "压缩包";
        } else if (fileName.contains(".txt") || fileName.contains(".epub") || fileName.contains(".azw3") || fileName.contains(".mobi") || fileName.contains(".pdf")
                || fileName.contains(".doc") || fileName.contains(".xls") || fileName.contains(".json")) {
            return "文档/电子书";
        } else if (fileName.contains(".apk") || fileName.contains(".exe") || fileName.contains(".hap") || fileName.contains(".msi") || fileName.contains(".dmg")) {
            return "安装包";
        }
        return "其它格式";
    }


    public static boolean isSystemFilm(String film) {
        return "音乐/音频".equals(film) || "图片".equals(film) || "压缩包".equals(film) || "文档/电子书".equals(film) || "安装包".equals(film) || "其它格式".equals(film);
    }

    private static boolean startDownloadUseIDM(Context context, String name, String url) {
        try {
            Intent localIntent = new Intent();
            localIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);//268435456
            localIntent.setType("text/plain");
            localIntent.setAction("android.intent.action.SEND");
            localIntent.putExtra("android.intent.extra.TEXT", url);
            localIntent.putExtra("title", name);
            localIntent.setComponent(new ComponentName("idm.internet.download.manager.plus", "idm.internet.download.manager.Downloader"));
            context.startActivity(localIntent);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            ToastMgr.shortBottomCenter(context, "调用IDM+下载器失败！" + e.getMessage());
        }
        return true;
    }

    private static boolean startDownloadUseNick(Context context, String name, String url) {
        try {
            Intent localIntent = new Intent();
            localIntent.setFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED | Intent.FLAG_ACTIVITY_NEW_TASK);
            localIntent.setType("text/plain");
            localIntent.setAction("android.intent.action.VIEW");
            localIntent.putExtra("android.intent.extra.TEXT", url);
            localIntent.putExtra("title", name);
            localIntent.setComponent(new ComponentName("com.nick.download", "com.nick.download.DownMainActivity"));
            context.startActivity(localIntent);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            ToastMgr.shortBottomCenter(context, "调用全能下载器失败！" + e.getMessage());
        }
        return true;
    }

    private static boolean startDownloadUseFDM(Context context, String name, String url) {
        try {
            Intent localIntent = new Intent();
            localIntent.setFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED | Intent.FLAG_ACTIVITY_NEW_TASK);
            localIntent.setType("text/plain");
            localIntent.setAction("android.intent.action.SEND");
            localIntent.putExtra("android.intent.extra.TEXT", url);
            localIntent.putExtra("title", name);
            localIntent.setComponent(new ComponentName("org.freedownloadmanager.fdm", "org.freedownloadmanager.fdm.MyActivity"));
            context.startActivity(localIntent);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            ToastMgr.shortBottomCenter(context, "调用Free Download Manager失败！" + e.getMessage());
        }
        return true;
    }
}

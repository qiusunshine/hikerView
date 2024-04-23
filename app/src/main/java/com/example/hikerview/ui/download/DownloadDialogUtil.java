package com.example.hikerview.ui.download;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.text.TextUtils;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatEditText;

import com.annimon.stream.function.Consumer;
import com.example.hikerview.R;
import com.example.hikerview.model.DownloadRecord;
import com.example.hikerview.service.parser.HttpParser;
import com.example.hikerview.ui.ActivityManager;
import com.example.hikerview.ui.Application;
import com.example.hikerview.ui.browser.model.UrlDetector;
import com.example.hikerview.ui.browser.util.CollectionUtil;
import com.example.hikerview.ui.download.util.UUIDUtil;
import com.example.hikerview.ui.download.util.VideoFormatUtil;
import com.example.hikerview.ui.thunder.ThunderManager;
import com.example.hikerview.ui.video.VideoPlayerActivity;
import com.example.hikerview.ui.webdlan.LocalServerParser;
import com.example.hikerview.utils.DisplayUtil;
import com.example.hikerview.utils.FileUtil;
import com.example.hikerview.utils.HeavyTaskUtil;
import com.example.hikerview.utils.PreferenceMgr;
import com.example.hikerview.utils.StringUtil;
import com.example.hikerview.utils.ThreadTool;
import com.example.hikerview.utils.ToastMgr;
import com.example.hikerview.utils.view.DialogUtil;
import com.king.app.updater.AppUpdater;
import com.lxj.xpopup.XPopup;
import com.lxj.xpopup.core.BasePopupView;

import org.apache.commons.lang3.StringUtils;
import org.litepal.LitePal;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import timber.log.Timber;

/**
 * 作者：By 15968
 * 日期：On 2019/12/4
 * 时间：At 23:29
 */
public class DownloadDialogUtil {
    private static final String TAG = "DownloadDialogUtil";

    private static final String[] System_DOWNLOAD = new String[]{".epub", ".zip", ".rar", ".txt", ".exe", ".pdf", ".doc", ".wps", ".ttf"
            , ".otf", ".sfnt", ".woff2", ".woff", ".gz", ".7z",
            ".docx", ".ppt", ".pptx", ".ass", ".srt", ".vtt", ".xls", ".xlsx", ".tar", ".hiker", ".json", ".apk", ".msi", ".torrent", ".md"};
    public static final List<AppUpdater> appTasks = Collections.synchronizedList(new ArrayList<>());

    /**
     * 默认下载器和downloadManager都不能下载的，只能调用第三方的软件
     *
     * @param title
     * @param url
     * @return
     */
    private static boolean cannotDownload(String title, String url) {
        if (StringUtil.isNotEmpty(url)) {
            if (StringUtil.isCannotHandleScheme(url)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 不能用默认下载器，只能调用Android的downloadManager
     *
     * @param title
     * @param url
     * @return
     */
    public static boolean useSystemDownload(String title, String url) {
        if (StringUtil.isNotEmpty(title)) {
            for (String s : System_DOWNLOAD) {
                if (title.endsWith(s)) {
                    return true;
                }
            }
        }
        if (StringUtil.isNotEmpty(url) && !UrlDetector.isVideoOrMusic(url)) {
            String ext = url.split("/")[url.split("/").length - 1];
            for (String s : System_DOWNLOAD) {
                if (ext.contains(s)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static void removeAppDownloadTask(AppUpdater appUpdater) {
        synchronized (appTasks) {
            appTasks.remove(appUpdater);
            if (appTasks.size() > 0) {
                appTasks.get(0).start();
            }
        }
    }

    public static String getFileName(String url) {
        if (StringUtil.isEmpty(url)) {
            return url;
        }
        String[] s = url.split("#");
        url = s[0];
        s = url.split("\\?");
        url = s[0];
        int start = url.lastIndexOf("/");
        if (start != -1 && start < url.length() - 1) {
            return url.substring(start + 1);
        } else {
            //没有/直接取base64
            return new String(Base64.encode(url.getBytes(), Base64.NO_WRAP));
        }
    }

    public static boolean isApk(String fileName, String mimetype) {
        if (mimetype == null) {
            mimetype = "";
        }
        if ("application/vnd.android.package-archive".equals(mimetype)) {
            return true;
        }
        try {
            if (StringUtil.isNotEmpty(fileName) && fileName.endsWith(".apk") && "application/octet-stream".endsWith(mimetype)) {
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static void showEditDialog(Activity context, @Nullable String mTitle, @Nullable String mUrl) {
        showEditDialog(context, mTitle, mUrl, null);
    }

    public static void showEditDialog(Activity context, @Nullable String mTitle, @Nullable String mUrl, @Nullable String film) {
        showEditDialog(context, mTitle, mUrl, film, null);
    }

    public static void showEditDialog(Activity context, @Nullable String mTitle, @Nullable String mUrl,
                                      @Nullable String film, @Nullable String mimetype) {
        showEditDialog(context, mTitle, mUrl, film, mimetype, null);
    }

    public static void showEditDialog(Activity context, @Nullable String mTitle, @Nullable String mUrl,
                                      @Nullable String film, @Nullable String mimetype, @Nullable Consumer<DownloadTask> interceptor) {
        showEditDialog(context, mTitle, mUrl, film, mimetype, -1, interceptor, null);
    }

    public static void showEditDialog(Activity context, @Nullable String mTitle, @Nullable String mUrl,
                                      @Nullable String film, @Nullable String mimetype, long size, @Nullable Consumer<DownloadTask> interceptor,
                                      @Nullable Consumer<String> videoPlayer) {
        showEditDialog(context, mTitle, mUrl, film, mimetype, size, interceptor, videoPlayer, null);
    }

    public static void showEditDialog(Activity context, @Nullable String mTitle, @Nullable String mUrl,
                                      @Nullable String film, @Nullable String mimetype, long size, @Nullable Consumer<DownloadTask> interceptor,
                                      @Nullable Consumer<String> videoPlayer, @Nullable String ext) {
        String originalUrl = mUrl;
        if (StringUtil.isNotEmpty(mUrl)) {
            mUrl = DownloadChooser.getCanDownloadUrl(context, mUrl);
            mUrl = HttpParser.getThirdDownloadSource(mUrl);
        }
        if (StringUtil.isNotEmpty(mTitle) && mTitle.equals(mUrl)) {
            //尝试提取一下文件名
            mTitle = getFileName(mTitle);
        }
        if (StringUtil.isNotEmpty(mTitle) && mTitle.startsWith("http")) {
            mTitle = getFileName(mTitle);
        }
        int downloader = PreferenceMgr.getInt(context, "defaultDownloader", 0);
        if (downloader != 0 && StringUtil.isNotEmpty(mUrl)) {
            DownloadChooser.startDownload(context, downloader, mTitle, mUrl, film, originalUrl);
            return;
        }

        if (cannotDownload(mTitle, mUrl)) {
            DownloadChooser.startDownloadByThird(context, mTitle, mUrl, film, originalUrl);
            return;
        }
        final View view1 = LayoutInflater.from(context).inflate(R.layout.view_dialog_download_add, null, false);
        CheckBox download_add_auto_clear = view1.findViewById(R.id.download_add_auto_clear);
        final AppCompatEditText titleE = view1.findViewById(R.id.download_add_title);
        final AppCompatEditText urlE = view1.findViewById(R.id.download_add_url);
        final AppCompatEditText suffixE = view1.findViewById(R.id.download_add_suffix);
        if (StringUtil.isNotEmpty(ext)) {
            suffixE.setText(ext);
        }
        TextView sizeView = view1.findViewById(R.id.download_add_size);
        if (size > 0) {
            sizeView.setVisibility(View.VISIBLE);
            sizeView.setText((FileUtil.getFormatedFileSize(size) + "（" + size + "个字节）"));
        }
        boolean isVideoOrMusic = UrlDetector.isVideoOrMusic(mUrl) || UrlDetector.isVideoOrMusic(mTitle) || UrlDetector.isTs(mUrl, mTitle);
        view1.findViewById(R.id.download_add_suffix_gen).setOnClickListener(v -> {
            String title = titleE.getText().toString();
            String url = urlE.getText().toString();
            if (TextUtils.isEmpty(title) && TextUtils.isEmpty(url)) {
                ToastMgr.shortCenter(context, "文件名和地址均为空，无法提取后缀");
            }
            VideoFormat videoFormat = VideoFormatUtil.getVideoFormat(title, url);
            if (videoFormat != null && StringUtil.isNotEmpty(videoFormat.getName())) {
                suffixE.setText(videoFormat.getName());
            } else {
                ToastMgr.shortCenter(context, "提取文件后缀失败");
            }
        });
        if (StringUtil.isNotEmpty(mTitle)) {
            titleE.setText(mTitle);
        }
        if (StringUtil.isNotEmpty(mUrl)) {
            urlE.setText(mUrl);
        }
        String finalMUrl = mUrl;
        titleE.setTag(originalUrl);
        view1.findViewById(R.id.info).setOnClickListener(v -> new XPopup.Builder(context)
                .borderRadius(DisplayUtil.dpToPx(context, 16))
                .asInputConfirm("完整链接", null, (String) titleE.getTag(), null, titleE::setTag, null, R.layout.xpopup_confirm_input).show());
        DialogInterface.OnClickListener confirm = (dialog, which) -> {
            String title = titleE.getText().toString();
            String url = urlE.getText().toString();
            if (TextUtils.isEmpty(title) || TextUtils.isEmpty(url)) {
                ToastMgr.shortCenter(context, "请输入完整信息");
            } else if (url.startsWith("video://")) {
                ToastMgr.longBottomCenter(context, "当前地址只能在播放器界面嗅探到真实视频后才能下载");
            } else {
                dialog.dismiss();
                String rUrl = StringUtils.equals(url, finalMUrl) ? (String) titleE.getTag() : url;
                if (StringUtil.isEmpty(rUrl)) {
                    rUrl = url;
                }
                rUrl = rUrl.split("##")[0];
                String taskId = UUIDUtil.genUUID();
                //边下边播且自动删除，那就设置为临时任务
                boolean autoClear = which == 100 && download_add_auto_clear.isChecked();
                if (autoClear) {
                    taskId = taskId + "@temp";
                }
                DownloadTask downloadTask = new DownloadTask(
                        taskId, null, null, null, rUrl, rUrl, title, 0L);
                downloadTask.setSuffix(suffixE.getText().toString());
                if (!context.getResources().getString(R.string.app_name).equals(film)) {
                    downloadTask.setFilm(film);
                }
                if (interceptor != null) {
                    interceptor.accept(downloadTask);
                }
                DownloadManager.instance().addTask(downloadTask);
                ToastMgr.shortBottomCenter(context, "已加入下载队列");
                if (which == 100) {
                    //边下边播
                    if (context instanceof VideoPlayerActivity) {
                        ((VideoPlayerActivity) context).setExcludeTaskId(taskId);
                    }
                    BasePopupView loading = new XPopup.Builder(context)
                            .borderRadius(DisplayUtil.dpToPx(context, 16))
                            .asLoading("加载中，请稍候")
                            .show();
                    String finalTaskId = taskId;
                    HeavyTaskUtil.executeNewTask(() -> {
                        try {
                            int count = 0;
                            while (count < 75) {
                                DownloadRecord record = LitePal.where("taskId = ?", finalTaskId).findFirst(DownloadRecord.class);
                                if (record != null) {
                                    if (DownloadStatusEnum.RUNNING.getCode().equals(record.getStatus()) && record.getTotalDownloaded() > 1) {
                                        //下载中，唤起播放器
                                        Thread.sleep(100);
                                        ThreadTool.INSTANCE.runOnUI(() -> DownloadRecordsFragment.playWinDownloading(ActivityManager.getInstance().getCurrentActivity(), record));
                                        break;
                                    } else if (DownloadStatusEnum.SAVING.getCode().equals(record.getStatus())) {
                                        //保存中，那就等等
                                        count--;
                                    } else if (DownloadStatusEnum.SUCCESS.getCode().equals(record.getStatus())) {
                                        //成功了，唤起播放器
                                        ThreadTool.INSTANCE.runOnUI(() -> {
                                            Context ctx = ActivityManager.getInstance().getCurrentActivity();
                                            String u = LocalServerParser.getRealUrl(ctx, record);
                                            DownloadRecordsFragment.playVideoChapter(ctx, record, u);
                                        });
                                        break;
                                    } else if (DownloadStatusEnum.BREAK.getCode().equals(record.getStatus())
                                            || DownloadStatusEnum.ERROR.getCode().equals(record.getStatus())
                                            || DownloadStatusEnum.CANCEL.getCode().equals(record.getStatus())) {
                                        //失败或者中断
                                        ToastMgr.shortBottomCenter(Application.getContext(), DownloadStatusEnum.getByCode(record.getStatus()).getDesc());
                                        break;
                                    }
                                }
                                Thread.sleep(200);
                                count++;
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            ToastMgr.shortBottomCenter(Application.getContext(), "出现错误：" + e.getMessage());
                        } finally {
                            ThreadTool.INSTANCE.runOnUI(loading::dismiss);
                        }
                    });
                }
            }
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .setTitle("添加文件下载")
                .setView(view1)
                .setCancelable(true)
                .setPositiveButton("下载", confirm).setNegativeButton("取消", (dialog, which) -> dialog.dismiss());
        if (UrlDetector.hasKey(".torrent", mUrl, mTitle)) {
            //种子文件云播
            isVideoOrMusic = false;
            builder.setNeutralButton("直接云播", (dialog, which) -> {
                String title = titleE.getText().toString();
                String url = urlE.getText().toString();
                if (TextUtils.isEmpty(title) || TextUtils.isEmpty(url)) {
                    ToastMgr.shortCenter(context, "请输入完整信息");
                } else if (url.startsWith("video://")) {
                    ToastMgr.longBottomCenter(context, "当前地址只能在播放器界面嗅探到真实视频后才能下载");
                } else {
                    dialog.dismiss();
                    String rUrl = StringUtils.equals(url, finalMUrl) ? (String) titleE.getTag() : url;
                    if (StringUtil.isEmpty(rUrl)) {
                        rUrl = url;
                    }
                    rUrl = rUrl.split("##")[0];
                    String taskId = UUIDUtil.genUUID();
                    DownloadTask downloadTask = new DownloadTask(
                            taskId, null, null, null, rUrl, rUrl, title, 0L);
                    downloadTask.setSuffix("torrent");
                    if (!context.getResources().getString(R.string.app_name).equals(film)) {
                        downloadTask.setFilm(film);
                    }
                    if (interceptor != null) {
                        interceptor.accept(downloadTask);
                    }
                    DownloadManager.instance().addTask(downloadTask);
                    BasePopupView loading = new XPopup.Builder(context)
                            .borderRadius(DisplayUtil.dpToPx(context, 16))
                            .asLoading("加载中，请稍候")
                            .show();
                    HeavyTaskUtil.executeNewTask(() -> {
                        try {
                            int count = 0;
                            while (count < 75) {
                                DownloadRecord record = LitePal.where("taskId = ?", taskId).findFirst(DownloadRecord.class);
                                if (record != null) {
                                    if (DownloadStatusEnum.SUCCESS.getCode().equals(record.getStatus())) {
                                        //成功了，唤起播放器
                                        ThreadTool.INSTANCE.runOnUI(() -> {
                                            Context ctx = ActivityManager.getInstance().getCurrentActivity();
                                            String p = DownloadRecordsFragment.getLocalPath(record);
                                            if (StringUtil.isNotEmpty(p) && p.endsWith(".torrent")) {
                                                ThunderManager.INSTANCE.startDownloadMagnet(ctx, p);
                                            }
                                        });
                                        break;
                                    } else if (DownloadStatusEnum.BREAK.getCode().equals(record.getStatus())
                                            || DownloadStatusEnum.ERROR.getCode().equals(record.getStatus())
                                            || DownloadStatusEnum.CANCEL.getCode().equals(record.getStatus())) {
                                        //失败或者中断
                                        ToastMgr.shortBottomCenter(Application.getContext(), DownloadStatusEnum.getByCode(record.getStatus()).getDesc());
                                        break;
                                    }
                                }
                                Thread.sleep(200);
                                count++;
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            ToastMgr.shortBottomCenter(Application.getContext(), "出现错误：" + e.getMessage());
                        } finally {
                            ThreadTool.INSTANCE.runOnUI(loading::dismiss);
                        }
                    });
                }
            });
        } else if (videoPlayer != null && isVideoOrMusic) {
            builder.setNeutralButton("播放", (dialog, which) -> {
                videoPlayer.accept(finalMUrl);
            });
        }
        AlertDialog alertDialog = builder.create();
        if (StringUtil.isNotEmpty(mUrl) && (isApk(mTitle, mimetype) || mUrl.contains(".apk"))) {
            TextView msgView = view1.findViewById(R.id.download_add_msg);
            msgView.setVisibility(View.VISIBLE);
        } else if ((isVideoOrMusic || context instanceof VideoPlayerActivity ||
                (StringUtil.isNotEmpty(mTitle) && (mTitle.contains(".mp4") || mTitle.contains(".mkv")))
        ) && !UrlDetector.isMusic(mUrl)) {
            TextView download_add_down_play = view1.findViewById(R.id.download_add_down_play);
            view1.findViewById(R.id.download_add_down_play_bg).setVisibility(View.VISIBLE);
            boolean playAutoClear = PreferenceMgr.getBoolean(context, "download", "playAutoClear", false);
            download_add_auto_clear.setChecked(playAutoClear);
            download_add_auto_clear.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    ToastMgr.shortBottomCenter(context, "退出播放器会自动删除下载任务和文件");
                }
                PreferenceMgr.put(context, "download", "playAutoClear", isChecked);
            });
            download_add_down_play.setOnClickListener(v -> confirm.onClick(alertDialog, 100));
        }
        View download_add_others = view1.findViewById(R.id.download_add_others);
        download_add_others.setOnClickListener(v -> {
            String title = titleE.getText().toString();
            String url = urlE.getText().toString();
            if (TextUtils.isEmpty(title) || TextUtils.isEmpty(url)) {
                ToastMgr.shortBottomCenter(context, "请输入完整信息");
            } else {
                alertDialog.dismiss();
                DownloadChooser.startDownloadByThird(context, title, url, film, (String) titleE.getTag());
            }
        });
        DialogUtil.INSTANCE.showAsCard(context, alertDialog);
        String tt = mTitle;
        if (StringUtil.isNotEmpty(tt)) {
            HeavyTaskUtil.executeNewTask(() -> {
                try {
                    List<DownloadRecord> records = LitePal.where("status = ?", DownloadStatusEnum.ERROR.getCode()).find(DownloadRecord.class);
                    if (CollectionUtil.isNotEmpty(records)) {
                        String t = FileUtil.getSimpleName(tt).replace(" ", "-");
                        float max = 0f;
                        DownloadRecord record = null;
                        String secondDom = StringUtil.getSecondDom(originalUrl);
                        String num = StringUtil.keepNums(t);
                        for (DownloadRecord record1 : records) {
                            if ("player/m3u8".equals(record1.getVideoType())) {
                                continue;
                            }
                            //暂时只考虑二级域名相同的情况
                            String sd = StringUtil.getSecondDom(record1.getSourcePageUrl());
                            if (!StringUtils.equals(sd, secondDom)) {
                                continue;
                            }
                            String sourcePageTitle = record1.getSourcePageTitle();
                            String num1 = StringUtil.keepNums(sourcePageTitle);
                            //标题的数字要一样，保证第1集不和第2集混一起
                            if (!StringUtils.equals(num, num1)) {
                                continue;
                            }
                            float like = StringUtil.levenshtein(sourcePageTitle, t);
                            if (like > max) {
                                max = like;
                                record = record1;
                            }
                        }
                        Timber.d("max levenshtein = %s", max);
                        if (max > 0.7f) {
                            DownloadRecord finalRecord = record;
                            String temp = DownloadConfig.rootPath + File.separator + record.getFileName() + "." + record.getFileExtension() + ".temp";
                            File tempDir = new File(temp);
                            File[] files = tempDir.isDirectory() ? tempDir.listFiles() : null;
                            if (!tempDir.exists() || files == null || files.length == 0) {
                                return;
                            }
                            ThreadTool.INSTANCE.runOnUI(() -> new XPopup.Builder(ActivityManager.getInstance().getCurrentActivity())
                                    .asConfirm("温馨提示", "检测到疑似同名但失败的下载任务（" + finalRecord.getSourcePageTitle() + "），是否更新该任务的下载地址为新地址并继续下载？", () -> {
                                        alertDialog.dismiss();
                                        Activity ctx = ActivityManager.getInstance().getCurrentActivity();
                                        DownloadRecordsFragment.showUpdateDownloadUrlPopup(ctx, finalRecord, originalUrl);
                                    }).show());
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
    }

    public static void downloadNow(Activity context, String mTitle, String mUrl, @Nullable String ext) {
        String originalUrl = mUrl;
        if (StringUtil.isNotEmpty(mUrl)) {
            mUrl = DownloadChooser.getCanDownloadUrl(context, mUrl);
            mUrl = HttpParser.getThirdDownloadSource(mUrl);
        }
        if (StringUtil.isNotEmpty(mTitle) && mTitle.equals(mUrl)) {
            //尝试提取一下文件名
            mTitle = getFileName(mTitle);
        }
        if (StringUtil.isNotEmpty(mTitle) && mTitle.startsWith("http")) {
            mTitle = getFileName(mTitle);
        }
        int downloader = PreferenceMgr.getInt(context, "defaultDownloader", 0);
        if (downloader != 0 && StringUtil.isNotEmpty(mUrl)) {
            DownloadChooser.startDownload(context, downloader, mTitle, mUrl, null, originalUrl);
            return;
        }
        if (cannotDownload(mTitle, mUrl)) {
            DownloadChooser.startDownloadByThird(context, mTitle, mUrl, null, originalUrl);
            return;
        }
        String rUrl = originalUrl.split("##")[0];
        DownloadTask downloadTask = new DownloadTask(
                UUIDUtil.genUUID(), null, null, null, rUrl, rUrl, mTitle, 0L);
        downloadTask.setSuffix(ext);
        downloadTask.setFilm(null);
        DownloadManager.instance().addTask(downloadTask);
        ToastMgr.shortBottomCenter(context, "已加入下载队列");
    }

    public static String getApkDownloadPath(Context context) {
        return DownloadChooser.getRootPath(context);
    }
}

package com.example.hikerview.ui.download;

import android.app.Activity;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.example.hikerview.R;
import com.example.hikerview.ui.browser.util.HttpRequestUtil;
import com.example.hikerview.ui.download.util.UUIDUtil;
import com.example.hikerview.ui.video.PlayerChooser;
import com.example.hikerview.utils.PreferenceMgr;
import com.example.hikerview.utils.StringUtil;
import com.example.hikerview.utils.ToastMgr;
import com.king.app.updater.AppUpdater;
import com.king.app.updater.http.OkHttpManager;
import com.lxj.xpopup.XPopup;

/**
 * 作者：By 15968
 * 日期：On 2019/12/4
 * 时间：At 23:29
 */
public class DownloadDialogUtil {
    private static final String TAG = "DownloadDialogUtil";

    public static void showEditApkDialog(Activity context, @Nullable String mTitle, @Nullable String mUrl) {
        String shortUrl = mUrl;
        if (StringUtil.isNotEmpty(shortUrl) && shortUrl.length() > 70) {
            shortUrl = shortUrl.substring(0, 70) + "...";
        }
        new XPopup.Builder(context)
                .asConfirm("温馨提示", "确定下载安装来自下面这个网址的软件？" + shortUrl + "（注意自行识别软件安全性）", () -> {
                    new AppUpdater.Builder()
                            .serUrl(mUrl)
                            .setShowNotification(true)
                            .setShowPercentage(true)
                            .build(context)
                            .setHttpManager(OkHttpManager.getInstance())
                            .start();
                    ToastMgr.shortBottomCenter(context, "已开始下载，通知栏可查看下载进度");
                }).show();
    }

    public static void showEditDialog(Activity context, @Nullable String mTitle, @Nullable String mUrl) {
        if(StringUtil.isNotEmpty(mUrl)){
            mUrl = DownloadChooser.getCanDownloadUrl(context, mUrl);
            mUrl = PlayerChooser.getThirdPlaySource(mUrl);
        }
        int downloader = PreferenceMgr.getInt(context, "defaultDownloader", 0);
        if (downloader != 0) {
            DownloadChooser.startDownload(context, downloader, mTitle, mUrl);
            return;
        }
        if (StringUtil.isNotEmpty(mUrl) && mUrl.contains(".apk")) {
            String ext = HttpRequestUtil.getFileExtensionFromUrl(mUrl);
            if ("apk".equalsIgnoreCase(ext)) {
                showEditApkDialog(context, mTitle, mUrl);
            } else {
                DownloadChooser.startDownloadByThird(context, mTitle, mUrl);
            }
            return;
        }
        final View view1 = LayoutInflater.from(context).inflate(R.layout.view_dialog_download_add, null, false);
        final EditText titleE = view1.findViewById(R.id.download_add_title);
        final EditText urlE = view1.findViewById(R.id.download_add_url);
        if (StringUtil.isNotEmpty(mTitle)) {
            titleE.setText(mTitle);
        }
        if (StringUtil.isNotEmpty(mUrl)) {
            urlE.setText(mUrl);
        }
        final AlertDialog alertDialog = new AlertDialog.Builder(context)
                .setTitle("添加文件下载")
                .setView(view1)
                .setCancelable(true)
                .setPositiveButton("下载", (dialog, which) -> {
                    String title = titleE.getText().toString();
                    String url = urlE.getText().toString();
                    if (TextUtils.isEmpty(title) || TextUtils.isEmpty(url)) {
                        ToastMgr.shortBottomCenter(context, "请输入完整信息");
                    } else {
                        dialog.dismiss();
                        DownloadTask downloadTask = new DownloadTask(
                                UUIDUtil.genUUID(), null, null, null, url, url, title, 0L);
                        DownloadManager.instance().addTask(downloadTask);
                        ToastMgr.shortBottomCenter(context, "已加入下载队列");
                    }
                }).setNegativeButton("取消", (dialog, which) -> dialog.dismiss()).create();
        View download_add_others = view1.findViewById(R.id.download_add_others);
        download_add_others.setOnClickListener(v -> {
            String title = titleE.getText().toString();
            String url = urlE.getText().toString();
            if (TextUtils.isEmpty(title) || TextUtils.isEmpty(url)) {
                ToastMgr.shortBottomCenter(context, "请输入完整信息");
            } else {
                alertDialog.dismiss();
                DownloadChooser.startDownloadByThird(context, title, url);
            }
        });
        alertDialog.show();
    }
}

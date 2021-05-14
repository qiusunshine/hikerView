package com.example.hikerview.utils;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.text.TextUtils;
import android.webkit.MimeTypeMap;

import androidx.core.content.FileProvider;

import java.io.File;
import java.net.URISyntaxException;

import timber.log.Timber;

/**
 * 作者：By hdy
 * 日期：On 2017/10/24
 * 时间：At 19:44
 */

public class ShareUtil {
    public static void shareText(Context context, String str) {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SEND);
        if (str != null) {
            intent.putExtra(Intent.EXTRA_TEXT, str);
        } else {
            intent.putExtra(Intent.EXTRA_TEXT, "");
        }
        intent.setType("text/plain");
        try {
            context.startActivity(Intent.createChooser(intent, "分享"));
        } catch (Exception e) {
            e.printStackTrace();
            ToastMgr.shortBottomCenter(context, "系统故障：" + e.getMessage());
        }
    }

    public static void findVideoPlayerToDeal(Context context, String url) {
        if (TextUtils.isEmpty(url)) {
            ToastMgr.shortBottomCenter(context, "此链接有问题，不过我们为您复制到了剪贴板");
            ClipboardUtil.copyToClipboard(context, url);
            return;
        }
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);//允许临时的读和写
        Uri content_url = FilesInAppUtil.getUri(context, url);
        intent.setDataAndType(content_url, "video/*");
        context.startActivity(Intent.createChooser(intent, "请选择应用"));
    }

    public static void findChooserToDeal(Context context, String url) {
        if (url.startsWith("intent:")) {
            try {
                Intent intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
                if (intent != null) {
                    PackageManager packageManager = context.getPackageManager();
                    ResolveInfo info = packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
                    if (info != null) {
                        context.startActivity(intent);
                    } else {
                        ToastMgr.shortBottomCenter(context, "找不到对应的应用");
                    }
                }
            } catch (URISyntaxException e) {
                Timber.e(e);
            }
            return;
        }
        if (TextUtils.isEmpty(url)) {
            ToastMgr.shortBottomCenter(context, "此链接有问题，不过我们为您复制到了剪贴板");
            ClipboardUtil.copyToClipboard(context, url);
            return;
        }
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);//允许临时的读和写
        Uri content_url = FilesInAppUtil.getUri(context, url);
        intent.setData(content_url);
        context.startActivity(Intent.createChooser(intent, "请选择应用"));
    }

    public static void findToDealFileByPath(Context context, String path) {
        if (TextUtils.isEmpty(path)) {
            ToastMgr.shortBottomCenter(context, "此链接有问题，不过我们为您复制到了剪贴板");
            ClipboardUtil.copyToClipboard(context, path);
            return;
        }
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);//允许临时的读和写
        Uri content_path = FileProvider.getUriForFile(context, "com.example.hikerview.provider", new File(path));
        intent.setData(content_path);
        context.startActivity(Intent.createChooser(intent, "请选择应用"));
    }

    public static void findChooserToSend(Context context, String url) {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SEND);
        intent.setType("*/*");
        Uri content_url = FilesInAppUtil.getUri(context, url);
        intent.putExtra(Intent.EXTRA_STREAM, content_url);
        context.startActivity(Intent.createChooser(intent, "请选择应用"));
    }

    public static void chooserMediaPlayer(Context context, String url) {
        if (url == null || url.length() < 10) {
            ToastMgr.shortBottomCenter(context, "此链接有问题，不过我们为您复制到了剪贴板");
            ClipboardUtil.copyToClipboard(context, url);
            return;
        }
        String extension = MimeTypeMap.getFileExtensionFromUrl(url);
        String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        Intent mediaIntent = new Intent(Intent.ACTION_VIEW);
        mediaIntent.setDataAndType(Uri.parse(url), mimeType);
        context.startActivity(Intent.createChooser(mediaIntent, "请选择播放器"));
    }

    public static void toWeChatScan(Context context) {
        try {
            Uri uri = Uri.parse("weixin://");
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            context.startActivity(intent);
        } catch (Exception e) {
            ToastMgr.shortBottomCenter(context, "打开微信失败！");
        }
    }

    public static void startUrl(Context context, String url) {
        try {
            Uri uri = Uri.parse(url);
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            context.startActivity(intent);
        } catch (Exception e) {
            ToastMgr.shortBottomCenter(context, "打开失败！");
        }
    }
}

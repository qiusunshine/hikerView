package com.example.hikerview.utils;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.example.hikerview.ui.Application;
import com.example.hikerview.ui.browser.model.JSManager;
import com.example.hikerview.ui.browser.util.CollectionUtil;
import com.example.hikerview.ui.view.colorDialog.PromptDialog;
import com.lxj.xpopup.XPopup;

import org.litepal.LitePal;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

/**
 * 作者：By 15968
 * 日期：On 2019/12/1
 * 时间：At 14:51
 */
public class BackupUtil {

    private static final String TAG = "BackupUtil";

    public static void backupDBAndJs(Context context, boolean silence) {
        BackupUtil.backupDB(context, true);
        List<String> filePaths = new ArrayList<>();
        String dbPath = UriUtils.getRootDir(context) + File.separator + "backup" + File.separator + BackupUtil.getDBFileNameWithVersion();
        filePaths.add(dbPath);
        //第三方规则的文件
        String rulesZipFilePath = UriUtils.getCacheDir(context) + File.separator + "rules.zip";
        List<String> ruleFiles = scanRuleFiles();
        if (CollectionUtil.isNotEmpty(ruleFiles)) {
            try {
                FileUtil.deleteFile(rulesZipFilePath);
                if (ZipUtils.zipFiles(ruleFiles, rulesZipFilePath)) {
                    filePaths.add(rulesZipFilePath);
                }
            } catch (Exception e) {
                Timber.e(e, "rulesZip: ");
            }
        }
        String zipFilePath = UriUtils.getRootDir(context) + File.separator + "backup" + File.separator + "hiker.zip";
        try {
            FileUtil.deleteFile(zipFilePath);
            if (ZipUtils.zipFiles(filePaths, zipFilePath)) {
                if (!silence) {
                    ToastMgr.shortBottomCenter(context, "备份成功！");
                    ShareUtil.findChooserToSend(context, "file://" + zipFilePath);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            new Handler(Looper.getMainLooper())
                    .post(() -> Toast
                            .makeText(Application.application.getApplicationContext(), e.getLocalizedMessage(), Toast.LENGTH_SHORT)
                            .show());
        }
    }

    public static List<String> scanRuleFiles() {
        List<String> ruleFiles = new ArrayList<>();
        String rulesPath = UriUtils.getRootDir(Application.getContext()) + File.separator + "rules";
        File file = new File(rulesPath);
        if (!file.exists()) {
            return ruleFiles;
        }
        File[] files = file.listFiles();
        if (files != null && files.length > 0) {
            for (File file1 : files) {
                ruleFiles.add(file1.getAbsolutePath());
            }
        }
        return ruleFiles;
    }

    public static void backupDB(Context context, boolean silence) {
        File dbFile = context.getDatabasePath(getDBFileName());
        Log.d(TAG, "backupDB: path==>" + dbFile.getAbsolutePath());
        if (dbFile.exists()) {
            File exportDir = new File(UriUtils.getRootDir(context) + File.separator + "backup");
            if (!exportDir.exists()) {
                boolean ok = exportDir.mkdirs();
                if (!ok) {
                    ToastMgr.shortBottomCenter(context, "创建备份文件夹失败");
                    return;
                }
            }
            File backup = new File(exportDir, getDBFileNameWithVersion());
            try {
                if (backup.exists()) {
                    boolean del = backup.delete();
                    if (!del) {
                        ToastMgr.shortBottomCenter(context, "删除旧数据库文件失败");
                        return;
                    }
                }
                boolean ok = backup.createNewFile();
                if (!ok) {
                    ToastMgr.shortBottomCenter(context, "创建新数据库文件失败");
                    return;
                }
                FileUtil.copyFile(dbFile.getAbsolutePath(), backup.getAbsolutePath());
                if (!silence) {
                    ToastMgr.shortBottomCenter(context, "备份成功！");
                    ShareUtil.findChooserToSend(context, "file://" + backup.getAbsolutePath());
                }
            } catch (Exception e) {
                DebugUtil.showErrorMsg((Activity) context, "数据库备份出错", e);
            }
        } else {
            ToastMgr.shortBottomCenter(context, "数据库文件不存在");
        }
    }

    public static void backupDB(Context context) {
        backupDB(context, false);
    }


    public static void recoveryDBAndJsNow(Context context) {
        String zipFilePath = UriUtils.getRootDir(context) + File.separator + "backup" + File.separator + "hiker.zip";
        try {
            String fileDirPath = UriUtils.getRootDir(context) + File.separator + "backup" + File.separator + "hiker";
            FileUtil.deleteDirs(fileDirPath);
            new File(fileDirPath).mkdirs();
            ZipUtils.unzipFile(zipFilePath, fileDirPath);
            String dbFilePath = fileDirPath + File.separator + BackupUtil.getDBFileNameWithVersion();
            File dbFile = new File(dbFilePath);
            String dbNewFilePath = UriUtils.getRootDir(context) + File.separator + "backup" + File.separator + BackupUtil.getDBFileNameWithVersion();
            boolean dbExist = dbFile.exists();
            if (dbExist) {
                FileUtil.copyFile(dbFile.getAbsolutePath(), dbNewFilePath);
            }

            int fileNum = 0;
            File rulesFile = new File(fileDirPath + File.separator + "rules.zip");
            if (rulesFile.exists()) {
                //新版带规则文件
                String rulesDir = fileDirPath + File.separator + "rules";
                ZipUtils.unzipFile(rulesFile.getAbsolutePath(), rulesDir);
                fileNum = FileUtil.getFileCount(rulesDir);
                FileUtil.copyDirectiory(UriUtils.getRootDir(Application.getContext()) + File.separator + "rules", rulesDir);
            } else {
                File jsFile = new File(fileDirPath);
                File[] jsFiles = jsFile.listFiles();
                String templateFilePath = fileDirPath + File.separator + "rule_template.json";
                File templateFile = new File(templateFilePath);
                if (templateFile.exists()) {
                    String templateNewFilePath = UriUtils.getRootDir(context) + File.separator + "rules" + File.separator + "rule_template.json";
                    FileUtil.copyFile(templateFile.getAbsolutePath(), templateNewFilePath);
                }
                if (jsFiles != null) {
                    for (File jsFilePath : jsFiles) {
                        if (!jsFilePath.getName().endsWith(".js")) {
                            jsFilePath.delete();
                        } else {
                            fileNum++;
                        }
                    }
                }
                FileUtil.deleteDirs(JSManager.getJsDirPath());
                FileUtil.copyDirectiory(JSManager.getJsDirPath(), jsFile.getAbsolutePath());
            }
            int finalFileNum = fileNum;
            String title = "";
            if (!dbExist) {
                title = "已恢复" + finalFileNum + "个应用插件，没有获取到适合当前版本的db文件";
            } else {
                title = "已恢复" + finalFileNum + "个应用插件，是否立即恢复db文件（包括小程序规则、历史记录、收藏等）？";
            }
            new XPopup.Builder(context)
                    .asConfirm("恢复完成", title, () -> {
                        if (dbExist) {
                            BackupUtil.recoveryDBNow(context);
                        }
                    }).show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void recoveryDBAndJs(Context context) {
        String zipFilePath = UriUtils.getRootDir(context) + File.separator + "backup" + File.separator + "hiker.zip";
        new PromptDialog(context)
                .setTitleText("风险提示")
                .setSpannedContentByStr("确定从备份文件（" + zipFilePath + "）恢复数据吗？当前支持的数据库版本为" + LitePal.getDatabase().getVersion() + "，注意“如果备份和恢复时的数据库版本不一致会导致软件启动闪退！”")
                .setPositiveListener("确定恢复", dialog1 -> {
                    dialog1.dismiss();
                    recoveryDBAndJsNow(context);
                }).show();

    }

    public static void recoveryDB(Context context) {
        String filePath = UriUtils.getRootDir(context) + File.separator + "backup" + File.separator + getDBFileNameWithVersion();
        new PromptDialog(context)
                .setTitleText("风险提示")
                .setSpannedContentByStr("确定从备份文件（" + filePath + "）恢复数据吗？当前支持的数据库版本为" + LitePal.getDatabase().getVersion() + "，注意“如果备份和恢复时的数据库版本不一致会导致软件启动闪退！”")
                .setPositiveListener("确定恢复", dialog1 -> {
                    dialog1.dismiss();
                    recoveryDBNow(context);
                }).show();

    }

    public static void recoveryDBNow(Context context) {
        synchronized (LitePal.class) {
            LitePal.getDatabase().close();
            File exportDir = new File(UriUtils.getRootDir(context) + File.separator + "backup");
            if (!exportDir.exists()) {
                boolean ok = exportDir.mkdirs();
                if (!ok) {
                    ToastMgr.shortBottomCenter(context, "创建备份文件夹失败");
                    return;
                }
            }
            File backup = new File(exportDir, getDBFileNameWithVersion());
            if (backup.exists()) {
                File dbFile = context.getDatabasePath(getDBFileName());
                if (dbFile.exists()) {
                    boolean del = dbFile.delete();
                    if (!del) {
                        ToastMgr.shortBottomCenter(context, "删除旧数据库文件失败");
                        return;
                    }
                }
                try {
                    boolean ok = dbFile.createNewFile();
                    if (!ok) {
                        ToastMgr.shortBottomCenter(context, "创建新数据库文件失败");
                        return;
                    }
                    FileUtil.copyFile(backup.getAbsolutePath(), dbFile.getAbsolutePath());
                    new PromptDialog(context)
                            .setDialogType(PromptDialog.DIALOG_TYPE_SUCCESS)
                            .setContentText("从备份恢复成功！开始重启软件使生效！")
                            .setTheCancelable(false)
                            .setAnimationEnable(true)
                            .setTitleText("温馨提示")
                            .setPositiveListener("立即重启", dialog -> {
                                dialog.dismiss();
                                android.os.Process.killProcess(android.os.Process.myPid());
                                System.exit(0);
                            }).show();
                } catch (Exception e) {
                    DebugUtil.showErrorMsg((Activity) context, "数据库恢复备份出错", e);
                }
            } else {
                ToastMgr.shortBottomCenter(context, backup.getAbsolutePath() + "数据库文件不存在");
            }
        }
    }

    public static String getDBFileNameWithVersion() {
        return "hiker_" + LitePal.getDatabase().getVersion() + ".db";
    }

    private static String getDBFileName() {
        return "hiker.db";
    }
}

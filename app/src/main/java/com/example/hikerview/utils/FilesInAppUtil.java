package com.example.hikerview.utils;

/**
 * 作者：By hdy
 * 日期：On 2017/11/9
 * 时间：At 14:00
 */

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import androidx.core.content.FileProvider;

import com.example.hikerview.R;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

public class FilesInAppUtil {


    public static Bitmap getImageFromAssetsFile(Context context, String fileName) {
        Bitmap image = null;
        AssetManager am = context.getResources().getAssets();
        try {
            InputStream is = am.open(fileName);
            image = BitmapFactory.decodeStream(is);
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return image;
    }

    public static void copyAssets(Context context, String assetDir, String dir) {
        String[] files;
        try {
            files = context.getResources().getAssets().list(assetDir);
        } catch (IOException e1) {
            return;
        }
        File mWorkingPath = new File(dir);
        // if this directory does not exists, make one.
        if (!mWorkingPath.exists()) {
            if (!mWorkingPath.mkdirs()) {

            }
        }

        if (files != null) {
            for (int i = 0; i < files.length; i++) {
                try {
                    String fileName = files[i];
                    // we make sure file name not contains '.' to be a folder.
                    if (!fileName.contains(".")) {
                        if (0 == assetDir.length()) {
                            copyAssets(context, fileName, dir + fileName + "/");
                        } else {
                            copyAssets(context, assetDir + "/" + fileName, dir + fileName + "/");
                        }
                        continue;
                    }
                    File outFile = new File(mWorkingPath, fileName);
                    if (outFile.exists())
                        outFile.delete();
                    InputStream in = null;
                    if (0 != assetDir.length())
                        in = context.getAssets().open(assetDir + "/" + fileName);
                    else
                        in = context.getAssets().open(fileName);
                    OutputStream out = new FileOutputStream(outFile);

                    // Transfer bytes from in to out
                    byte[] buf = new byte[1024];
                    int len;
                    while ((len = in.read(buf)) > 0) {
                        out.write(buf, 0, len);
                    }

                    in.close();
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 快速读取程序应用包下的文件内容
     *
     * @param context  上下文
     * @param filename 文件名称
     * @return 文件内容
     * @throws IOException
     */
    public static String read(Context context, String filename)
            throws IOException {
        File file = context.getFileStreamPath(filename);
        if (file == null || !file.exists()) {
            return "";
        }
        FileInputStream inStream = context.openFileInput(filename);
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len = 0;
        while ((len = inStream.read(buffer)) != -1) {
            outStream.write(buffer, 0, len);
        }
        byte[] data = outStream.toByteArray();
        return new String(data);
    }

    /**
     * 写入应用程序包files目录下文件
     *
     * @param context  上下文
     * @param fileName 文件名称
     * @param content  文件内容
     */
    public static void writeEnd(Context context, String fileName, String content) {
        try {
            FileOutputStream outStream = context.openFileOutput(fileName,
                    Context.MODE_APPEND);//表示如果已存在则追加数据
            outStream.write(content.getBytes());
            outStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 写入应用程序包files目录下文件
     *
     * @param context  上下文
     * @param fileName 文件名称
     * @param content  文件内容
     */
    public static void write(Context context, String fileName, String content) {
        try {
            FileOutputStream outStream = context.openFileOutput(fileName,
                    Context.MODE_PRIVATE);
            outStream.write(content.getBytes());
            outStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String getFilePath(Context context, String fileName) {
        return context.getFilesDir() + File.separator + fileName;
    }

    public static boolean exist(Context context, String fileName) {
        File file = new File(context.getFilesDir() + File.separator + fileName);
        return file.exists();
    }

    public static String getAssetsString(Context context, String fileName) {
        //将json数据变成字符串
        StringBuilder stringBuilder = new StringBuilder();
        try {
            //获取assets资源管理器
            AssetManager assetManager = context.getAssets();
            //通过管理器打开文件并读取
            BufferedReader bf = new BufferedReader(new InputStreamReader(
                    assetManager.open(fileName)));
            String line;
            while ((line = bf.readLine()) != null) {
                stringBuilder.append(line).append("\n");
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return stringBuilder.toString();
    }


    public static Uri getUri(Context context, String url) {
        if (url.startsWith("file:") || url.startsWith("content") || url.startsWith("/")) {
            try {
                String authority = context.getResources().getString(R.string.authority);
                return FileProvider.getUriForFile(context, authority, new File(url.replaceFirst("file://", "")));
            } catch (Exception e) {
                e.printStackTrace();
                return Uri.parse(url);
            }
        } else {
            return Uri.parse(url);
        }
    }

}
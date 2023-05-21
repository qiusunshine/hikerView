package com.jeffmony.videocache.utils;

import android.content.Context;

import com.jeffmony.videocache.model.VideoCacheInfo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * @author jeffmony
 * 本地代理存储相关的工具类
 */

public class StorageUtils {

    private static final String TAG = "StorageUtils";

    public static final int DEFAULT_BUFFER_SIZE = 8 * 1024;
    public static final String INFO_FILE = "video.info";
    public static final String LOCAL_M3U8_SUFFIX = "_local.m3u8";
    public static final String PROXY_M3U8_SUFFIX = "_proxy.m3u8";
    public static final String M3U8_SUFFIX = ".m3u8";
    public static final String NON_M3U8_SUFFIX = ".video";

    private static final Object sInfoFileLock = new Object();

    public static File getVideoFileDir(Context context) {
        return new File(context.getExternalFilesDir("Video"), "jeffmony");
    }

    public static VideoCacheInfo readVideoCacheInfo(File dir) {
        LogUtils.i(TAG, "readVideoCacheInfo : dir=" + dir.getAbsolutePath());
        File file = new File(dir, INFO_FILE);
        if (!file.exists()) {
            LogUtils.i(TAG,"readProxyCacheInfo failed, file not exist.");
            return null;
        }
        ObjectInputStream fis = null;
        try {
            synchronized (sInfoFileLock) {
                fis = new ObjectInputStream(new FileInputStream(file));
                VideoCacheInfo info = (VideoCacheInfo) fis.readObject();
                return info;
            }
        } catch (Exception e) {
            LogUtils.w(TAG,"readVideoCacheInfo failed, exception=" + e.getMessage());
        } finally {
            ProxyCacheUtils.close(fis);
        }
        return null;
    }

    public static void saveVideoCacheInfo(VideoCacheInfo info, File dir) {
        File file = new File(dir, INFO_FILE);
        ObjectOutputStream fos = null;
        try {
            synchronized (sInfoFileLock) {
                fos = new ObjectOutputStream(new FileOutputStream(file));
                fos.writeObject(info);
            }
        } catch (Exception e) {
            LogUtils.w(TAG,"saveVideoCacheInfo failed, exception=" + e.getMessage());
        } finally {
            ProxyCacheUtils.close(fos);
        }
    }

    /**
     * 清理过期的数据
     * @param file
     * @param expiredTime
     */
    public static void cleanExpiredCacheData(File file, long expiredTime) throws IOException {
        if (file == null || !file.exists()) return;
        File[] listFiles = file.listFiles();
        if (listFiles == null) return;
        for (File itemFile : listFiles) {
            if (isExpiredCacheData(itemFile.lastModified(), expiredTime)) {
                delete(itemFile);
            }
        }
    }

    private static boolean isExpiredCacheData(long lastModifiedTime, long expiredTime) {
        long now = System.currentTimeMillis();
        if (Math.abs(now - lastModifiedTime) > expiredTime) {
            return true;
        }
        return false;
    }

    private static void delete(File file) throws IOException {
        if (file.isFile() && file.exists()) {
            boolean isDeleted = file.delete();
            if (!isDeleted) {
                throw new IOException(String.format("File %s cannot be deleted", file.getAbsolutePath()));
            }
        }
    }

    public static boolean deleteFile(File file) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files == null) return true;
            for (File f : files) {
                if (!f.delete()) return false;
            }
            return file.delete();
        } else {
            return file.delete();
        }
    }

    /**
     * 获取file目录中所有文件的总大小
     * @param file
     * @return
     */
    public static long getTotalSize(File file) {
        if (file.isDirectory()) {
            long totalSize = 0;
            File[] files = file.listFiles();
            if (files == null) return 0;
            for (File f : files) {
                totalSize += getTotalSize(f);
            }
            return totalSize;
        } else {
            return file.length();
        }
    }

    public static void setLastModifiedTimeStamp(File file) throws IOException {
        if (file.exists()) {
            long now = System.currentTimeMillis();
            boolean modified = file.setLastModified(now);
            if (!modified) {
                modify(file);
            }
        }
    }

    //file是一个目录文件
    private static void modify(File file) throws IOException {
        File tempFile = new File(file, "tempFile");
        if (!tempFile.exists()) {
            tempFile.createNewFile();
            tempFile.delete();
        } else {
            tempFile.delete();
        }
    }

}

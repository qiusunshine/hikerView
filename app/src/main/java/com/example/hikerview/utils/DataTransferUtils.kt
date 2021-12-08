package com.example.hikerview.utils

import com.example.hikerview.ui.Application
import timber.log.Timber
import java.io.File

/**
 * 作者：By 15968
 * 日期：On 2021/11/5
 * 时间：At 22:46
 */
object DataTransferUtils {

    /**
     * 避免被杀掉页面导致数据完全丢失，因此存一份到缓存
     */
    fun saveCache(data: String, file: String) {
        HeavyTaskUtil.executeNewTask {
            try {
                val path =
                    Application.application.cacheDir
                        .toString() + File.separator + file + ".txt"
                Timber.d("setTempRuleData path: %s", path)
                FileUtil.stringToFile(data, path)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * 可能切换了深色模式，或者被后台杀掉了，可以从缓存取回数据
     */
    fun readFromCache(file: String): String? {
        //application也被回收了，尝试从文件中取
        val cache = Application.application.cacheDir.toString() + File.separator + file + ".txt"
        if (File(cache).exists()) {
            return FileUtil.fileToString(cache)
        }
        return null
    }
}
package com.example.hikerview.utils

import android.content.Context
import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.JSONObject
import com.example.hikerview.service.parser.JSEngine
import com.jeffmony.m3u8library.listener.IVideoTransformListener
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.net.URL
import java.util.*
import java.util.regex.Pattern

/**
 * 作者：By 15968
 * 日期：On 2021/11/10
 * 时间：At 10:38
 */
object M3u8Utils {

    /**
     * 替换本地m3u8文件索引相对路径为绝对路径
     */
    fun getLocalContent(
        context: Context?, m3u8Path: String, www: String,
        listener: IVideoTransformListener?, sync: Boolean
    ): String? {
        val m3u8 = File(m3u8Path)
        if (!m3u8.exists()) {
            if (!sync) {
                ToastMgr.shortCenter(context, "找不到m3u8文件")
            }
            listener?.onTransformFailed(Exception("找不到m3u8文件"))
            return null
        }
        //转成绝对地址
        val fileString: MutableList<String> = ArrayList()
        try {
            var hasTs = false
            m3u8.readLines().forEach {
                if (it.startsWith("/")) {
                    //经过下载的文件一定是/开头，不会是无/的相对路径
                    if (!hasTs && it.endsWith(".m3u8")) {
                        return getLocalContent(
                            context,
                            it.replace("/", "$www/"),
                            www,
                            listener,
                            sync
                        )
                    }
                    fileString.add(it.replace("/", "$www/"))
                    hasTs = true
                } else {
                    fileString.add(it)
                }
            }
        } catch (e: IOException) {
            Timber.d(e, "文件异常%s", e.message)
        }
        return StringUtil.listToString(fileString, "\n")
    }

    private fun isNotM3u8(url: String): Boolean {
        if (url.contains(".flv") || url.contains(".m4a")
            || url.contains(".mp3") || url.contains("mine_type=video_mp4")
        ) {
            return true
        }
        if (url.contains("mp4") && !url.contains("m3u8")) {
            return true
        }
        return false
    }

    /**
     * 下载m3u8索引文件
     */
    fun downloadM3u8(
        url: String,
        fileName: String = "video.m3u8",
        options: Any?,
        ruleKey: Any?
    ): String {
        if (isNotM3u8(url)) {
            return url
        }
        val response: String? = JSEngine.getInstance().fetchWithHeaders(url, options, ruleKey)
        var jsonObject = JSONObject()
        if (response != null && response.isNotEmpty()) {
            jsonObject = JSON.parseObject(response)
        }
        val filePath: String = JSEngine.getFilePath("hiker://files/cache/${fileName}")
        var content = if (jsonObject.containsKey("body")) jsonObject["body"] as String else ""
        val realUrl = if (jsonObject.containsKey("url")) jsonObject["url"] as String else url
        content.let {
            content = convertPath(it, realUrl, fileName, options, ruleKey)
        }
        FileUtil.stringToFile(content, filePath)
        return "file://$filePath"
    }

    /**
     * 替换本地m3u8文件索引相对路径为绝对路径
     */
    private fun convertPath(
        content: String,
        url: String,
        fileName: String,
        options: Any?,
        ruleKey: Any?
    ): String {
        val fileString: MutableList<String> = ArrayList()
        val lines = content.split("\n")
        var hasTs = false
        for (valueString in lines) {
            if (valueString.startsWith("#EXT-X-KEY:")) {
                //替换key地址
                val searchKeyUri = Pattern.compile("URI=\"(.*?)\"").matcher(valueString)
                if (!searchKeyUri.find()) {
                    fileString.add(valueString)
                    continue
                }
                val keyUri = searchKeyUri.group(1)
                if (keyUri != null) {
                    val keyUrl =
                        if (keyUri.startsWith("http://") || keyUri.startsWith("https://")) {
                            keyUri
                        } else {
                            URL(URL(url), keyUri.trim { it <= ' ' } ?: "").toString()
                        }
                    fileString.add(valueString.replace(keyUri, keyUrl))
                } else {
                    fileString.add(valueString)
                }
            } else if (valueString.startsWith("/") ||
                (!valueString.startsWith("#") && !valueString.startsWith(
                    "http"
                )
                        )
            ) {
                //替换相对路径为绝对路径
                val newUrl = URL(URL(url), valueString).toString()
                if (!hasTs && valueString.endsWith(".m3u8")) {
                    if (newUrl != url) {
                        //没有死循环的重定向
                        return downloadM3u8(
                            newUrl,
                            fileName,
                            options,
                            ruleKey
                        )
                    }
                }
                fileString.add(newUrl)
                hasTs = true
            } else {
                fileString.add(valueString)
            }
        }
        return StringUtil.listToString(fileString, "\n")
    }
}
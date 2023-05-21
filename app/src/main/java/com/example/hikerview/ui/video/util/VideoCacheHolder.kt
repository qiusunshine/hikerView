package com.example.hikerview.ui.video.util

import android.content.Context
import chuangyuan.ycj.videolibrary.factory.HttpDefaultDataSourceFactory
import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.JSONArray
import com.example.hikerview.model.BigTextDO
import com.example.hikerview.service.parser.JSEngine
import com.example.hikerview.ui.Application
import com.example.hikerview.ui.download.util.VideoFormatUtil
import com.example.hikerview.utils.FileUtil
import com.example.hikerview.utils.HeavyTaskUtil
import com.example.hikerview.utils.HttpUtil
import com.example.hikerview.utils.PreferenceMgr
import com.example.hikerview.utils.StringUtil
import com.example.hikerview.utils.ThreadTool
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.util.FileTypes
import com.jeffmony.videocache.VideoProxyCacheManager
import com.jeffmony.videocache.common.VideoMime
import com.jeffmony.videocache.common.VideoParams
import com.jeffmony.videocache.common.VideoType
import com.jeffmony.videocache.listener.IVideoCacheListener
import com.jeffmony.videocache.m3u8.M3U8Utils
import com.jeffmony.videocache.model.VideoCacheInfo
import com.jeffmony.videocache.utils.ProxyCacheUtils
import com.jeffmony.videocache.utils.StorageUtils
import timber.log.Timber
import java.io.File


/**
 * 作者：By 15968
 * 日期：On 2022/3/26
 * 时间：At 16:28
 */
object VideoCacheHolder {
    var useProxy: Boolean = false
    private var hasInit = false
    private var useProxying = false
    private var lastUrl: String = ""
    private val failedSet: MutableSet<String> by lazy {
        initFailedSet()
    }

    fun initConfig() {
        useProxy = PreferenceMgr.getBoolean(Application.getContext(), "useProxy", false)
    }

    private fun makeSureInit(context: Context) {
        if (!hasInit) {
            val saveFile: File = StorageUtils.getVideoFileDir(context)
            hasInit = true
            val builder = VideoProxyCacheManager.Builder()
                .setFilePath(saveFile.absolutePath)
                .setConnTimeOut(30 * 1000)
                .setReadTimeOut(30 * 1000)
                .setExpireTime(2L * 24 * 60 * 60 * 1000)
                .setMaxCacheSize(2L * 1024 * 1024 * 1024)
                .setIgnoreCert(true)
                .setUseOkHttp(true)
            VideoProxyCacheManager.getInstance().initProxyConfig(builder.build())
        }
    }

    fun isProxyError(): Boolean {
        if (useProxy && useProxying && lastUrl.isNotEmpty()) {
            val dom = StringUtil.getDom(lastUrl)
            if (failedSet.contains(dom)) {
                return false
            }
            Timber.w("proxyError: $lastUrl")
            failedSet.add(dom)
            saveFailedSet()
            ThreadTool.runOnUI {
                release()
            }
            return true
        }
        return false
    }

    @Synchronized
    fun getProxyUrl(
        context: Context,
        uu: String,
        streamType: Int,
        headers0: MutableMap<String, String?>?,
        failedConsumer: () -> Int
    ): String {
        val headers = initHeaders(headers0)
        var playUrl = uu
        var dom = StringUtil.getDom(playUrl)
        if (!useProxy || streamType == C.TYPE_DASH || streamType == C.TYPE_SS || failedSet.contains(
                dom
            )
        ) {
            return playUrl
        }
        if (!playUrl.lowercase().startsWith("http")) {
            if (playUrl.startsWith("file://") && playUrl.contains("##") && playUrl.contains(".m3u8")) {
                //已经缓存了m3u8文件没有缓存ts文件的，手动生成proxy文件
                try {
                    val saveFile: File = StorageUtils.getVideoFileDir(context)
                    if (!saveFile.exists()) {
                        saveFile.mkdirs()
                    }
                    makeSureInit(context)
                    val md5 = ProxyCacheUtils.computeMD5(playUrl)
                    val dir = File(ProxyCacheUtils.getConfig().filePath, md5)
                    //之前没有缓存信息
                    val local = playUrl.split("##")[0].replace("file://", "")
                    playUrl = playUrl.split("##")[1]
                    dom = StringUtil.getDom(playUrl)
                    if (failedSet.contains(dom)) {
                        return uu
                    }
                    val videoCacheInfo = VideoCacheInfo(playUrl)
                    videoCacheInfo.md5 = md5
                    videoCacheInfo.savePath = dir.absolutePath
                    val proxyM3U8File = File(
                        videoCacheInfo.savePath,
                        videoCacheInfo.md5 + StorageUtils.PROXY_M3U8_SUFFIX
                    )
                    if (proxyM3U8File.parentFile != null && !proxyM3U8File.parentFile!!.exists()) {
                        proxyM3U8File.parentFile.mkdirs()
                    }
                    val m3u8 = M3U8Utils.parseLocalM3U8Info(
                        File(local),
                        playUrl
                    )
                    videoCacheInfo.videoType = VideoType.M3U8_TYPE
                    videoCacheInfo.localPort = ProxyCacheUtils.getLocalPort()
                    videoCacheInfo.totalTs = m3u8.segCount
                    StorageUtils.saveVideoCacheInfo(videoCacheInfo, File(dir.absolutePath))
                    M3U8Utils.createProxyM3U8File(proxyM3U8File, m3u8, videoCacheInfo.md5, headers)
                    val localM3U8File = File(
                        videoCacheInfo.savePath,
                        videoCacheInfo.md5 + StorageUtils.LOCAL_M3U8_SUFFIX
                    )
                    M3U8Utils.createLocalM3U8File(localM3U8File, m3u8)
                } catch (e: Exception) {
                    e.printStackTrace()
                    return playUrl
                }
            } else {
                return playUrl
            }
        }
        if (playUrl.startsWith("http://127.") || playUrl.startsWith("http://192.") || playUrl.startsWith(
                "http://0.0."
            )
        ) {
            return playUrl
        }
        try {
            val saveFile: File = StorageUtils.getVideoFileDir(context)
            if (!saveFile.exists()) {
                saveFile.mkdirs()
            }
            makeSureInit(context)
            release()
            val extra: MutableMap<String, Any?> = HashMap()
            var m3u8 = false
            if (streamType == C.TYPE_HLS) {
                extra[VideoParams.CONTENT_TYPE] = VideoMime.MIME_TYPE_M3U8_2
                m3u8 = true
            } else if (streamType == C.TYPE_OTHER) {
                extra[VideoParams.CONTENT_TYPE] = "video/mp4"
            }
            val url = ProxyCacheUtils.getProxyUrl(playUrl, headers, extra)
            VideoProxyCacheManager.getInstance().addCacheListener(playUrl, object :
                IVideoCacheListener {
                override fun onCacheStart(cacheInfo: VideoCacheInfo?) {

                }

                override fun onCacheProgress(cacheInfo: VideoCacheInfo?) {

                }

                override fun onCacheError(cacheInfo: VideoCacheInfo?, errorCode: Int) {
                    //缓存失败降级为原链接
                    callError(uu, dom, failedConsumer, errorCode)
                }

                override fun onCacheForbidden(cacheInfo: VideoCacheInfo?) {
                    //直播
                    if (failedSet.contains(dom)) {
                        return
                    }
                    Timber.w("onCacheForbidden: $uu")
                    failedSet.add(dom)
                    saveFailedSet()
                    ThreadTool.runOnUI {
                        failedConsumer()
                        release()
                    }
                }

                override fun onCacheFinished(cacheInfo: VideoCacheInfo?) {

                }

            })
            startRequestVideoInfo(uu, dom, failedConsumer, m3u8, playUrl, headers, extra)
            VideoProxyCacheManager.getInstance().playingUrlMd5 = ProxyCacheUtils.computeMD5(playUrl)
            useProxying = true
            lastUrl = playUrl
            Timber.d("useProxying: original: $playUrl")
            Timber.d("useProxying: proxyUrl: $url")
            return url
        } catch (e: Exception) {
            Timber.e(e)
        }
        return playUrl
    }

    fun callError(
        uu: String,
        dom: String,
        failedConsumer: () -> Int
    ) {
        callError(uu, dom, failedConsumer, 0)
    }

    fun callError(
        uu: String,
        dom: String,
        failedConsumer: () -> Int,
        error: Int = 0
    ) {
        if (error != 404) {
            if (failedSet.contains(dom)) {
                return
            }
            Timber.w("cacheError: $uu")
            failedSet.add(dom)
            saveFailedSet()
        }
        ThreadTool.runOnUI {
            failedConsumer()
            release()
        }
    }

    fun startRequestVideoInfo(
        uu: String,
        dom: String,
        failedConsumer: () -> Int,
        m3u8: Boolean,
        playUrl: String,
        headers: MutableMap<String, String?>?,
        extra: MutableMap<String, Any?>
    ) {
        if (m3u8) {
            VideoProxyCacheManager.getInstance().startRequestVideoInfo(playUrl, headers, extra)
        } else {
            //自己校验一下header
            val ops: MutableMap<String, Any?> = HashMap()
            ops["onlyHeaders"] = true
            ops["headers"] = headers
            val res = JSEngine.getInstance().fetchWithHeadersInterceptor(
                playUrl, ops, null
            ) {
                //拦截header
                true
            }
            val j = JSON.parseObject(res)
            val headers1 = j.getJSONObject("headers")
            val headerMap = HashMap<String, MutableList<String?>>()
            for (key in headers1.keys) {
                headerMap[key] = arrayListOf()
                (headers1[key] as JSONArray?).let {
                    for (item in it!!) {
                        headerMap[key]?.add(item as String?)
                    }
                }
            }
            val t = HttpUtil.inferFileTypeFromResponse(playUrl, headerMap)
            when (t) {
                HttpUtil.M3U8 -> {
                    //伪装的M3U8
                    extra[VideoParams.CONTENT_TYPE] = VideoMime.MIME_TYPE_M3U8_2
                    VideoProxyCacheManager.getInstance()
                        .startRequestVideoInfo(playUrl, headers, extra)
                }

                else -> {
                    if (t == FileTypes.UNKNOWN) {
                        var contentTypes: List<String?>? = headerMap["Content-Type"]
                        if (contentTypes == null && headerMap.containsKey("content-type")) {
                            contentTypes = headerMap["content-type"]
                        }
                        val mimeType = if (contentTypes.isNullOrEmpty()) null else contentTypes[0]
                        if ((!VideoFormatUtil.isStream(mimeType)
                                    && mimeType?.contains("video") == false) && !mimeType.contains("audio")
                        ) {
                            //未知的视频格式
                            callError(uu, dom, failedConsumer)
                            return
                        }
                    }
                    try {
                        //把文件长度放进去
                        var contentLength: List<String?>? = headerMap["Content-Length"]
                        if (contentLength == null && headerMap.containsKey("content-length")) {
                            contentLength = headerMap["content-length"]
                        }
                        val length = if (contentLength.isNullOrEmpty()) "0" else contentLength[0]
                        val l = length?.toLong() ?: -1
                        if (l > 0) {
                            extra[VideoParams.CONTENT_LENGTH] = l
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    VideoProxyCacheManager.getInstance()
                        .startRequestVideoInfo(playUrl, headers, extra)
                }
            }
        }
    }

    private fun initHeaders(headers0: MutableMap<String, String?>?): MutableMap<String, String?>? {
        var headers = headers0
        if (headers == null) {
            headers = HashMap()
        }
        var userAgent = HttpDefaultDataSourceFactory.DEFAULT_UA
        var key = ""
        if (headers.containsKey("User-Agent")) {
            key = "User-Agent"
        } else if (headers.containsKey("user-agent")) {
            key = "user-agent"
        } else if (headers.containsKey("user-Agent")) {
            key = "user-Agent"
        }
        if (key.isNotEmpty()) {
            userAgent = headers[key]!!
            headers.remove(key)
        }
        headers["User-Agent"] = userAgent
        return headers
    }

    fun pause() {
        try {
            if (useProxy && useProxying && lastUrl.isNotEmpty()) {
                VideoProxyCacheManager.getInstance().pauseCacheTask(lastUrl)
                useProxying = false
            }
        } catch (e: Exception) {
        }
    }

    fun resume() {
        try {
            if (useProxy && lastUrl.isNotEmpty()) {
                VideoProxyCacheManager.getInstance().resumeCacheTask(lastUrl)
                useProxying = true
            }
        } catch (e: Exception) {
        }
    }

    fun release() {
        try {
            if (!useProxy || lastUrl.isEmpty()) {
                return
            }
            VideoProxyCacheManager.getInstance().stopCacheTask(lastUrl)
            VideoProxyCacheManager.getInstance().releaseProxyReleases(lastUrl)
        } catch (e: Exception) {
        }
        lastUrl = ""
        useProxying = false
    }

    fun seek(position: Long, duration: Long) {
        if (!useProxy || !useProxying || lastUrl.isEmpty()) {
            return
        }
        if (duration > 0) {
            val percent = position * 1.0f / duration
            VideoProxyCacheManager.getInstance().seekToCacheTaskFromClient(lastUrl, percent)
        }
    }

    fun destroy(context: Context) {
        release()
        val proxyAutoClean =
            PreferenceMgr.getBoolean(
                context,
                "proxyAutoClean",
                false
            )
        if (proxyAutoClean) {
            ThreadTool.executeNewTask {
                val saveFile: File = StorageUtils.getVideoFileDir(context)
                if (saveFile.exists()) {
                    FileUtil.deleteDirs(saveFile.absolutePath)
                }
            }
        }
        lastUrl = ""
        useProxying = false
    }

    private fun initFailedSet(): HashSet<String> {
        val set = HashSet<String>()
        val wl: String? = BigTextDO.getVideoCacheWhiteList()
        if (!wl.isNullOrEmpty()) {
            set.addAll(wl.split("@@@"))
        }
        return set
    }

    private fun saveFailedSet() {
        HeavyTaskUtil.executeNewTask {
            if (useProxy && failedSet.isNotEmpty()) {
                val s = failedSet.joinToString("@@@")
                BigTextDO.updateVideoCacheWhiteList(s)
            }
        }
    }

    fun getCacheWhiteList(): MutableList<String> {
        val set: MutableList<String> = ArrayList()
        val wl: String? = BigTextDO.getVideoCacheWhiteList()
        if (!wl.isNullOrEmpty()) {
            set.addAll(wl.split("@@@"))
        }
        return set
    }

    fun saveCacheWhiteList(dom: MutableList<String>) {
        BigTextDO.updateVideoCacheWhiteList(dom.joinToString("@@@"))
        failedSet.clear()
        for (s in dom) {
            failedSet.add(s)
        }
    }
}
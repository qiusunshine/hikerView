package com.example.hikerview.ui.thunder

import android.content.Context
import android.net.Uri
import android.text.TextUtils
import com.example.hikerview.service.http.CodeUtil
import com.example.hikerview.service.parser.HttpParser
import com.example.hikerview.service.parser.JSEngine
import com.example.hikerview.ui.Application
import com.example.hikerview.ui.browser.model.DetectedMediaResult
import com.example.hikerview.ui.browser.model.UrlDetector
import com.example.hikerview.ui.video.PlayerChooser
import com.example.hikerview.ui.video.VideoChapter
import com.example.hikerview.ui.view.XiuTanResultPopup
import com.example.hikerview.utils.*
import com.lxj.xpopup.XPopup
import com.xunlei.downloadlib.XLDownloadManager
import com.xunlei.downloadlib.XLTaskHelper
import com.xunlei.downloadlib.parameter.TorrentFileInfo
import com.xunlei.downloadlib.parameter.XLTaskInfo
import kotlinx.coroutines.*
import org.apache.commons.lang3.StringUtils
import java.io.File
import java.net.URLDecoder
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

/**
 * 作者：By 15968
 * 日期：On 2022/6/12
 * 时间：At 22:18
 */
object ThunderManager {

    var taskId: Long = 0
    var path: String = ""
    var scope: CoroutineScope? = null
    val errorCode = HashMap<Int, String>()
    var savePathNow: String = ""

    init {
        errorCode[9125] = "文件名太长"
        errorCode[111120] = "文件路径太长"
        errorCode[111142] = "文件太小"
        errorCode[111085] = "磁盘空间不足"
        errorCode[111171] = "拒绝的网络连接"
        errorCode[9301] = "缓冲区不足"
        errorCode[114001] = "版权限制：无权下载"
        errorCode[114004] = "版权限制：无权下载"
        errorCode[114005] = "版权限制：无权下载"
        errorCode[114006] = "版权限制：无权下载"
        errorCode[114007] = "版权限制：无权下载"
        errorCode[114011] = "版权限制：无权下载"
        errorCode[9304] = "版权限制：无权下载"
        errorCode[111154] = "版权限制：无权下载"
        errorCode[114101] = "无效链接"
    }

    fun isMagnetOrTorrent(url: String): Boolean {
        if (url.startsWith("magnet:") || url.split(";")[0].endsWith(".torrent")) {
            return true
        }
        return false
    }

    fun startDownloadMagnet(context: Context?, url: String?) {
        startDownloadMagnet(context, url, null)
    }

    /**
     * 下载磁力或者种子
     */
    fun startDownloadMagnet(context: Context?, url: String?, c: MagnetConsumer?) {
        if (url?.startsWith("magnet:") == false && url.split(";")[0].endsWith(".torrent")) {
            startParseTorrent(context!!, url, c)
            return
        }
        var consumer = c
        if (consumer == null) {
            consumer = object : MagnetConsumer {
                override fun consume(
                    u: String,
                    name: String,
                    list: java.util.ArrayList<TorrentFileInfo>
                ) {
                    if (list.size < 2) {
                        PlayerChooser.startPlayer(context, name, u, null)
                    } else {
                        val chapters = toChapters(url!!, u, name, list)
                        PlayerChooser.startPlayer(context, chapters, "", url, null)
                    }
                }
            }
        }
        parse(context!!, url!!, consumer)
    }

    fun startParseTorrent(context: Context, path: String) {
        startParseTorrent(context, path, null)
    }

    /**
     * 下载种子
     */
    fun startParseTorrent(context: Context, path: String, consumer: MagnetConsumer?) {
        if (path.startsWith("http")) {
            toast("种子解析中，请稍候")
            val u = path.split(";")[0]
            val headers = HttpParser.getHeaders(path)
            getActiveScope(true).launch(Dispatchers.IO) {
                CodeUtil.download(u, "hiker://files/magnet/t.torrent", headers, object :
                    CodeUtil.OnCodeGetListener {
                    override fun onSuccess(s: String?) {
                        s?.let {
                            val p = JSEngine.getFilePath(it)
                            startParseTorrent0(context, p, consumer)
                        }
                    }

                    override fun onFailure(errorCode: Int, msg: String?) {
                        toast("下载torrent失败")
                    }
                })
            }
        } else {
            startParseTorrent0(context, path, consumer)
        }
    }

    fun isFTPOrEd2k(url: String?): Boolean {
        if (url.isNullOrEmpty()) {
            return false
        }
        return (url.startsWith("ftp:") && UrlDetector.isVideoOrMusic(url)) || url.startsWith("ed2k:")
    }

    fun startParseFTPOrEd2k(context: Context, url: String) {
        startParseFTPOrEd2k(context, url, null)
    }

    /**
     * 解析FTP或者电驴
     */
    fun startParseFTPOrEd2k(context: Context, url: String, c: MagnetConsumer?) {
        if (!isFTPOrEd2k(url)) {
            toast("仅支持FTP和ed2k格式")
            return
        }
        initXL(context)
        release()
        val decode = URLDecoder.decode(url)
        val p = Uri.parse(decode)
        if (p == null) {
            ToastMgr.longCenter(context, "链接出错")
            return
        }
        var fileName = XLTaskHelper.instance().getFileName(decode)
        if (TextUtils.isEmpty(fileName)) {
            fileName = System.currentTimeMillis().toString() + ".mp4"
        }

        if (!fileName.contains(".")) {
            fileName = "$fileName.mp4"
        }
        var consumer = c
        if (consumer == null) {
            consumer = object : MagnetConsumer {
                override fun consume(
                    u: String,
                    name: String,
                    list: java.util.ArrayList<TorrentFileInfo>
                ) {
                    PlayerChooser.startPlayer(context, name, u, null)
                }
            }
        }
        downloadFTP(url, fileName, consumer)
    }

    private fun startParseTorrent0(context: Context, path: String, c: MagnetConsumer? = null) {
        initXL(context)
        release(path)
        var consumer = c
        if (consumer == null) {
            consumer = object : MagnetConsumer {
                override fun consume(
                    u: String,
                    name: String,
                    list: java.util.ArrayList<TorrentFileInfo>
                ) {
                    if (list.size < 2) {
                        PlayerChooser.startPlayer(context, name, u, null)
                    } else {
                        val chapters = toChapters(path, u, name, list)
                        PlayerChooser.startPlayer(context, chapters, "", path, null)
                    }
                }
            }
        }
        getTorrentInfo(context, path, File(path), consumer!!)
    }

    private fun initXL(context: Context) {
        XLTaskHelper.init(context, XL.getA(), XL.getB())
        path = UriUtils.getRootDir(context) + File.separator + "magnet"
        val dir = File(path)
        if (!dir.exists()) {
            dir.mkdirs()
        }
    }

    /**
     * 解析磁力
     */
    private fun parse(context: Context, url: String, consumer: MagnetConsumer) {
        initXL(context)
        val decode = URLDecoder.decode(
            if (url.lowercase(Locale.getDefault())
                    .startsWith("thunder")
            ) XLDownloadManager.getInstance()
                .parserThunderUrl(url) else url
        )
        val p = Uri.parse(decode)
        if (p == null) {
            ToastMgr.longCenter(context, "链接出错")
            return
        }
        val fileName = XLTaskHelper.instance().getFileName(decode)
        val file = File(path + File.separator + fileName)
        val dir = file.parentFile
        release()

        getActiveScope(true).launch(Dispatchers.IO) {
            taskId = XLTaskHelper.instance().addMagentTask(url, dir!!.absolutePath, fileName)
            var count = 0
            var hasToast = false
            while (isActive) {
                count++
                if (count > 120) {
                    toast("磁力链接解析超时")
                    return@launch
                }
                val i = XLTaskHelper.instance().getTaskInfo(taskId).mTaskStatus
                when {
                    i == 2 -> {
                        XLTaskHelper.instance().stopTask(taskId)
                        getTorrentInfo(context, url, file, consumer)
                        return@launch
                    }
                    i != 3 -> {
                        if (!hasToast) {
                            toast("磁链解析中，请稍候")
                            hasToast = true
                        }
                        delay(1000)
                    }
                    else -> {
                        XLTaskHelper.instance().stopTask(taskId)
                        toast("链接连接失败")
                        return@launch
                    }
                }
            }
        }
    }


    fun release() {
        release(null)
    }

    /**
     * 释放文件
     */
    fun release(exclude: String? = null) {
        stopTask()
        val files = File(path).listFiles()
        files?.let {
            for (file in it) {
                if (exclude != null && file.absolutePath == exclude) {
                    continue
                }
                file.deleteRecursively()
            }
        }
        scope?.let {
            ThreadTool.cancelScope(it)
        }
    }

    private fun stopTask() {
        if (taskId > 0) {
            XLTaskHelper.instance().stopTask(taskId)
        }
    }

    private fun toast(msg: String) {
        ThreadTool.runOnUI {
            ToastMgr.shortBottomCenter(Application.getContext(), msg)
        }
    }

    /**
     * 解析文件列表
     */
    private fun getTorrentInfo(
        context: Context,
        magnetUrl: String,
        file: File,
        consumer: MagnetConsumer
    ) {
        ThreadTool.runOnUI {
            try {
                val torrentInfo = XLTaskHelper.instance().getTorrentInfo(file.absolutePath)
                if (torrentInfo == null || TextUtils.isEmpty(torrentInfo.mInfoHash)) {
                    ToastMgr.longCenter(Application.getContext(), "链接解析失败")
                    return@runOnUI
                }
                val arrayList = ArrayList<TorrentFileInfo>()
                val torrentFileInfoArr = torrentInfo.mSubFileInfo
                if (!(torrentFileInfoArr == null || torrentFileInfoArr.isEmpty())) {
                    for (torrentFileInfo in listOf(*torrentInfo.mSubFileInfo)) {
                        if (isMedia(torrentFileInfo.mFileName)) {
                            torrentFileInfo.torrentPath = file.absolutePath
                            arrayList.add(torrentFileInfo)
                        }
                    }
                }
                if (arrayList.size == 0) {
                    ToastMgr.longCenter(Application.getContext(), "空的磁力链")
                    return@runOnUI
                } else {
                    onFileSelect(context, magnetUrl, file, arrayList, consumer)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }


    /**
     * 选择视频
     */
    private fun onFileSelect(
        context: Context,
        magnetUrl: String,
        file: File,
        arrayList: ArrayList<TorrentFileInfo>,
        consumer: MagnetConsumer
    ) {
        if (arrayList.size == 1) {
            val info0 = arrayList[0]
            download(file, info0, arrayList, consumer)
            return
        }
        XPopup.Builder(context)
            .moveUpToKeyboard(false) //如果不加这个，评论弹窗会移动到软键盘上面
            .asCustom(
                XiuTanResultPopup(context).with(
                    arrayList.map { DetectedMediaResult(it.mFileIndex.toString(), it.mFileName) }
                ) { url1: String, type: String ->
                    if ("play" == type) {
                        for (info in arrayList) {
                            if (info.mFileIndex.toString() == url1) {
                                download(file, info, arrayList, consumer)
                                break
                            }
                        }
                    } else if ("复制链接" == type) {
                        ClipboardUtil.copyToClipboard(context, magnetUrl)
                    } else {
                        ShareUtil.findChooserToDeal(context, magnetUrl)
                    }
                }.withTitle("选择视频")
            )
            .show()
    }

    /**
     * 提交下载
     */
    private fun download(
        file: File,
        info: TorrentFileInfo,
        arrayList: ArrayList<TorrentFileInfo>,
        consumer: MagnetConsumer
    ) {
        stopTask()
        val sb = StringBuilder()
        sb.append(path)
        sb.append("/")
        val str: String = info.mFileName
        sb.append(str.substring(0, str.lastIndexOf(".")))
        val savePath = sb.toString()
        taskId =
            XLTaskHelper.instance().addTorrentTask(file.absolutePath, savePath, info.mFileIndex)
        if (taskId < 0) {
            toast("磁链下载失败")
            return
        }

        var count = 0
        getActiveScope().launch(Dispatchers.IO) {
            //解析中
            var downStatus = 3
            while (downStatus == 3 && isActive) {
                count++
                if (count > 17) {
//                    toast("磁力链接解析超时")
                    return@launch
                }
                val taskInfo: XLTaskInfo =
                    XLTaskHelper.instance().getBtSubTaskInfo(taskId, info.mFileIndex).mTaskInfo
                when (taskInfo.mTaskStatus) {
                    3 -> {
                        //下载失败
                        if (errorCode.containsKey(taskInfo.mErrorCode)) {
                            toast(errorCode[taskInfo.mErrorCode]!!)
                        } else {
                            toast("连接失败: ErrorCode=" + taskInfo.mErrorCode)
                        }
                        return@launch
                    }
                    1, 4 -> {
                        //下载中
                        downStatus = 4
                        play(savePath, info, arrayList, consumer)
                    }
                    2 -> {
                        //下载完成
                        downStatus = 5
                        play(savePath, info, arrayList, consumer)
                    }
                }
                delay(300)
            }
        }
    }


    /**
     * 提交下载，没有多文件
     */
    private fun downloadFTP(
        url: String,
        fileName: String,
        consumer: MagnetConsumer
    ) {
        stopTask()
        taskId =
            XLTaskHelper.instance().addThunderTask(url, path, fileName)
        if (taskId < 0) {
            toast("提交下载失败")
            return
        }
        var count = 0
        getActiveScope().launch(Dispatchers.IO) {
            var downStatus = 3
            while (downStatus == 3 && isActive) {
                count++
                if (count > 17) {
//                    toast("磁力链接解析超时")
                    return@launch
                }
                val taskInfo: XLTaskInfo = XLTaskHelper.instance().getTaskInfo(taskId)
                when (taskInfo.mTaskStatus) {
                    3 -> {
                        //下载失败
                        if (errorCode.containsKey(taskInfo.mErrorCode)) {
                            toast(errorCode[taskInfo.mErrorCode]!!)
                        } else {
                            toast("连接失败: ErrorCode=" + taskInfo.mErrorCode)
                        }
                        return@launch
                    }
                    1, 4 -> {
                        //下载中
                        downStatus = 4
                        playFTP(fileName, consumer)
                    }
                    2 -> {
                        //下载完成
                        downStatus = 5
                        playFTP(fileName, consumer)
                    }
                }
                delay(300)
            }
        }
    }

    /**
     * 边下边播
     */
    private fun play(
        savePath: String,
        info: TorrentFileInfo,
        arrayList: ArrayList<TorrentFileInfo>,
        consumer: MagnetConsumer
    ) {
        ThreadTool.runOnUI {
            savePathNow = savePath
            consumer.consume(
                XLTaskHelper.instance().getLoclUrl(savePath + File.separator + info.mFileName),
                info.mFileName,
                arrayList
            )
        }
    }

    /**
     * 边下边播
     */
    private fun playFTP(
        fileName: String,
        consumer: MagnetConsumer
    ) {
        ThreadTool.runOnUI {
            consumer.consume(
                XLTaskHelper.instance().getLoclUrl(path + File.separator + fileName),
                fileName,
                arrayListOf()
            )
        }
    }

    fun playTorrentFile(
        info: TorrentFileInfo,
        consumer: MagnetVideoConsumer
    ) {
        download(File(info.torrentPath), info, ArrayList(), object : MagnetConsumer {
            override fun consume(
                url: String,
                name: String,
                arrayList: java.util.ArrayList<TorrentFileInfo>
            ) {
                consumer.consume(url, name)
            }
        })
    }

    /**
     * 只提取视频文件
     */
    private fun isMedia(str: String): Boolean {
        val vodTypeList: List<String> = ArrayList(
            listOf(
                ".rmvb",
                ".avi",
                ".mkv",
                ".flv",
                ".mp4",
                ".rm",
                ".vob",
                ".wmv",
                ".mov",
                ".3gp",
                ".asf",
                "mpg",
                "mpeg",
                "mpe"
            )
        )
        for (i in vodTypeList.indices) {
            if (str.lowercase(Locale.getDefault()).trim { it <= ' ' }.endsWith(vodTypeList[i])) {
                return true
            }
        }
        return false
    }

    /**
     * 自动取消之前的任务
     */
    private fun getActiveScope(autoCancel: Boolean = false): CoroutineScope {
        if (scope != null) {
            if (!autoCancel && scope?.isActive == true) {
                return scope!!
            }
            ThreadTool.cancelScope(scope!!)
        }
        scope = ThreadTool.newScope()
        return scope!!
    }

    private fun toChapters(
        magnetUrl: String,
        u: String,
        name: String,
        list: java.util.ArrayList<TorrentFileInfo>
    ): MutableList<VideoChapter> {
        val chapters: MutableList<VideoChapter> =
            java.util.ArrayList()
        for (torrentFileInfo in list) {
            val chapter = VideoChapter()
            chapter.torrentFileInfo = torrentFileInfo
            chapter.memoryTitle = torrentFileInfo.mFileName
            chapter.title = torrentFileInfo.mFileName
            chapter.url = if (StringUtils.equals(
                    name,
                    torrentFileInfo.mFileName
                )
            ) u else magnetUrl
            chapter.isUse = StringUtils.equals(
                name,
                torrentFileInfo.mFileName
            )
            chapter.originalUrl = chapter.url
            chapters.add(chapter)
        }
        return chapters
    }
}
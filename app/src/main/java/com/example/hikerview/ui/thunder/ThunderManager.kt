package com.example.hikerview.ui.thunder

import android.content.Context
import android.net.Uri
import android.text.TextUtils
import com.alibaba.fastjson.JSON
import com.example.hikerview.service.http.CodeUtil
import com.example.hikerview.service.parser.HttpParser
import com.example.hikerview.service.parser.JSEngine
import com.example.hikerview.service.parser.RetroLoadLibrary
import com.example.hikerview.ui.ActivityManager
import com.example.hikerview.ui.Application
import com.example.hikerview.ui.browser.model.DetectedMediaResult
import com.example.hikerview.ui.browser.model.UrlDetector
import com.example.hikerview.ui.video.PlayerChooser
import com.example.hikerview.ui.video.VideoChapter
import com.example.hikerview.ui.view.XiuTanResultPopup
import com.example.hikerview.utils.ClipboardUtil
import com.example.hikerview.utils.FileUtil
import com.example.hikerview.utils.PreferenceMgr
import com.example.hikerview.utils.ShareUtil
import com.example.hikerview.utils.StringUtil
import com.example.hikerview.utils.ThreadTool
import com.example.hikerview.utils.ToastMgr
import com.example.hikerview.utils.UriUtils
import com.lxj.xpopup.XPopup
import com.xunlei.downloadlib.XLDownloadManager
import com.xunlei.downloadlib.XLTaskHelper
import com.xunlei.downloadlib.parameter.TorrentFileInfo
import com.xunlei.downloadlib.parameter.XLTaskInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.apache.commons.lang3.StringUtils
import java.io.File
import java.net.URLDecoder
import java.util.Locale

/**
 * 作者：By 15968
 * 日期：On 2022/6/12
 * 时间：At 22:18
 */
object ThunderManager {

    //磁力引擎，0代表迅雷，1代表TorrentStream
    val engine: String
        get() = PreferenceMgr.getString(context, "magnet", "")

    var taskId: Long = 0
    var path: String = ""
    var scope: CoroutineScope? = null
    val errorCode = HashMap<Int, String>()
    var savePathNow: String = ""
    var mFilePathNow: String = ""
    var plugin: TorrentEngine? = null

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

    fun globalInit(context: Context) {
        scanPlugins(context)
        plugin?.initConfig()
    }

    private fun scanPlugins(context: Context) {
        plugin = scanPlugin(context, engine)
    }

    fun scanPlugin(context: Context, engine: String): TorrentEngine? {
        if (engine == "") {
            return null
        }
        val p = UriUtils.getRootDir(context) + File.separator + "plugins"
        val f = File(p)
        if (!f.exists() && !f.mkdirs()) {
            return null
        }
        val json = File(p + File.separator + "magnet.json")
        if (json.exists()) {
            val magnets = JSON.parseArray(
                FileUtil.fileToString(json.absolutePath),
                TorrentEngineBean::class.java
            )
            if (!magnets.isNullOrEmpty()) {
                for (magnet in magnets) {
                    if (magnet.name == engine) {
                        return loadPlugin(magnet)
                    }
                }
            }
        }
        return null
    }

    private fun loadPlugin(bean: TorrentEngineBean): TorrentEngine? {
        try {
            val soDir = Application.getContext().getDir("libs", Context.MODE_PRIVATE)
            if (bean.soFile.isNotEmpty()) {
                val soFile = File(soDir.toString(), "lib" + bean.soFile + ".so")
                if (!soFile.exists()) {
                    return null
                }
            }
            try {
                RetroLoadLibrary.installNativeLibraryPath(javaClass.classLoader, soDir)
            } catch (throwable: Throwable) {
                throwable.printStackTrace()
            }
            try {
                RetroLoadLibrary.installNativeLibraryPath(
                    javaClass.classLoader,
                    soDir
                )
            } catch (throwable: Throwable) {
                throwable.printStackTrace()
            }
            val instance1: Any? = JSEngine.getInstance().loadJavaInstance(bean.checkClass)
            if (instance1 == null) {
                return null
            }
            val instance: Any? = JSEngine.getInstance().loadJavaInstance(bean.className)
            if (instance != null) {
                val te = instance as TorrentEngine
                te.setContextProvider { context }
                return te
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
        return null
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
        if (url.isNullOrEmpty()) {
            toast("链接不能为空")
            return
        }
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
            val headers = HttpParser.getHeaders(path) ?: HashMap<String, String>()
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
        downloadFTP(context, url, fileName, consumer)
    }

    private fun startParseTorrent0(context: Context, path: String, c: MagnetConsumer? = null) {
        if (plugin != null) {
            plugin?.initEngine()
            plugin?.stopTask()
            plugin?.parse("file://$path")
            toast("种子加载中，请稍候")
            return
        }
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

    private fun initDir(context: Context) {
        path = UriUtils.getRootDir(context) + File.separator + "magnet"
        val dir = File(path)
        if (!dir.exists()) {
            dir.mkdirs()
        }
    }

    private fun initXL(context: Context) {
        XLTaskHelper.init(context, XL.getA(), XL.getB())
        initDir(context)
    }

    fun isWorking(): Boolean {
        if (isDownloadFinished()) {
            return true
        }
        if (plugin != null) {
            return plugin?.isDownloading == true
        }
        return taskId > 0
    }

    fun isDownloadFinished(): Boolean {
        if (plugin != null) {
            return plugin?.isDownloadFinished == true
        }
        if (taskId > 0) {
            val taskInfo = XLTaskHelper.instance().getTaskInfo(taskId)
            if (taskInfo != null) {
                return taskInfo.mTaskStatus == 2
            }
        }
        return false
    }

    fun getDownloaded(): String {
        if (plugin != null) {
            return plugin?.progress ?: ""
        }
        if (taskId > 0) {
            val taskInfo = XLTaskHelper.instance().getTaskInfo(taskId)
            if (taskInfo != null) {
                return FileUtil.getFormatedFileSize(taskInfo.mDownloadSize) + "/" +
                        FileUtil.getFormatedFileSize(taskInfo.mFileSize)
            }
        }
        return ""
    }

    fun getVideoFile(url: String): String {
        if (url.startsWith("/")) {
            return url
        }
        if (url.startsWith("http://")) {
            if (url.contains("#file=")) {
                val fileName = HttpParser.decodeUrl(url.split("#file=")[1], "UTF-8")
                return findVideoFile(path + File.separator + fileName).absolutePath
            }
            if (mFilePathNow.isNotEmpty() && File(mFilePathNow).exists()) {
                return mFilePathNow
            }
            val p = StringUtil.listToString(
                url.replace("http://", "").split("/").toList(),
                1,
                "/"
            )
            var path = HttpParser.decodeUrl(p, "UTF-8")
            if (path.startsWith("%2F")) {
                path = HttpParser.decodeUrl(path, "UTF-8")
            }
            return findVideoFile(path).absolutePath
        }
        return ""
    }

    private val context: Context
        get() = ActivityManager.instance.currentActivity

    /**
     * 解析磁力
     */
    private fun parse(context: Context, url: String, consumer: MagnetConsumer) {
        if (plugin != null) {
            plugin?.initEngine()
            plugin?.stopTask()
            plugin?.parse(url)
            toast("磁链解析中，请稍候")
            return
        }
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
            val taskId = XLTaskHelper.instance().addMagentTask(url, dir!!.absolutePath, fileName)
            this@ThunderManager.taskId = taskId
            var count = 0
            var hasToast = false
            while (isActive) {
                count++
                if (count > 240) {
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
                        delay(500)
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

    suspend fun parseMagnetToTorrent(context: Context, url: String): File? {
        initXL(context)
        val decode = URLDecoder.decode(url)
        val fileName = XLTaskHelper.instance().getFileName(decode)
        val file = File(path + File.separator + fileName)
        val dir = file.parentFile
        val taskId = XLTaskHelper.instance().addMagentTask(url, dir!!.absolutePath, fileName)
        this@ThunderManager.taskId = taskId
        var count = 0
        while (count < 12) {
            count++
            val i = XLTaskHelper.instance().getTaskInfo(taskId).mTaskStatus
            when {
                i == 2 -> {
                    XLTaskHelper.instance().stopTask(taskId)
                    return file
                }
                i != 3 -> {
                    delay(300)
                }
                else -> {
                    XLTaskHelper.instance().stopTask(taskId)
                    return null
                }
            }
        }
        try {
            XLTaskHelper.instance().stopTask(taskId)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }


    fun release() {
        release(null)
    }

    /**
     * 释放文件
     */
    fun release(exclude: String? = null) {
        try {
            Application.application.stopMagnetStatusService()
        } catch (e: Throwable) {
            e.printStackTrace()
        }
        stopTask()
        val files = File(path).listFiles()
        files?.let {
            for (file in it) {
                if (exclude != null && file.absolutePath == exclude) {
                    continue
                }
                try {
                    if (file.isDirectory) {
                        FileUtil.deleteDirs(file.absolutePath)
                    } else {
                        file.delete()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        scope?.let {
            ThreadTool.cancelScope(it)
        }
    }

    private fun stopTask() {
        if (taskId > 0) {
            XLTaskHelper.instance().stopTask(taskId)
            taskId = 0
        }
        if (plugin != null) {
            plugin?.stopTask()
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
            download(context, magnetUrl, file, info0, arrayList, consumer)
            return
        }
        XPopup.Builder(context)
            .moveUpToKeyboard(false) //如果不加这个，评论弹窗会移动到软键盘上面
            .asCustom(
                XiuTanResultPopup(context).withDismissOnClick(false).with(
                    arrayList.map { DetectedMediaResult(it.mFileIndex.toString(), it.mFileName) }
                ) { url1: String, type: String ->
                    if ("play" == type) {
                        for (info in arrayList) {
                            if (info.mFileIndex.toString() == url1) {
                                download(context, magnetUrl, file, info, arrayList, consumer)
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
        context: Context,
        magnetUrl: String?,
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
                        val msg = if (magnetUrl == null) "" else "，用第三方软件试试吧"
                        if (errorCode.containsKey(taskInfo.mErrorCode)) {
                            toast(errorCode[taskInfo.mErrorCode]!! + msg)
                        } else {
                            toast("连接失败: ErrorCode=" + taskInfo.mErrorCode + msg)
                        }
                        if (magnetUrl != null) {
                            ThreadTool.runOnUI {
                                ShareUtil.findChooserToDeal(context, magnetUrl)
                            }
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
        context: Context,
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
                            toast(errorCode[taskInfo.mErrorCode]!! + "，用第三方软件试试吧")
                        } else {
                            toast("连接失败: ErrorCode=" + taskInfo.mErrorCode + "，用第三方软件试试吧")
                        }
                        ThreadTool.runOnUI {
                            ShareUtil.findChooserToDeal(context, url)
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
            mFilePathNow = savePath + File.separator + info.mFileName
            val file = findVideoFile(mFilePathNow)
            val u: String? = XLTaskHelper.instance().getLoclUrl(file.absolutePath)
            if (u == null) {
                toast("当前文件格式不支持云播，试试第三方播放器吧")
                ShareUtil.findChooserToDeal(
                    ActivityManager.instance.currentActivity,
                    "file://" + file.absoluteFile
                )
                return@runOnUI
            }
            consumer.consume(
                u!!,
                info.mFileName,
                arrayList
            )
            try {
                Application.application.startMagnetStatusService()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun findVideoFile(path: String): File {
        val file = File(path)
        if (file.exists() && !file.isDirectory) {
            return file
        }
        var dir = file.parentFile
        if (dir == null || !dir.exists()) {
            //去根目录找，可能是名字包含+等特殊字符
            dir = File(UriUtils.getRootDir(context) + File.separator + "magnet")
            //return file
        }
        val name = file.name
        //优先通过文件名找
        val file1 = findFile({
            it.name == name
        }, dir)
        if (file1 != null) {
            return file1
        }
        //找不到就随便找个视频文件
        val file2 = findFile({
            UrlDetector.isVideoOrMusic(it.name)
        }, dir)
        if (file2 != null) {
            return file2
        }
        //还找不到就往根目录找
        val file3 = findFile({
            it.name == name
        }, File(UriUtils.getRootDir(context) + File.separator + "magnet"))
        if (file3 != null) {
            return file3
        }
        //还找不到就往根目录随便找个视频文件
        val file4 = findFile({
            UrlDetector.isVideoOrMusic(it.name)
        }, File(UriUtils.getRootDir(context) + File.separator + "magnet"))
        if (file4 != null) {
            return file4
        }
        //都找不到，没辙了
        return file
    }

    /**
     * 遍历找文件
     */
    private fun findFile(found: (file: File) -> Boolean, dir: File): File? {
        if (!dir.exists() || !dir.isDirectory) {
            return null
        }
        val files = dir.listFiles()
        if (files == null || files.isEmpty()) {
            return null
        }
        for (file in files) {
            if (!file.isDirectory) {
                if (found(file)) {
                    return file
                }
            } else {
                val f = findFile(found, file)
                if (f != null) {
                    return f
                }
            }
        }
        return null
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
                XLTaskHelper.instance()
                    .getLoclUrl(findVideoFile(path + File.separator + fileName).absolutePath),
                fileName,
                arrayListOf()
            )
            try {
                Application.application.startMagnetStatusService()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun playTorrentFile(
        context: Context,
        info: TorrentFileInfo,
        consumer: MagnetVideoConsumer
    ) {
        download(context, null, File(info.torrentPath), info, ArrayList(), object : MagnetConsumer {
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
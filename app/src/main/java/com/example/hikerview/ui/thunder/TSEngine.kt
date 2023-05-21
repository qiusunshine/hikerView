package com.example.hikerview.ui.thunder

import android.content.Context
import com.example.hikerview.event.home.LoadingEvent
import com.example.hikerview.service.parser.HttpParser
import com.example.hikerview.ui.Application
import com.example.hikerview.ui.browser.model.DetectedMediaResult
import com.example.hikerview.ui.setting.office.MoreSettingOfficer
import com.example.hikerview.ui.video.PlayerChooser
import com.example.hikerview.ui.view.XiuTanResultPopup
import com.example.hikerview.ui.webdlan.LocalServerParser
import com.example.hikerview.utils.ClipboardUtil
import com.example.hikerview.utils.FileUtil
import com.example.hikerview.utils.PreferenceMgr
import com.example.hikerview.utils.ShareUtil
import com.example.hikerview.utils.ThreadTool
import com.example.hikerview.utils.ToastMgr
import com.example.hikerview.utils.UriUtils
import com.github.se_bastiaan.torrentstream.StreamStatus
import com.github.se_bastiaan.torrentstream.Torrent
import com.github.se_bastiaan.torrentstream.TorrentOptions
import com.github.se_bastiaan.torrentstreamserver.TorrentServerListener
import com.github.se_bastiaan.torrentstreamserver.TorrentStreamServer
import com.lxj.xpopup.XPopup
import com.xunlei.downloadlib.XLTaskHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.joor.Reflect
import org.libtorrent4j.AnnounceEntry
import java.io.File
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 作者：By 15968
 * 日期：On 2022/9/20
 * 时间：At 20:01
 */
class TSEngine : TorrentEngine() {
    private var torrentStreamServer: TorrentStreamServer? = null
    private val context: Context
        get() = contextProvider.get()!!
    private var played = AtomicBoolean(false)
    private var scope: CoroutineScope? = null

    private val torrentListener = object : TorrentServerListener {
        private var bufferProgress = 0
        private var name = ""

        private fun startDownloadNow(
            torrent: Torrent,
            fileNames: Array<String>,
            name: String
        ) {
            for (item in fileNames.withIndex()) {
                if (item.value == name) {
                    torrent.setSelectedFileIndex(item.index)
                    EventBus.getDefault()
                        .post(LoadingEvent("为流畅播放而努力中，请稍候", true))
                    torrent.startDownload()
                    try {
                        Reflect.on(torrent).set("preparePieces", ArrayList<Int>())
                        Reflect.on(torrent).set("prepareProgress", (99F).toDouble())
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    try {
                        Application.application.startMagnetStatusService()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    break
                }
            }
        }

        override fun onStreamPrepared(torrent: Torrent) {
            bufferProgress = 0
            name = ""
            played.set(false)
            try {
                addTrackers(torrent)
                if (torrent.piecesToPrepare != null && torrent.piecesToPrepare > 0) {
                    Reflect.on(torrent).set("preparePieces", arrayListOf(0, 1))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            val fileNames = torrent.fileNames
            val files = fileNames.filter { isMedia(it) }
            if (files.isEmpty()) {
                ToastMgr.shortBottomCenter(context, "空的磁力链")
            } else if (files.size == 1) {
                startDownloadNow(torrent, fileNames, files[0])
            } else {
                torrent.pause()
                XPopup.Builder(context)
                    .moveUpToKeyboard(false)
                    .asCustom(
                        XiuTanResultPopup(context).withDismissOnClick(
                            true
                        ).with(
                            files.map {
                                DetectedMediaResult(
                                    it,
                                    it
                                )
                            }
                        ) { url1: String, type: String ->
                            if ("play" == type) {
                                startDownloadNow(torrent, fileNames, url1)
                            } else if ("复制链接" == type) {
                                ClipboardUtil.copyToClipboard(
                                    context,
                                    torrent.torrentHandle.makeMagnetUri()
                                )
                            } else {
                                ShareUtil.findChooserToDeal(
                                    context,
                                    torrent.torrentHandle.makeMagnetUri()
                                )
                            }
                        }.withTitle("选择视频")
                    )
                    .show()
            }
        }

        override fun onStreamStarted(torrent: Torrent) {
            EventBus.getDefault().post(LoadingEvent("为流畅播放而努力中 1%", true))
        }

        override fun onStreamError(torrent: Torrent?, e: java.lang.Exception?) {
            bufferProgress = 0
            name = ""
            played.set(true)
            EventBus.getDefault().post(LoadingEvent("", false))
            ToastMgr.shortBottomCenter(context, "出错：" + e?.message)
            e?.printStackTrace()
        }

        override fun onStreamReady(torrent: Torrent) {

        }

        private fun canServe(name: String): Boolean {
            val ext = arrayOf(".mp4", ".avi", ".mkv", ".3gp", ".mov")
            for (s in ext) {
                if (name.endsWith(s)) {
                    return true
                }
            }
            return false
        }

        override fun onServerReady(url: String?) {
            try {
                EventBus.getDefault().post(LoadingEvent("", false))
                val file = torrentStreamServer!!.currentTorrent.videoFile
                name = file.name
                if (played.get()) {
                    return
                }
                played.set(true)
                if (!canServe(name)) {
                    XLTaskHelper.init(context, XL.getA(), XL.getB())
                    val u: String? = XLTaskHelper.instance().getLoclUrl(file.absolutePath)
                    if (u == null) {
                        PlayerChooser.startPlayer(
                            context,
                            file.name,
                            "file://" + file.absolutePath,
                            null
                        )
                    } else {
                        PlayerChooser.startPlayer(context, file.name, u, null)
                    }
                } else {
                    PlayerChooser.startPlayer(
                        context,
                        name,
                        url?.replace(
                            "0.0.0.0",
                            LocalServerParser.getIP(context)
                        ) + "#file=" + HttpParser.encodeUrl(name),
                        null
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        override fun onStreamProgress(torrent: Torrent, status: StreamStatus) {
            if (status.bufferProgress <= 100 && bufferProgress < 100 && bufferProgress < status.bufferProgress) {
                bufferProgress = status.bufferProgress
                EventBus.getDefault().post(LoadingEvent("为流畅播放而努力中 $bufferProgress%", true))
            }
        }

        override fun onStreamStopped() {
        }

    }

    override fun initConfig() {
        initEngine()
    }

    override fun initEngine() {
        if (torrentStreamServer == null) {
            initDir(context)
            torrentStreamServer = TorrentStreamServer.getInstance()
            torrentStreamServer?.setTorrentOptions(buildOptions())
            torrentStreamServer?.setServerHost("0.0.0.0")
            torrentStreamServer?.setServerPort(52121)
            torrentStreamServer?.startTorrentStream()
            torrentStreamServer?.addListener(torrentListener)
        }
    }

    private fun buildOptions(): TorrentOptions {
        return TorrentOptions.Builder()
            .saveLocation(UriUtils.getRootDir(context) + File.separator + "magnet")
            .removeFilesAfterStop(true)
            .autoDownload(false)
            .prepareSize(1 * 1024L * 1024L)
            .build()
    }

    override fun isDownloading(): Boolean {
        return torrentStreamServer?.currentTorrent != null || torrentStreamServer?.isStreaming == true
    }

    override fun isDownloadFinished(): Boolean {
        if (torrentStreamServer?.currentTorrent == null) {
            return false
        }
        val status = torrentStreamServer?.currentTorrent?.torrentHandle?.status()
        return status?.progress() ?: 0F >= 1F
    }

    override fun parse(url: String?): Boolean {
        if (!url.isNullOrEmpty()) {
            val useThunder = PreferenceMgr.getBoolean(context, "magnetThunder", false)
            if (!useThunder || !url.startsWith("magnet")) {
                torrentStreamServer?.startStream(addTrackers(url))
            } else {
                scope?.cancel()
                scope = ThreadTool.newScope()
                scope?.launch(Dispatchers.IO) {
                    val file = ThunderManager.parseMagnetToTorrent(context, url)
                    if (isActive) {
                        if (file?.exists() == true) {
                            torrentStreamServer?.startStream("file://" + file.absolutePath)
                        } else {
                            torrentStreamServer?.startStream(addTrackers(url))
                        }
                    }
                }
            }
        }
        return true
    }

    private fun addTrackers(torrent: Torrent) {
        try {
            if (torrent.torrentHandle != null) {
                val size = torrent.torrentHandle?.trackers()?.size ?: 0
                if (size < 50) {
                    val trackers = MoreSettingOfficer.getTrackers(context)
                    if (!trackers.isNullOrEmpty()) {
                        for (s in trackers.split("\n")) {
                            if (!s.isNullOrEmpty()) {
                                torrent.torrentHandle.addTracker(AnnounceEntry(s))
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun addTrackers(url: String): String {
        if (url.startsWith("magnet:")) {
            var u = url
            val trackers = MoreSettingOfficer.getTrackers(context)
            if (!trackers.isNullOrEmpty()) {
                for (s in trackers.split("\n")) {
                    if (!s.isNullOrEmpty()) {
                        u = u + "&tr=" + HttpParser.encodeUrl(s)
                    }
                }
            }
            return u
        }
        return url
    }

    override fun stopTask() {
        torrentStreamServer?.stopStream()
        scope?.cancel()
    }

    override fun release() {
        stopTask()
    }

    override fun getProgress(): String {
        val status = torrentStreamServer?.currentTorrent?.torrentHandle?.status()
        val ok = status?.totalDownload() ?: 0L
        val all = status?.totalWanted() ?: 0L
        return if (all <= 0) "" else {
            FileUtil.getFormatedFileSize(ok) + "/" + FileUtil.getFormatedFileSize(
                all
            )
        }
    }

    private fun initDir(context: Context) {
        ThunderManager.path = UriUtils.getRootDir(context) + File.separator + "magnet"
        val dir = File(ThunderManager.path)
        if (!dir.exists()) {
            dir.mkdirs()
        }
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
}
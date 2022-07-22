package com.example.hikerview.ui.video

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.text.TextUtils
import android.view.*
import android.view.WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
import android.widget.*
import chuangyuan.ycj.videolibrary.listener.VideoInfoListener
import chuangyuan.ycj.videolibrary.video.ExoUserPlayer
import chuangyuan.ycj.videolibrary.video.GestureVideoPlayer.DoubleTapArea
import chuangyuan.ycj.videolibrary.video.ManualPlayer
import chuangyuan.ycj.videolibrary.video.VideoPlayerManager
import chuangyuan.ycj.videolibrary.widget.VideoPlayerView
import com.example.hikerview.R
import com.example.hikerview.constants.MediaType
import com.example.hikerview.constants.PreferenceConstant
import com.example.hikerview.constants.RemotePlayConfig
import com.example.hikerview.event.video.OnDeviceUpdateEvent
import com.example.hikerview.service.parser.HttpParser
import com.example.hikerview.ui.Application
import com.example.hikerview.ui.browser.model.DetectedMediaResult
import com.example.hikerview.ui.browser.model.DetectorManager
import com.example.hikerview.ui.browser.model.UrlDetector
import com.example.hikerview.ui.browser.model.VideoDetector
import com.example.hikerview.ui.browser.util.CollectionUtil
import com.example.hikerview.ui.dlan.DLandataInter
import com.example.hikerview.ui.dlan.DlanListPop
import com.example.hikerview.ui.dlan.MediaPlayActivity
import com.example.hikerview.ui.download.DownloadDialogUtil
import com.example.hikerview.ui.webdlan.LocalServerParser
import com.example.hikerview.ui.webdlan.RemoteServerManager
import com.example.hikerview.ui.webdlan.model.DlanUrlDTO
import com.example.hikerview.utils.*
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.Format
import com.google.android.exoplayer2.analytics.AnalyticsListener.EventTime
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.material.snackbar.Snackbar
import com.lxj.xpopup.XPopup
import com.qingfeng.clinglibrary.service.manager.ClingManager
import com.yanzhenjie.andserver.Server.ServerListener
import org.apache.commons.lang3.StringUtils
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import timber.log.Timber
import kotlin.math.abs
import kotlin.math.max

/**
 * 作者：By 15968
 * 日期：On 2022/5/22
 * 时间：At 10:54
 */
class FloatVideoController(
    var context: Activity,
    var container: ViewGroup,
    var pauseWebView: (pause: Boolean, force: Boolean) -> Int,
    var videoDetector: VideoDetector,
    var webHolder: WebHolder
) : View.OnClickListener {

    private var url: String = ""
    private var webUrl: String = ""
    private var title: String = ""
    private var position: Long = 0
    private var showing = false
    private var holderView: View? = null

    private var exo_bg_video_top: View? = null
    private var custom_lock_screen_bg: android.view.View? = null
    private var custom_control_bottom: android.view.View? = null
    private var exo_controller_bottom: android.view.View? = null
    private var exo_play_pause2: ImageView? = null
    private var exo_pip: View? = null
    private var layoutNow = VideoPlayerView.Layout.VERTICAL
    private var player: ManualPlayer? = null
    private var playerView: VideoPlayerView? = null
    private var listCard: View? = null
    private val timeView: TextView? = null
    private var descView: android.widget.TextView? = null
    private var listScrollView: ScrollView? = null
    private var video_str_view: TextView? = null
    private var audio_str_view: android.widget.TextView? = null
    private var video_address_view: android.widget.TextView? = null
    private var dlanListPop: DlanListPop? = null
    private var webDlanPlaying: Boolean = false
    private var analyticsListener: SimpleAnalyticsListener? = null
    private var vaildTicket: Long = 0

    /**
     * 初始化View
     */
    @SuppressLint("UseCompatLoadingForDrawables")
    private fun initView() {
        if (playerView != null) {
            return
        }
        val view = LayoutInflater.from(context).inflate(R.layout.view_float_video, null)
        val params = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        container.addView(view, params)
        holderView = view
        val pv = view.findViewById(R.id.float_video_player) as VideoPlayerView
        val layoutParams = pv.layoutParams as FrameLayout.LayoutParams
        layoutParams.height = ScreenUtil.getScreenMin(context) * 9 / 16
        pv.layoutParams = layoutParams
        exo_bg_video_top = view.findViewById(R.id.exo_controller_top)
        custom_lock_screen_bg = view.findViewById(R.id.custom_lock_screen_bg)
        custom_control_bottom = view.findViewById(R.id.custom_control_bottom)
        exo_controller_bottom = view.findViewById(R.id.exo_controller_bottom)
        exo_play_pause2 = view.findViewById(R.id.exo_play_pause2)
        exo_pip = view.findViewById(R.id.exo_pip)
        exo_pip?.setOnClickListener(this)
        descView = view.findViewById(R.id.custom_toolbar_desc)
        pv.bottomAnimateViews = listOf(
            custom_lock_screen_bg,
            custom_control_bottom,
            view.findViewById(R.id.exo_video_fullscreen),
            view.findViewById(R.id.exo_video_switch),
            exo_play_pause2
        )
        exo_play_pause2?.setOnClickListener { v: View? ->
            player?.let {
                it.setStartOrPause(
                    !it.isPlaying
                )
            }
        }
        pv.exoFullscreen.setOnClickListener { v: View? ->
            val format = player?.player?.videoFormat
            if (format != null) {
                if (format.width < format.height) {
                    if (playerView?.isNowVerticalFullScreen != true) {
                        playerView?.verticalFullScreen()
                        return@setOnClickListener
                    }
                }
            }
            playerView?.enterFullScreen()
        }
        pv.rightAnimateView = exo_pip
        pv.exoControlsBack.setOnClickListener {
            if (layoutNow != VideoPlayerView.Layout.VERTICAL) {
                playerView?.exoFullscreen?.performClick()
            } else {
                destroy()
            }
        }
        pv.setOnClickListener {

        }
        playerView = pv
        pv.playerView.setShowControllerIndefinitely(true)
        pv.setOnLayoutChangeListener {
            layoutNow = it
            if (it == VideoPlayerView.Layout.VERTICAL) {
                //非全屏
                if (listCard!!.visibility == View.VISIBLE) {
                    listCard!!.visibility = View.INVISIBLE
                }
            }
            if (it == VideoPlayerView.Layout.VERTICAL) {
                playerView?.playbackControlView?.postDelayed({
                    playerView?.playerView?.isShowControllerIndefinitely = true
                    playerView?.showControllerForce()
                    playerView?.playbackControlView?.postDelayed({
                        playerView?.showControllerForce()
                    }, 500)
                }, 300)
                pauseWebView(false, true)
                val lpa: WindowManager.LayoutParams = context.window.attributes
                lpa.screenBrightness = BRIGHTNESS_OVERRIDE_NONE
                context.window.attributes = lpa
            } else {
                pauseWebView(true, true)
                playerView!!.playerView.isShowControllerIndefinitely = false
            }
            //记忆进度
            player?.let { p ->
                if (p.currentPosition > 0) {
                    HeavyTaskUtil.saveNowPlayerPos(
                        getContext(),
                        getMemoryId(),
                        p.currentPosition
                    )
                }
            }
        }
        pv.playbackControlView
            .setPlayPauseListener { isPlaying: Boolean ->
                exo_play_pause2?.setImageDrawable(
                    context.resources
                        .getDrawable(if (isPlaying) R.drawable.ic_pause_ else R.drawable.ic_play_)
                )
            }
        pv.findViewById<View>(R.id.custom_download).setOnClickListener {
            var t: String = title.replace(" ", "")
            if (t.length > 85) {
                t = t.substring(0, 85)
            }
            DownloadDialogUtil.showEditDialog(context, t, url)
        }
        val networkNotify =
            PreferenceMgr.getBoolean(
                context,
                "networkNotify",
                true
            )
        pv.isNetworkNotify = networkNotify
        initActionAdapter()
        ScreenUtil.setDisplayInNotch(context)
    }

    /**
     * 加载新的网站就销毁
     */
    fun loadUrl(url: String) {
        val dom = StringUtil.getDom(webUrl)
        val domNow = StringUtil.getDom(url)
        if (!StringUtils.equals(dom, domNow) && holderView != null && showing) {
            destroy()
        }
    }

    /**
     * 嗅探到视频就显示
     */
    fun show(
        videoUrl: String,
        webUrl: String,
        title: String,
        checkDuplicate: Boolean = true
    ) {
        if (checkDuplicate && StringUtils.equals(webUrl, this.webUrl)) {
            return
        }
        videoDetector.putIntoXiuTanLiked(
            getContext(),
            StringUtil.getDom(webUrl),
            StringUtil.getDom(videoUrl)
        )
        val u = PlayerChooser.decorateHeader(
            getHeaders(webHolder.getRequestMap(), videoUrl),
            webUrl,
            videoUrl
        )
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this)
        }
        vaildTicket = System.currentTimeMillis()
        this.title = title
        this.webUrl = webUrl
        initView()
        if (player != null && url.isNotEmpty()) {
            HeavyTaskUtil.saveNowPlayerPos(getContext(), getMemoryId(), player!!.currentPosition)
        }
        updateUrl(u)
        showing = true
        if (player != null) {
            reStartPlayer(true)
            return
        }
        val realUrl = HttpParser.getRealUrlFilterHeaders(url)
        //恢复播放进度
        position = HeavyTaskUtil.getPlayerPos(context, getMemoryId()).toLong()
        player = VideoPlayerManager.Builder(VideoPlayerManager.TYPE_PLAY_MANUAL, playerView!!)
            .setTitle("")
            .setPosition(position)
            .setPlayUri(realUrl, HttpParser.getHeaders(url))
            .addVideoInfoListener(object : VideoInfoListener {
                override fun onPlayStart(currPosition: Long) {
                    pauseWebDelay()
                    player?.let {
                        if (it.duration > 0 && it.duration <= 20000 && it.player?.isCurrentWindowLive != true) {
                            //小于等于20秒的广告
                            vaildTicket = System.currentTimeMillis()
                            val count = max(5, (it.duration / 2000).toInt() + 1)
                            autoChangeXiuTanVideo(count, vaildTicket)
                        }
                    }
                }

                override fun onLoadingChanged() {}
                override fun onPlayerError(e: ExoPlaybackException?) {
                    vaildTicket = System.currentTimeMillis()
                    autoChangeXiuTanVideo(5, vaildTicket)
                }

                override fun onPlayEnd() {

                }

                override fun isPlaying(playWhenReady: Boolean) {}
            }).create()
        player?.setPlaybackParameters(VideoPlayerManager.PLAY_SPEED, 1f)
        player?.setPlayerGestureOnTouch(true)
        addFormatListener()
        player?.setVerticalMoveGestureListener { dx, dy ->
            if (abs(dy) > abs(dx) && player != null) {
                //竖向滑动
                holderView?.let {
                    var y = it.y + dy
                    if (y < 0) {
                        y = 0F
                    }
                    if (y + it.measuredHeight > container.measuredHeight) {
                        y = container.measuredHeight.toFloat() - it.measuredHeight
                    }
                    it.translationY = y
                }
            }
        }
        player?.setOnDoubleTapListener { e: MotionEvent?, tapArea: DoubleTapArea? ->
            if (player == null) {
                return@setOnDoubleTapListener
            }
            if (tapArea == DoubleTapArea.LEFT) {
                fastPositionJump(-10L)
                showFastJumpNotice(-10)
            } else if (tapArea == DoubleTapArea.RIGHT) {
                fastPositionJump(10L)
                showFastJumpNotice(10)
            } else {
                player?.setStartOrPause(!player!!.isPlaying)
            }
        }
        player?.startPlayer<ExoUserPlayer>()
        //停止网页里面的播放
        playerView?.postDelayed({
            if (playerView != null && player != null && url.isNotEmpty()) {
                pauseWebDelay()
            }
        }, 1000)
    }

    /**
     * activity onPause
     */
    fun onPause() {
        player?.onPause()
        player?.let {
            if (it.currentPosition > 0) {
                HeavyTaskUtil.saveNowPlayerPos(getContext(), getMemoryId(), it.currentPosition)
            }
        }
    }

    /**
     * activity onResume
     */
    fun onResume() {
        player?.onResume()
    }

    /**
     * activity onBackPressed
     */
    fun onBackPressed(): Boolean {
        if (playerView != null && playerView!!.exoFullscreen.isChecked) {
            if (listCard?.visibility == View.VISIBLE) {
                reverseListCardVisibility()
                return true
            }
            playerView?.exitFullView()
            return true
        }
        return false
    }

    fun isFullScreen(): Boolean {
        return layoutNow != VideoPlayerView.Layout.VERTICAL
    }

    /**
     * 销毁、隐藏
     */
    fun destroy() {
        showing = false
        webUrl = ""
        url = ""
        position = 0
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this)
        }
        if (holderView != null) {
            container.removeView(holderView)
            holderView = null
        }
        player?.let {
            try {
                val p = it.currentPosition
                player?.onDestroy()
                player = null
                if (p > 0) {
                    HeavyTaskUtil.saveNowPlayerPos(getContext(), getMemoryId(), p)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        try {
            playerView?.onDestroy()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        playerView = null
        pauseWebView(false, true)
    }


    private fun addFormatListener() {
        if (analyticsListener == null) {
            analyticsListener = object : SimpleAnalyticsListener() {
                override fun onDecoderInputFormatChanged(
                    eventTime: EventTime,
                    trackType: Int,
                    format: Format
                ) {
                    playerView?.let {
                        if (trackType == C.TRACK_TYPE_VIDEO) {
                            video_str_view!!.text = "视频格式：" + getVideoString(format)
                        } else if (trackType == C.TRACK_TYPE_AUDIO) {
                            audio_str_view!!.text = "音频格式：" + getAudioString(format)
                        }
//                        if (format.width > format.height) {
//                            val layoutParams = it.layoutParams as FrameLayout.LayoutParams
//                            layoutParams.height =
//                                ScreenUtil.getScreenMin(context) * format.height / format.width
//                            it.layoutParams = layoutParams
//                        }
                    }
                }
            }
        }
        player!!.player.removeAnalyticsListener(analyticsListener!!)
        player!!.player.addAnalyticsListener(analyticsListener!!)
    }

    private fun fastPositionJump(forward: Long) {
        if (player == null) {
            return
        }
        val newPos = player!!.currentPosition + forward * 1000
        position = if (player!!.duration < newPos) {
            player!!.duration - 1000
        } else if (newPos < 0) {
            if (forward > 0) {
                forward * 1000
            } else {
                0
            }
        } else {
            newPos
        }
        player!!.seekTo(position)
        try {
            HeavyTaskUtil.saveNowPlayerPos(getContext(), getMemoryId(), position)
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    private fun showFastJumpNotice(gap: Int) {
        val notice: String = playerView!!.notice
        var finalJump = gap
        if (StringUtil.isNotEmpty(notice)) {
            if (notice.contains("已快进") && gap < 0) {
                Timber.d("之前快进，现在快退, gap=%s", gap)
            } else if (notice.contains("已快退") && gap > 0) {
                Timber.d("之前快退，现在快进, gap=%s", gap)
            } else {
                val nowJump = notice.replace("已快退", "").replace("秒", "").replace("已快进", "")
                if (StringUtil.isNotEmpty(nowJump)) {
                    try {
                        var jump = nowJump.toInt()
                        if (jump != 0) {
                            if (notice.contains("已快退")) {
                                jump = -jump
                            }
                            finalJump = jump + gap
                        }
                    } catch (e: NumberFormatException) {
                        e.printStackTrace()
                    }
                }
            }
        }
        if (finalJump != 0) {
            playerView!!.showNotice((if (finalJump > 0) "已快进" else "已快退") + Math.abs(finalJump) + "秒")
        }
    }

    private fun playNow() {
        if (StringUtil.isEmpty(url) || player == null) {
            return
        }
        player!!.reset()
        try {
            player!!.setPlayUri(HttpParser.getRealUrlFilterHeaders(url), HttpParser.getHeaders(url))
            player!!.setPosition(position)
            player!!.startPlayer<ExoUserPlayer>()
            addFormatListener()
        } catch (e: Exception) {
        }
    }

    private fun reStartPlayer(reGetPos: Boolean) {
        if (StringUtil.isEmpty(url)) {
            return
        }
        if (reGetPos) {
            position = HeavyTaskUtil.getPlayerPos(context, getMemoryId()).toLong()
        }
        playNow()
    }

    private fun getMemoryId(): String {
        return url.split(";")[0]
    }

    /**
     * 启动切换播放地址，超时找不到则销毁
     */
    private fun autoChangeXiuTanVideo(count: Int, ticket: Long) {
        if (vaildTicket != ticket) {
            //ticket已经失效，说明有别的地方发起了新的任务
            return
        }
        if (count <= 0) {
            destroy()
            ToastMgr.shortBottomCenter(getContext(), "找不到可播放的视频地址")
            return
        }
        val ok = autoChangeXiuTanVideo()
        if (!ok) {
            playerView?.postDelayed({
                autoChangeXiuTanVideo(count - 1, ticket)
            }, 2000)
        }
    }

    /**
     * 切换播放地址
     */
    private fun autoChangeXiuTanVideo(): Boolean {
        if (player == null || playerView == null) {
            return false
        }
        val dom = StringUtil.getDom(this.webUrl) ?: ""
        videoDetector.putIntoXiuTanLiked(context, dom, "www.fy-sys.cn")
        val results = videoDetector.getDetectedMediaResults(MediaType.VIDEO)
        if (CollectionUtil.isEmpty(results)) {
            return false
        }
        val nowVideoUrl = url.split(";")[0]
        if (results.size == 1 && StringUtils.equals(results[0]!!.url, nowVideoUrl)) {
            return false
        }
        var idx = -1
        for ((index, mediaResult) in results.withIndex()) {
            if (nowVideoUrl == mediaResult.url) {
                idx = index
                break
            }
        }
        //优先找后面的，像哔哩那种动态hash的，找前面的可能是之前播的地址
        for (i in idx + 1 until results.size) {
            if (playMedia(results[i], nowVideoUrl, dom)) {
                return true
            }
        }

        for (i in 0 until idx) {
            if (playMedia(results[i], nowVideoUrl, dom)) {
                return true
            }
        }
        return false
    }

    private fun playMedia(result: DetectedMediaResult, nowVideoUrl: String, dom: String): Boolean {
        if (!result.isClicked && !StringUtils.equals(result.url, nowVideoUrl)) {
            result.isClicked = true
            val uu = PlayerChooser.decorateHeader(
                context,
                webUrl,
                result.url
            )
            updateUrl(uu)
            HeavyTaskUtil.updateHistoryVideoUrl(WebUtil.getShowingUrl(), url)
            reStartPlayer(true)
            DetectorManager.getInstance()
                .putIntoXiuTanLiked(getContext(), dom, StringUtil.getDom(url))
            return true
        }
        return false
    }

    private fun updateUrl(newUrl: String) {
        url = UrlDetector.clearTag(newUrl)
        video_address_view?.text = url.split(";")[0]
    }

    private fun getContext(): Context {
        return context
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.custom_mode, R.id.custom_toolbar_desc -> {
                if (layoutNow != VideoPlayerView.Layout.VERTICAL) {
                    reverseListCardVisibility()
                } else {
                    playerView?.exoFullscreen?.performClick()
                }
            }
            R.id.custom_dlan -> {
                val dlanTypes = arrayOf("传统投屏", "网页投屏")
                val popupView = XPopup.Builder(getContext())
                    .hasNavigationBar(false)
                    .isRequestFocus(false)
                    .atView(v)
                    .asAttachList(
                        dlanTypes,
                        null
                    ) { position: Int, text: String? ->
                        when (text) {
                            "传统投屏" -> startDlan()
                            "网页投屏" -> startWebDlan(true, false)
                            else -> {
                            }
                        }
                    }
                popupView.show()
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onDlanDeviceUpdated(event: OnDeviceUpdateEvent?) {
        if (dlanListPop != null) {
            dlanListPop!!.notifyDataChanged()
        }
    }

    /**
     * 传统DLAN投屏
     */
    private fun startDlan() {
        player!!.setStartOrPause(false)
        val playUrl = LocalServerParser.getRealUrlForRemotedPlay(
            Application.getContext(),
            PlayerChooser.getThirdPlaySource(url)
        )
        if (CollectionUtil.isEmpty(DlanListPopUtil.instance().devices)) {
            DlanListPopUtil.instance().reInit()
        } else {
            if (DlanListPopUtil.instance().usedDevice != null &&
                DlanListPopUtil.instance().devices.contains(DlanListPopUtil.instance().usedDevice)
            ) {
                ClingManager.getInstance().selectedDevice = DlanListPopUtil.instance().usedDevice
                val intent1 = Intent(
                    getContext(),
                    MediaPlayActivity::class.java
                )
                intent1.putExtra(DLandataInter.Key.PLAY_TITLE, title)
                intent1.putExtra(DLandataInter.Key.PLAYURL, playUrl)
                intent1.putExtra(
                    DLandataInter.Key.HEADER,
                    DlanListPop.genHeaderString(HttpParser.getHeaders(url))
                )
                context.startActivity(intent1)
                ToastMgr.shortBottomCenter(getContext(), "已使用常用设备投屏，长按投屏按钮切换设备")
                return
            }
        }
        if (dlanListPop == null) {
            dlanListPop = DlanListPop(context, DlanListPopUtil.instance().devices)
        }
        dlanListPop?.updateTitleAndUrl(playUrl, title, HttpParser.getHeaders(url))
        XPopup.Builder(context)
            .asCustom(dlanListPop)
            .show()
    }

    /**
     * 网页投屏
     *
     * @param showToast 复制链接时是否显示toast
     */
    private fun startWebDlan(showToast: Boolean, forRedirect: Boolean) {
        webDlanPlaying = true
        player!!.setStartOrPause(false)
        Snackbar.make(playerView!!, "努力为您加载中，请稍候", Snackbar.LENGTH_SHORT).show()
        try {
            if (RemotePlayConfig.playerPath == RemotePlayConfig.WEBS) {
                RemotePlayConfig.playerPath = RemotePlayConfig.D_PLAYER_PATH
                RemoteServerManager.instance().destroyServer()
            }
            RemoteServerManager.instance().startServer(getContext(), object : ServerListener {
                override fun onStarted() {
                    val playUrl0 = LocalServerParser.getRealUrlForRemotedPlay(
                        Application.getContext(),
                        PlayerChooser.getThirdPlaySource(url)
                    )
                    val jumpStartDuration =
                        PreferenceMgr.getInt(getContext(), "jumpStartDuration", 0)
                    val jumpEndDuration = PreferenceMgr.getInt(getContext(), "jumpEndDuration", 0)
                    val urlDTO = DlanUrlDTO(
                        playUrl0, HttpParser.getHeaders(url),
                        jumpStartDuration, jumpEndDuration
                    )
                    urlDTO.title = title
                    RemoteServerManager.instance().urlDTO = urlDTO
                    var playUrl = RemoteServerManager.instance().getServerUrl(getContext())
                    if (TextUtils.isEmpty(playUrl)) {
                        Snackbar.make(playerView!!, "出现错误：链接为空！", Snackbar.LENGTH_LONG).show()
                        return
                    }
                    if (forRedirect) {
                        playUrl = "$playUrl/redirectPlayUrl"
                    }
                    val dlanCopyUrl = PreferenceMgr.getBoolean(
                        getContext(),
                        PreferenceConstant.FILE_SETTING_CONFIG,
                        "dlanCopyUrl",
                        true
                    )
                    if (dlanCopyUrl) {
                        ClipboardUtil.copyToClipboard(getContext(), playUrl, showToast)
                    }
                    if (forRedirect) {
                        val text =
                            if (dlanCopyUrl) "已复制链接，请在电脑上用PotPlayer等第三方播放器打开：$playUrl" else "链接: $playUrl，请在电脑上用PotPlayer等第三方播放器打开"
                        Snackbar.make(playerView!!, text, Snackbar.LENGTH_LONG).show()
                    } else {
                        val text =
                            if (dlanCopyUrl) "已复制链接，请在同一WiFi下的电脑或者电视上打开：$playUrl" else "链接: $playUrl，请在同一WiFi下的电脑或者电视上打开"
                        Snackbar.make(playerView!!, text, Snackbar.LENGTH_LONG).show()
                    }
                }

                override fun onStopped() {}
                override fun onException(e: java.lang.Exception) {
                    Snackbar.make(playerView!!, "出现错误：" + e.message, Snackbar.LENGTH_LONG).show()
                }
            })
        } catch (e: java.lang.Exception) {
            Snackbar.make(playerView!!, "出现错误：" + e.message, Snackbar.LENGTH_LONG).show()
        }
    }

    private fun reverseListCardVisibility() {
        if (listCard!!.visibility == View.INVISIBLE || listCard!!.visibility == View.GONE) {
            refreshListScrollView(true, false, listScrollView!!)
            setListCardTextColor()
            listCard!!.visibility = View.VISIBLE
            playerView!!.getPlaybackControlView().setShowTimeoutMs(0)
            listCard!!.animate().alpha(1f).start()
            listCard!!.setOnClickListener { v: View? -> reverseListCardVisibility() }
        } else {
            refreshListScrollView(false, false, listScrollView!!)
            playerView!!.getPlaybackControlView().setShowTimeoutMs(5000)
        }
    }

    private fun refreshListScrollView(open: Boolean, halfWidth: Boolean, listScrollView: View) {
        var start = 0
        var end = 0
        var width = 0
        listCard!!.post {
            if (open) {
                listScrollView.visibility = View.VISIBLE
            }
            val layoutParams1 = listScrollView.layoutParams as RelativeLayout.LayoutParams
            if (playerView!!.isNowVerticalFullScreen) {
                width = playerView!!.measuredWidth
                if (halfWidth) {
                    start = if (open) width else width / 2
                    end = if (open) width / 2 else width
                } else {
                    start = if (open) width else 0
                    end = if (open) 0 else width
                }
            } else {
                width = playerView!!.measuredWidth
                if (halfWidth) {
                    start = if (open) width else width / 4 * 3
                    end = if (open) width / 4 * 3 else width
                } else {
                    start = if (open) width else width / 2
                    end = if (open) width / 2 else width
                }
            }
            val anim = ValueAnimator.ofInt(start, end)
            anim.duration = 300
            val finalEnd = end
            anim.addUpdateListener { animation: ValueAnimator ->
                layoutParams1.leftMargin = (animation.animatedValue as Int)
                listScrollView.layoutParams = layoutParams1
                if (!open && layoutParams1.leftMargin == finalEnd) {
                    playerView!!.getPlaybackControlView().hide()
                    listCard!!.visibility = View.INVISIBLE
                }
            }
            anim.start()
        }
    }

    /**
     * 倍速、显示比例
     */
    private fun setListCardTextColor() {
        val white: Int = context.getResources().getColor(R.color.white)
        val green: Int = context.getResources().getColor(R.color.greenAction)
        val mode_fit = listCard!!.findViewById<TextView>(R.id.mode_fit)
        mode_fit.setTextColor(white)
        val mode_fill = listCard!!.findViewById<TextView>(R.id.mode_fill)
        mode_fill.setTextColor(white)
        val mode_fixed_width = listCard!!.findViewById<TextView>(R.id.mode_fixed_width)
        mode_fixed_width.setTextColor(white)
        val mode_fixed_height = listCard!!.findViewById<TextView>(R.id.mode_fixed_height)
        mode_fixed_height.setTextColor(white)
        val speed_1 = listCard!!.findViewById<TextView>(R.id.speed_1)
        speed_1.setTextColor(white)
        val speed_1_2 = listCard!!.findViewById<TextView>(R.id.speed_1_2)
        speed_1_2.setTextColor(white)
        val speed_1_5 = listCard!!.findViewById<TextView>(R.id.speed_1_5)
        speed_1_5.setTextColor(white)
        val speed_2 = listCard!!.findViewById<TextView>(R.id.speed_2)
        speed_2.setTextColor(white)
        val speed_p8 = listCard!!.findViewById<TextView>(R.id.speed_p8)
        speed_p8.setTextColor(white)
        val speed_p5 = listCard!!.findViewById<TextView>(R.id.speed_p5)
        speed_p5.setTextColor(white)
        val speed_3 = listCard!!.findViewById<TextView>(R.id.speed_3)
        speed_3.setTextColor(white)
        val speed_4 = listCard!!.findViewById<TextView>(R.id.speed_4)
        speed_4.setTextColor(white)
        val speed_5 = listCard!!.findViewById<TextView>(R.id.speed_5)
        speed_5.setTextColor(white)
        val speed_6 = listCard!!.findViewById<TextView>(R.id.speed_6)
        speed_6.setTextColor(white)
        when (playerView?.playerView?.resizeMode ?: AspectRatioFrameLayout.RESIZE_MODE_FIT) {
            AspectRatioFrameLayout.RESIZE_MODE_FIT -> mode_fit.setTextColor(green)
            AspectRatioFrameLayout.RESIZE_MODE_FILL -> mode_fill.setTextColor(green)
            AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH -> mode_fixed_width.setTextColor(green)
            AspectRatioFrameLayout.RESIZE_MODE_FIXED_HEIGHT -> mode_fixed_height.setTextColor(green)
        }
        when ((VideoPlayerManager.PLAY_SPEED * 10).toInt()) {
            10 -> speed_1.setTextColor(green)
            12 -> speed_1_2.setTextColor(green)
            15 -> speed_1_5.setTextColor(green)
            20 -> speed_2.setTextColor(green)
            8 -> speed_p8.setTextColor(green)
            5 -> speed_p5.setTextColor(green)
            30 -> speed_3.setTextColor(green)
            40 -> speed_4.setTextColor(green)
            50 -> speed_5.setTextColor(green)
            60 -> speed_6.setTextColor(green)
        }
    }

    /**
     * 倍速、显示比例
     */
    private fun initActionAdapter() {
        playerView?.findViewById<View>(R.id.custom_dlan)?.setOnClickListener(this)
        descView?.setOnClickListener(this)
        playerView?.findViewById<View>(R.id.custom_mode)?.setOnClickListener(this)
        listCard = playerView!!.findViewById<View>(R.id.custom_list_bg)
        val listener = View.OnClickListener { v: View? ->
            dealActionViewClick(v!!)
            reverseListCardVisibility()
        }
        listCard?.let {
            it.setOnClickListener(listener)
            it.findViewById<View>(R.id.mode_fit).setOnClickListener(listener)
            it.findViewById<View>(R.id.mode_fill).setOnClickListener(listener)
            it.findViewById<View>(R.id.mode_fixed_width).setOnClickListener(listener)
            it.findViewById<View>(R.id.mode_fixed_height).setOnClickListener(listener)
            it.findViewById<View>(R.id.jump_10).setOnClickListener(listener)
            it.findViewById<View>(R.id.jump_30).setOnClickListener(listener)
            it.findViewById<View>(R.id.jump_60).setOnClickListener(listener)
            it.findViewById<View>(R.id.jump_120).setOnClickListener(listener)
            it.findViewById<View>(R.id.jump_10_l).setOnClickListener(listener)
            it.findViewById<View>(R.id.jump_30_l).setOnClickListener(listener)
            it.findViewById<View>(R.id.jump_60_l).setOnClickListener(listener)
            it.findViewById<View>(R.id.speed_1).setOnClickListener(listener)
            it.findViewById<View>(R.id.speed_1_2).setOnClickListener(listener)
            it.findViewById<View>(R.id.speed_1_5).setOnClickListener(listener)
            it.findViewById<View>(R.id.speed_2).setOnClickListener(listener)
            it.findViewById<View>(R.id.speed_p8).setOnClickListener(listener)
            it.findViewById<View>(R.id.speed_p5).setOnClickListener(listener)
            it.findViewById<View>(R.id.speed_3).setOnClickListener(listener)
            it.findViewById<View>(R.id.speed_4).setOnClickListener(listener)
            it.findViewById<View>(R.id.speed_5).setOnClickListener(listener)
            it.findViewById<View>(R.id.speed_6).setOnClickListener(listener)
            video_str_view = it.findViewById(R.id.video_str_view)
            audio_str_view = it.findViewById(R.id.audio_str_view)
            video_address_view = it.findViewById(R.id.video_address_view)
            video_address_view?.setOnClickListener {
                ClipboardUtil.copyToClipboardForce(getContext(), url.split(";")[0])
            }
            listScrollView = holderView!!.findViewById(R.id.custom_list_scroll_view)
            val dp44 = DisplayUtil.dpToPx(getContext(), 44)
            val dp10 = DisplayUtil.dpToPx(getContext(), 10)
            listScrollView!!.setPadding(dp10, dp44, dp10, dp44)
        }
    }


    /**
     * 倍速、显示比例
     */
    private fun dealActionViewClick(v: View) {
        when (v.id) {
            R.id.mode_fit -> {
                playerView!!.getPlayerView()
                    .setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIT)
                descView!!.text = "速度×" + VideoPlayerManager.PLAY_SPEED + "/自适应"
                PreferenceMgr.put(getContext(), "ijkplayer", "resizeMode", 0)
            }
            R.id.mode_fill -> {
                playerView!!.getPlayerView()
                    .setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FILL)
                descView!!.text = "速度×" + VideoPlayerManager.PLAY_SPEED + "/充满"
                PreferenceMgr.put(getContext(), "ijkplayer", "resizeMode", 3)
            }
            R.id.mode_fixed_width -> {
                playerView!!.getPlayerView()
                    .setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH)
                descView!!.text = "速度×" + VideoPlayerManager.PLAY_SPEED + "/宽度"
                PreferenceMgr.put(getContext(), "ijkplayer", "resizeMode", 1)
            }
            R.id.mode_fixed_height -> {
                playerView!!.getPlayerView()
                    .setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIXED_HEIGHT)
                descView!!.text = "速度×" + VideoPlayerManager.PLAY_SPEED + "/高度"
                PreferenceMgr.put(getContext(), "ijkplayer", "resizeMode", 2)
            }
            R.id.jump_10, R.id.jump_30, R.id.jump_60, R.id.jump_120, R.id.jump_10_l, R.id.jump_30_l, R.id.jump_60_l -> {
                val forward: Long = (v.tag as String).toLong()
                fastPositionJump(forward)
            }
            R.id.speed_1, R.id.speed_1_2, R.id.speed_1_5, R.id.speed_2, R.id.speed_p8, R.id.speed_p5, R.id.speed_3, R.id.speed_4, R.id.speed_5, R.id.speed_6 -> {
                val speed: Float = (v.tag as String).toFloat()
                playFromSpeed(speed)
            }
        }
    }

    /**
     * 倍速，全局保存，重启软件失效
     */
    private fun playFromSpeed(speed: Float) {
        VideoPlayerManager.PLAY_SPEED = speed
        player!!.setPlaybackParameters(speed, 1f)
        descView!!.text =
            "速度×" + VideoPlayerManager.PLAY_SPEED + "/" + descView!!.text.toString().split("/")
                .toTypedArray()[1]
    }

    private fun getHeaders(
        map: MutableMap<String, MutableMap<String, String?>?>?,
        videoUrl: String
    ): MutableMap<String, String?>? {
        if (map == null) {
            return null
        }
        return map[videoUrl]
    }

    private fun pauseWebDelay(count: Int = 5) {
        if (count < 0) {
            return
        }
        if (playerView != null && player != null && url.isNotEmpty()) {
            pauseWebView(true, false)
            playerView?.postDelayed({
                pauseWebDelay(count - 1)
            }, 1000)
        }
    }

    interface WebHolder {
        fun getRequestMap(): MutableMap<String, MutableMap<String, String?>?>?
    }

}
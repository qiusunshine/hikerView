package com.example.hikerview.ui.music

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.util.Log
import android.view.KeyEvent
import com.example.hikerview.ui.video.MusicForegroundService
import com.example.hikerview.ui.video.event.MusicAction
import org.greenrobot.eventbus.EventBus
import timber.log.Timber

/**
 * 作者：By 15968
 * 日期：On 2022/1/19
 * 时间：At 23:32
 */
class HeadsetButtonReceiver : BroadcastReceiver() {
    private var lastClickTime: Long = 0

    override fun onReceive(context: Context?, intent: Intent) {
        if (Intent.ACTION_MEDIA_BUTTON == intent.action) {
            val keyEvent: KeyEvent? = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT)
            val gap = System.currentTimeMillis() - lastClickTime
            Timber.i("HeadsetButtonReceiver：" + "onReceive: gap: " + gap + ", if:" + keyEvent?.keyCode + ", action: " + keyEvent?.action)
            if (gap < 500) {
                //不处理双击的情况
                return
            }
            lastClickTime = System.currentTimeMillis()
            val isPlayOrPause = keyEvent?.keyCode === KeyEvent.KEYCODE_HEADSETHOOK
                    || keyEvent?.keyCode === KeyEvent.KEYCODE_MEDIA_PLAY
                    || keyEvent?.keyCode === KeyEvent.KEYCODE_MEDIA_PAUSE
                    || keyEvent?.keyCode === KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
            if (isPlayOrPause && keyEvent?.action === KeyEvent.ACTION_UP) {
                Log.i(
                    "headSet",
                    "HeadsetButtonReceiver：" + "onReceive:" + "if:" + "if" + " HEADSETHOOK"
                )
                EventBus.getDefault().post(MusicAction(MusicForegroundService.PAUSE))
            } else if (keyEvent?.keyCode === KeyEvent.KEYCODE_MEDIA_NEXT) {
                Log.i(
                    "headSet",
                    "HeadsetButtonReceiver：" + "onReceive:" + "if:" + "if" + " KEYCODE_HEADSETHOOK"
                )
                EventBus.getDefault().post(MusicAction(MusicForegroundService.NEXT))
            } else if (keyEvent?.keyCode === KeyEvent.KEYCODE_MEDIA_PREVIOUS) {
                Log.i(
                    "headSet",
                    "HeadsetButtonReceiver：" + "onReceive:" + "if:" + "if" + " KEYCODE_MEDIA_PREVIOUS"
                )
                EventBus.getDefault().post(MusicAction(MusicForegroundService.PREV))
            }
        }
    }

    companion object {

        fun registerHeadsetReceiver(context: Context) {
            val audioManager: AudioManager =
                context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val name = ComponentName(context.packageName, HeadsetButtonReceiver::class.java.name)
            audioManager.registerMediaButtonEventReceiver(name)
        }

        fun unregisterHeadsetReceiver(context: Context) {
            val audioManager: AudioManager =
                context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val name = ComponentName(context.packageName, HeadsetButtonReceiver::class.java.name)
            audioManager.unregisterMediaButtonEventReceiver(name)
        }
    }
}
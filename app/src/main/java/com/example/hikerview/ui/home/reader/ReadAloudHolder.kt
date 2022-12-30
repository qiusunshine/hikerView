package com.example.hikerview.ui.home.reader

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.example.hikerview.ui.Application
import com.example.hikerview.utils.PreferenceMgr
import com.example.hikerview.utils.ToastMgr

/**
 * 作者：By 15968
 * 日期：On 2022/12/20
 * 时间：At 20:52
 */
class ReadAloudHolder {
    private var textToSpeech: TextToSpeech? = null
    private var engineStatus: Int = -3
    private var engineSelected: String = ""
    private var doneCallback: () -> Unit = {}
    private var reading: String = ""

    private fun initSpeech(ctx: Context, text: String? = null, engine0: String? = null) {
        val engine = if (engine0.isNullOrEmpty()) {
            null
        } else engine0
        textToSpeech = TextToSpeech(ctx, { status ->
            engineStatus = status
            if (engineStatus == TextToSpeech.SUCCESS) {
                engineSelected = engine ?: textToSpeech!!.defaultEngine
                initListener()
                if (!text.isNullOrEmpty()) {
                    val ts = System.currentTimeMillis().toString()
                    textToSpeech?.speak(
                        text,
                        TextToSpeech.QUEUE_FLUSH,
                        null,
                        ts
                    )
                }
            } else {
                if (engine0 != null) {
                    initSpeech(ctx, text, null)
                    ToastMgr.shortBottomCenter(
                        Application.getContext(),
                        "初始化" + engine0 + "引擎失败，尝试换默认引擎"
                    )
                } else {
                    ToastMgr.shortBottomCenter(Application.getContext(), "初始化语音引擎失败：$status")
                }
            }
        }, engine)
    }

    private fun initListener() {
        textToSpeech?.setOnUtteranceProgressListener(object :
            UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {

            }

            override fun onDone(utteranceId: String?) {
                println("textToSpeech onDone: $utteranceId")
                reading = ""
                doneCallback()
            }

            override fun onError(utteranceId: String?) {
                ToastMgr.shortBottomCenter(Application.getContext(), "语音朗读出错：$utteranceId")
            }
        })
    }

    fun getEngines(): List<TextToSpeech.EngineInfo> {
        return textToSpeech?.engines ?: emptyList()
    }

    fun getSelectedEngine(): String {
        val list = getEngines()
        for (info in list) {
            if (engineSelected == info.name) {
                return info.label
            }
        }
        return engineSelected
    }

    fun read(ctx: Context, text: String, callback: () -> Unit) {
        doneCallback = callback
        reading = text
        if (textToSpeech == null) {
            val engine = PreferenceMgr.getString(ctx, "readAloud", null)
            initSpeech(ctx, text, engine)
        } else {
            val ts = System.currentTimeMillis().toString()
            textToSpeech?.speak(
                text,
                TextToSpeech.QUEUE_FLUSH,
                null,
                ts
            )
        }
    }

    fun shutdown() {
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        textToSpeech = null
        reading = ""
    }
}
package com.example.hikerview.ui.thunder

import com.xunlei.downloadlib.parameter.TorrentFileInfo
import java.util.*

/**
 * 作者：By 15968
 * 日期：On 2022/6/13
 * 时间：At 9:57
 */
interface MagnetConsumer {
    fun consume(url: String, name: String, arrayList: ArrayList<TorrentFileInfo>)
}
package com.example.hikerview.ui.download

/**
 * 作者：By 15968
 * 日期：On 2022/6/6
 * 时间：At 21:12
 */
data class DownloadInfo(
    var lastClearSpeedTime: Long = 0,
    var lastDownloadedSize: Long = 0,
    var currentSpeed: Long = 0
)
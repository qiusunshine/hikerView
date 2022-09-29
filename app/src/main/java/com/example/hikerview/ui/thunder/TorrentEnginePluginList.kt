package com.example.hikerview.ui.thunder

/**
 * 作者：By 15968
 * 日期：On 2022/9/20
 * 时间：At 20:54
 */
enum class TorrentEnginePluginList(
    val bean: TorrentEngineBean
) {
    TSEngine(
        TorrentEngineBean(
            "libtorrent4j",
            "com.example.hikerview.ui.thunder.TSEngine",
            "org.libtorrent4j.swig.libtorrent_jni",
            "torrent4j",
            "http://gh.haikuoshijie.cn/https://github.com/qiusunshine/hiker-rules/blob/master/plugins/ts.so",
            "http://gh.haikuoshijie.cn/https://github.com/qiusunshine/hiker-rules/blob/master/plugins/ts32.so",
            ""
        )
    )
}
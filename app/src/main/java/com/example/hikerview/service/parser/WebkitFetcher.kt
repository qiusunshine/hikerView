package com.example.hikerview.service.parser

import android.webkit.WebView
import com.alibaba.fastjson.JSONArray
import com.annimon.stream.function.Consumer
import com.example.hikerview.ui.Application
import com.example.hikerview.ui.browser.util.UUIDUtil
import com.example.hikerview.ui.home.model.article.extra.X5Extra
import com.example.hikerview.ui.home.webview.ArticleWebkitHolder
import com.example.hikerview.utils.StringUtil
import com.example.hikerview.utils.ThreadTool
import org.apache.commons.lang3.StringUtils
import java.util.*

/**
 * 作者：By 15968
 * 日期：On 2021/9/20
 * 时间：At 14:42
 */
class WebkitFetcher {
    private var webViewHolder: ArticleWebkitHolder? = null
    fun fetch(url0: String?, op: Map<*, *>?, codeListener: Consumer<String?>) {
        ThreadTool.runOnUI {
            val headers: Map<String, String?>? =
                if (op == null) null else op["headers"] as Map<String, String?>?
            val url = StringUtils.replaceOnceIgnoreCase(url0, "webview://", "")

            val webView = WebView(Application.application.homeActivity)
            webViewHolder = ArticleWebkitHolder(webView)
            webViewHolder!!.initWebView(Application.application.homeActivity)
            var ua: String? = null
            if (headers != null) {
                ua = headers["content-type"]
                if (StringUtil.isEmpty(ua)) {
                    ua = headers["Content-Type"]
                }
                if (StringUtil.isEmpty(ua)) {
                    ua = headers["Content-type"]
                }
                if (StringUtil.isNotEmpty(ua)) {
                    webViewHolder!!.webView.settings.userAgentString = ua
                }
            }
            val x5Extra = X5Extra()
            if (ua != null) {
                x5Extra.ua = ua
            }
            val blockRules =
                if (op != null && op.containsKey("blockRules")) op["blockRules"] as JSONArray? else null
            val js =
                if (op != null && op.containsKey("js")) op["js"] as String? else null
            if (!js.isNullOrEmpty()) {
                x5Extra.js = js
            }
            if (blockRules != null && blockRules.size > 0) {
                val rules: MutableList<String> = ArrayList()
                for (i in blockRules.indices) {
                    rules.add(blockRules.getString(i))
                }
                x5Extra.blockRules = rules
            }
            webViewHolder!!.x5Extra = x5Extra
            webViewHolder!!.finishPageConsumer = Consumer {
//                JSEngine.getInstance().log("webview pageFinish: $url", null)
                val sign = UUIDUtil.genUUID()
                webViewHolder!!.webView.evaluateJavascript("(function(){fy_bridge_app.putVar('$sign', document.getElementsByTagName('html')[0].outerHTML);return 'ok'})()") { value: String? ->
                    val code = JSEngine.getInstance().getVar(sign, "")
                    JSEngine.getInstance().clearVar(sign)
                    codeListener.accept(code)
//                    JSEngine.getInstance().log("webview pageFinish2: $url", null)
                    destroy()
                }
            }
//            JSEngine.getInstance().log("webview start: $url", null)
            if (headers != null) {
                webViewHolder!!.webView.loadUrl(url, headers)
            } else {
                webViewHolder!!.webView.loadUrl(url)
            }
        }
    }

    fun destroy() {
        ThreadTool.runOnUI {
            if (webViewHolder != null && webViewHolder!!.webView != null) {
                webViewHolder!!.webView.stopLoading()
                webViewHolder!!.webView.onPause()
                webViewHolder!!.webView.destroy()
                webViewHolder = null
            }
        }
    }

    companion object {
        fun canParse(url: String): Boolean {
            return StringUtil.isNotEmpty(url) && url.startsWith("webview://")
        }
    }
}
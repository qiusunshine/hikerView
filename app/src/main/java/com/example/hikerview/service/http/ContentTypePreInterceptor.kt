package com.example.hikerview.service.http

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

/**
 * Bridges from application code to network code. First it builds a network request from a user
 * request. Then it proceeds to call the network. Finally it builds a user response from the network
 * response.
 */
object ContentTypePreInterceptor : Interceptor {

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val userRequest = chain.request()
        val requestBuilder = userRequest.newBuilder()

        val body = userRequest.body
        if (body != null) {
            val contentType = body.contentType()
            //FormBody写死了contentType方法，BridgeInterceptor会覆盖Content-Type，这里反覆盖一下
            if (contentType != null && "application/x-www-form-urlencoded" == contentType.toString() && userRequest.header("Content-Type") != null) {
                requestBuilder.header("Content-Type-Temp", userRequest.header("Content-Type")!!)
            }
        }
        try {
            if(userRequest.url.host.contains("gitee.com") && userRequest.header("Referer") == null){
                requestBuilder.header("Referer", "https://gitee.com")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return chain.proceed(requestBuilder.build())
    }
}
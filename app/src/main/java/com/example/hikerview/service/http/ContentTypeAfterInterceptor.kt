package com.example.hikerview.service.http

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

/**
 * Bridges from application code to network code. First it builds a network request from a user
 * request. Then it proceeds to call the network. Finally it builds a user response from the network
 * response.
 */
object ContentTypeAfterInterceptor : Interceptor {

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val userRequest = chain.request()
        val requestBuilder = userRequest.newBuilder()

        val body = userRequest.body
        if (body != null) {
            if (userRequest.header("Content-Type-Temp") != null) {
                requestBuilder.header("Content-Type", userRequest.header("Content-Type-Temp")!!)
                requestBuilder.removeHeader("Content-Type-Temp")
            }
        }

        return chain.proceed(requestBuilder.build())
    }
}
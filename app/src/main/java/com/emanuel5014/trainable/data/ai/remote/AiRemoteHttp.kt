package com.emanuel5014.trainable.data.ai.remote

import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

object AiRemoteHttp {

    /**
     * The shared client set up for a remote AI call: a slow upload and a model that is still loading on the
     * PC get room, there is no overall deadline (the user cancels instead), and a redirect is never followed
     * so a key can't be carried to another host or down to plain http. Nothing is logged.
     */
    fun client(base: OkHttpClient): OkHttpClient = base.newBuilder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .callTimeout(0, TimeUnit.SECONDS)
        .followRedirects(false)
        .followSslRedirects(false)
        .build()
}

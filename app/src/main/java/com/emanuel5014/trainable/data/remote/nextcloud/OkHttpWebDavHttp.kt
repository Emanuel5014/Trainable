package com.emanuel5014.trainable.data.remote.nextcloud

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Android's Nextcloud transport: the OkHttp client the old NextcloudWebDavClient built (https only through
 * [CleartextGuard], no https→http redirect, 30 s connect, 120 s read/write), building each request the same way.
 */
@Singleton
class OkHttpWebDavHttp @Inject constructor(baseOkHttpClient: OkHttpClient) : WebDavHttp {

    private val client: OkHttpClient = CleartextGuard.applyTo(
        baseOkHttpClient.newBuilder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
    ).build()

    override suspend fun send(request: WebDavRequest): WebDavResponse = withContext(Dispatchers.IO) {
        val builder = Request.Builder().url(request.url)
        request.headers.forEach { (name, value) -> builder.header(name, value) }
        when (request.method) {
            "GET" -> builder.get()
            "DELETE" -> builder.delete()
            "PUT" -> builder.put(body(request.body))
            else -> builder.method(request.method, body(request.body))
        }
        client.newCall(builder.build()).execute().use { response ->
            val target = request.downloadTo
            if (target == null) return@use WebDavResponse(response.code, response.message, response.body.string())
            if (response.isSuccessful) FileOutputStream(File(target)).use { output -> response.body.byteStream().copyTo(output) }
            WebDavResponse(response.code, response.message, null)
        }
    }

    private fun body(body: WebDavBody): RequestBody = when (body) {
        is WebDavBody.Text -> body.text.toRequestBody(body.contentType.toMediaType())
        is WebDavBody.FilePath -> File(body.path).asRequestBody(body.contentType.toMediaType())
        WebDavBody.Empty -> ByteArray(0).toRequestBody(null)
    }
}

package com.arcusfoundry.labs.pixelpilot.source.youtube

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request as NpRequest
import org.schabi.newpipe.extractor.downloader.Response as NpResponse
import java.util.concurrent.TimeUnit

/**
 * Thin OkHttp-backed Downloader for NewPipeExtractor. Needed because the extractor
 * library depends on the app providing an HTTP transport.
 */
class NewPipeDownloader private constructor(private val client: OkHttpClient) : Downloader() {

    override fun execute(request: NpRequest): NpResponse {
        val reqBuilder = Request.Builder().url(request.url())
        request.headers().forEach { (name, values) ->
            values.forEach { reqBuilder.addHeader(name, it) }
        }
        val body = request.dataToSend()?.toRequestBody()
        reqBuilder.method(request.httpMethod(), body)

        client.newCall(reqBuilder.build()).execute().use { resp ->
            val responseBody = resp.body?.string().orEmpty()
            val headers = resp.headers.toMultimap()
            return NpResponse(
                resp.code,
                resp.message,
                headers,
                responseBody,
                resp.request.url.toString()
            )
        }
    }

    companion object {
        @Volatile private var INSTANCE: NewPipeDownloader? = null

        fun getInstance(): NewPipeDownloader = INSTANCE ?: synchronized(this) {
            INSTANCE ?: run {
                val client = OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .build()
                NewPipeDownloader(client).also { INSTANCE = it }
            }
        }
    }
}

package org.chimeramc.client.core.nethernet

import java.io.IOException
import java.util.concurrent.TimeUnit
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.HttpUrl.Companion.toHttpUrl

/** The outcome of one HTTP call, reduced to what the auth chain needs. */
data class HttpResult(val status: Int, val body: String) {
    val isSuccess: Boolean get() = status in 200..299
}

/** The one seam every auth call goes through. A real OkHttp client, or a scripted test double. */
interface HttpTransport {
    /**
     * POSTs a form body.
     *
     * @param timeoutMs a hard per-call cap. OkHttp enforces it as connect/read/write timeouts, so a
     *   stalled server surfaces as an [IOException] instead of an unbounded wait.
     */
    suspend fun postForm(
        url: String,
        form: Map<String, String>,
        timeoutMs: Long,
        headers: Map<String, String> = emptyMap(),
    ): HttpResult
}

/** The production transport: OkHttp, with the timeout applied per call and no shared client state. */
class OkHttpTransport(private val base: OkHttpClient = OkHttpClient()) : HttpTransport {

    override suspend fun postForm(
        url: String,
        form: Map<String, String>,
        timeoutMs: Long,
        headers: Map<String, String>,
    ): HttpResult {
        val client = base.newBuilder()
            .connectTimeout(timeoutMs, TimeUnit.MILLISECONDS)
            .readTimeout(timeoutMs, TimeUnit.MILLISECONDS)
            .writeTimeout(timeoutMs, TimeUnit.MILLISECONDS)
            .callTimeout(timeoutMs, TimeUnit.MILLISECONDS)
            .build()
        val builder = FormBody.Builder()
        for ((k, v) in form) builder.add(k, v)
        val request = Request.Builder()
            .url(url.toHttpUrl())
            .post(builder.build())
        for ((k, v) in headers) request.header(k, v)
        client.newCall(request.build()).execute().use { response ->
            val text = response.body?.string().orEmpty()
            return HttpResult(response.code, text)
        }
    }
}

/** Thrown when the transport itself fails (no route, TLS, timeout), before any status is read. */
class TransportFailure(message: String, cause: Throwable? = null) : IOException(message, cause)

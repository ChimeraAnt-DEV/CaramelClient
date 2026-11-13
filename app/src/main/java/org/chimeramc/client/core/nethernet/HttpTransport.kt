package org.chimeramc.client.core.nethernet

import java.io.IOException
import java.util.concurrent.TimeUnit
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.HttpUrl.Companion.toHttpUrl

/** The outcome of one HTTP call, reduced to what the auth chain needs. */
data class HttpResult(val status: Int, val body: String) {
    val isSuccess: Boolean get() = status in 200..299
}

/**
 * The one seam every auth call goes through. A real OkHttp client, or a scripted test double.
 *
 * The three shapes are separate methods rather than one method with a mode flag because each is used
 * by a different protocol: the MSA token endpoint is form-encoded, the Xbox endpoints are JSON, and
 * the Minecraft profile is a GET. Keeping them apart means a caller cannot accidentally send a JSON
 * body where a form is required.
 */
interface HttpTransport {

    /**
     * POSTs a form body.
     *
     * @param timeoutMs a hard per-call cap, applied as connect/read/write/call timeouts, so a stalled
     *   server surfaces as an [IOException] instead of an unbounded wait.
     */
    suspend fun postForm(
        url: String,
        form: Map<String, String>,
        timeoutMs: Long,
        headers: Map<String, String> = emptyMap(),
    ): HttpResult

    /** POSTs a JSON body. */
    suspend fun postJson(
        url: String,
        json: String,
        timeoutMs: Long,
        headers: Map<String, String> = emptyMap(),
    ): HttpResult

    /** GETs a URL. */
    suspend fun get(
        url: String,
        timeoutMs: Long,
        headers: Map<String, String> = emptyMap(),
    ): HttpResult
}

/** The production transport: OkHttp, with the timeout applied per call and no shared client state. */
class OkHttpTransport(private val base: OkHttpClient = OkHttpClient()) : HttpTransport {

    private fun clientFor(timeoutMs: Long): OkHttpClient = base.newBuilder()
        .connectTimeout(timeoutMs, TimeUnit.MILLISECONDS)
        .readTimeout(timeoutMs, TimeUnit.MILLISECONDS)
        .writeTimeout(timeoutMs, TimeUnit.MILLISECONDS)
        .callTimeout(timeoutMs, TimeUnit.MILLISECONDS)
        .build()

    private fun execute(request: Request, timeoutMs: Long): HttpResult {
        clientFor(timeoutMs).newCall(request).execute().use { response ->
            return HttpResult(response.code, response.body?.string().orEmpty())
        }
    }

    override suspend fun postForm(
        url: String,
        form: Map<String, String>,
        timeoutMs: Long,
        headers: Map<String, String>,
    ): HttpResult {
        val builder = FormBody.Builder()
        for ((k, v) in form) builder.add(k, v)
        val request = Request.Builder().url(url.toHttpUrl()).post(builder.build())
        for ((k, v) in headers) request.header(k, v)
        return execute(request.build(), timeoutMs)
    }

    override suspend fun postJson(
        url: String,
        json: String,
        timeoutMs: Long,
        headers: Map<String, String>,
    ): HttpResult {
        val body = json.toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder().url(url.toHttpUrl()).post(body)
        for ((k, v) in headers) request.header(k, v)
        return execute(request.build(), timeoutMs)
    }

    override suspend fun get(
        url: String,
        timeoutMs: Long,
        headers: Map<String, String>,
    ): HttpResult {
        val request = Request.Builder().url(url.toHttpUrl()).get()
        for ((k, v) in headers) request.header(k, v)
        return execute(request.build(), timeoutMs)
    }
}

/** Thrown when the transport itself fails (no route, TLS, timeout), before any status is read. */
class TransportFailure(message: String, cause: Throwable? = null) : IOException(message, cause)

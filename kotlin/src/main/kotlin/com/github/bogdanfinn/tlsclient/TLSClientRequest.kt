package com.github.bogdanfinn.tlsclient

import com.squareup.moshi.*
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okio.Buffer
import java.util.*

class TLSClientRequest(var method: String, var url: String) {
    companion object {
        init {
            TLSClientInit.init()
        }
    }

    val headers = mutableMapOf<String, MutableList<String>>()
    var headerOrder = listOf<String>()

    var body: String = ""

    val options = TLSClientRequestOptions()

    fun send(session: String? = null): TLSClientResponse {
        val buffer = Buffer()
        val writer = JsonWriter.of(buffer)

        writer.beginObject()
        if (session != null) writer.name("sessionId").value(session)
        this.toJSON(writer)
        writer.endObject()

        val requestJson = buffer.readByteArray()
        val responseJsonString = TLSClientJNI.request(requestJson)
        val reader = JsonReader.of(Buffer().writeUtf8(responseJsonString))
        val response = TLSClientResponse.fromJSON(reader)

        return response
    }

    fun addHeader(key: String, value: String) {
        this.headers.getOrPut(key, ::mutableListOf).add(value)
    }

    fun clearHeader(key: String) {
        this.headers.remove(key)
    }

    /** it is the caller's responsibility to call beginObject() and endObject() itself */
    fun toJSON(writer: JsonWriter) {
        writer.name("requestMethod").value(this.method)
        writer.name("requestUrl").value(this.url)

        if (this.headers.isNotEmpty()) writer.name("headers").jsonValue(this.headers)
        if (this.headerOrder.isNotEmpty()) writer.name("headerOrder").jsonValue(this.headerOrder)

        if (this.body.isNotEmpty()) writer.name("requestBody").value(this.body)

        this.options.toJSON(writer)
    }
}

@JsonClass(generateAdapter = true)
data class TLSClientRequestOptions(
    @Json(name = "defaultHeaders") var defaultHeaders: Map<String, List<String>>? = null,
    @Json(name = "connectHeaders") var connectHeaders: Map<String, List<String>>? = null,
    @Json(name = "catchPanics") var catchPanics: Boolean? = null,
    @Json(name = "certificatePinningHosts") var certificatePinningHosts: Map<String, List<String>>? = null,
    @Json(name = "customTlsClient") var customTlsClient: CustomTLSClientJSON? = null,
    @Json(name = "transportOptions") var transportOptions: TransportOptionsJSON? = null,
    @Json(name = "followRedirects") var followRedirects: Boolean? = null,
    @Json(name = "forceHttp1") var forceHttp1: Boolean? = null,
    @Json(name = "insecureSkipVerify") var insecureSkipVerify: Boolean? = null,
    @Json(name = "isByteRequest") var isByteRequest: Boolean? = null,
    @Json(name = "isByteResponse") var isByteResponse: Boolean? = null,
    @Json(name = "isRotatingProxy") var isRotatingProxy: Boolean? = null,
    @Json(name = "disableIPV6") var disableIPV6: Boolean? = null,
    @Json(name = "disableIPV4") var disableIPV4: Boolean? = null,
    @Json(name = "localAddress") var localAddress: String? = null,
    @Json(name = "serverNameOverwrite") var serverNameOverwrite: String? = null,
    @Json(name = "proxyUrl") var proxyUrl: String? = null,
    @Json(name = "requestCookies") var requestCookies: List<CookieJSON>? = null,
    @Json(name = "requestHostOverride") var requestHostOverride: String? = null,
    @Json(name = "streamOutputBlockSize") var streamOutputBlockSize: Int? = null,
    @Json(name = "streamOutputEOFSymbol") var streamOutputEOFSymbol: String? = null,
    @Json(name = "streamOutputPath") var streamOutputPath: String? = null,
    @Json(name = "timeoutMilliseconds") var timeoutMilliseconds: Int? = null,
    @Json(name = "timeoutSeconds") var timeoutSeconds: Int? = null,
    @Json(name = "withDebug") var withDebug: Boolean? = null,
    @Json(name = "withDefaultCookieJar") var withDefaultCookieJar: Boolean? = null,
    @Json(name = "withoutCookieJar") var withoutCookieJar: Boolean? = null,
    @Json(name = "withRandomTLSExtensionOrder") var withRandomTLSExtensionOrder: Boolean? = null,
) {
    companion object {
        private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()!!
        private val extraAdapter = moshi.adapter(TLSClientRequestOptions::class.java)
    }

    fun toJSON(writer: JsonWriter) {
        @Suppress("UNCHECKED_CAST")
        val options = extraAdapter.toJsonValue(this) as Map<String, Any?>
        for ((key, value) in options.entries) {
            if (value == null) continue
            writer.name(key).jsonValue(value)
        }
    }
}
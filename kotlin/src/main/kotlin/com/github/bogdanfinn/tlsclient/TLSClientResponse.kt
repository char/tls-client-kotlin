package com.github.bogdanfinn.tlsclient

import com.squareup.moshi.JsonClass
import com.squareup.moshi.JsonReader
import java.io.Closeable


@JsonClass(generateAdapter = true)
data class TLSClientResponse(
    val id: String,
    val body: String,
    val status: Int,
    val cookies: Map<String, String>,
    val headers: Map<String, List<String>>,
    val sessionId: String?,
    val target: String?,
    val usedProtocol: String?,
) {
    companion object {
        fun fromJSON(reader: JsonReader): TLSClientResponse {
            var id = ""
            var body = ""
            var status = 0
            var cookies = mapOf<String, String>()
            var headers = mapOf<String, List<String>>()
            var sessionId: String? = null
            var target: String? = null
            var usedProtocol: String? = null

            reader.beginObject()
            while (reader.hasNext()) {
                val name = reader.nextName()
                when (name) {
                    "id" -> { id = reader.nextString() }
                    "body" -> { body = reader.nextString() }
                    "status" -> { status = reader.nextInt() }
                    "cookies" -> {
                        @Suppress("UNCHECKED_CAST")
                        val value = reader.readJsonValue() as? Map<String, String>
                        if (value != null) cookies = value
                    }
                    "headers" -> {
                        @Suppress("UNCHECKED_CAST")
                        val value = reader.readJsonValue() as? Map<String, List<String>>
                        if (value != null) headers = value
                    }
                    "sessionId" -> { sessionId = reader.nextString() }
                    "target" -> { target = reader.nextString() }
                    "usedProtocol" -> { usedProtocol = reader.nextString() }

                    else -> { reader.skipValue() }
                }
            }
            reader.endObject()
            val response =  TLSClientResponse(id, body, status, cookies, headers, sessionId, target, usedProtocol)
            TLSClientJNI.freeMemory(id)
            return response;
        }
    }
}

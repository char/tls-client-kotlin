package com.github.bogdanfinn.tlsclient

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class TransportOptionsJSON(
    @Json(name = "disableKeepAlives") val disableKeepAlives: Boolean,
    @Json(name = "disableCompression") val disableCompression: Boolean,
    @Json(name = "maxIdleConns") val maxIdleConns: Int,
    @Json(name = "maxIdleConnsPerHost") val maxIdleConnsPerHost: Int,
    @Json(name = "maxConnsPerHost") val maxConnsPerHost: Int,
    @Json(name = "maxResponseHeaderBytes") val maxResponseHeaderBytes: Long,
    @Json(name = "writeBufferSize") val writeBufferSize: Int,
    @Json(name = "readBufferSize") val readBufferSize: Int,
    @Json(name = "idleConnTimeout") val idleConnTimeout: Long? // Representing time.Duration as Long milliseconds
)

@JsonClass(generateAdapter = true)
class CustomTLSClientJSON {
    @Json(name = "certCompressionAlgo") var certCompressionAlgo: String? = null
    @Json(name = "connectionFlow") var connectionFlow: Int? = null
    @Json(name = "h2Settings") var h2Settings: Map<String, Int>? = null
    @Json(name = "h2SettingsOrder") var h2SettingsOrder: List<String>? = null
    @Json(name = "headerPriority") var headerPriority: PriorityParamJSON? = null
    @Json(name = "ja3String") var ja3String: String? = null
    @Json(name = "keyShareCurves") var keyShareCurves: List<String>? = null
    @Json(name = "alpnProtocols") var alpnProtocols: List<String>? = null
    @Json(name = "alpsProtocols") var alpsProtocols: List<String>? = null
    @Json(name = "ECHCandidatePayloads") var echCandidatePayloads: List<Short>? = null
    @Json(name = "ECHCandidateCipherSuites") var echCandidateCipherSuites: List<CandidateCipherSuiteJSON>? = null
    @Json(name = "priorityFrames") var priorityFrames: List<PriorityFramesJSON>? = null
    @Json(name = "pseudoHeaderOrder") var pseudoHeaderOrder: List<String>? = null
    @Json(name = "supportedDelegatedCredentialsAlgorithms") var supportedDelegatedCredentialsAlgorithms: List<String>? = null
    @Json(name = "supportedSignatureAlgorithms") var supportedSignatureAlgorithms: List<String>? = null
    @Json(name = "supportedVersions") var supportedVersions: List<String>? = null
}

@JsonClass(generateAdapter = true)
data class CandidateCipherSuiteJSON(
    @Json(name = "kdfId") val kdfId: String,
    @Json(name = "aeadId") val aeadId: String
)

@JsonClass(generateAdapter = true)
data class PriorityFramesJSON(
    @Json(name = "priorityParam") val priorityParam: PriorityParamJSON,
    @Json(name = "streamID") val streamID: Int
)

@JsonClass(generateAdapter = true)
data class PriorityParamJSON(
    @Json(name = "exclusive") val exclusive: Boolean,
    @Json(name = "streamDep") val streamDep: Int,
    @Json(name = "weight") val weight: Byte
)

@JsonClass(generateAdapter = true)
class CookieJSON {
    @Json(name = "domain") var domain: String? = null
    @Json(name = "expires") var expires: Long? = null
    @Json(name = "maxAge") var maxAge: Int? = null
    @Json(name = "name") var name: String? = null
    @Json(name = "path") var path: String? = null
    @Json(name = "value") var value: String? = null
}

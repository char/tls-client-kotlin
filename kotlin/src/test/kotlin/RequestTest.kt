import com.github.bogdanfinn.tlsclient.TLSClientJNI
import com.github.bogdanfinn.tlsclient.TLSClientRequest

fun main() {
    val request = TLSClientRequest("GET", "https://tls.peet.ws/api/all").apply {
        addHeader("accept", "*/*")
        addHeader("accept-language", "en-US;q=0.9,en;q=0.8")
        addHeader("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")

        headerOrder = listOf(
            "accept",
            "accept-language",
            "user-agent"
        )

        options.insecureSkipVerify = true
    }

    val response1 = request.send()
    println(response1.body)

    if (response1.sessionId != null) {
        TLSClientJNI.destroySession(response1.sessionId)
    }
}

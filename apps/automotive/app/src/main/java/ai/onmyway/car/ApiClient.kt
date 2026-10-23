package ai.onmyway.car

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URI

/** Stateless service client; API credentials never enter this process. */
class ApiClient(private val baseUrl: String) {
    fun call(path: String, body: JSONObject? = null): JSONObject {
        val uri = URI(baseUrl.trimEnd('/') + path)
        require(uri.scheme == "https" || uri.scheme == "http") { "서버 주소는 http 또는 https여야 합니다." }
        val conn = uri.toURL().openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = 10000; conn.readTimeout = 35000
            conn.requestMethod = if (body == null) "GET" else "POST"
            conn.setRequestProperty("Accept", "application/json")
            if (body != null) {
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            }
            if (conn.responseCode !in 200..299) {
                val detail = conn.errorStream?.bufferedReader()?.use { it.readText() }?.take(300).orEmpty()
                val code = runCatching { JSONObject(detail).optJSONObject("detail")?.optString("code") }.getOrNull()
                val message = when (code) {
                    "provider_not_configured" -> "실제 서비스가 아직 설정되지 않았습니다. 서버에서 API 설정을 완료해 주세요."
                    "provider_timeout" -> "실제 데이터 조회 시간이 초과되었습니다. 잠시 후 다시 요청해 주세요."
                    "provider_unreachable", "provider_http_error" -> "실제 데이터 제공자에 연결하지 못했습니다. 서버 연결 상태를 확인해 주세요."
                    "llm_incomplete", "llm_invalid_output" -> "할 일을 정확히 정리하지 못했습니다. 문장을 더 구체적으로 입력해 주세요."
                    "llm_refusal" -> "이 요청을 정리할 수 없습니다. 할 일의 내용을 바꿔 다시 입력해 주세요."
                    else -> if (conn.responseCode == 422) "입력 형식 또는 시간·위치 조건을 확인해 주세요." else "서버가 요청을 처리하지 못했습니다. 다시 시도해 주세요."
                }
                throw IllegalStateException("$message (${conn.responseCode})")
            }
            return JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
        } finally { conn.disconnect() }
    }
}

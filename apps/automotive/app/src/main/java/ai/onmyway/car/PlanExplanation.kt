package ai.onmyway.car

import org.json.JSONObject
import java.time.Duration
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

/** Human-readable evidence from the validated solver response, without invented causal claims. */
object PlanExplanation {
    fun lines(request: JSONObject?, result: JSONObject): List<String> {
        val lines = mutableListOf<String>()
        if (result.optBoolean("is_fixture")) lines += "예시 장소·영업시간·이동시간으로 계산했어요. 실제 경로가 아닙니다."
        when (result.optString("status")) {
            "feasible" -> lines += "입력한 데이터 기준으로 필수 일정과 시간 조건을 맞췄어요."
            "infeasible" -> lines += "현재 조건으로는 필수 일정과 귀가 마감을 함께 지킬 동선을 찾지 못했어요."
            else -> lines += "영업·서비스 또는 이동 정보 확인 전에는 가능 여부를 확정할 수 없어요."
        }
        val home = date(result.optString("home_arrival_time"))
        val deadline = date(request?.optString("deadline").orEmpty())
        if (home != null && deadline != null && result.optString("status") != "infeasible") {
            val margin = Duration.between(home, deadline).seconds
            lines += "${clock(home)} 귀가 예상 · 마감 ${clock(deadline)}" + if (margin >= 0) " · 여유 ${margin / 60}분" else " · 마감 초과"
            val stops = result.optJSONArray("stops")
            var wait = 0L
            var dwell = 0L
            if (stops != null) for (i in 0 until stops.length()) {
                val stop = stops.getJSONObject(i)
                wait += stop.optLong("wait_seconds").coerceAtLeast(0)
                val start = date(stop.optString("start_time"))
                val end = date(stop.optString("end_time"))
                if (start != null && end != null) dwell += Duration.between(start, end).seconds.coerceAtLeast(0)
            }
            lines += "귀가 구간 포함 이동 ${minutes(result.optLong("total_travel_seconds"))}분 · 영업 대기 ${minutes(wait)}분 · 머무름 ${minutes(dwell)}분"
        }
        val skipped = result.optJSONArray("skipped_errands")
        val errands = request?.optJSONArray("errands")
        if (skipped != null) for (i in 0 until skipped.length()) {
            val id = skipped.getJSONObject(i).optString("id")
            val item = errands?.let { array -> (0 until array.length()).map { array.getJSONObject(it) }.firstOrNull { it.optString("id") == id } }
            lines += "‘${item?.optString("label") ?: id}’는 이번 동선에 포함되지 않았어요. 제외 사유와 조건을 세부 정보에서 확인하세요."
        }
        result.optJSONObject("relaxation")?.let {
            val proposed = date(it.optString("required_deadline"))
            if (proposed != null) {
                val qualifier = if (it.optBoolean("requires_verification")) "조건부 예상 제안이에요. 영업·수령 또는 이동 정보가 미확인이므로 가능 여부를 확정할 수 없어요. " else ""
                lines += qualifier + "귀가 마감을 ${clock(proposed)}까지 ${minutes(it.optLong("extension_seconds"))}분 늘리는 제안이 있어요. 선택 항목은 제외한 제안이며, 승인 후 다시 계산해야 합니다."
            }
        }
        return lines
    }
    private fun date(value: String) = runCatching { OffsetDateTime.parse(value) }.getOrNull()
    private fun clock(value: OffsetDateTime) = value.format(DateTimeFormatter.ofPattern("HH:mm"))
    private fun minutes(seconds: Long) = (seconds.coerceAtLeast(0) + 59) / 60
}

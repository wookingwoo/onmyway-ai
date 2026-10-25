package ai.onmyway.car

import org.json.JSONArray
import org.json.JSONObject
import java.time.OffsetDateTime

/** Speech is a faithful view of the solver, never an additional feasibility claim. */
object SpokenPlan {
    private fun stops(plan: JSONObject, completed: Set<String>) = plan.optJSONArray("stops")?.let { a ->
        (0 until a.length()).map { a.getJSONObject(it) }.filter { it.optString("errand_id") !in completed }
    }.orEmpty()
    fun destinationId(plan: JSONObject, completed: Set<String>) = stops(plan, completed).firstOrNull()?.optString("errand_id") ?: "__home"
    fun canApprove(plan: JSONObject?, errands: JSONArray, completed: Set<String>): Boolean {
        if (plan == null || plan.optString("status") !in setOf("feasible", "needs_verification")) return false
        if (runCatching { OffsetDateTime.parse(plan.optString("home_arrival_time")) }.isFailure) return false
        val remaining = stops(plan, completed).map { it.optString("errand_id") }.toSet()
        return (0 until errands.length()).none { errands.getJSONObject(it).let { e ->
            e.optBoolean("mandatory", true) && e.optString("id") !in completed && e.optString("id") !in remaining
        } }
    }
    fun summary(request: JSONObject?, plan: JSONObject, errands: JSONArray, completed: Set<String>): String {
        val lines = mutableListOf<String>()
        if (plan.optBoolean("is_fixture")) lines += "샘플 계획입니다."
        when (plan.optString("status")) {
            "infeasible" -> return (lines + "현재 조건으로는 필수 볼일과 귀가 마감을 함께 지킬 동선을 찾지 못했어요. 조건을 바꿔 주세요.").joinToString(" ")
            "feasible" -> lines += "입력한 조건으로 계산한 계획을 설명할게요."
            else -> lines += "영업이나 서비스, 이동 정보가 미확인이어서 가능 여부는 확정할 수 없어요."
        }
        for ((index, stop) in stops(plan, completed).withIndex()) {
            val item = (0 until errands.length()).map { errands.getJSONObject(it) }.firstOrNull { it.optString("id") == stop.optString("errand_id") }
            val finish = item?.optString("latest_finish")?.takeUnless { it.isBlank() || it == "null" }
            lines += "${index + 1}번째는 ${stop.getJSONObject("place").optString("name")}에서 ${stop.optString("label")}입니다. ${((item?.optLong("dwell_seconds") ?: 0) + 59) / 60}분 머물러 ${time(stop.optString("end_time"))} 완료 예상이에요." +
                (finish?.let { " 완료 마감은 ${time(it)}입니다." } ?: "")
        }
        val omitted = plan.optJSONArray("skipped_errands") ?: JSONArray()
        for (i in 0 until omitted.length()) {
            val id = omitted.getJSONObject(i).optString("id")
            val item = (0 until errands.length()).map { errands.getJSONObject(it) }.firstOrNull { it.optString("id") == id }
            lines += "${item?.optString("label") ?: id}는 이번 동선에서 제외했어요."
        }
        lines += "집 도착은 ${time(plan.optString("home_arrival_time"))} 예상이고 귀가 마감은 ${time(request?.optString("deadline").orEmpty())}입니다."
        lines += "바꿀 조건이나 출발 의사를 말해 주세요."
        return lines.joinToString(" ")
    }
    private fun time(value: String): String = runCatching {
        val t = OffsetDateTime.parse(value)
        "${if (t.hour < 12) "오전" else "오후"} ${if (t.hour % 12 == 0) 12 else t.hour % 12}시" + if (t.minute == 0) "" else " ${t.minute}분"
    }.getOrDefault("확인이 필요한 시각")
}

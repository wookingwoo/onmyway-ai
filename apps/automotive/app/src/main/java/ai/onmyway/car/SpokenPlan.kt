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
    fun briefing(plan: JSONObject, errands: JSONArray, completed: Set<String>): String {
        when (plan.optString("status")) {
            "infeasible" -> return "현재 조건으로는 필수 일정과 귀가 마감을 함께 지키기 어려워요."
            "feasible" -> Unit
            else -> return "영업, 서비스 또는 이동 정보를 더 확인해야 해요. 시간에 맞출 수 있을지는 아직 확정할 수 없어요."
        }
        val labels = stops(plan, completed).map { it.optString("label") }.filter { it.isNotBlank() }
        val omitted = plan.optJSONArray("skipped_errands") ?: JSONArray()
        val skippedIds = (0 until omitted.length()).map { omitted.getJSONObject(it).optString("id") }.toSet()
        val skipped = (0 until errands.length()).map { errands.getJSONObject(it) }
            .filter { it.optString("id") in skippedIds && !it.optBoolean("mandatory", true) }
            .map { it.optString("label") }.filter { it.isNotBlank() }
        return buildList {
            add(if (labels.isEmpty()) "이제 집으로 가요." else "${labels.joinToString(", ")} 순서로 들러요.")
            if (skipped.isNotEmpty()) add("${skipped.joinToString(", ")}는 다음으로 미뤘어요.")
            val eta = plan.optString("home_arrival_time")
            if (runCatching { OffsetDateTime.parse(eta) }.isSuccess) add("집에는 ${time(eta)}쯤 도착할 예정이에요.")
            else add("귀가 시각을 확인해 주세요.")
        }.joinToString(" ")
    }
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

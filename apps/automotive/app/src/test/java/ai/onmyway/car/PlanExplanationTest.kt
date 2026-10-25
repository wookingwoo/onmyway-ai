package ai.onmyway.car

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class PlanExplanationTest {
    private fun request() = JSONObject("""{"deadline":"2026-10-23T19:00:00+09:00","errands":[{"id":"milk","label":"우유 구매"}]}""")
    private fun result(status: String = "feasible") = JSONObject("""{"status":"$status","is_fixture":true,"home_arrival_time":"2026-10-23T18:40:00+09:00","total_travel_seconds":1200,"stops":[{"wait_seconds":300,"start_time":"2026-10-23T18:10:00+09:00","end_time":"2026-10-23T18:20:00+09:00"}],"skipped_errands":[],"reasons":[],"warnings":[]}""")
    @Test fun totalsIncludeRoadWaitDwellAndDeadlineMargin() {
        val text = PlanExplanation.lines(request(), result()).joinToString("\n")
        assertTrue(text.contains("이동 20분"))
        assertTrue(text.contains("대기 5분"))
        assertTrue(text.contains("머무름 10분"))
        assertTrue(text.contains("여유 20분"))
        assertTrue(text.contains("예시"))
    }
    @Test fun uncertainResultNeverClaimsConfirmedFeasibility() {
        val text = PlanExplanation.lines(request(), result("needs_verification")).joinToString("\n")
        assertTrue(text.contains("확정할 수 없어요"))
        assertFalse(text.contains("시간 조건을 맞췄어요"))
    }
    @Test fun skippedExplanationNamesItemWithoutInventingCause() {
        val result = result().put("skipped_errands", JSONArray("""[{"id":"milk","reason":"unavailable, unverified, or no better route"}]"""))
        val text = PlanExplanation.lines(request(), result).joinToString("\n")
        assertTrue(text.contains("우유 구매"))
        assertTrue(text.contains("포함되지 않았어요"))
        assertFalse(text.contains("시간이 부족해서"))
    }
    @Test fun infeasibleDoesNotInventArrivalOrDurationAndExplainsProposal() {
        val result = result("infeasible").put("home_arrival_time", JSONObject.NULL)
            .put("relaxation", JSONObject("""{"required_deadline":"2026-10-23T19:10:00+09:00","extension_seconds":600}"""))
        val text = PlanExplanation.lines(request(), result).joinToString("\n")
        assertTrue(text.contains("필수"))
        assertTrue(text.contains("10분"))
        assertTrue(text.contains("다시 계산"))
        assertFalse(text.contains("여유"))
    }
    @Test fun uncertainProposalExplainsThatChangingTheDeadlineDoesNotVerifyFeasibility() {
        val result = result("infeasible").put("home_arrival_time", JSONObject.NULL)
            .put("relaxation", JSONObject("""{"required_deadline":"2026-10-23T19:10:00+09:00","extension_seconds":600,"requires_verification":true}"""))
        val text = PlanExplanation.lines(request(), result).joinToString("\n")
        assertTrue(text.contains("조건부"))
        assertTrue(text.contains("확정할 수 없어요"))
    }
}

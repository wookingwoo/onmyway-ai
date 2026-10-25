package ai.onmyway.car

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class SpokenPlanTest {
    private val errands = JSONArray("""[{"id":"laundry","label":"세탁물 찾기","mandatory":true,"dwell_seconds":300,"latest_finish":"2026-10-25T18:30:00+09:00"},{"id":"milk","label":"우유 구매","mandatory":false}]""")
    private val request = JSONObject("""{"deadline":"2026-10-25T19:00:00+09:00"}""")
    private fun plan(status: String = "feasible") = JSONObject("""{"status":"$status","is_fixture":false,"home_arrival_time":"2026-10-25T18:53:00+09:00","stops":[{"errand_id":"laundry","label":"세탁물 찾기","place":{"name":"한결세탁소"},"end_time":"2026-10-25T18:11:00+09:00"}],"skipped_errands":[{"id":"milk"}]}""")
    @Test fun explanationUsesActualOrderDwellDeadlinesHomeAndOmission() {
        val text = SpokenPlan.summary(request, plan(), errands, emptySet())
        for (value in listOf("한결세탁소", "5분", "6시 11분", "6시 30분", "6시 53분", "7시", "우유 구매")) assertTrue(value, text.contains(value))
    }
    @Test fun unknownAndImpossibleNeverSpeakAFeasibilityPromise() {
        assertTrue(SpokenPlan.summary(request, plan("needs_verification"), errands, emptySet()).contains("확정할 수 없어요"))
        assertFalse(SpokenPlan.canApprove(plan("infeasible"), errands, emptySet()))
        assertFalse(SpokenPlan.canApprove(plan().put("home_arrival_time", JSONObject.NULL), errands, emptySet()))
    }
    @Test fun missingMandatoryStopAndPrematureHomeCannotBeApproved() {
        assertFalse(SpokenPlan.canApprove(plan().put("stops", JSONArray()), errands, emptySet()))
        assertTrue(SpokenPlan.canApprove(plan().put("stops", JSONArray()), errands, setOf("laundry")))
        assertEquals("__home", SpokenPlan.destinationId(plan(), setOf("laundry")))
    }
    @Test fun completedStopsAreNotReadAsNewErrandsAndSampleIsAudible() {
        val text=SpokenPlan.summary(request, plan().put("is_fixture", true), errands, setOf("laundry"))
        assertTrue(text.contains("샘플")); assertFalse(text.contains("한결세탁소")); assertTrue(text.contains("집"))
    }
}

package ai.onmyway.car

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant

class PlanTimingTest {
    private val departure = Instant.parse("2026-10-25T09:00:00Z").toEpochMilli()
    private fun request() = JSONObject("""{"departure_time":"2026-10-25T09:00:00Z","deadline":"2026-10-25T10:00:00Z","errands":[{"id":"pickup","mandatory":true,"dwell_seconds":600,"latest_finish":"2026-10-25T09:30:00Z"}]}""")
    private fun result() = JSONObject("""{"status":"feasible","is_fixture":false,"home_arrival_time":"2026-10-25T09:50:00Z","stops":[{"errand_id":"pickup","arrival_time":"2026-10-25T09:10:00Z","start_time":"2026-10-25T09:10:00Z","end_time":"2026-10-25T09:20:00Z","place":{"hours_status":"verified","opening_intervals":[{"start":"2026-10-25T09:00:00Z","end":"2026-10-25T09:25:00Z"}]}}]}""")
    private fun approve(request: JSONObject? = request(), result: JSONObject? = result(), wall: Long = departure, elapsed: Long = 1000, startedWall: Long? = departure, startedElapsed: Long? = 1000) =
        PlanTiming.canApprove(request, result, wall, elapsed, startedWall, startedElapsed)

    @Test fun smallestClosingMarginBoundsApproval() {
        assertEquals(300_000L, PlanTiming.remainingMarginMillis(request(), result()))
        assertTrue(approve(wall = departure + 299_999, elapsed = 300_999))
        assertFalse(approve(wall = departure + 300_001, elapsed = 301_001))
    }
    @Test fun computationAndReviewShareFiveMinuteFreshnessWindow() {
        val r = result().apply { getJSONArray("stops").getJSONObject(0).getJSONObject("place").getJSONArray("opening_intervals").getJSONObject(0).put("end", "2026-10-25T10:00:00Z") }
        assertTrue(approve(result = r, elapsed = 301_000))
        assertFalse(approve(result = r, elapsed = 301_001))
        assertFalse(approve(result = r, startedElapsed = null))
        assertFalse(approve(result = r, startedWall = null))
    }
    @Test fun backwardWallClockCannotRestoreConsumedMargin() {
        val q = request().put("deadline", "2026-10-25T09:50:01Z")
        assertFalse(approve(request = q, wall = departure - 60_000, elapsed = 2001))
        assertTrue(approve(request = q, wall = departure - 60_000, elapsed = 2000))
    }
    @Test fun forwardWallClockConsumesMarginEvenWhenMonotonicIsFresh() {
        assertFalse(approve(wall = departure + 300_001))
    }
    @Test fun zeroMarginExpiresOnFirstMillisecond() {
        val q = request().put("deadline", "2026-10-25T09:50:00Z")
        assertEquals(0L, PlanTiming.remainingMarginMillis(q, result()))
        assertTrue(approve(request = q))
        assertFalse(approve(request = q, elapsed = 1001))
    }
    @Test fun futureDepartureOffsetDoesNotSpendSlackBeforeDeparture() {
        val start = departure - 120_000
        assertTrue(approve(wall = start + 60_000, elapsed = 61_000, startedWall = start))
        assertTrue(approve(wall = departure, elapsed = 121_000, startedWall = start))
        assertFalse(approve(wall = departure, elapsed = 301_001, startedWall = start))
    }
    @Test fun perErrandDeadlineAndHomeDeadlineEachBoundSlack() {
        val q = request().apply { getJSONArray("errands").getJSONObject(0).put("latest_finish", "2026-10-25T09:20:02Z") }
        assertEquals(2000L, PlanTiming.remainingMarginMillis(q, result()))
        assertFalse(approve(request = q, elapsed = 3001))
        assertEquals(1000L, PlanTiming.remainingMarginMillis(request().put("deadline", "2026-10-25T09:50:01Z"), result()))
    }
    @Test fun openingIntervalMustContainWholeDwell() {
        val r = result().apply { getJSONArray("stops").getJSONObject(0).getJSONObject("place").getJSONArray("opening_intervals").getJSONObject(0).put("start", "2026-10-25T09:15:00Z") }
        assertNull(PlanTiming.remainingMarginMillis(request(), r))
        assertFalse(approve(result = r))
    }
    @Test fun missingMalformedOrUnknownBoundsCannotApprove() {
        assertFalse(approve(request = null))
        assertFalse(approve(result = null))
        assertFalse(approve(request = request().put("deadline", "not-a-date")))
        assertFalse(approve(request = request().put("departure_time", "2026-10-25T09:00:00")))
        assertFalse(approve(result = result().put("home_arrival_time", JSONObject.NULL)))
        assertFalse(approve(request = request().apply { getJSONArray("errands").getJSONObject(0).put("latest_finish", "bad") }))
        assertFalse(approve(result = result().apply { getJSONArray("stops").getJSONObject(0).getJSONObject("place").put("hours_status", "closed") }))
        assertFalse(approve(result = result().apply { getJSONArray("stops").getJSONObject(0).getJSONObject("place").remove("opening_intervals") }))
    }
    @Test fun unknownHoursKeepApprovalSeparateFromFeasibility() {
        val r = result().put("status", "needs_verification").apply { getJSONArray("stops").getJSONObject(0).getJSONObject("place").put("hours_status", "unknown").remove("opening_intervals") }
        assertEquals(600_000L, PlanTiming.remainingMarginMillis(request(), r))
        assertTrue(approve(result = r))
        assertEquals("needs_verification", r.getString("status"))
    }
    @Test fun unknownHoursStillRespectSuppliedClosingMargin() {
        val r = result().put("status", "needs_verification").apply {
            getJSONArray("stops").getJSONObject(0).getJSONObject("place").apply {
                put("hours_status", "unknown")
                getJSONArray("opening_intervals").getJSONObject(0).put("end", "2026-10-25T09:20:01Z")
            }
        }
        assertEquals(1000L, PlanTiming.remainingMarginMillis(request(), r))
        assertTrue(approve(result = r, elapsed = 2000))
        assertFalse(approve(result = r, elapsed = 2001))
        assertEquals("needs_verification", r.getString("status"))
    }
    @Test fun unknownHoursSuppliedWindowMustContainCompleteDwell() {
        val r = result().put("status", "needs_verification").apply {
            getJSONArray("stops").getJSONObject(0).getJSONObject("place").apply {
                put("hours_status", "unknown")
                getJSONArray("opening_intervals").getJSONObject(0).put("start", "2026-10-25T09:15:00Z")
            }
        }
        assertNull(PlanTiming.remainingMarginMillis(request(), r))
        assertFalse(approve(result = r))
    }
    @Test fun unknownHoursEmptyIntervalsRetainWarningBasedApproval() {
        val r = result().put("status", "needs_verification").apply {
            getJSONArray("stops").getJSONObject(0).getJSONObject("place").put("hours_status", "unknown").put("opening_intervals", org.json.JSONArray())
        }
        assertEquals(600_000L, PlanTiming.remainingMarginMillis(request(), r))
        assertTrue(approve(result = r))
    }
    @Test fun noMandatoryStopCanDisappearAndUnknownStopCannotAcquireBounds() {
        assertFalse(approve(result = result().put("stops", org.json.JSONArray())))
        assertFalse(approve(result = result().apply { getJSONArray("stops").getJSONObject(0).put("errand_id", "another") }))
    }
    @Test fun nullableLatestFinishIsOptionalAndHomeOnlyPlanHasHomeSlack() {
        val q = request().apply { getJSONArray("errands").getJSONObject(0).put("latest_finish", JSONObject.NULL) }
        assertEquals(300_000L, PlanTiming.remainingMarginMillis(q, result()))
        assertEquals(600_000L, PlanTiming.remainingMarginMillis(q.put("errands", org.json.JSONArray()), result().put("stops", org.json.JSONArray())))
    }
    @Test fun malformedMandatoryFlagCannotSilentlyDropRequiredErrand() {
        val q = request().apply { getJSONArray("errands").getJSONObject(0).put("mandatory", "false") }
        assertFalse(approve(request = q, result = result().put("stops", org.json.JSONArray())))
    }
    @Test fun forwardWallJumpCannotExtendFreshnessBeforeFutureDeparture() {
        assertFalse(approve(wall = departure - 1_000, startedWall = departure - 302_000))
    }
    @Test fun invalidChronologyAndClockRollbackFailClosed() {
        assertFalse(approve(elapsed = 999))
        assertFalse(approve(result = result().put("home_arrival_time", "2026-10-25T08:00:00Z")))
        assertFalse(approve(result = result().apply { getJSONArray("stops").getJSONObject(0).put("end_time", "2026-10-25T09:09:00Z") }))
        assertFalse(approve(result = result().put("status", "infeasible")))
    }
}

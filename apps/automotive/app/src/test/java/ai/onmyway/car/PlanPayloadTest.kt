package ai.onmyway.car

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class PlanPayloadTest {
    private fun request() = JSONObject("""{
        "origin":{"id":"pharmacy"},"home":{"id":"home"},
        "errands":[{"id":"laundry-task","mandatory":true,"latest_finish":"2026-10-23T19:00:00+09:00","candidates":[{"id":"laundry"}]}],
        "travel_edges":[
            {"from_id":"old-origin","to_id":"pharmacy","seconds":120},
            {"from_id":"pharmacy","to_id":"laundry","seconds":300},
            {"from_id":"pharmacy","to_id":"home","seconds":450},
            {"from_id":"laundry","to_id":"home","seconds":240}
        ]} """)
    @Test fun completionReplanDropsRemovedLocationEdgesButRetainsCurrentOrigin() {
        val original = request()
        val result = PlanPayload.trimFixtureEdges(original)
        assertEquals(3, result.getJSONArray("travel_edges").length())
        assertEquals("pharmacy", result.getJSONArray("travel_edges").getJSONObject(0).getString("from_id"))
        assertEquals(4, original.getJSONArray("travel_edges").length())
        assertTrue(result.getJSONArray("errands").getJSONObject(0).getBoolean("mandatory"))
        assertEquals("2026-10-23T19:00:00+09:00", result.getJSONArray("errands").getJSONObject(0).getString("latest_finish"))
    }
    @Test fun afterFinalErrandHomeLegRemainsPlannable() {
        val input = request().put("origin", JSONObject().put("id", "laundry")).put("errands", org.json.JSONArray())
        val result = PlanPayload.trimFixtureEdges(input)
        assertEquals(1, result.getJSONArray("travel_edges").length())
        assertEquals("home", result.getJSONArray("travel_edges").getJSONObject(0).getString("to_id"))
    }
}

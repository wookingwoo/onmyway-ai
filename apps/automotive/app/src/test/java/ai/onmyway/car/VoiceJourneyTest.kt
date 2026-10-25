package ai.onmyway.car

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class VoiceJourneyTest {
    private fun item(mode: String? = null) = JSONObject().put("id", "e").put("label", "약 구매").put("query", "약국")
        .apply { if (mode != null) put("place_mode", mode) }

    @Test fun onlyExplicitlyFlexibleErrandsBypassVenueSelection() {
        assertFalse(VoiceJourney.placesReady(JSONArray().put(item()), false))
        assertTrue(VoiceJourney.placesReady(JSONArray().put(item("flexible")), false))
        assertFalse(VoiceJourney.placesReady(JSONArray().put(item("fixed")), false))
    }

    @Test fun unchangedPinsSurviveRefinementButQueryChangesDropThem() {
        val previous = item("fixed").put("selected_place", JSONObject().put("id", "my-pickup"))
        val next = item("fixed").put("dwell_seconds", 1800)
        val merged = VoiceJourney.retainPins(JSONArray().put(next), JSONArray().put(previous))
        assertEquals("my-pickup", merged.getJSONObject(0).getJSONObject("selected_place").getString("id"))
        assertFalse(next.has("selected_place"))
        assertFalse(VoiceJourney.retainPins(JSONArray().put(item("fixed").put("query", "other")), JSONArray().put(previous)).getJSONObject(0).has("selected_place"))
    }

    @Test fun placeModeSurvivesDiscoveryAndRefinementPayloads() {
        val items = JSONArray().put(item("flexible"))
        assertEquals("flexible", PlaceReview.discoveryErrands(items, emptySet()).getJSONObject(0).getString("place_mode"))
        val review = IntentReview("약국에 들러줘", "2026-10-23T18:00:00+09:00", false,
            JSONObject().put("errands", items).put("deadline", "2026-10-23T19:00:00+09:00").put("clarification_questions", JSONArray()))
        val payload = review.refinementPayload("약국은 30분 걸릴 것 같아", items)
        assertEquals("flexible", payload.getJSONArray("previous_errands").getJSONObject(0).getString("place_mode"))
        assertEquals(0, review.answers.length())
    }

    @Test fun exampleAndBlankRefinementsNeverProduceLivePayload() {
        val r = IntentReview("예시", "2026-10-23T18:00:00+09:00", true, JSONObject())
        assertThrows(IllegalArgumentException::class.java) { r.refinementPayload("30분", JSONArray()) }
        assertThrows(IllegalArgumentException::class.java) { r.refinementPayload("  ", JSONArray()) }
    }
}

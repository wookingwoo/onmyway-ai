package ai.onmyway.car

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class PlaceReviewTest {
    private fun errand() = JSONObject("""{"id":"laundry","label":"세탁물 찾기","query":"맡긴 세탁소","mandatory":true,"dwell_seconds":300,"latest_finish":null}""")
    private fun place(fixture: Boolean = false) = JSONObject("""{"id":"store-17","name":"맡긴 세탁소","address":"서울 테스트로 17","latitude":37.5,"longitude":127.0,"hours_status":"unknown","service_status":"unknown","opening_intervals":[],"source":{"provider":"kakao-local","retrieved_at":"2026-10-25T08:00:00+09:00","is_fixture":$fixture}}""")

    @Test fun chosenPickupIdentitySurvivesRemainingPayloadWithoutCandidateLeak() {
        val original = errand()
        val selected = PlaceReview.pin(original, place(), false)
        selected.put("candidates", JSONArray().put(place().put("id", "closer-wrong-store")))
        val payload = PlaceReview.discoveryErrands(JSONArray().put(selected).put(errand().put("id", "done")), setOf("done"))
        assertEquals(1, payload.length())
        assertEquals("store-17", payload.getJSONObject(0).getJSONObject("selected_place").getString("id"))
        assertFalse(payload.getJSONObject(0).has("candidates"))
        assertFalse(original.has("selected_place"))
        assertTrue(PlaceReview.allSelected(payload, false))
    }

    @Test fun changingSearchQueryClearsTheOldVenueAndItsEvidence() {
        val selected = PlaceReview.pin(errand(), place(), false)
        val changed = PlaceReview.withQuery(selected, "다른 곳")
        assertFalse(changed.has("selected_place"))
        assertFalse(PlaceReview.allSelected(JSONArray().put(changed), false))
        assertEquals("store-17", selected.getJSONObject("selected_place").getString("id"))
        assertTrue(PlaceReview.withQuery(selected, "맡긴 세탁소").has("selected_place"))
    }

    @Test fun explicitEvidenceKeepsBasisAndWindowWithoutInventingServiceConfirmation() {
        val selected = PlaceReview.pin(errand(), place(), false)
        val updated = PlaceReview.confirmAvailability(selected, "2026-10-25T17:00:00+09:00", "2026-10-25T19:00:00+09:00", "매장에 전화해 수령시간 확인", "2026-10-25T16:00:00+09:00", false)
        val venue = updated.getJSONObject("selected_place")
        assertEquals("verified", venue.getString("hours_status"))
        assertEquals("unknown", venue.getString("service_status"))
        assertEquals("매장에 전화해 수령시간 확인", venue.getJSONObject("availability_evidence").getString("basis"))
        assertEquals("2026-10-25T19:00:00+09:00", venue.getJSONArray("opening_intervals").getJSONObject(0).getString("end"))
        assertEquals("unknown", selected.getJSONObject("selected_place").getString("hours_status"))
    }

    @Test fun liveSelectionCannotImportSyntheticPlaces() {
        try { PlaceReview.pin(errand(), place(true), false); fail("Fixture must be rejected") } catch (_: IllegalArgumentException) {}
        assertTrue(PlaceReview.allSelected(JSONArray().put(PlaceReview.pin(errand(), place(true), true)), true))
    }

    @Test fun blankBasisAndReversedAvailabilityCannotBeSaved() {
        val selected = PlaceReview.pin(errand(), place(), false)
        for ((end, basis) in listOf("2026-10-25T19:00:00+09:00" to " ", "2026-10-25T16:00:00+09:00" to "전화 확인")) {
            try { PlaceReview.confirmAvailability(selected, "2026-10-25T17:00:00+09:00", end, basis, "2026-10-25T16:00:00+09:00", true); fail("Invalid evidence accepted") } catch (_: IllegalArgumentException) {}
        }
    }

    @Test fun changingTheRequestedServiceInvalidatesEvidenceButKeepsTheVenue() {
        val known = PlaceReview.confirmAvailability(PlaceReview.pin(errand(), place(), false), "2026-10-25T17:00:00+09:00", "2026-10-25T19:00:00+09:00", "전화 확인", "2026-10-25T16:00:00+09:00", true)
        val changed = PlaceReview.withLabel(known, "새 세탁물 맡기기")
        val venue = changed.getJSONObject("selected_place")
        assertEquals("store-17", venue.getString("id"))
        assertEquals("unknown", venue.getString("service_status"))
        assertFalse(venue.has("availability_evidence"))
        assertTrue(known.getJSONObject("selected_place").has("availability_evidence"))
    }

    @Test fun editedFixtureSelectionIsAlsoTheOnlyPlannerCandidate() {
        val selected = PlaceReview.pin(errand().put("candidates", JSONArray().put(place(true))), place(true), true)
        val confirmed = PlaceReview.confirmAvailability(selected, "2026-10-25T17:00:00+09:00", "2026-10-25T19:00:00+09:00", "예시 확인 체험", "2026-10-25T16:00:00+09:00", true)
        val payload = PlaceReview.fixtureErrands(JSONArray().put(confirmed), emptySet())
        val candidate = payload.getJSONObject(0).getJSONArray("candidates").getJSONObject(0)
        assertEquals("예시 확인 체험", candidate.getJSONObject("availability_evidence").getString("basis"))
        assertEquals("store-17", candidate.getString("id"))
    }
}

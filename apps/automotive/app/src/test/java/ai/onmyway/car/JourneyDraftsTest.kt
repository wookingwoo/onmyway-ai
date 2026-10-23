package ai.onmyway.car

import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class JourneyDraftsTest {
    @Test fun largeDurationControlsPreserveSecondsAndRespectApiBoundaries() {
        assertEquals(900, JourneyDrafts.adjustDwell(600, 5))
        assertEquals(600, JourneyDrafts.adjustDwell(900, -5))
        assertEquals(0, JourneyDrafts.adjustDwell(60, -5))
        assertEquals(86400, JourneyDrafts.adjustDwell(86300, 5))
        assertEquals(390, JourneyDrafts.adjustDwell(90, 5))
    }
    @Test fun aRestoredDeadlineCanMoveToAnExplicitlyChosenNewDate() {
        assertEquals("2026-10-24T20:15+09:00", JourneyDrafts.deadline(
            "2026-10-23T19:00:00+09:00", LocalDate.of(2026, 10, 24), 20, 15))
    }
    @Test fun leavingAnExampleCannotReuseItsCoordinatesAsLiveLocations() {
        val drafts = mapOf("draft-originLat" to "37.5", "draft-originLon" to "127.0",
            "draft-homeLat" to "37.51", "draft-homeLon" to "127.02",
            "draft-deadline" to "2026-10-23T19:00:00+09:00",
            "server" to "http://10.0.2.2:8000", "input" to "실제 할 일")
        val clean = JourneyDrafts.withoutFixtureLocations(drafts, true)
        assertFalse(clean.keys.any { it.startsWith("draft-") })
        assertEquals(drafts["server"], clean["server"])
        assertEquals(drafts["input"], clean["input"])
        assertEquals(drafts, JourneyDrafts.withoutFixtureLocations(drafts, false))
    }
}

package ai.onmyway.car

import org.junit.Assert.*
import org.junit.Test

class FilmJourneyTest {
    @Test fun unknownAndDrivingCannotAdvanceTheFilmingJourney() {
        val f = FilmJourney()
        assertFalse(f.next(VehicleState.UNKNOWN))
        assertFalse(f.next(VehicleState.RESTRICTED))
        assertEquals(FilmStage.REQUEST, f.stage)
        assertTrue(f.next(VehicleState.PARKED))
    }
    @Test fun revisedPlanNeedsFreshApprovalAndArrivalDoesNotComplete() {
        val f = FilmJourney()
        repeat(4) { assertTrue(f.next(VehicleState.PARKED)) }
        assertEquals(FilmStage.REVISED, f.stage)
        assertNull(f.session.activeId)
        assertTrue(f.next(VehicleState.PARKED))
        assertEquals("laundry", f.session.activeId)
        assertTrue(f.next(VehicleState.PARKED))
        assertEquals("laundry", f.session.arrivedId)
        assertTrue(f.session.completed.isEmpty())
        assertTrue(f.next(VehicleState.PARKED))
        assertEquals(setOf("laundry"), f.session.completed)
        assertNull(f.session.activeId)
    }
    @Test fun bothMandatoryErrandsPrecedeSeparateHomeApproval() {
        val f = FilmJourney()
        repeat(10) { assertTrue(f.next(VehicleState.PARKED)) }
        assertEquals(FilmStage.HOME, f.stage)
        assertEquals(setOf("laundry", "medicine"), f.session.completed)
        assertNull(f.session.activeId)
        assertTrue(f.next(VehicleState.PARKED))
        assertEquals("home", f.session.activeId)
        assertTrue(f.next(VehicleState.PARKED))
        assertEquals(FilmStage.FINISHED, f.stage)
    }
}

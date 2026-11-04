package ai.onmyway.car

import org.junit.Assert.*
import org.junit.Test

class CockpitPresentationTest {
    @Test fun routeMenuOffersOnlyActionsForTheCurrentLeg() {
        assertEquals(listOf(RouteMenuAction.DETAILS, RouteMenuAction.EDIT, RouteMenuAction.RECALCULATE, RouteMenuAction.SETTINGS),
            CockpitPresentation.routeMenu(null, null, false, false, true))
        assertEquals(listOf(RouteMenuAction.DETAILS, RouteMenuAction.CANCEL, RouteMenuAction.RECONNECT, RouteMenuAction.SETTINGS),
            CockpitPresentation.routeMenu("laundry", null, false, false, false))
        assertEquals(listOf(RouteMenuAction.DETAILS, RouteMenuAction.OVERVIEW, RouteMenuAction.CANCEL, RouteMenuAction.SETTINGS),
            CockpitPresentation.routeMenu("laundry", null, false, true, true))
        assertEquals(listOf(RouteMenuAction.DETAILS, RouteMenuAction.SETTINGS),
            CockpitPresentation.routeMenu("laundry", null, true, true, false))
        assertEquals(listOf(RouteMenuAction.DETAILS, RouteMenuAction.SETTINGS),
            CockpitPresentation.routeMenu("laundry", "laundry", false, true, false))
        assertTrue(RouteMenuAction.VOICE in CockpitPresentation.routeMenu(null, null, true, false, true, voiceInMenu = true))
        assertFalse(RouteMenuAction.VOICE in CockpitPresentation.routeMenu("__home", null, true, true, true, voiceInMenu = true))
    }

    @Test fun movingOverviewOpensOnlyForAnApprovedDestinationWhileParked() {
        assertTrue(CockpitPresentation.canOpenMovingOverview("laundry", null, true, VehicleState.PARKED, false))
        assertFalse(CockpitPresentation.canOpenMovingOverview("laundry", null, true, VehicleState.UNKNOWN, false))
        assertFalse(CockpitPresentation.canOpenMovingOverview("laundry", null, true, VehicleState.RESTRICTED, false))
        assertFalse(CockpitPresentation.canOpenMovingOverview("laundry", "laundry", true, VehicleState.PARKED, false))
        assertFalse(CockpitPresentation.canOpenMovingOverview("laundry", null, false, VehicleState.PARKED, false))
        assertFalse(CockpitPresentation.canOpenMovingOverview(null, null, true, VehicleState.PARKED, false))
        assertFalse(CockpitPresentation.canOpenMovingOverview("laundry", null, true, VehicleState.PARKED, true))
    }
    @Test fun arrivalUtilityCannotAffectLiveRequestsOrAnAlreadyArrivedLeg() {
        assertTrue(CockpitPresentation.canSendSampleArrival(true, true, true, "laundry", null, VehicleState.PARKED, false))
        assertFalse(CockpitPresentation.canSendSampleArrival(false, true, true, "laundry", null, VehicleState.PARKED, false))
        assertFalse(CockpitPresentation.canSendSampleArrival(true, false, true, "laundry", null, VehicleState.PARKED, false))
        assertFalse(CockpitPresentation.canSendSampleArrival(true, true, false, "laundry", null, VehicleState.PARKED, false))
        assertFalse(CockpitPresentation.canSendSampleArrival(true, true, true, null, null, VehicleState.PARKED, false))
        assertFalse(CockpitPresentation.canSendSampleArrival(true, true, true, "laundry", "laundry", VehicleState.PARKED, false))
        assertFalse(CockpitPresentation.canSendSampleArrival(true, true, true, "laundry", null, VehicleState.UNKNOWN, false))
        assertFalse(CockpitPresentation.canSendSampleArrival(true, true, true, "laundry", null, VehicleState.RESTRICTED, false))
        assertFalse(CockpitPresentation.canSendSampleArrival(true, true, true, "laundry", null, VehicleState.PARKED, true))
    }

    @Test fun staleCompletionControlCannotCompleteTheCurrentLeg() {
        val state = SessionState()
        state.approve("laundry"); assertTrue(state.requestNavigation("laundry")); state.routeAccepted(); state.arrived()
        val rendered = state.revision
        state.edited()
        assertFalse(state.completeForRevision("laundry", rendered))
        assertTrue(state.completed.isEmpty())
        assertTrue(state.completeForRevision("laundry", state.revision))
        assertEquals(setOf("laundry"), state.completed)
        assertFalse(state.completeForRevision("laundry", state.revision))
    }
}

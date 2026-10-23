package ai.onmyway.car

import org.junit.Assert.*
import org.junit.Test

class SessionStateTest {
    @Test fun unknownVehicleStateDisablesInteraction() {
        assertFalse(SessionState().canInteract(VehicleState.UNKNOWN, false))
        assertFalse(SessionState().canInteract(VehicleState.RESTRICTED, false))
        assertTrue(SessionState().canInteract(VehicleState.PARKED, false))
    }
    @Test fun fixtureOverrideNeverAppliesToLiveBuild() {
        assertFalse(SessionState().canInteract(VehicleState.UNKNOWN, false, true))
        assertTrue(SessionState().canInteract(VehicleState.UNKNOWN, true, true))
        assertFalse(SessionState().canInteract(VehicleState.RESTRICTED, true, true))
    }
    @Test fun routeRequiresApprovalForCurrentRevision() {
        val state = SessionState()
        assertFalse(state.requestNavigation("pharmacy"))
        state.approve("pharmacy")
        state.edited()
        assertFalse(state.requestNavigation("pharmacy"))
    }
    @Test fun approvalCannotAuthorizeDifferentStop() {
        val state = SessionState()
        state.approve("pharmacy")
        assertFalse(state.requestNavigation("laundry"))
        assertTrue(state.requestNavigation("pharmacy"))
    }
    @Test fun arrivalNeverCompletesErrand() {
        val state = SessionState()
        state.approve("pharmacy")
        state.requestNavigation("pharmacy")
        state.routeAccepted()
        state.arrived()
        assertEquals("pharmacy", state.arrivedId)
        assertTrue(state.completed.isEmpty())
        state.complete("pharmacy")
        assertTrue(state.completed.contains("pharmacy"))
    }
    @Test fun staleResponseCannotReplaceEdits() {
        val state = SessionState()
        val requestRevision = state.revision
        state.edited()
        assertFalse(state.acceptsResponse(requestRevision))
    }
    @Test fun navigationFailureAndRepeatedArrivalDoNotAdvance() {
        val state = SessionState()
        state.approve("a"); state.requestNavigation("a"); state.routeFailed()
        state.arrived()
        assertNull(state.arrivedId)
        assertTrue(state.completed.isEmpty())
    }
}

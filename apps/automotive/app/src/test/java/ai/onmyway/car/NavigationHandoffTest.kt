package ai.onmyway.car

import org.junit.Assert.*
import org.junit.Test

class NavigationHandoffTest {
    @Test fun currentAcceptedLiveDestinationLaunchesOnceIncludingHome() {
        for (id in listOf("laundry", "__home")) {
            val h=NavigationHandoff();h.arm(7,id)
            assertFalse(h.take(7,id,false,true,VehicleState.PARKED,false))
            assertTrue(h.take(7,id,true,true,VehicleState.PARKED,false))
            assertFalse(h.take(7,id,true,true,VehicleState.PARKED,false))
        }
    }
    @Test fun fixtureStaleBackgroundRestrictedAndArrivalCannotLaunch() {
        fun gate()=NavigationHandoff().also { it.arm(7,"laundry") }
        assertFalse(gate().take(7,"laundry",true,true,VehicleState.PARKED,true))
        assertFalse(gate().take(8,"laundry",true,true,VehicleState.PARKED,false))
        assertFalse(gate().take(7,"medicine",true,true,VehicleState.PARKED,false))
        assertFalse(gate().take(7,"laundry",true,false,VehicleState.PARKED,false))
        assertFalse(gate().take(7,"laundry",true,true,VehicleState.RESTRICTED,false))
        assertFalse(gate().take(7,"laundry",true,true,VehicleState.UNKNOWN,false))
        assertFalse(gate().take(7,null,true,true,VehicleState.PARKED,false))
        val h=gate();h.cancel();assertFalse(h.take(7,"laundry",true,true,VehicleState.PARKED,false))
    }
    @Test fun backgroundAcceptanceIsNotDeferredUntilAnUnsolicitedReturn() {
        val h=NavigationHandoff();h.arm(7,"laundry")
        assertFalse(h.take(7,"laundry",true,false,VehicleState.PARKED,false))
        assertFalse(h.take(7,"laundry",true,true,VehicleState.PARKED,false))
    }
    @Test fun driveOrUnknownThenParkCannotReviveTheApprovedAutomaticHandoff() {
        for (restricted in listOf(VehicleState.RESTRICTED, VehicleState.UNKNOWN)) {
            val h=NavigationHandoff();h.arm(7,"laundry")
            h.vehicleChanged(restricted);h.vehicleChanged(VehicleState.PARKED)
            assertFalse(h.take(7,"laundry",true,true,VehicleState.PARKED,false))
        }
    }
    @Test fun mapOnlyRecoveryDoesNotSupplyRouteAuthorityAndStillRequiresPark() {
        assertTrue(NavigationHandoff.canShow(true,false,true,false,VehicleState.PARKED,false))
        assertTrue(NavigationHandoff.canShow(true,false,false,true,VehicleState.PARKED,false))
        assertFalse(NavigationHandoff.canShow(true,false,false,false,VehicleState.PARKED,false))
        assertFalse(NavigationHandoff.canShow(true,true,true,true,VehicleState.PARKED,false))
        assertFalse(NavigationHandoff.canShow(false,false,true,true,VehicleState.PARKED,false))
        assertFalse(NavigationHandoff.canShow(true,false,true,true,VehicleState.RESTRICTED,false))
        assertFalse(NavigationHandoff.canShow(true,false,true,true,VehicleState.PARKED,true))
    }
}

package ai.onmyway.car

import org.junit.Assert.*
import org.junit.Test

class LocationSetupTest {
    private val now = 1_800_000_000_000L
    private val open = LocationAccess(true, VehicleState.PARKED, false, false, false)
    private fun fix(time: Long = now) = SetupLocation(37.5, 127.0, LocationSource.DEVICE, "차량 현재 위치", time)

    @Test fun interpretingALiveRequestPreservesConfirmedDepartureWhileFixtureTransitionClearsIt() {
        val values = mapOf("live-origin" to fix().toJson(), "draft-originLat" to "37.5", "draft-originLon" to "127.0")
        val live = LocationSetup.prepareForInterpretation(values, false)
        assertEquals("37.5", live["draft-originLat"])
        assertEquals(fix().toJson(), live["live-origin"])
        assertFalse(LocationSetup.prepareForInterpretation(values, true).containsKey("draft-originLat"))
    }

    @Test fun newRequestCannotReuseAPreviousJourneysCompletedStopAsCurrentPosition() {
        val stop = fix(0).copy(source = LocationSource.COMPLETED_STOP, label = "마지막 완료 장소")
        val values = mapOf("live-origin" to stop.toJson(), "draft-originLat" to "37.5", "draft-originLon" to "127.0")
        assertFalse(LocationSetup.prepareForInterpretation(values, false, true).containsKey("live-origin"))
        assertFalse(LocationSetup.prepareForInterpretation(values, false, true).containsKey("draft-originLat"))
        assertEquals(stop.toJson(), LocationSetup.prepareForInterpretation(values, false, false)["live-origin"])
        val configured = values + ("live-origin" to fix().toJson())
        assertEquals(fix().toJson(), LocationSetup.prepareForInterpretation(configured, false, true)["live-origin"])
    }

    @Test fun acceptsOnlyFreshFiniteDeviceFixesAtExactBoundaries() {
        assertTrue(fix(now - 60_000).usable(now, true))
        assertFalse(fix(now - 60_001).usable(now, true))
        assertTrue(fix(now + 5_000).usable(now, true))
        assertFalse(fix(now + 5_001).usable(now, true))
        assertFalse(fix(0).usable(now, true))
        assertFalse(fix().copy(latitude = Double.NaN).usable(now, true))
        assertFalse(fix().copy(longitude = Double.POSITIVE_INFINITY).usable(now, true))
        assertFalse(fix().copy(latitude = 91.0).usable(now, true))
        assertFalse(fix().copy(longitude = -181.0).usable(now, true))
    }

    @Test fun deviceOriginExpiresButSavedHomeAndExplicitManualOriginDoNotClaimFreshness() {
        assertFalse(fix(now - 61_000).usable(now, true))
        assertTrue(fix(now - 61_000).usable(now, false))
        assertTrue(fix(0).copy(source = LocationSource.MANUAL).usable(now, true))
        assertTrue(fix(0).copy(source = LocationSource.COMPLETED_STOP).usable(now, true))
    }

    @Test fun everyMutationRequiresForegroundNativeParkAndNoSyntheticOrOwnedGuidance() {
        assertTrue(open.canEdit)
        for (blocked in listOf(open.copy(foreground = false), open.copy(vehicle = VehicleState.UNKNOWN),
            open.copy(vehicle = VehicleState.RESTRICTED), open.copy(synthetic = true),
            open.copy(guidance = true), open.copy(busy = true))) assertFalse(blocked.canEdit)
    }

    @Test fun duplicateOldCancelledAndTimedOutCallbacksCannotProduceASelection() {
        val s = LocationAttempt()
        val old = s.begin(4, 100, open)!!
        val current = s.begin(4, 110, open)!!
        assertFalse(s.finish(old, 4, 120, open))
        assertTrue(s.finish(current, 4, 130, open))
        assertFalse(s.finish(current, 4, 140, open))
        val cancelled = s.begin(4, 150, open)!!; s.cancel()
        assertFalse(s.finish(cancelled, 4, 160, open))
        val expired = s.begin(4, 200, open)!!
        assertFalse(s.finish(expired, 4, 10_201, open))
    }

    @Test fun revisionChangeOrRestrictionInvalidatesAnAttemptEvenIfParkReturns() {
        val s = LocationAttempt()
        val token = s.begin(3, 100, open)!!
        assertFalse(s.finish(token, 4, 110, open))
        val driveToken = s.begin(4, 120, open)!!
        s.update(open.copy(vehicle = VehicleState.RESTRICTED))
        s.update(open)
        assertFalse(s.finish(driveToken, 4, 130, open))
        assertNull(s.begin(4, 140, open.copy(synthetic = true)))
    }

    @Test fun confirmationRechecksRevisionFreshnessAndGuidanceInsteadOfRestoringAuthority() {
        assertTrue(LocationSetup.canSave(8, 8, open, fix(), now))
        assertFalse(LocationSetup.canSave(8, 9, open, fix(), now))
        assertFalse(LocationSetup.canSave(8, 8, open.copy(guidance = true), fix(), now))
        assertFalse(LocationSetup.canSave(8, 8, open, fix(now - 60_001), now))
    }

    @Test fun realHomeIsPreservedAcrossFixtureResetWithoutRetainingFixtureCoordinates() {
        val home = fix().copy(source = LocationSource.PLACE, label = "선택한 집 근처")
        val values = mapOf("real-home" to home.toJson(), "draft-homeLat" to "35.0",
            "draft-homeLon" to "128.0", "draft-originLat" to "35.1", "draft-originLon" to "128.1",
            "live-origin" to fix().toJson(), "server" to "http://127.0.0.1:8000")
        val reset = LocationSetup.reset(values, true)
        assertEquals("37.5", reset["draft-homeLat"])
        assertEquals("127.0", reset["draft-homeLon"])
        assertEquals("선택한 집 근처", SetupLocation.parse(reset["real-home"])?.label)
        assertFalse(reset.containsKey("draft-originLat"))
        assertFalse(reset.containsKey("live-origin"))
        assertEquals("http://127.0.0.1:8000", reset["server"])
    }

    @Test fun corruptHomeCannotRepopulateAResetAndOldDeviceMetadataMustMatchCoordinates() {
        val reset = LocationSetup.reset(mapOf("real-home" to "{}", "draft-homeLat" to "35"), true)
        assertFalse(reset.containsKey("draft-homeLat"))
        assertNull(SetupLocation.parse("{}"))
        assertNull(SetupLocation.parse("{\"latitude\":37.5}"))
        assertTrue(LocationSetup.originReady(fix().toJson(), "37.5", "127.0", now))
        assertFalse(LocationSetup.originReady(fix(now - 61_000).toJson(), "37.5", "127.0", now))
        assertFalse(LocationSetup.originReady(fix().toJson(), "35.0", "128.0", now))
        assertFalse(LocationSetup.originReady(null, "37.5", "127.0", now))
        assertFalse(LocationSetup.originReady(null, "", "127.0", now))
    }
}

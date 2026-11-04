package ai.onmyway.car

/** Service presentation is shared; source-specific testing controls stay in settings. */
object CockpitPresentation {
    fun canOpenMovingOverview(activeId: String?, arrivedId: String?, accepted: Boolean,
                              vehicle: VehicleState, busy: Boolean) =
        !activeId.isNullOrBlank() && arrivedId == null && accepted && vehicle == VehicleState.PARKED && !busy

    fun canSendSampleArrival(demo: Boolean, fixture: Boolean, simulation: Boolean,
                             activeId: String?, arrivedId: String?, vehicle: VehicleState, busy: Boolean) =
        demo && fixture && simulation && !activeId.isNullOrBlank() && arrivedId == null && vehicle == VehicleState.PARKED && !busy
}

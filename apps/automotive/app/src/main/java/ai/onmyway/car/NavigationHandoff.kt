package ai.onmyway.car

/** Screen visibility is separate from route authority. One current automatic handoff. */
class NavigationHandoff {
    private var pending: Pair<Long,String>? = null
    fun arm(revision: Long, destinationId: String) { pending=revision to destinationId }
    fun cancel() { pending=null }
    fun vehicleChanged(state: VehicleState) { if (state!=VehicleState.PARKED) cancel() }
    fun take(revision: Long, destinationId: String?, accepted: Boolean, foreground: Boolean,
             vehicle: VehicleState, fixture: Boolean): Boolean {
        if (!accepted) return false
        val expected=pending;pending=null
        return expected!=null && expected==revision to destinationId && foreground &&
            vehicle==VehicleState.PARKED && !fixture
    }
    companion object {
        fun canShow(connected: Boolean, fixture: Boolean, accepted: Boolean, interrupted: Boolean,
                    vehicle: VehicleState, busy: Boolean) = connected && !fixture &&
            (accepted || interrupted) && vehicle==VehicleState.PARKED && !busy
    }
}

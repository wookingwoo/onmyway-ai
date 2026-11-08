package ai.onmyway.car

enum class RouteMenuAction { DETAILS, EDIT, RECALCULATE, VOICE, OVERVIEW, CANCEL, RECONNECT, SETTINGS }

/** Service presentation is shared; source-specific testing controls stay in settings. */
object CockpitPresentation {
    fun routeMenu(activeId: String?, arrivedId: String?, fixture: Boolean,
                  accepted: Boolean, connected: Boolean, voiceInMenu: Boolean = false) = buildList {
        add(RouteMenuAction.DETAILS)
        if (activeId == null && arrivedId == null) {
            add(RouteMenuAction.EDIT)
            add(RouteMenuAction.RECALCULATE)
            if (voiceInMenu) add(RouteMenuAction.VOICE)
        }
        if (activeId != null && arrivedId == null && !fixture) {
            if (accepted) add(RouteMenuAction.OVERVIEW)
            add(RouteMenuAction.CANCEL)
        }
        if (!fixture && !connected && arrivedId == null && !accepted) add(RouteMenuAction.RECONNECT)
        add(RouteMenuAction.SETTINGS)
    }

    fun canOpenMovingOverview(activeId: String?, arrivedId: String?, accepted: Boolean,
                              vehicle: VehicleState, busy: Boolean) =
        !activeId.isNullOrBlank() && arrivedId == null && accepted && vehicle == VehicleState.PARKED && !busy

    fun canSendSampleArrival(demo: Boolean, fixture: Boolean, simulation: Boolean,
                             activeId: String?, arrivedId: String?, vehicle: VehicleState, busy: Boolean) =
        demo && fixture && simulation && !activeId.isNullOrBlank() && arrivedId == null && vehicle == VehicleState.PARKED && !busy
}

package ai.onmyway.car

/** Authored filming scenario. No provider or navigation SDK calls are made here. */
enum class FilmStage {
    REQUEST, CLARIFY, BASELINE, CHANGE, REVISED, GUIDANCE_LAUNDRY,
    ARRIVED_LAUNDRY, NEXT_MEDICINE, GUIDANCE_MEDICINE, ARRIVED_MEDICINE,
    HOME, GUIDANCE_HOME, FINISHED
}

class FilmJourney {
    var stage = FilmStage.REQUEST; private set
    val session = SessionState()
    fun next(vehicle: VehicleState): Boolean {
        if (vehicle != VehicleState.PARKED || stage == FilmStage.FINISHED) return false
        when(stage) {
            FilmStage.CHANGE -> session.edited()
            FilmStage.REVISED -> approve("laundry")
            FilmStage.GUIDANCE_LAUNDRY, FilmStage.GUIDANCE_MEDICINE, FilmStage.GUIDANCE_HOME -> session.arrived()
            FilmStage.ARRIVED_LAUNDRY -> session.complete("laundry")
            FilmStage.NEXT_MEDICINE -> approve("medicine")
            FilmStage.ARRIVED_MEDICINE -> session.complete("medicine")
            FilmStage.HOME -> {
                check(session.completed.containsAll(listOf("laundry", "medicine")))
                approve("home")
            }
            else -> Unit
        }
        stage = FilmStage.entries[stage.ordinal+1]
        return true
    }
    private fun approve(id: String) {
        session.approve(id)
        check(session.requestNavigation(id))
        session.routeAccepted()
    }
}

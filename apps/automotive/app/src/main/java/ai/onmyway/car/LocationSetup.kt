package ai.onmyway.car

import org.json.JSONObject

enum class LocationSource { DEVICE, PLACE, MANUAL, COMPLETED_STOP }
data class LocationAccess(val foreground: Boolean, val vehicle: VehicleState, val synthetic: Boolean, val guidance: Boolean, val busy: Boolean) {
    val canEdit: Boolean get() = foreground && vehicle == VehicleState.PARKED && !synthetic && !guidance && !busy
}
data class SetupLocation(val latitude: Double, val longitude: Double, val source: LocationSource, val label: String, val timestampMillis: Long = 0) {
    fun usable(nowMillis: Long, origin: Boolean): Boolean {
        if (!latitude.isFinite() || !longitude.isFinite() || latitude !in -90.0..90.0 || longitude !in -180.0..180.0 || label.isBlank()) return false
        return !origin || source != LocationSource.DEVICE || timestampMillis > 0 &&
            timestampMillis >= nowMillis - 60_000 && timestampMillis <= nowMillis + 5_000
    }
    fun toJson(): String = JSONObject().put("latitude", latitude).put("longitude", longitude)
        .put("source", source.name).put("label", label).put("timestampMillis", timestampMillis).toString()
    companion object {
        fun parse(value: String?): SetupLocation? = runCatching {
            val j = JSONObject(requireNotNull(value))
            SetupLocation(j.getDouble("latitude"), j.getDouble("longitude"), LocationSource.valueOf(j.getString("source")),
                j.getString("label"), j.getLong("timestampMillis")).takeIf { it.usable(0, false) }
        }.getOrNull()
    }
}

/** Only the current foreground request can consume one result; a gate loss is terminal. */
class LocationAttempt {
    private data class Pending(val token: Long, val revision: Long, val elapsed: Long)
    private var sequence = 0L
    private var pending: Pending? = null
    fun begin(revision: Long, elapsed: Long, access: LocationAccess): Long? {
        cancel()
        if (!access.canEdit) return null
        return (++sequence).also { pending = Pending(it, revision, elapsed) }
    }
    fun finish(token: Long, revision: Long, elapsed: Long, access: LocationAccess): Boolean {
        val current = pending ?: return false
        if (current.token != token) return false
        pending = null
        return current.revision == revision && elapsed - current.elapsed in 0..10_000 && access.canEdit
    }
    fun cancel() { pending = null }
    fun update(access: LocationAccess) { if (!access.canEdit) cancel() }
}
object LocationSetup {
    fun prepareForInterpretation(values: Map<String, String>, wasFixture: Boolean, newJourney: Boolean = false): Map<String, String> =
        when {
            wasFixture -> reset(values, true)
            newJourney && SetupLocation.parse(values["live-origin"])?.source == LocationSource.COMPLETED_STOP ->
                values.filterKeys { it !in setOf("live-origin", "draft-originLat", "draft-originLon") }
            else -> values
        }
    fun canSave(shownRevision: Long, revision: Long, access: LocationAccess, location: SetupLocation, nowMillis: Long): Boolean =
        shownRevision == revision && access.canEdit && location.usable(nowMillis, true)
    fun reset(values: Map<String, String>, wasFixture: Boolean): Map<String, String> {
        val reset = JourneyDrafts.withoutFixtureLocations(values, wasFixture).toMutableMap()
        reset.remove("live-origin"); reset.remove("draft-originLat"); reset.remove("draft-originLon")
        SetupLocation.parse(values["real-home"])?.let {
            reset["draft-homeLat"] = it.latitude.toString(); reset["draft-homeLon"] = it.longitude.toString()
        }
        return reset
    }
    fun originReady(metadata: String?, latitude: String, longitude: String, nowMillis: Long): Boolean {
        val lat = latitude.toDoubleOrNull() ?: return false
        val lon = longitude.toDoubleOrNull() ?: return false
        val selected = SetupLocation.parse(metadata) ?: return false
        return selected.latitude == lat && selected.longitude == lon && selected.usable(nowMillis, true)
    }
}

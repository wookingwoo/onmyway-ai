package ai.onmyway.car

enum class VehicleState { UNKNOWN, RESTRICTED, PARKED }

class SessionState {
    var revision = 0L; private set
    var arrivedId: String? = null; private set
    val completed = linkedSetOf<String>()
    var activeId: String? = null; private set
    var navigationError: String? = null; private set
    private var approval: Pair<Long, String>? = null
    private var accepted = false
    val guidanceAccepted: Boolean get() = accepted
    fun canInteract(state: VehicleState, demoBuild: Boolean, fixtureOverride: Boolean = false) =
        state == VehicleState.PARKED || (state == VehicleState.UNKNOWN && demoBuild && fixtureOverride)
    fun approve(id: String) { approval = revision to id }
    fun edited() { revision++; approval = null; navigationError = null }
    fun requestNavigation(id: String): Boolean {
        if (approval != revision to id || id in completed) return false
        approval = null; activeId = id; arrivedId = null; accepted = false; navigationError = null
        return true
    }
    fun routeAccepted() { if (activeId != null) accepted = true }
    fun arrived() { if (accepted) arrivedId = activeId }
    fun complete(id: String) {
        if (id != arrivedId) return
        completed.add(id); activeId = null; arrivedId = null; accepted = false; edited()
    }
    fun completeForRevision(id: String, displayedRevision: Long): Boolean {
        if (revision != displayedRevision || arrivedId != id || id in completed) return false
        complete(id)
        return id in completed
    }
    fun routeFailed(reason: String? = null) { activeId = null; arrivedId = null; accepted = false; approval = null; navigationError = reason }
    fun acceptsResponse(requestRevision: Long) = revision == requestRevision
}

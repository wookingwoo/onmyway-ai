package ai.onmyway.car

/** One request per SDK connection: SDK callbacks have no unique route/request identifier. */
class NavigationRequestState(private val clock: () -> Long) {
    private enum class Phase { IDLE, WAITING, ACCEPTED, ARRIVED }
    private var phase = Phase.IDLE
    private var generation = 0L
    private var started = 0L
    private var expectedName = ""
    private var matchedRoute = false
    private var completedRouting = false
    private var usedConnection = false
    var requiresReset = false; private set

    fun begin(name: String): Long? {
        if (name.isBlank() || usedConnection || requiresReset || phase != Phase.IDLE) return null
        generation++
        usedConnection = true
        expectedName = name
        started = clock()
        matchedRoute = false
        completedRouting = false
        phase = Phase.WAITING
        return generation
    }

    private fun waiting(token: Long) = token == generation && phase == Phase.WAITING
    private fun expired(): Boolean = runCatching { Math.subtractExact(clock(), started) !in 0 until ACCEPTANCE_TIMEOUT_MILLIS }.getOrDefault(true)
    private fun accept(): Boolean {
        if (!matchedRoute || !completedRouting) return false
        phase = Phase.ACCEPTED
        return true
    }
    fun routeStarted(token: Long, name: String): Boolean {
        if (!waiting(token) || expired()) return false
        matchedRoute = name == expectedName
        return accept()
    }
    fun routingCompleted(token: Long): Boolean {
        if (!waiting(token) || expired()) return false
        completedRouting = true
        return accept()
    }
    fun arrived(token: Long, name: String): Boolean {
        if (token != generation || phase != Phase.ACCEPTED || name != expectedName) return false
        phase = Phase.ARRIVED
        requiresReset = true
        return true
    }
    fun expire(token: Long): Boolean {
        if (!waiting(token) || !expired()) return false
        invalidate()
        return true
    }
    fun fail(token: Long): Boolean {
        if (token != generation || phase !in setOf(Phase.WAITING, Phase.ACCEPTED)) return false
        invalidate()
        return true
    }
    fun cancel(): Boolean {
        val active = phase in setOf(Phase.WAITING, Phase.ACCEPTED)
        invalidate()
        return active
    }
    /** Resource teardown cancels unresolved work, but does not revoke already accepted vehicle guidance. */
    fun teardown(): Boolean {
        val cancelUnresolvedRoute = phase == Phase.WAITING
        invalidate()
        return cancelUnresolvedRoute
    }
    private fun invalidate() {
        generation++
        phase = Phase.IDLE
        requiresReset = usedConnection
    }
    /** Call only after the prior SDK listener is removed and its connection released. */
    fun resetConnection() {
        generation++
        phase = Phase.IDLE
        usedConnection = false
        requiresReset = false
        matchedRoute = false
        completedRouting = false
    }
    companion object { const val ACCEPTANCE_TIMEOUT_MILLIS = 15_000L }
}

package ai.onmyway.car

import org.junit.Assert.*
import org.junit.Test

class NavigationRequestStateTest {
    private var now = 1000L
    private fun state() = NavigationRequestState { now }

    @Test fun acceptanceRequiresBothMatchedRouteAndRoutingCompletion() {
        val s = state(); val token = s.begin("약국")!!
        assertFalse(s.routeStarted(token, "다른 곳"))
        assertFalse(s.routingCompleted(token))
        assertFalse(s.arrived(token, "약국"))
        assertTrue(s.routeStarted(token, "약국"))
        assertFalse(s.routingCompleted(token))
        assertFalse(s.arrived(token, "다른 곳"))
        assertTrue(s.arrived(token, "약국"))
        assertFalse(s.arrived(token, "약국"))
    }
    @Test fun mismatchedRouteRevokesEarlierNameMatchBeforeAcceptance() {
        val s = state(); val token = s.begin("집")!!
        assertFalse(s.routeStarted(token, "집"))
        assertFalse(s.routeStarted(token, "다른 곳"))
        assertFalse(s.routingCompleted(token))
        assertFalse(s.arrived(token, "집"))
    }
    @Test fun routeStartBeforeRoutingCompletionAlsoWorks() {
        val s = state(); val token = s.begin("집")!!
        assertFalse(s.routeStarted(token, "집"))
        assertTrue(s.routingCompleted(token))
        now += 60_000
        assertFalse(s.expire(token))
        assertTrue(s.arrived(token, "집"))
    }
    @Test fun requestExpiresAtFifteenSecondsAndNeedsConnectionReset() {
        val s = state(); val token = s.begin("약국")!!
        now += 14_999; assertFalse(s.expire(token))
        now++; assertTrue(s.expire(token))
        assertTrue(s.requiresReset)
        assertFalse(s.routeStarted(token, "약국"))
        assertFalse(s.routingCompleted(token))
        assertFalse(s.arrived(token, "약국"))
        assertNull(s.begin("약국"))
    }
    @Test fun callbackAtTimeoutCannotWinDelayedTimeoutRunnable() {
        val s = state(); val token = s.begin("약국")!!
        assertFalse(s.routeStarted(token, "약국"))
        now += 15_000
        assertFalse(s.routingCompleted(token))
        assertTrue(s.expire(token))
    }
    @Test fun cancelledGenerationAndPreviousSessionCannotAffectRetry() {
        val s = state(); val old = s.begin("약국")!!
        assertTrue(s.cancel())
        assertNull(s.begin("약국"))
        s.resetConnection()
        val fresh = s.begin("약국")!!
        assertNotEquals(old, fresh)
        assertFalse(s.routeStarted(old, "약국"))
        assertFalse(s.routingCompleted(old))
        assertFalse(s.arrived(old, "약국"))
        assertFalse(s.routeStarted(fresh, "약국"))
        assertTrue(s.routingCompleted(fresh))
    }
    @Test fun activeRequestCannotBeOverwrittenAndFailureBlocksRetry() {
        val s = state(); val token = s.begin("약국")!!
        assertNull(s.begin("집"))
        assertTrue(s.fail(token))
        assertFalse(s.fail(token))
        assertNull(s.begin("집"))
    }
    @Test fun completedRequestAlsoRequiresFreshConnectionForNextRoute() {
        val s = state(); val token = s.begin("약국")!!
        s.routeStarted(token, "약국"); s.routingCompleted(token)
        assertTrue(s.arrived(token, "약국"))
        assertNull(s.begin("집"))
        s.resetConnection()
        assertNotNull(s.begin("집"))
    }
    @Test fun monotonicRollbackCannotExtendAcceptanceWait() {
        val s = state(); val token = s.begin("약국")!!
        now--
        assertFalse(s.routeStarted(token, "약국"))
        assertTrue(s.expire(token))
    }
}

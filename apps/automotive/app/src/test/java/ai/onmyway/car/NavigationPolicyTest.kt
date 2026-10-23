package ai.onmyway.car

import org.junit.Assert.*
import org.junit.Test

class NavigationPolicyTest {
    @Test fun restoredLivePlanRequiresRecalculation() {
        assertFalse(NavigationPolicy.canApprove(false, false, false, 1000, 2000, 100, null))
    }
    @Test fun expiredDeadlineCannotNavigateEvenWithFreshPlan() {
        assertFalse(NavigationPolicy.canApprove(false, false, false, 3000, 2000, 101, 100))
    }
    @Test fun freshnessMustHoldWhenUserConfirmsDelayedDialog() {
        assertTrue(NavigationPolicy.canApprove(false, false, false, 1000, 999999, 100, 100))
        assertFalse(NavigationPolicy.canApprove(false, false, false, 1000, 999999, 300101, 100))
    }
    @Test fun fixtureCanNeverRequestActualNavigationOrBypassLiveBuild() {
        assertFalse(NavigationPolicy.canApprove(true, true, false, 3000, 2000, 100, 100))
        assertFalse(NavigationPolicy.canApprove(true, false, true, 3000, 2000, 100, 100))
        assertTrue(NavigationPolicy.canApprove(true, true, true, 3000, 2000, 100, null))
    }
}

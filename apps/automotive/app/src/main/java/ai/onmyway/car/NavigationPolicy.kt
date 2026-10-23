package ai.onmyway.car

object NavigationPolicy {
    fun canApprove(fixture: Boolean, demoBuild: Boolean, simulation: Boolean, nowWallMillis: Long,
                   deadlineWallMillis: Long?, nowElapsedMillis: Long, computedElapsedMillis: Long?): Boolean {
        if (fixture) return demoBuild && simulation
        if (deadlineWallMillis == null || nowWallMillis > deadlineWallMillis || computedElapsedMillis == null) return false
        return nowElapsedMillis - computedElapsedMillis in 0..300_000
    }
}

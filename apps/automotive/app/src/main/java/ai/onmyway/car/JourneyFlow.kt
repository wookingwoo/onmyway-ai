package ai.onmyway.car

enum class JourneyScreen { INPUT, REVIEW, ROUTE, SETTINGS, LOCATIONS }

/** Presentation state only; SessionState remains authoritative for approvals. */
class JourneyFlow {
    var screen = JourneyScreen.INPUT; private set
    private var returnScreen = JourneyScreen.INPUT
    private var locationReturnScreen = JourneyScreen.INPUT
    fun restore(hasErrands: Boolean, hasPlan: Boolean) {
        screen = if (!hasErrands) JourneyScreen.INPUT else if (hasPlan) JourneyScreen.ROUTE else JourneyScreen.REVIEW
    }
    fun parsed() { screen = JourneyScreen.REVIEW }
    fun planned() { screen = JourneyScreen.ROUTE }
    fun edited() {
        if (screen == JourneyScreen.ROUTE) screen = JourneyScreen.REVIEW
        if (returnScreen == JourneyScreen.ROUTE) returnScreen = JourneyScreen.REVIEW
        if (locationReturnScreen == JourneyScreen.ROUTE) locationReturnScreen = JourneyScreen.REVIEW
    }
    fun settings() { returnScreen = screen; screen = JourneyScreen.SETTINGS }
    fun locations() { locationReturnScreen = screen; screen = JourneyScreen.LOCATIONS }
    fun back() {
        screen = when (screen) {
            JourneyScreen.SETTINGS -> returnScreen
            JourneyScreen.LOCATIONS -> locationReturnScreen
            JourneyScreen.ROUTE -> JourneyScreen.REVIEW
            else -> JourneyScreen.INPUT
        }
    }
    fun start() { screen = JourneyScreen.INPUT; returnScreen = JourneyScreen.INPUT }
}

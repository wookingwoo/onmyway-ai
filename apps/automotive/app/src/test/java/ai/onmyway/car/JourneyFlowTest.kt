package ai.onmyway.car

import org.junit.Assert.assertEquals
import org.junit.Test

class JourneyFlowTest {
    @Test fun aNavigationFailureRemainsVisibleAfterTheActiveRouteClears() {
        val state = SessionState()
        state.approve("medicine"); assertEquals(true, state.requestNavigation("medicine"))
        state.routeFailed("경로 탐색 실패")
        assertEquals(null, state.activeId)
        assertEquals("경로 탐색 실패", state.navigationError)
        state.approve("medicine"); assertEquals(true, state.requestNavigation("medicine"))
        assertEquals(null, state.navigationError)
    }
    @Test fun parsedRequirementsNeedReviewBeforeRoute() {
        val flow = JourneyFlow()
        flow.parsed()
        assertEquals(JourneyScreen.REVIEW, flow.screen)
        flow.planned()
        assertEquals(JourneyScreen.ROUTE, flow.screen)
    }
    @Test fun editingARouteReturnsToRequirementReview() {
        val flow = JourneyFlow()
        flow.parsed(); flow.planned(); flow.edited()
        assertEquals(JourneyScreen.REVIEW, flow.screen)
    }
    @Test fun settingsReturnToTheStageThatOpenedThem() {
        val flow = JourneyFlow()
        flow.parsed(); flow.planned(); flow.settings()
        assertEquals(JourneyScreen.SETTINGS, flow.screen)
        flow.back()
        assertEquals(JourneyScreen.ROUTE, flow.screen)
        flow.back()
        assertEquals(JourneyScreen.REVIEW, flow.screen)
        flow.settings(); flow.edited(); flow.back()
        assertEquals(JourneyScreen.REVIEW, flow.screen)
    }
    @Test fun restoredEmptyDataNeverOpensAnEmptyRoute() {
        val flow = JourneyFlow()
        flow.restore(false, true)
        assertEquals(JourneyScreen.INPUT, flow.screen)
        flow.restore(true, false)
        assertEquals(JourneyScreen.REVIEW, flow.screen)
        flow.restore(true, true)
        assertEquals(JourneyScreen.ROUTE, flow.screen)
        flow.start()
        assertEquals(JourneyScreen.INPUT, flow.screen)
    }
}

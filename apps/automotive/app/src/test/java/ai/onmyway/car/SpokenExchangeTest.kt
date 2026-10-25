package ai.onmyway.car

import org.junit.Assert.*
import org.junit.Test

class SpokenExchangeTest {
    @Test fun onlyConfirmedCurrentOutputCompletionCanStartOneInputTurn() {
        val e=SpokenExchange();val token=e.read(4,"laundry",true)
        assertTrue(e.done(token,4,"laundry",true))
        assertFalse(e.done(token,4,"laundry",true))
    }
    @Test fun stopBackgroundNewPlanAndNewDestinationInvalidateAutoListening() {
        val e=SpokenExchange();var t=e.read(4,"laundry",true);e.cancel();assertFalse(e.done(t,4,"laundry",true))
        t=e.read(4,"laundry",true);assertFalse(e.done(t,5,"laundry",true))
        t=e.read(4,"laundry",true);assertFalse(e.done(t,4,"medicine",true))
        t=e.read(4,"laundry",true);assertFalse(e.done(t,4,"laundry",false))
    }
    @Test fun replayDoesNotEnableTheMicrophoneWithoutAnActiveConversation() {
        val e=SpokenExchange();val t=e.read(4,"laundry",false)
        assertFalse(e.done(t,4,"laundry",true))
    }
    @Test fun interpretingRemainingSpeechKeepsCompletionButDropsOldRouteApproval() {
        val state=SessionState();state.approve("laundry");state.requestNavigation("laundry");state.routeAccepted();state.arrived();state.complete("laundry")
        state.approve("medicine")
        state.interpreted(newJourney=false)
        assertEquals(setOf("laundry"),state.completed)
        assertFalse(state.requestNavigation("medicine"))
        state.interpreted(newJourney=true)
        assertTrue(state.completed.isEmpty())
    }
}

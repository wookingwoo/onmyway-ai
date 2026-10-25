package ai.onmyway.car

import org.junit.Assert.*
import org.junit.Test

class SpokenCommandTest {
    @Test fun onlyWholeExplicitApprovalIsNavigationAuthority() {
        for (s in listOf("이 계획대로 출발해", "출발해!", "네, 이대로 출발해")) assertEquals(s, SpokenCommand.APPROVE, SpokenCommand.parse(s, false))
        for (s in listOf("네", "응", "출발하지 마", "출발해도 돼?", "출발해라고 말하면 되나", "출발해, 그런데 약국은 30분", "\"출발해\"", "조건이 맞으면 출발해")) assertNotEquals(s, SpokenCommand.APPROVE, SpokenCommand.parse(s, false))
        assertEquals(SpokenCommand.REFINE, SpokenCommand.parse("약국에서 30분 걸릴 것 같아", false))
    }
    @Test fun homeCommandDoesNotSkipTheCurrentRequiredStop() {
        assertEquals(SpokenCommand.CLARIFY, SpokenCommand.parse("집으로 출발해", false))
        assertEquals(SpokenCommand.APPROVE, SpokenCommand.parse("집으로 출발해", true))
    }
    @Test fun approvalTurnIsBoundToFreshRevisionDestinationParkAndConsumedOnce() {
        fun turn() = SpokenTurn(7, "laundry")
        assertEquals(SpokenCommand.STALE, turn().take("출발해",8,"laundry",true,true,true))
        assertEquals(SpokenCommand.STALE, turn().take("출발해",7,"pharmacy",true,true,true))
        assertEquals(SpokenCommand.STALE, turn().take("출발해",7,"laundry",false,true,true))
        assertEquals(SpokenCommand.STALE, turn().take("출발해",7,"laundry",true,false,true))
        assertEquals(SpokenCommand.STALE, turn().take("출발해",7,"laundry",true,true,false))
        val t=turn();assertEquals(SpokenCommand.APPROVE,t.take("출발해",7,"laundry",true,true,true))
        assertNull(t.take("출발해",7,"laundry",true,true,true))
    }
}

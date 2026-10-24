package ai.onmyway.car

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class IntentReviewTest {
    private fun response(deadline: String? = "2026-10-23T19:00:00+09:00", questions: List<String> = emptyList()) = JSONObject()
        .put("errands", JSONArray("""[{"id":"medicine","label":"약 구매","query":"약국","mandatory":true,"dwell_seconds":600}]"""))
        .put("deadline", deadline ?: JSONObject.NULL).put("clarification_questions", JSONArray(questions)).put("warnings", JSONArray())
    private fun review(r: JSONObject = response(), example: Boolean = false) = IntentReview("약국에 들러 줘", "2026-10-23T18:00:00+09:00", example, r)

    @Test fun missingDeadlineAndUnansweredQuestionBlockPlanning() {
        assertFalse(review(response(null)).ready)
        assertFalse(review(response(questions = listOf("도착인가요?"))).ready)
        assertTrue(review().ready)
    }
    @Test fun answerPayloadRetainsOriginalReferenceAndPreviousErrandsWithoutMutation() {
        val value = review(response(questions = listOf("도착인가요?")))
        val payload = value.answerPayload("도착인가요?", "완료예요", value.response.getJSONArray("errands"))
        assertEquals(value.originalText, payload.getString("text"))
        assertEquals(value.referenceTime, payload.getString("reference_time"))
        assertEquals("medicine", payload.getJSONArray("previous_errands").getJSONObject(0).getString("id"))
        assertEquals("완료예요", payload.getJSONArray("answers").getJSONObject(0).getString("answer"))
        assertFalse(value.ready)
        assertEquals(0, value.answers.length())
    }
    @Test fun examplesNeverProduceLiveClarificationPayloads() {
        assertThrows(IllegalArgumentException::class.java) { review(example = true).answerPayload("질문", "답변", JSONArray()) }
    }
    @Test fun explicitHomeTimeSurvivesRestoreButDoesNotResolveOtherQuestions() {
        val value = review(response(null, listOf("완료인가요?")))
        value.selectDeadline("2026-10-23T19:00:00+09:00")
        val restored = IntentReview.restore(value.toJson())
        assertEquals("2026-10-23T19:00:00+09:00", restored.deadline)
        assertFalse(restored.ready)
        assertEquals(1, restored.answers.length())
    }
    @Test fun blankOrUnaskedAnswerCannotBeSubmitted() {
        val value = review(response(questions = listOf("질문")))
        assertThrows(IllegalArgumentException::class.java) { value.answerPayload("질문", "  ", JSONArray()) }
        assertThrows(IllegalArgumentException::class.java) { value.answerPayload("다른 질문", "답", JSONArray()) }
    }
    @Test fun answeredDraftKeepsConfirmedHomeTimeWhenProviderOmitsIt() {
        val value = review(response(null, listOf("완료인가요?")))
        value.selectDeadline("2026-10-23T19:00:00+09:00")
        val payload = value.answerPayload("완료인가요?", "네", value.response.getJSONArray("errands"))
        val next = value.acceptAnswer(response(null), payload)
        assertEquals(value.deadline, next.deadline)
        assertTrue(next.ready)
        assertEquals(2, next.answers.length())
    }
    @Test fun unrelatedAnswerCannotRelaxAnExtractedHomeDeadline() {
        val value = review(response(questions = listOf("완료인가요?")))
        val payload = value.answerPayload("완료인가요?", "네", value.response.getJSONArray("errands"))
        assertEquals(value.deadline, payload.getString("previous_deadline"))
        val next = value.acceptAnswer(response("2026-10-23T20:00:00+09:00"), payload)
        assertEquals("2026-10-23T19:00:00+09:00", next.deadline)
    }
    @Test fun onePrimaryActionGuidesQuestionThenHomeTimeThenPlan() {
        val value = review(response(null, listOf("완료인가요?")))
        assertEquals(ReviewAction.ANSWER, value.action)
        value.response.put("clarification_questions", JSONArray())
        assertEquals(ReviewAction.HOME_TIME, value.action)
        value.selectDeadline("2026-10-23T19:00:00+09:00")
        assertEquals(ReviewAction.PLAN, value.action)
        value.selectDeadline("2026-10-23T19:15:00+09:00")
        assertEquals("2026-10-23T19:15:00+09:00", IntentReview.restore(value.toJson()).deadline)
    }
}

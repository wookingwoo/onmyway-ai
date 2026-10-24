package ai.onmyway.car

import org.json.JSONArray
import org.json.JSONObject
import java.time.OffsetDateTime

enum class ReviewAction { ANSWER, HOME_TIME, EDIT_REQUEST, PLAN }

/** Review context is a draft, never navigation authority. No network or Android dependency. */
class IntentReview(
    val originalText: String,
    val referenceTime: String,
    val example: Boolean,
    val response: JSONObject,
    val answers: JSONArray = JSONArray(),
    private var selectedDeadline: String? = null
) {
    val deadline: String? get() = selectedDeadline ?: response.optString("deadline").takeUnless { it.isBlank() || it == "null" }
    val questions: List<String> get() = response.optJSONArray("clarification_questions")?.let { array ->
        (0 until array.length()).map { array.optString(it).trim() }.filter { it.isNotBlank() }.distinct()
    }.orEmpty()
    val action: ReviewAction get() = when {
        questions.isNotEmpty() -> ReviewAction.ANSWER
        (response.optJSONArray("errands")?.length() ?: 0) == 0 -> ReviewAction.EDIT_REQUEST
        deadline == null -> ReviewAction.HOME_TIME
        else -> ReviewAction.PLAN
    }
    val ready: Boolean get() = action == ReviewAction.PLAN

    fun selectDeadline(value: String) {
        OffsetDateTime.parse(value)
        selectedDeadline = value
        response.put("deadline", value)
        putAnswer(answers, HOME_QUESTION, value)
    }

    fun answerPayload(question: String, answer: String, currentErrands: JSONArray): JSONObject {
        require(!example) { "예시 질문은 실제 AI 서버로 전송하지 않습니다." }
        require(question in questions && answer.isNotBlank() && answer.length <= 1000)
        val next = JSONArray(answers.toString())
        putAnswer(next, question, answer.trim())
        require(next.length() <= 10) { "확인 답변이 많아졌어요. 요청 문장을 정리해 다시 입력해 주세요." }
        // Candidate/provider data are deliberately excluded from extraction context.
        val previous = JSONArray()
        for (i in 0 until currentErrands.length()) {
            val old = currentErrands.getJSONObject(i)
            val item = JSONObject()
            for (key in listOf("id", "label", "query", "mandatory", "dwell_seconds", "latest_finish")) if (old.has(key)) item.put(key, old.get(key))
            previous.put(item)
        }
        return JSONObject().put("text", originalText).put("reference_time", referenceTime)
            .put("answers", next).put("previous_errands", previous).put("previous_deadline", deadline ?: JSONObject.NULL)
    }

    fun acceptAnswer(result: JSONObject, payload: JSONObject): IntentReview {
        val returned = result.optString("deadline").takeUnless { it.isBlank() || it == "null" }
        val previous = deadline
        val retained = selectedDeadline ?: previous?.takeIf { returned == null || OffsetDateTime.parse(returned).isAfter(OffsetDateTime.parse(it)) }
        if (retained != null) result.put("deadline", retained)
        return IntentReview(originalText, referenceTime, false, result, JSONArray(payload.getJSONArray("answers").toString()), selectedDeadline)
    }

    fun toJson(): String = JSONObject().put("original", originalText).put("reference", referenceTime)
        .put("example", example).put("response", response).put("answers", answers)
        .put("selected_deadline", selectedDeadline ?: JSONObject.NULL).toString()

    companion object {
        const val HOME_QUESTION = "집에 몇 시까지 도착해야 하나요?"
        const val EXAMPLE_TEXT = "약국은 꼭 들르고 세탁물도 찾아줘. 우유는 시간 되면 사고, 세탁소는 6시 반 전이야."
        const val EXAMPLE_QUESTION = "세탁소는 18시 30분 전에 도착해야 하나요, 수령을 마쳐야 하나요?"
        fun restore(value: String): IntentReview {
            val raw = JSONObject(value)
            return IntentReview(raw.getString("original"), raw.getString("reference"), raw.getBoolean("example"),
                raw.getJSONObject("response"), raw.getJSONArray("answers"), raw.optString("selected_deadline").takeUnless { it.isBlank() || it == "null" })
        }
        private fun putAnswer(array: JSONArray, question: String, answer: String) {
            val item = JSONObject().put("question", question).put("answer", answer)
            val index = (0 until array.length()).firstOrNull { array.getJSONObject(it).optString("question") == question }
            if (index == null) array.put(item) else array.put(index, item)
        }
    }
}

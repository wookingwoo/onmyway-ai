package ai.onmyway.car

import org.json.JSONArray
import org.json.JSONObject

/** Proposal automation never supplies navigation authority or venue evidence. */
object VoiceJourney {
    fun needsVenue(item: JSONObject) = item.optString("place_mode", "fixed") != "flexible" && !PlaceReview.allSelected(JSONArray().put(item), false)
    fun placesReady(items: JSONArray, fixture: Boolean) = fixture || (0 until items.length()).none { needsVenue(items.getJSONObject(it)) }

    fun retainPins(next: JSONArray, previous: JSONArray): JSONArray {
        val result = JSONArray(next.toString())
        for (i in 0 until result.length()) {
            val item = result.getJSONObject(i)
            val old = (0 until previous.length()).map { previous.getJSONObject(it) }.firstOrNull { it.optString("id") == item.optString("id") } ?: continue
            if (listOf("query", "label").all { item.optString(it) == old.optString(it) } && item.optString("place_mode", "fixed") == old.optString("place_mode", "fixed")) {
                old.optJSONObject("selected_place")?.let { item.put("selected_place", JSONObject(it.toString())) }
            }
        }
        return result
    }

    const val EXAMPLE_REQUEST = "약국은 꼭 들러서 10분 정도 걸릴 것 같아. 맡긴 세탁물은 한결세탁소에서 6시 반 전에 수령을 마쳐야 해. 우유는 시간 되면 사고, 7시까지 집에 가게 적절한 순서로 짜줘."
    const val EXAMPLE_REFINEMENT = "약국에서 상담도 해야 해서 30분은 걸릴 것 같아. 나머지 조건은 그대로 두고 다시 맞춰줘."
}

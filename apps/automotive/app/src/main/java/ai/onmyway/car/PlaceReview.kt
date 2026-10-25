package ai.onmyway.car

import org.json.JSONArray
import org.json.JSONObject
import java.time.OffsetDateTime

/** User-selected venue identity is separate from a search query or AI interpretation. */
object PlaceReview {
    private val inputKeys = listOf("id", "label", "query", "place_mode", "mandatory", "dwell_seconds", "latest_finish", "selected_place")

    fun pin(errand: JSONObject, place: JSONObject, allowFixture: Boolean): JSONObject {
        require(place.optString("id").isNotBlank() && place.optString("name").isNotBlank()) { "방문할 장소를 선택해 주세요." }
        require(place.getDouble("latitude").isFinite() && place.getDouble("latitude") in -90.0..90.0 &&
            place.getDouble("longitude").isFinite() && place.getDouble("longitude") in -180.0..180.0) { "장소 좌표를 확인해 주세요." }
        require(allowFixture || !place.getJSONObject("source").optBoolean("is_fixture")) { "예시 장소는 실제 일정에 사용할 수 없습니다." }
        return JSONObject(errand.toString()).put("selected_place", JSONObject(place.toString()))
    }

    fun withQuery(errand: JSONObject, query: String): JSONObject {
        require(query.isNotBlank() && query.length <= 200) { "찾을 장소를 입력해 주세요." }
        return JSONObject(errand.toString()).apply {
            if (optString("query") != query.trim()) { remove("selected_place"); remove("candidates") }
            put("query", query.trim())
        }
    }

    fun withLabel(errand: JSONObject, label: String): JSONObject {
        require(label.isNotBlank() && label.length <= 200) { "할 일을 200자 이내로 입력해 주세요." }
        return JSONObject(errand.toString()).apply {
            if (optString("label") != label.trim()) optJSONObject("selected_place")?.apply {
                remove("availability_evidence")
                put("hours_status", "unknown").put("service_status", "unknown").put("opening_intervals", JSONArray())
            }
            put("label", label.trim())
        }
    }

    fun confirmAvailability(errand: JSONObject, start: String, end: String, basis: String,
                            confirmedAt: String, serviceConfirmed: Boolean): JSONObject {
        require(basis.isNotBlank() && basis.trim().length <= 500) { "직접 확인한 근거를 500자 이내로 입력해 주세요." }
        require(OffsetDateTime.parse(end).isAfter(OffsetDateTime.parse(start))) { "이용 종료는 시작보다 늦어야 합니다." }
        OffsetDateTime.parse(confirmedAt)
        val result = JSONObject(errand.toString())
        val place = result.getJSONObject("selected_place")
        place.put("opening_intervals", JSONArray().put(JSONObject().put("start", start).put("end", end)))
            .put("hours_status", "verified").put("service_status", if (serviceConfirmed) "verified" else "unknown")
            .put("availability_evidence", JSONObject().put("basis", basis.trim()).put("confirmed_at", confirmedAt))
        return result
    }

    fun discoveryErrands(errands: JSONArray, completed: Set<String>): JSONArray {
        val result = JSONArray()
        for (i in 0 until errands.length()) {
            val old = errands.getJSONObject(i)
            if (old.optString("id") in completed) continue
            val item = JSONObject()
            for (key in inputKeys) if (old.has(key)) item.put(key, old.get(key))
            result.put(JSONObject(item.toString()))
        }
        return result
    }

    fun fixtureErrands(errands: JSONArray, completed: Set<String>): JSONArray {
        val result = JSONArray()
        for (i in 0 until errands.length()) {
            val item = JSONObject(errands.getJSONObject(i).toString())
            if (item.optString("id") in completed) continue
            item.optJSONObject("selected_place")?.let { selected ->
                require(selected.getJSONObject("source").optBoolean("is_fixture")) { "예시 경로에는 예시 장소만 사용할 수 있습니다." }
                item.put("candidates", JSONArray().put(JSONObject(selected.toString())))
            }
            result.put(item)
        }
        return result
    }

    fun allSelected(errands: JSONArray, allowFixture: Boolean): Boolean = (0 until errands.length()).all {
        val item = errands.getJSONObject(it)
        val place = item.optJSONObject("selected_place")
        place != null && runCatching { pin(item, place, allowFixture) }.isSuccess
    }
}

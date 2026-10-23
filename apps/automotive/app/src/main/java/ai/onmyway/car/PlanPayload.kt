package ai.onmyway.car

import org.json.JSONObject
import org.json.JSONArray

object PlanPayload {
    fun trimFixtureEdges(payload: JSONObject): JSONObject {
        val result = JSONObject(payload.toString())
        val knownIds = mutableSetOf(result.getJSONObject("origin").getString("id"), result.getJSONObject("home").getString("id"))
        val errands = result.getJSONArray("errands")
        for (i in 0 until errands.length()) {
            val candidates = errands.getJSONObject(i).optJSONArray("candidates") ?: continue
            for (j in 0 until candidates.length()) knownIds += candidates.getJSONObject(j).getString("id")
        }
        val filtered = JSONArray()
        val edges = result.getJSONArray("travel_edges")
        for (i in 0 until edges.length()) {
            val edge = edges.getJSONObject(i)
            if (edge.getString("from_id") in knownIds && edge.getString("to_id") in knownIds) filtered.put(edge)
        }
        return result.put("travel_edges", filtered)
    }
}

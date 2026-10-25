package ai.onmyway.car

import org.json.JSONObject
import java.time.OffsetDateTime

/** Time bounds only: unknown availability remains a separate, explicitly warned decision. */
object PlanTiming {
    fun canApprove(request: JSONObject?, result: JSONObject?, nowWallMillis: Long,
                   nowElapsedMillis: Long, startedWallMillis: Long?, startedElapsedMillis: Long?): Boolean =
        runCatching {
            val margin = remainingMarginMillis(request, result) ?: return false
            val wallStart = startedWallMillis ?: return false
            val elapsedStart = startedElapsedMillis ?: return false
            require(nowElapsedMillis >= 0 && elapsedStart >= 0)
            val elapsed = Math.subtractExact(nowElapsedMillis, elapsedStart)
            require(elapsed in 0..300_000)
            // A forward wall-clock jump is conservative; a backward jump cannot refund elapsed time.
            require(Math.subtractExact(nowWallMillis, wallStart) <= 300_000)
            val departure = instant(requireNotNull(request), "departure_time")
            val scheduledOffset = Math.subtractExact(departure, wallStart)
            val delay = maxOf(0L, Math.subtractExact(nowWallMillis, departure),
                Math.subtractExact(elapsed, scheduledOffset))
            margin >= delay
        }.getOrDefault(false)

    fun remainingMarginMillis(request: JSONObject?, result: JSONObject?): Long? = runCatching {
        val q = requireNotNull(request)
        val r = requireNotNull(result)
        require(r.getString("status") in setOf("feasible", "needs_verification"))
        val departure = instant(q, "departure_time")
        val deadline = instant(q, "deadline")
        require(deadline >= departure)
        val homeArrival = instant(r, "home_arrival_time")
        var margin = Math.subtractExact(deadline, homeArrival)
        val errands = q.getJSONArray("errands")
        val byId = mutableMapOf<String, JSONObject>()
        for (i in 0 until errands.length()) {
            val errand = errands.getJSONObject(i)
            val id = errand.getString("id")
            require(!errand.has("mandatory") || errand.get("mandatory") is Boolean)
            require(id.isNotBlank() && byId.put(id, errand) == null)
        }
        val seen = mutableSetOf<String>()
        val stops = r.getJSONArray("stops")
        var precedingEnd = departure
        for (i in 0 until stops.length()) {
            val stop = stops.getJSONObject(i)
            val id = stop.getString("errand_id")
            require(seen.add(id))
            val errand = requireNotNull(byId[id])
            val arrival = instant(stop, "arrival_time")
            val start = instant(stop, "start_time")
            val end = instant(stop, "end_time")
            require(arrival >= precedingEnd && start >= arrival && end >= start)
            val dwell = errand.get("dwell_seconds")
            require(dwell is Number && dwell.toDouble() == dwell.toLong().toDouble() && dwell.toLong() in 0..86400)
            require(Math.subtractExact(end, start) >= Math.multiplyExact(dwell.toLong(), 1000L))
            if (errand.has("latest_finish") && !errand.isNull("latest_finish")) {
                margin = minOf(margin, Math.subtractExact(instant(errand, "latest_finish"), end))
            }
            val place = stop.getJSONObject("place")
            val hours = place.getString("hours_status")
            require(hours in setOf("verified", "unknown"))
            val intervals = if (place.has("opening_intervals")) place.getJSONArray("opening_intervals") else null
            // Known windows still constrain timing while their provenance remains unverified.
            if (hours == "verified" || (intervals != null && intervals.length() > 0)) {
                val windows = requireNotNull(intervals)
                var closingMargin: Long? = null
                for (j in 0 until windows.length()) {
                    val interval = windows.getJSONObject(j)
                    val opens = instant(interval, "start")
                    val closes = instant(interval, "end")
                    require(closes > opens)
                    if (opens <= start && closes >= end) {
                        val slack = Math.subtractExact(closes, end)
                        closingMargin = closingMargin?.let { minOf(it, slack) } ?: slack
                    }
                }
                margin = minOf(margin, requireNotNull(closingMargin))
            }
            precedingEnd = end
        }
        require(homeArrival >= precedingEnd)
        for ((id, errand) in byId) {
            if (errand.optBoolean("mandatory", true)) require(id in seen)
        }
        margin
    }.getOrNull()

    private fun instant(value: JSONObject, field: String): Long =
        OffsetDateTime.parse(value.getString(field)).toInstant().toEpochMilli()
}

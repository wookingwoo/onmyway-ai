package ai.onmyway.car

import java.time.LocalDate
import java.time.OffsetDateTime

/** Shared draft transforms used by the native editors and session reset. */
object JourneyDrafts {
    fun adjustDwell(seconds: Int, deltaMinutes: Int): Int = (seconds + deltaMinutes * 60).coerceIn(0, 86400)
    fun deadline(value: String, date: LocalDate, hour: Int, minute: Int): String =
        OffsetDateTime.parse(value).with(date).withHour(hour).withMinute(minute).withSecond(0).withNano(0).toString()

    fun withoutFixtureLocations(values: Map<String, String>, wasFixture: Boolean): Map<String, String> {
        if (!wasFixture) return values
        val fixtureKeys = setOf("draft-originLat", "draft-originLon", "draft-homeLat", "draft-homeLon", "draft-deadline")
        return values.filterKeys { it !in fixtureKeys }
    }
}

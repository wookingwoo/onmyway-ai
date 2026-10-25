package ai.onmyway.car

/** Half duplex: only the completed output of the same parked plan can open input. */
class SpokenExchange {
    private var generation = 0L
    private var reading = false
    private var revision = -1L
    private var destination = ""
    private var listen = false
    val token: Long get() = generation
    fun isCurrent(token: Long) = token == generation
    fun read(revision: Long, destination: String, listen: Boolean): Long {
        cancel(); this.revision=revision; this.destination=destination; this.listen=listen; reading=true
        return generation
    }
    fun done(token: Long, revision: Long, destination: String, parked: Boolean): Boolean {
        if (token != generation || !reading) return false
        reading=false
        return listen && parked && this.revision==revision && this.destination==destination
    }
    fun cancel() { generation++; reading=false; listen=false }
}

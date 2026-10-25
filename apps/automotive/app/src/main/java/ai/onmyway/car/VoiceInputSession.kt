package ai.onmyway.car

enum class VoiceSource { PLEOS, EXAMPLE }
enum class VoicePhase { IDLE, CONNECTING, LISTENING, REVIEW, ERROR }
data class VoiceDraft(val text: String, val source: VoiceSource)

/** Session boundary shared by the native dialog and the SDK callbacks. */
class VoiceInputSession {
    var phase = VoicePhase.IDLE; private set
    var transcript = ""; private set
    var error: String? = null; private set
    var source = VoiceSource.PLEOS; private set
    private var generation = 0L
    private fun permitted(source: VoiceSource, vehicle: VehicleState, demoBuild: Boolean) =
        if (source == VoiceSource.PLEOS) vehicle == VehicleState.PARKED
        else demoBuild && vehicle != VehicleState.RESTRICTED
    fun start(source: VoiceSource, vehicle: VehicleState, demoBuild: Boolean): Long? {
        cancel()
        if (!permitted(source, vehicle, demoBuild)) return null
        this.source = source
        phase = VoicePhase.CONNECTING
        return generation
    }
    private fun receiving(token: Long) = token == generation &&
        phase in setOf(VoicePhase.CONNECTING, VoicePhase.LISTENING)
    fun update(token: Long, text: String, completed: Boolean) {
        if (!receiving(token)) return
        transcript = text.trim()
        if (completed) {
            if (transcript.isBlank()) fail(token, "말씀을 인식하지 못했어요. 다시 말해 주세요.")
            else phase = VoicePhase.REVIEW
        }
    }
    fun fail(token: Long, message: String) {
        if (!receiving(token)) return
        error = message
        phase = VoicePhase.ERROR
    }
    fun ended(token: Long) {
        if (receiving(token)) fail(token, "인식이 끝났지만 최종 결과가 없어요. 다시 말해 주세요.")
    }
    fun listening(token: Long) { if (receiving(token)) phase = VoicePhase.LISTENING }
    fun confirm(token: Long, vehicle: VehicleState, demoBuild: Boolean): VoiceDraft? {
        if (token != generation) return null
        if (!permitted(source, vehicle, demoBuild)) { cancel(); return null }
        if (phase != VoicePhase.REVIEW) return null
        val result = VoiceDraft(transcript, source)
        cancel()
        return result
    }
    fun cancel() {
        generation++
        phase = VoicePhase.IDLE
        transcript = ""
        error = null
    }
    companion object {
        fun isExampleDraft(text: String, marked: Boolean, demoBuild: Boolean) =
            marked && demoBuild && text == EXAMPLE_TEXT
        const val EXAMPLE_TEXT = VoiceJourney.EXAMPLE_REQUEST
    }
}

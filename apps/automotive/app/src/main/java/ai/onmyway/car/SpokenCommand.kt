package ai.onmyway.car

import java.text.Normalizer

/** No model or substring match can turn an errand refinement into navigation authority. */
enum class SpokenCommand { APPROVE, REFINE, CLARIFY, CANCEL, STALE;
    companion object {
        fun parse(text: String, home: Boolean): SpokenCommand {
            val s = Normalizer.normalize(text, Normalizer.Form.NFKC).trim().replace(Regex("[\\s,.!…]"), "")
            if (s in setOf("취소", "그만", "멈춰", "출발하지마", "출발안할게")) return CANCEL
            if (s in setOf("집으로출발해", "집으로안내해줘", "집으로출발할게")) return if (home) APPROVE else CLARIFY
            if (s in setOf("출발해", "출발하자", "이계획대로출발해", "이계획대로출발할게", "이대로출발해", "그대로출발해", "네이대로출발해", "네이계획대로출발해")) return APPROVE
            if (s.isBlank() || s in setOf("네", "응", "좋아", "알겠어") || s.contains("출발") || s.contains("?") || s.startsWith("\"")) return CLARIFY
            return REFINE
        }
    }
}

/** One final transcript, one current displayed plan and one next destination. */
class SpokenTurn(private val revision: Long, private val destinationId: String) {
    private var consumed = false
    fun take(text: String, currentRevision: Long, currentDestination: String, parked: Boolean, fresh: Boolean, eligible: Boolean): SpokenCommand? {
        if (consumed) return null
        consumed = true
        if (!parked || revision != currentRevision || destinationId != currentDestination) return SpokenCommand.STALE
        val command = SpokenCommand.parse(text, destinationId == "__home")
        if (command == SpokenCommand.APPROVE && (!fresh || !eligible)) return SpokenCommand.STALE
        return command
    }
}

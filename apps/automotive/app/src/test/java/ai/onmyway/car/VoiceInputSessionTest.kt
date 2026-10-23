package ai.onmyway.car

import org.junit.Assert.*
import org.junit.Test

class VoiceInputSessionTest {
    @Test fun microphoneRequiresActualParkEvenWhenDemoCanOtherwiseInteract() {
        val voice = VoiceInputSession()
        assertNull(voice.start(VoiceSource.PLEOS, VehicleState.UNKNOWN, true))
        assertNull(voice.start(VoiceSource.PLEOS, VehicleState.RESTRICTED, true))
        assertNotNull(voice.start(VoiceSource.PLEOS, VehicleState.PARKED, false))
    }

    @Test fun partialResultsCannotReplaceTheDraftUntilAFinalResultIsConfirmed() {
        val voice = VoiceInputSession()
        val token = voice.start(VoiceSource.PLEOS, VehicleState.PARKED, false)!!
        voice.update(token, "약국", false)
        assertEquals("약국", voice.transcript)
        assertNull(voice.confirm(token, VehicleState.PARKED, false))
        voice.update(token, "약국 들르고 일곱 시까지 집에 가야 해", true)
        val result = voice.confirm(token, VehicleState.PARKED, false)!!
        assertEquals(VoiceSource.PLEOS, result.source)
        assertEquals("약국 들르고 일곱 시까지 집에 가야 해", result.text)
        assertNull(voice.confirm(token, VehicleState.PARKED, false))
    }

    @Test fun cancelAndRestartRejectOldResultsAndErrors() {
        val voice = VoiceInputSession()
        val old = voice.start(VoiceSource.PLEOS, VehicleState.PARKED, false)!!
        voice.cancel()
        val current = voice.start(VoiceSource.PLEOS, VehicleState.PARKED, false)!!
        voice.update(old, "오래된 결과", true)
        voice.fail(old, "오래된 오류")
        assertEquals("", voice.transcript)
        assertNull(voice.error)
        voice.update(current, "새 결과", true)
        assertEquals("새 결과", voice.confirm(current, VehicleState.PARKED, false)?.text)
    }

    @Test fun finalResultIsFrozenAndEmptySpeechIsAnError() {
        val voice = VoiceInputSession()
        val token = voice.start(VoiceSource.PLEOS, VehicleState.PARKED, false)!!
        voice.update(token, "세탁물 찾기", true)
        voice.update(token, "중복 이벤트", true)
        voice.fail(token, "뒤늦은 오류")
        assertEquals("세탁물 찾기", voice.transcript)
        assertNull(voice.error)
        voice.cancel()
        val blank = voice.start(VoiceSource.PLEOS, VehicleState.PARKED, false)!!
        voice.update(blank, "   ", true)
        assertEquals(VoicePhase.ERROR, voice.phase)
        assertNull(voice.confirm(blank, VehicleState.PARKED, false))
    }

    @Test fun actualRestrictionInvalidatesEvenAFinishedTranscript() {
        val voice = VoiceInputSession()
        val token = voice.start(VoiceSource.PLEOS, VehicleState.PARKED, false)!!
        voice.update(token, "약국", true)
        assertNull(voice.confirm(token, VehicleState.RESTRICTED, false))
        assertNull(voice.confirm(token, VehicleState.PARKED, false))
    }

    @Test fun examplesAreExplicitDemoOnlyAndNeverBecomeLiveSpeech() {
        val voice = VoiceInputSession()
        assertNull(voice.start(VoiceSource.EXAMPLE, VehicleState.PARKED, false))
        val token = voice.start(VoiceSource.EXAMPLE, VehicleState.PARKED, true)!!
        voice.update(token, VoiceInputSession.EXAMPLE_TEXT, true)
        val result = voice.confirm(token, VehicleState.PARKED, true)!!
        assertEquals(VoiceSource.EXAMPLE, result.source)
        assertEquals(VoiceInputSession.EXAMPLE_TEXT, result.text)
    }

    @Test fun endedWithoutFinalDoesNotPromotePartialSpeech() {
        val voice = VoiceInputSession()
        val token = voice.start(VoiceSource.PLEOS, VehicleState.PARKED, false)!!
        voice.update(token, "일곱", false)
        voice.ended(token)
        assertEquals(VoicePhase.ERROR, voice.phase)
        assertNull(voice.confirm(token, VehicleState.PARKED, false))
    }
}

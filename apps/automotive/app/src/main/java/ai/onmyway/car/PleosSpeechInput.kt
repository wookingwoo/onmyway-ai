package ai.onmyway.car

import android.content.Context
import android.os.Handler
import android.os.Looper
import ai.pleos.playground.stt.SpeechToText
import ai.pleos.playground.stt.constant.ErrorType
import ai.pleos.playground.stt.constant.Mode
import ai.pleos.playground.stt.listener.OnInitializeListener
import ai.pleos.playground.stt.listener.ResultListener
import java.util.Locale

sealed class SpeechInputEvent {
    object Connected : SpeechInputEvent()
    object Listening : SpeechInputEvent()
    data class Transcript(val text: String, val completed: Boolean) : SpeechInputEvent()
    data class Failure(val message: String) : SpeechInputEvent()
}

/** One SDK instance per attempt; callbacks and watchdogs are confined to the UI thread. */
class PleosSpeechInput(private val context: Context, private val event: (SpeechInputEvent) -> Unit) {
    private val handler = Handler(Looper.getMainLooper())
    private val cleanupHandler = Handler(Looper.getMainLooper())
    private class Attempt(val sdk: SpeechToText) {
        var listener: ResultListener? = null
        var initialized = false
        var closing = false
        var released = false
        var cleanup: Runnable? = null
    }
    private var current: Attempt? = null
    private val retiring = mutableSetOf<Attempt>()
    private var pendingStart = false

    fun start() {
        close()
        pendingStart = true
        connectPending()
    }
    private fun connectPending() {
        if (!pendingStart || retiring.isNotEmpty()) return
        pendingStart = false
        try {
            val attempt = Attempt(SpeechToText(context.applicationContext, Mode.ON_DEVICE))
            current = attempt
            handler.postDelayed({ if (current === attempt) fail(attempt, "음성 서비스 연결 시간이 초과됐어요. 다시 시도하거나 직접 입력해 주세요.") }, 8_000)
            attempt.sdk.initialize(object : OnInitializeListener {
                override fun onSuccess() = dispatch(attempt) {
                    attempt.initialized = true
                    handler.removeCallbacksAndMessages(null)
                    val listener = object : ResultListener {
                        override fun onReady() = dispatch(attempt) { event(SpeechInputEvent.Listening) }
                        override fun onStartedRecognition() = dispatch(attempt) { event(SpeechInputEvent.Listening) }
                        override fun onUpdated(stt: String, completed: Boolean) = dispatch(attempt) {
                            event(SpeechInputEvent.Transcript(stt, completed))
                            if (completed) close()
                        }
                        override fun onEndedRecognition() {
                            cleanupHandler.post {
                                if (attempt.closing) release(attempt)
                                else dispatch(attempt) {
                                    // Allow an already queued final callback before reporting an empty result.
                                    handler.postDelayed({ if (current === attempt) fail(attempt, "최종 인식 결과를 받지 못했어요. 다시 말하거나 직접 입력해 주세요.") }, 350)
                                }
                            }
                        }
                        override fun onError(errorType: ErrorType) = dispatch(attempt) { fail(attempt, errorMessage(errorType)) }
                        override fun onUpdatedEpdData(on: Long, off: Long) = Unit
                        override fun onUpdatedRms(rms: Double) = Unit
                    }
                    attempt.listener = listener
                    attempt.sdk.addListener(listener)
                    attempt.sdk.setLocale(Locale.KOREA)
                    event(SpeechInputEvent.Connected)
                    handler.postDelayed({ if (current === attempt) fail(attempt, "음성 입력 시간이 초과됐어요. 짧게 다시 말해 주세요.") }, 30_000)
                    attempt.sdk.request()
                }
                override fun onFailure(errorType: ErrorType) = dispatch(attempt) { fail(attempt, errorMessage(errorType)) }
            })
        } catch (_: Exception) {
            close()
            event(SpeechInputEvent.Failure("음성 서비스를 시작하지 못했어요. Pleos 지원 환경을 확인하거나 직접 입력해 주세요."))
        }
    }

    private fun dispatch(attempt: Attempt, action: () -> Unit) {
        handler.post {
            if (current !== attempt) return@post
            try { action() } catch (_: Exception) {
                fail(attempt, "음성 서비스 응답을 처리하지 못했어요. 다시 시도하거나 직접 입력해 주세요.")
            }
        }
    }
    private fun fail(attempt: Attempt, message: String) {
        if (current !== attempt) return
        close()
        event(SpeechInputEvent.Failure(message))
    }
    fun close() {
        pendingStart = false
        val attempt = current
        current = null
        handler.removeCallbacksAndMessages(null)
        if (attempt == null) return
        if (!attempt.initialized) { release(attempt); return }
        attempt.closing = true
        retiring += attempt
        // SDK stop() launches a coroutine. Releasing in the same call cancels
        // that coroutine before its remote close(), leaving the vehicle mic on.
        // Keep the binding/listener until the ended callback or a bounded grace.
        attempt.cleanup = Runnable { release(attempt) }.also { cleanupHandler.postDelayed(it, 2_000) }
        runCatching { attempt.sdk.stop() }
    }
    private fun release(attempt: Attempt) {
        if (attempt.released) return
        attempt.released = true
        attempt.cleanup?.let { cleanupHandler.removeCallbacks(it) }
        attempt.listener?.let { runCatching { attempt.sdk.removeListener(it) } }
        runCatching { attempt.sdk.release() }
        retiring -= attempt
        // A retry cannot race the preceding request's remote stop.
        connectPending()
    }
    private fun errorMessage(error: ErrorType): String = when (error) {
        ErrorType.ON_DEVICE_ENGINE_ERROR, ErrorType.NOT_SUPPORTED_API -> "이 환경에서 온디바이스 음성 인식을 사용할 수 없어요. 직접 입력하거나 지원 환경에서 다시 시도해 주세요."
        ErrorType.UNAVAILABLE_LANGUAGE -> "한국어 음성 인식을 사용할 수 없어요. 언어 지원을 확인하거나 직접 입력해 주세요."
        ErrorType.PERMISSION_DENIED -> "Pleos 음성 서비스 접근 권한이 없어요. 권한을 확인하거나 직접 입력해 주세요."
        ErrorType.UNAUTHENTICATED -> "음성 서비스 인증이 필요해요. 현재 환경의 SDK 지원 상태를 확인해 주세요."
        ErrorType.MIC_ERROR -> "마이크를 사용할 수 없어요. 마이크 연결 상태를 확인하고 다시 시도해 주세요."
        ErrorType.TIMEOUT -> "음성 인식 시간이 초과됐어요. 짧게 다시 말해 주세요."
        else -> "음성 인식을 마치지 못했어요. 다시 시도하거나 직접 입력해 주세요."
    } + " (${error.name})"
}

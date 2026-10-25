package ai.onmyway.car

import android.content.Context
import android.media.AudioAttributes
import android.os.Handler
import android.os.Looper
import android.util.Log
import ai.pleos.playground.tts.TextToSpeech
import ai.pleos.playground.tts.constant.Mode
import ai.pleos.playground.tts.constant.ErrorType
import ai.pleos.playground.tts.listener.EventListener
import ai.pleos.playground.tts.listener.OnInitializeListener
import java.util.Locale

sealed class SpeechOutputEvent {
    object Preparing : SpeechOutputEvent()
    object Started : SpeechOutputEvent()
    object Done : SpeechOutputEvent()
    object Stopped : SpeechOutputEvent()
    data class Failure(val message: String) : SpeechOutputEvent()
}

/** Explicit ON_DEVICE only. Never registers cloud credentials or changes providers. */
class PleosSpeechOutput(private val context: Context) {
    private val handler=Handler(Looper.getMainLooper())
    private class Attempt(val sdk: TextToSpeech, val event: (SpeechOutputEvent)->Unit) {
        var listener: EventListener?=null
        var initialized=false
        var ending=false
        var stopReply: ((Boolean)->Unit)?=null
        var connectionTimeout: Runnable?=null
        var playbackTimeout: Runnable?=null
    }
    private var current: Attempt?=null
    private val retiring=mutableSetOf<Attempt>()
    private var generation=0L
    fun speak(text: String, event: (SpeechOutputEvent)->Unit) {
        val token=++generation
        stopCurrent { clean ->
            if (token!=generation) return@stopCurrent
            if (!clean) { event(SpeechOutputEvent.Failure("앞선 음성 안내의 종료를 확인하지 못했어요. 잠시 뒤 다시 들어 주세요.")); return@stopCurrent }
            begin(text,event)
        }
    }
    private fun begin(text: String, event: (SpeechOutputEvent)->Unit) {
        try {
            val a=Attempt(TextToSpeech(context.applicationContext,Mode.ON_DEVICE),event);current=a
            event(SpeechOutputEvent.Preparing)
            a.connectionTimeout=Runnable { if (current===a) fail(a,"음성 안내 연결 시간이 초과됐어요.") }.also { handler.postDelayed(it,8000) }
            a.sdk.initialize(object: OnInitializeListener {
                override fun onSuccess() = dispatch(a) {
                    if (a.initialized || a.ending) return@dispatch
                    a.initialized=true
                    a.connectionTimeout?.let { handler.removeCallbacks(it) }
                    a.sdk.setLocale(Locale.KOREA)
                    a.sdk.setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
                    val listener=object: EventListener {
                        override fun onReady()=Unit
                        override fun onUpdatedRms(rms: Double)=Unit
                        override fun onStart()=dispatch(a) { if(!a.ending) { Log.i("OnMyWayVoice","tts-start");event(SpeechOutputEvent.Started) } }
                        override fun onDone()=dispatch(a) { finish(a,true) }
                        override fun onStop()=dispatch(a) { finish(a,false) }
                        override fun onError(errorType: ErrorType)=dispatch(a) { fail(a,"한국어 음성 안내를 재생하지 못했어요. (${errorType.name})") }
                    }
                    a.listener=listener;a.sdk.addEventListener(listener)
                    a.playbackTimeout=Runnable { if (current===a && !a.ending) fail(a,"음성 안내 시간이 초과됐어요. 화면에서 계획을 확인해 주세요.") }.also { handler.postDelayed(it,120000) }
                    a.sdk.speak(text,true)
                }
                override fun onFailure(errorType: ErrorType)=dispatch(a) { fail(a,"이 환경에서 음성 안내를 시작하지 못했어요. (${errorType.name})") }
            })
        } catch (_: Exception) { current?.let { fail(it,"음성 안내를 시작하지 못했어요. 화면에서 확인하거나 다시 들어 주세요.") } ?: event(SpeechOutputEvent.Failure("음성 서비스를 사용할 수 없어요.")) }
    }
    private fun dispatch(a: Attempt, action: ()->Unit) {
        handler.post { if (current===a || a in retiring) runCatching(action).onFailure { fail(a,"음성 안내 응답을 처리하지 못했어요.") } }
    }
    private fun finish(a: Attempt, done: Boolean) {
        val wasStopping=a.ending
        if (current!==a && a !in retiring) return
        if (current===a) current=null
        release(a)
        if (wasStopping) { a.stopReply?.invoke(true);a.stopReply=null }
        else { Log.i("OnMyWayVoice",if(done) "tts-done" else "tts-stopped");a.event(if(done) SpeechOutputEvent.Done else SpeechOutputEvent.Stopped) }
    }
    private fun fail(a: Attempt, message: String) {
        if (current!==a && a !in retiring) return
        val callback=a.stopReply;a.stopReply=null
        if (current===a) current=null
        runCatching { a.sdk.stop() };release(a)
        callback?.invoke(false)
        Log.i("OnMyWayVoice","tts-failed")
        if (!a.ending) a.event(SpeechOutputEvent.Failure(message))
    }
    fun stopThen(ready: (Boolean)->Unit) {
        generation++
        stopCurrent(ready)
    }
    private fun stopCurrent(ready: (Boolean)->Unit) {
        val a=current
        if (a==null) { ready(retiring.isEmpty());return }
        current=null;a.ending=true;a.stopReply=ready;retiring+=a
        if (!a.initialized) { release(a);a.stopReply=null;ready(true);return }
        runCatching { a.sdk.stop() }.onFailure { fail(a,"음성 안내를 중지하지 못했어요.") }
        handler.postDelayed({ if(a in retiring) { release(a);val reply=a.stopReply;a.stopReply=null;reply?.invoke(false) } },2000)
    }
    fun close() { stopThen { } }
    private fun release(a: Attempt) {
        retiring-=a
        a.connectionTimeout?.let { handler.removeCallbacks(it) }
        a.playbackTimeout?.let { handler.removeCallbacks(it) }
        a.listener?.let { runCatching { a.sdk.removeEventListener(it) } };a.listener=null
        runCatching { a.sdk.release() }
    }
}

package ai.onmyway.car

import android.content.Context
import android.os.Handler
import android.os.Looper
import ai.pleos.playground.location.fused.FusedLocation
import ai.pleos.playground.location.fused.listener.FusedLocationListener
import ai.pleos.playground.location.fused.model.LocationData

/** Foreground one-shot acquisition. Never logs position or keeps a background listener. */
class PleosLocation(private val context: Context) {
    private val handler = Handler(Looper.getMainLooper())
    private var generation = 0L
    private var sdk: FusedLocation? = null
    private var listener: FusedLocationListener? = null

    fun acquire(done: (Result<SetupLocation>) -> Unit) {
        close()
        val own = generation
        fun finish(result: Result<SetupLocation>) {
            if (generation != own) return
            close()
            done(result)
        }
        try {
            val service = FusedLocation(context.applicationContext)
            check(service.isSupported()) { "이 차량 환경은 현재 위치를 지원하지 않아요. 위치를 직접 설정해 주세요." }
            sdk = service
            service.initialize()
            val callback = object : FusedLocationListener {
                override fun onChanged(location: LocationData?) {
                    handler.post {
                        if (generation != own || location == null) return@post
                        val selection = SetupLocation(location.latitude, location.longitude, LocationSource.DEVICE,
                            "차량 현재 위치", location.timestampMillis)
                        // A cached/stale/null fix is not success; allow a fresh update until timeout.
                        if (selection.usable(System.currentTimeMillis(), true)) finish(Result.success(selection))
                    }
                }
                override fun onError(exception: Exception) {
                    handler.post { finish(Result.failure(IllegalStateException("차량 위치를 받지 못했어요. 위치 권한·차량 개인정보 설정을 확인하거나 직접 설정해 주세요."))) }
                }
            }
            listener = callback
            handler.postDelayed({ finish(Result.failure(IllegalStateException("최신 차량 위치를 10초 안에 받지 못했어요. 다시 확인하거나 직접 설정해 주세요."))) }, 10_000)
            service.registerFusedLocationCallback(callback)
            service.getLastKnownLocation(callback)
        } catch (error: Exception) {
            finish(Result.failure(IllegalStateException(if (error is SecurityException)
                "차량 위치 권한이 없어요. 권한을 허용하거나 직접 설정해 주세요."
                else "차량 위치를 시작하지 못했어요. Pleos 지원 환경을 확인하거나 직접 설정해 주세요.")))
        }
    }

    fun showAccessHelp(): Boolean = runCatching {
        val service = FusedLocation(context)
        try { check(service.isSupported()); service.initialize(); service.showLocationAccessBlockedPopup() }
        finally { service.release() }
    }.isSuccess

    fun close() {
        generation++
        handler.removeCallbacksAndMessages(null)
        val service = sdk; val registered = listener
        sdk = null; listener = null
        if (service != null) {
            if (registered != null) runCatching { service.unregisterFusedLocationCallback(registered) }
            runCatching { service.release() }
        }
    }
}

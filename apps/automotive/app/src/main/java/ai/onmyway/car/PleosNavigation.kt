package ai.onmyway.car

import android.content.Context
import android.os.Handler
import android.os.Looper
import ai.pleos.playground.navi.constants.NaviErrorCode
import ai.pleos.playground.navi.data.DestinationArrivedInfo
import ai.pleos.playground.navi.data.RouteInfo
import ai.pleos.playground.navi.data.RouteStartInfo
import ai.pleos.playground.navi.helper.NaviHelper
import ai.pleos.playground.navi.helper.listener.NaviHelperEventListener

/** Typed integration against NaviHelper 3.0.5 / navi-data 3.0.4, inspected using javap. */
class PleosNavigation(context: Context, private val event: (String) -> Unit) {
    private val helper = NaviHelper(context)
    private val handler = Handler(Looper.getMainLooper())
    var connected = false; private set
    private var pending = false
    private var expectedName: String? = null
    private var matchedRouteStart = false
    private var routingCompleted = false
    private var acknowledged = false
    private fun acknowledgeWhenMatched() {
        if (pending && matchedRouteStart && routingCompleted && !acknowledged) {
            acknowledged = true; event("accepted")
        }
    }
    private val listener = object : NaviHelperEventListener {
        override fun onRouteStarted(info: RouteStartInfo) {
            matchedRouteStart = pending && info.name == expectedName
            acknowledgeWhenMatched()
        }
        override fun onRoutingCompleted() {
            if (pending) routingCompleted = true
            acknowledgeWhenMatched()
        }
        override fun onDestinationArrived(info: DestinationArrivedInfo) {
            if (pending && acknowledged && info.name == expectedName) { pending = false; event("arrived") }
        }
        override fun onError(errorCode: NaviErrorCode) { pending = false; event("error:$errorCode") }
        override fun onRouteCancelled() { pending = false; event("error:경로가 취소되었습니다") }
    }
    fun connect() {
        runCatching {
            helper.initialize(
                onConnected = { connected = true; helper.addListener(listener); event("connected") },
                onFailed = { connected = false; event("error:내비 서비스 연결 실패") }
            )
        }.onFailure { connected = false; event("error:내비 서비스를 사용할 수 없습니다") }
        handler.postDelayed({ if (!connected) event("error:Pleos 연결 응답 없음 · 실제 서비스 연결 미확인") }, 10000)
    }
    fun request(latitude: Double, longitude: Double, name: String) {
        check(connected) { "Pleos 내비 서비스가 연결되지 않았습니다." }
        pending = true
        expectedName = name; matchedRouteStart = false; routingCompleted = false; acknowledged = false
        try {
            helper.requestRoute(RouteInfo(latitude = latitude, longitude = longitude,
                displayLatitude = null, displayLongitude = null, poiName = name,
                poiId = "", address = "", poiSubId = ""))
            event("requested")
        } catch (e: Exception) { pending = false; throw e }
    }
    fun close() {
        handler.removeCallbacksAndMessages(null)
        pending = false; connected = false
        runCatching { helper.removeListener(listener) }
        runCatching { helper.release() }
    }
}

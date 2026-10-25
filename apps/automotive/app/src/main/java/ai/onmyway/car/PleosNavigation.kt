package ai.onmyway.car

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import ai.pleos.playground.navi.constants.NaviErrorCode
import ai.pleos.playground.navi.data.DestinationArrivedInfo
import ai.pleos.playground.navi.data.RouteInfo
import ai.pleos.playground.navi.data.RouteStartInfo
import ai.pleos.playground.navi.helper.NaviHelper
import ai.pleos.playground.navi.helper.listener.NaviHelperEventListener

/** Typed integration against NaviHelper 3.0.5 / navi-data 3.0.4, inspected using javap. */
class PleosNavigation(private val context: Context, private val event: (String) -> Unit) {
    private val handler = Handler(Looper.getMainLooper())
    private val requests = NavigationRequestState(SystemClock::elapsedRealtime)
    private var helper: NaviHelper? = null
    private var listener: NaviHelperEventListener? = null
    private var session = 0L
    private var connecting = false
    private var requestToken: Long? = null
    private var acceptanceTimeout: Runnable? = null
    var connected = false; private set

    private fun dispatch(connection: Long, action: () -> Unit) {
        handler.post { if (connection == session) action() }
    }
    private fun clearAcceptanceTimeout() {
        acceptanceTimeout?.let(handler::removeCallbacks)
        acceptanceTimeout = null
    }
    private fun accepted() {
        clearAcceptanceTimeout()
        event("accepted")
    }
    private fun requestTimedOut(token: Long) {
        if (!requests.expire(token)) return
        disconnect(cancelRoute = true)
        event("error:내비 경로 수락 응답이 15초 안에 오지 않았습니다. 다시 연결하고 새 계획을 승인하세요.")
    }
    private fun failRequest(message: String) {
        val token = requestToken ?: return
        if (!requests.fail(token)) return
        disconnect(cancelRoute = true)
        event("error:$message")
    }

    /** Idempotent while connecting/connected. A released session gets a new helper and listener. */
    fun connect() {
        if (connected || connecting) return
        disconnect(cancelRoute = false)
        val connection = session
        connecting = true
        val sdk = runCatching { NaviHelper(context) }.getOrElse {
            disconnect(cancelRoute = false)
            event("error:내비 서비스를 사용할 수 없습니다")
            return
        }
        helper = sdk
        val callbacks = object : NaviHelperEventListener {
            override fun onRouteStarted(info: RouteStartInfo) = dispatch(connection) {
                val token = requestToken ?: return@dispatch
                if (requests.routeStarted(token, info.name)) accepted() else requestTimedOut(token)
            }
            override fun onRoutingCompleted() = dispatch(connection) {
                val token = requestToken ?: return@dispatch
                if (requests.routingCompleted(token)) accepted() else requestTimedOut(token)
            }
            override fun onDestinationArrived(info: DestinationArrivedInfo) = dispatch(connection) {
                val token = requestToken ?: return@dispatch
                if (requests.arrived(token, info.name)) {
                    disconnect(cancelRoute = false)
                    event("arrived")
                }
            }
            override fun onError(errorCode: NaviErrorCode) = dispatch(connection) { failRequest(errorCode.toString()) }
            override fun onRouteCancelled() = dispatch(connection) { failRequest("경로가 취소되었습니다. 다시 연결하고 새 계획을 승인하세요.") }
        }
        listener = callbacks
        runCatching {
            sdk.initialize(
                onConnected = { dispatch(connection) {
                    if (!connecting || helper !== sdk) return@dispatch
                    runCatching { sdk.addListener(callbacks) }.onFailure {
                        disconnect(cancelRoute = false)
                        event("error:내비 이벤트를 연결하지 못했습니다")
                        return@dispatch
                    }
                    requests.resetConnection()
                    connecting = false
                    connected = true
                    event("connected")
                } },
                onFailed = { dispatch(connection) {
                    disconnect(cancelRoute = false)
                    event("error:내비 서비스 연결 실패")
                } }
            )
        }.onFailure {
            disconnect(cancelRoute = false)
            event("error:내비 서비스를 사용할 수 없습니다")
        }
        handler.postDelayed({
            if (session == connection && connecting) {
                disconnect(cancelRoute = false)
                event("error:Pleos 연결 응답 없음 · 실제 서비스 연결 미확인")
            }
        }, 10_000)
    }

    fun request(latitude: Double, longitude: Double, name: String) {
        check(connected) { "Pleos 내비 서비스가 연결되지 않았습니다. 다시 연결하세요." }
        val token = checkNotNull(requests.begin(name)) { "이전 내비 요청이 남아 있습니다. 취소 후 다시 연결하고 승인하세요." }
        requestToken = token
        val timeout = Runnable { requestTimedOut(token) }
        acceptanceTimeout = timeout
        handler.postDelayed(timeout, NavigationRequestState.ACCEPTANCE_TIMEOUT_MILLIS)
        try {
            requireNotNull(helper).requestRoute(RouteInfo(latitude = latitude, longitude = longitude,
                displayLatitude = null, displayLongitude = null, poiName = name,
                poiId = "", address = "", poiSubId = ""))
            event("requested")
        } catch (e: Exception) {
            requests.fail(token)
            disconnect(cancelRoute = true)
            throw e
        }
    }

    fun cancelPending() {
        val active = requests.cancel()
        disconnect(cancelRoute = true)
        if (active) event("error:경로 요청을 취소했습니다. 다시 연결하고 새 계획을 승인하세요.")
    }
    private fun disconnect(cancelRoute: Boolean) {
        // Invalidate before calling SDK: synchronous and queued old callbacks cannot mutate a retry.
        session++
        connected = false
        connecting = false
        requestToken = null
        requests.cancel()
        handler.removeCallbacksAndMessages(null)
        acceptanceTimeout = null
        val sdk = helper
        helper = null
        listener?.let { callbacks -> runCatching { sdk?.removeListener(callbacks) } }
        listener = null
        if (cancelRoute) runCatching { sdk?.cancelRoute() }
        runCatching { sdk?.release() }
    }
    fun close() { disconnect(cancelRoute = requests.teardown()) }
}

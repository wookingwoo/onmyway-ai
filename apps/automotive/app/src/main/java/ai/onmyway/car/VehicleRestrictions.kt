package ai.onmyway.car

import android.content.Context
import android.os.Handler
import android.os.Looper
import java.lang.reflect.Proxy

/** Uses actual AAOS restrictions. Missing service, permission, or malformed state stays UNKNOWN. */
class VehicleRestrictions(private val context: Context, private val changed: (VehicleState) -> Unit) {
    private var car: Any? = null
    private var manager: Any? = null
    private var listener: Any? = null
    private val handler = Handler(Looper.getMainLooper())
    private val refresh = object : Runnable {
        override fun run() { connect(); handler.postDelayed(this, 2000) }
    }
    fun start() { handler.post(refresh) }
    private fun publish(state: VehicleState) { handler.post { changed(state) } }
    private fun decode(restrictions: Any?) {
        try {
            requireNotNull(restrictions)
            val requires = restrictions.javaClass.getMethod("isRequiresDistractionOptimization").invoke(restrictions) as Boolean
            val active = restrictions.javaClass.getMethod("getActiveRestrictions").invoke(restrictions) as Int
            publish(if (!requires && active == 0) VehicleState.PARKED else VehicleState.RESTRICTED)
        } catch (_: Exception) {
            publish(VehicleState.UNKNOWN)
            runCatching { manager?.javaClass?.getMethod("unregisterListener")?.invoke(manager) }
            runCatching { car?.javaClass?.getMethod("disconnect")?.invoke(car) }
            car = null; manager = null; listener = null
        }
    }
    private fun connect() {
        try {
            if (car == null) {
                val cls = Class.forName("android.car.Car")
                car = cls.getMethod("createCar", Context::class.java).invoke(null, context)
                manager = cls.getMethod("getCarManager", String::class.java).invoke(car, "uxrestriction")
                val callback = Class.forName("android.car.drivingstate.CarUxRestrictionsManager\$OnUxRestrictionsChangedListener")
                listener = Proxy.newProxyInstance(callback.classLoader, arrayOf(callback)) { proxy, method, args ->
                    when (method.name) {
                        "onUxRestrictionsChanged" -> { decode(args?.firstOrNull()); null }
                        "hashCode" -> System.identityHashCode(proxy)
                        "equals" -> proxy === args?.firstOrNull()
                        "toString" -> "OnMyWayUxListener"
                        else -> null
                    }
                }
                manager?.javaClass?.getMethod("registerListener", callback)?.invoke(manager, listener)
            }
            decode(manager?.javaClass?.getMethod("getCurrentCarUxRestrictions")?.invoke(manager))
        } catch (_: Exception) {
            publish(VehicleState.UNKNOWN)
            runCatching { manager?.javaClass?.getMethod("unregisterListener")?.invoke(manager) }
            runCatching { car?.javaClass?.getMethod("disconnect")?.invoke(car) }
            car = null; manager = null; listener = null
        }
    }
    fun stop() {
        handler.removeCallbacksAndMessages(null)
        runCatching { manager?.javaClass?.getMethod("unregisterListener")?.invoke(manager) }
        runCatching { car?.javaClass?.getMethod("disconnect")?.invoke(car) }
        car = null; manager = null; listener = null
    }
}

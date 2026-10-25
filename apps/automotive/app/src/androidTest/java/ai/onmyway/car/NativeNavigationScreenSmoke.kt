package ai.onmyway.car

import android.app.Activity
import android.app.Instrumentation
import android.os.Bundle
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Explicit device-only smoke: no route/destination, microphone, provider or fixture. */
class NativeNavigationScreenSmoke : Instrumentation() {
    private var mode: String?=null
    override fun onCreate(arguments: Bundle?) { super.onCreate(arguments);mode=arguments?.getString("mode");start() }
    override fun onStart() {
        if (mode=="seed-recovery" || mode=="restore-sample") {
            val prefs=targetContext.getSharedPreferences("onmyway",0)
            val backup=targetContext.getSharedPreferences("navigation-smoke-backup",0)
            val setup=runCatching {
                check(BuildConfig.DEMO)
                if(mode=="seed-recovery") {
                    check(prefs.getBoolean("fixture",false)) { "Only an independently selected sample may be seeded" }
                    check(!backup.contains("fixture")) { "Restore previous smoke state first" }
                    check(backup.edit().putBoolean("fixture",true).putBoolean("unresolved",prefs.getBoolean("guidance-unresolved",false)).commit())
                    check(prefs.edit().putBoolean("fixture",false).putBoolean("guidance-unresolved",true).commit())
                } else {
                    check(backup.contains("fixture"))
                    check(prefs.edit().putBoolean("fixture",backup.getBoolean("fixture",true)).putBoolean("guidance-unresolved",backup.getBoolean("unresolved",false)).commit())
                    backup.edit().clear().commit()
                }
            }
            finish(if(setup.isSuccess) Activity.RESULT_OK else Activity.RESULT_CANCELED,Bundle().apply { putString("stream",if(setup.isSuccess) "PASS: explicit recovery-hint setup/restore; not a real route" else "FAIL: "+setup.exceptionOrNull()?.message) })
            return
        }
        val ready=CountDownLatch(1)
        var connected=false
        var navigation: PleosNavigation?=null
        val outcome=runCatching {
            runOnMainSync {
                navigation=PleosNavigation(targetContext) { event ->
                    if(event=="connected") { connected=true;ready.countDown() }
                    if(event.startsWith("error:")) ready.countDown()
                }.also { it.connect() }
            }
            check(ready.await(12,TimeUnit.SECONDS)) { "Native connection timed out" }
            check(connected) { "Actual Pleos navigation service unavailable" }
            var result: Result<Unit>?=null
            runOnMainSync { result=navigation?.showNavigationScreen() }
            check(result?.isSuccess==true) { "Screen dispatch failed" }
            android.util.Log.i("OnMyWayNavi","device-smoke-dispatch-only; visibility requires separate inspection")
        }
        runOnMainSync { navigation?.close() }
        val result=Bundle().apply {
            putString("stream",if(outcome.isSuccess) "PASS: actual SDK screen dispatch; visibility must be inspected separately" else "FAIL: "+outcome.exceptionOrNull()?.message)
        }
        finish(if(outcome.isSuccess) Activity.RESULT_OK else Activity.RESULT_CANCELED,result)
    }
}

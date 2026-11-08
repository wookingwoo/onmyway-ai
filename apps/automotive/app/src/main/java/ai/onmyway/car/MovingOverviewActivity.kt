package ai.onmyway.car

import android.app.Activity
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import java.lang.ref.WeakReference
import java.util.UUID

/** Static, private view of the approved leg. No route actions or inputs are hosted here. */
class MovingOverviewActivity : Activity() {
    private lateinit var restrictions: VehicleRestrictions
    private lateinit var status: TextView
    private lateinit var overview: LinearLayout
    private var sawRestriction = false
    private var observing = false
    private val snapshotValid get() = activeKey != null && activeKey == intent.getStringExtra(SNAPSHOT)
    private val palette get() = CockpitTheme.forNight(
        resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        shown = WeakReference(this)
        sawRestriction = savedInstanceState?.getBoolean("sawRestriction") ?: false
        val destination = intent.getStringExtra(DESTINATION)?.takeIf { it.isNotBlank() }
        if (destination == null) { finish(); return }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(46), dp(36), dp(46), dp(36))
            setBackgroundColor(Color.parseColor(palette.canvas))
        }
        root.addView(label("OnMyWay", 28f, palette.ink, true))
        status = label("차량 상태를 확인하고 있어요", 23f, palette.muted).also { root.addView(it) }
        overview = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(34), dp(24), dp(34), dp(26))
            background = GradientDrawable().apply {
                setColor(Color.parseColor(palette.surface))
                cornerRadius = dp(28).toFloat()
            }
            visibility = View.GONE
        }
        root.addView(overview, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(24) })
        overview.addView(label("승인한 목적지", 22f, palette.muted))
        overview.addView(label(destination, 52f, palette.ink, true).apply {
            maxLines = 1; ellipsize = TextUtils.TruncateAt.END
        })
        val times = LinearLayout(this)
        addTime(times, "도착 예상", intent.getStringExtra(ARRIVAL))
        addTime(times, "귀가 예상", intent.getStringExtra(HOME))
        overview.addView(times, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(16) })
        val source = if (intent.getBooleanExtra(EXAMPLE, false)) "예시 일정 · 계획 시각" else "계획을 승인했을 때의 예상 시각"
        overview.addView(label(source, 20f, palette.muted))
        root.addView(label("일정 변경과 완료 확인은 정차 후에 할 수 있어요.", 23f, palette.muted).apply {
            setPadding(0, dp(22), 0, 0)
        })
        setContentView(root)
        restrictions = VehicleRestrictions(this) { state ->
            if (!observing) return@VehicleRestrictions
            when (state) {
                VehicleState.RESTRICTED -> {
                    sawRestriction = true
                    if (snapshotValid) {
                        status.text = "주행 중 · 읽기 전용"
                        overview.visibility = View.VISIBLE
                    } else hideSnapshot()
                }
                VehicleState.PARKED -> {
                    if (sawRestriction || !snapshotValid) finish()
                    else {
                        status.text = "정차 중 · 이동 현황"
                        overview.visibility = View.VISIBLE
                    }
                }
                VehicleState.UNKNOWN -> {
                    status.text = "차량 상태를 확인하고 있어요"
                    overview.visibility = View.GONE
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (::restrictions.isInitialized) {
            observing = true
            restrictions.start()
        }
    }

    override fun onStop() {
        observing = false
        if (::restrictions.isInitialized) restrictions.stop()
        if (::overview.isInitialized) overview.visibility = View.GONE
        super.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean("sawRestriction", sawRestriction)
        super.onSaveInstanceState(outState)
    }

    private fun hideSnapshot() {
        if (!::overview.isInitialized) return
        status.text = "정차 후 안내를 확인해 주세요"
        overview.visibility = View.GONE
    }

    private fun addTime(row: LinearLayout, title: String, value: String?) {
        val column = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        column.addView(label(title, 21f, palette.muted))
        column.addView(label(value?.takeUnless { it.isBlank() } ?: "—", 36f, palette.ink, true))
        row.addView(column, LinearLayout.LayoutParams(0, -2, 1f))
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    private fun label(value: String, size: Float, color: String, bold: Boolean = false) = TextView(this).apply {
        text = value; textSize = size; setTextColor(Color.parseColor(color))
        if (bold) setTypeface(typeface, Typeface.BOLD)
        setPadding(0, dp(4), 0, dp(4))
    }

    companion object {
        private const val DESTINATION = "destination"
        private const val ARRIVAL = "arrival"
        private const val HOME = "home"
        private const val EXAMPLE = "example"
        private const val SNAPSHOT = "snapshot"
        private var activeKey: String? = null
        private var shown: WeakReference<MovingOverviewActivity>? = null

        fun intent(from: Activity, destination: String, arrival: String, home: String, example: Boolean): Intent {
            invalidate()
            val key = UUID.randomUUID().toString()
            activeKey = key
            return Intent(from, MovingOverviewActivity::class.java).putExtra(DESTINATION, destination)
                .putExtra(ARRIVAL, arrival).putExtra(HOME, home).putExtra(EXAMPLE, example).putExtra(SNAPSHOT, key)
        }

        fun invalidate() {
            activeKey = null
            shown?.get()?.hideSnapshot()
            shown = null
        }
    }
}

package ai.onmyway.car

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

/** Foreground speech creates a proposal automatically; navigation is approved separately. */
class VoiceInputDialog(
    private val owner: Activity,
    private val source: VoiceSource,
    private val vehicle: () -> VehicleState,
    private val exampleText: String = VoiceInputSession.EXAMPLE_TEXT,
    private val prompt: String = "할 일과 집에 도착할 시간을 함께 말해 주세요.",
    private val applied: (VoiceDraft) -> Unit
) : AlertDialog(owner) {
    private val session = VoiceInputSession()
    private var token: Long? = null
    private val sdk = PleosSpeechInput(owner) { event ->
        val id = token
        if (id != null && isShowing) {
            if (vehicle() != VehicleState.PARKED) { dismiss(); return@PleosSpeechInput }
            when (event) {
                SpeechInputEvent.Connected -> connection = "마이크를 준비하고 있어요"
                SpeechInputEvent.Listening -> session.listening(id)
                is SpeechInputEvent.Transcript -> session.update(id, event.text, event.completed)
                is SpeechInputEvent.Failure -> session.fail(id, event.message)
            }
            refresh()
        }
    }
    private var connection = "차량 음성 서비스에 연결하고 있어요"
    private val dark = owner.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK == android.content.res.Configuration.UI_MODE_NIGHT_YES
    private val palette = CockpitTheme.forNight(dark)
    private val surface = Color.parseColor(palette.surface)
    private val ink = Color.parseColor(palette.ink)
    private val muted = Color.parseColor(palette.muted)
    private val soft = Color.parseColor(palette.secondary)
    private val mint = Color.parseColor(if (dark) "#9BE4CA" else "#216A53")
    private lateinit var title: TextView
    private lateinit var transcript: TextView
    private lateinit var detail: TextView
    private lateinit var confirm: Button
    private lateinit var retry: Button
    private lateinit var orb: View
    private fun dp(value: Int) = (value * owner.resources.displayMetrics.density).toInt()
    private fun shape(color: Int, radius: Int = 28) = GradientDrawable().apply { setColor(color); cornerRadius = dp(radius).toFloat() }
    private fun label(value: String, size: Float, color: Int = ink, bold: Boolean = false) = TextView(owner).apply {
        text = value; textSize = size; setTextColor(color)
        if (bold) setTypeface(typeface, Typeface.BOLD)
    }
    private fun LinearLayout.add(view: View, gap: Int = 16) {
        addView(view, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(gap) })
    }
    private fun action(value: String, primary: Boolean = false, click: () -> Unit) = Button(owner).apply {
        text = value; textSize = 24f; isAllCaps = false; minHeight = dp(80)
        setTextColor(if (primary) surface else ink); backgroundTintList = null
        background = shape(if (primary) ink else soft, 40)
        setOnClickListener { click() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setCanceledOnTouchOutside(false)
        val body = LinearLayout(owner).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(40), dp(32), dp(40), dp(32))
            background = shape(surface)
        }
        body.add(label(if (source == VoiceSource.EXAMPLE) "샘플 요청 · 저장된 문장" else "음성으로 요청", 19f, muted), 24)
        val heading = LinearLayout(owner).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        orb = object : View(owner) {
            private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            override fun onDraw(canvas: Canvas) {
                super.onDraw(canvas)
                val scale = width / 112f
                canvas.save(); canvas.scale(scale, scale)
                paint.style = Paint.Style.FILL; paint.color = ink; canvas.drawCircle(56f, 56f, 54f, paint)
                paint.color = surface; canvas.drawRoundRect(47f, 27f, 65f, 63f, 9f, 9f, paint)
                paint.style = Paint.Style.STROKE; paint.strokeWidth = 4f; paint.strokeCap = Paint.Cap.ROUND
                canvas.drawArc(38f, 38f, 74f, 73f, 0f, 180f, false, paint)
                canvas.drawLine(56f, 73f, 56f, 84f, paint); canvas.drawLine(45f, 84f, 67f, 84f, paint)
                paint.style = Paint.Style.FILL; paint.color = mint; canvas.drawCircle(96f, 16f, 9f, paint)
                canvas.restore()
            }
        }.apply { importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO }
        heading.addView(orb, LinearLayout.LayoutParams(dp(112), dp(112)).apply { rightMargin = dp(28) })
        val headings = LinearLayout(owner).apply { orientation = LinearLayout.VERTICAL }
        title = label("", 34f, ink, true).apply { accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE }
        headings.add(title, 10)
        headings.add(label(prompt, 22f, muted), 0)
        heading.addView(headings, LinearLayout.LayoutParams(0, -2, 1f)); body.add(heading, 28)
        transcript = label("", 28f).apply {
            setPadding(dp(26), dp(24), dp(26), dp(24)); background = shape(soft, 20)
            minHeight = dp(136); maxLines = 4; gravity = Gravity.CENTER_VERTICAL
        }
        body.add(transcript, 16)
        detail = label("", 21f, muted).apply { minHeight = dp(64); maxLines = 3 }
        body.add(detail, 24)
        val actions = LinearLayout(owner).apply { orientation = LinearLayout.HORIZONTAL }
        val cancel = action("취소") { dismiss() }
        retry = action("다시 말하기") { begin() }
        confirm = action("계획 준비하기", true) { deliver() }
        actions.addView(cancel, LinearLayout.LayoutParams(0, -2, .65f).apply { rightMargin = dp(12) })
        actions.addView(retry, LinearLayout.LayoutParams(0, -2, .9f).apply { rightMargin = dp(12) })
        actions.addView(confirm, LinearLayout.LayoutParams(0, -2, 1.35f))
        body.add(actions, 0)
        setContentView(body)
        window?.setBackgroundDrawableResource(android.R.color.transparent)
        window?.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
        window?.setLayout(minOf(dp(1180), (owner.resources.displayMetrics.widthPixels * .9).toInt()), -2)
    }

    override fun show() { super.show(); begin() }
    private fun begin() {
        sdk.close()
        token = session.start(source, vehicle(), BuildConfig.DEMO)
        if (token == null) { dismiss(); return }
        connection = "차량 음성 서비스에 연결하고 있어요"
        if (source == VoiceSource.EXAMPLE) session.update(token!!, exampleText, true)
        refresh()
        if (source == VoiceSource.PLEOS) sdk.start()
    }
    private fun refresh() {
        val phase = session.phase
        title.text = when (phase) {
            VoicePhase.REVIEW -> "이 요청으로 준비할게요"
            VoicePhase.ERROR -> "음성 입력을 마치지 못했어요"
            VoicePhase.LISTENING -> "듣고 있어요"
            else -> connection
        }
        transcript.text = session.transcript.ifBlank {
            if (phase == VoicePhase.ERROR) "직접 입력으로도 계속할 수 있어요."
            else "“약국 들르고, 저녁 7시까지 집에 가야 해.”"
        }
        transcript.setTextColor(if (session.transcript.isBlank()) muted else ink)
        detail.text = when {
            source == VoiceSource.EXAMPLE -> "장소와 순서를 정리할게요. 준비된 계획을 보고 출발을 확인해 주세요."
            phase == VoicePhase.ERROR -> session.error
            phase == VoicePhase.REVIEW -> "계획 요약에서 내용을 확인하고 고칠 수 있어요. 출발은 별도로 승인해 주세요."
            else -> "최대 30초 동안 입력해요. 말을 마치면 자동으로 계획을 준비합니다. 출발 전 요약을 확인해 주세요."
        }
        confirm.visibility = if (source == VoiceSource.EXAMPLE) View.VISIBLE else View.GONE
        confirm.isEnabled = phase == VoicePhase.REVIEW
        confirm.alpha = if (confirm.isEnabled) 1f else .35f
        retry.visibility = if (source == VoiceSource.EXAMPLE) View.GONE else View.VISIBLE
        retry.isEnabled = phase in setOf(VoicePhase.REVIEW, VoicePhase.ERROR)
        retry.alpha = if (retry.isEnabled) 1f else .35f
        if (source == VoiceSource.PLEOS && phase == VoicePhase.REVIEW) window?.decorView?.post { if (isShowing) deliver() }
    }
    private fun deliver() {
        val result = token?.let { session.confirm(it, vehicle(), BuildConfig.DEMO) } ?: return
        dismiss()
        applied(result)
    }
    override fun dismiss() {
        token = null
        session.cancel()
        sdk.close()
        super.dismiss()
    }
}

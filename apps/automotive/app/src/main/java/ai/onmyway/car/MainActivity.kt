package ai.onmyway.car

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.SystemClock
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.Executors

/** Native landscape cockpit. Every state-changing action rechecks the restriction gate. */
class MainActivity : Activity() {
    private val dark get() = resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK == android.content.res.Configuration.UI_MODE_NIGHT_YES
    private val bg get() = Color.parseColor(if (dark) "#141516" else "#F5F6F7")
    private val panel get() = Color.parseColor(if (dark) "#222426" else "#FFFFFF")
    private val ink get() = Color.parseColor(if (dark) "#F2F3F4" else "#202124")
    private val muted get() = Color.parseColor(if (dark) "#B4B8BC" else "#62676C")
    private val soft get() = Color.parseColor(if (dark) "#303336" else "#E9ECEF")
    private val amber get() = Color.parseColor(if (dark) "#F3CE8D" else "#795313")
    private val flow = JourneyFlow()
    private var errorMessage: String? = null
    private var reviewNotes: String? = null
    private var intentReview: IntentReview? = null
    private val openDialogs = mutableListOf<android.app.Dialog>()
    private val state = SessionState()
    private val worker = Executors.newSingleThreadExecutor()
    private val controls = mutableListOf<Pair<View, () -> Boolean>>()
    private val prefs by lazy { getSharedPreferences("onmyway", MODE_PRIVATE) }
    private var vehicle = VehicleState.UNKNOWN
    private var simulate = false
    private var busy = false
    private var fixture = false
    private var request: JSONObject? = null
    private var plan: JSONObject? = null
    private var errands = JSONArray()
    private var restored = false
    private var requiresParse = false
    private var voiceExample = false
    private val exampleInput get() = VoiceInputSession.isExampleDraft(prefs.getString("input", "").orEmpty(), voiceExample, BuildConfig.DEMO)
    private var voiceDialog: VoiceInputDialog? = null
    private var status = "정차 상태를 확인한 뒤 계획을 시작하세요."
    private var navStatus = "Pleos 내비 연결 확인 중"
    private var computedElapsedMillis: Long? = null
    private var computationStartedWallMillis: Long? = null
    private lateinit var restrictions: VehicleRestrictions
    private lateinit var navigation: PleosNavigation
    private lateinit var root: LinearLayout
    private lateinit var bodyScroll: ScrollView
    private lateinit var lockedPanel: LinearLayout
    private lateinit var footer: LinearLayout
    private var demoRescue: Button? = null
    private lateinit var lockLabel: TextView
    private lateinit var statusLabel: TextView
    private lateinit var input: EditText
    private lateinit var server: EditText
    private lateinit var deadline: EditText
    private lateinit var originLat: EditText
    private lateinit var originLon: EditText
    private lateinit var homeLat: EditText
    private lateinit var homeLon: EditText
    private lateinit var delay: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        restore()
        flow.restore(errands.length() > 0, plan != null)
        render()
        restrictions = VehicleRestrictions(this) { value ->
            if (vehicle != value) { vehicle = value; applyLock() }
        }
        navigation = PleosNavigation(this) { event -> runOnUiThread { onNavigation(event) } }
        navigation.connect()
    }
    override fun onStart() { super.onStart(); if (::restrictions.isInitialized) restrictions.start() }
    override fun onStop() { voiceDialog?.dismiss(); if (::restrictions.isInitialized) restrictions.stop(); vehicle = VehicleState.UNKNOWN; applyLock(); super.onStop() }
    override fun onDestroy() { navigation.close(); worker.shutdownNow(); super.onDestroy() }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    private fun column() = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
    private fun row() = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
    private fun shape(color: Int, radius: Int = 24) = GradientDrawable().apply { setColor(color); cornerRadius = dp(radius).toFloat() }
    private fun text(value: String, size: Float = 24f, color: Int = ink, bold: Boolean = false) = TextView(this).apply {
        text = value; textSize = size; setTextColor(color)
        if (bold) setTypeface(typeface, Typeface.BOLD)
        setPadding(0, dp(4), 0, dp(4))
    }
    private fun LinearLayout.add(view: View, margin: Int = 16) {
        addView(view, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(margin) })
    }
    private fun card(parent: LinearLayout, title: String, subtitle: String? = null): LinearLayout {
        val card = column().apply { background = shape(panel); setPadding(dp(28), dp(22), dp(28), dp(22)) }
        if (title.isNotBlank()) card.add(text(title, 28f, ink, true), 8)
        if (subtitle != null) card.add(text(subtitle, 22f, muted), 8)
        parent.add(card, 20)
        return card
    }
    private fun field(parent: LinearLayout, label: String, value: String, multiline: Boolean = false, onChange: (String) -> Unit): EditText {
        parent.add(text(label, 22f, muted), 8)
        val editor = EditText(this).apply {
            setText(value); textSize = 26f; setTextColor(ink); setHintTextColor(muted)
            background = shape(soft, 16); setPadding(dp(24), dp(18), dp(24), dp(18)); minHeight = dp(80); minimumHeight = dp(80)
            inputType = InputType.TYPE_CLASS_TEXT or if (multiline) InputType.TYPE_TEXT_FLAG_MULTI_LINE else 0
            setSingleLine(!multiline)
            if (multiline) { minLines = 2; maxLines = 3; gravity = Gravity.TOP }
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { onChange(s.toString()) }
                override fun afterTextChanged(s: Editable?) {}
            })
        }
        controls += editor to { true }; parent.add(editor)
        return editor
    }
    private fun button(parent: LinearLayout, label: String, primary: Boolean = false, enabled: () -> Boolean = { true }, action: () -> Unit): Button {
        val button = Button(this).apply {
            text = label; textSize = 25f; isAllCaps = false; minHeight = dp(80); minimumHeight = dp(80)
            setPadding(dp(24), dp(14), dp(24), dp(14)); setTextColor(if (primary) bg else ink)
            backgroundTintList = null; background = shape(if (primary) ink else soft, 40)
            elevation = 0f
            setOnClickListener { if (allowed() && !busy && enabled()) runCatching(action).onFailure { showError(it) } }
        }
        controls += button to enabled; parent.add(button, 12)
        return button
    }
    private fun showEditor(parent: LinearLayout, label: String, editor: EditText) {
        (editor.parent as? ViewGroup)?.removeView(editor)
        parent.add(text(label, 22f, muted), 8); parent.add(editor, 24)
    }
    private fun dialog(builder: AlertDialog.Builder): AlertDialog {
        val result = builder.create()
        trackDialog(result)
        result.setOnShowListener {
            result.findViewById<TextView>(android.R.id.message)?.apply {
                textSize = 24f; setTextColor(ink); setLineSpacing(dp(4).toFloat(), 1f)
            }
            for (id in listOf(AlertDialog.BUTTON_POSITIVE, AlertDialog.BUTTON_NEGATIVE, AlertDialog.BUTTON_NEUTRAL)) {
                result.getButton(id)?.apply { textSize = 24f; minHeight = dp(76); minWidth = dp(120); setTextColor(ink) }
            }
        }
        result.window?.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN or android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        result.show()
        result.window?.setLayout((resources.displayMetrics.widthPixels * .82).toInt().coerceAtMost(dp(1100)), -2)
        return result
    }
    private fun trackDialog(value: android.app.Dialog) {
        openDialogs += value
        value.setOnDismissListener { openDialogs.remove(value) }
    }
    private fun heading(parent: LinearLayout, title: String, description: String? = null) {
        parent.add(text(title, 40f, ink, true), 16)
        if (description != null) parent.add(text(description, 24f, muted), 28)
    }
    private fun allowed() = state.canInteract(vehicle, BuildConfig.DEMO, simulate)
    private fun edited() {
        voiceDialog?.dismiss()
        val wasRoute = flow.screen == JourneyScreen.ROUTE
        state.edited(); plan = null; flow.edited(); errorMessage = null
        persist()
        if (wasRoute) render() else applyLock()
    }
    @Deprecated("Uses the app journey instead of closing an in-progress drive")
    override fun onBackPressed() { if (allowed() && !busy) { flow.back(); render() } }

    private fun render() {
        voiceDialog?.dismiss()
        controls.clear(); demoRescue = null
        root = column().apply { setBackgroundColor(bg); setPadding(dp(40), dp(20), dp(40), dp(20)) }
        root.setOnApplyWindowInsetsListener { view, insets ->
            @Suppress("DEPRECATION")
            view.setPadding(dp(40) + insets.systemWindowInsetLeft, dp(20) + insets.systemWindowInsetTop,
                dp(40) + insets.systemWindowInsetRight, dp(20) + insets.systemWindowInsetBottom)
            insets
        }
        val header = row()
        if (flow.screen != JourneyScreen.INPUT) {
            val backHost = column()
            button(backHost, "‹ 뒤로") { flow.back(); render() }
            header.addView(backHost, LinearLayout.LayoutParams(dp(150), -2).apply { rightMargin = dp(24) })
        }
        header.addView(text("OnMyWay", 28f, ink, true), LinearLayout.LayoutParams(0, -2, 1f))
        header.addView(text(when(flow.screen) { JourneyScreen.INPUT -> "1 / 3"; JourneyScreen.REVIEW -> "2 / 3"; JourneyScreen.ROUTE -> "3 / 3"; else -> "설정" }, 22f, muted))
        if (flow.screen != JourneyScreen.SETTINGS) {
            val settingsHost = column()
            button(settingsHost, "설정") { flow.settings(); render() }
            header.addView(settingsHost, LinearLayout.LayoutParams(dp(130), -2).apply { leftMargin = dp(24) })
        }
        root.add(header, 4)
        lockLabel = text("", 20f, amber)
        root.add(lockLabel, 12)

        // All inputs stay native and bound to persisted drafts; only the current
        // stage attaches the relevant controls to the visible hierarchy.
        val storage = column()
        input = field(storage, "할 일", prefs.getString("input", "").orEmpty(), true) {
            voiceExample = false
            intentReview = null
            prefs.edit().putString("input", it).putBoolean("voice-example", false).apply(); requiresParse = true; edited()
        }.apply { hint = "약국 들르고, 세탁물 찾고, 장도 보고 싶어요" }
        server = field(storage, "서버 주소", prefs.getString("server", BuildConfig.DEFAULT_SERVER_URL).orEmpty()) {
            prefs.edit().putString("server", it).apply(); edited()
        }
        originLat = field(storage, "출발 위도", draft("originLat", "origin", "latitude")) { saveDraft("originLat", it) }
        originLon = field(storage, "출발 경도", draft("originLon", "origin", "longitude")) { saveDraft("originLon", it) }
        homeLat = field(storage, "집 위도", draft("homeLat", "home", "latitude")) { saveDraft("homeLat", it) }
        homeLon = field(storage, "집 경도", draft("homeLon", "home", "longitude")) { saveDraft("homeLon", it) }
        deadline = field(storage, "귀가 마감", prefs.getString("draft-deadline", request?.optString("deadline") ?: OffsetDateTime.now().withHour(23).withMinute(0).withSecond(0).toString()).orEmpty()) { saveDraft("deadline", it) }
        delay = field(storage, "추가 지연 (분)", prefs.getString("delay", "0").orEmpty()) { prefs.edit().putString("delay", it).apply(); edited() }

        val body = column().apply { setPadding(0, dp(16), 0, dp(16)) }
        bodyScroll = ScrollView(this).apply { isFillViewport = true; isVerticalScrollBarEnabled = false; addView(body) }
        root.addView(bodyScroll, LinearLayout.LayoutParams(-1, 0, 1f))
        lockedPanel = column().apply { gravity = Gravity.CENTER; visibility = View.GONE }
        lockedPanel.add(text("정차 후 이용해 주세요", 40f, ink, true).apply { gravity = Gravity.CENTER }, 20)
        lockedPanel.add(text("주행 중에는 입력과 목적지 변경을 잠급니다.", 24f, muted).apply { gravity = Gravity.CENTER }, 30)
        if (BuildConfig.DEMO) {
            demoRescue = Button(this).apply {
                text = "예시로 체험 · 모의 안내"; textSize = 25f; minHeight = dp(80)
                setTextColor(ink); backgroundTintList = null; background = shape(soft, 40)
                setOnClickListener { if (vehicle == VehicleState.UNKNOWN && !busy) { simulate = true; flow.start(); render(); loadFixture() } }
            }
            lockedPanel.add(demoRescue!!)
        }
        root.addView(lockedPanel, LinearLayout.LayoutParams(-1, 0, 1f))
        footer = column()
        statusLabel = text("", 22f, amber).apply { maxLines = 3 }
        footer.add(statusLabel, 8)
        root.add(footer, 0)
        when (flow.screen) {
            JourneyScreen.INPUT -> renderInput(body)
            JourneyScreen.REVIEW -> renderReview(body)
            JourneyScreen.ROUTE -> renderRoute(body)
            JourneyScreen.SETTINGS -> renderSettings(body)
        }
        setContentView(root); root.requestApplyInsets(); applyLock()
    }
    private fun renderInput(body: LinearLayout) {
        heading(body, "가는 길에, 무엇을 할까요?", "말하거나 적으면 방문 순서와 시간을 맞춰드려요.")
        val voiceCard = card(body, "말로 간편하게", "할 일과 집에 도착할 시간을 함께 말해 주세요.")
        val voiceActions = row()
        val realVoice = column()
        button(realVoice, "음성으로 말하기", enabled = { vehicle == VehicleState.PARKED }) { openVoice(VoiceSource.PLEOS) }
        voiceActions.addView(realVoice, LinearLayout.LayoutParams(0, -2, 1f))
        if (BuildConfig.DEMO) {
            val exampleVoice = column()
            button(exampleVoice, "예시 음성 흐름 체험") { openVoice(VoiceSource.EXAMPLE) }
            voiceActions.addView(exampleVoice, LinearLayout.LayoutParams(0, -2, 1f).apply { leftMargin = dp(16) })
        }
        voiceCard.add(voiceActions, 0)
        showEditor(body, "오늘 할 일", input)
        val inputNote = text(if (exampleInput) "예시 문장 · 실제 음성 인식·AI 해석 아님" else "음성 입력과 수정은 정차 중에만 해 주세요.", 22f, if (exampleInput) amber else muted)
        body.add(inputNote, 20)
        if (BuildConfig.DEMO) {
            button(body, "질문과 답변 체험 · 예시 해석") { simulate = true; loadFixture(clarify = true) }
            button(body, "예시로 체험 · 모의 안내") { simulate = true; loadFixture() }
            button(body, "마감·미확인 정보 체험 · 예시") { simulate = true; loadFixture(scenario = "deadline_conflict") }
        }
        val next = button(footer, if (exampleInput) "예시 할 일 확인하기" else "할 일 확인하기", true, { input.text.isNotBlank() }) {
            if (exampleInput) {
                simulate = true; loadFixture()
            } else parse()
        }
        // TextWatcher can invalidate an example without rebuilding the active editor.
        input.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                next.text = "할 일 확인하기"
                inputNote.text = "음성 입력과 수정은 정차 중에만 해 주세요."
                inputNote.setTextColor(muted)
            }
            override fun afterTextChanged(s: Editable?) = Unit
        })
    }
    private fun openVoice(source: VoiceSource) {
        if (source == VoiceSource.PLEOS && vehicle != VehicleState.PARKED) return
        voiceDialog?.dismiss()
        val dialog = VoiceInputDialog(this, source, { vehicle }) { draft ->
            if (!allowed() || busy) return@VoiceInputDialog
            input.setText(draft.text)
            voiceExample = draft.source == VoiceSource.EXAMPLE && BuildConfig.DEMO
            prefs.edit().putBoolean("voice-example", voiceExample).apply()
            status = "문장을 적용했습니다. 내용을 확인하고 필요하면 수정해 주세요."
            render()
        }
        voiceDialog = dialog
        dialog.setOnDismissListener { if (voiceDialog === dialog) voiceDialog = null }
        dialog.show()
    }
    private fun renderReview(body: LinearLayout) {
        val remaining = (0 until errands.length()).map { errands.getJSONObject(it) }.filter { it.optString("id") !in state.completed }
        heading(body, if (remaining.isEmpty()) "이제 집으로 갈까요?" else "이렇게 들르고 갈까요?", if (requiresParse) "입력이 바뀌었어요. 이전 화면에서 다시 정리해 주세요." else "꼭 해야 할 일과 머무를 시간을 확인하세요.")
        if (state.completed.isEmpty()) renderIntentReview(body)
        for (item in remaining) {
            val line = row().apply { background = shape(panel); setPadding(dp(24), dp(14), dp(20), dp(14)) }
            val copy = column()
            copy.add(text(item.optString("label"), 28f, ink, true), 4)
            val finish = item.optString("latest_finish").takeUnless { it.isBlank() || it == "null" }
            copy.add(text("${if (item.optBoolean("mandatory", true)) "필수" else "선택"} · ${minutes(item.optInt("dwell_seconds"))}분${finish?.let { " · ${clock(it)} 전 완료" }.orEmpty()}", 22f, muted), 0)
            val selectedPlace = item.optJSONObject("selected_place")
            copy.add(text(selectedPlace?.let { "방문 장소 고정 · ${it.optString("name")}" }
                ?: if (fixture) "예시 장소 · 직접 확인할 수 있어요" else "방문 장소 미선택 · 맡긴 점포를 확인하세요", 22f,
                if (selectedPlace == null && !fixture) amber else muted), 0)
            line.addView(copy, LinearLayout.LayoutParams(0, -2, 1f))
            val actions = column(); button(actions, "수정") { editErrand(item) }
            line.addView(actions, LinearLayout.LayoutParams(dp(130), -2))
            body.add(line, 16)
        }
        if (state.completed.isNotEmpty()) body.add(text("${state.completed.size}개 완료했어요", 22f, muted))
        val dateLabel = runCatching { OffsetDateTime.parse(deadline.text.toString()).format(DateTimeFormatter.ofPattern("M월 d일")) }.getOrDefault("")
        button(body, if (intentReview?.deadline == null && intentReview != null) "귀가 시간 정하기" else "$dateLabel ${clock(deadline.text.toString())}까지 집에 도착") { chooseHomeTime() }
        if (!fixture && (originLat.text.isBlank() || homeLat.text.isBlank())) {
            body.add(text("출발지와 집이 아직 설정되지 않았어요.", 22f, amber))
            button(body, "출발 · 귀가 설정") { flow.settings(); render() }
        }
        if (!fixture && !reviewNotes.isNullOrBlank()) body.add(text(reviewNotes!!, 22f, amber))
        val reviewAction = intentReview?.action ?: ReviewAction.PLAN
        val missingPlace = remaining.firstOrNull { !PlaceReview.allSelected(JSONArray().put(it), fixture) }
        button(footer, when {
            requiresParse -> "할 일 다시 정리하기"
            reviewAction == ReviewAction.ANSWER -> "질문에 답하기"
            reviewAction == ReviewAction.HOME_TIME -> "귀가 시간 정하기"
            reviewAction == ReviewAction.EDIT_REQUEST -> "요청 다시 입력하기"
            !fixture && missingPlace != null -> "방문할 장소 선택"
            else -> "동선 확인하기"
        }, true) {
            when {
                requiresParse || reviewAction == ReviewAction.EDIT_REQUEST -> { flow.start(); render() }
                reviewAction == ReviewAction.ANSWER -> intentReview?.let { answerQuestion(it, it.questions.first()) }
                reviewAction == ReviewAction.HOME_TIME -> chooseHomeTime()
                !fixture && missingPlace != null -> choosePlace(missingPlace)
                else -> calculate()
            }
        }
    }
    private fun renderIntentReview(body: LinearLayout) {
        val review = intentReview ?: return
        val panel = card(body, if (review.example) "예시 요청 해석 · 준비된 내용" else "AI가 이해한 요청",
            if (review.example) "실제 STT·LLM 결과가 아닌 질문·답변 체험입니다." else "AI 초안이에요. 원문과 비교해 빠진 조건이 없는지 확인하세요.")
        panel.add(text("“${review.originalText}”", 22f, muted).apply { maxLines = 2; ellipsize = android.text.TextUtils.TruncateAt.END }, 8)
        val required = (0 until errands.length()).count { errands.getJSONObject(it).optBoolean("mandatory", true) }
        panel.add(text("필수 ${required}개 · 선택 ${errands.length() - required}개 · " + (review.deadline?.let { "${clock(it)}까지 귀가" } ?: "귀가 시간 미정"), 23f, ink, true), 4)
        if (review.deadline == null) panel.add(text("귀가 시간은 추측하지 않아요. 아래에서 직접 정해 주세요.", 22f, amber), 4)
        review.questions.firstOrNull()?.let { question ->
            panel.add(text("확인 ${review.questions.size}개 · $question", 23f, amber, true), 4)
        }
        button(panel, "원문 · 내 답변 보기") {
                val lines = "입력 원문\n${review.originalText}\n\n" + (0 until review.answers.length()).joinToString("\n\n") {
                    review.answers.getJSONObject(it).let { answer -> "${answer.optString("question")}\n→ ${answer.optString("answer")}" }
                }
                val content = text(lines, 24f).apply { setPadding(dp(28), dp(12), dp(28), dp(12)) }
                dialog(AlertDialog.Builder(this).setTitle("원문과 확인한 조건").setView(ScrollView(this).apply { addView(content) }).setPositiveButton("닫기", null))
        }
    }

    private fun answerQuestion(review: IntentReview, question: String) {
        if (review !== intentReview || !allowed()) return
        if (review.example) {
            dialog(AlertDialog.Builder(this).setTitle("예시 확인 질문").setMessage(question + "\n\n준비된 답변을 선택합니다. 실제 AI 호출은 없습니다.")
                .setNegativeButton("취소", null)
                .setNeutralButton("시간 제한은 없어요") { _, _ -> resolveExampleQuestion(review, question, null) }
                .setPositiveButton("18:30 전 수령 완료") { _, _ -> resolveExampleQuestion(review, question, "2026-10-23T18:30:00+09:00") })
            return
        }
        val content = column().apply { setPadding(dp(28), dp(12), dp(28), dp(12)) }
        content.add(text(question, 24f, ink, true))
        val answer = field(content, "내 답변", "", true) {}
        var window: AlertDialog? = null
        button(content, "답변 반영하기", true, { answer.text.isNotBlank() && answer.text.length <= 1000 }) {
            val payload = review.answerPayload(question, answer.text.toString(), errands)
            window?.dismiss()
            submitParse(payload, review)
        }
        window = dialog(AlertDialog.Builder(this).setTitle("요청을 조금 더 알려 주세요").setView(ScrollView(this).apply { addView(content) }).setNegativeButton("취소", null))
        answer.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { applyLock() }
            override fun afterTextChanged(s: Editable?) {}
        })
        applyLock()
    }

    private fun resolveExampleQuestion(review: IntentReview, question: String, finish: String?) {
        if (!allowed() || busy || intentReview !== review || !BuildConfig.DEMO || !fixture) return
        for (i in 0 until errands.length()) if (errands.getJSONObject(i).optString("id") == "laundry")
            errands.getJSONObject(i).put("latest_finish", finish ?: JSONObject.NULL)
        review.response.put("clarification_questions", JSONArray())
        review.answers.put(JSONObject().put("question", question).put("answer", if (finish == null) "시간 제한은 없어요" else "18:30 전 수령 완료"))
        state.edited(); plan = null; persist(); render()
    }
    private fun renderSettings(body: LinearLayout) {
        heading(body, "설정", "정차 중에만 변경할 수 있어요.")
        if (BuildConfig.DEMO) button(body, if (simulate) "모의 안내 끄기" else "모의 안내 켜기") {
            cancelOwnedNavigation()
            simulate = !simulate; state.routeFailed(); state.edited(); flow.edited(); plan = null; persist(); render()
        }
        button(body, "새 일정 시작") { resetJourney() }
        button(body, "연결 · 위치 고급 설정") {
            val advanced = column().apply { setPadding(dp(28), dp(12), dp(28), dp(12)) }
            advanced.add(text("개발용 수동 설정입니다. 위치 자동 감지는 아직 지원하지 않습니다.", 22f, muted))
            for ((label, editor) in listOf("서버 주소" to server, "출발 위도" to originLat, "출발 경도" to originLon, "집 위도" to homeLat, "집 경도" to homeLon, "추가 지연 (분)" to delay)) showEditor(advanced, label, editor)
            dialog(AlertDialog.Builder(this).setTitle("고급 설정").setView(ScrollView(this).apply { addView(advanced) }).setPositiveButton("닫기") { _, _ -> render() })
        }
        body.add(text(if (fixture) "예시 장소·영업시간·이동시간을 사용합니다. 모의 안내는 실제 내비게이션을 요청하지 않습니다." else "실제 데이터는 서버 연결과 API 설정이 필요합니다.", 24f, muted))
        button(footer, "돌아가기", true) { flow.back(); render() }
    }
    private fun renderRoute(body: LinearLayout) {
        val result = plan
        if (result == null) { heading(body, "동선을 다시 확인해 주세요"); button(footer, "할 일 확인하기", true) { flow.parsed(); render() }; return }
        val outcome = result.optString("status")
        val next = firstRemainingStop(result)
        val homeLeg = next == null
        val id = next?.optString("errand_id") ?: "__home"
        heading(body, when { outcome == "infeasible" -> "시간 조정이 필요해요"; state.arrivedId == id -> if (homeLeg) "집에 도착했어요" else "할 일을 마쳤나요?"; state.activeId == id -> "다음 장소로 이동해요"; else -> if (homeLeg) "이제 집으로 가요" else "다음은 이곳이에요" })
        if (outcome != "infeasible") {
            val nextCard = card(body, next?.optString("label") ?: "집으로 돌아가기", next?.optJSONObject("place")?.optString("name"))
            val metrics = row()
            val arrival = column(); arrival.add(text(if (homeLeg) "집 도착 예정" else "도착 예정", 22f, muted), 4)
            arrival.add(text(clock(if (homeLeg) result.optString("home_arrival_time") else next?.optString("arrival_time").orEmpty()), 44f, ink, true), 0)
            metrics.addView(arrival, LinearLayout.LayoutParams(0, -2, 1f))
            val home = column(); home.add(text("집 도착 / 마감", 22f, muted), 4)
            home.add(text("${clock(result.optString("home_arrival_time"))} / ${clock(request?.optString("deadline").orEmpty())}", 32f, ink, true), 0)
            metrics.addView(home, LinearLayout.LayoutParams(0, -2, 1f)); nextCard.add(metrics, 0)
        }
        if (outcome == "infeasible") body.add(text("필수 일정과 귀가 시간을 함께 지킬 수 없어요.", 24f, amber))
        else if (outcome != "feasible") body.add(text("영업·서비스 또는 이동 정보 확인이 필요해요. 가능 여부가 확정되지 않았습니다.", 24f, amber))
        PlanTiming.remainingMarginMillis(request, result)?.let { margin ->
            if (margin <= 300_000 && outcome != "infeasible") body.add(text(
                "가장 촉박한 마감의 여유 ${((margin.coerceAtLeast(0) + 59_999) / 60_000)}분 · 주차·대기와 교통 변동에 주의하세요.", 24f, amber))
        }
        if (state.arrivedId == id && !homeLeg) body.add(text("도착과 할 일 완료는 달라요. 직접 완료를 확인해 주세요.", 24f, muted))
        else if (state.activeId == id) body.add(text(navStatus, 22f, muted))
        if (state.activeId == null && state.arrivedId == null) {
            val explanation = PlanExplanation.lines(request, result)
            val evidence = card(body, "이렇게 계산했어요", explanation.firstOrNull())
            explanation.drop(1).take(2).forEach { evidence.add(text(it, 22f, muted), 4) }
            button(evidence, "계획 근거 자세히") {
                val content = text(explanation.joinToString("\n\n"), 24f).apply { setPadding(dp(28), dp(12), dp(28), dp(12)) }
                dialog(AlertDialog.Builder(this).setTitle("계획의 근거와 한계").setView(ScrollView(this).apply { addView(content) }).setPositiveButton("닫기", null))
            }
        }
        val stops = result.optJSONArray("stops") ?: JSONArray()
        val upcoming = (0 until stops.length()).map { stops.getJSONObject(it) }.filter { it.optString("errand_id") !in state.completed && it.optString("errand_id") != id }
        for (stop in upcoming) {
            val line = row()
            line.addView(text(stop.optString("label"), 24f), LinearLayout.LayoutParams(0, -2, 1f))
            line.addView(text(clock(stop.optString("arrival_time")), 24f, muted)); body.add(line, 12)
        }
        val skipped = result.optJSONArray("skipped_errands")
        if (skipped != null && skipped.length() > 0) body.add(text("포함되지 않은 할 일 ${skipped.length()}개 · 세부 정보 확인", 22f, amber))
        button(body, "전체 일정 · 확인할 정보") { showPlanDetails(result) }
        if (state.activeId == null && state.arrivedId == null) button(body, "최신 시간으로 다시 계산") { calculate() }
        if (!fixture && !navigation.connected) button(body, "차량 내비 다시 연결") { navigation.connect(); status = "내비 연결을 다시 확인하고 있어요."; applyLock() }
        if (state.activeId != null && state.arrivedId == null && !fixture) button(body, "안내 취소 · 동선 다시 확인") {
            dialog(AlertDialog.Builder(this).setTitle("현재 안내를 취소할까요?")
                .setMessage("차량 내비 안내를 취소하고 남은 동선을 다시 계산합니다. 완료하지 않은 볼일은 그대로 남습니다.")
                .setNegativeButton("유지", null).setPositiveButton("취소하고 다시 계산") { _, _ -> if (allowed() && !busy) {
                    runCatching { navigation.cancelPending(); state.routeFailed(); navigation.connect(); calculate() }.onFailure { showError(it) }
                } })
        }
        when {
            outcome == "infeasible" -> button(footer, if (result.has("relaxation") && !result.isNull("relaxation")) "시간 조정 제안 보기" else "할 일 다시 확인", true) {
                val relaxation = result.optJSONObject("relaxation")
                if (relaxation == null) { flow.parsed(); render() } else {
                    val proposed = relaxation.getString("required_deadline")
                    val conditional = relaxation.optBoolean("requires_verification")
                    val proposal = if (conditional) "${clock(proposed)}까지 ${minutes(relaxation.optInt("extension_seconds"))}분 늘리는 조건부 예상입니다. 영업·수령 또는 이동 정보가 미확인이므로 가능 여부는 확정할 수 없어요."
                        else "${clock(proposed)}까지 ${minutes(relaxation.optInt("extension_seconds"))}분 늘리면 입력한 시간 조건을 맞출 수 있어요."
                    dialog(AlertDialog.Builder(this).setTitle(if (conditional) "미확인 정보가 있는 시간 제안" else "귀가 시간을 바꿀까요?").setMessage("$proposal\n\n선택 볼일은 제외한 제안입니다. 승인 후 최신 시간으로 다시 계산해야 합니다.")
                        .setNegativeButton("유지", null).setPositiveButton("바꾸고 재계산") { _, _ -> if (allowed() && !busy) { applyHomeDeadline(proposed); render(); calculate() } })
                }
            }
            state.arrivedId == "__home" -> button(footer, "새 일정 시작", true) { resetJourney() }
            state.arrivedId == id && next != null -> button(footer, "할 일 완료", true) { confirmCompletion(next) }
            state.activeId == id -> button(footer, if (simulate && BuildConfig.DEMO) "도착 체험 · 모의 이벤트" else "차량 내비게이션을 확인하세요", true, { simulate && BuildConfig.DEMO }) { onNavigation("arrived") }
            fixture && !simulate -> button(footer, "모의 안내 켜기", true) { simulate = true; state.routeFailed(); state.edited(); render() }
            homeLeg && (0 until errands.length()).any { errands.getJSONObject(it).let { item -> item.optBoolean("mandatory", true) && item.optString("id") !in state.completed } } -> button(footer, "남은 필수 할 일 확인", true) { flow.parsed(); render() }
            homeLeg && result.isNull("home_arrival_time") -> button(footer, "동선 다시 확인", true) { calculate() }
            !fixture && !navigationAllowed() -> button(footer, "시간이 흘렀어요 · 다시 계산", true) { calculate() }
            else -> button(footer, if (homeLeg) "집으로 안내" else "이곳으로 안내", true) {
                val destination = next ?: JSONObject().put("errand_id", "__home").put("label", "귀가").put("place", requireNotNull(request).getJSONObject("home"))
                confirmNavigation(destination, outcome)
            }
        }
    }
    private fun chooseHomeTime() {
        chooseDeadline(deadline.text.toString(), "집 도착 시간") { selected -> applyHomeDeadline(selected); render() }
    }
    private fun applyHomeDeadline(value: String) {
        intentReview?.selectDeadline(value)
        deadline.setText(value)
        persist()
    }
    private fun chooseDeadline(value: String, title: String, chosen: (String) -> Unit) {
        val current = runCatching { OffsetDateTime.parse(value) }.getOrDefault(OffsetDateTime.now())
        var selectedDate = current.toLocalDate()
        val content = column().apply { setPadding(dp(28), dp(16), dp(28), dp(16)) }
        val timeRow = row()
        fun picker(label: String, max: Int, initial: Int): NumberPicker {
            val slot = column()
            slot.add(text(label, 24f, muted).apply { gravity = Gravity.CENTER }, 0)
            val picker = NumberPicker(this).apply {
                minValue = 0; maxValue = max; this.value = initial
                wrapSelectorWheel = true; descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
                setFormatter { number -> String.format("%02d", number) }
                if (android.os.Build.VERSION.SDK_INT >= 29) { setTextSize(dp(36).toFloat()); setTextColor(ink) }
            }
            slot.addView(picker, LinearLayout.LayoutParams(-1, dp(220)))
            timeRow.addView(slot, LinearLayout.LayoutParams(0, -2, 1f))
            return picker
        }
        val hours = picker("시", 23, current.hour)
        val minutes = picker("분", 59, current.minute)
        content.add(timeRow, 24)
        lateinit var dateButton: Button
        fun dateLabel() = selectedDate.format(DateTimeFormatter.ofPattern("M월 d일")) + " · 날짜 변경"
        dateButton = button(content, dateLabel()) {
            val calendar = android.app.DatePickerDialog(this, { _, year, month, day -> if (allowed() && !busy) {
                selectedDate = java.time.LocalDate.of(year, month + 1, day); dateButton.text = dateLabel()
            } }, selectedDate.year, selectedDate.monthValue - 1, selectedDate.dayOfMonth)
            trackDialog(calendar); calendar.show()
            calendar.window?.setLayout((resources.displayMetrics.widthPixels * .82).toInt().coerceAtMost(dp(1100)), -2)
            for (which in listOf(AlertDialog.BUTTON_POSITIVE, AlertDialog.BUTTON_NEGATIVE)) calendar.getButton(which)?.apply { minHeight = dp(76); minWidth = dp(120); textSize = 24f }
        }
        dialog(AlertDialog.Builder(this).setTitle(title).setView(content).setNegativeButton("취소", null).setPositiveButton("적용") { _, _ ->
            if (allowed() && !busy) chosen(JourneyDrafts.deadline(current.toString(), selectedDate, hours.value, minutes.value))
        })
    }
    private fun editErrand(item: JSONObject) {
        val content = column().apply { setPadding(dp(28), dp(16), dp(28), dp(16)); isFocusableInTouchMode = true }
        var editWindow: AlertDialog? = null
        val selected = item.optJSONObject("selected_place")
        button(content, selected?.let { "방문 장소 · ${it.optString("name")}" } ?: "방문 장소 선택 · 맡긴 점포 확인") {
            editWindow?.dismiss(); reviewPlace(item)
        }
        val name = field(content, "할 일", item.optString("label")) {}
        val query = if (!fixture) field(content, "찾을 장소", item.optString("query")) {} else null
        var dwellSeconds = item.optInt("dwell_seconds", 600)
        fun durationLabel() = if (dwellSeconds % 60 == 0) "${dwellSeconds / 60}분" else "${dwellSeconds / 60}분 ${dwellSeconds % 60}초"
        content.add(text("머무를 시간", 22f, muted), 8)
        val durationRow = row()
        val durationValue = text(durationLabel(), 28f, ink, true).apply { gravity = Gravity.CENTER }
        val minus = column(); button(minus, "− 5분") { dwellSeconds = JourneyDrafts.adjustDwell(dwellSeconds, -5); durationValue.text = durationLabel() }
        val plus = column(); button(plus, "+ 5분") { dwellSeconds = JourneyDrafts.adjustDwell(dwellSeconds, 5); durationValue.text = durationLabel() }
        durationRow.addView(minus, LinearLayout.LayoutParams(dp(180), -2))
        durationRow.addView(durationValue, LinearLayout.LayoutParams(0, -2, 1f))
        durationRow.addView(plus, LinearLayout.LayoutParams(dp(180), -2))
        content.add(durationRow)
        val mandatory = CheckBox(this).apply { text = "꼭 해야 해요"; textSize = 24f; minHeight = dp(76); setTextColor(ink); isChecked = item.optBoolean("mandatory", true) }
        content.add(mandatory)
        var finish = item.optString("latest_finish").takeUnless { it.isBlank() || it == "null" }
        lateinit var finishButton: Button
        finishButton = button(content, finish?.let { "${clock(it)}까지 완료" } ?: "완료 마감 없음") {
            chooseDeadline(finish ?: deadline.text.toString(), "이 일의 완료 시간") { selected ->
                finish = selected
                finishButton.text = "${clock(finish.orEmpty())}까지 완료"
            }
        }
        val editor = AlertDialog.Builder(this).setTitle("할 일 수정").setView(ScrollView(this).apply { addView(content) })
            .setNeutralButton("개별 마감 해제") { _, _ -> if (allowed() && !busy) { item.put("latest_finish", JSONObject.NULL); edited(); render() } }
            .setNegativeButton("취소", null).setPositiveButton("저장") { _, _ -> if (allowed() && !busy) {
                runCatching {
                    require(name.text.isNotBlank()) { "할 일을 입력해 주세요." }
                    val updated = if (query == null) JSONObject(item.toString()) else PlaceReview.withQuery(item, query.text.toString())
                    val described = PlaceReview.withLabel(updated, name.text.toString())
                    described.put("dwell_seconds", dwellSeconds).put("mandatory", mandatory.isChecked).put("latest_finish", finish ?: JSONObject.NULL)
                    saveErrand(described)
                }.onFailure { showError(it) }
            } }
        content.requestFocus(); editWindow = dialog(editor)
    }

    private fun saveErrand(item: JSONObject) {
        check(allowed() && !busy)
        val index = (0 until errands.length()).first { errands.getJSONObject(it).optString("id") == item.optString("id") }
        errands.put(index, item)
        edited(); render()
    }

    private fun reviewPlace(item: JSONObject) {
        val selected = item.optJSONObject("selected_place")
        if (selected == null) { choosePlace(item); return }
        val content = column().apply { setPadding(dp(28), dp(16), dp(28), dp(16)) }
        content.add(text(item.optString("label"), 26f, ink, true))
        content.add(text(selected.optString("name"), 28f, ink, true))
        selected.optString("address").takeIf { it.isNotBlank() }?.let { content.add(text(it, 24f, muted)) }
        content.add(text("이 볼일을 처리할 정확한 점포로 고정했어요. 가까운 다른 점포로 자동 교체하지 않습니다.", 23f))
        val evidence = selected.optJSONObject("availability_evidence")
        content.add(text(if (fixture) "예시 장소·영업시간입니다. 실제 확인 결과가 아닙니다."
            else evidence?.let { "직접 확인한 근거 · ${it.optString("basis")}\n확인 시각 · ${displayDateTime(it.optString("confirmed_at"))}" }
            ?: "영업·수령 가능 시간은 아직 미확인입니다. 검색 결과만으로 가능하다고 판단하지 않아요.", 23f, amber))
        content.add(text("이용 시간 ${evidenceLabel(selected.optString("hours_status"))} · 수령/구매 ${evidenceLabel(selected.optString("service_status"))}", 23f))
        var window: AlertDialog? = null
        button(content, "이용 · 수령 가능 시간 확인") { window?.dismiss(); editAvailability(item) }
        button(content, "다른 장소 선택") { window?.dismiss(); choosePlace(item) }
        window = dialog(AlertDialog.Builder(this).setTitle("방문 장소와 확인 근거").setView(ScrollView(this).apply { addView(content) }).setPositiveButton("닫기", null))
    }

    private fun choosePlace(item: JSONObject) {
        if (fixture) {
            val candidates = item.optJSONArray("candidates") ?: JSONArray()
            require(candidates.length() > 0) { "예시 장소가 없습니다. 예시 일정을 다시 선택해 주세요." }
            showPlaceChoices(item, candidates)
            return
        }
        val origin = location(request?.optJSONObject("origin")?.optString("id") ?: "origin", "출발", originLat, originLon)
        val payload = JSONObject().put("query", item.getString("query")).put("origin", origin)
        async({ api().call("/v1/places", payload) }) { response -> showPlaceChoices(item, response.getJSONArray("places")) }
    }

    private fun showPlaceChoices(item: JSONObject, choices: JSONArray) {
        val content = column().apply { setPadding(dp(28), dp(16), dp(28), dp(16)) }
        content.add(text("${item.optString("label")}를 실제로 처리할 점포를 골라 주세요. 맡긴 세탁물·예약 물품은 해당 점포를 확인하세요.", 24f))
        if (fixture) content.add(text("직접 선택한 예시 장소입니다. 실제 장소 검색 결과가 아닙니다.", 22f, amber))
        if (choices.length() == 0) content.add(text("검색 결과가 없어요. 할 일 수정에서 점포명과 지역을 구체적으로 입력해 주세요.", 24f, amber))
        var window: AlertDialog? = null
        for (i in 0 until choices.length()) {
            val place = choices.getJSONObject(i)
            val label = place.getString("name") + place.optString("address").takeIf { it.isNotBlank() }?.let { "\n$it" }.orEmpty()
            button(content, label) {
                val updated = PlaceReview.pin(item, place, fixture && BuildConfig.DEMO)
                window?.dismiss(); saveErrand(updated); reviewPlace(updated)
            }
        }
        window = dialog(AlertDialog.Builder(this).setTitle("방문 장소 선택").setView(ScrollView(this).apply { addView(content) }).setNegativeButton("취소", null))
    }

    private fun editAvailability(item: JSONObject) {
        val place = item.getJSONObject("selected_place")
        var start: String? = null
        var end: String? = null
        val content = column().apply { setPadding(dp(28), dp(16), dp(28), dp(16)) }
        content.add(text("${place.getString("name")} · ${item.optString("label")}", 26f, ink, true))
        content.add(text(if (fixture) "예시 데이터 편집 체험입니다. 실제 영업·서비스 확인을 뜻하지 않습니다."
            else "직접 확인한 이용 가능 시간과 근거를 적어 주세요. 주차·대기·수령에 걸리는 시간은 ‘머무를 시간’에 포함해 주세요.", 23f, amber))
        lateinit var startButton: Button
        lateinit var endButton: Button
        startButton = button(content, "이용 가능 시작 시각 선택") {
            chooseDeadline(start ?: deadline.text.toString(), "이용 가능 시작") { start = it; startButton.text = "시작 · ${displayDateTime(it)}" }
        }
        endButton = button(content, "이용 가능 종료 시각 선택") {
            chooseDeadline(end ?: deadline.text.toString(), "이용 가능 종료") { end = it; endButton.text = "종료 · ${displayDateTime(it)}" }
        }
        val basis = field(content, "직접 확인한 근거 (예: 오늘 매장에 전화해 수령 시간 확인)", "", true) {}
        val service = CheckBox(this).apply { text = "이 점포에서 이 볼일을 처리할 수 있다고 직접 확인했어요"; textSize = 24f; minHeight = dp(80); setTextColor(ink) }
        controls += service to { true }; content.add(service)
        var window: AlertDialog? = null
        button(content, "확인 근거 저장", true) {
            require(start != null && end != null) { "직접 확인한 시작·종료 시각을 모두 선택해 주세요." }
            val confirmed = if (fixture) request?.optString("departure_time") ?: OffsetDateTime.now().toString() else OffsetDateTime.now().toString()
            val updated = PlaceReview.confirmAvailability(item, start!!, end!!, basis.text.toString(), confirmed, service.isChecked)
            window?.dismiss(); saveErrand(updated)
        }
        window = dialog(AlertDialog.Builder(this).setTitle("이용 시간과 수령 가능 여부").setView(ScrollView(this).apply { addView(content) }).setNegativeButton("취소", null))
    }

    private fun displayDateTime(value: String) = runCatching {
        OffsetDateTime.parse(value).format(DateTimeFormatter.ofPattern("M월 d일 HH:mm"))
    }.getOrDefault(value)
    private fun showPlanDetails(result: JSONObject) {
        val lines = mutableListOf(if (fixture) "예시 데이터입니다. 실제 장소·영업·도로 정보가 아닙니다." else "실제 제공자 조회 결과입니다. 미확인 정보는 확정된 사실이 아닙니다.")
        val warnings = result.optJSONArray("warnings") ?: JSONArray()
        for (i in 0 until warnings.length()) lines += humanMessage(warnings.getString(i))
        val stops = result.optJSONArray("stops") ?: JSONArray()
        for (i in 0 until stops.length()) {
            val stop = stops.getJSONObject(i); val place = stop.getJSONObject("place"); val source = place.optJSONObject("source")
            lines += "${stop.optString("label")} · ${place.optString("name")}\n${clock(stop.optString("arrival_time"))} 도착 · ${clock(stop.optString("end_time"))} 완료\n이동 ${minutes(stop.optInt("travel_seconds"))}분 · 대기 ${minutes(stop.optInt("wait_seconds"))}분 · 체류 ${minutesBetween(stop.optString("start_time"), stop.optString("end_time"))}분\n영업 ${evidenceLabel(place.optString("hours_status"))} · 서비스 ${evidenceLabel(place.optString("service_status"))}\n출처 ${providerName(source?.optString("provider"))} · 조회 ${source?.optString("retrieved_at").orEmpty()}"
        }
        val skipped = result.optJSONArray("skipped_errands") ?: JSONArray()
        for (i in 0 until skipped.length()) skipped.getJSONObject(i).let { lines += "제외: ${errandLabel(it.optString("id"))} · ${humanMessage(it.optString("reason"))}" }
        val content = text(lines.joinToString("\n\n"), 24f).apply { setPadding(dp(28), dp(12), dp(28), dp(12)) }
        dialog(AlertDialog.Builder(this).setTitle("일정과 데이터 확인").setView(ScrollView(this).apply { addView(content) }).setPositiveButton("닫기", null))
    }
    private fun resetJourney() {
        dialog(AlertDialog.Builder(this).setTitle("새 일정을 시작할까요?").setMessage("현재 일정과 완료 기록을 초기화합니다.").setNegativeButton("유지", null).setPositiveButton("새로 시작") { _, _ -> if (allowed() && !busy) {
            cancelOwnedNavigation()
            clearFixtureDrafts()
            state.completed.clear(); state.routeFailed(); state.edited(); request = null; plan = null; errands = JSONArray(); fixture = false; requiresParse = false; voiceExample = false; intentReview = null
            prefs.edit().putString("input", "").putBoolean("voice-example", false).putString("delay", "0").apply(); errorMessage = null; reviewNotes = null; simulate = false; computedElapsedMillis = null; flow.start(); persist(); render()
        } })
    }
    private fun clearFixtureDrafts() {
        val values = prefs.all.filterValues { it is String }.mapValues { it.value as String }
        val clean = JourneyDrafts.withoutFixtureLocations(values, fixture)
        val edit = prefs.edit()
        (values.keys - clean.keys).forEach { edit.remove(it) }
        edit.apply()
    }
    private fun applyLock() {
        if (!::lockLabel.isInitialized) return
        val can = allowed()
        lockLabel.text = when {
            fixture -> "예시 데이터 · ${if (simulate) "모의 안내" else "실제 내비 요청 없음"}"
            simulate && BuildConfig.DEMO -> "모의 안내 · 실제 내비 요청 없음"
            vehicle == VehicleState.RESTRICTED -> "주행 중 · 조작 잠금"
            !can -> "차량 상태 확인 중 · 조작 잠금"
            BuildConfig.DEMO -> "체험 버전"
            else -> ""
        }
        if (!can) {
            voiceDialog?.dismiss()
            openDialogs.toList().forEach { it.dismiss() }
            (getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager).hideSoftInputFromWindow(root.windowToken, 0)
        }
        if (vehicle != VehicleState.PARKED) voiceDialog?.dismiss()
        lockLabel.visibility = if (lockLabel.text.isBlank()) View.GONE else View.VISIBLE
        bodyScroll.visibility = if (can) View.VISIBLE else View.GONE
        lockedPanel.visibility = if (can) View.GONE else View.VISIBLE
        footer.visibility = if (can) View.VISIBLE else View.GONE
        demoRescue?.visibility = if (vehicle == VehicleState.UNKNOWN && !busy) View.VISIBLE else View.GONE
        controls.forEach { (view, enabled) -> view.isEnabled = can && !busy && enabled(); view.alpha = if (view.isEnabled) 1f else .4f }
        statusLabel.text = if (busy) "확인 중이에요…" else (errorMessage ?: state.navigationError).orEmpty()
        statusLabel.visibility = if (statusLabel.text.isBlank()) View.GONE else View.VISIBLE
    }
    private fun api() = ApiClient(server.text.toString())
    private fun async(work: () -> JSONObject, done: (JSONObject) -> Unit) {
        val revision = state.revision
        busy = true; errorMessage = null; (getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager).hideSoftInputFromWindow(root.windowToken, 0); applyLock()
        worker.submit {
            val response = runCatching(work)
            runOnUiThread {
                if (isDestroyed) return@runOnUiThread
                busy = false
                if (!state.acceptsResponse(revision)) {
                    status = "입력이 바뀌어 이전 서버 응답을 버렸습니다. 다시 계산하세요."; applyLock(); return@runOnUiThread
                }
                response.onSuccess { runCatching { done(it) }.onFailure { error -> showError(error) } }.onFailure { showError(it) }
                applyLock()
            }
        }
    }
    private fun parse() {
        val payload = JSONObject().put("text", input.text.toString()).put("reference_time", OffsetDateTime.now().toString())
        require(payload.getString("text").isNotBlank()) { "할 일을 입력하세요." }
        submitParse(payload, null)
    }
    private fun submitParse(payload: JSONObject, prior: IntentReview?) {
        val client = api()
        async({ client.call("/v1/parse", payload) }) { result ->
            cancelOwnedNavigation()
            clearFixtureDrafts()
            fixture = false; request = null; plan = null; state.routeFailed(); state.completed.clear(); state.edited()
            requiresParse = false
            errands = result.getJSONArray("errands")
            intentReview = prior?.acceptAnswer(result, payload) ?: IntentReview(payload.getString("text"), payload.getString("reference_time"), false, result)
            request = JSONObject().put("deadline", intentReview?.deadline ?: JSONObject.NULL)
            prefs.edit().putString("draft-deadline", intentReview?.deadline).apply()
            val warnings = result.optJSONArray("warnings") ?: JSONArray()
            status = ((0 until warnings.length()).joinToString(" · ") { humanMessage(warnings.getString(it)) } + " " + result.optJSONArray("clarification_questions")?.join(" · ").orEmpty()).trim()
            if (status.isBlank()) status = "정리된 내용을 확인하고 출발 · 귀가 위치를 입력하세요."
            reviewNotes = status.takeIf { warnings.length() > 0 || (result.optJSONArray("clarification_questions")?.length() ?: 0) > 0 }
            flow.parsed(); persist(); render()
        }
    }
    private fun loadFixture(clarify: Boolean = false, scenario: String = "basic") {
        check(BuildConfig.DEMO)
        val client = api()
        async({ client.call("/v1/demo?scenario=$scenario") }) { result ->
            cancelOwnedNavigation()
            request = result; errands = result.getJSONArray("errands"); fixture = true; plan = null
            intentReview = null
            voiceExample = false
            prefs.edit().putBoolean("voice-example", false).apply()
            requiresParse = false
            syncDraftLocations(result)
            prefs.edit().putString("draft-deadline", result.getString("deadline")).putString("input", "예시: " + (0 until errands.length()).joinToString(" · ") { errands.getJSONObject(it).optString("label") }).apply()
            state.completed.clear(); state.routeFailed(); state.edited()
            status = "예시 데이터를 불러왔습니다. 영업시간 · 이동 시간은 시연용 fixture입니다."
            if (clarify) {
                val interpreted = JSONObject().put("errands", errands).put("deadline", JSONObject.NULL)
                    .put("clarification_questions", JSONArray().put(IntentReview.EXAMPLE_QUESTION)).put("warnings", JSONArray())
                intentReview = IntentReview(IntentReview.EXAMPLE_TEXT, result.getString("departure_time"), true, interpreted)
                prefs.edit().putString("input", IntentReview.EXAMPLE_TEXT).apply()
            }
            prefs.edit().putString("delay", "0").apply(); flow.parsed(); persist(); render()
        }
    }
    private fun location(id: String, name: String, latitude: EditText, longitude: EditText): JSONObject {
        val lat = latitude.text.toString().toDoubleOrNull() ?: error("$name 위도를 입력하세요.")
        val lon = longitude.text.toString().toDoubleOrNull() ?: error("$name 경도를 입력하세요.")
        require(lat in -90.0..90.0 && lon in -180.0..180.0) { "$name 좌표 범위를 확인하세요." }
        return JSONObject().put("id", id).put("name", name).put("latitude", lat).put("longitude", lon)
    }
    private fun calculate() {
        check(allowed())
        require(!requiresParse) { "입력 문장이 바뀌었습니다. 먼저 할 일을 다시 정리하세요." }
        require(intentReview?.ready != false) { "확인 질문과 귀가 시간에 먼저 답해 주세요." }
        val remaining = if (fixture) PlaceReview.fixtureErrands(errands, state.completed) else PlaceReview.discoveryErrands(errands, state.completed)
        require(fixture || PlaceReview.allSelected(remaining, false)) { "볼일을 처리할 정확한 방문 장소를 먼저 선택해 주세요." }
        val deadlineValue = OffsetDateTime.parse(deadline.text.toString()).toString()
        val delayMinutes = delay.text.toString().toLongOrNull() ?: error("지연 시간은 분 단위 숫자여야 합니다.")
        require(delayMinutes in 0..1440) { "지연 시간은 0~1440분이어야 합니다." }
        val payload = if (fixture) JSONObject(requireNotNull(request).toString()) else JSONObject()
        val startedWall = System.currentTimeMillis()
        val startedElapsed = SystemClock.elapsedRealtime()
        val departure = if (fixture) OffsetDateTime.parse(payload.getString("departure_time")) else OffsetDateTime.now()
        payload.put("origin", location(request?.optJSONObject("origin")?.optString("id") ?: "origin", "출발", originLat, originLon))
            .put("home", location(request?.optJSONObject("home")?.optString("id") ?: "home", "집", homeLat, homeLon))
            .put("departure_time", departure.plusMinutes(delayMinutes).toString()).put("deadline", deadlineValue).put("errands", remaining)
        if (fixture) {
            val original = requireNotNull(request).getJSONObject("origin")
            val oldHome = requireNotNull(request).getJSONObject("home")
            require(payload.getJSONObject("origin").getDouble("latitude") == original.getDouble("latitude") &&
                payload.getJSONObject("origin").getDouble("longitude") == original.getDouble("longitude") &&
                payload.getJSONObject("home").getDouble("latitude") == oldHome.getDouble("latitude") &&
                payload.getJSONObject("home").getDouble("longitude") == oldHome.getDouble("longitude")) {
                "예시 좌표를 바꾸면 이동 시간 fixture를 사용할 수 없습니다. 예시를 다시 불러오세요."
            }
        }
        // Validate the full draft before asking to cancel an already approved route.
        if (navigation.hasActiveRequest) {
            dialog(AlertDialog.Builder(this).setTitle("현재 안내를 취소하고 다시 계산할까요?")
                .setMessage("이미 승인한 안내가 진행 중입니다. 취소한 뒤 새 동선을 확인하고 목적지를 다시 승인해야 해요.")
                .setNegativeButton("안내 유지", null).setPositiveButton("취소하고 다시 계산") { _, _ -> if (allowed() && !busy) {
                    runCatching { cancelOwnedNavigation(); state.routeFailed(); navigation.connect(); calculate() }.onFailure { showError(it) }
                } })
            return
        }
        val client = api()
        async({
            val ready = if (fixture) PlanPayload.trimFixtureEdges(payload) else client.call("/v1/discover", payload)
            val result = client.call("/v1/plan", ready)
            JSONObject().put("request", ready).put("plan", result)
        }) { result ->
            require(!result.getJSONObject("plan").optBoolean("is_fixture") || BuildConfig.DEMO) { "LIVE 앱에서는 fixture 결과를 사용할 수 없습니다." }
            state.edited(); state.routeFailed(); request = result.getJSONObject("request"); plan = result.getJSONObject("plan")
            fixture = plan?.optBoolean("is_fixture", fixture) ?: fixture
            computedElapsedMillis = startedElapsed
            computationStartedWallMillis = startedWall
            status = "계획을 계산했습니다. 데이터 경고와 마감을 확인한 뒤 다음 목적지를 승인하세요."
            flow.planned(); persist(); render()
        }
    }

    private fun renderPlan() {
        if (flow.screen == JourneyScreen.ROUTE) render() else applyLock()
    }
    private fun firstRemainingStop(result: JSONObject): JSONObject? {
        val stops = result.optJSONArray("stops") ?: return null
        for (i in 0 until stops.length()) if (stops.getJSONObject(i).getString("errand_id") !in state.completed) return stops.getJSONObject(i)
        return null
    }
    private fun confirmNavigation(stop: JSONObject, outcome: String) {
        ensureNavigationAllowed()
        val currentRevision = state.revision
        val place = stop.getJSONObject("place")
        val warning = if (outcome != "feasible") "영업시간 또는 서비스가 미확인입니다. 가능 여부가 검증되지 않았습니다.\n\n" else ""
        dialog(AlertDialog.Builder(this).setTitle("다음 목적지로 안내할까요?")
            .setMessage("$warning${place.optString("name")}\n\n${if (simulate) "모의 안내입니다. 실제 내비는 요청하지 않습니다." else "이 목적지를 차량 내비게이션에 보냅니다."}\n도착한 뒤 할 일 완료를 직접 확인해 주세요.")
            .setNegativeButton("다시 확인", null).setPositiveButton("목적지 승인") { _, _ ->
                if (!allowed() || busy || !state.acceptsResponse(currentRevision)) return@setPositiveButton
                if (!navigationAllowed()) {
                    showError(IllegalStateException(navigationBlockMessage())); return@setPositiveButton
                }
                val id = stop.getString("errand_id")
                state.approve(id)
                if (!state.requestNavigation(id)) return@setPositiveButton
                if (simulate && BuildConfig.DEMO) { onNavigation("requested"); onNavigation("accepted") }
                else runCatching { navigation.request(place.getDouble("latitude"), place.getDouble("longitude"), place.getString("name")) }.onFailure {
                    state.routeFailed(); showError(it)
                }
                renderPlan()
            })
    }
    private fun confirmCompletion(stop: JSONObject) {
        dialog(AlertDialog.Builder(this).setTitle("할 일을 마쳤나요?").setMessage("${stop.optString("label")}\n\n완료를 확인하면 남은 할 일만 다시 계산할 수 있습니다.")
            .setNegativeButton("아직이에요", null).setPositiveButton("완료했어요") { _, _ -> if (allowed() && !busy) {
                val id = stop.getString("errand_id")
                state.complete(id)
                if (id !in state.completed) return@setPositiveButton
                val place = stop.getJSONObject("place")
                request?.put("origin", JSONObject().put("id", place.getString("id")).put("name", place.getString("name")).put("latitude", place.getDouble("latitude")).put("longitude", place.getDouble("longitude")))
                request?.let { syncDraftLocations(it) }
                if (fixture) request?.put("departure_time", stop.getString("end_time"))
                plan = null; status = "완료를 저장했어요. 선택한 장소를 유지하고 남은 동선을 새로 확인합니다."
                flow.parsed(); persist(); render()
                if (!fixture) navigation.connect()
                runCatching { calculate() }.onFailure { showError(it) }
            } })
    }
    private fun onNavigation(event: String) {
        when {
            event == "connected" -> navStatus = "Pleos 서비스 연결됨 · 경로 요청 전"
            event == "requested" -> navStatus = if (simulate) "시뮬레이션 · 요청 발생" else "Pleos 요청 전송됨 · 경로 수락 미확인"
            event == "accepted" -> { state.routeAccepted(); navStatus = if (simulate) "시뮬레이션 · 경로 탐색 완료 이벤트" else "Pleos 경로 탐색 완료 이벤트 수신" }
            event == "arrived" -> { state.arrived(); navStatus = "목적지 도착 이벤트 수신 · 사용자의 할 일 완료 확인 필요" }
            event.startsWith("error:") -> { navStatus = event.removePrefix("error:"); state.routeFailed(navStatus) }
        }
        renderPlan(); persist()
    }
    private fun cancelOwnedNavigation() {
        if (::navigation.isInitialized && allowed() && navigation.hasActiveRequest) navigation.cancelPending()
    }
    private fun persist() {
        prefs.edit().putString("request", request?.toString()).putString("plan", plan?.toString()).putString("errands", errands.toString())
            .putBoolean("fixture", fixture).putBoolean("requiresParse", requiresParse).putStringSet("completed", state.completed.toSet()).apply()
        prefs.edit().putString("review-notes", reviewNotes).putString("intent-review", intentReview?.toJson()).apply()
    }
    private fun restore() {
        runCatching {
            voiceExample = BuildConfig.DEMO && prefs.getBoolean("voice-example", false) && prefs.getString("input", "") == VoiceInputSession.EXAMPLE_TEXT
            fixture = prefs.getBoolean("fixture", false)
            requiresParse = prefs.getBoolean("requiresParse", false)
            reviewNotes = prefs.getString("review-notes", null)
            intentReview = prefs.getString("intent-review", null)?.let(IntentReview::restore)
            if (intentReview?.example == true && !BuildConfig.DEMO) intentReview = null
            if (fixture && !BuildConfig.DEMO) return@runCatching
            request = prefs.getString("request", null)?.let(::JSONObject)
            plan = prefs.getString("plan", null)?.let(::JSONObject)
            if (!fixture) { plan = null; status = "이전 진행을 복원했습니다. 최신 정보로 동선을 다시 계산하세요." }
            errands = JSONArray(prefs.getString("errands", "[]"))
            state.completed.addAll(prefs.getStringSet("completed", emptySet()).orEmpty())
            restored = errands.length() > 0
        }.onFailure { request = null; plan = null; errands = JSONArray() }
    }
    private fun showError(error: Throwable) { status = error.message ?: "처리 중 오류가 발생했습니다. 다시 확인하세요."; errorMessage = status; applyLock(); Toast.makeText(this, status, Toast.LENGTH_LONG).show() }
    private fun navigationAllowed() = NavigationPolicy.canApprove(fixture, BuildConfig.DEMO, simulate,
        System.currentTimeMillis(), runCatching { OffsetDateTime.parse(request?.optString("deadline")).toInstant().toEpochMilli() }.getOrNull(),
        SystemClock.elapsedRealtime(), computedElapsedMillis) && (fixture || PlanTiming.canApprove(request, plan,
        System.currentTimeMillis(), SystemClock.elapsedRealtime(), computationStartedWallMillis, computedElapsedMillis))
    private fun navigationBlockMessage() = if (fixture) "예시 목적지는 실제 내비로 보낼 수 없습니다. 데모 조작 시뮬레이션을 켜 주세요." else "계산·검토 중 시간이 흘렀거나 마감 여유가 부족해졌어요. ‘최신 시간으로 다시 계산’을 눌러 주세요."
    private fun ensureNavigationAllowed() { check(navigationAllowed()) { navigationBlockMessage() } }
    private fun evidenceLabel(value: String) = when(value) { "verified" -> "확인됨"; "closed" -> "영업 종료"; "unavailable" -> "제공 안 함"; else -> "미확인" }
    private fun providerName(value: String?) = when(value) { "synthetic-demo" -> "시연용 예시"; "kakao-local" -> "카카오 장소 검색"; null, "" -> "미확인"; else -> value }
    private fun errandLabel(id: String): String {
        for (i in 0 until errands.length()) if (errands.getJSONObject(i).optString("id") == id) return errands.getJSONObject(i).optString("label")
        return id
    }
    private fun humanMessage(value: String): String = when {
        value.startsWith("DEMO FIXTURE") -> "시연용 예시 데이터입니다. 실제 장소·영업·도로 상황이 아닙니다."
        value.startsWith("Editable AI draft") -> "AI가 정리한 초안입니다. 빠진 일, 필수 여부, 체류 시간과 마감을 원문과 비교하세요."
        value.contains("opening hours need verification") -> "${errandLabel(value.substringBefore('/'))}: 영업시간을 직접 확인해야 합니다."
        value.contains("requested service needs verification") -> "${errandLabel(value.substringBefore('/'))}: 필요한 서비스 제공 여부를 확인해야 합니다."
        value.contains("availability evidence is missing") -> "${errandLabel(value.substringBefore('/'))}: 직접 확인한 이용·수령 가능 근거가 없습니다."
        value.contains("availability evidence is in the future") -> "${errandLabel(value.substringBefore('/'))}: 확인 시각이 출발보다 늦습니다. 시간과 확인 근거를 다시 확인하세요."
        value.contains("availability evidence is older than 24 hours") -> "${errandLabel(value.substringBefore('/'))}: 이용·수령 가능 여부를 확인한 지 하루가 지났어요. 다시 확인해 주세요."
        value.contains("road snapshot is stale") -> "도로 정보가 출발 시점과 15분 이상 차이 납니다. 최신 정보로 다시 조회하세요."
        value.contains("current-traffic snapshot") -> "현재 교통 정보는 나중에 출발하는 구간의 예측이 아닙니다. 이동 시간 확인이 필요합니다."
        value.startsWith("Missing road travel time") -> "일부 구간의 도로 이동 시간이 없어 가능 여부를 검증할 수 없습니다."
        value.contains("no place candidates") -> "${errandLabel(value.substringBefore(':'))}: 장소 후보가 없습니다. 검색어를 수정하고 다시 조회하세요."
        value.startsWith("No route satisfies") -> "필수 일정·영업시간·체류 시간·귀가 마감을 모두 만족하는 동선이 없습니다."
        value.startsWith("Mandatory errand") -> "${errandLabel(value.removePrefix("Mandatory errand ").substringBefore(" was"))}: 반드시 할 일이 계획에 포함되지 않았습니다."
        value.startsWith("No complete mandatory route") -> "필수 일정을 모두 완료하는 동선을 찾지 못했습니다."
        value.startsWith("Optional errand excluded") -> "선택 일정 제외: 장소·시간 조건을 확인하거나 여유 시간을 늘려 보세요."
        else -> value
    }
    private fun draft(key: String, location: String, coordinate: String) = prefs.getString("draft-$key", request?.optJSONObject(location)?.optString(coordinate).orEmpty()).orEmpty()
    private fun saveDraft(key: String, value: String) { prefs.edit().putString("draft-$key", value).apply(); edited() }
    private fun syncDraftLocations(payload: JSONObject) {
        val edit = prefs.edit()
        for ((key, location, coordinate) in listOf(Triple("originLat", "origin", "latitude"), Triple("originLon", "origin", "longitude"), Triple("homeLat", "home", "latitude"), Triple("homeLon", "home", "longitude"))) {
            payload.optJSONObject(location)?.let { edit.putString("draft-$key", it.optString(coordinate)) }
        }
        edit.apply()
    }
    private fun clock(value: String): String = runCatching { OffsetDateTime.parse(value).format(DateTimeFormatter.ofPattern("HH:mm")) }.getOrDefault("미확인")
    private fun minutes(seconds: Int) = (seconds + 59) / 60
    private fun minutesBetween(start: String, end: String) = runCatching { java.time.Duration.between(OffsetDateTime.parse(start), OffsetDateTime.parse(end)).toMinutes().toString() }.getOrDefault("?")
}

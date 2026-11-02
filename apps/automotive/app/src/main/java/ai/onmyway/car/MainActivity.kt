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
    private val palette get() = CockpitTheme.forNight(dark)
    private val bg get() = Color.parseColor(palette.canvas)
    private val panel get() = Color.parseColor(palette.surface)
    private val ink get() = Color.parseColor(palette.ink)
    private val muted get() = Color.parseColor(palette.muted)
    private val soft get() = Color.parseColor(palette.secondary)
    private val amber get() = Color.parseColor(palette.warning)
    private val flow = JourneyFlow()
    private var errorMessage: String? = null
    private var reviewNotes: String? = null
    private var intentReview: IntentReview? = null
    private var pendingParse: JSONObject? = null
    private var pendingPrior: IntentReview? = null
    private val openDialogs = mutableListOf<android.app.Dialog>()
    private val state = SessionState()
    private val worker = Executors.newSingleThreadExecutor()
    private val controls = mutableListOf<Pair<View, () -> Boolean>>()
    private val prefs by lazy { getSharedPreferences("onmyway", MODE_PRIVATE) }
    private val sampleExperience get() = BuildConfig.DEMO && prefs.getBoolean("sample-experience", false)
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
    private val speechOutput by lazy { PleosSpeechOutput(this) }
    private val spokenExchange = SpokenExchange()
    private var voiceConversation = false
    private var foreground = false
    private var spokenStatus: String? = null
    private var status = "정차 상태를 확인한 뒤 계획을 시작하세요."
    private var navStatus = "Pleos 내비 연결 확인 중"
    private val navigationHandoff = NavigationHandoff()
    private var navigationRecovery = false
    private var mapMessage: String? = null
    private val navigationConnected get() = ::navigation.isInitialized && navigation.connected
    private val locationProvider by lazy { PleosLocation(this) }
    private val locationAttempt = LocationAttempt()
    private var locationEpoch = 0L
    private var locationBusy = false
    private var locationMessage: String? = null
    private var locationStatusView: TextView? = null
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
    override fun onResume() { super.onResume(); foreground = true; if (::root.isInitialized) applyLock() }
    override fun onPause() { foreground = false; cancelLocationWork(); navigationHandoff.cancel(); stopSpoken(true); voiceDialog?.dismiss(); super.onPause() }
    override fun onStop() { foreground = false; navigationHandoff.cancel(); stopSpoken(true); voiceDialog?.dismiss(); if (::restrictions.isInitialized) restrictions.stop(); vehicle = VehicleState.UNKNOWN; applyLock(); super.onStop() }
    override fun onDestroy() { cancelLocationWork(); navigation.close(); worker.shutdownNow(); super.onDestroy() }

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
            text = label; textSize = CockpitTheme.ACTION_SP; isAllCaps = false; minHeight = dp(CockpitTheme.TOUCH_DP); minimumHeight = dp(CockpitTheme.TOUCH_DP)
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
        parent.add(text(title, CockpitTheme.HEADING_SP, ink, true), 16)
        if (description != null) parent.add(text(description, 24f, muted), 28)
    }
    private fun allowed() = state.canInteract(vehicle, BuildConfig.DEMO, simulate)
    private fun edited(continueVoice: Boolean = false) {
        navigationHandoff.cancel(); mapMessage = null
        stopSpoken(!continueVoice)
        voiceDialog?.dismiss()
        val wasRoute = flow.screen == JourneyScreen.ROUTE
        state.edited(); plan = null; flow.edited(); errorMessage = null
        persist()
        if (wasRoute) render() else applyLock()
    }
    @Deprecated("Uses the app journey instead of closing an in-progress drive")
    override fun onBackPressed() { if (allowed() && !busy) { stopSpoken(true); flow.back(); render() } }

    private fun render() {
        cancelLocationWork()
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
            button(backHost, "‹ 뒤로") { stopSpoken(true); flow.back(); render() }
            header.addView(backHost, LinearLayout.LayoutParams(dp(150), -2).apply { rightMargin = dp(24) })
        }
        header.addView(text("OnMyWay", 28f, ink, true), LinearLayout.LayoutParams(0, -2, 1f))
        header.addView(text(when(flow.screen) { JourneyScreen.INPUT -> "요청"; JourneyScreen.REVIEW -> "조건 확인"; JourneyScreen.ROUTE -> "계획 · 안내"; else -> "설정" }, 22f, muted))
        if (flow.screen != JourneyScreen.SETTINGS && flow.screen != JourneyScreen.LOCATIONS) {
            val settingsHost = column()
            button(settingsHost, "설정") { stopSpoken(true); flow.settings(); render() }
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
            pendingParse = null; pendingPrior = null
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
                text = "샘플로 둘러보기"; textSize = 25f; minHeight = dp(80)
                setTextColor(ink); backgroundTintList = null; background = shape(soft, 40)
                setOnClickListener { if (vehicle == VehicleState.UNKNOWN && !busy) { prefs.edit().putBoolean("sample-experience", true).apply(); simulate = true; flow.start(); render() } }
            }
            lockedPanel.add(demoRescue!!)
        }
        root.addView(lockedPanel, LinearLayout.LayoutParams(-1, 0, 1f))
        footer = column()
        statusLabel = text("", 22f, amber).apply { maxLines = 3 }
        footer.add(statusLabel, 8)
        root.add(footer, 0)
        if (navigationRecovery) renderNavigationRecovery(body) else when (flow.screen) {
            JourneyScreen.INPUT -> renderInput(body)
            JourneyScreen.REVIEW -> renderReview(body)
            JourneyScreen.LOCATIONS -> renderLocations(body)
            JourneyScreen.ROUTE -> renderRoute(body)
            JourneyScreen.SETTINGS -> renderSettings(body)
        }
        setContentView(root); root.requestApplyInsets(); applyLock()
    }
    private fun renderNavigationRecovery(body: LinearLayout) {
        heading(body, "차량 내비를 먼저 확인해 주세요", "앱이 다시 열렸어요. 기존 안내가 진행 중일 수 있어요.")
        val recovery = card(body, "일정과 완료 기록은 보관했어요")
        recovery.add(text("차량 내비에서 목적지와 안내 상태를 확인해 주세요. 확인 전에는 새 목적지를 보내지 않아요.", 26f))
        mapMessage?.let { recovery.add(text(it, 22f, amber)) }
        button(footer, "차량 내비 보기", true, { NavigationHandoff.canShow(navigationConnected, fixture || simulate, false, navigationRecovery, vehicle, busy) }) { showVehicleNavigation() }
        if (!navigationConnected) button(body, "차량 내비 다시 연결") { if (::navigation.isInitialized) navigation.connect() }
        button(body, "기존 안내를 종료했어요 · 다시 계획") {
            dialog(AlertDialog.Builder(this).setTitle("기존 안내를 종료했나요?")
                .setMessage("차량 내비에서 기존 안내를 종료했는지 먼저 확인해 주세요. 이 확인은 새 출발 승인이 아닙니다. 남은 볼일을 다시 계산하고 새 계획을 승인해야 해요.")
                .setNegativeButton("확인하러 가기", null).setPositiveButton("종료 확인 · 다시 계산") { _, _ -> if (allowed() && !busy) {
                    navigationRecovery=false; navigationHandoff.cancel(); mapMessage=null
                    state.routeFailed(); state.edited(); plan=null; flow.parsed(); persist(); render()
                    runCatching { calculate() }.onFailure { showError(it) }
                } })
        }
    }
    private fun showVehicleNavigation() {
        if (!foreground || !NavigationHandoff.canShow(navigationConnected, fixture || simulate,
                state.guidanceAccepted && state.activeId!=null && state.arrivedId==null,
                navigationRecovery, vehicle, busy)) return
        stopSpoken(true); voiceDialog?.dismiss(); persist()
        // Commit the recovery hint before another app can take foreground.
        prefs.edit().putBoolean("guidance-unresolved", navigationRecovery || !fixture && state.activeId!=null && state.arrivedId==null).commit()
        navigation.showNavigationScreen().fold(
            onSuccess = { mapMessage="차량 내비 화면을 요청했어요. 화면이 열리지 않으면 다시 눌러 주세요." },
            onFailure = { mapMessage="차량 내비 화면을 열지 못했어요. 안내 상태는 유지합니다. 내비 연결을 확인하고 다시 시도해 주세요." }
        )
        renderPlan()
    }
    private fun renderInput(body: LinearLayout) {
        heading(body, "가는 길에, 무엇을 할까요?", "할 일과 귀가 시간을 편하게 말해 주세요.")
        val requestCard = card(body, "말하면, 갈 길이 정리돼요", "꼭 해야 할 일과 마감까지 함께요.")
        requestCard.add(text("“약국 들르고, 세탁물 찾고 싶어.\n7시까지 집에 가게 맞춰줘.”", 32f, ink, true).apply {
            setPadding(0, dp(28), 0, dp(28)); setLineSpacing(dp(8).toFloat(), 1f)
        }, 8)
        requestCard.add(text("장소와 순서는 AI가 준비하고, 출발은 내가 확인해요.", 24f, muted), 4)
        button(body, "직접 입력하기") {
            val content = column().apply { setPadding(dp(28), dp(12), dp(28), dp(12)) }
            showEditor(content, "오늘 할 일", input)
            var window: AlertDialog? = null
            button(content, "계획 준비하기", true, { input.text.isNotBlank() }) {
                window?.dismiss()
                require(!sampleExperience) { "자유 입력은 서비스 연결에서 사용할 수 있어요. 샘플에서는 ‘말로 부탁하기’로 저장된 요청을 선택해 주세요." }
                parse()
            }
            window = dialog(AlertDialog.Builder(this).setTitle("오늘의 요청").setView(content).setNegativeButton("취소", null))
        }
        body.add(text("음성 입력과 수정은 정차 중에 이용해 주세요.", 22f, muted))
        button(footer, "말로 부탁하기", true, { sampleExperience || vehicle == VehicleState.PARKED }) {
            openVoice(if (sampleExperience) VoiceSource.EXAMPLE else VoiceSource.PLEOS)
        }
    }
    private fun openVoice(source: VoiceSource) {
        if (source == VoiceSource.PLEOS && vehicle != VehicleState.PARKED) return
        voiceDialog?.dismiss()
        voiceConversation = true
        stopSpoken()
        val dialog = VoiceInputDialog(this, source, { vehicle }, VoiceJourney.EXAMPLE_REQUEST, onCancelled = { stopSpoken(true) }) { draft ->
            if (!allowed() || busy) return@VoiceInputDialog
            input.setText(draft.text)
            voiceConversation = true
            voiceExample = draft.source == VoiceSource.EXAMPLE && BuildConfig.DEMO
            prefs.edit().putBoolean("voice-example", voiceExample).apply()
            if (voiceExample) { simulate = true; loadFixture(voiceJourney = true) }
            else parse()
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
        if (!fixture && (!LocationSetup.originReady(prefs.getString("live-origin", null), originLat.text.toString(), originLon.text.toString(), System.currentTimeMillis()) || homeLat.text.isBlank() || homeLon.text.isBlank())) {
            body.add(text("출발 위치를 새로 확인하거나 집을 설정해 주세요.", 22f, amber))
            button(body, "출발 · 귀가 설정", enabled = { locationAccess().canEdit }) { flow.locations(); render() }
        }
        if (!fixture && !reviewNotes.isNullOrBlank()) body.add(text(reviewNotes!!, 22f, amber))
        val reviewAction = intentReview?.action ?: ReviewAction.PLAN
        val missingPlace = remaining.firstOrNull { VoiceJourney.needsVenue(it) }
        button(footer, when {
            requiresParse && pendingParse != null -> "추가 요청 다시 반영"
            requiresParse -> "할 일 다시 정리하기"
            reviewAction == ReviewAction.ANSWER -> "질문에 답하기"
            reviewAction == ReviewAction.HOME_TIME -> "귀가 시간 정하기"
            reviewAction == ReviewAction.EDIT_REQUEST -> "요청 다시 입력하기"
            !fixture && missingPlace != null -> "방문할 장소 선택"
            else -> "동선 확인하기"
        }, true) {
            when {
                requiresParse && pendingParse != null -> {
                    val payload = pendingPrior?.retryPayload(requireNotNull(pendingParse), errands) ?: JSONObject(pendingParse.toString())
                    submitParse(payload, pendingPrior)
                }
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
        val panel = card(body, "요청을 정리했어요", "빠진 조건이 없는지 확인해 주세요.")
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
        button(content, "말로 답하기", true, { vehicle == VehicleState.PARKED }) {
            window?.dismiss()
            openContinuationVoice(review, question)
        }
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

    private fun openContinuationVoice(review: IntentReview, question: String? = null) {
        if (question == null && plan != null) { openPlanVoice(VoiceJourney.EXAMPLE_REFINEMENT); return }
        if (review !== intentReview || state.activeId != null || state.arrivedId != null) return
        val revision = state.revision
        val source = if (review.example) VoiceSource.EXAMPLE else VoiceSource.PLEOS
        if (source == VoiceSource.PLEOS && vehicle != VehicleState.PARKED) return
        voiceDialog?.dismiss()
        voiceConversation = true
        stopSpoken()
        val dialog = VoiceInputDialog(this, source, { vehicle }, VoiceJourney.EXAMPLE_REFINEMENT,
            question ?: "바꿀 조건을 편하게 말해 주세요. 나머지 필수 일정과 마감은 유지해요.", onCancelled = { stopSpoken(true) }) { draft ->
            if (!allowed() || busy || review !== intentReview || !state.acceptsResponse(revision)) return@VoiceInputDialog
            applyVoiceAnswer(review, draft, question)
        }
        voiceDialog = dialog
        dialog.setOnDismissListener { if (voiceDialog === dialog) voiceDialog = null }
        dialog.show()
    }

    private fun applyVoiceAnswer(review: IntentReview, draft: VoiceDraft, question: String? = null) {
        if (draft.source == VoiceSource.EXAMPLE) {
            check(BuildConfig.DEMO && fixture && review.example)
            for (i in 0 until errands.length()) if (errands.getJSONObject(i).optString("id") == "medicine" && "medicine" !in state.completed)
                errands.getJSONObject(i).put("dwell_seconds", 1800)
            review.answers.put(JSONObject().put("question", IntentReview.REFINEMENT_QUESTION).put("answer", draft.text))
            edited(continueVoice = true); render(); continueProposal()
        } else {
            val payload = if (question != null) review.answerPayload(question, draft.text, errands) else review.refinementPayload(draft.text, errands)
            submitParse(payload, review)
        }
    }

    private fun stopSpoken(clearConversation: Boolean = false) {
        spokenExchange.cancel(); speechOutput.close(); spokenStatus = null
        if (clearConversation) voiceConversation = false
    }

    private fun announcePlan(listenAfter: Boolean = voiceConversation, prompt: String? = null) {
        val result = plan ?: return
        if (!foreground || flow.screen != JourneyScreen.ROUTE || vehicle != VehicleState.PARKED || busy || state.activeId != null || state.arrivedId != null) return
        voiceDialog?.dismiss()
        val revision = state.revision
        val target = SpokenPlan.destinationId(result, state.completed)
        val token = spokenExchange.read(revision, target, listenAfter)
        val narration = prompt ?: SpokenPlan.summary(request, result, errands, state.completed)
        speechOutput.speak(narration) { event ->
            if (!spokenExchange.isCurrent(token) || !foreground || flow.screen != JourneyScreen.ROUTE || !state.acceptsResponse(revision) || plan !== result) return@speak
            when(event) {
                SpeechOutputEvent.Preparing -> spokenStatus = "음성 안내를 준비하고 있어요"
                SpeechOutputEvent.Started -> spokenStatus = "계획을 설명하고 있어요"
                SpeechOutputEvent.Done -> {
                    spokenStatus = null
                    if (spokenExchange.done(token, state.revision, SpokenPlan.destinationId(result, state.completed), vehicle == VehicleState.PARKED)) {
                        root.postDelayed({ if (spokenExchange.isCurrent(token) && foreground && vehicle == VehicleState.PARKED) openPlanVoice() }, 350)
                    }
                }
                SpeechOutputEvent.Stopped -> { spokenExchange.cancel(); spokenStatus = "음성 안내가 중단됐어요. 다시 듣거나 말로 대답해 주세요." }
                is SpeechOutputEvent.Failure -> { spokenExchange.cancel(); spokenStatus = event.message }
            }
            applyLock()
        }
    }

    private fun openPlanVoice(exampleText: String? = null) {
        val result = plan ?: return
        if (!foreground || flow.screen != JourneyScreen.ROUTE || vehicle != VehicleState.PARKED || busy || state.activeId != null || state.arrivedId != null) return
        voiceDialog?.dismiss(); voiceConversation = true; spokenExchange.cancel(); spokenStatus = null
        val epoch = spokenExchange.token
        val revision = state.revision
        val destination = SpokenPlan.destinationId(result, state.completed)
        val turn = SpokenTurn(revision, destination)
        speechOutput.stopThen { clean ->
            if (!spokenExchange.isCurrent(epoch) || !foreground || flow.screen != JourneyScreen.ROUTE || vehicle != VehicleState.PARKED || plan !== result || !state.acceptsResponse(revision)) return@stopThen
            if (!clean) { spokenStatus = "음성 안내 종료를 확인하지 못했어요. 잠시 뒤 다시 말해 주세요."; applyLock(); return@stopThen }
            val initialSample = state.completed.isEmpty() && (0 until errands.length()).any { errands.getJSONObject(it).let { e -> e.optString("id") == "medicine" && e.optInt("dwell_seconds") == 600 } }
            val source = if (fixture && sampleExperience) VoiceSource.EXAMPLE else VoiceSource.PLEOS
            val dialog = VoiceInputDialog(this, source, { vehicle }, exampleText ?: if (initialSample) VoiceJourney.EXAMPLE_REFINEMENT else VoiceJourney.EXAMPLE_APPROVAL,
                "바꿀 조건이나, 이 계획으로 출발할 뜻을 말해 주세요.", "말 전달하기", { stopSpoken(true) }) { draft ->
                if (!foreground || !allowed() || busy || plan !== result || draft.source == VoiceSource.EXAMPLE && !(fixture && BuildConfig.DEMO)) return@VoiceInputDialog
                runCatching { when(turn.take(draft.text, state.revision, SpokenPlan.destinationId(result, state.completed), vehicle == VehicleState.PARKED,
                    navigationAllowed(), SpokenPlan.canApprove(result, errands, state.completed))) {
                    SpokenCommand.APPROVE -> {
                        val stop = firstRemainingStop(result) ?: JSONObject().put("errand_id", "__home").put("label", "귀가").put("place", requireNotNull(request).getJSONObject("home"))
                        android.util.Log.i("OnMyWayVoice", "voice-approved revision=$revision")
                        approveNavigation(stop, revision)
                    }
                    SpokenCommand.REFINE -> intentReview?.let { applyVoiceAnswer(it, draft) }
                    SpokenCommand.CLARIFY -> announcePlan(true, "조건을 바꾸시거나 출발하겠다는 뜻을 분명히 말해 주세요. 단순한 네라는 답만으로는 출발하지 않아요.")
                    SpokenCommand.CANCEL -> { stopSpoken(true); applyLock() }
                    SpokenCommand.STALE -> { stopSpoken(true); showError(IllegalStateException("계획이나 시간이 바뀌었어요. 다시 계산하고 승인해 주세요.")) }
                    null -> Unit
                } }.onFailure { stopSpoken(true); showError(it) }
            }
            voiceDialog = dialog
            dialog.setOnDismissListener { if (voiceDialog === dialog) voiceDialog = null }
            dialog.show()
        }
    }

    private fun continueProposal() {
        if (!allowed() || busy || requiresParse || flow.screen != JourneyScreen.REVIEW) return
        val review = intentReview ?: return
        when (review.action) {
            ReviewAction.ANSWER -> { answerQuestion(review, review.questions.first()); return }
            ReviewAction.HOME_TIME -> { answerQuestion(review, IntentReview.HOME_QUESTION); return }
            ReviewAction.EDIT_REQUEST -> return
            ReviewAction.PLAN -> Unit
        }
        if (!fixture && listOf(originLat, originLon, homeLat, homeLon).any { it.text.isBlank() }) return
        val missing = (0 until errands.length()).map { errands.getJSONObject(it) }.firstOrNull { VoiceJourney.needsVenue(it) }
        if (!fixture && missing != null) { choosePlace(missing, true); return }
        calculate()
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
        if (BuildConfig.DEMO) {
            val connection = card(body, "연결", if (sampleExperience) "샘플 데이터 사용 중" else "서비스 연결 사용 중")
            button(connection, if (sampleExperience) "서비스 연결로 전환" else "샘플로 둘러보기", enabled = { request == null && state.activeId == null }) {
                prefs.edit().putBoolean("sample-experience", !sampleExperience).apply(); simulate = false; render()
            }
            connection.add(text("연결 모드는 새 일정에서 바꿀 수 있어요.", 22f, muted), 0)
        }
        if (CockpitPresentation.canSendSampleArrival(BuildConfig.DEMO, fixture, simulate, state.activeId, state.arrivedId, vehicle, busy)) button(body, "샘플 안내 · 도착 신호") {
            if (CockpitPresentation.canSendSampleArrival(BuildConfig.DEMO, fixture, simulate, state.activeId, state.arrivedId, vehicle, busy)) {
                onNavigation("arrived"); flow.back(); render()
            }
        }
        if (BuildConfig.DEMO && fixture) button(body, if (simulate) "샘플 안내 끊기" else "샘플 안내 연결") {
            cancelOwnedNavigation()
            simulate = !simulate; state.routeFailed(); state.edited(); flow.edited(); plan = null; persist(); render()
        }
        button(body, "새 일정 시작") { resetJourney() }
        button(body, "출발 · 귀가 설정", enabled = { locationAccess().canEdit }) { flow.locations(); render() }
        button(body, "연결 · 위치 고급 설정") {
            val advanced = column().apply { setPadding(dp(28), dp(12), dp(28), dp(12)) }
            advanced.add(text("서버 연결과 추가 지연을 설정해요. 출발·귀가는 별도 위치 설정에서 확인해 주세요.", 22f, muted))
            for ((label, editor) in listOf("서버 주소" to server, "추가 지연 (분)" to delay)) showEditor(advanced, label, editor)
            dialog(AlertDialog.Builder(this).setTitle("고급 설정").setView(ScrollView(this).apply { addView(advanced) }).setPositiveButton("닫기") { _, _ -> render() })
        }
        body.add(text(if (fixture) "예시 장소·영업시간·이동시간을 사용합니다. 모의 안내는 실제 내비게이션을 요청하지 않습니다." else "실제 데이터는 서버 연결과 API 설정이 필요합니다.", 24f, muted))
        button(footer, "돌아가기", true) { flow.back(); render() }
    }
    private fun locationAccess() = LocationAccess(foreground, vehicle, fixture || simulate || sampleExperience,
        navigationRecovery || state.activeId != null || (::navigation.isInitialized && navigation.hasActiveRequest), busy)

    private fun cancelLocationWork() {
        if (locationBusy) { locationMessage = "위치 확인을 취소했어요. 정차 중 다시 확인해 주세요."; locationStatusView?.text = locationMessage }
        locationEpoch++; locationAttempt.cancel(); locationProvider.close(); locationBusy = false
    }

    private fun renderLocations(body: LinearLayout) {
        heading(body, "출발 · 귀가 설정", "출발지는 차량 위치로, 집은 직접 확인해 저장해요.")
        val origin = SetupLocation.parse(prefs.getString("live-origin", null))
        val home = SetupLocation.parse(prefs.getString("real-home", null))
        val ready = LocationSetup.originReady(prefs.getString("live-origin", null), originLat.text.toString(), originLon.text.toString(), System.currentTimeMillis())
        card(body, "출발지", when {
            !ready -> "설정이 없거나 위치가 오래됐어요. 다시 확인해 주세요."
            origin?.source == LocationSource.DEVICE -> "차량 위치 확인됨 · 1분 안에 동선을 계산해 주세요."
            origin?.source == LocationSource.COMPLETED_STOP -> "마지막으로 완료한 장소에서 이어갑니다."
            else -> "직접 지정한 출발지 · 현재 차량 위치는 아니에요."
        })
        card(body, "집", home?.label ?: if (homeLat.text.isNotBlank() && homeLon.text.isNotBlank()) "직접 지정한 귀가 위치" else "아직 저장하지 않았어요.")
        locationStatusView = text(locationMessage.orEmpty(), 23f, amber).also { body.add(it) }
        val originEditable = state.completed.isEmpty()
        button(body, "현재 위치로 출발지 설정", true, { locationAccess().canEdit && !locationBusy && originEditable }) { acquireLocation(false) }
        button(body, "현재 위치를 집으로 저장", enabled = { locationAccess().canEdit && !locationBusy }) { acquireLocation(true) }
        button(body, "집 근처 장소 검색", enabled = { locationAccess().canEdit && !locationBusy }) { searchHome() }
        body.add(text("장소명·지역으로 검색해요. 모든 주거 주소를 찾을 수 있는 검색은 아니에요. 집에 있을 때 현재 위치를 저장하거나 직접 지정할 수도 있어요.", 22f, muted))
        if (!originEditable) body.add(text("진행 중인 일정의 출발지는 마지막 완료 장소로 유지해요. 새 출발지는 새 일정에서 정해 주세요.", 22f, muted))
        button(body, "위치 직접 지정", enabled = { locationAccess().canEdit && !locationBusy }) { manualLocation() }
        button(body, "차량 위치 접근 설정 안내", enabled = { locationAccess().canEdit && !locationBusy }) {
            if (!locationProvider.showAccessHelp()) showError(IllegalStateException("차량 개인정보 설정에서 위치 접근을 확인해 주세요."))
        }
        button(footer, "돌아가기", true) { flow.back(); render() }
    }

    private fun acquireLocation(home: Boolean) {
        check(flow.screen == JourneyScreen.LOCATIONS && locationAccess().canEdit)
        check(home || state.completed.isEmpty())
        val permission = "pleos.car.permission.FUSED_LOCATION"
        if (checkSelfPermission(permission) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(permission), 401)
            return
        }
        cancelLocationWork()
        val epoch = locationEpoch
        val revision = state.revision
        val token = locationAttempt.begin(revision, SystemClock.elapsedRealtime(), locationAccess()) ?: return
        locationBusy = true; locationMessage = "차량의 최신 위치를 확인하고 있어요…"
        locationStatusView?.text = locationMessage; applyLock()
        locationProvider.acquire { result ->
            if (epoch != locationEpoch || flow.screen != JourneyScreen.LOCATIONS) return@acquire
            locationBusy = false
            locationMessage = null; locationStatusView?.text = ""
            val owned = locationAttempt.finish(token, state.revision, SystemClock.elapsedRealtime(), locationAccess())
            if (!owned) { locationMessage = "위치 확인이 취소됐어요. 정차 후 다시 확인해 주세요."; render(); return@acquire }
            result.onSuccess { confirmLocation(it, home, revision, epoch) }.onFailure {
                locationMessage = it.message; render()
            }
            applyLock()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != 401) return
        cancelLocationWork()
        locationMessage = if (grantResults.firstOrNull() == android.content.pm.PackageManager.PERMISSION_GRANTED)
            "위치 권한을 허용했어요. 정차 중 위치 확인 버튼을 다시 눌러 주세요."
            else "위치 권한이 없어 차량 위치를 받지 않아요. 다시 허용하거나 위치를 직접 지정해 주세요."
        locationStatusView?.text = locationMessage
        if (foreground && flow.screen == JourneyScreen.LOCATIONS) render()
    }

    private fun confirmLocation(selection: SetupLocation, home: Boolean, revision: Long, epoch: Long) {
        if (epoch != locationEpoch || !LocationSetup.canSave(revision, state.revision, locationAccess(), selection, System.currentTimeMillis())) return
        dialog(AlertDialog.Builder(this).setTitle(if (home) "이 위치를 집으로 저장할까요?" else "이 위치에서 출발할까요?")
            .setMessage(if (selection.source == LocationSource.DEVICE)
                "차량에서 받은 위치예요. ${if (home) "지금 집에 있는지 확인해 주세요. 다음 일정에도 귀가 위치로 유지해요." else "현재 출발 위치가 맞는지 확인해 주세요."}\n확인 시각 ${java.time.Instant.ofEpochMilli(selection.timestampMillis).atZone(java.time.ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("HH:mm:ss"))}"
                else "${selection.label}\n정확한 귀가 장소가 맞는지 확인해 주세요.")
            .setNegativeButton("취소", null).setPositiveButton("확인하고 저장") { _, _ ->
                if (epoch == locationEpoch && flow.screen == JourneyScreen.LOCATIONS &&
                    LocationSetup.canSave(revision, state.revision, locationAccess(), selection, System.currentTimeMillis()) &&
                    (home || state.completed.isEmpty())) {
                    val edit = prefs.edit()
                    val prefix = if (home) "home" else "origin"
                    edit.putString("draft-${prefix}Lat", selection.latitude.toString()).putString("draft-${prefix}Lon", selection.longitude.toString())
                    edit.putString(if (home) "real-home" else "live-origin", selection.toJson()).apply()
                    locationMessage = if (home) "귀가 위치를 저장했어요. 새 계획을 확인해 주세요." else "출발 위치를 저장했어요. 1분 안에 동선을 확인해 주세요."
                    edited(); render()
                } else if (foreground && flow.screen == JourneyScreen.LOCATIONS) {
                    locationMessage = "위치나 일정 상태가 바뀌었어요. 다시 확인해 주세요."; render()
                }
            })
    }

    private fun searchHome() {
        check(locationAccess().canEdit)
        require(LocationSetup.originReady(prefs.getString("live-origin", null), originLat.text.toString(), originLon.text.toString(), System.currentTimeMillis())) {
            "검색 전에 출발 위치를 먼저 확인해 주세요."
        }
        val content = column().apply { setPadding(dp(28), dp(16), dp(28), dp(16)) }
        val query = field(content, "장소명 · 지역", "") { }
        content.add(text("아파트·건물 등 검색된 장소만 선택할 수 있어요. 결과가 없으면 집에서 현재 위치를 저장하거나 직접 지정해 주세요.", 22f, muted))
        val epoch = locationEpoch; val revision = state.revision
        val prompt = dialog(AlertDialog.Builder(this).setTitle("집 근처 장소 검색").setView(ScrollView(this).apply { addView(content) })
            .setNegativeButton("취소", null).setPositiveButton("검색", null))
        prompt.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            if (epoch != locationEpoch || !locationAccess().canEdit) return@setOnClickListener
            val value = query.text.toString().trim()
            if (value.isBlank() || value.length > 200) { query.error = "장소명·지역을 1~200자로 입력해 주세요."; return@setOnClickListener }
            val payload = JSONObject().put("query", value).put("origin", location("origin", "출발", originLat, originLon))
            prompt.dismiss()
            async({ api().call("/v1/places", payload) }) { response ->
                if (epoch != locationEpoch || revision != state.revision || !locationAccess().canEdit || flow.screen != JourneyScreen.LOCATIONS) return@async
                val choices = response.getJSONArray("places")
                val list = column().apply { setPadding(dp(28), dp(16), dp(28), dp(16)) }
                if (choices.length() == 0) list.add(text("검색 결과가 없어요. 집에서 현재 위치를 저장하거나 위치를 직접 지정해 주세요.", 24f, amber))
                var window: AlertDialog? = null
                for (i in 0 until choices.length()) {
                    val p = choices.getJSONObject(i)
                    val label = p.getString("name") + p.optString("address").takeIf { it.isNotBlank() }?.let { "\n$it" }.orEmpty()
                    button(list, label, enabled = { locationAccess().canEdit }) {
                        check(!p.optJSONObject("source")!!.optBoolean("is_fixture")) { "예시 장소는 실제 집 위치로 저장할 수 없어요." }
                        window?.dismiss()
                        confirmLocation(SetupLocation(p.getDouble("latitude"), p.getDouble("longitude"), LocationSource.PLACE, label), true, revision, epoch)
                    }
                }
                window = dialog(AlertDialog.Builder(this).setTitle("귀가 장소 선택").setView(ScrollView(this).apply { addView(list) }).setNegativeButton("닫기", null))
            }
        }
    }

    private fun manualLocation() {
        check(locationAccess().canEdit)
        val content = column().apply { setPadding(dp(28), dp(16), dp(28), dp(16)) }
        content.add(text("좌표를 확인한 위치만 직접 지정해 주세요. 차량 현재 위치로 확인된 정보는 아니에요.", 22f, muted))
        var home = true
        val target = android.widget.RadioGroup(this)
        val homeChoice = RadioButton(this).apply { text = "귀가 위치"; textSize = 24f; id = View.generateViewId(); isChecked = true; minHeight = dp(80) }
        val originChoice = RadioButton(this).apply { text = "출발 위치"; textSize = 24f; id = View.generateViewId(); minHeight = dp(80); isEnabled = state.completed.isEmpty() }
        target.addView(homeChoice); if (state.completed.isEmpty()) target.addView(originChoice)
        target.setOnCheckedChangeListener { _, id -> home = id == homeChoice.id }
        content.add(target)
        val lat = field(content, "위도", "") { }
        val lon = field(content, "경도", "") { }
        val epoch = locationEpoch; val revision = state.revision
        val prompt = dialog(AlertDialog.Builder(this).setTitle("위치 직접 지정").setView(ScrollView(this).apply { addView(content) })
            .setNegativeButton("취소", null).setPositiveButton("확인", null))
        prompt.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val selected = SetupLocation(lat.text.toString().toDoubleOrNull() ?: Double.NaN, lon.text.toString().toDoubleOrNull() ?: Double.NaN, LocationSource.MANUAL, "직접 지정한 ${if (home) "귀가 위치" else "출발지"}")
            if (!selected.usable(System.currentTimeMillis(), true)) { lat.error = "올바른 위도·경도를 입력해 주세요."; return@setOnClickListener }
            prompt.dismiss(); confirmLocation(selected, home, revision, epoch)
        }
    }

    private fun renderRoute(body: LinearLayout) {
        val displayedRevision = state.revision
        val result = plan
        if (result == null) { heading(body, "동선을 다시 확인해 주세요"); button(footer, "할 일 확인하기", true) { flow.parsed(); render() }; return }
        val outcome = result.optString("status")
        val next = firstRemainingStop(result)
        val homeLeg = next == null
        val id = next?.optString("errand_id") ?: "__home"
        heading(body, when { outcome == "infeasible" -> "시간 조정이 필요해요"; state.arrivedId == id -> if (homeLeg) "집에 도착했어요" else "할 일을 마쳤나요?"; state.activeId == id -> "다음 장소로 이동해요"; else -> if (homeLeg) "이제 집으로 가요" else if (state.completed.isEmpty()) "이렇게 들르고 갈까요?" else "다음은 이곳이에요" })
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
        else if (state.activeId == id) body.add(text(if (state.guidanceAccepted) "내비게이션 안내를 확인해 주세요." else "차량 내비에서 안내를 준비하고 있어요.", 22f, muted))
        if (state.activeId == null && state.arrivedId == null && outcome != "infeasible") {
            val summary = card(body, "들를 순서")
            val stops = result.optJSONArray("stops") ?: JSONArray()
            for (i in 0 until stops.length()) {
                val stop = stops.getJSONObject(i)
                if (stop.optString("errand_id") in state.completed) continue
                val item = (0 until errands.length()).map { errands.getJSONObject(it) }.firstOrNull { it.optString("id") == stop.optString("errand_id") }
                val finish = item?.optString("latest_finish")?.takeUnless { it.isBlank() || it == "null" }
                summary.add(text("${i + 1}. ${stop.optString("label")}  ·  ${stop.getJSONObject("place").optString("name")}", 26f, ink, true), 4)
                summary.add(text("${if (item?.optBoolean("mandatory", true) != false) "필수" else "선택"} · ${minutes(item?.optInt("dwell_seconds") ?: 0)}분 머무름 · ${clock(stop.optString("end_time"))} 완료${finish?.let { " / ${clock(it)} 마감" }.orEmpty()}", 22f, muted), 16)
            }
            val omitted = result.optJSONArray("skipped_errands") ?: JSONArray()
            for (i in 0 until omitted.length()) summary.add(text("${errandLabel(omitted.getJSONObject(i).optString("id"))}는 다음에 · 필수 일정과 마감을 우선했어요", 22f, amber), 4)
        }
        if (state.activeId == null && state.arrivedId == null) intentReview?.let { review ->
            button(body, "말로 조건 바꾸기", enabled = { review.example || vehicle == VehicleState.PARKED }) { openContinuationVoice(review) }
        }
        if (state.activeId == null && state.arrivedId == null) button(body, "계획 듣기", enabled = { vehicle == VehicleState.PARKED }) { announcePlan(false) }
        button(body, "요청 · 일정 자세히") { showPlanDetails(result) }
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
            state.arrivedId == id && next != null -> button(footer, "완료했어요", true) { completeCurrent(next, displayedRevision) }
            state.activeId == id -> {
                footer.add(text(if (state.guidanceAccepted) "안내 중 · 차량 내비게이션을 확인해 주세요" else "차량 내비의 경로 수락을 기다리고 있어요", 24f, muted).apply { gravity = Gravity.CENTER }, 8)
                if (state.guidanceAccepted) {
                    button(footer, "차량 내비 보기", true, { NavigationHandoff.canShow(navigationConnected, fixture || simulate, state.guidanceAccepted, false, vehicle, busy) }) { showVehicleNavigation() }
                    if (fixture) footer.add(text("샘플 안내는 실제 차량 내비를 실행하지 않아요.", 20f, muted), 0)
                    else mapMessage?.let { footer.add(text(it, 20f, amber), 0) }
                }
            }
            fixture && !simulate -> button(footer, "안내 연결", true) { simulate = true; state.routeFailed(); state.edited(); render() }
            homeLeg && (0 until errands.length()).any { errands.getJSONObject(it).let { item -> item.optBoolean("mandatory", true) && item.optString("id") !in state.completed } } -> button(footer, "남은 필수 할 일 확인", true) { flow.parsed(); render() }
            homeLeg && result.isNull("home_arrival_time") -> button(footer, "동선 다시 확인", true) { calculate() }
            !fixture && !navigationAllowed() -> button(footer, "시간이 흘렀어요 · 다시 계산", true) { calculate() }
            else -> {
                footer.add(text("확인한 다음 목적지로 안내합니다.", 20f, muted), 4)
                val approvals = row()
                val voice = column()
                button(voice, "말로 대답하기", true, { vehicle == VehicleState.PARKED }) { openPlanVoice(VoiceJourney.EXAMPLE_APPROVAL) }
                approvals.addView(voice, LinearLayout.LayoutParams(0, -2, 1f).apply { rightMargin = dp(16) })
                val touch = column()
                button(touch, if (homeLeg) "집으로 출발" else "확인하고 출발") {
                val destination = next ?: JSONObject().put("errand_id", "__home").put("label", "귀가").put("place", requireNotNull(request).getJSONObject("home"))
                approveNavigation(destination, displayedRevision)
                }
                approvals.addView(touch, LinearLayout.LayoutParams(0, -2, 1f))
                footer.add(approvals, 0)
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

    private fun choosePlace(item: JSONObject, continuePlanning: Boolean = false) {
        if (fixture) {
            val candidates = item.optJSONArray("candidates") ?: JSONArray()
            require(candidates.length() > 0) { "예시 장소가 없습니다. 예시 일정을 다시 선택해 주세요." }
            showPlaceChoices(item, candidates, continuePlanning)
            return
        }
        val origin = location(request?.optJSONObject("origin")?.optString("id") ?: "origin", "출발", originLat, originLon)
        val payload = JSONObject().put("query", item.getString("query")).put("origin", origin)
        async({ api().call("/v1/places", payload) }) { response -> showPlaceChoices(item, response.getJSONArray("places"), continuePlanning) }
    }

    private fun showPlaceChoices(item: JSONObject, choices: JSONArray, continuePlanning: Boolean = false) {
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
                window?.dismiss(); saveErrand(updated)
                if (continuePlanning) continueProposal() else reviewPlace(updated)
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
        stopSpoken(true)
        val lines = mutableListOf(if (fixture) "예시 데이터입니다. 실제 장소·영업·도로 정보가 아닙니다." else "실제 제공자 조회 결과입니다. 미확인 정보는 확정된 사실이 아닙니다.")
        intentReview?.let { review ->
            lines += "처음 요청\n${review.originalText}"
            for (i in 0 until review.answers.length()) lines += "추가로 반영한 조건\n${review.answers.getJSONObject(i).optString("answer")}"
        }
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
        val builder = AlertDialog.Builder(this).setTitle("요청과 일정").setView(ScrollView(this).apply { addView(content) }).setPositiveButton("닫기", null)
        if (state.activeId == null && state.arrivedId == null) builder.setNeutralButton("조건 직접 수정") { _, _ -> if (allowed() && !busy) { flow.parsed(); render() } }
        dialog(builder)
    }
    private fun resetJourney() {
        dialog(AlertDialog.Builder(this).setTitle("새 일정을 시작할까요?").setMessage("현재 일정과 완료 기록을 초기화합니다.").setNegativeButton("유지", null).setPositiveButton("새로 시작") { _, _ -> if (allowed() && !busy) {
            cancelOwnedNavigation()
            clearFixtureDrafts(true)
            state.completed.clear(); state.routeFailed(); state.edited(); request = null; plan = null; errands = JSONArray(); fixture = false; requiresParse = false; voiceExample = false; intentReview = null; pendingParse = null; pendingPrior = null
            prefs.edit().putString("input", "").putBoolean("voice-example", false).putString("delay", "0").apply(); errorMessage = null; reviewNotes = null; simulate = false; computedElapsedMillis = null; flow.start(); persist(); render()
        } })
    }
    private fun clearFixtureDrafts(resetOrigin: Boolean = false, newJourney: Boolean = false) {
        val values = prefs.all.filterValues { it is String }.mapValues { it.value as String }
        val clean = if (resetOrigin) LocationSetup.reset(values, fixture) else LocationSetup.prepareForInterpretation(values, fixture, newJourney)
        val edit = prefs.edit()
        (values.keys - clean.keys).forEach { edit.remove(it) }
        clean.forEach { (key, value) -> edit.putString(key, value) }
        edit.apply()
    }
    private fun applyLock() {
        navigationHandoff.vehicleChanged(vehicle)
        if (!locationAccess().copy(busy = false).canEdit) cancelLocationWork()
        if (!::lockLabel.isInitialized) return
        val can = allowed()
        lockLabel.text = when {
            fixture || sampleExperience -> "샘플 데이터 · ${if (simulate) "모의 안내" else "실제 내비 요청 없음"}"
            simulate && BuildConfig.DEMO -> "모의 안내 · 실제 내비 요청 없음"
            vehicle == VehicleState.RESTRICTED -> "주행 중 · 조작 잠금"
            !can -> "차량 상태 확인 중 · 조작 잠금"
            else -> "정차 중"
        }
        if (!can) {
            voiceDialog?.dismiss()
            openDialogs.toList().forEach { it.dismiss() }
            (getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager).hideSoftInputFromWindow(root.windowToken, 0)
        }
        if (vehicle != VehicleState.PARKED) voiceDialog?.dismiss()
        if (vehicle != VehicleState.PARKED) stopSpoken(true)
        lockLabel.visibility = if (lockLabel.text.isBlank()) View.GONE else View.VISIBLE
        bodyScroll.visibility = if (can) View.VISIBLE else View.GONE
        lockedPanel.visibility = if (can) View.GONE else View.VISIBLE
        footer.visibility = if (can) View.VISIBLE else View.GONE
        demoRescue?.visibility = if (vehicle == VehicleState.UNKNOWN && !busy) View.VISIBLE else View.GONE
        controls.forEach { (view, enabled) -> view.isEnabled = can && !busy && enabled(); view.alpha = if (view.isEnabled) 1f else .4f }
        statusLabel.text = if (busy) "확인 중이에요…" else (errorMessage ?: state.navigationError ?: spokenStatus).orEmpty()
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
        stopSpoken()
        val retainedRequest = if (prior != null) request?.let { JSONObject(it.toString()) } else null
        // A newly spoken constraint invalidates the old proposal immediately,
        // including when parsing fails. A failure must not re-enable old approval.
        pendingParse = JSONObject(payload.toString()); pendingPrior = prior
        if (prior != null) { requiresParse = true; state.edited(); plan = null; flow.parsed(); persist(); render() }
        val client = api()
        async({ client.call("/v1/parse", payload) }) { result ->
            pendingParse = null; pendingPrior = null
            cancelOwnedNavigation()
            clearFixtureDrafts(newJourney = prior == null)
            fixture = false; request = retainedRequest; plan = null
            state.interpreted(newJourney = prior == null)
            requiresParse = false
            errands = VoiceJourney.retainPins(result.getJSONArray("errands"), if (prior != null) errands else JSONArray())
            result.put("errands", errands)
            intentReview = prior?.acceptAnswer(result, payload) ?: IntentReview(payload.getString("text"), payload.getString("reference_time"), false, result)
            request = (retainedRequest ?: JSONObject()).put("deadline", intentReview?.deadline ?: JSONObject.NULL)
            prefs.edit().putString("draft-deadline", intentReview?.deadline).apply()
            val warnings = result.optJSONArray("warnings") ?: JSONArray()
            status = ((0 until warnings.length()).joinToString(" · ") { humanMessage(warnings.getString(it)) } + " " + result.optJSONArray("clarification_questions")?.join(" · ").orEmpty()).trim()
            if (status.isBlank()) status = "정리된 내용을 확인하고 출발 · 귀가 위치를 입력하세요."
            reviewNotes = status.takeIf { warnings.length() > 0 || (result.optJSONArray("clarification_questions")?.length() ?: 0) > 0 }
            flow.parsed(); persist(); render(); continueProposal()
        }
    }
    private fun loadFixture(clarify: Boolean = false, scenario: String = "basic", voiceJourney: Boolean = false) {
        check(BuildConfig.DEMO)
        val client = api()
        async({ client.call("/v1/demo?scenario=$scenario") }) { result ->
            cancelOwnedNavigation()
            request = result; errands = result.getJSONArray("errands"); fixture = true; plan = null
            prefs.edit().putBoolean("sample-experience", true).apply()
            pendingParse = null; pendingPrior = null
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
            if (voiceJourney) {
                for (i in 0 until errands.length()) {
                    val item = errands.getJSONObject(i)
                    item.put("place_mode", if (item.optString("id") == "laundry") "fixed" else "flexible")
                    if (item.optString("id") == "laundry") {
                        item.put("latest_finish", "2026-10-23T18:30:00+09:00")
                        item.put("selected_place", JSONObject(item.getJSONArray("candidates").getJSONObject(0).toString()))
                    }
                }
                intentReview = IntentReview(VoiceJourney.EXAMPLE_REQUEST, result.getString("departure_time"), true,
                    JSONObject().put("errands", errands).put("deadline", result.getString("deadline")).put("clarification_questions", JSONArray()).put("warnings", JSONArray()))
                prefs.edit().putString("input", VoiceJourney.EXAMPLE_REQUEST).apply()
            }
            prefs.edit().putString("delay", "0").apply(); flow.parsed(); persist(); render()
            if (voiceJourney) continueProposal()
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
        require(fixture || LocationSetup.originReady(prefs.getString("live-origin", null), originLat.text.toString(), originLon.text.toString(), System.currentTimeMillis())) {
            "출발 위치가 없거나 오래됐어요. 출발 · 귀가 설정에서 현재 위치를 다시 확인해 주세요."
        }
        require(!requiresParse) { "입력 문장이 바뀌었습니다. 먼저 할 일을 다시 정리하세요." }
        require(intentReview?.ready != false) { "확인 질문과 귀가 시간에 먼저 답해 주세요." }
        val remaining = if (fixture) PlaceReview.fixtureErrands(errands, state.completed) else PlaceReview.discoveryErrands(errands, state.completed)
        require(VoiceJourney.placesReady(remaining, fixture)) { "맡긴 물품·지정 점포는 정확한 방문 장소를 확인해 주세요." }
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
            announcePlan()
        }
    }

    private fun renderPlan() {
        if ((navigationRecovery || flow.screen == JourneyScreen.ROUTE) && voiceDialog?.isShowing != true) render() else applyLock()
    }
    private fun firstRemainingStop(result: JSONObject): JSONObject? {
        val stops = result.optJSONArray("stops") ?: return null
        for (i in 0 until stops.length()) if (stops.getJSONObject(i).getString("errand_id") !in state.completed) return stops.getJSONObject(i)
        return null
    }
    private fun approveNavigation(stop: JSONObject, displayedRevision: Long) {
        check(!navigationRecovery) { "기존 차량 내비 안내를 먼저 확인해 주세요." }
        check(state.acceptsResponse(displayedRevision)) { "계획이 바뀌었어요. 최신 계획을 다시 확인해 주세요." }
        check(SpokenPlan.canApprove(plan, errands, state.completed)) { "필수 볼일과 귀가를 포함한 현재 계획을 먼저 확인해 주세요." }
        check(stop.optString("errand_id") == plan?.let { SpokenPlan.destinationId(it, state.completed) }) { "다음 목적지가 바뀌었어요. 다시 확인해 주세요." }
        ensureNavigationAllowed()
        stopSpoken(true)
        val place = stop.getJSONObject("place")
        if (!allowed() || busy) return
        val id = stop.getString("errand_id")
        state.approve(id)
        if (!state.requestNavigation(id)) return
        navigationHandoff.arm(displayedRevision, id); mapMessage=null
        if (simulate && BuildConfig.DEMO) { onNavigation("requested"); onNavigation("accepted") }
        else runCatching { navigation.request(place.getDouble("latitude"), place.getDouble("longitude"), place.getString("name")) }.onFailure {
            navigationHandoff.cancel(); state.routeFailed(); showError(it)
        }
        renderPlan()
    }
    private fun completeCurrent(stop: JSONObject, displayedRevision: Long) {
        if (!allowed() || busy) return
        val id = stop.getString("errand_id")
        if (!state.completeForRevision(id, displayedRevision)) return
        val place = stop.getJSONObject("place")
        request?.put("origin", JSONObject().put("id", place.getString("id")).put("name", place.getString("name")).put("latitude", place.getDouble("latitude")).put("longitude", place.getDouble("longitude")))
        request?.let { syncDraftLocations(it) }
        if (fixture) request?.put("departure_time", stop.getString("end_time"))
        plan = null; status = "완료했어요. 남은 동선을 준비할게요."
        flow.parsed(); persist(); render()
        if (!fixture) navigation.connect()
        runCatching { calculate() }.onFailure { showError(it) }
    }
    private fun onNavigation(event: String) {
        when {
            event == "connected" -> navStatus = "Pleos 서비스 연결됨 · 경로 요청 전"
            event == "requested" -> navStatus = if (simulate) "시뮬레이션 · 요청 발생" else "Pleos 요청 전송됨 · 경로 수락 미확인"
            event == "accepted" -> { state.routeAccepted(); navStatus = if (simulate) "시뮬레이션 · 경로 탐색 완료 이벤트" else "Pleos 경로 탐색 완료 이벤트 수신" }
            event == "arrived" -> { navigationHandoff.cancel(); mapMessage=null; state.arrived(); navStatus = "목적지 도착 이벤트 수신 · 사용자의 할 일 완료 확인 필요" }
            event.startsWith("error:") -> { navigationHandoff.cancel(); mapMessage=null; navStatus = event.removePrefix("error:"); state.routeFailed(navStatus) }
        }
        renderPlan(); persist()
        if (event=="accepted" && navigationHandoff.take(state.revision,
                state.activeId?.takeIf { state.arrivedId==null },state.guidanceAccepted,
                foreground && flow.screen==JourneyScreen.ROUTE, vehicle, fixture || simulate)) showVehicleNavigation()
    }
    private fun cancelOwnedNavigation() {
        navigationHandoff.cancel(); mapMessage=null
        if (::navigation.isInitialized && allowed() && navigation.hasActiveRequest) navigation.cancelPending()
    }
    private fun persist() {
        prefs.edit().putString("request", request?.toString()).putString("plan", plan?.toString()).putString("errands", errands.toString())
            .putBoolean("fixture", fixture).putBoolean("requiresParse", requiresParse).putStringSet("completed", state.completed.toSet()).apply()
        prefs.edit().putBoolean("guidance-unresolved", navigationRecovery || !fixture && state.activeId!=null && state.arrivedId==null).apply()
        prefs.edit().putString("review-notes", reviewNotes).putString("intent-review", intentReview?.toJson()).apply()
    }
    private fun restore() {
        runCatching {
            voiceExample = BuildConfig.DEMO && prefs.getBoolean("voice-example", false) && prefs.getString("input", "") == VoiceInputSession.EXAMPLE_TEXT
            fixture = prefs.getBoolean("fixture", false)
            navigationRecovery = !fixture && prefs.getBoolean("guidance-unresolved", false)
            if (fixture && BuildConfig.DEMO) prefs.edit().putBoolean("sample-experience", true).apply()
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
        if (!fixture && state.completed.isNotEmpty()) {
            payload.optJSONObject("origin")?.let { origin ->
                edit.putString("live-origin", SetupLocation(origin.getDouble("latitude"), origin.getDouble("longitude"),
                    LocationSource.COMPLETED_STOP, "마지막 완료 장소").toJson())
            }
        }
        for ((key, location, coordinate) in listOf(Triple("originLat", "origin", "latitude"), Triple("originLon", "origin", "longitude"), Triple("homeLat", "home", "latitude"), Triple("homeLon", "home", "longitude"))) {
            payload.optJSONObject(location)?.let { edit.putString("draft-$key", it.optString(coordinate)) }
        }
        edit.apply()
    }
    private fun clock(value: String): String = runCatching { OffsetDateTime.parse(value).format(DateTimeFormatter.ofPattern("HH:mm")) }.getOrDefault("미확인")
    private fun minutes(seconds: Int) = (seconds + 59) / 60
    private fun minutesBetween(start: String, end: String) = runCatching { java.time.Duration.between(OffsetDateTime.parse(start), OffsetDateTime.parse(end)).toMinutes().toString() }.getOrDefault("?")
}

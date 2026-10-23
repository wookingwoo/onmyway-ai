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
    private val bg = Color.rgb(11, 21, 29)
    private val panel = Color.rgb(20, 35, 45)
    private val ink = Color.rgb(235, 244, 247)
    private val muted = Color.rgb(158, 179, 191)
    private val teal = Color.rgb(112, 224, 196)
    private val amber = Color.rgb(255, 200, 112)
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
    private var status = "정차 상태를 확인한 뒤 계획을 시작하세요."
    private var navStatus = "Pleos 내비 연결 확인 중"
    private var computedElapsedMillis: Long? = null
    private lateinit var restrictions: VehicleRestrictions
    private lateinit var navigation: PleosNavigation
    private lateinit var root: LinearLayout
    private lateinit var right: LinearLayout
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
        render()
        restrictions = VehicleRestrictions(this) { value ->
            if (vehicle != value) { vehicle = value; applyLock() }
        }
        navigation = PleosNavigation(this) { event -> runOnUiThread { onNavigation(event) } }
        navigation.connect()
    }
    override fun onStart() { super.onStart(); if (::restrictions.isInitialized) restrictions.start() }
    override fun onStop() { if (::restrictions.isInitialized) restrictions.stop(); vehicle = VehicleState.UNKNOWN; applyLock(); super.onStop() }
    override fun onDestroy() { navigation.close(); worker.shutdownNow(); super.onDestroy() }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    private fun column() = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
    private fun row() = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
    private fun shape(color: Int, radius: Int = 14) = GradientDrawable().apply { setColor(color); cornerRadius = dp(radius).toFloat() }
    private fun text(value: String, size: Float = 16f, color: Int = ink, bold: Boolean = false) = TextView(this).apply {
        text = value; textSize = size; setTextColor(color)
        if (bold) setTypeface(typeface, Typeface.BOLD)
        setPadding(0, dp(5), 0, dp(5))
    }
    private fun LinearLayout.add(view: View, margin: Int = 6) {
        addView(view, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(margin) })
    }
    private fun card(parent: LinearLayout, title: String, subtitle: String? = null): LinearLayout {
        val card = column().apply { background = shape(panel); setPadding(dp(18), dp(14), dp(18), dp(16)) }
        card.add(text(title, 19f, ink, true), 4)
        if (subtitle != null) card.add(text(subtitle, 13f, muted), 9)
        parent.add(card, 12)
        return card
    }
    private fun field(parent: LinearLayout, label: String, value: String, multiline: Boolean = false, onChange: (String) -> Unit): EditText {
        parent.add(text(label, 12f, muted), 0)
        val editor = EditText(this).apply {
            setText(value); textSize = 16f; setTextColor(ink); setHintTextColor(muted)
            backgroundTintList = android.content.res.ColorStateList.valueOf(teal)
            setPadding(dp(8), dp(8), dp(8), dp(8))
            inputType = InputType.TYPE_CLASS_TEXT or if (multiline) InputType.TYPE_TEXT_FLAG_MULTI_LINE else 0
            setSingleLine(!multiline)
            if (multiline) { minLines = 2; maxLines = 4; gravity = Gravity.TOP }
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
            text = label; textSize = 15f; isAllCaps = false; minHeight = dp(52)
            setTextColor(if (primary) bg else ink)
            backgroundTintList = android.content.res.ColorStateList.valueOf(if (primary) teal else Color.rgb(39, 61, 73))
            setOnClickListener { if (allowed() && !busy && enabled()) runCatching(action).onFailure { showError(it) } }
        }
        controls += button to enabled; parent.add(button)
        return button
    }
    private fun allowed() = state.canInteract(vehicle, BuildConfig.DEMO, simulate)
    private fun edited() {
        state.edited(); plan = null; status = "변경 사항이 있습니다. 동선을 다시 계산하세요."
        persist(); renderPlan(); applyLock()
    }
    private fun render() {
        controls.clear()
        root = column().apply { setBackgroundColor(bg); setPadding(dp(24), dp(10), dp(24), dp(10)) }
        root.setOnApplyWindowInsetsListener { view, insets ->
            @Suppress("DEPRECATION")
            view.setPadding(dp(24) + insets.systemWindowInsetLeft, dp(10) + insets.systemWindowInsetTop,
                dp(24) + insets.systemWindowInsetRight, dp(10) + insets.systemWindowInsetBottom)
            insets
        }
        val header = row()
        val brand = column().apply { add(text("OnMyWay", 27f, ink, true), 0); add(text("퇴근길에, 오늘의 할 일까지", 13f, muted), 0) }
        header.addView(brand, LinearLayout.LayoutParams(0, -2, 1f))
        header.addView(text(if (BuildConfig.DEMO) "DEMO · 예시 / 시뮬레이션" else "LIVE · 실제 데이터", 13f, if (BuildConfig.DEMO) amber else teal, true))
        root.add(header, 6)
        lockLabel = text("", 14f, amber).apply { background = shape(panel, 8); setPadding(dp(12), dp(7), dp(12), dp(7)) }
        root.add(lockLabel, 6)
        if (BuildConfig.DEMO) {
            val toggle = Switch(this).apply {
                text = "데모 조작 시뮬레이션 (차량 정차 상태 아님 · 실제 내비 요청 안 함)"
                textSize = 12f; setTextColor(amber); isChecked = simulate
                setOnCheckedChangeListener { _, checked ->
                    simulate = checked; state.routeFailed(); navStatus = if (checked) "시뮬레이션 · 실제 내비 요청 안 함" else "Pleos 연결 상태: 재확인 필요"
                    state.edited(); applyLock(); renderPlan()
                }
            }
            root.add(toggle, 3)
        }
        val body = row().apply { gravity = Gravity.TOP }
        val left = column().apply { setPadding(0, 0, dp(14), 0) }
        right = column()
        body.addView(ScrollView(this).apply { isFillViewport = true; addView(left) }, LinearLayout.LayoutParams(0, -1, .43f))
        body.addView(ScrollView(this).apply { isFillViewport = true; addView(this@MainActivity.right) }, LinearLayout.LayoutParams(0, -1, .57f))
        root.addView(body, LinearLayout.LayoutParams(-1, 0, 1f))
        statusLabel = text(status, 12f, muted)
        root.add(statusLabel, 0)
        setContentView(root)
        root.requestApplyInsets()

        val start = card(left, "01  오늘, 어떤 일을 할까요?", "자연어를 확인 가능한 할 일로 바꿉니다.")
        input = field(start, "하고 싶은 일", prefs.getString("input", "약국에 들르고 세탁물을 찾은 뒤 20시까지 집에 가고 싶어").orEmpty(), true) {
            prefs.edit().putString("input", it).apply(); requiresParse = true; edited()
        }
        button(start, "할 일 정리하기", true) { parse() }
        if (BuildConfig.DEMO) button(start, "예시 시나리오 불러오기 · 서버 fixture") { loadFixture() }

        val settingsHost = card(left, "출발 · 귀가", if (fixture) "예시 위치와 날짜 · 실제 현재 위치가 아닙니다" else "직접 설정한 출발지와 집으로 계획합니다")
        settingsHost.add(text("귀가 마감 ${clock(prefs.getString("draft-deadline", request?.optString("deadline")).orEmpty())}", 16f, teal, true))
        val settings = column().apply { visibility = if (!fixture && draft("originLat", "origin", "latitude").isBlank()) View.VISIBLE else View.GONE }
        button(settingsHost, "위치 · 시간 · 서버 설정 펼치기") { settings.visibility = if (settings.visibility == View.VISIBLE) View.GONE else View.VISIBLE }
        settingsHost.add(settings)
        server = field(settings, "백엔드 서버 (키는 서버에서만 설정)", prefs.getString("server", BuildConfig.DEFAULT_SERVER_URL).orEmpty()) {
            prefs.edit().putString("server", it).apply(); edited()
        }
        originLat = field(settings, "출발 위도", draft("originLat", "origin", "latitude")) { saveDraft("originLat", it) }
        originLon = field(settings, "출발 경도", draft("originLon", "origin", "longitude")) { saveDraft("originLon", it) }
        homeLat = field(settings, "귀가 위도", draft("homeLat", "home", "latitude")) { saveDraft("homeLat", it) }
        homeLon = field(settings, "귀가 경도", draft("homeLon", "home", "longitude")) { saveDraft("homeLon", it) }
        deadline = field(settings, "귀가 마감 (예: 2026-10-23T20:00:00+09:00)", prefs.getString("draft-deadline", request?.optString("deadline") ?: OffsetDateTime.now().withHour(23).withMinute(0).withSecond(0).toString()).orEmpty()) { saveDraft("deadline", it) }
        delay = field(settings, "재계산 시 추가 지연 (분)", "0") { edited() }

        val taskCard = card(left, "02  할 일 확인 · 수정", "필수 여부, 체류 시간과 개별 마감을 확인하세요.")
        if (errands.length() == 0) taskCard.add(text("먼저 할 일을 정리하거나 예시를 불러오세요.", 14f, muted))
        for (i in 0 until errands.length()) {
            val errand = errands.getJSONObject(i)
            taskCard.add(text("${i + 1}. ${if (errand.optString("id") in state.completed) "완료한 일" else "남은 일"}", 13f, teal, true))
            field(taskCard, "할 일 이름", errand.optString("label")) { errand.put("label", it); edited() }
            if (!fixture) field(taskCard, "장소 검색어", errand.optString("query")) { errand.put("query", it); edited() }
            field(taskCard, "체류 시간 (분)", (errand.optInt("dwell_seconds", 600) / 60).toString()) {
                errand.put("dwell_seconds", (it.toIntOrNull() ?: 0) * 60); edited()
            }.inputType = InputType.TYPE_CLASS_NUMBER
            field(taskCard, "이 일의 완료 마감 (ISO / 없으면 빈칸)", errand.optString("latest_finish", "").takeUnless { it == "null" }.orEmpty()) {
                errand.put("latest_finish", if (it.isBlank()) JSONObject.NULL else it); edited()
            }
            val mandatory = CheckBox(this).apply {
                text = "반드시 할 일"; setTextColor(ink); isChecked = errand.optBoolean("mandatory", true)
                setOnCheckedChangeListener { _, checked -> errand.put("mandatory", checked); edited() }
            }
            controls += mandatory to { true }; taskCard.add(mandatory)
        }
        button(taskCard, "남은 일정으로 동선 계산", true, { errands.length() > 0 && !requiresParse }) { calculate() }
        if (requiresParse) taskCard.add(text("입력 문장이 바뀌었습니다. 먼저 ‘할 일 정리하기’를 눌러 주세요.", 13f, amber))
        button(taskCard, "새 일정 시작 · 저장된 진행 초기화") {
            AlertDialog.Builder(this).setTitle("저장된 진행을 지울까요?").setMessage("완료 기록과 현재 계획을 초기화합니다.")
                .setNegativeButton("취소", null).setPositiveButton("초기화") { _, _ -> if (allowed()) {
                    state.completed.clear(); state.routeFailed(); state.edited(); request = null; plan = null; errands = JSONArray(); fixture = false; persist(); render()
                } }.show()
        }
        renderPlan(); applyLock()
    }

    private fun applyLock() {
        if (!::lockLabel.isInitialized) return
        val label = when (vehicle) {
            VehicleState.PARKED -> "● 차량 UX 제한 해제 확인 · 조작 가능"
            VehicleState.RESTRICTED -> "● 주행 / 차량 UX 제한 중 · 편집과 목적지 요청 잠금"
            VehicleState.UNKNOWN -> if (simulate && BuildConfig.DEMO) "● 차량 상태 알 수 없음 · 데모 시뮬레이션만 조작 가능" else "● 차량 상태 알 수 없음 · 안전을 위해 조작 잠금"
        }
        lockLabel.text = label
        lockLabel.setTextColor(if (vehicle == VehicleState.PARKED) teal else amber)
        controls.forEach { (view, enabled) -> view.isEnabled = allowed() && !busy && enabled(); view.alpha = if (view.isEnabled) 1f else .45f }
        if (::statusLabel.isInitialized) statusLabel.text = if (busy) "서버에서 확인 중… 입력을 바꾸면 이전 응답은 적용하지 않습니다." else status
    }
    private fun api() = ApiClient(server.text.toString())
    private fun async(work: () -> JSONObject, done: (JSONObject) -> Unit) {
        val revision = state.revision
        busy = true; applyLock()
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
        val client = api()
        async({ client.call("/v1/parse", payload) }) { result ->
            fixture = false; request = null; plan = null; state.routeFailed(); state.completed.clear(); state.edited()
            requiresParse = false
            errands = result.getJSONArray("errands")
            request = JSONObject().put("deadline", result.optString("deadline").takeUnless { it.isBlank() || it == "null" }
                ?: OffsetDateTime.now().withHour(23).withMinute(0).withSecond(0).toString())
            prefs.edit().putString("draft-deadline", request?.optString("deadline")).apply()
            val warnings = result.optJSONArray("warnings") ?: JSONArray()
            status = ((0 until warnings.length()).joinToString(" · ") { humanMessage(warnings.getString(it)) } + " " + result.optJSONArray("clarification_questions")?.join(" · ").orEmpty()).trim()
            if (status.isBlank()) status = "정리된 내용을 확인하고 출발 · 귀가 위치를 입력하세요."
            persist(); render()
        }
    }
    private fun loadFixture() {
        check(BuildConfig.DEMO)
        val client = api()
        async({ client.call("/v1/demo") }) { result ->
            request = result; errands = result.getJSONArray("errands"); fixture = true; plan = null
            requiresParse = false
            syncDraftLocations(result)
            prefs.edit().putString("draft-deadline", result.getString("deadline")).putString("input", "예시: " + (0 until errands.length()).joinToString(" · ") { errands.getJSONObject(it).optString("label") }).apply()
            state.completed.clear(); state.routeFailed(); state.edited()
            status = "예시 데이터를 불러왔습니다. 영업시간 · 이동 시간은 시연용 fixture입니다."
            persist(); render()
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
        val remaining = JSONArray()
        for (i in 0 until errands.length()) {
            val e = JSONObject(errands.getJSONObject(i).toString())
            if (e.getString("id") !in state.completed) remaining.put(e)
        }
        val deadlineValue = OffsetDateTime.parse(deadline.text.toString()).toString()
        val delayMinutes = delay.text.toString().toLongOrNull() ?: error("지연 시간은 분 단위 숫자여야 합니다.")
        require(delayMinutes in 0..1440) { "지연 시간은 0~1440분이어야 합니다." }
        val payload = if (fixture) JSONObject(requireNotNull(request).toString()) else JSONObject()
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
        val client = api()
        async({
            val ready = if (fixture) PlanPayload.trimFixtureEdges(payload) else client.call("/v1/discover", payload)
            val result = client.call("/v1/plan", ready)
            JSONObject().put("request", ready).put("plan", result)
        }) { result ->
            require(!result.getJSONObject("plan").optBoolean("is_fixture") || BuildConfig.DEMO) { "LIVE 앱에서는 fixture 결과를 사용할 수 없습니다." }
            state.edited(); state.routeFailed(); request = result.getJSONObject("request"); plan = result.getJSONObject("plan")
            fixture = plan?.optBoolean("is_fixture", fixture) ?: fixture
            computedElapsedMillis = SystemClock.elapsedRealtime()
            status = "계획을 계산했습니다. 데이터 경고와 마감을 확인한 뒤 다음 목적지를 승인하세요."
            persist(); renderPlan()
        }
    }

    private fun renderPlan() {
        if (!::right.isInitialized) return
        controls.removeAll { (view, _) -> isDescendant(view, right) }
        right.removeAllViews()
        val intro = card(right, "03  오늘의 귀갓길", "이동 + 대기 + 체류 + 귀가 시간을 함께 확인합니다.")
        intro.add(text(if (fixture) "FIXTURE · 시연용 장소 / 영업 / 이동 데이터" else if (plan == null) "LIVE · 실제 API 모드 · 조회 전" else "LIVE · 실제 제공자 조회 결과", 12f, if (fixture) amber else teal, true))
        intro.add(text(navStatus, 12f, muted))
        if (restored) intro.add(text("저장된 진행을 복원했습니다. 내비 승인은 복원하지 않습니다.", 12f, amber))
        val result = plan
        if (result == null) {
            intro.add(text("하루의 마지막 이동을\n가장 편안한 순서로.", 29f, ink, true))
            intro.add(text("할 일을 확인하고 동선을 계산하면\n방문 순서와 확인이 필요한 정보를 보여 드립니다.", 15f, muted))
            if (state.completed.isNotEmpty()) intro.add(text("완료한 할 일 ${state.completed.size}개 · 남은 일정 재계산 가능", 15f, teal))
            return
        }
        val outcome = result.optString("status")
        intro.add(text(when (outcome) { "feasible" -> if (fixture) "예시 조건에서 시간 충족" else "확인된 조건에서 시간 충족"; "infeasible" -> "현재 조건으로는 완료 불가"; else -> "추가 확인 필요 · 가능 여부 미확정" }, 23f, if (outcome == "feasible") teal else amber, true))
        for (key in listOf("warnings", "reasons")) {
            val warnings = result.optJSONArray(key) ?: continue
            for (i in 0 until warnings.length()) intro.add(text("• ${humanMessage(warnings.getString(i))}", 13f, amber), 1)
        }
        result.optJSONObject("relaxation")?.let { relaxation ->
            intro.add(text("마감 변경 제안 · 자동 적용하지 않음", 16f, amber, true))
            intro.add(text("귀가 마감을 ${clock(relaxation.optString("required_deadline"))}까지 ${minutes(relaxation.optInt("extension_seconds"))}분 연장하면 시간 조건을 충족할 수 있습니다.", 14f, amber))
            button(intro, "제안 마감을 직접 적용하고 재계산") {
                val proposed = relaxation.getString("required_deadline")
                AlertDialog.Builder(this).setTitle("귀가 마감을 바꿀까요?")
                    .setMessage("${clock(proposed)}까지로 변경합니다. 다음 목적지는 새 계획에서 다시 승인해야 합니다.")
                    .setNegativeButton("유지", null).setPositiveButton("변경 후 재계산") { _, _ -> if (allowed()) {
                        deadline.setText(proposed); calculate()
                    } }.show()
            }
        }
        val stops = result.optJSONArray("stops") ?: JSONArray()
        for (i in 0 until stops.length()) {
            val stop = stops.getJSONObject(i)
            val place = stop.getJSONObject("place")
            val id = stop.getString("errand_id")
            val next = firstRemainingStop(result)?.optString("errand_id") == id
            val stopCard = card(right, "${String.format("%02d", i + 1)}  ${stop.optString("label")}", place.optString("name"))
            stopCard.add(text("${clock(stop.optString("arrival_time"))} 도착  →  ${clock(stop.optString("end_time"))} 완료 예정", 17f, ink, true))
            stopCard.add(text("이동 ${minutes(stop.optInt("travel_seconds"))}분 · 대기 ${minutes(stop.optInt("wait_seconds"))}분 · 체류 ${minutesBetween(stop.optString("start_time"), stop.optString("end_time"))}분", 13f, muted))
            val source = place.optJSONObject("source")
            stopCard.add(text("장소 출처 ${providerName(source?.optString("provider"))} · 영업 ${evidenceLabel(place.optString("hours_status"))} · 서비스 ${evidenceLabel(place.optString("service_status"))}", 12f, amber))
            if (source != null) stopCard.add(text("조회 ${source.optString("retrieved_at")}", 11f, muted))
            when {
                id in state.completed -> stopCard.add(text("✓ 사용자가 완료 확인함", 16f, teal, true))
                state.arrivedId == id -> {
                    stopCard.add(text("목적지 도착 · 할 일은 아직 완료되지 않았습니다", 14f, amber, true))
                    button(stopCard, "이 할 일을 직접 완료 확인", true) { confirmCompletion(stop) }
                }
                state.activeId == id -> {
                    stopCard.add(text("안내 요청 진행 중 · 내비 응답을 기다립니다", 14f, teal))
                    if (simulate && BuildConfig.DEMO) button(stopCard, "시뮬레이션 · 도착 이벤트 보내기") { onNavigation("arrived") }
                }
                next && outcome != "infeasible" -> button(stopCard, "다음 목적지 확인 · ${place.optString("name")}", true) { confirmNavigation(stop, outcome) }
            }
        }
        val skipped = result.optJSONArray("skipped_errands") ?: JSONArray()
        if (skipped.length() > 0) {
            val omitted = card(right, "포함되지 않은 할 일 · 확인 필요")
            for (i in 0 until skipped.length()) {
                val item = skipped.getJSONObject(i)
                omitted.add(text("${errandLabel(item.optString("id"))} · ${humanMessage(item.optString("reason"))}", 13f, amber))
            }
        }
        val home = card(right, "마지막 목적지 · 집")
        home.add(text("${clock(result.optString("home_arrival_time"))} 도착 예정", 23f, teal, true))
        home.add(text("귀가 마감 ${clock(request?.optString("deadline").orEmpty())} · 총 이동 ${minutes(result.optInt("total_travel_seconds"))}분", 13f, muted))
        val hasUnfinishedMandatory = (0 until errands.length()).any { errands.getJSONObject(it).let { item -> item.optBoolean("mandatory", true) && item.optString("id") !in state.completed } }
        if (firstRemainingStop(result) == null && !hasUnfinishedMandatory && outcome != "infeasible" && !result.isNull("home_arrival_time")) {
            when (state.activeId) {
                "__home" -> {
                    home.add(text(if (state.arrivedId == "__home") "집에 도착했습니다 · 오늘 일정 끝" else "집으로 안내 중 · 도착 이벤트 대기", 16f, teal, true))
                    if (simulate && BuildConfig.DEMO && state.arrivedId == null) button(home, "시뮬레이션 · 집 도착 이벤트") { onNavigation("arrived") }
                }
                else -> button(home, "마지막 목적지 확인 · 집", true) {
                    confirmNavigation(JSONObject().put("errand_id", "__home").put("label", "귀가").put("place", requireNotNull(request).getJSONObject("home")), outcome)
                }
            }
        }
        button(home, "지연 반영 · 남은 일정 재계산", false) { calculate() }
        applyLock()
    }
    private fun isDescendant(view: View, ancestor: ViewGroup): Boolean {
        var parent = view.parent
        while (parent is View) { if (parent === ancestor) return true; parent = parent.parent }
        return false
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
        AlertDialog.Builder(this).setTitle("다음 목적지로 안내할까요?")
            .setMessage("$warning${place.optString("name")}\n위도 ${place.optDouble("latitude")} / 경도 ${place.optDouble("longitude")}\n\n${if (simulate) "시뮬레이션 이벤트만 발생합니다." else "승인 후 Pleos에 이 목적지 1개를 요청합니다."}\n도착해도 할 일은 자동 완료되지 않습니다.")
            .setNegativeButton("다시 확인", null).setPositiveButton("목적지 승인") { _, _ ->
                if (!allowed() || !state.acceptsResponse(currentRevision)) return@setPositiveButton
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
            }.show()
    }
    private fun confirmCompletion(stop: JSONObject) {
        AlertDialog.Builder(this).setTitle("할 일을 마쳤나요?").setMessage("${stop.optString("label")}\n\n완료를 확인하면 남은 할 일만 다시 계산할 수 있습니다.")
            .setNegativeButton("아직이에요", null).setPositiveButton("완료했어요") { _, _ -> if (allowed()) {
                val id = stop.getString("errand_id")
                state.complete(id)
                if (id !in state.completed) return@setPositiveButton
                val place = stop.getJSONObject("place")
                request?.put("origin", JSONObject().put("id", place.getString("id")).put("name", place.getString("name")).put("latitude", place.getDouble("latitude")).put("longitude", place.getDouble("longitude")))
                request?.let { syncDraftLocations(it) }
                if (fixture) request?.put("departure_time", stop.getString("end_time"))
                plan = null; status = "완료를 저장했습니다. 현재 위치와 지연을 확인하고 남은 일정을 계산하세요."
                persist(); render()
            } }.show()
    }
    private fun onNavigation(event: String) {
        when {
            event == "connected" -> navStatus = "Pleos 서비스 연결됨 · 경로 요청 전"
            event == "requested" -> navStatus = if (simulate) "시뮬레이션 · 요청 발생" else "Pleos 요청 전송됨 · 경로 수락 미확인"
            event == "accepted" -> { state.routeAccepted(); navStatus = if (simulate) "시뮬레이션 · 경로 탐색 완료 이벤트" else "Pleos 경로 탐색 완료 이벤트 수신" }
            event == "arrived" -> { state.arrived(); navStatus = "목적지 도착 이벤트 수신 · 사용자의 할 일 완료 확인 필요" }
            event.startsWith("error:") -> { state.routeFailed(); navStatus = event.removePrefix("error:") }
        }
        renderPlan(); persist()
    }
    private fun persist() {
        prefs.edit().putString("request", request?.toString()).putString("plan", plan?.toString()).putString("errands", errands.toString())
            .putBoolean("fixture", fixture).putBoolean("requiresParse", requiresParse).putStringSet("completed", state.completed.toSet()).apply()
    }
    private fun restore() {
        runCatching {
            fixture = prefs.getBoolean("fixture", false)
            requiresParse = prefs.getBoolean("requiresParse", false)
            if (fixture && !BuildConfig.DEMO) return@runCatching
            request = prefs.getString("request", null)?.let(::JSONObject)
            plan = prefs.getString("plan", null)?.let(::JSONObject)
            if (!fixture) { plan = null; status = "이전 진행을 복원했습니다. 최신 정보로 동선을 다시 계산하세요." }
            errands = JSONArray(prefs.getString("errands", "[]"))
            state.completed.addAll(prefs.getStringSet("completed", emptySet()).orEmpty())
            restored = errands.length() > 0
        }.onFailure { request = null; plan = null; errands = JSONArray() }
    }
    private fun showError(error: Throwable) { status = error.message ?: "처리 중 오류가 발생했습니다. 다시 확인하세요."; applyLock(); Toast.makeText(this, status, Toast.LENGTH_LONG).show() }
    private fun navigationAllowed() = NavigationPolicy.canApprove(fixture, BuildConfig.DEMO, simulate,
        System.currentTimeMillis(), runCatching { OffsetDateTime.parse(request?.optString("deadline")).toInstant().toEpochMilli() }.getOrNull(),
        SystemClock.elapsedRealtime(), computedElapsedMillis)
    private fun navigationBlockMessage() = if (fixture) "예시 목적지는 실제 내비로 보낼 수 없습니다. 데모 조작 시뮬레이션을 켜 주세요." else "계획이 오래되었거나 마감이 지났습니다. 최신 정보로 동선을 다시 계산하세요."
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

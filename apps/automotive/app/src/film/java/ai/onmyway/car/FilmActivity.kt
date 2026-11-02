package ai.onmyway.car

import android.app.Activity
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.SystemClock
import android.view.*
import android.widget.*

/** Installed native filming prototype. Authored scenario, explicitly isolated flavor.
 * Real CarUxRestrictions are always authoritative; unknown state never permits edits.
 */
class FilmActivity : Activity() {
    private val bg = Color.rgb(15,23,29)
    private val panel = Color.rgb(28,40,48)
    private val ink = Color.rgb(242,246,243)
    private val muted = Color.rgb(166,185,192)
    private val mint = Color.rgb(166,231,204)
    private val amber = Color.rgb(245,188,127)
    private val journey = FilmJourney()
    private var vehicle = VehicleState.UNKNOWN
    private lateinit var restrictions: VehicleRestrictions
    private lateinit var root: LinearLayout
    private val stage get() = journey.stage
    private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
    private fun column()=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL }
    private fun row()=LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL; gravity=Gravity.CENTER_VERTICAL }
    private fun shape(color:Int,radius:Int=26)=GradientDrawable().apply { setColor(color);cornerRadius=dp(radius).toFloat() }
    private fun text(value:String,size:Float=26f,color:Int=ink,bold:Boolean=false)=TextView(this).apply {
        text=value;textSize=size;setTextColor(color);fontFeatureSettings="tnum"
        if(bold)typeface=Typeface.create("sans-serif",Typeface.BOLD)
        setPadding(0,dp(5),0,dp(5))
    }
    private fun LinearLayout.add(view:View,gap:Int=18) { addView(view,LinearLayout.LayoutParams(-1,-2).apply { bottomMargin=dp(gap) }) }
    private fun card()=column().apply { background=shape(panel);setPadding(dp(30),dp(25),dp(30),dp(25)) }
    private fun action(value:String,handler:()->Unit)=Button(this).apply {
        text=value;textSize=28f;isAllCaps=false;setTextColor(bg);backgroundTintList=null;background=shape(mint,42)
        minHeight=dp(84);setPadding(dp(32),dp(16),dp(32),dp(16));isEnabled=vehicle==VehicleState.PARKED
        alpha=if(isEnabled)1f else .35f
        setOnClickListener { if(vehicle==VehicleState.PARKED)handler() }
    }
    override fun onCreate(savedInstanceState:Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor=bg;window.navigationBarColor=bg
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility=0
        restrictions=VehicleRestrictions(this) { s -> if(s!=vehicle) { vehicle=s;render() } }
        render()
    }
    override fun onStart() { super.onStart();restrictions.start() }
    override fun onStop() { restrictions.stop();vehicle=VehicleState.UNKNOWN;render();super.onStop() }
    override fun onSaveInstanceState(outState:Bundle) { super.onSaveInstanceState(outState) }
    private fun render() {
        root=column().apply {setBackgroundColor(bg);setPadding(dp(44),dp(24),dp(44),dp(24))}
        val header=row()
        header.addView(text("OnMyWay",32f,ink,true),LinearLayout.LayoutParams(0,-2,1f))
        header.addView(text(if(vehicle==VehicleState.PARKED)"P  ·  정차 중" else if(vehicle==VehicleState.RESTRICTED)"D  ·  주행 중" else "차량 상태 확인 중",21f,muted))
        root.add(header,14)
        root.add(text("가는 길에, 할 일까지.",21f,mint),22)
        if(vehicle!=VehicleState.PARKED) {
            val lock=column().apply {gravity=Gravity.CENTER}
            lock.addView(text("정차 후 이용해 주세요",48f,ink,true))
            lock.addView(text("주행 중에는 입력과 계획 승인을 잠급니다.",28f,muted))
            lock.addView(RouteCanvas(false),LinearLayout.LayoutParams(-1,dp(360)))
            root.addView(lock,LinearLayout.LayoutParams(-1,0,1f));setContentView(root);return
        }
        val content=row().apply {gravity=Gravity.TOP}
        val left=column();val right=column()
        content.addView(left,LinearLayout.LayoutParams(0,-1,.93f).apply {rightMargin=dp(32)})
        content.addView(right,LinearLayout.LayoutParams(0,-1,1.07f))
        root.addView(content,LinearLayout.LayoutParams(-1,0,1f))
        val headings=when(stage) {
            FilmStage.REQUEST -> "가는 길에,\n무엇을 할까요?" to "할 일과 귀가 시간을 함께 말해 주세요."
            FilmStage.CLARIFY -> "마감의 뜻까지\n정확하게." to "한 가지만 확인할게요."
            FilmStage.BASELINE -> "세 가지 용무,\n하나의 여정." to "수령 마감과 귀가 시간을 함께 봤어요."
            FilmStage.CHANGE -> "계획이 달라져도,\n대화는 이어집니다." to "약국 상담 시간을 반영했어요."
            FilmStage.REVISED -> "꼭 해야 할 일은\n지키고." to "우유는 다음에. 필수 용무는 그대로."
            FilmStage.GUIDANCE_LAUNDRY -> "첫 번째 할 일,\n세탁물 수령." to "확인한 계획으로 출발해요."
            FilmStage.ARRIVED_LAUNDRY -> "도착했다고,\n끝난 건 아니니까." to "수령을 마치면 완료를 눌러 주세요."
            FilmStage.NEXT_MEDICINE -> "하나를 끝내면,\n다음 할 일로." to "세탁물 수령을 완료했어요."
            FilmStage.GUIDANCE_MEDICINE -> "다음은,\n약국입니다." to "상담 시간 30분을 유지할게요."
            FilmStage.ARRIVED_MEDICINE -> "상담을 마치고,\n약까지 챙겼다면." to "약 수령을 마쳤는지 확인해 주세요."
            FilmStage.HOME -> "마지막 목적지는,\n집." to "두 가지 필수 용무를 모두 마쳤어요."
            FilmStage.GUIDANCE_HOME -> "이제,\n집으로 가요." to "오늘의 할 일을 끝낸 퇴근길."
            FilmStage.FINISHED -> "할 일은 마치고,\n집에 도착했어요." to "다음 퇴근길에도 OnMyWay."
        }
        left.add(text(headings.first,43f,ink,true),20)
        left.add(text(headings.second,25f,muted),30)
        when(stage) {
            FilmStage.REQUEST -> {
                val quote=card();quote.add(text("“집엔 7시까지.\n6시 반까지 세탁물 찾고,\n약은 꼭. 우유는 시간이 되면.”",30f,ink,true),5);left.add(quote)
                left.addView(SpeechWave(),LinearLayout.LayoutParams(-1,dp(80)).apply {topMargin=dp(20)})
            }
            FilmStage.CLARIFY -> {
                val c=card();c.add(text("18:30까지",34f,mint,true));c.add(text("세탁소 도착인가요,\n수령 완료인가요?",30f,ink,true),4);left.add(c)
                left.add(text("필수 · 세탁물 수령 완료 마감",22f,muted))
            }
            FilmStage.CHANGE -> {
                val c=card();c.add(text("약국에서 상담도 할 거야.",27f));c.add(text("총 30분",62f,amber,true));left.add(c)
                left.add(text("모두 들르면 19:05 귀가 예상",28f,amber,true))
                left.add(text("19:00 마감보다 5분 늦어요.",23f,muted))
            }
            FilmStage.ARRIVED_LAUNDRY,FilmStage.ARRIVED_MEDICINE -> {
                val c=card();c.add(text("도착 · 아직 미완료",26f,amber,true));c.add(text(if(stage==FilmStage.ARRIVED_LAUNDRY)"한결세탁소" else "온유약국",38f,ink,true));c.add(text("실제로 마친 일만 완료해요.",23f,muted),0);left.add(c)
            }
            else -> {
                val time=if(stage==FilmStage.BASELINE)"18:40" else "18:53"
                left.add(text(if(stage==FilmStage.FINISHED)"오늘의 여정 완료" else "귀가 예상",23f,muted),0)
                left.add(text(time,82f,mint,true),12)
                left.add(text(if(stage==FilmStage.BASELINE)"19:00까지 · 20분 여유" else "19:00까지 · 7분 여유",25f,muted),20)
                val c=card();c.add(text("이동  +  영업 대기  +  체류",24f,ink,true),8)
                c.add(text("세탁물 18:05 수령 완료 예정\n18:30 수령 마감 안에",23f,muted),0);left.add(c)
            }
        }
        if(stage in setOf(FilmStage.GUIDANCE_LAUNDRY,FilmStage.GUIDANCE_MEDICINE,FilmStage.GUIDANCE_HOME,FilmStage.FINISHED)) {
            right.addView(RouteCanvas(stage==FilmStage.GUIDANCE_HOME||stage==FilmStage.FINISHED),LinearLayout.LayoutParams(-1,0,1f).apply {bottomMargin=dp(20)})
            right.add(routeCard(if(stage==FilmStage.GUIDANCE_MEDICINE)1 else if(stage==FilmStage.GUIDANCE_LAUNDRY)0 else 3))
        } else {
            for(i in 0..2)right.add(routeCard(i),18)
            val note=card();note.add(text(if(stage==FilmStage.REQUEST||stage==FilmStage.CLARIFY||stage==FilmStage.BASELINE)"집 도착 마감 19:00" else "필수는 그대로 · 우유만 제외",23f,mint,true),0);right.add(note,0)
        }
        val footer=row();footer.setPadding(0,dp(16),0,0)
        footer.addView(text("${stage.ordinal+1} / 13   ·   오늘의 퇴근길",20f,muted),LinearLayout.LayoutParams(0,-2,1f))
        val label=when(stage) {
            FilmStage.REQUEST -> "요청 보내기"
            FilmStage.CLARIFY -> "수령 완료까지"
            FilmStage.BASELINE -> "말로 조건 바꾸기"
            FilmStage.CHANGE -> "수정한 계획 보기"
            FilmStage.REVISED,FilmStage.NEXT_MEDICINE -> "확인하고 출발"
            FilmStage.GUIDANCE_LAUNDRY,FilmStage.GUIDANCE_MEDICINE,FilmStage.GUIDANCE_HOME -> "목적지 도착"
            FilmStage.ARRIVED_LAUNDRY,FilmStage.ARRIVED_MEDICINE -> "수령을 마쳤어요"
            FilmStage.HOME -> "집으로 출발"
            FilmStage.FINISHED -> "오늘의 여정 완료"
        }
        if(stage!=FilmStage.FINISHED)footer.addView(action(label){if(journey.next(vehicle))render()},LinearLayout.LayoutParams(dp(420),dp(90)))
        root.add(footer,0);setContentView(root)
    }
    private fun routeCard(index:Int):LinearLayout {
        val c=card()
        val completed=(index==0&&"laundry" in journey.session.completed)||(index==1&&"medicine" in journey.session.completed)
        val revised=stage.ordinal>=FilmStage.REVISED.ordinal
        val names=listOf("세탁물 수령",if(revised)"약국 상담" else "약 수령","우유 구매","집으로")
        val line=row();line.addView(text(names[index],30f,if(revised&&index==2)muted else ink,true),LinearLayout.LayoutParams(0,-2,1f))
        line.addView(text(if(completed)"완료" else if(index==2)"선택" else if(index==3)"귀가" else "필수",21f,if(completed)mint else muted))
        c.add(line,8)
        val sub=when(index) {
            0 -> if(completed)"한결세탁소 · 수령 완료" else "한결세탁소 · 18:05 완료 예정"
            1 -> if(completed)"온유약국 · 상담과 수령 완료" else if(revised)"온유약국 · 상담 30분 · 18:43 완료 예정" else "온유약국 · 18:18 완료 예정"
            2 -> if(revised)"이번에는 제외 · 필수 용무 유지" else "봄날마트 · 시간이 되면"
            else -> "19:00 마감 · 18:53 도착 예정"
        }
        c.add(text(sub,22f,muted),0)
        if(completed)c.background=shape(Color.rgb(30,65,54))
        return c
    }
    private inner class SpeechWave:View(this@FilmActivity) {
        private val p=Paint(Paint.ANTI_ALIAS_FLAG).apply {color=mint}
        init {minimumHeight=dp(75)}
        override fun onDraw(c:Canvas) {super.onDraw(c);val t=SystemClock.uptimeMillis()/250.0
            for(i in 0..28) {val h=(.15+.7*kotlin.math.abs(kotlin.math.sin(t+i*.72)))*height;val x=i*width/29f
                c.drawRoundRect(x,(height-h).toFloat()/2,x+width/42f,(height+h).toFloat()/2,5f,5f,p)}
            if(isAttachedToWindow)postInvalidateDelayed(40)
        }
    }
    private inner class RouteCanvas(private val home:Boolean):View(this@FilmActivity) {
        private val p=Paint(Paint.ANTI_ALIAS_FLAG)
        override fun onDraw(c:Canvas) {super.onDraw(c);val w=width.toFloat();val h=height.toFloat()
            p.color=panel;c.drawRoundRect(0f,0f,w,h,dp(26).toFloat(),dp(26).toFloat(),p)
            p.strokeWidth=dp(9).toFloat();p.color=Color.rgb(53,69,78)
            for(i in 1..6)c.drawLine(i*w/7,25f,i*w/7-45,h-25,p)
            for(i in 1..5)c.drawLine(20f,i*h/6,w-20,i*h/6-55,p)
            p.color=mint;p.style=Paint.Style.STROKE;p.strokeWidth=dp(10).toFloat();p.strokeJoin=Paint.Join.ROUND
            val route=Path().apply {moveTo(w*.13f,h*.78f);lineTo(w*.4f,h*.78f);lineTo(w*.4f,h*.41f);lineTo(w*.73f,h*.41f);lineTo(w*.86f,h*.22f)}
            c.drawPath(route,p);p.style=Paint.Style.FILL;c.drawCircle(w*.86f,h*.22f,dp(17).toFloat(),p)
            p.color=ink;c.drawCircle(w*.4f,h*.62f,dp(15).toFloat(),p)
            p.textSize=dp(24).toFloat();p.typeface=Typeface.create("sans-serif",Typeface.BOLD)
            c.drawText(if(home)"집으로 가는 길" else "다음 목적지로",dp(24).toFloat(),h-dp(30),p)
        }
    }
}

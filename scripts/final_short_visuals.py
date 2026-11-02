"""Playful original quest/milk comedy, designed separately for vertical video."""
import math
from PIL import Image, ImageDraw
from final_film_visuals import text, box, tag, icon, waveform, font, MINT, WHITE, BG, MUTED, LINE

LIME='#DDF58A'
PAPER='#F1F0E5'
INK='#172622'
CORAL='#FFAA8C'
BLUE='#B3CEE8'


def tag(im,xy,value,color=LIME,size=20):
    d=ImageDraw.Draw(im);width=d.textlength(value,font=font(size,True))+30
    box(im,(xy[0],xy[1],xy[0]+width,xy[1]+size+22),color,10)
    text(im,(xy[0]+15,xy[1]+10),value,size,PAPER if color==INK else INK,True)


def milk(scale=1,mood='hope'):
    im=Image.new('RGBA',(420,540)); d=ImageDraw.Draw(im)
    d.polygon([(65,135),(151,40),(300,40),(365,135),(365,465),(65,465)],fill=PAPER,outline=INK,width=8)
    d.polygon([(65,135),(151,40),(300,40),(232,135)],fill=LIME,outline=INK,width=6)
    d.line([(232,135),(365,135)],fill=INK,width=6)
    d.rectangle((113,282,321,424),fill=BLUE)
    text(im,(217,326),'우유',53,INK,True,center=True)
    # Original cartoon eyes and expressions; no licensed character assets.
    d.ellipse((136,198,156,230),fill=INK);d.ellipse((273,198,293,230),fill=INK)
    if mood=='sad': d.arc((183,245,244,292),180,360,fill=INK,width=6)
    else:d.arc((183,223,244,270),0,180,fill=INK,width=6)
    d.line([(65,340),(18,300)],fill=INK,width=9);d.line([(365,340),(400,293)],fill=INK,width=9)
    d.line([(130,465),(103,515)],fill=INK,width=10);d.line([(302,465),(337,515)],fill=INK,width=10)
    return im.resize((round(420*scale),round(540*scale)),Image.Resampling.LANCZOS)


def quest_card(im,y,kind,title,subtitle,priority,done=False,muted=False):
    fill='#294237' if done else '#24342F'
    box(im,(72,y,1008,y+171),fill,23)
    icon(im,104,y+47,'check' if done else kind,52,LIME if not muted else MUTED)
    text(im,(191,y+28),title,38,PAPER if not muted else MUTED,True)
    text(im,(191,y+99),subtitle,25,'#B8CAC1')
    tag(im,(862,y+41),priority,LIME if priority=='필수' else '#BDCDC3',20)


def render_short(scene,t,duration,cue=None,index=0,count=15):
    kind=scene['kind']; w,h=1080,1920
    light=kind in {'hook','milkplea','milkexit','loop'}
    im=Image.new('RGB',(w,h),LIME if kind=='hook' else (PAPER if light else INK))
    d=ImageDraw.Draw(im); fg=INK if light else PAPER;muted='#506257' if light else '#B5C8BE'
    # Large type and a simple quest-strip establish a casual Shorts voice.
    text(im,(72,132),'퇴근길 생존 퀘스트',25,muted,True)
    if kind!='loop':
        text(im,(72,251),scene['heading'],70,fg,True,width=914,spacing=1.37)
        text(im,(72,365),scene['subheading'],75 if kind in {'hook','milkplea','milkexit'} else 59,
             INK if light else LIME,True,width=920,spacing=1.4)
    if kind=='hook':
        # Staggered cards reveal an immediate relatable problem within one second.
        for i,(k,title,sub,priority) in enumerate([('laundry','세탁물 수령','18:30 마감','필수'),('medicine','약 챙기기','꼭 해야 함','필수'),('milk','우유 구매','되면 해 줘…','선택')]):
            p=min(1,max(0,(t-i*.15)/.35));y=675+i*194+(1-p)**3*150
            quest_card(im,y,k,title,sub,priority)
        tag(im,(72,1350),'퇴근 2회차 입장',INK,25)
        text(im,(w/2,1450),'회사 퇴근 ≠ 오늘 퇴근',31,INK,True,center=True)
    elif kind=='quests':
        quest_card(im,659,'laundry','세탁물 수령','18:30까지 수령 완료','필수')
        quest_card(im,860,'medicine','약 수령','반드시 챙기기','필수')
        quest_card(im,1061,'milk','우유','시간이 되면','선택')
        text(im,(72,1345),'메인 퀘스트 2  +  사이드 퀘스트 1',31,LIME,True)
    elif kind=='deadline':
        text(im,(72,675),'19:00',175,LIME,True)
        text(im,(82,910),'집 도착 마감',39,PAPER,True)
        for i in range(8):box(im,(80+i*116,1060,176+i*116,1120),CORAL if i>=6 else LIME,7)
        text(im,(72,1220),'시간은 유한한데\n할 일은 왜 무한함?',54,PAPER,True,spacing=1.55)
    elif kind=='calculate':
        for i,(value,label) in enumerate([('08','이동'),('02','영업 대기'),('05','수령')]):
            x=72+i*320;box(im,(x,690,x+296,1000),'#2B4136',24)
            text(im,(x+148,735),value,90,LIME,True,center=True)
            text(im,(x+148,885),label+' 분',28,PAPER,True,center=True)
        text(im,(72,1135),'세탁물 18:05 완료 예정',45,PAPER,True)
        tag(im,(72,1250),'마감 18:30 안에',LIME,25)
    elif kind=='change':
        box(im,(72,677,1008,1166),'#2B4136',30)
        text(im,(118,727),'약국 상담',44,PAPER,True)
        text(im,(118,842),'총 30분',116,CORAL,True)
        waveform(im,(160,1278,760,85),t,LIME,True)
        text(im,(72,1450),'계획: 잠깐만요.',36,muted,True)
    elif kind=='conflict':
        text(im,(72,686),'19:05',158,CORAL,True)
        box(im,(72,935,1008,1053),CORAL,22)
        text(im,(540,967),'귀가 마감보다 5분 초과',41,INK,True,center=True)
        quest_card(im,1145,'medicine','필수는 못 빼지','다시 계획해야 하는 순간','필수')
    elif kind in {'milkplea','milkexit','loop'}:
        if kind=='loop':
            text(im,(72,300),'내일은',91,INK,True)
            text(im,(72,448),'나도…?',121,INK,True)
        scale=1.48 if kind=='milkplea' else 1.30
        sprite=milk(scale,'sad' if kind=='milkexit' else 'hope')
        x=(w-sprite.width)//2
        y=690+round(12*math.sin(t*6))
        if kind=='milkexit':x+=round(max(0,(t/duration-.25))*1700)
        im.paste(sprite,(x,y),sprite)
        if kind=='milkplea':tag(im,(72,1390),'선택 용무의 간절함',INK,24)
        elif kind=='milkexit':text(im,(72,1340),'중요한 건\n필수를 지키는 마음.',48,INK,True,spacing=1.55)
        else:
            text(im,(72,1320),'당신의 퇴근길엔\n퀘스트가 몇 개?',46,INK,True,spacing=1.55)
    elif kind in {'revised','approve'}:
        text(im,(72,660),'18:53',135,LIME,True)
        text(im,(81,858),'귀가 예상 · 마감까지 7분',31,muted)
        quest_card(im,960,'laundry','세탁물 수령','필수 유지','필수')
        quest_card(im,1150,'medicine','약국 상담 30분','필수 유지','필수')
        label='우유는 다음 기회에.' if kind=='revised' else ('승인 완료 · 출발' if t>=scene.get('offset',.25)+scene.get('audio_seconds',1.35) else '이 계획으로 출발')
        text(im,(72,1390),label,36,CORAL if kind=='revised' else LIME,True)
    elif kind=='driving':
        icon(im,446,720,'lock',180,LIME)
        text(im,(540,1050),'편집 · 승인',62,PAPER,True,center=True)
        text(im,(540,1160),'정차 후에',79,LIME,True,center=True)
        text(im,(540,1358),'운전에 집중할 시간',31,muted,center=True)
    elif kind=='arrival':
        box(im,(72,696,1008,927),'#2B4136',25)
        text(im,(122,743),'세탁소 도착',57,PAPER,True)
        text(im,(122,839),'아직 수령 전',31,CORAL,True)
        d.line((540,967,540,1050),fill=LIME,width=6)
        box(im,(72,1095,1008,1326),LIME,25)
        text(im,(122,1145),'수령했어요',57,INK,True)
        text(im,(122,1240),'이때 진짜 완료',31,INK,True)
    elif kind=='homeapprove':
        quest_card(im,680,'laundry','세탁물 수령','끝!','필수',True)
        quest_card(im,880,'medicine','약 수령 + 상담','끝!','필수',True)
        icon(im,95,1200,'home',90,LIME)
        text(im,(235,1205),'집으로 출발',63,LIME,True)
        text(im,(235,1320),'마지막 귀가도 내가 승인.',28,muted)
    elif kind=='closing':
        text(im,(72,745),'OnMyWay',112,PAPER,True)
        text(im,(72,920),'AI',155,LIME,True)
        text(im,(72,1193),'오늘의 퇴근길, 클리어.',45,PAPER,True)
        text(im,(72,1350),'퇴근길에 퀘스트 몇 개?',32,muted)
    if cue:
        # Consistent subtitle safe band; key hooks are larger above it.
        y=1590
        from final_film_visuals import wrap
        for j,line in enumerate(wrap(cue,35,870)):
            tw=d.textlength(line,font=font(35))
            box(im,(540-tw/2-18,y+j*53-9,540+tw/2+18,y+j*53+48),INK,9)
            text(im,(540,y+j*53),line,35,PAPER,center=True)
    d.line((72,1812,1008,1812),fill='#7B9384' if light else '#455D50',width=4)
    d.line((72,1812,72+936*(index+t/duration)/count,1812),fill=INK if light else LIME,width=4)
    return im

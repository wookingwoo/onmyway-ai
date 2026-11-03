"""Original Korean product-film artwork. Illustrated cockpit, not captured app UI."""
from functools import lru_cache
import math
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont, ImageFilter

BG = '#0C1117'
SURFACE = '#161E26'
CARD = '#202B35'
WHITE = '#F1F5F2'
MUTED = '#A4B4BD'
MINT = '#AAE8CF'
AMBER = '#F2BC7B'
LINE = '#34434D'
FONT = Path('C:/Windows/Fonts/malgun.ttf')
BOLD = Path('C:/Windows/Fonts/malgunbd.ttf')


@lru_cache(maxsize=100)
def font(size, bold=False):
    return ImageFont.truetype(str(BOLD if bold else FONT), round(size))


def wrap(value, size, width, bold=False):
    d=ImageDraw.Draw(Image.new('RGB',(1,1)))
    lines=[]
    for paragraph in value.split('\n'):
        current=''
        for word in paragraph.split(' '):
            test=(current+' '+word).strip()
            if d.textlength(test,font=font(size,bold)) > width and current:
                lines.append(current); current=word
            else: current=test
        lines.append(current)
    return lines


def text(im, xy, value, size=28, color=WHITE, bold=False, width=None, spacing=1.45, center=False):
    d=ImageDraw.Draw(im)
    lines=wrap(value,size,width,bold) if width else value.split('\n')
    x,y=xy
    for line in lines:
        left=x-d.textlength(line,font=font(size,bold))/2 if center else x
        d.text((round(left),round(y)),line,font=font(size,bold),fill=color,anchor='lt',stroke_width=0)
        y+=size*spacing
    return y


def box(im, bounds, fill=CARD, radius=22, outline=None, line=1):
    ImageDraw.Draw(im).rounded_rectangle(tuple(round(x) for x in bounds),radius,fill=fill,outline=outline,width=line)


def tag(im,xy,value,color=MINT,size=20):
    d=ImageDraw.Draw(im); w=d.textlength(value,font=font(size,True))+30
    box(im,(xy[0],xy[1],xy[0]+w,xy[1]+size+22),fill=color,radius=10)
    text(im,(xy[0]+15,xy[1]+10),value,size,BG,True)
    return w


def icon(im,x,y,kind,size=48,color=MINT):
    d=ImageDraw.Draw(im); w=max(2,round(size/12))
    if kind=='home':
        d.line([(x,y+size*.42),(x+size*.5,y),(x+size,y+size*.42)],fill=color,width=w,joint='curve')
        d.line([(x+size*.14,y+size*.34),(x+size*.14,y+size),(x+size*.86,y+size),(x+size*.86,y+size*.34)],fill=color,width=w)
        d.rectangle((x+size*.4,y+size*.65,x+size*.6,y+size),fill=color)
    elif kind=='check':
        d.line([(x,y+size*.48),(x+size*.37,y+size*.86),(x+size,y+size*.13)],fill=color,width=w,joint='curve')
    elif kind=='lock':
        d.arc((x+size*.2,y,x+size*.8,y+size*.7),180,360,fill=color,width=w)
        d.rounded_rectangle((x,y+size*.4,x+size,y+size),radius=6,outline=color,width=w)
    elif kind=='medicine':
        d.rounded_rectangle((x+size*.35,y,x+size*.65,y+size),radius=3,fill=color)
        d.rounded_rectangle((x,y+size*.35,x+size,y+size*.65),radius=3,fill=color)
    elif kind=='laundry':
        d.line([(x+size*.5,y+size*.25),(x+size*.5,y+size*.13),(x+size*.65,y+size*.05)],fill=color,width=w)
        d.line([(x+size*.5,y+size*.25),(x,y+size*.8),(x+size,y+size*.8),(x+size*.5,y+size*.25)],fill=color,width=w,joint='curve')
    elif kind=='milk':
        d.polygon([(x+size*.2,y+size*.25),(x+size*.35,y),(x+size*.8,y),(x+size*.95,y+size*.25),(x+size*.95,y+size),(x+size*.2,y+size)],outline=color,width=w)
    else:
        d.ellipse((x,y,x+size,y+size),outline=color,width=w)


@lru_cache(maxsize=2)
def background(portrait):
    w,h=(1080,1920) if portrait else (1920,1080)
    im=Image.new('RGB',(w,h),BG)
    # Original road-like light ribbon; quiet depth rather than borrowed photography.
    layer=Image.new('RGB',(w,h),BG); d=ImageDraw.Draw(layer)
    path=[(-200,h*.82),(w*.1,h*.82),(w*.36,h*.61),(w*.60,h*.61),(w*.86,h*.35),(w+200,h*.35)]
    d.line(path,fill='#1F3A39',width=85,joint='curve')
    layer=layer.filter(ImageFilter.GaussianBlur(75))
    im=Image.blend(im,layer,.55)
    return im


def brand(im,portrait):
    x,y=(72,125) if portrait else (80,53)
    d=ImageDraw.Draw(im)
    d.line([(x,y+22),(x+15,y+7),(x+29,y+22),(x+45,y+6)],fill=MINT,width=4,joint='curve')
    text(im,(x+64,y+1),'OnMyWay AI',28,WHITE,True)
    if not portrait: text(im,(1660,57),'가는 길에, 할 일까지.',19,MUTED)


def shell(size,clock='17:50',driving=False):
    w,h=size; im=Image.new('RGB',size,SURFACE); d=ImageDraw.Draw(im)
    text(im,(35,25),'OnMyWay',24,WHITE,True)
    tag(im,(w-177,21),'D · 주행 중' if driving else 'P · 정차 중',AMBER if driving else MINT,17)
    d.line((30,77,w-30,77),fill=LINE,width=1)
    text(im,(35,h-44),clock,18,MUTED)
    text(im,(w-267,h-44),'집 도착 마감 19:00',18,MUTED)
    return im


def waveform(im,rect,t,color=MINT,active=True):
    d=ImageDraw.Draw(im); x,y,w,h=rect
    for i in range(25):
        phase=t*5+i*.73
        v=(.17+.80*abs(math.sin(phase)*math.cos(i*.31))) if active else .1
        bh=h*v; bx=x+i*w/25
        d.rounded_rectangle((bx,y+(h-bh)/2,bx+max(3,w/25-7),y+(h+bh)/2),radius=4,fill=color)


def route_rows(im,revised=False,completed=0,arrival=False,approved=False):
    w,h=im.size; tall=h>750
    y=220 if tall else 190
    rowh=121 if tall else 83
    names=[('laundry','세탁물 수령','18:05 완료 예정','필수'),
           ('medicine','약국 상담' if revised else '약 수령','18:43 완료 예정 · 30분' if revised else '18:18 완료 예정','필수'),
           ('milk','우유 구매','이번에는 제외' if revised else '18:32 완료 예정','선택')]
    for i,(kind,name,detail,priority) in enumerate(names):
        top=y+i*rowh
        fill='#183D35' if i<completed else CARD
        box(im,(32,top,w-32,top+rowh-12),fill=fill,radius=17,outline='#3B6C5A' if i<completed else None)
        icon(im,55,top+(30 if tall else 20),'check' if i<completed else kind,38,color=MUTED if revised and i==2 else MINT)
        text(im,(116,top+18),name,28 if tall else 24,MUTED if revised and i==2 else WHITE,True)
        text(im,(116,top+(61 if tall else 51)), '수령 완료' if i<completed else detail,20 if tall else 17,MUTED)
        if i==2 and revised:
            text(im,(w-145,top+24),'제외',21,MUTED,True)
        elif i<completed:
            text(im,(w-145,top+24),'완료',21,MINT,True)
        else: tag(im,(w-141,top+22),priority,MINT if i<2 else '#CFD9DD',16)
    buttony=h-117
    label='승인한 계획 · 세탁소로' if approved else ('수령을 마쳤어요' if arrival else ('남은 일정 확인' if completed else '이 계획으로 출발'))
    box(im,(32,buttony,w-32,buttony+58),MINT,radius=15)
    text(im,(w/2,buttony+15),label,23,BG,True,center=True)


def schematic(im,progress=0,home=False):
    w,h=im.size; d=ImageDraw.Draw(im)
    x0,y0=40,110; mw,mh=w-80,h-220
    box(im,(x0,y0,x0+mw,y0+mh),'#1C2931',radius=20)
    # Deliberately schematic blocks and unnamed roads: no real-provider map claim.
    for i in range(6):
        x=x0+30+i*mw/5
        d.line([(x,y0+20),(x-45,y0+mh-20)],fill='#33414A',width=12)
    for j in range(5):
        y=y0+30+j*mh/4
        d.line([(x0+15,y),(x0+mw-15,y-55)],fill='#33414A',width=12)
    points=[(x0+mw*.13,y0+mh*.76),(x0+mw*.38,y0+mh*.76),(x0+mw*.38,y0+mh*.39),(x0+mw*.69,y0+mh*.39),(x0+mw*.86,y0+mh*.19)]
    d.line(points,fill='#588274',width=18,joint='curve')
    d.line(points,fill=MINT,width=6,joint='curve')
    end=points[-1]; d.ellipse((end[0]-18,end[1]-18,end[0]+18,end[1]+18),fill=MINT)
    icon(im,end[0]-11,end[1]-11,'home' if home else 'laundry',22,BG)
    # Small forward marker animated over the route; this is cinematic artwork.
    section=min(3,int(progress*4)); frac=progress*4-section
    a,b=points[section],points[section+1]
    x,y=a[0]+(b[0]-a[0])*frac,a[1]+(b[1]-a[1])*frac
    d.ellipse((x-18,y-18,x+18,y+18),fill=WHITE,outline=BG,width=4)
    box(im,(70,h-180,w-70,h-105),SURFACE,17)
    text(im,(93,h-161),'집으로' if home else '다음 할 일 · 세탁물 수령',26,WHITE,True)


def cockpit(scene,size,t,duration):
    kind=scene['kind']; w,h=size; tall=h>750
    clock='18:43' if kind in {'home','homeapprove','medicinecomplete'} else ('18:05' if kind in {'complete','next'} else ('17:58' if kind=='arrival' else '17:50'))
    im=shell(size,clock=clock,driving=kind=='driving')
    if kind in {'request','clarify','answer','change'}:
        label={'request':'오늘 할 일을 말해 주세요','clarify':'한 가지만 확인할게요','answer':'마감 조건을 반영했어요','change':'변경할 내용을 듣고 있어요'}[kind]
        text(im,(35,110),label,30 if tall else 28,WHITE,True,width=w-70)
        quotes={'request':'집엔 7시까지.\n세탁물과 약은 꼭.\n우유는 시간이 되면.',
                'clarify':'18:30까지\n무엇을 마쳐야 하나요?',
                'answer':'세탁물 수령 완료\n18:30까지',
                'change':'약국 상담\n30분 필요해.'}
        y=230 if tall else 204
        box(im,(32,y,w-32,y+(260 if tall else 180)),CARD,radius=23)
        text(im,(64,y+38),quotes[kind],38 if tall else 32,WHITE,True,width=w-125,spacing=1.55)
        if kind=='clarify':
            yy=y+(300 if tall else 210)
            box(im,(32,yy,w/2-10,yy+67),CARD,radius=15)
            box(im,(w/2+10,yy,w-32,yy+67),MINT,radius=15)
            text(im,(w*.25,yy+20),'도착',24,WHITE,True,center=True)
            text(im,(w*.75,yy+20),'수령 완료',24,BG,True,center=True)
        elif kind=='answer':
            tag(im,(36,y+(300 if tall else 220)),'필수 용무 · 완료 마감',MINT,21)
        else:
            waveform(im,(w*.25,h-198,w*.50,72),t,MINT,True)
            text(im,(w/2,h-100),'듣고 있어요' if scene['role']=='driver' else 'OnMyWay',20,MUTED,center=True)
    elif kind=='calculate':
        text(im,(35,113),'빠뜨린 시간까지',32,WHITE,True)
        terms=[('08','이동'),('02','영업 대기'),('05','수령')]
        y=240 if tall else 205
        cw=(w-105)/3
        for i,(n,label) in enumerate(terms):
            xx=32+i*(cw+20); box(im,(xx,y,xx+cw,y+180),CARD,18)
            text(im,(xx+cw/2,y+27),n,58,MINT,True,center=True)
            text(im,(xx+cw/2,y+110),label+' 분',20,MUTED,center=True)
        text(im,(w/2,y+235),'18:05 수령 완료 예정',33,WHITE,True,center=True)
        text(im,(w/2,y+302),'18:30 마감 안에',23,MINT,center=True)
    elif kind in {'plan','revised','approve','next','arrival','complete','medicinecomplete','conflict'}:
        revised=kind not in {'plan'}
        if kind=='conflict':
            text(im,(35,110),'모두 들르면',30,WHITE,True)
            text(im,(35,180),'19:05',91,AMBER,True)
            tag(im,(35,302),'귀가 마감보다 5분 늦어요',AMBER,20)
            box(im,(32,400,w-32,h-94),CARD,20)
            text(im,(61,430),'세탁물  +  약국 상담 30분  +  우유',24,WHITE,True,width=w-120)
            text(im,(61,490),'필수 용무를 지키는 수정안이 필요해요.',21,MUTED,width=w-120)
        else:
            label='도착 · 아직 미완료' if kind=='arrival' else ('필수 용무 완료' if kind=='medicinecomplete' else ('수령 완료' if kind in {'complete','next'} else '귀가 예상'))
            text(im,(35,103),label,22,MUTED)
            if kind=='arrival':
                text(im,(35,143),'세탁물 수령',36,WHITE,True)
            elif kind=='medicinecomplete':
                text(im,(35,143),'두 가지 모두 완료',36,MINT,True)
            elif kind in {'complete','next'}:
                text(im,(35,143),'다음은 약국',36,MINT,True)
            else:
                text(im,(35,138),'18:53' if revised else '18:40',49,MINT,True)
                text(im,(w-270,159),'마감까지 7분' if revised else '마감까지 20분',21,MUTED)
            route_rows(im,revised,completed=2 if kind=='medicinecomplete' else (1 if kind in {'complete','next'} else 0),arrival=kind=='arrival',approved=kind=='approve' and t>=scene.get('audio_seconds',2)+scene.get('offset',.55))
    elif kind=='driving':
        schematic(im,min(.95,.06+t/max(1,duration)*.84))
        icon(im,40,h-91,'lock',27,AMBER)
        text(im,(85,h-87),'편집·승인은 정차 후에',20,AMBER)
    elif kind in {'home','homeapprove'}:
        if kind=='homeapprove':
            schematic(im,min(.95,.1+t/max(1,duration)*.8),True)
        else:
            cx,cy=w/2,h*.39; d=ImageDraw.Draw(im)
            d.ellipse((cx-96,cy-96,cx+96,cy+96),outline='#406C5D',width=2)
            icon(im,cx-42,cy-48,'home',84)
            text(im,(cx,cy+135),'필수 용무 모두 완료',36,WHITE,True,center=True)
            text(im,(cx,cy+205),'집으로 안내할까요?',27,MUTED,center=True)
            box(im,(32,h-146,w-32,h-78),MINT,18)
            text(im,(cx,h-126),'집으로 출발',27,BG,True,center=True)
    return im


def opening(im,scene,t,duration,portrait):
    w,h=im.size
    if portrait:
        text(im,(72,270),scene['heading'],77,WHITE,True,width=920,spacing=1.4)
        text(im,(72,390),scene['subheading'],88,MINT,True,width=920)
        x,y,cw=74,660,932; rh=188
    else:
        text(im,(90,260),scene['heading'],84,WHITE,True,width=1000)
        text(im,(90,385),scene['subheading'],94,MINT,True,width=1100)
        x,y,cw=1160,210,650; rh=166
    for i,(k,name,detail) in enumerate([('laundry','세탁물','18:30까지 수령 완료'),('medicine','약','꼭 챙겨야 해'),('milk','우유','시간이 되면')]):
        delay=i*.16
        p=min(1,max(0,(t-delay)/.55)); shift=(1-p)**3*70
        yy=y+i*(rh+22)+shift
        box(im,(x,yy,x+cw,yy+rh),CARD,23)
        icon(im,x+34,yy+45,k,55,MINT if i<2 else MUTED)
        text(im,(x+120,yy+31),name,36,WHITE,True)
        text(im,(x+120,yy+91),detail,24,MUTED)
        tag(im,(x+cw-115,yy+38),'필수' if i<2 else '선택',MINT if i<2 else '#C6D4D6',18)
    if not portrait:
        text(im,(95,638),'집엔 19:00까지.',36,MUTED)
        text(im,(95,708),'매일의 퇴근길, 풀어야 할 작은 숙제.',24,MUTED)


def closing(im,scene,t,duration,portrait):
    w,h=im.size
    cx=w/2
    y=380 if portrait else 245
    d=ImageDraw.Draw(im)
    pts=[(cx-230,y+95),(cx-130,y-5),(cx+30,y+130),(cx+225,y-65)]
    d.line(pts,fill=MINT,width=10,joint='curve')
    for x,yy in [pts[0],pts[-1]]: d.ellipse((x-12,yy-12,x+12,yy+12),fill=MINT)
    text(im,(cx,y+240),'OnMyWay AI',82 if portrait else 100,WHITE,True,center=True)
    if portrait:
        text(im,(cx,y+470),'가는 길에,',69,MINT,True,center=True)
        text(im,(cx,y+575),'할 일까지.',69,MINT,True,center=True)
        text(im,(cx,y+780),'오늘의 할 일을 끝내는 여정',27,MUTED,center=True)
    else:
        text(im,(cx,y+415),'가는 길에, 할 일까지.',51,MINT,True,center=True)
        text(im,(cx,y+526),'오늘의 할 일을 끝내는 여정',26,MUTED,center=True)


def value(im,portrait):
    if portrait: return
    text(im,(90,220),'매일의 퇴근길에,',69,WHITE,True)
    text(im,(90,320),'다시 쓰는 이유.',69,MINT,True)
    entries=[('laundry','퇴근길 픽업','세탁물 · 약 · 예약 수령'),('milk','생활 장보기','꼭 필요한 것부터'),('home','가족의 생활 용무','마지막 귀가까지 함께')]
    for i,(k,a,b) in enumerate(entries):
        xx=90+i*585
        box(im,(xx,520,xx+550,827),CARD,28)
        icon(im,xx+35,559,k,55)
        text(im,(xx+35,659),a,34,WHITE,True)
        text(im,(xx+35,728),b,23,MUTED)


def render_frame(scene,t,duration,portrait,cue=None,index=0,count=1):
    im=background(portrait).copy(); w,h=im.size; brand(im,portrait)
    kind=scene['kind']
    if kind=='opening': opening(im,scene,t,duration,portrait)
    elif kind=='closing': closing(im,scene,t,duration,portrait)
    elif kind=='value': value(im,portrait)
    else:
        if portrait:
            y=text(im,(72,260),scene['heading'],66,WHITE,True,width=915,spacing=1.43)
            text(im,(72,max(447,y+25)),scene['subheading'],31,MINT,width=900,spacing=1.5)
            ui=cockpit(scene,(936,860),t,duration)
            box(im,(60,613,1020,1497),'#33434B',31)
            im.paste(ui,(72,625))
        else:
            # Deliberate editorial line breaks; wide sidebar preserves reading room.
            text(im,(80,245),scene['heading'],58,WHITE,True,width=570,spacing=1.55)
            text(im,(83,525),scene['subheading'],29,MINT,width=555,spacing=1.65)
            text(im,(84,765),f'{index+1:02} / {count:02}',22,MUTED)
            ui=cockpit(scene,(1100,660),t,duration)
            box(im,(728,179,1852,863),'#33434B',30)
            im.paste(ui,(740,191))
    # Captions occupy their own stable safe band; never cover application actions.
    if cue:
        cy=1572 if portrait else 947
        cw=880 if portrait else 1680
        size=36 if portrait else 35
        lines=wrap(cue,size,cw)
        if len(lines)>2: raise ValueError('Caption exceeds two-line safe area')
        for i,line in enumerate(lines):
            tw=ImageDraw.Draw(im).textlength(line,font=font(size))
            box(im,(w/2-tw/2-20,cy+i*54-8,w/2+tw/2+20,cy+i*54+size+15),BG,10)
            text(im,(w/2,cy+i*54),line,size,WHITE,center=True)
    # Semantic progress, not an always-moving decorative overlay.
    d=ImageDraw.Draw(im)
    yy=1812 if portrait else 1050
    left,right=(72,1008) if portrait else (80,1840)
    d.line((left,yy,right,yy),fill=LINE,width=3)
    d.line((left,yy,left+(right-left)*(index+t/duration)/count,yy),fill=MINT,width=3)
    # Short exposure dissolve at each scene's entrance, with no black-frame holes.
    fade=min(1,.65+t/.35)
    if fade<1: im=Image.blend(background(portrait),im,fade)
    return im

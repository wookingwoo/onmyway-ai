"""Build original local-only product films from timed ElevenLabs speech.

No credentials are read. Large outputs remain under ignored artifacts/final-films.
"""
import argparse
from hashlib import sha256
import json
import math
from pathlib import Path
import subprocess
import sys
import time
import wave

import numpy as np
from PIL import Image, ImageDraw

from final_film import audio_fingerprint, alignment_cues, scene_seconds, validate_story, validate_delivery, srt, parse_loudness
from final_film_visuals import render_frame, waveform, text, box, wrap, font, MINT, MUTED, BG, LINE, schematic
from final_short_visuals import render_short

ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'artifacts/final-films'
FPS=30
RATE=48000


def run(*args):
    return subprocess.run([str(a) for a in args],check=True,capture_output=True)


def probe(path):
    return json.loads(run('ffprobe','-v','error','-show_streams','-show_format','-of','json',path).stdout)


def load_track(story,track,audio_dir=None):
    result=[]; cursor=0; allcues=[]
    for c in story[track]:
        path=(audio_dir if audio_dir is not None else OUT/'audio')/(c['id']+'.mp3')
        record=json.loads(path.with_suffix('.json').read_text(encoding='utf-8'))
        voice=story['voices'][c['role']]
        payload={k:v for k,v in voice.items() if k not in {'name','voice_id'}}
        payload.update(text=c['text'],apply_text_normalization='on',voice_id=voice['voice_id'],output_format='mp3_44100_128')
        if record['fingerprint']!=audio_fingerprint(payload) or record['sha256']!=sha256(path.read_bytes()).hexdigest():
            raise ValueError('Stale audio inputs: '+c['id'])
        seconds=float(probe(path)['format']['duration'])
        cues=alignment_cues(record['alignment'],c['text'],seconds)
        duration=scene_seconds(c['min_seconds'],seconds) if track=='main' else max(c['min_seconds'],math.ceil((seconds+.65)*2)/2)
        offset=.55 if track=='main' else .25
        scene={**c,'audio':path,'audio_seconds':seconds,'seconds':duration,'offset':offset,'cues':cues,'at':cursor}
        if offset+seconds>duration: raise ValueError('Speech crosses cut')
        allcues.extend([[cursor+offset+a,cursor+offset+b,t] for a,b,t in cues])
        result.append(scene); cursor+=duration
    if track=='short' and not 30<=cursor<=60: raise ValueError(f'Short exceeds official duration: {cursor}')
    return result,allcues,cursor


def cue_at(scene,t):
    return next((v for a,b,v in scene['cues'] if a+scene['offset']<=t<b+scene['offset']),None)


def draw_caption(im,cue,portrait):
    if not cue: return
    w,h=im.size; cy=1572 if portrait else 947; size=36 if portrait else 35
    lines=wrap(cue,size,880 if portrait else 1680)
    if len(lines)>2: raise ValueError('Caption safe area overflow')
    for i,line in enumerate(lines):
        tw=ImageDraw.Draw(im).textlength(line,font=font(size))
        box(im,(w/2-tw/2-20,cy+i*54-8,w/2+tw/2+20,cy+i*54+size+15),BG,10)
        text(im,(w/2,cy+i*54),line,size,center=True)


def stills(scenes,track):
    folder=OUT/track; folder.mkdir(parents=True,exist_ok=True)
    portrait=track=='short'; tw,th=(270,480) if portrait else (480,270)
    cols=4 if portrait else 3
    sheet=Image.new('RGB',(cols*tw,math.ceil(len(scenes)/cols)*(th+38)),BG)
    for i,c in enumerate(scenes):
        at=c['offset']+c['audio_seconds']*.45
        frame=render_short(c,at,c['seconds'],cue_at(c,at),i,len(scenes)) if portrait else render_frame(c,at,c['seconds'],False,cue_at(c,at),i,len(scenes))
        frame.save(folder/f"{c['id']}-review.jpg",quality=93)
        thumb=frame.resize((tw,th),Image.Resampling.LANCZOS)
        x=(i%cols)*tw;y=(i//cols)*(th+38)
        sheet.paste(thumb,(x,y));text(sheet,(x+12,y+th+8),f"{c['id']} · {c['at']:.1f}s · {c['seconds']:.1f}s",16,MUTED)
    sheet.save(folder/'contact-sheet.jpg',quality=94)
    cover=scenes[-2] if portrait else scenes[-1]
    poster=render_short(cover,2,cover['seconds'],None,len(scenes)-2,len(scenes)) if portrait else render_frame(cover,2,cover['seconds'],False,None,len(scenes)-1,len(scenes))
    poster.save(folder/'poster.jpg',quality=95)


def mix_audio(scenes,seconds,path,background_music=True,assistant_filter=None):
    n=round(seconds*RATE); t=np.arange(n,dtype=np.float64)/RATE
    music=np.zeros(n,dtype=np.float64)
    # Original restrained pulse/chord composition, not a downloaded backing track.
    chords=[(146.832,220,293.665),(130.813,195.998,261.626),(164.814,220,329.628),(146.832,195.998,293.665)]
    span=8.0
    for k,start in enumerate(np.arange(0,seconds,span) if background_music else ()):
        end=min(seconds,start+span); a,b=round(start*RATE),round(end*RATE); tt=t[a:b]-start
        env=np.minimum(1,tt/1.4)*np.minimum(1,(end-start-tt)/1.6)
        for freq in chords[k%len(chords)]:
            music[a:b]+=.0055*env*(np.sin(2*np.pi*freq*tt)+.22*np.sin(2*np.pi*freq*2*tt))
    beat=60/(114 if any(c['id'].startswith('v') for c in scenes) else 100)
    for at in np.arange(0,seconds,beat) if background_music else ():
        a=round(at*RATE); b=min(n,a+round(.19*RATE)); tt=np.arange(b-a)/RATE
        music[a:b]+=.020*np.sin(2*np.pi*(57*tt+1.4*(1-np.exp(-tt*20))))*np.exp(-tt*22)
    voice=np.zeros(n); duck=np.ones(n)
    for c in scenes:
        is_filtered_assistant = assistant_filter and c['role'] == 'assistant'
        filters = ('-af', assistant_filter) if is_filtered_assistant else ()
        raw=run('ffmpeg','-v','error','-i',c['audio'],*filters,'-f','f32le','-ac','1','-ar',RATE,'-').stdout
        speech=np.frombuffer(raw,dtype='<f4').astype(np.float64)
        rms=float(np.sqrt(np.mean(speech**2)))
        if rms<.002: raise ValueError('Unexpected silent utterance: '+c['id'])
        gain=min(.125/rms,.79/max(np.max(np.abs(speech)),1e-6))
        speech*=gain
        fade=min(len(speech)//4,round(.012*RATE))
        speech[:fade]*=np.linspace(0,1,fade);speech[-fade:]*=np.linspace(1,0,fade)
        a=round((c['at']+c['offset'])*RATE); b=a+len(speech)
        if b>n: raise ValueError('Speech exceeds final mix')
        voice[a:b]+=speech
        da=max(0,a-round(.15*RATE)); db=min(n,b+round(.25*RATE)); duck[da:db]=.34
        if is_filtered_assistant:
            cue = t[:round(.14*RATE)]
            start = a-round(.18*RATE)
            music[start:start+len(cue)] += .028*np.sin(2*np.pi*(840*cue+1550*cue**2))*np.sin(np.pi*cue/.14)**2
        # Soft confirmation tone for state-changing shots only.
        if c['kind'] in {'approve','complete','homeapprove','revised'}:
            aa=round((c['at']+.12)*RATE); bb=min(n,aa+round(.24*RATE)); tt=np.arange(bb-aa)/RATE
            music[aa:bb]+=.022*np.sin(2*np.pi*880*tt)*np.sin(np.pi*np.minimum(tt/.24,1))**2
    fadein=np.minimum(1,t/1.0); fadeout=np.minimum(1,(seconds-t)/1.8)
    bed=music*duck*fadein*fadeout
    stereo=np.column_stack((voice+bed,voice+np.roll(bed,round(.007*RATE))))
    if np.max(np.abs(stereo))>=1: raise ValueError('Mix would clip')
    with wave.open(str(path),'wb') as f:
        f.setnchannels(2);f.setsampwidth(2);f.setframerate(RATE)
        f.writeframes((np.clip(stereo,-1,1)*32767).astype('<i2').tobytes())


def encode(scenes,track,seconds):
    portrait=track=='short';w,h=(1080,1920) if portrait else (1920,1080)
    folder=OUT/track;work=folder/'work';work.mkdir(parents=True,exist_ok=True)
    silent=work/'picture.mp4'; mixed=work/'mix.wav'
    command=['ffmpeg','-y','-v','error','-f','rawvideo','-pix_fmt','rgb24','-s',f'{w}x{h}','-r','30','-i','-',
             '-an','-c:v','libx264','-preset','fast','-crf','18','-pix_fmt','yuv420p','-threads','4',str(silent)]
    started=time.monotonic()
    errfile=(work/'encode.log').open('wb')
    process=subprocess.Popen(command,stdin=subprocess.PIPE,stderr=errfile,creationflags=getattr(subprocess,'CREATE_NO_WINDOW',0))
    previous_frame=None
    try:
        for i,c in enumerate(scenes):
            print(f"Render {track} {i+1}/{len(scenes)} {c['id']}: {c['seconds']:.1f}s",flush=True)
            base=render_short(c,1,c['seconds'],None,i,len(scenes)) if portrait else render_frame(c,1,c['seconds'],False,None,i,len(scenes))
            # Cached type/art is immutable; only meaningful wave/route/progress moves.
            last_cue=object(); captioned=base
            for frame in range(round(c['seconds']*FPS)):
                t=frame/FPS; cue=cue_at(c,t)
                if cue!=last_cue:
                    captioned=base.copy();draw_caption(captioned,cue,portrait);last_cue=cue
                im=captioned.copy()
                if portrait and c['kind'] in {'hook','milkplea','milkexit','loop','change','approve'}:
                    im=render_short(c,t,c['seconds'],cue,i,len(scenes))
                if not portrait and c['kind'] in {'request','change'}:
                    ui_x,ui_y,ui_w,ui_h=(72,625,936,860) if portrait else (740,191,1100,660)
                    rx,ry,rw,rh=ui_x+ui_w*.25,ui_y+ui_h-198,ui_w*.5,72
                    ImageDraw.Draw(im).rectangle((rx-3,ry-2,rx+rw+3,ry+rh+2),fill='#161E26')
                    waveform(im,(rx,ry,rw,rh),t,active=c['offset']<=t<c['offset']+c['audio_seconds'])
                if not portrait and c['kind'] in {'driving','homeapprove'}:
                    size=(936,860) if portrait else (1100,660)
                    tile=Image.new('RGB',size,'#161E26');schematic(tile,min(.95,.06+t/c['seconds']*.84),c['kind']=='homeapprove')
                    x,y=(72,625) if portrait else (740,191)
                    im.paste(tile.crop((0,100,size[0],size[1]-102)),(x,y+100))
                if not portrait and c['kind']=='opening' and t<1:
                    im=render_frame(c,t,c['seconds'],portrait,cue,i,len(scenes))
                if not portrait:
                    d=ImageDraw.Draw(im);yy=1050;left,right=80,1840
                    d.line((left,yy,right,yy),fill=LINE,width=3)
                    d.line((left,yy,left+(right-left)*(i+t/c['seconds'])/len(scenes),yy),fill=MINT,width=3)
                    if c['kind']=='approve' and t>=c['offset']+c['audio_seconds']:
                        box(im,(772,734,1808,792),MINT,15)
                        text(im,(1290,749),'승인한 계획 · 세탁소로',23,BG,True,center=True)
                transition=.13 if portrait else .4
                if previous_frame is not None and t<transition:
                    p=min(1,t/transition);e=1-(1-p)**3
                    if portrait:
                        shift=round(w*(1-e));incoming=im
                        im=previous_frame.copy();im.paste(incoming,(shift,0))
                    else: im=Image.blend(previous_frame,im,e)
                process.stdin.write(im.tobytes())
            previous_frame=im
        process.stdin.close()
        if process.wait()!=0: raise RuntimeError('Picture encode failed; see local encode.log')
    except BaseException:
        process.kill();process.wait();raise
    finally: errfile.close()
    print(f'Picture complete in {time.monotonic()-started:.1f}s; mixing',flush=True)
    mix_audio(scenes,seconds,mixed)
    final=folder/('OnMyWay_AI_Shorts_Final.mp4' if portrait else 'OnMyWay_AI_Product_Film_Final.mp4')
    run('ffmpeg','-y','-v','error','-i',silent,'-i',mixed,'-map','0:v:0','-map','1:a:0','-c:v','copy',
        '-af','loudnorm=I=-16:TP=-1.5:LRA=8','-ar','48000','-c:a','aac','-b:a','192k',
        '-map_metadata','-1','-metadata','title=OnMyWay AI','-movflags','+faststart',final)
    return final


def verify(path,track,seconds):
    p=probe(path); videos=[s for s in p['streams'] if s['codec_type']=='video'];audio=[s for s in p['streams'] if s['codec_type']=='audio']
    if len(videos)!=1:raise ValueError('Expected one video')
    v=videos[0];info={'width':v['width'],'height':v['height'],'fps':30 if v['avg_frame_rate']=='30/1' and v['r_frame_rate']=='30/1' else 0,
         'seconds':float(p['format']['duration']),'audio_streams':len(audio)}
    validate_delivery(info,track=='short')
    if abs(float(v['duration'])-seconds)>.05 or int(v['nb_frames'])!=round(seconds*FPS):raise ValueError('Frame count/duration mismatch')
    run('ffmpeg','-v','error','-xerror','-i',path,'-f','null','-')
    loudness=run('ffmpeg','-hide_banner','-i',path,'-af','loudnorm=I=-16:TP=-1.5:LRA=8:print_format=json','-f','null','-').stderr.decode()
    metrics=parse_loudness(loudness)
    if float(metrics['input_tp'])>-.9 or not -18<=float(metrics['input_i'])<=-14:
        raise ValueError('Mix loudness outside delivery targets')
    return {**info,'file':path.name,'sha256':sha256(path.read_bytes()).hexdigest(),'bytes':path.stat().st_size,
            'video_codec':v['codec_name'],'pixel_format':v['pix_fmt'],'audio_codec':audio[0]['codec_name'],
            'audio_channels':audio[0]['channels'],'frames':int(v['nb_frames']),'full_decode':'passed','loudness':metrics}


def player():
    (OUT/'player.html').write_text('''<!doctype html><html lang="ko"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>OnMyWay AI · 최종 영상 검수</title><style>*{box-sizing:border-box}body{margin:0;background:#0c1117;color:#f1f5f2;font:17px "Malgun Gothic",sans-serif;padding:36px;max-width:1600px;margin:auto}h1{font-size:32px}p{color:#a4b4bd;line-height:1.8}main{display:grid;grid-template-columns:minmax(0,2.5fr) minmax(280px,1fr);gap:32px}video{width:100%;background:#161e26;border-radius:16px}a{color:#aae8cf}.short{max-height:82vh}button{padding:14px;background:#aae8cf;border:0;border-radius:8px;cursor:pointer;margin:12px 0}@media(max-width:900px){main{display:block}body{padding:18px}.short{max-width:420px}}</style>
<h1>OnMyWay AI</h1><p>최종 영상 로컬 검수 · 한국어 3인 음성 · 자막 포함</p><main><section><h2>제품 소개 영상</h2>
<video id="main" controls playsinline preload="metadata" poster="main/poster.jpg" src="main/OnMyWay_AI_Product_Film_Final.mp4"></video>
<p><a href="main/OnMyWay_AI_Product_Film_Final.mp4" download>MP4 저장</a> · <a href="main/OnMyWay_AI_Product_Film_Final.srt">자막</a></p></section><section><h2>세로 쇼츠</h2>
<video id="short" class="short" controls playsinline preload="metadata" poster="short/poster.jpg" src="short/OnMyWay_AI_Shorts_Final.mp4"></video>
<p><a href="short/OnMyWay_AI_Shorts_Final.mp4" download>MP4 저장</a> · <a href="short/OnMyWay_AI_Shorts_Final.srt">자막</a></p></section></main>
<script>document.querySelectorAll('video').forEach(v=>{v.addEventListener('play',()=>document.querySelectorAll('video').forEach(o=>{if(o!==v)o.pause()}))})</script></html>''',encoding='utf-8')


def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--track',choices=['main','short','all'],default='all')
    parser.add_argument('--stills-only',action='store_true')
    args=parser.parse_args()
    story=json.loads((ROOT/'scripts/final_film_story.json').read_text(encoding='utf-8'));validate_story(story)
    for track in (['main','short'] if args.track=='all' else [args.track]):
        scenes,cues,seconds=load_track(story,track)
        print(f'{track}: {seconds}s, {len(scenes)} scenes, {len(cues)} timed cues',flush=True)
        stills(scenes,track)
        serial=[{k:v for k,v in c.items() if k not in {'audio','cues'}} for c in scenes]
        (OUT/track/'timeline.json').write_text(json.dumps(serial,ensure_ascii=False,indent=2),encoding='utf-8')
        if args.stills_only: continue
        final=encode(scenes,track,seconds)
        final.with_suffix('.srt').write_text(srt(cues),encoding='utf-8')
        verification={**verify(final,track,seconds),'scope':story['provenance'],
                      'caption_source':'ElevenLabs character alignment; original speech, no STT claim',
                      'voices':story['voices'],'music':'Original synthesized chords/pulse and UI tones',
                      'source_sha256':{p.name:sha256(p.read_bytes()).hexdigest() for p in [Path(__file__),ROOT/'scripts/final_film.py',ROOT/'scripts/final_film_visuals.py',ROOT/'scripts/final_short_visuals.py',ROOT/'scripts/final_film_story.json']},
                      'visual_review':'pending','playback_review':'pending'}
        (OUT/track/'verification.json').write_text(json.dumps(verification,ensure_ascii=False,indent=2),encoding='utf-8')
        print(json.dumps({k:verification[k] for k in ['file','seconds','bytes','sha256','full_decode']},ensure_ascii=True),flush=True)
    player()


if __name__=='__main__': main()

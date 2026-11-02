"""Edit actual official Pleos recordings; never render a replacement cockpit."""
from hashlib import sha256
import argparse
import importlib.util
import json
import math
from pathlib import Path
import subprocess

from PIL import Image,ImageDraw
from final_film import srt
from final_film_visuals import text,box,wrap,font,BG,MINT,MUTED
from final_short_visuals import render_short,INK,PAPER,LIME
from native_film import source_window,validate_native_capture,restricted_window

ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'artifacts/final-films/pleos'
spec=importlib.util.spec_from_file_location('film_builder',ROOT/'scripts/build-final-films.py')
b=importlib.util.module_from_spec(spec);spec.loader.exec_module(b)

MAIN={
 'm01':('request',0.4),'m02':('request',8),'m03':('clarify',0.2),'m04':('clarify',9),
 'm05':('baseline',0.2),'m06':('baseline',11),'m07':('change',0.2),'m08':('change',8),
 'm09':('revised',0.2),'m10':('guidance-laundry',0.2),'m11':('guidance-laundry','drive'),
 'm12':('arrival-laundry',0.2),'m13':('next-medicine',0.2),'m14':('next-medicine',9),
 'm14b':('home',0.2),'m15':('home',8),'m16':('guidance-home',1.2),
 'm17':('finished',0.2),'m18':('finished',11)}
SHORT={
 'v02':('request',5),'v03':('baseline',7),'v04':('baseline',12),'v05':('change',1.7),
 'v06':('change',8),'v08':('revised',5),'v10':('guidance-laundry',1.8),
 'v11':('guidance-laundry','drive'),'v12':('arrival-laundry',5),
 'v13':('guidance-home',0.2),'v14':('finished',8)}


def source(scene,track,captures):
    key,start=(MAIN if track=='main' else SHORT)[scene['id']]
    c=captures[key];path=OUT/c['file']
    duration=float(b.probe(path)['format']['duration'])
    if start=='drive':
        start,_=restricted_window(c,scene['seconds'])
    a,z=source_window(start,scene['seconds'],duration)
    return path,a,z


def caption(im,cue,portrait):
    if not cue:return
    size=35 if portrait else 32
    lines=wrap(cue,size,870 if portrait else 1800)
    if len(lines)>(2 if portrait else 1):raise ValueError('Native caption safe band overflow')
    y=1590 if portrait else 1022
    for i,line in enumerate(lines):
        width=ImageDraw.Draw(im).textlength(line,font=font(size));cx=im.width/2
        box(im,(cx-width/2-18,y+i*53-7,cx+width/2+18,y+i*53+47),BG,8)
        text(im,(cx,y+i*53),line,size,PAPER,center=True)


def compose(raw,scene,t,track,i,count):
    cue=b.cue_at(scene,t)
    if track=='main':
        im=Image.new('RGB',(1920,1080),BG)
        # Whole native display, including Pleos vehicle panel and system bars.
        im.paste(raw.resize((1778,1000),Image.Resampling.LANCZOS),(71,0))
        caption(im,cue,False)
        return im
    if raw is None:return render_short(scene,t,scene['seconds'],cue,i,count)
    # Editorial headlines/comedy remain outside the actual captured app pixels.
    im=render_short(scene,t,scene['seconds'],None,i,count)
    ImageDraw.Draw(im).rectangle((45,585,1035,1520),fill=INK)
    if scene['kind'] in {'quests','driving','closing'}:
        im.paste(raw.resize((936,526),Image.Resampling.LANCZOS),(72,635))
        if scene['kind']=='driving':
            crop=raw.crop((650,250,1895,700)).resize((936,338),Image.Resampling.LANCZOS)
            im.paste(crop,(72,1190))
        else:
            text(im,(72,1250),'OnMyWay AI',52,PAPER,True)
            text(im,(72,1350),'가는 길에, 할 일까지.',36,LIME,True)
    else:
        app=raw.crop((640,68,1900,975)).resize((936,674),Image.Resampling.LANCZOS)
        im.paste(app,(72,670))
        text(im,(72,1400),'내가 확인하고, 다음 할 일로.',27,MUTED)
    caption(im,cue,True)
    return im


def build(track,story,manifest):
    scenes,cues,seconds=b.load_track(story,track)
    captures={c['id']:c for c in manifest['captures']}
    folder=OUT/track;folder.mkdir(exist_ok=True)
    w,h=(1920,1080) if track=='main' else (1080,1920)
    picture=folder/'native-picture.mp4'
    err=(folder/'encode.log').open('wb')
    enc=subprocess.Popen(['ffmpeg','-y','-v','error','-f','rawvideo','-pix_fmt','rgb24','-s',f'{w}x{h}','-r','30','-i','-',
        '-an','-c:v','libx264','-preset','fast','-crf','18','-pix_fmt','yuv420p','-threads','4',str(picture)],stdin=subprocess.PIPE,stderr=err)
    provenance=[]
    try:
        for i,c in enumerate(scenes):
            dec=None;window=None
            mapping=MAIN if track=='main' else SHORT
            if c['id'] in mapping:
                path,a,z=source(c,track,captures);window=[a,z]
                dec=subprocess.Popen(['ffmpeg','-v','error','-ss',str(a),'-i',str(path),'-t',str(c['seconds']),
                    '-vf','fps=30,scale=1920:1080','-f','rawvideo','-pix_fmt','rgb24','-'],stdout=subprocess.PIPE,stderr=subprocess.PIPE)
                provenance.append({'scene':c['id'],'source':path.name,'window':window,'rate':1,'sha256':sha256(path.read_bytes()).hexdigest()})
            print(f'Native edit {track} {i+1}/{len(scenes)} {c["id"]}',flush=True)
            try:
                for f in range(round(c['seconds']*30)):
                    raw=None
                    if dec:
                        data=dec.stdout.read(1920*1080*3)
                        if len(data)!=1920*1080*3:raise ValueError('Native source exhausted; no invented frame substitution')
                        raw=Image.frombytes('RGB',(1920,1080),data)
                    im=compose(raw,c,f/30,track,i,len(scenes));enc.stdin.write(im.tobytes())
            finally:
                if dec:
                    dec.stdout.close();dec.wait(timeout=20)
        enc.stdin.close()
        if enc.wait()!=0:raise RuntimeError('Native encode failed')
    except BaseException:
        enc.kill();enc.wait();raise
    finally:err.close()
    mix=folder/'mix.wav';b.mix_audio(scenes,seconds,mix)
    final=OUT/('OnMyWay_AI_Pleos_Final.mp4' if track=='main' else 'OnMyWay_AI_Pleos_Shorts_Final.mp4')
    b.run('ffmpeg','-y','-v','error','-i',picture,'-i',mix,'-map','0:v:0','-map','1:a:0','-c:v','copy',
        '-af','loudnorm=I=-16:TP=-1.5:LRA=8','-ar','48000','-c:a','aac','-b:a','192k','-map_metadata','-1','-movflags','+faststart',final)
    final.with_suffix('.srt').write_text(srt(cues),encoding='utf-8')
    verification={**b.verify(final,track,seconds),'scope':manifest['scope'],'native_sources':provenance,
        'voice_scope':'ElevenLabs editorial narration and dialogue, not live STT/native output',
        'illustrated_product_screens':0,'visual_review':'pending','playback_review':'pending'}
    (folder/'verification.json').write_text(json.dumps(verification,ensure_ascii=False,indent=2),encoding='utf-8')
    tw,th=(480,270) if track=='main' else (270,480);cols=3 if track=='main' else 4
    sheet=Image.new('RGB',(cols*tw,math.ceil(len(scenes)/cols)*(th+27)),BG)
    for i,c in enumerate(scenes):
        path=folder/(c['id']+'.jpg');b.run('ffmpeg','-y','-v','error','-ss',c['at']+c['seconds']*.5,'-i',final,'-frames:v','1',path)
        thumb=Image.open(path).resize((tw,th));x=i%cols*tw;y=i//cols*(th+27);sheet.paste(thumb,(x,y));text(sheet,(x+8,y+th+4),c['id'],16,MUTED)
    sheet.save(folder/'native-decoded-contact-sheet.jpg',quality=95)
    print(json.dumps({k:verification[k] for k in ['file','seconds','bytes','sha256','full_decode']},ensure_ascii=True),flush=True)


def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--track',choices=['main','short','all'],default='all')
    args=parser.parse_args()
    story=json.loads((ROOT/'scripts/final_film_story.json').read_text(encoding='utf-8'))
    manifest=json.loads((OUT/'native-capture-manifest.json').read_text(encoding='utf-8'))
    validate_native_capture(manifest,OUT)
    for track in (['main','short'] if args.track=='all' else [args.track]):build(track,story,manifest)
    (OUT/'player.html').write_text('''<!doctype html><html lang="ko"><meta charset="utf-8"><title>OnMyWay AI · Pleos 최종본</title>
<style>body{background:#0f171d;color:#f2f6f3;font:18px sans-serif;margin:32px}main{display:grid;grid-template-columns:2.4fr 1fr;gap:32px}video{width:100%;max-height:80vh}a{color:#a6e7cc}</style>
<h1>OnMyWay AI · Pleos 화면 녹화 최종본</h1><main><section><h2>본편 · 2분13.5초</h2><video id="main" controls src="OnMyWay_AI_Pleos_Final.mp4"></video><p><a href="OnMyWay_AI_Pleos_Final.mp4" download>본편 저장</a></p></section>
<section><h2>쇼츠 · 50초</h2><video id="short" controls src="OnMyWay_AI_Pleos_Shorts_Final.mp4"></video><p><a href="OnMyWay_AI_Pleos_Shorts_Final.mp4" download>쇼츠 저장</a></p></section></main></html>''',encoding='utf-8')


if __name__=='__main__':main()

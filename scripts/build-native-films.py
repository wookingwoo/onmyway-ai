"""Cut existing MainActivity recordings with time-aligned Eleven v4 dialogue."""
import argparse
from hashlib import sha256
import importlib.util
import json
from pathlib import Path
import subprocess

from PIL import Image

from final_film import srt, validate_utterances
from final_film_visuals import text
from native_film import source_window, validate_native_capture

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'artifacts/final-films/mainactivity'
CAPTURE = OUT / 'cockpit-capture'
NAVIGATION = OUT / 'navigation-check'
spec = importlib.util.spec_from_file_location('film_builder', ROOT / 'scripts/build-final-films.py')
builder = importlib.util.module_from_spec(spec)
spec.loader.exec_module(builder)

APPROVALS = {'laundry-approval': '확인하고 출발', 'home-approval': '집으로 출발'}
FULL_SHORT_SHOTS = {'voice-home', 'native-navigation-driving', 'home-arrival'}
ASSISTANT_FILTER = 'highpass=f=210,lowpass=f=6100,equalizer=f=1900:t=q:w=1.2:g=3,tremolo=f=46:d=0.26'


def navigation_example(duration, driving):
    manifest = json.loads((NAVIGATION / ('drive-manifest.json' if driving else 'manifest.json')).read_text(encoding='utf-8'))
    path = NAVIGATION / ('public-drive.mp4' if driving else 'public-route.mp4')
    digest = sha256(path.read_bytes()).hexdigest()
    if digest != manifest['sha256'] or manifest['destination'] != '63빌딩':
        raise ValueError('Verified native Pleos navigation capture required')
    start, limit = manifest['reviewed_window']
    available = float(builder.probe(path)['format']['duration'])
    end = round(start + duration, 6)
    if not 0 <= start < end <= min(limit, available):
        raise ValueError('Navigation scene exceeds the reviewed native route interval')
    return path, start, end, digest


def find_source(scene, manifest):
    name = scene['beat']
    if name in {'native-navigation-example', 'native-navigation-driving'}:
        return navigation_example(scene['seconds'], driving=name == 'native-navigation-driving')
    chapters = manifest['chapters']
    if name == 'parked-overview':
        chapter = next(item for item in chapters if item['id'] == 'driving')
        path = CAPTURE / chapter['raw']
        start, limit = manifest['reviewed_parked_overview']
        end = start + scene['seconds']
        if not 0 <= start < end <= min(limit, float(builder.probe(path)['format']['duration'])):
            raise ValueError('The parked cut exceeds the reviewed visible interval')
        return path, start, end, chapter['sha256']
    matches = [(chapter, beat) for chapter in chapters for beat in chapter['beats'] if beat['name'] == name]
    if len(matches) != 1:
        raise ValueError(f'Expected exactly one verified native shot for {name}')
    chapter, beat = matches[0]
    path = CAPTURE / chapter['raw']
    available = float(builder.probe(path)['format']['duration'])
    duration = scene.get('native_seconds', scene['seconds'])
    start = beat['at'] + .12
    if name in {'voice-request', 'spoken-refinement'}:
        # Show the real dialog opening and the sample words arriving, including the waveform.
        opening = '말로 부탁하기' if name == 'voice-request' else '말로 조건 바꾸기'
        start = max(0, next(item['at'] for item in chapter['beats'] if item['name'] == 'tap'
                            and item.get('label') == opening) - .08)
    if name in {'baseline-plan', 'revised-summary'}:
        submit = '계획 준비하기' if name == 'baseline-plan' else '말 전달하기'
        tap = next(item['at'] for item in chapter['beats'] if item['name'] == 'tap' and item.get('label') == submit)
        start = min(beat['at'], tap + .15)
    if name in APPROVALS:
        tap = next(item['at'] for item in chapter['beats'] if item['name'] == 'tap' and
                   item['at'] > beat['at'] and item.get('label') == APPROVALS[name])
        # End the line on the actual tap. Leave time to see the accepted state.
        start = tap - min(duration - 1.05, scene['offset'] + scene['audio_seconds'])
    elif name == 'voice-home':
        tap = next(item['at'] for item in chapter['beats'] if item['name'] == 'tap' and
                   item['at'] > beat['at'] and item.get('label') == '말로 부탁하기')
        start = tap - duration - .08
    elif name == 'automatic-next':
        tap = max(item['at'] for item in chapter['beats'] if item['name'] == 'tap' and
                  item['at'] < beat['at'] and item.get('label') == '완료했어요')
        start = max(0, tap - .7)
    elif name in {'laundry-arrival', 'medicine-arrival', 'home-arrival'}:
        tap = max(item['at'] for item in chapter['beats'] if item['name'] == 'tap' and
                  item['at'] < beat['at'] and item.get('label') == '샘플 안내 · 도착 신호')
        start = min(beat['at'], tap + .16)
    start, end = source_window(start, duration, available - .12)
    return path, start, end, chapter['sha256']


def captions(path, scene, short):
    def ass_time(seconds):
        ticks = round(seconds * 100)
        return f'{ticks//360000}:{ticks//6000%60:02}:{ticks//100%60:02}.{ticks%100:02}'

    w, h, size, margin = (1080, 1920, 42, 335) if short else (1920, 1080, 32, 17)
    header = f'''[Script Info]
ScriptType: v4.00+
PlayResX: {w}
PlayResY: {h}
WrapStyle: 0
[V4+ Styles]
Format: Name, Fontname, Fontsize, PrimaryColour, SecondaryColour, OutlineColour, BackColour, Bold, Italic, Underline, StrikeOut, ScaleX, ScaleY, Spacing, Angle, BorderStyle, Outline, Shadow, Alignment, MarginL, MarginR, MarginV, Encoding
Style: Default,Malgun Gothic,{size},&H00FFFFFF,&H00FFFFFF,&H00171310,&H00171310,0,0,0,0,100,100,0,0,3,10,0,2,70,70,{margin},1
[Events]
Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text
'''
    events = []
    for i, (start, end, cue) in enumerate(scene['cues']):
        if any(char in cue for char in '{}\\\r\n'):
            raise ValueError('Caption contains ASS control characters')
        if i == 0 and scene['role'] == 'assistant':
            cue = '차량 AI  ·  ' + cue
        events.append(f"Dialogue: 0,{ass_time(start+scene['offset'])},{ass_time(end+scene['offset'])},Default,,0,0,0,,{cue}")
    if scene['kind'] in {'navigation', 'driving'} and not short:
        events.append(f"Dialogue: 0,0:00:00.00,{ass_time(scene['seconds'])},Default,,0,0,0,,"
                      "{\\an8\\pos(1450,84)}" + scene['heading'])
    path.write_text(header + '\n'.join(events) + '\n', encoding='utf-8-sig')


def encode_shot(folder, scene, source, start, end, short, stem):
    captions(folder / f'{stem}.ass', scene, short)
    cmd = ['ffmpeg', '-y', '-v', 'error', '-ss', str(start), '-i', str(source)]
    speed = (end - start) / scene['seconds']
    timing = f'setpts=(PTS-STARTPTS)/{speed:.9f},' if abs(speed - 1) > .001 else ''
    if short:
        # Neutral editorial margins follow the existing app's daytime palette.
        card = Image.new('RGB', (1080, 1920), '#f5f6f7')
        text(card, (64, 174), 'OnMyWay AI', 35, '#62676c', True)
        text(card, (64, 268), scene['heading'], 74, '#202124', True, width=952)
        card.save(folder / f'{stem}.png')
        if scene['beat'] in FULL_SHORT_SHOTS:
            crop, scale, y = '', '960:540', 640
        else:
            crop, scale, y = 'crop=1256:906:640:68,', '960:692', 570
        cmd += ['-loop', '1', '-framerate', '30', '-i', f'{stem}.png', '-filter_complex_threads', '2',
                '-filter_complex', f'[0:v]{timing}fps=30,{crop}scale={scale}:flags=lanczos[screen];'
                f'[1:v][screen]overlay=60:{y}:shortest=1,ass={stem}.ass']
    else:
        cmd += ['-vf', f'{timing}fps=30,scale=1778:1000:flags=lanczos,pad=1920:1080:71:0:color=0x11161a,ass={stem}.ass']
    cmd += ['-frames:v', str(round(scene['seconds'] * 30)), '-an', '-c:v', 'libx264',
            '-preset', 'fast', '-crf', '18', '-pix_fmt', 'yuv420p', '-threads', '4', f'{stem}.mp4']
    result = subprocess.run(cmd, cwd=folder, capture_output=True)
    if result.returncode:
        raise RuntimeError(result.stderr.decode('utf-8', errors='replace'))
    encoded = builder.probe(folder / f'{stem}.mp4')['streams'][0]
    if int(encoded['nb_frames']) != round(scene['seconds'] * 30):
        raise ValueError(f'Insufficient source frames for {scene["id"]}')


def build(track, story, manifest):
    scenes, cues, seconds = builder.load_track(story, track, OUT / 'audio')
    folder = OUT / track
    folder.mkdir(exist_ok=True)
    cuts = []
    for index, scene in enumerate(scenes):
        source, start, end, digest = find_source(scene, manifest)
        stem = f'{index:02d}'
        print(f'{track} {scene["id"]} {scene["beat"]}: {start:.2f}–{end:.2f}', flush=True)
        encode_shot(folder, scene, source, start, end, track == 'short', stem)
        cuts.append({'scene': scene['id'], 'beat': scene['beat'], 'source': source.name,
                     'sha256': digest, 'source_window': [start, end], 'at': scene['at'],
                     'duration': scene['seconds'], 'native_speed': round((end-start)/scene['seconds'], 4)})
    listing = folder / 'cuts.txt'
    listing.write_text(''.join(f"file '{i:02d}.mp4'\n" for i in range(len(scenes))), encoding='ascii')
    mix = folder / 'mix.wav'
    builder.mix_audio(scenes, seconds, mix, background_music=False, assistant_filter=ASSISTANT_FILTER, interaction_cues=True)
    final = OUT / ('OnMyWay_AI_Pleos_Product.mp4' if track == 'main' else 'OnMyWay_AI_Pleos_Product_Shorts.mp4')
    rendering = final.with_stem(final.stem + '.rendering')
    builder.run('ffmpeg', '-y', '-v', 'error', '-f', 'concat', '-safe', '0', '-i', listing,
                '-i', mix, '-map', '0:v:0', '-map', '1:a:0', '-c:v', 'copy',
                '-af', 'loudnorm=I=-16:TP=-1.5:LRA=8', '-ar', '48000', '-c:a', 'aac', '-b:a', '192k',
                '-map_metadata', '-1', '-movflags', '+faststart', rendering)
    rendering.replace(final)
    final.with_suffix('.srt').write_text(srt(cues), encoding='utf-8')
    verification = {**builder.verify(final, track, seconds), 'source': 'Native OnMyWay and Navigation on official Pleos AVD',
                    'model': 'eleven_v4', 'voices': story['voices'], 'background_music': False,
                    'assistant_filter': ASSISTANT_FILTER, 'interaction_cues': [
                        {'scene': scene['id'], 'event': scene['cue'],
                         **({'offset': scene['cue_offset']} if 'cue_offset' in scene else {})}
                        for scene in scenes if scene.get('cue')
                    ], 'caption_count': len(cues), 'cuts': cuts}
    tw, th, cols = (480, 270, 3) if track == 'main' else (270, 480, 3)
    sheet = Image.new('RGB', (cols*tw, ((len(cuts)+cols-1)//cols)*(th+25)), '#11161a')
    for i, cut in enumerate(cuts):
        frame = folder / (cut['scene'] + '.jpg')
        builder.run('ffmpeg', '-y', '-v', 'error', '-ss', cut['at']+cut['duration']*.6,
                    '-i', final, '-frames:v', '1', frame)
        x, y = (i%cols)*tw, (i//cols)*(th+25)
        with Image.open(frame) as im:
            sheet.paste(im.resize((tw, th)), (x, y))
        text(sheet, (x+6, y+th+2), cut['beat'], 15, '#f5f6f7')
    sheet.save(folder / 'contact.jpg', quality=94)
    (folder/'verification.json').write_text(json.dumps(verification, ensure_ascii=False, indent=2), encoding='utf-8')
    print(f'{final.name} {seconds}s {verification["sha256"]}', flush=True)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--track', choices=['main', 'short', 'all'], default='all')
    args = parser.parse_args()
    story = json.loads((ROOT/'scripts/mainactivity_film_story.json').read_text(encoding='utf-8'))
    manifest = json.loads((CAPTURE/'capture-manifest.json').read_text(encoding='utf-8'))
    validate_utterances(story)
    validate_native_capture(manifest, CAPTURE)
    if not manifest.get('voice_motion'):
        raise ValueError('Native voice-motion capture is required')
    if not manifest.get('moving_overview'):
        raise ValueError('Native moving overview capture is required')
    for track in ['main', 'short'] if args.track == 'all' else [args.track]:
        build(track, story, manifest)
    (OUT/'player.html').write_text('''<!doctype html><meta charset="utf-8"><title>OnMyWay AI · Pleos 녹화본</title>
<style>body{background:#f5f6f7;color:#202124;font:18px sans-serif;margin:32px}main{display:grid;grid-template-columns:2.4fr 1fr;gap:32px}video{width:100%;max-height:80vh;background:#11161a}</style>
<h1>OnMyWay AI · 기존 앱 Pleos 녹화본</h1><main>
<section><h2>본편</h2><video id="main" controls preload="metadata" src="OnMyWay_AI_Pleos_Product.mp4"></video><p><a href="OnMyWay_AI_Pleos_Product.mp4" download>본편 저장</a></p></section>
<section><h2>쇼츠</h2><video id="short" controls preload="metadata" src="OnMyWay_AI_Pleos_Product_Shorts.mp4"></video><p><a href="OnMyWay_AI_Pleos_Product_Shorts.mp4" download>쇼츠 저장</a></p></section>
</main>''', encoding='utf-8')


if __name__ == '__main__':
    main()

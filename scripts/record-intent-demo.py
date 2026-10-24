"""Record/assemble honest, local-only footage from the installed native demo.

Requires the official isolated AVD in Park, local API/APK, adb, ffmpeg and ffprobe.
Raw footage is never synthesized. The final video is explicitly labelled 2x.
"""
import argparse
from hashlib import sha256
import json
from pathlib import Path
import subprocess
import sys
import time

from demo_recording import DemoRecording

ROOT = Path(__file__).resolve().parents[1]


def run(*args):
    return subprocess.run(list(map(str, args)), cwd=ROOT, check=True, capture_output=True,
                          text=True, encoding='utf-8').stdout


def probe(path):
    return json.loads(run('ffprobe', '-v', 'error', '-show_format', '-show_streams', '-of', 'json', path))


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--adb', required=True)
    parser.add_argument('--serial', required=True)
    parser.add_argument('--reuse-journeys', action='store_true', help='Keep existing normal/tight captures; freshly record driving')
    parser.add_argument('--assemble-only', action='store_true', help='Re-encode existing three raw segments without operating the AVD')
    args = parser.parse_args()
    out = ROOT / 'artifacts/evidence/intent-demo'
    out.mkdir(parents=True, exist_ok=True)
    common = ['--adb', args.adb, '--serial', args.serial, '--output', str(out)]
    if not args.assemble_only:
        if not args.reuse_journeys:
            print(run(sys.executable, 'scripts/smoke-intent.py', *common, '--record'), flush=True)
        print(run(sys.executable, 'scripts/smoke-intent.py', *common, '--mode', 'lock-ready'), flush=True)
        recorder = DemoRecording(args.adb, args.serial, out)
        recorder.start('driving')
        try:
            time.sleep(2)
            print(run(sys.executable, 'scripts/smoke-pleos-lock.py', *common), flush=True)
            time.sleep(3)
        finally:
            recorder.stop()
    names = ['normal', 'tight', 'driving']
    titles = ['01  요청 확인부터 세 용무 완료·귀가까지', '02  필수 유지 · 마감 변경 승인 · 선택 제외', '03  실제 차량 상태 Drive 30 km/h · 조작 잠금']
    (out / 'disclosure.txt').write_text('준비된 예시 해석 · 가상 장소·이동시간 · 모의 안내·도착', encoding='utf-8')
    (out / 'disclosure-scope.txt').write_text('실제 STT·LLM·실경로 결과 아님 / 2배속 / 실제 Pleos 앱 화면', encoding='utf-8')
    records = []
    for name, title in zip(names, titles):
        raw = out / (name + '-raw.mp4')
        info = probe(raw)
        duration = float(info['format']['duration'])
        assert 3 < duration < 180, 'Unexpected capture duration'
        (out / 'stage.txt').write_text(title, encoding='utf-8')
        # Use a fixed local font; no external fonts, narration, or identity metadata.
        vf = "setpts=PTS/2,fps=30,drawbox=x=0:y=966:w=iw:h=114:color=black@0.92:t=fill,drawtext=fontfile='C\\:/Windows/Fonts/malgun.ttf':textfile=disclosure.txt:fontcolor=white:fontsize=26:x=30:y=980,drawtext=fontfile='C\\:/Windows/Fonts/malgun.ttf':textfile=disclosure-scope.txt:fontcolor=white:fontsize=26:x=30:y=1025,drawtext=fontfile='C\\:/Windows/Fonts/malgun.ttf':textfile=stage.txt:fontcolor=white:fontsize=32:x=30:y=16:box=1:boxcolor=black@0.85:boxborderw=10"
        subprocess.run(['ffmpeg', '-y', '-v', 'error', '-i', str(raw), '-vf', vf,
            '-an', '-map_metadata', '-1', '-c:v', 'libx264', '-preset', 'fast', '-crf', '21',
            '-pix_fmt', 'yuv420p', '-movflags', '+faststart', name + '-captioned.mp4'],
            cwd=out, check=True)
        records.append({'name': name, 'raw_seconds': duration, 'raw_sha256': sha256(raw.read_bytes()).hexdigest()})
    (out / 'concat.txt').write_text(''.join("file '" + name + "-captioned.mp4'\n" for name in names), encoding='utf-8')
    final = out / 'OnMyWay_AI_intent_demo.mp4'
    subprocess.run(['ffmpeg', '-y', '-v', 'error', '-f', 'concat', '-safe', '0', '-i', 'concat.txt',
                    '-c', 'copy', '-map_metadata', '-1', '-movflags', '+faststart', final.name], cwd=out, check=True)
    info = probe(final)
    video = next(s for s in info['streams'] if s['codec_type'] == 'video')
    assert (video['width'], video['height']) == (1920, 1080)
    assert not any(s['codec_type'] == 'audio' for s in info['streams'])
    run('ffmpeg', '-v', 'error', '-i', final, '-f', 'null', '-')
    cursor = 0
    for segment in records:
        length = float(probe(out / (segment['name'] + '-captioned.mp4'))['format']['duration'])
        segment['start_seconds'] = round(cursor, 3)
        segment['captioned_seconds'] = length
        # Inspect these local frames after assembly; a full decode alone is not visual QA.
        for label, point in [('middle', cursor + length / 2), ('end', cursor + max(0, length - 2))]:
            run('ffmpeg', '-y', '-v', 'error', '-ss', point, '-i', final, '-frames:v', '1', out / (segment['name'] + '-' + label + '.png'))
        cursor += length
    evidence = {'file': final.name, 'width': video['width'], 'height': video['height'],
        'seconds': float(info['format']['duration']), 'sha256': sha256(final.read_bytes()).hexdigest(),
        'full_decode': 'passed', 'audio': 'none', 'speed': '2x, explicitly labelled',
        'scope': 'actual native app, prepared interpretation and synthetic routes, simulated navigation/arrival; actual driving restriction',
        'segments': records, 'visual_review': 'pending: inspect extracted scene frames and actual playback'}
    (out / 'media-verification.json').write_text(json.dumps(evidence, ensure_ascii=False, indent=2), encoding='utf-8')
    print(json.dumps(evidence, ensure_ascii=True, indent=2), flush=True)


if __name__ == '__main__':
    main()

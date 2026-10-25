"""Full-Pleos film, actual device speech plus disclosed offline sample dialogue.

Source footage and output audio share one timestamp timeline at 1x. Captions are
authored scene summaries, not claimed word-level ASR or TTS alignment.
"""
import argparse
from hashlib import sha256
import json
from pathlib import Path
import subprocess

from PIL import Image, ImageDraw, ImageFont
from story_demo import inside, native_presentation, probe, stamp, validate_final_video, wrapped
from spoken_demo import remap_interval, story_windows


def run(*args):
    subprocess.run(list(map(str, args)), check=True)


def build(root):
    manifest = json.loads((root / "capture-manifest.json").read_text(encoding="utf-8"))
    assert manifest["result"] == "pass"
    assert [c["id"] for c in manifest["chapters"]] == ["request", "refinement", "departure", "remaining", "home"]
    dialogue = {c["id"]: c["narration"] for c in json.loads(Path("scripts/spoken-demo-dialogue.json").read_text(encoding="utf-8"))["chapters"]}
    provenance = json.loads((root / "audio/prepared-provenance.json").read_text(encoding="utf-8-sig"))
    prepared = {p["id"]: p for p in provenance["chapters"]}
    edit = root / "film"; edit.mkdir(exist_ok=True)
    fonts = Path("C:/Windows/Fonts")
    font = ImageFont.truetype(str(fonts / "malgun.ttf"), 29)
    bold = ImageFont.truetype(str(fonts / "malgunbd.ttf"), 31)
    small = ImageFont.truetype(str(fonts / "malgun.ttf"), 19)
    titles = {
        "request": "19시 저녁 약속. 약도, 세탁물도 챙겨야 하는 퇴근길.",
        "refinement": "상담이 길어지겠네. 약국은 30분으로 바꿔줘.",
        "departure": "새 계획을 들었으니, 말로 출발을 승인해요.",
        "remaining": "세탁물 수령 완료. 끝낸 일은 빼고 다음 계획을 들어요.",
        "home": "약까지 챙겼어요. 마지막 길도, 집까지 이어지도록.",
    }
    summaries = {
        "request": "앱의 실제 음성 안내 · 첫 계획은 18:40 귀가 예상 · 출발 승인은 아직",
        "refinement": "앱의 실제 음성 안내 · 세탁소 먼저 → 약국 · 우유는 다음에 · 18:53 귀가 예상",
        "departure": "명시한 현재 계획만 승인 · 실제 AVD Drive 상태에서는 음성 입력과 화면 조작 제한",
        "remaining": "도착과 완료는 별개 · 완료 확인 뒤 남은 약국 계획을 자동 계산하고 음성으로 안내",
        "home": "귀가 안내도 별도로 승인 · 샘플 일정 18:53 집 도착 · 귀가 마감 19:00",
    }
    cues, chapters, cursor = [], [], 0.0
    for number, chapter in enumerate(manifest["chapters"], 1):
        name = chapter["id"]
        raw = inside(root, chapter["raw"])
        assert sha256(raw.read_bytes()).hexdigest() == chapter["sha256"]
        native = inside(root, chapter["native_audio"])
        assert native.is_file()
        raw_seconds = float(probe(raw)["format"]["duration"])
        assert abs(raw_seconds - chapter["capture_seconds"]) < 1.5
        windows = story_windows(chapter, raw_seconds)
        duration = sum(b - a for a, b in windows)
        if name != "departure":
            assert chapter["speaker_capture"]["peak"] > 100
        image = Image.new("RGB", (1920, 1080), "#101C29"); draw = ImageDraw.Draw(image)
        draw.text((32, 8), f"OnMyWay AI · {number:02d}/05 · {titles[name]}", font=bold, fill="#F4F2EA")
        draw.text((32, 51), "앱 안내: 실제 Pleos 기기 음성  |  사용자: 준비된 샘플 대사 · 오프라인 음성", font=small, fill="#8BE0CB")
        draw.text((1300, 52), "샘플·모의 안내/도착 · 1× · 과정 축약", font=small, fill="#E8C88C")
        draw.rectangle((0, 970, 1920, 1080), fill="#0B1420")
        image.save(edit / (name + ".png"))
        command = ["ffmpeg", "-y", "-v", "error", "-i", str(raw), "-loop", "1", "-i", str(edit / (name + ".png")), "-i", str(native)]
        x, y, width, height = native_presentation()
        graph = []
        for j, (a, b) in enumerate(windows):
            graph += [f"[0:v]trim=start={a}:end={b},setpts=PTS-STARTPTS[v{j}]",
                      f"[2:a]atrim=start={a}:end={b},asetpts=PTS-STARTPTS[a{j}]"]
        graph += ["".join(f"[v{j}]" for j in range(len(windows))) + f"concat=n={len(windows)}:v=1:a=0,scale={width}:{height},setsar=1,fps=30[native]",
                 f"[1:v][native]overlay={x}:{y}:shortest=1,format=yuv420p[v]",
                 "".join(f"[a{j}]" for j in range(len(windows))) + f"concat=n={len(windows)}:v=0:a=1,aresample=48000,apad,atrim=duration={duration}[device]"]
        audio = ["[device]"]
        prepared_cues = []
        for index, line in enumerate(chapter.get("prepared", [])):
            path = inside(root, "audio/" + line["id"] + ".mp3")
            assert sha256(path.read_bytes()).hexdigest() == line["sha256"] == prepared[line["id"]]["sha256"]
            assert dialogue[line["id"]] == prepared[line["id"]]["text"]
            at, end = remap_interval(line["at"], line["at"] + line["seconds"], windows)
            command += ["-i", str(path)]
            graph.append(f"[{3+index}:a]aresample=48000,adelay={round(at*1000)}:all=1,apad,atrim=duration={duration}[user{index}]")
            audio.append(f"[user{index}]")
            # Sentence timing is authored proportionally over the known clip,
            # explicitly not a recognition or provider word-boundary claim.
            sentences = [s.strip() + "." for s in dialogue[line["id"]].split(".") if s.strip()]
            total = sum(map(len, sentences)); offset = at
            for sentence in sentences:
                end = offset + line["seconds"] * len(sentence) / total
                prepared_cues.append([offset, end, "사용자 · " + sentence]); offset = end
        graph.append("".join(audio) + f"amix=inputs={len(audio)}:duration=longest:normalize=0,loudnorm=I=-18:TP=-2:LRA=8,alimiter=limit=0.92[a]")
        command += ["-filter_complex", ";".join(graph), "-map", "[v]", "-map", "[a]", "-t", str(duration), "-c:v", "libx264", "-preset", "fast", "-crf", "20", "-r", "30", "-video_track_timescale", "90000", "-c:a", "aac", "-ar", "48000", "-ac", "2", "-b:a", "160k", "-movflags", "+faststart", str(edit / (name + ".mp4"))]
        run(*command)
        offset = 0
        for a, b, text in sorted(prepared_cues):
            if a - offset > .2: cues.append([cursor + offset, cursor + a, summaries[name]])
            cues.append([cursor + a, cursor + b, text]); offset = b
        if duration - offset > .2: cues.append([cursor + offset, cursor + duration, summaries[name]])
        chapters.append({"id": name, "start": cursor, "seconds": duration, "source_windows": windows, "raw_sha256": chapter["sha256"], "native_audio_sha256": sha256(native.read_bytes()).hexdigest()})
        cursor += duration
        print("RENDERED: " + name, flush=True)
    ass = ["[Script Info]", "ScriptType: v4.00+", "PlayResX: 1920", "PlayResY: 1080", "WrapStyle: 2", "", "[V4+ Styles]",
           "Format: Name,Fontname,Fontsize,PrimaryColour,SecondaryColour,OutlineColour,BackColour,Bold,Italic,Underline,StrikeOut,ScaleX,ScaleY,Spacing,Angle,BorderStyle,Outline,Shadow,Alignment,MarginL,MarginR,MarginV,Encoding",
           "Style: Default,Malgun Gothic,29,&H00FFFFFF,&H00FFFFFF,&H0020140B,&H0020140B,0,0,0,0,100,100,0,0,1,1,0,2,60,60,28,1", "", "[Events]",
           "Format: Layer,Start,End,Style,Name,MarginL,MarginR,MarginV,Effect,Text"]
    srt = []
    for index, (a, b, text) in enumerate(cues, 1):
        lines = wrapped(text, font, 1740); assert len(lines) <= 2
        srt.append(f"{index}\n{stamp(a)} --> {stamp(b)}\n" + "\n".join(lines) + "\n")
        ass.append(f"Dialogue: 0,{stamp(a, True)},{stamp(b, True)},Default,,0,0,0,," + "\\N".join(lines))
    (root / "OnMyWay_AI_spoken_demo.srt").write_text("\n".join(srt), encoding="utf-8")
    (edit / "captions.ass").write_text("\n".join(ass), encoding="utf-8-sig")
    (edit / "concat.txt").write_text("".join("file '" + c["id"] + ".mp4'\n" for c in chapters), encoding="utf-8")
    subprocess.run(["ffmpeg", "-y", "-v", "error", "-f", "concat", "-safe", "0", "-i", "concat.txt", "-c", "copy", "assembled.mp4"], cwd=edit, check=True)
    final = root / "OnMyWay_AI_spoken_demo.mp4"
    subprocess.run(["ffmpeg", "-y", "-v", "error", "-i", "assembled.mp4", "-vf", "fps=30:start_time=0,ass=captions.ass", "-r", "30", "-fps_mode", "cfr", "-t", str(cursor), "-c:v", "libx264", "-preset", "fast", "-crf", "20", "-c:a", "copy", "-map_metadata", "-1", "-movflags", "+faststart", str(final)], cwd=edit, check=True)
    data = probe(final); video = next(s for s in data["streams"] if s["codec_type"] == "video")
    validate_final_video(video); assert (video["width"], video["height"]) == (1920, 1080)
    assert abs(float(data["format"]["duration"]) - cursor) < .15
    run("ffmpeg", "-v", "error", "-i", final, "-f", "null", "-")
    frames = root / "frames"; frames.mkdir(exist_ok=True)
    for chapter in chapters:
        run("ffmpeg", "-y", "-v", "error", "-ss", chapter["start"] + chapter["seconds"] * .65, "-i", final, "-frames:v", "1", frames / (chapter["id"] + ".png"))
    evidence = {"file": final.name, "sha256": sha256(final.read_bytes()).hexdigest(), "bytes": final.stat().st_size,
                "seconds": float(data["format"]["duration"]), "fps": "30/1", "full_decode": "pass", "speed": "1x, matching audio/video source windows; setup/operation waits cut",
                "audio": "actual Pleos ON_DEVICE plan speech + offline Windows Heami prepared user dialogue", "captions": "authored scene summaries and proportional prepared-dialogue sentence cues; not word-level ASR alignment",
                "scope": manifest["scope"], "chapters": chapters, "visual_review": "pending", "playback_review": "pending"}
    (root / "media-verification.json").write_text(json.dumps(evidence, ensure_ascii=False, indent=2), encoding="utf-8")
    (root / "index.html").write_text('<!doctype html><html lang="ko"><meta charset="utf-8"><title>OnMyWay AI — 말로 이어지는 퇴근길</title><style>body{margin:0;background:#101c29;color:#f4f2ea;font-family:Malgun Gothic,sans-serif}main{max-width:1440px;margin:24px auto}video{width:100%;max-height:calc(100vh - 180px)}p{color:#a8b9c7;line-height:1.6}</style><main><h1>말로 부탁하고, 듣고, 출발하세요.</h1><video controls preload="metadata" src="OnMyWay_AI_spoken_demo.mp4"></video><p>실제 Pleos 전체 화면·앱 음성 안내 / 준비된 샘플 사용자 대사·오프라인 음성 / 샘플 데이터·모의 안내·도착<br>실제 ASR·LLM·지도·차량 내비 수락/도착 및 물리 차량 검증은 별도입니다. 자막은 장면 요약입니다.</p></main></html>', encoding="utf-8")
    print(json.dumps({k: v for k, v in evidence.items() if k != "chapters"}, ensure_ascii=True, indent=2))


if __name__ == "__main__":
    parser = argparse.ArgumentParser(); parser.add_argument("--output", default="artifacts/evidence/spoken-plan")
    build(Path(parser.parse_args().output).resolve())

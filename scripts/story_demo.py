"""Local story-film assembly with actual-footage and spoken-timing boundaries."""
from array import array
from hashlib import sha256
import html
import json
import math
from pathlib import Path
import re
import subprocess
import wave


def inside(root, relative):
    root = root.resolve()
    result = (root / relative).resolve()
    if not result.is_relative_to(root):
        raise ValueError("Media path is outside the local capture directory")
    return result


def validate_inputs(story, capture, root):
    if capture.get("result") != "pass":
        raise ValueError("A successful actual capture is required")
    ids = [c["id"] for c in story["chapters"]]
    if len(ids) != len(set(ids)) or not ids:
        raise ValueError("Unique nonempty story chapters are required")
    for chapter in story["chapters"]:
        name = chapter["id"]
        if not re.fullmatch(r"[a-z-]+", name):
            raise ValueError("Unsafe chapter id")
        for extension in [".mp3", ".json"]:
            path = inside(root, "audio/" + name + extension)
            if not path.is_file() or path.stat().st_size == 0:
                raise ValueError("Missing narration or spoken timing: " + name)
    provenance_path = inside(root, "audio/provenance.json")
    if not provenance_path.is_file():
        raise ValueError("Missing narration provenance")
    provenance = {c["id"]: c for c in json.loads(provenance_path.read_text(encoding="utf-8"))["chapters"]}
    for chapter in story["chapters"]:
        name = chapter["id"]
        audio = inside(root, "audio/"+name+".mp3")
        timings = audio.with_suffix(".json")
        record = provenance.get(name, {})
        words = json.loads(timings.read_text(encoding="utf-8"))
        normalize = lambda value: re.sub(r"[\W_]", "", html.unescape(value))
        if (record.get("sha256") != sha256(audio.read_bytes()).hexdigest()
                or record.get("words_sha256") != sha256(timings.read_bytes()).hexdigest()
                or record.get("text_sha256") != sha256(chapter["narration"].encode("utf-8")).hexdigest()
                or normalize("".join(w["text"] for w in words)) != normalize(chapter["narration"])):
            raise ValueError("Stale or changed narration/boundaries: " + name)
    sources = {c["id"]: c for c in capture["chapters"]}
    for chapter in story["chapters"]:
        if chapter["kind"] in {"opening", "closing"}:
            continue
        source = sources.get(chapter["id"])
        if source is None:
            raise ValueError("Missing actual capture chapter")
        raw = inside(root, source["raw"])
        if not raw.is_file() or sha256(raw.read_bytes()).hexdigest() != source["sha256"]:
            raise ValueError("Actual capture hash mismatch")


def captions(words, duration, script=None):
    """Group provider word boundaries into readable, nonoverlapping subtitle cues."""
    if not words:
        raise ValueError("Missing spoken boundaries")
    cues, pending = [], []
    previous = 0
    for word in words:
        start, end = word["start"], word["end"]
        if not all(math.isfinite(x) for x in [start, end]) or not 0 <= start <= end <= duration + .05 or start < previous:
            raise ValueError("Invalid spoken boundary")
        previous = end
        text = html.unescape(word["text"]).strip()
        if not text:
            continue
        pending.append({**word, "text": text})
        phrase = " ".join(w["text"] for w in pending)
        if text.endswith((".", "?", "!")) or len(phrase) >= 35:
            cues.append([max(0, pending[0]["start"]-.05), min(duration, round(end+.2, 3)), phrase])
            pending = []
    if pending:
        cues.append([max(0, pending[0]["start"]-.05), min(duration, round(pending[-1]["end"]+.2, 3)), " ".join(w["text"] for w in pending)])
    for first, second in zip(cues, cues[1:]):
        first[1] = min(first[1], second[0])
    if script is not None:
        # Speech word events omit punctuation. Keep the author's complete Korean
        # sentences while anchoring their first/last characters to real timings.
        clean = lambda value: re.sub(r"[\W_]", "", html.unescape(value))
        if clean("".join(w["text"] for w in words)) != clean(script):
            raise ValueError("Spoken boundary text differs from narration")
        positions, cursor = [], 0
        for word in words:
            size = len(clean(word["text"]))
            positions.append((cursor, cursor+size, word))
            cursor += size
        cues, cursor = [], 0
        for sentence in re.split(r"(?<=[.!?])\s+", script.strip()):
            end = cursor + len(clean(sentence))
            touched = [w for a, b, w in positions if b > cursor and a < end]
            if not touched:
                raise ValueError("Sentence has no spoken boundary")
            cues.append([max(0,touched[0]["start"]-.05), min(duration,round(touched[-1]["end"]+.2,3)), sentence])
            cursor = end
        for first, second in zip(cues,cues[1:]):
            first[1] = min(first[1],second[0])
    return cues


def video_windows(raw_seconds, beats, budget):
    """Keep real-time windows around native states; cut waiting, never speed footage."""
    if raw_seconds <= 0 or budget <= 0 or not beats:
        raise ValueError("Capture timings are required")
    if any(not 0 <= b <= raw_seconds for b in beats):
        raise ValueError("Capture beat outside recording")
    span = min(5.0, budget / len(beats))
    windows = []
    for point in beats:
        end = min(raw_seconds, point + min(1.0, span/3))
        start = max(0, end-span)
        if windows and start <= windows[-1][1]:
            windows[-1] = (windows[-1][0], end)
        else:
            windows.append((start, end))
    return windows


def verified_beats(source, raw_seconds):
    beats = [b["at"] for b in source["beats"]]
    if (not beats or any(not math.isfinite(b) or not 0 <= b <= raw_seconds for b in beats)
            or raw_seconds < source["capture_seconds"]-1):
        raise ValueError("Actual capture is truncated; final native states may be missing")
    return beats


def run(*args):
    return subprocess.run(list(map(str, args)), check=True, capture_output=True, text=True, encoding="utf-8").stdout


def probe(path):
    return json.loads(run("ffprobe", "-v", "error", "-show_format", "-show_streams", "-of", "json", path))


def validate_concat_streams(parts):
    signatures = []
    for part in parts:
        video = next(s for s in part["streams"] if s["codec_type"] == "video")
        signatures.append(tuple(video.get(k) for k in ["codec_name", "width", "height", "r_frame_rate", "time_base", "pix_fmt"]))
    if not signatures or len(set(signatures)) != 1 or signatures[0][3] != "30/1":
        raise ValueError("Mixed video frame rate/timebase would desynchronize concatenated chapters")


def stamp(seconds, ass=False):
    total = int(round(seconds * (100 if ass else 1000)))
    divisor = 100 if ass else 1000
    h, remainder = divmod(total, divisor*3600)
    m, remainder = divmod(remainder, divisor*60)
    s, part = divmod(remainder, divisor)
    return f"{h:d}:{m:02d}:{s:02d}.{part:02d}" if ass else f"{h:02d}:{m:02d}:{s:02d},{part:03d}"


def wrapped(text, font, maximum):
    """Pixel-bounded Korean wrapping; preserve every character."""
    lines, line = [], ""
    for char in text:
        if char == "\n":
            lines.append(line); line = ""
        elif line and font.getlength(line + char) > maximum:
            lines.append(line.rstrip()); line = char.lstrip()
        else:
            line += char
    if line:
        lines.append(line.rstrip())
    return lines


def native_presentation():
    """Full 16:9 Pleos surface, including the left vehicle panel and bottom bar."""
    return 176, 88, 1568, 882


def chapter_windows(chapter, source, raw_seconds, seconds):
    beats = verified_beats(source, raw_seconds)
    focus = chapter.get("focus_beat")
    if focus:
        beat = next((b for b in source["beats"] if b.get("name") == focus), None)
        if beat is None:
            raise ValueError("Missing requested actual focus beat")
        start = beat["at"]
        return [(start, min(start + 1, raw_seconds))]
    return video_windows(raw_seconds, beats, seconds)


def validate_final_video(video):
    if video["r_frame_rate"] != "30/1" or video["avg_frame_rate"] != "30/1" or abs(float(video["start_time"])) > .001:
        raise ValueError("Final video is not constant 30fps from timestamp zero")
    if abs(int(video["nb_frames"]) - round(float(video["duration"]) * 30)) > 1:
        raise ValueError("Final constant-rate frame count disagrees with duration")


def background(chapter, index, total, target, font_dir):
    from PIL import Image, ImageDraw, ImageFont
    bg, panel, ink, muted, mint, gold = "#101C29", "#192A3B", "#F4F2EA", "#A8B9C7", "#8BE0CB", "#E8C88C"
    image = Image.new("RGB", (1920, 1080), bg)
    draw = ImageDraw.Draw(image)

    def font(size, bold=False):
        return ImageFont.truetype(str(font_dir / ("malgunbd.ttf" if bold else "malgun.ttf")), size)

    def text(x, y, value, size=28, color=ink, bold=False):
        draw.text((x, y), value, font=font(size, bold), fill=color, anchor="lt")

    if chapter["kind"] not in {"opening", "closing"}:
        draw.rectangle((0, 970, 1920, 1080), fill="#0B1420")
        text(32, 10, f"OnMyWay AI · {index+1:02d}/{total:02d} · {chapter['eyebrow']}", 21, mint)
        text(32, 42, chapter["title"].replace("\n", " "), 30, ink, True)
        text(1370, 10, "샘플 데이터 · 모의 안내", 20, gold)
        text(1370, 43, "Pleos 전체 화면 · 1× · 일부 정지", 19, muted)
        draw.rectangle((174, 86, 1746, 972), outline="#466276", width=2)
        image.save(target)
        return

    draw.rectangle((0, 958, 1920, 1080), fill="#0B1420")
    draw.line((76, 104, 1844, 104), fill="#2B3E4F", width=1)
    text(76, 43, "OnMyWay AI", 30, ink, True)
    text(346, 49, "퇴근길 완료 플래너", 23, muted)
    text(1255, 49, "예시 데이터 · 모의 안내·도착", 23, gold)
    text(76, 908, f"{index+1:02d} / {total:02d}    한 번의 퇴근길", 21, muted)

    if chapter["kind"] in {"opening", "closing"}:
        text(92, 177, chapter["eyebrow"], 30, mint)
        y = 255
        for line in chapter["title"].split("\n"):
            text(88, y, line, 88, ink, True); y += 126
        if chapter["kind"] == "opening":
            text(95, 565, "18:00 출발   →   19:00 집에서 저녁", 36, muted)
            for j, (title, detail, accent) in enumerate([
                    ("약 구매", "꼭 해야 하는 일", mint), ("세탁물 수령", "18:30 전 수령 완료", mint), ("우유 구매", "시간이 되면", gold)]):
                yy = 210+j*197
                draw.rounded_rectangle((1140, yy, 1825, yy+153), radius=22, fill=panel)
                text(1176, yy+28, title, 37, ink, True)
                text(1176, yy+89, detail, 27, accent)
            text(96, 721, "볼일도, 저녁 약속도 챙기고 싶은 날.", 31, gold)
            text(96, 815, "생활상황 재구성 · 준비된 예시로 시연", 22, muted)
        else:
            text(96, 569, "예시 여정 · 필수 볼일 2개 완료 · 집 도착", 32, mint)
            draw.rounded_rectangle((1160, 227, 1816, 656), radius=26, fill=panel)
            text(1200, 268, "18:53", 100, mint, True)
            text(1205, 424, "귀가 마감 19:00", 34)
            text(1205, 494, "선택 볼일인 우유는 다음에", 26, muted)
            for j, line in enumerate(["실제 Pleos 앱 · 예시 데이터와 모의 안내", "설명용 AI 음성 · 실제 STT 시연 아님", "실제 STT·LLM·지도·내비 수락/도착 및", "물리 차량 검증은 별도로 필요합니다."]):
                text(96, 700+j*41, line, 23, muted)
    else:
        text(76, 168, chapter["eyebrow"], 24, mint)
        size = 57
        while max(font(size, True).getlength(line) for line in chapter["title"].split("\n")) > 555:
            size -= 1
        for j, line in enumerate(chapter["title"].split("\n")):
            text(73, 234+j*86, line, size, ink, True)
        changed = index >= 5
        text(78, 470, "예시 계산 요약" if changed else "오늘의 조건", 24, muted)
        rows = [("18:11", "세탁물 수령 완료"), ("18:46", "약 상담·구매 완료"), ("18:53", "집 도착 예상")] if changed else [
            ("18:00", "퇴근 후 출발"), ("18:30", "세탁물 수령 마감"), ("19:00", "집에서 저녁 약속")]
        for j, (when, what) in enumerate(rows):
            yy = 532+j*91
            if j < 2:
                draw.line((90, yy+12, 90, yy+103), fill="#365468", width=3)
            draw.ellipse((81, yy+4, 99, yy+22), fill=mint)
            text(120, yy-1, when, 31, mint, True)
            text(255, yy+2, what, 27)
        text(79, 819, "우유 구매 · 이번에는 제외" if changed else "우유는 시간이 되면", 25, gold)
        text(79, 864, "집 도착까지 7분 여유 · 예시 기준" if changed else "설명용 한국어 AI 내레이션", 20, muted)
        draw.rectangle((696, 130, 1856, 950), outline="#466276", width=2)
        text(704, 111, "실제 차량 전체 화면" if chapter["kind"] == "vehicle" else "실제 Pleos 앱 화면 · 과정 축약·일부 화면 정지", 18, muted)
    image.save(target)


def music_bed(path, seconds):
    """Original quiet sine-keyboard bed; no downloaded stock music."""
    rate = 16000
    chords = [(261.63, 329.63, 392.0), (220.0, 261.63, 329.63), (174.61, 261.63, 349.23), (196.0, 293.66, 392.0)]
    samples = array("h")
    for i in range(math.ceil(seconds*rate)):
        t = i/rate
        position = t % 6
        envelope = min(1, position/.4) * math.exp(-position/2.7) * min(1, (6-position)/.5)
        chord = chords[int(t//6) % len(chords)]
        tone = sum(math.sin(2*math.pi*f*t) + .16*math.sin(4*math.pi*f*t) for f in chord)/3
        samples.append(int(32767 * .012 * envelope * tone))
    with wave.open(str(path), "wb") as wav:
        wav.setparams((1, 2, rate, 0, "NONE", "not compressed")); wav.writeframes(samples.tobytes())


def build(root, story_path, font_dir):
    (root / "media-verification.json").write_text(json.dumps({"state": "building", "file": "OnMyWay_AI_story_demo.mp4"}), encoding="utf-8")
    story = json.loads(story_path.read_text(encoding="utf-8"))
    capture = json.loads((root / "capture-manifest.json").read_text(encoding="utf-8"))
    validate_inputs(story, capture, root)
    cuts = root / "edit"
    cuts.mkdir(exist_ok=True)
    sources = {c["id"]: c for c in capture["chapters"]}
    all_cues, chapters, cursor = [], [], 0
    for index, ch in enumerate(story["chapters"]):
        audio = root / "audio" / (ch["id"] + ".mp3")
        info = probe(audio)
        if not any(s["codec_type"] == "audio" for s in info["streams"]):
            raise ValueError("Narration has no audio stream")
        audio_seconds = float(info["format"]["duration"])
        words = json.loads(audio.with_suffix(".json").read_text(encoding="utf-8"))
        cues = captions(words, audio_seconds, ch["narration"])
        duration = max(12, math.ceil((audio_seconds + (5 if ch["kind"] == "closing" else 2.1))*30)/30)
        for a, b, text in cues:
            all_cues.append([cursor+.7+a, cursor+.7+b, text])
        chapters.append({"id": ch["id"], "kind": ch["kind"], "start": cursor, "seconds": duration, "audio_seconds": audio_seconds})
        cursor += duration
        background(ch, index, len(story["chapters"]), cuts / (ch["id"] + ".png"), font_dir)
    music_bed(cuts / "original-bed.wav", cursor)
    from PIL import ImageFont
    caption_font = ImageFont.truetype(str(font_dir / "malgun.ttf"), 33)
    ass = ["[Script Info]", "ScriptType: v4.00+", "PlayResX: 1920", "PlayResY: 1080", "WrapStyle: 2", "", "[V4+ Styles]",
           "Format: Name,Fontname,Fontsize,PrimaryColour,SecondaryColour,OutlineColour,BackColour,Bold,Italic,Underline,StrikeOut,ScaleX,ScaleY,Spacing,Angle,BorderStyle,Outline,Shadow,Alignment,MarginL,MarginR,MarginV,Encoding",
           "Style: Default,Malgun Gothic,33,&H00FFFFFF,&H00FFFFFF,&H0020140B,&H0020140B,0,0,0,0,100,100,0,0,1,1,0,2,90,90,30,1", "", "[Events]",
           "Format: Layer,Start,End,Style,Name,MarginL,MarginR,MarginV,Effect,Text"]
    srt = []
    for i, (a, b, text) in enumerate(all_cues):
        lines = wrapped(text, caption_font, 1720)
        if len(lines) > 2:
            raise ValueError("Caption overflow")
        srt.append(f"{i+1}\n{stamp(a)} --> {stamp(b)}\n" + "\n".join(lines) + "\n")
        ass.append(f"Dialogue: 0,{stamp(a, True)},{stamp(b, True)},Default,,0,0,0,," + "\\N".join(lines))
    (root / "OnMyWay_AI_story_demo.srt").write_text("\n".join(srt), encoding="utf-8")
    (cuts / "captions.ass").write_text("\n".join(ass), encoding="utf-8-sig")
    for ch, timing in zip(story["chapters"], chapters):
        name, duration = ch["id"], timing["seconds"]
        bg = cuts / (name + ".png")
        target = cuts / (name + ".mp4")
        command = ["ffmpeg", "-y", "-v", "error"]
        graph = []
        if name in sources:
            source = sources[name]
            raw = root / source["raw"]
            raw_seconds = float(probe(raw)["format"]["duration"])
            windows = chapter_windows(ch, source, raw_seconds, duration-1)
            timing["source_windows"] = windows
            timing["raw_sha256"] = source["sha256"]
            command += ["-i", str(raw), "-loop", "1", "-i", str(bg), "-i", str(root/"audio"/(name+".mp3")), "-ss", str(timing["start"]), "-i", str(cuts/"original-bed.wav")]
            for j, (a, b) in enumerate(windows):
                graph.append(f"[0:v]trim=start={a}:end={b},setpts=PTS-STARTPTS[v{j}]")
            graph.append("".join(f"[v{j}]" for j in range(len(windows))) + f"concat=n={len(windows)}:v=1:a=0[native]")
            x, y, width, height = native_presentation()
            graph.append(f"[native]scale={width}:{height}:force_original_aspect_ratio=decrease,pad={width}:{height}:(ow-iw)/2:(oh-ih)/2,setsar=1,fps=30,tpad=stop_mode=clone:stop_duration={duration},trim=duration={duration}[screen]")
            graph.append(f"[1:v][screen]overlay={x}:{y}:shortest=1,fps=30,format=yuv420p[v]")
            ai, mi = 2, 3
        else:
            command += ["-loop", "1", "-i", str(bg), "-i", str(root/"audio"/(name+".mp3")), "-ss", str(timing["start"]), "-i", str(cuts/"original-bed.wav")]
            graph.append(f"[0:v]fps=30,trim=duration={duration},format=yuv420p[v]")
            ai, mi = 1, 2
        graph += [f"[{ai}:a]loudnorm=I=-17:TP=-2:LRA=8,aresample=48000,adelay=700|700,apad,atrim=duration={duration}[speech]",
                  f"[{mi}:a]aresample=48000,atrim=duration={duration}[bed]",
                  "[speech][bed]amix=inputs=2:duration=first:normalize=0,alimiter=limit=0.92[a]"]
        command += ["-filter_complex", ";".join(graph), "-map", "[v]", "-map", "[a]", "-t", str(duration), "-c:v", "libx264", "-preset", "fast", "-crf", "20", "-pix_fmt", "yuv420p", "-r", "30", "-video_track_timescale", "90000", "-c:a", "aac", "-b:a", "160k", "-ar", "48000", "-ac", "2", "-map_metadata", "-1", "-movflags", "+faststart", str(target)]
        run(*command)
        print("RENDERED: " + name, flush=True)
    validate_concat_streams([probe(cuts/(c["id"]+".mp4")) for c in chapters])
    (cuts / "concat.txt").write_text("".join("file '"+c["id"]+".mp4'\n" for c in chapters), encoding="utf-8")
    intermediate = cuts / "assembled.mp4"
    subprocess.run(["ffmpeg", "-y", "-v", "error", "-f", "concat", "-safe", "0", "-i", "concat.txt", "-c", "copy", str(intermediate)], cwd=cuts, check=True)
    final = root / "OnMyWay_AI_story_demo.mp4"
    pending = root / ".story-rendering.mp4"
    subprocess.run(["ffmpeg", "-y", "-v", "error", "-i", "assembled.mp4", "-vf", "fps=30:start_time=0,ass=captions.ass", "-r", "30", "-fps_mode", "cfr", "-t", str(cursor), "-c:v", "libx264", "-preset", "fast", "-crf", "20", "-c:a", "copy", "-map_metadata", "-1", "-movflags", "+faststart", str(pending)], cwd=cuts, check=True)
    data = probe(pending)
    video = next(s for s in data["streams"] if s["codec_type"] == "video")
    validate_final_video(video)
    assert (video["width"], video["height"]) == (1920, 1080)
    assert any(s["codec_type"] == "audio" for s in data["streams"])
    assert abs(float(data["format"]["duration"])-cursor) < .1, "Chapter timeline drift"
    run("ffmpeg", "-v", "error", "-i", pending, "-f", "null", "-")
    pending.replace(final)
    frames = root / "frames"
    frames.mkdir(exist_ok=True)
    for ch in chapters:
        run("ffmpeg", "-y", "-v", "error", "-ss", ch["start"]+ch["seconds"]*.65, "-i", final, "-frames:v", "1", frames/(ch["id"]+".png"))
    evidence = {"file": final.name, "seconds": float(data["format"]["duration"]), "width": 1920, "height": 1080,
                "sha256": sha256(final.read_bytes()).hexdigest(), "bytes": final.stat().st_size,
                "audio": "Korean AI explanatory narration plus original quiet synthesized music", "speed": "1x actual footage; waiting cut, final states held; explicitly labelled",
                "scope": capture["scope"], "full_decode": "passed", "visual_review": "pending", "playback_review": "pending", "chapters": chapters}
    (root / "media-verification.json").write_text(json.dumps(evidence, ensure_ascii=False, indent=2), encoding="utf-8")
    (root / "index.html").write_text('<!doctype html><html lang="ko"><meta charset="utf-8"><title>OnMyWay AI — 오늘의 약속</title><style>body{margin:0;background:#101c29;color:#f4f2ea;font-family:Malgun Gothic,sans-serif}main{max-width:1440px;margin:28px auto}video{width:100%;max-height:calc(100vh - 190px);background:#0b1420}p{line-height:1.7;color:#a8b9c7}a{color:#8be0cb}</style><main><h1>오늘의 약속을, 놓치지 않도록.</h1><video controls preload="metadata" src="OnMyWay_AI_story_demo.mp4" poster="frames/opening.png"></video><p>실제 Pleos 앱 화면 · 예시 데이터와 모의 안내·도착 · 한국어 AI 설명 내레이션<br>실제 STT·LLM·지도·내비 수락/도착 및 물리 차량 검증은 별도입니다.</p><a href="OnMyWay_AI_story_demo.srt">한국어 자막 SRT</a></main></html>', encoding="utf-8")
    return evidence

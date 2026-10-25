"""Build the local-only Korean contest Short from approved genuine captures.

Requires Python 3, Pillow, ffmpeg/ffprobe and the three original raw recordings.
No network, credentials, TTS, music, invented native UI or provider fallback.
"""
import argparse
from functools import lru_cache
from hashlib import sha256
import json
from pathlib import Path
import subprocess

from PIL import Image, ImageDraw, ImageFont, ImageOps

ROOT = Path(__file__).resolve().parents[1]
TIMELINE = ROOT / "assets/shortform/timeline-ko.json"
W, H, FPS = 1080, 1920, 30
WHITE, MUTED, LIME, ORANGE = "#F4F1E9", "#ACBBC7", "#DBFF73", "#FFB087"
PANEL, BORDER = "#152B3B", "#345061"


def run(*args, capture=True):
    return subprocess.run(list(map(str, args)), check=True, capture_output=capture,
                          text=True, encoding="utf-8").stdout


def probe(path):
    return json.loads(run("ffprobe", "-v", "error", "-show_streams", "-show_format",
                          "-of", "json", path))


def digest(path):
    with path.open("rb") as stream:
        return sha256(stream.read()).hexdigest()


def validate_timeline(data):
    if (data["width"], data["height"], data["fps"]) != (W, H, FPS):
        raise ValueError("Expected 1080x1920 at 30 fps")
    scenes = data["scenes"]
    if not scenes or any(s["duration"] <= 0 for s in scenes):
        raise ValueError("Scene durations must be positive")
    if not 30 <= sum(s["duration"] for s in scenes) <= 60:
        raise ValueError("Contest duration must be 30 to 60 seconds")
    if len({s["id"] for s in scenes}) != len(scenes):
        raise ValueError("Scene ids must be unique")
    for source in data["sources"].values():
        if Path(source["file"]).name != source["file"] or ":" in source["file"] or "\\" in source["file"]:
            raise ValueError("Source files must be basenames")
    for scene in scenes:
        lines = scene["caption"].splitlines()
        if not 1 <= len(lines) <= 2 or any(len(line) > 32 for line in lines):
            raise ValueError("Captions need one or two short lines")
        if len("".join(lines)) / scene["duration"] > 11:
            raise ValueError("Caption reading rate exceeds 11 characters/second")
        if any(c in scene["caption"] for c in "<>\r"):
            raise ValueError("SRT must be plain text")
        if "clip" in scene and scene["clip"]["source"] not in data["sources"]:
            raise ValueError("Unknown genuine source")


def validate_clip(clip, duration, width, height, seconds):
    x, y, w, h = clip["crop"]
    if min(x, y) < 0 or min(w, h) <= 0 or x + w > width or y + h > height:
        raise ValueError("Crop exceeds source frame")
    if clip["rate"] <= 0 or clip["start"] < 0 or clip["start"] + duration * clip["rate"] > seconds:
        raise ValueError("Trim exceeds source duration or has invalid speed")


def validate_sources(data, source_dir):
    result = {}
    for key, source in data["sources"].items():
        path = source_dir / source["file"]
        if not path.is_file():
            raise FileNotFoundError(f"Missing genuine capture: {source['file']}; no replacement is generated")
        if digest(path) != source["sha256"]:
            raise ValueError(f"Source hash mismatch: {source['file']}")
        info = probe(path)
        video = next(s for s in info["streams"] if s["codec_type"] == "video")
        result[key] = {"path": path, "width": video["width"], "height": video["height"],
                       "seconds": float(info["format"]["duration"])}
    for scene in data["scenes"]:
        if "clip" in scene:
            source = result[scene["clip"]["source"]]
            validate_clip(scene["clip"], scene["duration"], source["width"], source["height"], source["seconds"])
    return result


def stamp(seconds):
    ms = round(seconds * 1000)
    hours, ms = divmod(ms, 3600000)
    minutes, ms = divmod(ms, 60000)
    seconds, ms = divmod(ms, 1000)
    return f"{hours:02}:{minutes:02}:{seconds:02},{ms:03}"


def make_srt(data):
    output, cursor = [], 0
    for number, scene in enumerate(data["scenes"], 1):
        end = cursor + scene["duration"]
        output.append(f"{number}\n{stamp(cursor)} --> {stamp(end)}\n{scene['caption']}\n")
        cursor = end
    return "\n".join(output) + "\n"


@lru_cache(maxsize=100)
def font(size, bold=False):
    return ImageFont.truetype(str(FONT_BOLD if bold else FONT_REGULAR), size)


def text(draw, xy, value, size, fill=WHITE, bold=False, max_width=808):
    selected = font(size, bold)
    while draw.textlength(value, font=selected) > max_width and size > 22:
        size -= 1
        selected = font(size, bold)
    if draw.textlength(value, font=selected) > max_width:
        raise ValueError(f"Text cannot fit safely: {value}")
    draw.text(xy, value, font=selected, fill=fill, anchor="lt")


def panel(draw, box, outline=BORDER, fill=PANEL, radius=28):
    draw.rounded_rectangle(box, radius=radius, fill=fill, outline=outline, width=2)


def pill(draw, x, y, label, color=LIME):
    width = int(draw.textlength(label, font=font(28, True))) + 36
    panel(draw, (x, y, x + width, y + 54), outline=color, fill=color, radius=20)
    text(draw, (x + 18, y + 10), label, 28, "#102331", True)


def route(draw, y, completed=False):
    positions = [112, 354, 596, 838]
    draw.line((positions[0], y, positions[-1], y), fill=BORDER, width=7)
    for n, (x, label) in enumerate(zip(positions, ["약", "세탁물", "우유", "집"])):
        draw.ellipse((x - 15, y - 15, x + 15, y + 15), fill=LIME if completed or n < 2 else PANEL,
                     outline=LIME, width=4)
        text(draw, (x - 36, y + 46), label, 34, WHITE, True, max_width=150)


def artwork(scene, index, total):
    gradient = Image.linear_gradient("L").resize((W, H))
    image = ImageOps.colorize(gradient, "#081A29", "#163C48").convert("RGB")
    draw = ImageDraw.Draw(image)
    accent = ORANGE if scene.get("warning") else LIME
    # Original schematic road, deliberately not a geographic map or native UI.
    for offset in range(-800, 1600, 240):
        draw.line((offset, H, offset + 850, 0), fill="#1B3544", width=2)
    draw.arc((410, 450, 1330, 1370), 100, 275, fill="#254754", width=4)
    text(draw, (72, 104), "ONMYWAY AI", 29, WHITE, True)
    text(draw, (640, 109), "PLEOS CONNECT", 22, MUTED, max_width=240)
    draw.line((72, 174, 880, 174), fill=BORDER, width=3)
    text(draw, (72, 216), scene["chapter"], 30, accent, True)
    for line, value in enumerate(scene["headline"]):
        text(draw, (72, 300 + line * 111), value, 78,
             accent if line == scene.get("accent_line") else WHITE, True)
    art = scene["art"]
    if art == "clock":
        text(draw, (78, 642), "귀가 마감", 36, MUTED)
        text(draw, (66, 702), "19:00", 190, LIME, True)
        draw.line((80, 943, 872, 943), fill=BORDER, width=2)
        for n, label in enumerate(["약", "세탁물", "우유"]):
            x = 78 + n * 274
            panel(draw, (x, 1010, x + 254, 1194))
            text(draw, (x + 26, 1042), label, 44, WHITE, True, 210)
            text(draw, (x + 26, 1110), "필수" if n < 2 else "선택", 29, LIME if n < 2 else MUTED)
    elif art == "required":
        for n, (label, note) in enumerate([("약 구매", "필수 · 10분 머무름"), ("세탁물 찾기", "필수 · 18:30 전 수령 완료")]):
            y = 678 + n * 268
            panel(draw, (78, y, 880, y + 232), outline=LIME if n == 0 else ORANGE)
            text(draw, (112, y + 34), label, 54, WHITE, True)
            text(draw, (112, y + 128), note, 35, LIME if n == 0 else ORANGE)
    elif art == "milk":
        draw.polygon([(300, 770), (430, 664), (630, 664), (724, 770), (724, 1195), (300, 1195)], fill=PANEL)
        draw.line([(300, 770), (430, 664), (630, 664), (724, 770), (724, 1195), (300, 1195), (300, 770)], fill=LIME, width=6)
        draw.line((300, 770, 724, 770), fill=LIME, width=5)
        text(draw, (409, 880), "우유", 88, WHITE, True)
        pill(draw, 419, 1040, "선택")
    elif art in ("route", "close"):
        text(draw, (76, 682), "OnMyWay", 115, WHITE, True)
        text(draw, (78, 820), "AI", 105, LIME, True)
        route(draw, 1055, completed=art == "close")
        text(draw, (78, 1210), "퇴근길 완료 플래너", 41, WHITE, True)
        if art == "close":
            pill(draw, 78, 1310, "당신의 퇴근길은?")
    elif art == "deadline":
        text(draw, (78, 680), "19:00", 77, MUTED, True)
        draw.line((78, 724, 340, 724), fill=ORANGE, width=6)
        text(draw, (66, 820), "18:20", 186, ORANGE, True)
        panel(draw, (78, 1088, 880, 1212), outline=ORANGE)
        text(draw, (110, 1128), "필수 일정과 함께 지킬 수 없어요", 36, ORANGE, True)
    elif art == "footage":
        panel(draw, (72, 620, 896, 1244), outline=BORDER, fill="#0A1C28")
        text(draw, (94, 646), "실제 앱 화면 확대 · 편집 · 1배속", 26, MUTED)
        text(draw, (94, 710), scene["focus"], 42, WHITE, True, max_width=780)
        text(draw, (78, 1275), scene["note"], 32, accent, True)
    else:
        raise ValueError(f"Unknown artwork: {art}")
    # The exact same copy is exported to SRT for later owner-produced TTS.
    caption_lines = scene["caption"].splitlines()
    for line, value in enumerate(caption_lines):
        text(draw, (78, 1420 + line * 62), value, 41, WHITE, True)
    draw.line((78, 1570, 880, 1570), fill=BORDER, width=2)
    text(draw, (78, 1598), "예시 해석·가상 장소·시간 / 안내·도착은 모의", 27, MUTED)
    text(draw, (78, 1644), "실제 AI·도로·내비 결과 아님", 27, MUTED)
    # Area below 1680 and right of 900 intentionally carries no critical copy.
    return image


def encode_scene(scene, index, start, sources, work):
    duration = scene["duration"]
    poster, segment = work / f"{index:02}-{scene['id']}.png", work / f"{index:02}-{scene['id']}.mp4"
    artwork(scene, index, 14).save(poster)
    args = ["ffmpeg", "-y", "-v", "error", "-loop", "1", "-framerate", FPS, "-i", poster]
    if "clip" in scene:
        clip = scene["clip"]
        source = sources[clip["source"]]["path"]
        args += ["-ss", clip["start"], "-t", duration * clip["rate"], "-i", source]
        x, y, w, h = clip["crop"]
        target_w = 792
        target_h = int(target_w * h / w) // 2 * 2
        if target_h > 440:
            target_h = 440
            target_w = int(target_h * w / h) // 2 * 2
        px, py = 484 - target_w // 2, 1000 - target_h // 2
        filters = (f"[1:v:0]setpts=(PTS-STARTPTS)/{clip['rate']},crop={w}:{h}:{x}:{y},"
                   f"scale={target_w}:{target_h}:flags=lanczos,setsar=1,fps={FPS}[capture];"
                   f"[0:v][capture]overlay={px}:{py}:shortest=1[base];")
    else:
        filters = "[0:v]null[base];"
    # A continuous route cursor adds motion without simulating native interaction.
    # The hook is fully visible on frame zero; no opening black/fade frames.
    filters += (f"color=c=0xDBFF73:s=12x12:r={FPS}:d={duration}[cursor];"
                f"[base][cursor]overlay=x='72+808*({start}+t)/54':y=168,"
                "format=yuv420p[out]")
    args += ["-filter_complex", filters, "-map", "[out]", "-frames:v", round(duration * FPS),
             "-an", "-map_metadata", "-1", "-c:v", "libx264", "-preset", "fast", "-crf", "19",
             "-r", FPS, "-pix_fmt", "yuv420p", "-movflags", "+faststart", segment]
    run(*args)
    return segment


def verify_output(final, data, srt):
    info = probe(final)
    streams = info["streams"]
    video = [s for s in streams if s["codec_type"] == "video"]
    if len(video) != 1 or any(s["codec_type"] == "audio" for s in streams):
        raise ValueError("Output must have one video stream and no audio")
    v = video[0]
    seconds = float(info["format"]["duration"])
    if (v["width"], v["height"], v["codec_name"], v["pix_fmt"], v["avg_frame_rate"]) != (W, H, "h264", "yuv420p", "30/1"):
        raise ValueError("Unexpected output encoding")
    if abs(seconds - sum(s["duration"] for s in data["scenes"])) > 1 / FPS:
        raise ValueError("Encoded timeline length mismatch")
    if srt.read_text(encoding="utf-8") != make_srt(data):
        raise ValueError("SRT does not match rendered timeline")
    run("ffmpeg", "-v", "error", "-xerror", "-i", final, "-f", "null", "-")
    run("ffprobe", "-v", "error", "-show_streams", "-of", "json", srt)
    return {"file": final.name, "sha256": digest(final), "bytes": final.stat().st_size,
            "width": W, "height": H, "fps": FPS, "seconds": seconds, "codec": "h264",
            "pixel_format": "yuv420p", "audio_streams": 0, "full_decode": "passed",
            "srt_file": srt.name, "srt_sha256": digest(srt), "srt_cues": len(data["scenes"]),
            "source_hashes": {k: v["sha256"] for k, v in data["sources"].items()},
            "source_scope": "genuine Pleos emulator recordings; prepared interpretation and synthetic place/time; simulated navigation/arrival",
            "editorial_speed": "1x selected raw excerpts; crops and titles are editorial",
            "visual_review": "pending", "browser_playback": "pending"}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--source-dir", required=True, type=Path, help="Directory containing the three approved raw MP4s")
    parser.add_argument("--output-dir", type=Path, default=ROOT / "artifacts/shortform")
    parser.add_argument("--font", type=Path, default=Path("C:/Windows/Fonts/malgun.ttf"))
    parser.add_argument("--font-bold", type=Path, default=Path("C:/Windows/Fonts/malgunbd.ttf"))
    parser.add_argument("--reuse-segments", action="store_true", help="Reuse existing encoded segments only when their inputs match")
    args = parser.parse_args()
    global FONT_REGULAR, FONT_BOLD
    FONT_REGULAR, FONT_BOLD = args.font, args.font_bold
    if not FONT_REGULAR.is_file() or not FONT_BOLD.is_file():
        raise FileNotFoundError("Provide locally installed Korean fonts with --font/--font-bold")
    data = json.loads(TIMELINE.read_text(encoding="utf-8"))
    validate_timeline(data)
    sources = validate_sources(data, args.source_dir.resolve())
    work = args.output_dir.resolve() / "work"
    work.mkdir(parents=True, exist_ok=True)
    srt = ROOT / "artifacts/evidence/OnMyWay_AI_Shorts_ko.srt"
    srt.write_text(make_srt(data), encoding="utf-8", newline="\n")
    (args.output_dir / srt.name).write_bytes(srt.read_bytes())
    cursor, segments = 0, []
    for index, scene in enumerate(data["scenes"], 1):
        # Include both code and fonts: cached shots never hide a changed renderer.
        key = sha256((json.dumps(scene, sort_keys=True, ensure_ascii=False) + digest(Path(__file__)) +
                      digest(FONT_REGULAR) + digest(FONT_BOLD) + str(cursor) +
                      (data["sources"][scene["clip"]["source"]]["sha256"] if "clip" in scene else "")).encode()).hexdigest()
        cache = work / f"{index:02}-{scene['id']}.sha256"
        segment = work / f"{index:02}-{scene['id']}.mp4"
        if not (args.reuse_segments and segment.is_file() and cache.is_file() and cache.read_text() == key):
            print(f"Rendering {index}/{len(data['scenes'])}: {scene['id']}", flush=True)
            segment = encode_scene(scene, index, cursor, sources, work)
            cache.write_text(key)
        segments.append(segment)
        cursor += scene["duration"]
    (work / "concat.txt").write_text("".join(f"file '{s.name}'\n" for s in segments), encoding="utf-8")
    final = args.output_dir.resolve() / "OnMyWay_AI_Shorts_silent.mp4"
    run("ffmpeg", "-y", "-v", "error", "-f", "concat", "-safe", "0", "-i", work / "concat.txt",
        "-c", "copy", "-an", "-map_metadata", "-1", "-movflags", "+faststart", final)
    evidence = verify_output(final, data, srt)
    cursor = 0
    for index, scene in enumerate(data["scenes"], 1):
        run("ffmpeg", "-y", "-v", "error", "-ss", cursor + scene["duration"] / 2, "-i", final,
            "-frames:v", "1", work / f"sample-{index:02}.jpg")
        cursor += scene["duration"]
    (args.output_dir / "verification.json").write_text(json.dumps(evidence, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    (args.output_dir / "player.html").write_text('''<!doctype html><html lang="ko"><meta charset="utf-8">
<title>OnMyWay AI · 쇼츠 로컬 검수</title>
<style>body{margin:0;background:#061622;color:#eee;font:16px sans-serif;display:grid;place-items:center}
video{height:min(90vh,900px);max-width:100vw}p{margin:8px}</style>
<p>54초 무음 · TTS 추가 전 · 로컬 검수</p>
<video id="short" controls playsinline muted preload="auto" src="OnMyWay_AI_Shorts_silent.mp4"></video>
</html>''', encoding="utf-8")
    print(json.dumps(evidence, ensure_ascii=False, indent=2), flush=True)


if __name__ == "__main__":
    main()

"""Build the local-only Korean contest Short from approved genuine captures.

Requires Python 3, Pillow, ffmpeg/ffprobe and the three original raw recordings.
No network, credentials, TTS, music, invented native UI or provider fallback.
"""
import argparse
from hashlib import sha256
import json
from pathlib import Path
import subprocess

from PIL import Image
from shortform_motion import prepare_scene, render_frame, TRANSITIONS

ROOT = Path(__file__).resolve().parents[1]
TIMELINE = ROOT / "assets/shortform/timeline-ko.json"
MOTION = ROOT / "scripts/shortform_motion.py"
W, H, FPS = 1080, 1920, 30


def run(*args, capture=True):
    return subprocess.run(list(map(str, args)), check=True, capture_output=capture,
                          text=True, encoding="utf-8").stdout


def probe(path):
    return json.loads(run("ffprobe", "-v", "error", "-show_streams", "-show_format",
                          "-of", "json", path))


def digest(path):
    with path.open("rb") as stream:
        return sha256(stream.read()).hexdigest()


def frame_bounds(data):
    result, cursor = [], 0
    for scene in data["scenes"]:
        frames = scene["duration"] * FPS
        if abs(frames - round(frames)) > 1e-6:
            raise ValueError("Scene duration must align to a video frame")
        result.append((cursor, cursor + round(frames)))
        cursor += round(frames)
    return result


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
    bounds = frame_bounds(data)
    for scene, (start, end) in zip(scenes, bounds):
        transition = scene.get("transition", {"kind": "cut", "frames": 0})
        if transition["kind"] not in TRANSITIONS or not isinstance(transition["frames"], int) or not 0 <= transition["frames"] <= min(8, (end - start) // 2):
            raise ValueError("Invalid transition kind or duration")
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
    return "\n".join(output)


def encode_scene(scene, index, start, sources, work):
    """Stream deterministic animated RGB frames and genuine footage to FFmpeg."""
    duration = scene["duration"]
    frames = round(duration * FPS)
    segment = work / f"{index:02}-{scene['id']}.mp4"
    layout = prepare_scene(scene, FONT_REGULAR, FONT_BOLD)
    previous_path = work / f"last-{index-1:02}.png"
    previous = Image.open(previous_path).convert("RGB") if index > 1 else None
    capture_reader = None
    capture_size = None
    with (work / f"encode-{index:02}.log").open("wb") as error_log:
        if "clip" in scene:
            clip = scene["clip"]
            x, y, w, h = clip["crop"]
            target_w = 792
            target_h = int(target_w * h / w) // 2 * 2
            if target_h > 440:
                target_h = 440
                target_w = int(target_h * w / h) // 2 * 2
            capture_size = (target_w, target_h)
            capture_reader = subprocess.Popen([
                "ffmpeg", "-v", "error", "-ss", str(clip["start"]), "-i", str(sources[clip["source"]]["path"]),
                "-map", "0:v:0", "-vf", f"setpts=(PTS-STARTPTS)/{clip['rate']},crop={w}:{h}:{x}:{y},scale={target_w}:{target_h}:flags=lanczos,fps={FPS}",
                "-frames:v", str(frames), "-an", "-f", "rawvideo", "-pix_fmt", "rgb24", "pipe:1"
            ], stdout=subprocess.PIPE, stderr=error_log)
        encoder = subprocess.Popen([
            "ffmpeg", "-y", "-v", "error", "-f", "rawvideo", "-pix_fmt", "rgb24", "-s", f"{W}x{H}",
            "-r", str(FPS), "-i", "pipe:0", "-frames:v", str(frames), "-an", "-map_metadata", "-1",
            "-c:v", "libx264", "-preset", "fast", "-crf", "19", "-pix_fmt", "yuv420p", "-movflags", "+faststart", str(segment)
        ], stdin=subprocess.PIPE, stderr=error_log)
        try:
            for frame_index in range(frames):
                capture = None
                if capture_reader is not None:
                    size = capture_size[0] * capture_size[1] * 3
                    raw = capture_reader.stdout.read(size)
                    if len(raw) != size:
                        raise RuntimeError("Genuine capture ended early; no synthetic replacement frame is generated")
                    capture = Image.frombytes("RGB", capture_size, raw)
                frame = render_frame(layout, frame_index / FPS, capture, previous)
                encoder.stdin.write(frame.tobytes())
            encoder.stdin.close()
            if encoder.wait() != 0:
                raise RuntimeError(f"FFmpeg encoding failed; see {error_log.name}")
            if capture_reader is not None and capture_reader.wait() != 0:
                raise RuntimeError(f"FFmpeg capture decoding failed; see {error_log.name}")
            frame.save(work / f"last-{index:02}.png")
        finally:
            for process in (encoder, capture_reader):
                if process is not None and process.poll() is None:
                    process.kill()
                    process.wait()
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
            "srt_file": srt.name, "srt_sha256": digest(srt), "srt_cues": len(data["scenes"]), "rendered_frames": round(seconds * FPS),
            "motion": "original layered character/card animation; swipe, wipe, split and zoom cuts; stationary caption/disclosure",
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
    cursor, segments, previous_key = 0, [], ""
    for index, scene in enumerate(data["scenes"], 1):
        # Include both code and fonts: cached shots never hide a changed renderer.
        key = sha256((json.dumps(scene, sort_keys=True, ensure_ascii=False) + digest(Path(__file__)) +
                      digest(FONT_REGULAR) + digest(FONT_BOLD) + digest(MOTION) + previous_key + str(cursor) +
                      (data["sources"][scene["clip"]["source"]]["sha256"] if "clip" in scene else "")).encode()).hexdigest()
        cache = work / f"{index:02}-{scene['id']}.sha256"
        segment = work / f"{index:02}-{scene['id']}.mp4"
        if not (args.reuse_segments and segment.is_file() and cache.is_file() and cache.read_text() == key and (work / f"last-{index:02}.png").is_file()):
            print(f"Rendering {index}/{len(data['scenes'])}: {scene['id']}", flush=True)
            segment = encode_scene(scene, index, cursor, sources, work)
            cache.write_text(key)
        previous_key = key
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
video{width:min(100vw,50vh);height:auto;aspect-ratio:9/16}p{margin:8px}</style>
<p>42초 무음 · TTS 추가 전 · 로컬 검수</p>
<video id="short" controls playsinline muted preload="auto" src="OnMyWay_AI_Shorts_silent.mp4"></video>
</html>''', encoding="utf-8")
    print(json.dumps(evidence, ensure_ascii=False, indent=2), flush=True)


if __name__ == "__main__":
    main()

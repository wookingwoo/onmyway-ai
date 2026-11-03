"""Editorial film timing and validation. No credential or environment-file reads."""
from hashlib import sha256
import json
import math
import re


def audio_fingerprint(payload):
    return sha256(json.dumps(payload, sort_keys=True, ensure_ascii=False).encode()).hexdigest()


def parse_loudness(stderr):
    start=stderr.rfind('{')
    if start<0: raise ValueError('FFmpeg did not emit loudness metrics')
    return json.JSONDecoder().raw_decode(stderr[start:])[0]


def cached_record(audio,record,fingerprint,pending):
    if audio.exists() and record.exists():
        saved=json.loads(record.read_text(encoding='utf-8'))
        if saved['fingerprint']!=fingerprint or saved['sha256']!=sha256(audio.read_bytes()).hexdigest():
            raise ValueError('Stale audio cache; archive explicitly before generation')
        return saved
    if audio.exists() or record.exists() or pending.exists():
        raise ValueError('An incomplete or interrupted paid request exists; inspect local recovery files before any new charge')
    return None


def alignment_cues(alignment, text, duration):
    chars = alignment.get("characters", [])
    starts = alignment.get("character_start_times_seconds", [])
    ends = alignment.get("character_end_times_seconds", [])
    normalize = lambda s: re.sub(r"\s", "", s)
    if (not chars or len(chars) != len(starts) or len(chars) != len(ends)
            or normalize("".join(chars)) != normalize(text)):
        raise ValueError("Missing, incomplete or stale provider alignment")
    previous = 0
    for s, e in zip(starts, ends):
        if not all(math.isfinite(x) for x in (s,e)) or not 0 <= s <= e <= duration+.08 or s < previous-.08:
            raise ValueError("Invalid provider timing")
        previous = s
    result, pending, begin = [], "", None
    for i, char in enumerate(chars):
        if begin is None and char.strip(): begin = starts[i]
        pending += char
        if begin is not None and (char in ".?!" or (char.isspace() and len(pending.strip()) >= 22) or i == len(chars)-1):
            end = min(duration, max(ends[i]+.12, begin+.2))
            result.append([max(0, begin-.03), end, pending.strip()])
            pending, begin = "", None
    if pending.strip(): result.append([begin, duration, pending.strip()])
    for a, b in zip(result, result[1:]): a[1] = min(a[1], b[0])
    if not result or any(e <= s for s,e,_ in result): raise ValueError("Empty or overlapping caption")
    return result


def scene_seconds(minimum, audio_seconds):
    return max(minimum, math.ceil((audio_seconds+1.35)*2)/2)


def validate_delivery(info, short=False):
    if info["fps"] != 30 or info["audio_streams"] != 1: raise ValueError("Missing speech or non-CFR delivery")
    if short:
        if (info["width"],info["height"]) != (1080,1920) or not 30 <= info["seconds"] <= 60:
            raise ValueError("Short must be vertical 1080x1920 and 30–60 seconds")
    elif (info["width"],info["height"]) != (1920,1080): raise ValueError("Main must be 1920x1080")


def validate_story(story):
    s = story["scenario"]
    minute = lambda t: int(t[:2])*60+int(t[3:])
    departure = minute(s["departure"])
    laundry = departure+8+2+5
    base = laundry+8+5
    revised = laundry+8+30
    expected = {"laundry_complete":laundry, "pharmacy_base_complete":base,
                "pharmacy_revised_complete":revised, "home_base":base+7+7+8,
                "home_unchanged":revised+7+7+8, "home_revised":revised+10}
    if any(minute(s[k]) != v for k,v in expected.items()): raise ValueError("Scenario arithmetic mismatch")
    if laundry > minute(s["laundry_deadline"]) or revised+10 > minute(s["home_deadline"]):
        raise ValueError("Mandatory or final deadline missed")
    if s["mandatory"] != ["laundry","medicine"] or s["omitted"] != ["milk"]:
        raise ValueError("Only optional milk may be omitted")
    validate_utterances(story)


def validate_utterances(story):
    ids = [c["id"] for track in ("main","short","auditions") for c in story[track]]
    if len(ids) != len(set(ids)) or any(not re.fullmatch(r"[a-z0-9-]+",i) for i in ids):
        raise ValueError("Unsafe or duplicate utterance ID")
    for track in ("main","short","auditions"):
        for c in story[track]:
            if c["role"] not in story["voices"] or not c["text"].strip(): raise ValueError("Invalid voice role or empty text")


def srt(cues):
    def stamp(t):
        n=round(t*1000)
        return f"{n//3600000:02}:{n//60000%60:02}:{n//1000%60:02},{n%1000:03}"
    return "\n\n".join(f"{i}\n{stamp(a)} --> {stamp(b)}\n{text}" for i,(a,b,text) in enumerate(cues,1))+"\n"

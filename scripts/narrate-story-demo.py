"""Produce explicitly selected editorial Korean narration, separate from app STT.

Uses the public edge-tts client with an installed/discovered Korean voice. No
application key or environment file is read. Audio and spoken boundaries stay local.
"""
import argparse
import asyncio
from hashlib import sha256
import importlib.metadata
import json
from pathlib import Path

import edge_tts


async def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--story", default="scripts/story_demo.json")
    parser.add_argument("--output", default="artifacts/evidence/pleos-service-demo")
    parser.add_argument("--voice", default="ko-KR-SunHiNeural")
    args = parser.parse_args()
    story = json.loads(Path(args.story).read_text(encoding="utf-8"))
    available = await edge_tts.list_voices()
    assert any(v["ShortName"] == args.voice and v["Locale"] == "ko-KR" for v in available), "Choose an available Korean voice"
    output = Path(args.output) / "audio"
    output.mkdir(parents=True, exist_ok=True)
    records = []
    for chapter in story["chapters"]:
        name = chapter["id"]
        path = output / (name + ".mp3")
        words = []
        speech = edge_tts.Communicate(chapter["narration"], args.voice, rate="-6%", pitch="-1Hz", boundary="WordBoundary")
        with path.open("wb") as audio:
            async for chunk in speech.stream():
                if chunk["type"] == "audio":
                    audio.write(chunk["data"])
                elif chunk["type"] == "WordBoundary":
                    words.append({"text": chunk["text"], "start": chunk["offset"]/10_000_000,
                                  "end": (chunk["offset"]+chunk["duration"])/10_000_000})
        assert path.stat().st_size > 1000 and words, "Missing audio/boundaries; no silent fallback"
        timing_path = output / (name + ".json")
        timing_path.write_text(json.dumps(words, ensure_ascii=False, indent=2), encoding="utf-8")
        records.append({"id": name, "sha256": sha256(path.read_bytes()).hexdigest(), "words": len(words),
                        "words_sha256": sha256(timing_path.read_bytes()).hexdigest(),
                        "text_sha256": sha256(chapter["narration"].encode("utf-8")).hexdigest()})
        print("NARRATED: " + name, flush=True)
    provenance = {"provider": "Microsoft Edge online speech via edge-tts", "edge_tts": importlib.metadata.version("edge-tts"),
                  "voice": args.voice, "rate": "-6%", "pitch": "-1Hz", "purpose": "editorial AI narration, not app STT or app voice output",
                  "chapters": records}
    (output / "provenance.json").write_text(json.dumps(provenance, ensure_ascii=False, indent=2), encoding="utf-8")


if __name__ == "__main__":
    asyncio.run(main())

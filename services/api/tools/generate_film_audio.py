"""Server-side, local-only ElevenLabs production. Never prints keys or response bodies.

Run from repository root; only this server-side helper reads ignored .env.
Paid POSTs are never automatically retried. Cached bytes require matching inputs.
"""
import argparse
import base64
from hashlib import sha256
import json
from pathlib import Path
import subprocess
import sys

import httpx
from dotenv import dotenv_values

ROOT = Path(__file__).resolve().parents[3]
sys.path.insert(0,str(ROOT/"scripts"))
from final_film import audio_fingerprint, alignment_cues, validate_story, validate_utterances, cached_record


def main():
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument("--track",choices=["auditions","main","short","all"],default="auditions")
    p.add_argument("--mainactivity", action="store_true", help="Use the existing-app cut and its separate audio cache")
    args=p.parse_args()
    filename = "mainactivity_film_story.json" if args.mainactivity else "final_film_story.json"
    data=json.loads((ROOT/"scripts"/filename).read_text(encoding="utf-8"))
    (validate_utterances if args.mainactivity else validate_story)(data)
    key=dotenv_values(ROOT/".env").get("ELEVENLABS_API_KEY","")
    if not key: raise SystemExit("Missing local ELEVENLABS_API_KEY; no fallback generated")
    out=ROOT/ ("artifacts/final-films/mainactivity/audio" if args.mainactivity else "artifacts/final-films/audio")
    out.mkdir(parents=True,exist_ok=True)
    tracks=["main","short"] if args.track=="all" else [args.track]
    with httpx.Client(base_url="https://api.elevenlabs.io",headers={"xi-api-key":key},timeout=120) as client:
        for c in [c for t in tracks for c in data[t] if not c.get("visual_only")]:
            voice=data["voices"][c["role"]]
            payload={k:v for k,v in voice.items() if k not in {"name","voice_id"}}
            payload.update(text=c["text"],apply_text_normalization="on")
            inputs={**payload,"voice_id":voice["voice_id"],"output_format":"mp3_44100_128"}
            fingerprint=audio_fingerprint(inputs)
            audio=out/(c.get("audio_id", c["id"])+".mp3")
            record=audio.with_suffix(".json")
            pending=audio.with_suffix('.pending.json')
            saved=cached_record(audio,record,fingerprint,pending)
            if saved is not None:
                print(f"Cached {c['id']}: {saved['seconds']:.2f}s",flush=True)
                continue
            print(f"Generating {c['id']} / {c['role']} / {voice['name']}",flush=True)
            # Record intent BEFORE a paid POST, including uncertain network failures.
            pending.write_text(json.dumps({'fingerprint':fingerprint,'state':'request_started'}),encoding='utf-8')
            try:
                r=client.post(f"/v1/text-to-speech/{voice['voice_id']}/with-timestamps",
                              params={"output_format":"mp3_44100_128"},json=payload)
            except httpx.HTTPError:
                raise SystemExit(f"Network failure for {c['id']}; no automatic paid retry") from None
            if r.status_code!=200:
                detail=r.json().get("detail",{}) if r.headers.get("content-type","").startswith("application/json") else {}
                code=detail.get("status","unknown") if isinstance(detail,dict) else "unknown"
                raise SystemExit(f"ElevenLabs HTTP{r.status_code}, status={code}; no fallback")
            response=r.json()
            # Keep the complete recoverable result even if local probing fails.
            audio.with_suffix('.response.json').write_text(json.dumps(response),encoding='utf-8')
            raw=base64.b64decode(response["audio_base64"],validate=True)
            audio.write_bytes(raw)
            seconds=float(subprocess.check_output(["ffprobe","-v","error","-show_entries","format=duration",
                          "-of","default=nw=1:nk=1",str(audio)],text=True))
            alignment=response.get("alignment") or response.get("normalized_alignment")
            cues=alignment_cues(alignment,c["text"],seconds)
            saved={"fingerprint":fingerprint,"sha256":sha256(raw).hexdigest(),"text_sha256":sha256(c["text"].encode()).hexdigest(),
                   "provider":"ElevenLabs","voice_id":voice["voice_id"],"voice_name":voice["name"],"model_id":voice["model_id"],
                   "seconds":seconds,"alignment":alignment,"cues":cues,"request_id":r.headers.get("request-id"),
                   "scope":"Editorial speech; not native Pleos output or recognized user input"}
            record.write_text(json.dumps(saved,ensure_ascii=False,indent=2),encoding="utf-8")
            pending.unlink()
            print(f"Saved {c['id']}: {seconds:.2f}s, {len(cues)} timed captions",flush=True)


if __name__=="__main__": main()

"""Assemble the local-only film from verified actual captures and narration."""
import argparse
import json
from pathlib import Path

from story_demo import build


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--output", default="artifacts/evidence/story-demo")
    parser.add_argument("--story", default="scripts/story_demo.json")
    parser.add_argument("--font-dir", default="C:/Windows/Fonts")
    args = parser.parse_args()
    try:
        result = build(Path(args.output).resolve(), Path(args.story), Path(args.font_dir))
    except Exception as error:
        (Path(args.output) / "media-verification.json").write_text(json.dumps({"state": "failed", "reason": str(error)}), encoding="utf-8")
        raise
    print(json.dumps({k: v for k, v in result.items() if k != "chapters"}, ensure_ascii=True, indent=2))

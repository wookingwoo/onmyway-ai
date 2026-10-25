"""Boundary checks for the local contest-video production contract."""
import importlib.util
import json
from pathlib import Path
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[2]


class ShortformTests(unittest.TestCase):
    def builder(self):
        path = ROOT / "scripts/build-shortform.py"
        self.assertTrue(path.is_file(), "Shortform builder has not been implemented")
        spec = importlib.util.spec_from_file_location("shortform", path)
        module = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(module)
        return module

    def timeline(self):
        return json.loads((ROOT / "assets/shortform/timeline-ko.json").read_text(encoding="utf-8"))

    def test_contest_duration_and_caption_readability(self):
        module = self.builder()
        data = self.timeline()
        module.validate_timeline(data)
        self.assertEqual(sum(s["duration"] for s in data["scenes"]), 54)
        for scene in data["scenes"]:
            self.assertLessEqual(len(scene["caption"].splitlines()), 2)
            self.assertLessEqual(len(scene["caption"].replace("\n", "")) / scene["duration"], 11)

    def test_srt_is_plain_utf8_ordered_and_ends_with_video(self):
        module = self.builder()
        result = module.make_srt(self.timeline())
        self.assertEqual(result.encode("utf-8").decode("utf-8"), result)
        self.assertNotIn("\ufeff", result)
        self.assertIn("1\n00:00:00,000 --> 00:00:03,000\n", result)
        self.assertIn("00:00:48,000 --> 00:00:54,000", result)
        self.assertNotIn("<", result)
        self.assertEqual(len(result.strip().split("\n\n")), 14)
        self.assertFalse(result.endswith("\n\n"), "Keep SRT separators between cues without a Git trailing-blank-line warning")

    def test_timeline_rejects_overlong_or_zero_length_scenes(self):
        module = self.builder()
        for duration in (0, -1, 100):
            data = self.timeline()
            data["scenes"][0]["duration"] = duration
            with self.assertRaises(ValueError):
                module.validate_timeline(data)

    def test_timeline_rejects_escaping_source_paths(self):
        module = self.builder()
        data = self.timeline()
        data["sources"]["normal"]["file"] = "../different.mp4"
        with self.assertRaises(ValueError):
            module.validate_timeline(data)

    def test_missing_or_tampered_footage_fails_without_substitution(self):
        module = self.builder()
        with tempfile.TemporaryDirectory() as directory:
            base = Path(directory)
            with self.assertRaises(FileNotFoundError):
                module.validate_sources(self.timeline(), base)
            source = {"sources": {"test": {"file": "capture.mp4", "sha256": "0" * 64}}, "scenes": []}
            (base / "capture.mp4").write_bytes(b"not the approved capture")
            with self.assertRaisesRegex(ValueError, "hash"):
                module.validate_sources(source, base)

    def test_crop_and_trim_cannot_exceed_genuine_source(self):
        module = self.builder()
        good = {"start": 2, "rate": 1, "crop": [640, 70, 1240, 880]}
        module.validate_clip(good, 4, 1920, 1080, 10)
        for edit in ({"start": 9}, {"rate": 0}, {"crop": [1800, 0, 200, 100]},
                     {"crop": [-1, 0, 200, 100]}, {"crop": [0, 0, 0, 100]}):
            with self.assertRaises(ValueError):
                module.validate_clip(dict(good, **edit), 4, 1920, 1080, 10)


if __name__ == "__main__":
    unittest.main()

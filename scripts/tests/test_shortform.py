"""Boundary checks for the local contest-video production contract."""
import importlib.util
import json
from pathlib import Path
import tempfile
import sys
import unittest

from PIL import Image, ImageChops

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / "scripts"))


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
        self.assertEqual(sum(s["duration"] for s in data["scenes"]), 42)
        self.assertEqual(module.frame_bounds(data)[-1][1], 1260)
        for scene in data["scenes"]:
            self.assertLessEqual(len(scene["caption"].splitlines()), 2)
            self.assertLessEqual(len(scene["caption"].replace("\n", "")) / scene["duration"], 11)

    def test_srt_is_plain_utf8_ordered_and_ends_with_video(self):
        module = self.builder()
        result = module.make_srt(self.timeline())
        self.assertEqual(result.encode("utf-8").decode("utf-8"), result)
        self.assertNotIn("\ufeff", result)
        self.assertIn("1\n00:00:00,000 --> 00:00:01,500\n", result)
        self.assertIn("00:00:38,000 --> 00:00:42,000", result)
        self.assertNotIn("<", result)
        self.assertEqual(len(result.strip().split("\n\n")), 18)
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

    def test_fractional_duration_must_align_exactly_to_a_video_frame(self):
        module = self.builder()
        data = self.timeline()
        data["scenes"][0]["duration"] = 1.501
        with self.assertRaisesRegex(ValueError, "frame"):
            module.validate_timeline(data)

    def test_transition_cannot_be_unknown_or_consume_the_scene(self):
        module = self.builder()
        for transition in ({"kind": "unknown", "frames": 5}, {"kind": "swipe", "frames": 50},
                           {"kind": "wipe", "frames": -1}):
            data = self.timeline()
            data["scenes"][0]["transition"] = transition
            with self.assertRaisesRegex(ValueError, "transition"):
                module.validate_timeline(data)

    def motion(self):
        self.assertTrue((ROOT / "scripts/shortform_motion.py").is_file(), "Motion renderer has not been implemented")
        import shortform_motion
        return shortform_motion

    def fonts(self):
        regular = Path("C:/Windows/Fonts/malgun.ttf")
        bold = Path("C:/Windows/Fonts/malgunbd.ttf")
        if not regular.is_file():
            regular = Path("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf")
            bold = Path("/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf")
        if not regular.is_file() or not bold.is_file():
            self.skipTest("No local font available for compositor unit tests")
        return regular, bold

    def test_animation_changes_body_but_keeps_captions_and_disclosure_stationary(self):
        module = self.motion()
        regular, bold = self.fonts()
        scene = self.timeline()["scenes"][0]
        layout = module.prepare_scene(scene, regular, bold)
        a = module.render_frame(layout, 0.2)
        b = module.render_frame(layout, 0.7)
        self.assertIsNotNone(ImageChops.difference(a.crop((60, 270, 920, 1360)), b.crop((60, 270, 920, 1360))).getbbox())
        self.assertIsNone(ImageChops.difference(a.crop((60, 1380, 920, 1690)), b.crop((60, 1380, 920, 1690))).getbbox())
        self.assertIsNone(ImageChops.difference(a, module.render_frame(layout, 0.2)).getbbox())
        transition_layout = module.prepare_scene(self.timeline()["scenes"][8], regular, bold)
        with_previous = module.render_frame(transition_layout, 0.1, previous=Image.new("RGB", (1080, 1920), "magenta"))
        without_previous = module.render_frame(transition_layout, 0.1)
        self.assertIsNone(ImageChops.difference(with_previous.crop((60, 1380, 920, 1690)), without_previous.crop((60, 1380, 920, 1690))).getbbox())

    def test_hook_is_visible_on_frame_zero_without_black_transition(self):
        module = self.motion()
        regular, bold = self.fonts()
        layout = module.prepare_scene(self.timeline()["scenes"][0], regular, bold)
        hook = module.render_frame(layout, 0).crop((72, 280, 900, 570))
        bright = sum(1 for r, g, b in hook.getdata() if min(r, g, b) > 200)
        self.assertGreater(bright, 1000)


if __name__ == "__main__":
    unittest.main()

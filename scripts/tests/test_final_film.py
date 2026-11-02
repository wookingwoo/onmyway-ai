"""Paid-media cache and timeline boundaries; no network or credential access."""
import copy
import importlib.util
import math
from pathlib import Path
import sys
import tempfile
import unittest

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from final_film import audio_fingerprint, alignment_cues, scene_seconds, validate_story, validate_delivery, parse_loudness, cached_record


class FinalFilmBoundaries(unittest.TestCase):
    def test_an_interrupted_paid_request_cannot_be_automatically_rebilled(self):
        with tempfile.TemporaryDirectory() as folder:
            audio=Path(folder)/'voice.mp3'; record=audio.with_suffix('.json'); pending=audio.with_suffix('.pending.json')
            self.assertIsNone(cached_record(audio,record,'current',pending))
            for partial in (audio,record,pending):
                partial.write_text('interrupted',encoding='utf-8')
                with self.assertRaisesRegex(ValueError,'incomplete|interrupted'):
                    cached_record(audio,record,'current',pending)
                partial.unlink()

    def test_ffmpeg_loudness_json_can_have_a_trailing_progress_summary(self):
        self.assertEqual(parse_loudness('progress\n{ "input_i": "-16.1", "input_tp": "-1.5" }\n[out] audio:12kB'),
                         {"input_i":"-16.1","input_tp":"-1.5"})

    def test_cache_changes_with_every_paid_generation_input(self):
        base = {"text": "집으로 가자.", "voice_id": "actual-id", "model_id": "eleven_v3",
                "voice_settings": {"stability": .5}, "language_code": "ko"}
        for key, value in [("text", "약국으로 가자."), ("voice_id", "other-id"),
                           ("model_id", "eleven_multilingual_v2"),
                           ("voice_settings", {"stability": 1.0})]:
            changed = {**base, key: value}
            self.assertNotEqual(audio_fingerprint(base), audio_fingerprint(changed))
        self.assertEqual(audio_fingerprint(base), audio_fingerprint(dict(reversed(list(base.items())))))

    def test_captions_use_complete_matching_provider_timestamps(self):
        text = "도착. 완료는 따로."
        a = {"characters": list(text), "character_start_times_seconds": [i*.1 for i in range(len(text))],
             "character_end_times_seconds": [(i+1)*.1 for i in range(len(text))]}
        cues = alignment_cues(a, text, 2)
        self.assertEqual("".join(c[2] for c in cues).replace(" ", ""), text.replace(" ", ""))
        self.assertTrue(all(0 <= s < e <= 2 for s,e,_ in cues))
        for bad in [{**a, "characters": list("stale")},
                    {**a, "character_end_times_seconds": [math.nan]*len(text)},
                    {**a, "character_start_times_seconds": [3.0]*len(text)}]:
            with self.assertRaises(ValueError): alignment_cues(bad, text, 2)

    def test_scene_never_truncates_a_long_utterance(self):
        self.assertEqual(scene_seconds(3, 7.11), 8.5)
        self.assertEqual(scene_seconds(8, 2), 8)

    def test_short_rule_is_enforced_and_no_audio_is_rejected(self):
        good = {"width": 1080, "height": 1920, "fps": 30, "seconds": 52, "audio_streams": 1}
        validate_delivery(good, short=True)
        for bad in [{**good, "seconds": 61}, {**good, "seconds": 29.9},
                    {**good, "width": 1920}, {**good, "audio_streams": 0}]:
            with self.assertRaises(ValueError): validate_delivery(bad, short=True)

    def test_scenario_preserves_deadlines_and_mandatory_errands(self):
        import json
        data = json.loads((Path(__file__).resolve().parents[1]/"final_film_story.json").read_text(encoding="utf-8"))
        validate_story(data)
        for change in [("home_revised", "19:01"), ("laundry_complete", "18:31"),
                       ("pharmacy_revised_complete", "18:42")]:
            bad = copy.deepcopy(data)
            bad["scenario"][change[0]] = change[1]
            with self.assertRaises(ValueError): validate_story(bad)


if __name__ == "__main__": unittest.main()

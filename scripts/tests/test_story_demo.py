"""Meaningful local assembly boundaries; no AVD or provider access."""
import importlib.util
from hashlib import sha256
import json
from pathlib import Path
import sys
import tempfile
import unittest

SCRIPTS = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(SCRIPTS))
from story_demo import inside, captions, validate_inputs, video_windows, validate_concat_streams, verified_beats


class StoryDemoBoundaries(unittest.TestCase):
    def test_rejects_path_escaping_the_local_capture_directory(self):
        with tempfile.TemporaryDirectory() as folder:
            with self.assertRaisesRegex(ValueError, "outside"):
                inside(Path(folder), "../private.mp4")

    def test_subtitles_follow_spoken_boundaries_without_overlapping(self):
        words = [{"text": "도착은", "start": .2, "end": .8},
                 {"text": "완료가 아닙니다.", "start": .9, "end": 2.3},
                 {"text": "직접 확인해 주세요.", "start": 3, "end": 5.1}]
        cues = captions(words, 6)
        self.assertEqual(" ".join(c[2] for c in cues), "도착은 완료가 아닙니다. 직접 확인해 주세요.")
        self.assertGreaterEqual(cues[0][0], 0)
        self.assertEqual(cues[-1][1], 5.3)
        self.assertTrue(all(a[1] <= b[0] for a, b in zip(cues, cues[1:])))

    def test_missing_narration_cannot_silently_make_a_silent_film(self):
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)
            with self.assertRaisesRegex(ValueError, "narration"):
                validate_inputs({"chapters": [{"id": "opening", "kind": "opening"}]},
                                {"result": "pass", "chapters": []}, root)

    def test_failed_capture_cannot_become_successful_media(self):
        with tempfile.TemporaryDirectory() as folder:
            with self.assertRaisesRegex(ValueError, "capture"):
                validate_inputs({"chapters": []}, {"result": "failed"}, Path(folder))

    def test_cut_windows_are_bounded_and_keep_the_final_decision(self):
        windows = video_windows(40, [5, 15, 38], 18)
        self.assertTrue(all(0 <= a < b <= 40 for a, b in windows))
        self.assertTrue(any(a <= 38 <= b for a, b in windows))
        self.assertLessEqual(sum(b-a for a, b in windows), 18.001)

    def test_invalid_spoken_boundary_is_rejected(self):
        with self.assertRaisesRegex(ValueError, "boundary"):
            captions([{"text": "잘못된 시간", "start": 3, "end": 2}], 5)

    def test_mixed_timebases_cannot_silently_desynchronize_chapters(self):
        def stream(rate, timebase):
            return {"streams": [{"codec_type": "video", "codec_name": "h264", "width": 1920, "height": 1080,
                                 "r_frame_rate": rate, "time_base": timebase, "pix_fmt": "yuv420p"}]}
        with self.assertRaisesRegex(ValueError, "timebase"):
            validate_concat_streams([stream("30/1", "1/15360"), stream("25/1", "1/12800")])

    def test_missing_spoken_words_cannot_produce_uncaptioned_success(self):
        with self.assertRaisesRegex(ValueError, "spoken"):
            captions([], 8)

    def test_recording_cannot_clamp_a_missing_final_state_into_view(self):
        with self.assertRaisesRegex(ValueError, "truncated"):
            verified_beats({"capture_seconds": 25, "beats": [{"at": 23}]}, 15)

    def test_current_script_is_bound_to_generated_narration(self):
        with tempfile.TemporaryDirectory() as folder:
            root=Path(folder); audio=root/'audio'; audio.mkdir()
            (audio/'opening.mp3').write_bytes(b'generated audio')
            (audio/'opening.json').write_text('[{"text":"집으로 갑니다.","start":0,"end":1}]',encoding='utf-8')
            original='집으로 갑니다.'
            provenance={"chapters":[{"id":"opening", "sha256":sha256((audio/'opening.mp3').read_bytes()).hexdigest(),
                         "words_sha256":sha256((audio/'opening.json').read_bytes()).hexdigest(),
                         "text_sha256":sha256(original.encode()).hexdigest()}]}
            (audio/'provenance.json').write_text(json.dumps(provenance),encoding='utf-8')
            capture={"result":"pass","chapters":[]}
            good={"chapters":[{"id":"opening","kind":"opening","narration":original}]}
            validate_inputs(good,capture,root)
            good['chapters'][0]['narration']='약국에 먼저 갑니다.'
            with self.assertRaisesRegex(ValueError,'narration'):
                validate_inputs(good,capture,root)

    def test_actual_word_timing_preserves_complete_korean_sentences(self):
        words=[{"text":"귀가는", "start":0,"end":.4}, {"text":"여섯", "start":.5,"end":.8},
               {"text":"시오십삼분예상", "start":.9,"end":2},
               {"text":"바뀐순서를확인합니다", "start":2.6,"end":4}]
        result=captions(words,5,"귀가는 여섯 시 오십삼 분 예상. 바뀐 순서를 확인합니다.")
        self.assertEqual([c[2] for c in result],["귀가는 여섯 시 오십삼 분 예상.","바뀐 순서를 확인합니다."])
        self.assertGreaterEqual(result[1][0],2.5)


if __name__ == "__main__":
    unittest.main()

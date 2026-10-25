import unittest
from array import array
from pathlib import Path
import sys
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from native_demo_audio import aligned_pcm


class AlignmentTest(unittest.TestCase):
    def test_keeps_silence_between_speaker_packets_without_speeding_up(self):
        data = aligned_pcm([(100.1, array('h', [10, 20]).tobytes()), (100.5, array('h', [30]).tobytes())], 100, 1, rate=10)
        self.assertEqual(list(array('h', data)), [0, 10, 20, 0, 0, 30, 0, 0, 0, 0])

    def test_clips_packets_before_and_after_the_video_window(self):
        data = aligned_pcm([(99.9, array('h', [8, 9]).tobytes()), (100.9, array('h', [6, 7]).tobytes())], 100, 1, rate=10)
        self.assertEqual(list(array('h', data)), [9, 0, 0, 0, 0, 0, 0, 0, 0, 6])

    def test_rejects_partial_pcm(self):
        with self.assertRaises(ValueError):
            aligned_pcm([(100, b'x')], 100, 1)

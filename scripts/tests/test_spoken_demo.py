import unittest
from pathlib import Path
import sys
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from spoken_demo import merge_windows, remap_interval


class CutsTest(unittest.TestCase):
    def test_merge_never_reorders_or_speeds_up_kept_source(self):
        self.assertEqual([(1, 5), (8, 10)], merge_windows([(8, 10), (1, 3), (2, 5)], 10))
        for windows in [[(-1, 2)], [(1, 12)], [(4, 4)]]:
            with self.assertRaises(ValueError): merge_windows(windows, 10)

    def test_audio_and_caption_mapping_share_the_same_cut_clock(self):
        windows = [(1, 5), (8, 10)]
        self.assertEqual((4.5, 5.5), remap_interval(8.5, 9.5, windows))
        with self.assertRaises(ValueError): remap_interval(4, 9, windows)

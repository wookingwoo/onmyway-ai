"""Native-footage provenance and trim boundaries, without device access."""
from hashlib import sha256
from pathlib import Path
import sys
import tempfile
import unittest

sys.path.insert(0,str(Path(__file__).resolve().parents[1]))
from native_film import source_window, validate_native_capture, restricted_window


class NativeFilmBoundaries(unittest.TestCase):
    def test_driving_trim_uses_reviewed_locked_interval_not_late_screenshot_time(self):
        self.assertEqual(restricted_window({'locked_window':[11.2,19.8]},8.5),(11.2,19.7))
        for record in ({},{'locked_window':[12,19]}):
            with self.assertRaises(ValueError):restricted_window(record,8.5)

    def test_short_or_untrusted_capture_cannot_substitute_a_drawn_screen(self):
        with tempfile.TemporaryDirectory() as d:
            root=Path(d);(root/'request.mp4').write_bytes(b'actual capture bytes')
            (root/'captures').mkdir()
            xml = root/'captures/request.xml'
            xml.write_text('<hierarchy><node package="ai.onmyway.car.demo" text="Request"/></hierarchy>')
            record={'id':'request','raw':'request.mp4','sha256':sha256(b'actual capture bytes').hexdigest(),
                    'beats':[{'name':'request','expected':'Request'}]}
            manifest={'result':'pass','restored_gear':'Park','restored_speed_kph':0,'chapters':[record]}
            validate_native_capture(manifest,root)
            for change in ({'result':'fail'},{'restored_gear':'Drive'},{'restored_speed_kph':30}):
                with self.assertRaises(ValueError): validate_native_capture({**manifest,**change},root)
            xml.write_text('<hierarchy><node package="ai.onmyway.car.film" text="Request"/></hierarchy>')
            with self.assertRaises(ValueError): validate_native_capture(manifest,root)
            (root/'request.mp4').write_bytes(b'changed')
            with self.assertRaises(ValueError): validate_native_capture(manifest,root)

    def test_source_trim_is_one_x_and_stays_within_genuine_footage(self):
        self.assertEqual(source_window(2,5,8),(2,7))
        self.assertEqual(source_window(5,5,8),(3,8))
        for seconds in (3,0):
            with self.assertRaises(ValueError):source_window(1,5,seconds)

if __name__=='__main__':unittest.main()

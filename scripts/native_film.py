"""Pure provenance/trim rules for actual native Pleos screen recordings."""
from hashlib import sha256
import xml.etree.ElementTree as ET


def source_window(start,duration,available):
    if min(start,duration,available)<0 or duration==0 or duration>available:
        raise ValueError('Native source is too short; never generate replacement UI')
    start=min(start,available-duration)
    return start,start+duration


def restricted_window(capture,duration):
    window=capture.get('locked_window')
    if not window or len(window)!=2 or window[0]<0 or window[1]-window[0]<duration:
        raise ValueError('A reviewed continuously restricted source interval is required')
    return window[0],round(window[0]+duration,6)


def validate_native_capture(manifest,root):
    if manifest.get('result') != 'pass' or manifest.get('restored_gear') != 'Park' or manifest.get('restored_speed_kph') != 0:
        raise ValueError('Complete official Pleos native capture required')
    root=root.resolve()
    for c in manifest['chapters']:
        path=(root/c['raw']).resolve()
        if not path.is_relative_to(root) or not path.is_file() or sha256(path.read_bytes()).hexdigest()!=c['sha256']:
            raise ValueError('Missing or changed native capture')
        for beat in c['beats']:
            if 'expected' not in beat:
                continue
            xml = (root / 'captures' / (beat['name'] + '.xml')).resolve()
            if not xml.is_relative_to(root) or not xml.is_file():
                raise ValueError('Missing native UI evidence')
            if not any(node.get('package') == 'ai.onmyway.car.demo' and
                       beat['expected'] in node.get('text', '') for node in ET.parse(xml).iter('node')):
                raise ValueError('Expected screen from existing MainActivity demo package')

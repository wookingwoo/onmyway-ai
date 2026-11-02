"""Pure provenance/trim rules for actual native Pleos screen recordings."""
from hashlib import sha256


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
    if not manifest.get('complete') or manifest.get('avd')!='OnMyWay_Pleos' or manifest.get('package')!='ai.onmyway.car.film':
        raise ValueError('Complete official Pleos native capture required')
    root=root.resolve()
    for c in manifest['captures']:
        path=(root/c['file']).resolve()
        if not path.is_relative_to(root) or not path.is_file() or sha256(path.read_bytes()).hexdigest()!=c['sha256']:
            raise ValueError('Missing or changed native capture')

"""Actual Pleos voice UI checks. Example text is never a live STT/LLM claim.

Requires the signed demo installed on the isolated OnMyWay_Pleos emulator,
parked, at the input stage, with the localhost API available for --mode example.
Only synthetic/example drafts may be used in screenshots.
"""
import argparse
import json
from pathlib import Path
import re
import subprocess
import time
import xml.etree.ElementTree as ET


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--adb', required=True)
    parser.add_argument('--serial', required=True)
    parser.add_argument('--mode', choices=['example', 'sdk', 'lock-ready'], default='example')
    parser.add_argument('--reset-fixture', action='store_true', help='Explicitly reset a previous labelled fixture journey')
    parser.add_argument('--output', default='artifacts/evidence/voice-demo')
    args = parser.parse_args()
    assert args.serial.startswith('emulator-'), 'Use an isolated emulator'
    out = Path(args.output)
    out.mkdir(parents=True, exist_ok=True)

    def adb(*items):
        return subprocess.run([args.adb, '-s', args.serial, *items], check=True,
                              capture_output=True, text=True, encoding='utf-8').stdout

    assert 'OnMyWay_Pleos' in adb('emu', 'avd', 'name')

    def nodes():
        adb('shell', 'uiautomator', 'dump', '/sdcard/omw-voice-smoke.xml')
        return list(ET.fromstring(adb('shell', 'cat', '/sdcard/omw-voice-smoke.xml')).iter('node'))

    def find(text, tree, contains=False):
        return next((n for n in tree if (text in n.get('text', '') if contains else text == n.get('text', ''))), None)

    def wait(text, contains=False, timeout=18):
        until = time.monotonic() + timeout
        while time.monotonic() < until:
            n = find(text, nodes(), contains)
            if n is not None:
                return n
            time.sleep(.2)
        raise AssertionError('Missing visible UI: ' + text)

    def tap(node):
        assert node is not None and node.get('enabled') == 'true'
        x, y, right, bottom = map(int, re.findall(r'\d+', node.get('bounds', '')))
        adb('shell', 'input', 'tap', str((x + right) // 2), str((y + bottom) // 2))

    def click(text):
        tap(wait(text))

    def shot(name):
        adb('shell', 'screencap', '-p', '/sdcard/omw-voice-smoke.png')
        adb('pull', '/sdcard/omw-voice-smoke.png', str(out / name))

    def draft(tree):
        return next(n.get('text') for n in tree if n.get('class') == 'android.widget.EditText')

    def microphone_idle():
        until = time.monotonic() + 4
        while time.monotonic() < until:
            value = adb('shell', 'cmd', 'appops', 'get', '--user', '10', 'ai.pleos.playground.caas', 'RECORD_AUDIO')
            if '(running)' not in value:
                return
            time.sleep(.2)
        raise AssertionError('Pleos Speech Service still owns the microphone after cancellation')

    tree = nodes()
    assert any(n.get('package') == 'ai.onmyway.car.demo' for n in tree)
    if find('PLEOS · 온디바이스 음성 입력', tree) is not None or find('예시 문장 체험 · 실제 음성 인식 아님', tree) is not None:
        click('취소')
    tree = nodes()
    if args.reset_fixture and find('가는 길에, 무엇을 할까요?', tree) is None:
        assert find('예시 데이터', tree, True) is not None, 'Never reset a non-fixture journey'
        click('설정')
        click('새 일정 시작')
        click('새로 시작')
    wait('가는 길에, 무엇을 할까요?')
    original = draft(nodes())
    if args.mode in ('sdk', 'lock-ready'):
        click('음성으로 말하기')
        wait('듣고 있어요')
        assert find('이 내용 사용하기', nodes()).get('enabled') == 'false'
        shot('voice-sdk-listening.png')
        if args.mode == 'lock-ready':
            print('READY: actual SDK listening dialog for driving restriction test', flush=True)
            return
        click('취소')
        microphone_idle()
        assert draft(nodes()) == original
        click('음성으로 말하기')
        wait('듣고 있어요')
        # Pleos Home can leave the app visible in its multi-window workspace.
        # Open a different foreground Activity to exercise actual backgrounding.
        adb('shell', 'am', 'start', '--user', 'current', '-a', 'android.settings.SETTINGS')
        for _ in range(5):
            if not any(n.get('package') == 'ai.onmyway.car.demo' for n in nodes()):
                break
        else:
            raise AssertionError('Settings did not background the app')
        adb('shell', 'am', 'start', '--user', 'current', '-n', 'ai.onmyway.car.demo/ai.onmyway.car.MainActivity')
        wait('가는 길에, 무엇을 할까요?')
        microphone_idle()
        assert find('이 내용 사용하기', nodes()) is None, 'Background recognition dialog survived'
        assert draft(nodes()) == original
        click('음성으로 말하기')
        wait('음성 입력을 마치지 못했어요', timeout=40)
        assert find('이 내용 사용하기', nodes()).get('enabled') == 'false'
        assert find('다시 말하기', nodes()).get('enabled') == 'true'
        shot('voice-sdk-error.png')
        click('취소')
        microphone_idle()
        assert draft(nodes()) == original, 'Failed live STT replaced the original draft'
        print('PASS: actual SDK readiness; microphone released after cancel/background/timeout; original draft preserved; no fixture fallback', flush=True)
        return

    shot('voice-input.png')
    click('예시 음성 흐름 체험')
    wait('이렇게 말씀하셨나요?')
    wait('예시 문장 체험 · 실제 음성 인식 아님')
    shot('voice-example-review.png')
    click('취소')
    assert draft(nodes()) == original, 'Cancelled example changed input'
    click('예시 음성 흐름 체험')
    click('이 내용 사용하기')
    wait('예시 문장 · 실제 음성 인식·AI 해석 아님')
    wait('예시 할 일 확인하기')
    shot('voice-example-applied.png')
    click('예시 할 일 확인하기')
    wait('이렇게 들르고 갈까요?')
    click('‹ 뒤로')
    wait('할 일 확인하기')
    assert find('예시 할 일 확인하기', nodes()) is None, 'Stale example-speech provenance survived fixture load'
    assert find('예시 문장 · 실제 음성 인식·AI 해석 아님', nodes()) is None
    # Explicitly request the example again after verifying the provenance round trip.
    click('예시 음성 흐름 체험')
    click('이 내용 사용하기')
    click('예시 할 일 확인하기')
    wait('이렇게 들르고 갈까요?')
    shot('voice-example-errands.png')
    click('동선 확인하기')
    wait('다음은 이곳이에요')
    wait('18:40 / 19:00')
    shot('voice-example-route.png')
    for index in range(3):
        click('이곳으로 안내')
        click('목적지 승인')
        click('도착 체험 · 모의 이벤트')
        wait('할 일을 마쳤나요?')
        assert find('도착과 할 일 완료는 달라요.', nodes(), True) is not None
        click('할 일 완료')
        click('완료했어요')
        click('동선 확인하기')
        wait('다음은 이곳이에요' if index < 2 else '이제 집으로 가요')
    click('집으로 안내')
    click('목적지 승인')
    click('도착 체험 · 모의 이벤트')
    wait('집에 도착했어요')
    shot('voice-example-home.png')
    (out / 'voice-example-check.json').write_text(json.dumps({
        'result': 'pass', 'speech': 'explicit example transcript, not real STT',
        'parse': 'explicit fixture request, not live LLM', 'navigation': 'simulated',
        'checks': ['cancel preserves input', 'explicit transcript apply', 'fixture provenance round trip',
                   'three explicit errand completions', 'separate home approval']
    }, indent=2), encoding='utf-8')
    print('PASS: labelled example transcript -> confirmation -> fixture plan -> three explicit completions -> approved home', flush=True)


if __name__ == '__main__':
    main()

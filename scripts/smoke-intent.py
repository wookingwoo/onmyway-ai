"""Actual isolated Pleos UI: explicit prepared interpretation, answers and grounded plans.

No live speech/model result is claimed. Resets only labelled fixture journeys.
"""
import argparse
import json
from pathlib import Path
import re
import subprocess
import time
import urllib.request
import xml.etree.ElementTree as ET


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--adb', required=True)
    parser.add_argument('--serial', required=True)
    parser.add_argument('--mode', choices=['all', 'lock-ready', 'edits'], default='all')
    parser.add_argument('--output', default='artifacts/evidence/intent-demo')
    args = parser.parse_args()
    out = Path(args.output)
    out.mkdir(parents=True, exist_ok=True)

    def adb(*items):
        return subprocess.run([args.adb, '-s', args.serial, *items], check=True, capture_output=True, text=True, encoding='utf-8').stdout

    assert args.serial.startswith('emulator-') and 'OnMyWay_Pleos' in adb('emu', 'avd', 'name')

    def nodes():
        adb('shell', 'uiautomator', 'dump', '/sdcard/omw-intent.xml')
        return list(ET.fromstring(adb('shell', 'cat', '/sdcard/omw-intent.xml')).iter('node'))

    def find(label, tree, contains=False):
        return next((n for n in tree if (label in n.get('text', '') if contains else label == n.get('text', ''))), None)

    def bounds(node):
        return list(map(int, re.findall(r'\d+', node.get('bounds', ''))))

    def tap(node):
        assert node is not None and node.get('enabled') == 'true'
        x, y, right, bottom = bounds(node)
        adb('shell', 'input', 'tap', str((x + right) // 2), str((y + bottom) // 2))

    def scroll(tree, down=True):
        host = next((n for n in tree if n.get('scrollable') == 'true' and n.get('package') == 'ai.onmyway.car.demo'), None)
        if host is None: return False
        left, top, right, bottom = bounds(host)
        start, end = int(top + (bottom-top)*.8), int(top + (bottom-top)*.25)
        if not down: start, end = end, start
        adb('shell', 'input', 'swipe', str((left+right)//2), str(start), str((left+right)//2), str(end), '300')
        return True

    def wait(label, contains=False, timeout=16):
        until = time.monotonic() + timeout
        while time.monotonic() < until:
            tree = nodes()
            node = find(label, tree, contains)
            if node is not None: return node
            time.sleep(.2)
        raise AssertionError('Missing visible UI: ' + label)

    def click(label, contains=False):
        for down in [True]*7 + [False]*10:
            tree = nodes()
            node = find(label, tree, contains)
            if node is not None:
                tap(node)
                return
            if not scroll(tree, down): break
        tap(wait(label, contains))

    def shot(name):
        adb('shell', 'screencap', '-p', '/sdcard/omw-intent.png')
        adb('pull', '/sdcard/omw-intent.png', str(out / name))

    def reset_fixture():
        tree = nodes()
        if find('가는 길에, 무엇을 할까요?', tree) is not None: return
        assert find('예시 데이터', tree, True) is not None, 'Refuse to reset a live journey'
        click('설정'); click('새 일정 시작'); click('새로 시작')
        wait('가는 길에, 무엇을 할까요?')

    def restart():
        adb('shell', 'am', 'force-stop', 'ai.onmyway.car.demo')
        adb('shell', 'am', 'start', '--user', 'current', '-n', 'ai.onmyway.car.demo/ai.onmyway.car.MainActivity')

    # Pleos may use a full-screen OEM keyboard whose overlay is not in the app XML.
    adb('shell', 'input', 'keyevent', '4')
    if find('집 도착 시간', nodes()) is not None or find('예시 확인 질문', nodes()) is not None:
        click('취소')
    reset_fixture()
    if args.mode == 'edits':
        with urllib.request.urlopen('http://127.0.0.1:8000/health', timeout=3) as response:
            assert not json.load(response)['providers']['openai'], 'No provider calls are permitted by this check'
        click('질문과 답변 체험 · 예시 해석')
        wait('예시 요청 해석 · 준비된 내용')
        click('‹ 뒤로')
        wait('가는 길에, 무엇을 할까요?')
        editor = next(n for n in nodes() if n.get('class') == 'android.widget.EditText')
        tap(editor)
        adb('shell', 'input', 'keyevent', '123')
        adb('shell', 'input', 'text', 'x')
        adb('shell', 'input', 'keyevent', '4')
        click('할 일 확인하기')
        wait('실제 서비스가 아직 설정되지 않았습니다.', contains=True)
        assert find('가는 길에, 무엇을 할까요?', nodes()) is not None
        assert find('동선 확인하기', nodes()) is None
        shot('intent-no-provider.png')
        print('PASS: original edit invalidates review; explicit live parse returns missing-key error without fixture fallback', flush=True)
        return
    click('질문과 답변 체험 · 예시 해석')
    wait('예시 요청 해석 · 준비된 내용')
    assert wait('질문에 답하기').get('enabled') == 'true'
    assert find('동선 확인하기', nodes()) is None
    shot('intent-pending.png')
    click('질문에 답하기')
    wait('예시 확인 질문')
    shot('intent-question.png')
    if args.mode == 'lock-ready':
        print('READY: explicit example question dialog for actual driving lock', flush=True)
        return
    click('취소')
    assert wait('질문에 답하기').get('enabled') == 'true'
    assert find('동선 확인하기', nodes()) is None
    restart()
    wait('예시 요청 해석 · 준비된 내용')
    assert wait('질문에 답하기').get('enabled') == 'true'
    click('질문에 답하기'); click('18:30 전 수령 완료')
    assert wait('귀가 시간 정하기').get('enabled') == 'true'
    assert find('동선 확인하기', nodes()) is None, 'Missing home time must still block calculation'
    click('귀가 시간 정하기'); click('적용')
    assert wait('동선 확인하기').get('enabled') == 'true'
    restart()
    wait('동선 확인하기')
    assert find('질문에 답하기', nodes()) is None
    # Restore scroll to the interpretation card for unedited day/night evidence.
    for _ in range(4): scroll(nodes(), False)
    shot('intent-confirmed.png')
    adb('shell', 'cmd', 'uimode', 'night', 'yes')
    wait('예시 요청 해석 · 준비된 내용')
    shot('intent-night.png')
    adb('shell', 'cmd', 'uimode', 'night', 'no')
    click('동선 확인하기')
    wait('다음은 이곳이에요')
    if find('모의 안내 켜기', nodes()) is not None: click('모의 안내 켜기')
    shot('intent-plan.png')
    click('계획 근거 자세히')
    wait('계획의 근거와 한계')
    assert find('이동', nodes(), True) is not None
    shot('intent-explanation.png')
    click('닫기')
    for index in range(3):
        click('이곳으로 안내'); click('목적지 승인'); click('도착 체험 · 모의 이벤트')
        wait('할 일을 마쳤나요?')
        click('할 일 완료'); click('완료했어요'); click('동선 확인하기')
        wait('다음은 이곳이에요' if index < 2 else '이제 집으로 가요')
    click('집으로 안내'); click('목적지 승인'); click('도착 체험 · 모의 이벤트')
    wait('집에 도착했어요')
    shot('intent-home.png')

    reset_fixture()
    click('질문과 답변 체험 · 예시 해석')
    wait('이렇게 들르고 갈까요?')
    click('질문에 답하기'); click('18:30 전 수령 완료')
    click('귀가 시간 정하기')
    wait('집 도착 시간')
    pickers = [n for n in nodes() if n.get('class') == 'android.widget.NumberPicker']
    assert len(pickers) == 2
    # Native wheel input deliberately blocks text focus; operate its real +/- buttons.
    for picker, value, size in zip(pickers, [18, 20], [24, 60]):
        current = int(next(n.get('text') for n in picker.iter('node') if n.get('class') == 'android.widget.EditText'))
        forward, backward = (value-current) % size, (current-value) % size
        buttons = [n for n in picker.iter('node') if n.get('class') == 'android.widget.Button']
        assert len(buttons) == 2
        control = buttons[1] if forward <= backward else buttons[0]
        for _ in range(min(forward, backward)):
            tap(control)
            time.sleep(.35)
    actual = [int(n.get('text')) for n in nodes() if n.get('class') == 'android.widget.EditText']
    assert actual == [18, 20], actual
    click('적용')
    click('동선 확인하기')
    wait('시간 조정이 필요해요')
    shot('intent-tight.png')
    click('시간 조정 제안 보기')
    wait('귀가 시간을 바꿀까요?')
    proposal = next(re.search(r'(\d{2}:\d{2})까지', n.get('text', '')).group(1) for n in nodes() if re.search(r'(\d{2}:\d{2})까지', n.get('text', '')))
    shot('intent-adjustment.png')
    click('바꾸고 재계산')
    wait('다음은 이곳이에요')
    click('‹ 뒤로')
    wait('이렇게 들르고 갈까요?')
    assert find(proposal + '까지 귀가', nodes(), True) is not None, 'Approved extension must update interpreted conditions'
    shot('intent-adjusted-review.png')
    click('동선 확인하기')
    wait('다음은 이곳이에요')
    click('계획 근거 자세히')
    wait('계획의 근거와 한계')
    assert find('우유 구매', nodes(), True) is not None
    shot('intent-optional.png')
    click('닫기')
    (out / 'intent-check.json').write_text(json.dumps({'result':'pass', 'interpretation':'prepared example, not live STT/LLM',
        'navigation':'simulated', 'checks':['cancel leaves unanswered', 'pending survives restart', 'missing home blocks',
        'explicit answer and home selection', 'confirmed survives restart/theme', 'grounded explanation',
        'three separate completions', 'separate home approval', 'tight deadline explicit adjustment', 'optional exclusion named']}, indent=2), encoding='utf-8')
    print('PASS: actual native example clarification, restoration, full home journey and tight-deadline adjustment', flush=True)


if __name__ == '__main__':
    main()

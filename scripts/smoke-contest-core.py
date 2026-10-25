"""Actual isolated Pleos UI check; all errands/data/guidance are explicit examples.

Requires the current signed demo installed. Never resets a live journey or calls
live providers. Keeps captures/logs local; copy only reviewed authorized PNGs.
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
    p = argparse.ArgumentParser()
    p.add_argument("--adb", required=True)
    p.add_argument("--serial", required=True)
    p.add_argument("--port", type=int, default=8001)
    p.add_argument("--output", default="artifacts/evidence/contest-core")
    p.add_argument("--lock-ready", action="store_true")
    a = p.parse_args()
    out = Path(a.output)
    out.mkdir(parents=True, exist_ok=True)
    checks = []

    def adb(*args):
        return subprocess.run([a.adb, "-s", a.serial, *args], check=True,
                              capture_output=True, text=True, encoding="utf-8").stdout

    assert a.serial.startswith("emulator-") and adb("emu", "avd", "name").startswith("OnMyWay_Pleos")
    with urllib.request.urlopen(f"http://127.0.0.1:{a.port}/health", timeout=4) as r:
        assert not any(json.load(r)["providers"].values()), "This check requires all live providers disabled"

    def tree():
        adb("shell", "uiautomator", "dump", "/sdcard/omw-contest.xml")
        return ET.fromstring(adb("shell", "cat", "/sdcard/omw-contest.xml"))

    def find(label, root, contains=False):
        return next((n for n in root.iter("node") if
                     (label in n.get("text", "") if contains else label == n.get("text", ""))), None)

    def bounds(n):
        return list(map(int, re.findall(r"\d+", n.get("bounds", ""))))

    def tap(n):
        assert n is not None and n.get("enabled") == "true", "Missing or disabled action"
        x, y, right, bottom = bounds(n)
        assert right > x and bottom > y, "Action outside visible viewport"
        adb("shell", "input", "tap", str((x + right) // 2), str((y + bottom) // 2))

    def scroll(root, down):
        host = next((n for n in root.iter("node") if n.get("scrollable") == "true"
                     and n.get("package") == "ai.onmyway.car.demo"), None)
        if host is None:
            return False
        left, top, right, bottom = bounds(host)
        start, end = int(top + (bottom-top)*.8), int(top + (bottom-top)*.25)
        if not down:
            start, end = end, start
        adb("shell", "input", "swipe", str((left+right)//2), str(start), str((left+right)//2), str(end), "250")
        return True

    def wait(label, contains=False, timeout=20):
        until = time.monotonic() + timeout
        while time.monotonic() < until:
            n = find(label, tree(), contains)
            if n is not None:
                return n
            time.sleep(.2)
        raise AssertionError("Missing UI: " + label)

    def click(label, contains=False):
        for down in [True]*8 + [False]*12:
            root = tree()
            n = find(label, root, contains)
            if n is not None and len(bounds(n)) == 4 and bounds(n)[2] > bounds(n)[0]:
                tap(n)
                return
            if not scroll(root, down):
                break
        tap(wait(label, contains))

    def edit(label):
        for _ in range(9):
            root = tree()
            parents = {child: parent for parent in root.iter() for child in parent}
            node = find(label, root)
            while node is not None:
                action = find("수정", node)
                if action is not None and bounds(action)[2] > bounds(action)[0]:
                    tap(action)
                    return
                node = parents.get(node)
            assert scroll(root, True), "Cannot locate errand editor"
        raise AssertionError("Missing editor for " + label)

    def shot(name):
        adb("shell", "screencap", "-p", "/sdcard/omw-contest.png")
        adb("pull", "/sdcard/omw-contest.png", str(out / (name + ".png")))

    def reset():
        root = tree()
        if find("가는 길에, 무엇을 할까요?", root) is not None:
            return
        assert find("예시 데이터", root, True) is not None, "Refusing to reset a live journey"
        click("설정"); click("새 일정 시작"); click("새로 시작")
        wait("가는 길에, 무엇을 할까요?")

    # Configure only the app's server address through its native editor.
    click("설정"); click("연결 · 위치 고급 설정")
    editors = [n for n in tree().iter("node") if n.get("class") == "android.widget.EditText"]
    assert len(editors) == 6
    tap(editors[0])
    adb("shell", "input", "keycombination", "113", "29")
    adb("shell", "input", "text", f"http://10.0.2.2:{a.port}")
    adb("shell", "input", "keyevent", "4")
    click("닫기"); click("돌아가기")
    reset()
    click("예시로 체험 · 모의 안내")
    wait("이렇게 들르고 갈까요?")
    edit("세탁물 찾기")
    click("방문 장소 선택 · 맡긴 점포 확인")
    click("데모 세탁소")
    wait("방문 장소와 확인 근거")
    assert find("자동 교체하지 않습니다", tree(), True) is not None
    shot("venue-selected")
    click("닫기")
    wait("방문 장소 고정 · 데모 세탁소", True)
    shot("review-pinned")
    checks.append("explicit example venue pinned with pickup identity visible")
    click("동선 확인하기")
    wait("다음은 이곳이에요")
    for index in range(3):
        click("이곳으로 안내"); click("목적지 승인"); click("도착 체험 · 모의 이벤트")
        wait("할 일을 마쳤나요?")
        if index == 0:
            shot("arrival-not-completion")
        click("할 일 완료"); click("완료했어요")
        wait("다음은 이곳이에요" if index < 2 else "이제 집으로 가요")
        assert find("동선 확인하기", tree()) is None, "Completion should automatically refresh the preview"
        if index == 0:
            assert find("데모 세탁소", tree(), True) is not None
            shot("automatic-next-preview")
    click("집으로 안내"); click("목적지 승인"); click("도착 체험 · 모의 이벤트")
    wait("집에 도착했어요")
    shot("home")
    checks += ["arrival stays incomplete", "three explicit completions auto-refresh previews", "separately approved simulated home"]
    reset()
    click("마감·미확인 정보 체험 · 예시")
    click("동선 확인하기")
    wait("시간 조정이 필요해요")
    click("시간 조정 제안 보기")
    wait("미확인 정보가 있는 시간 제안")
    assert find("확정할 수 없어요", tree(), True) is not None
    shot("conditional-proposal")
    click("유지")
    wait("시간 조정이 필요해요")
    click("시간 조정 제안 보기"); click("바꾸고 재계산")
    wait("다음은 이곳이에요")
    assert find("가능 여부가 확정되지 않았습니다", tree(), True) is not None
    assert find("여유 0분", tree(), True) is not None
    shot("low-margin-uncertainty")
    click("계획 근거 자세히")
    wait("계획의 근거와 한계")
    assert find("우유 구매", tree(), True) is not None
    shot("grounded-explanation")
    click("닫기")
    checks += ["independently selected uncertain fixture stays labelled", "conditional deadline preserves uncertainty", "zero margin caution and optional exclusion visible"]
    try:
        adb("shell", "cmd", "uimode", "night", "yes")
        time.sleep(1)
        wait("다음은 이곳이에요")
        shot("night")
        checks.append("actual system night mode")
    finally:
        adb("shell", "cmd", "uimode", "night", "no")
    if a.lock_ready:
        click("‹ 뒤로")
        wait("이렇게 들르고 갈까요?")
        edit("약 구매")
        click("방문 장소 선택 · 맡긴 점포 확인")
        wait("방문 장소 선택")
        print("READY: actual venue dialog open for driving-lock verification", flush=True)
    (out / "contest-check.json").write_text(json.dumps({
        "result": "pass", "scope": "actual native UI; synthetic examples and simulated guidance only",
        "checks": checks, "not_verified": ["live STT/LLM/Kakao", "actual navigation acceptance/arrival", "physical vehicle", "user benefit"]
    }, ensure_ascii=False, indent=2), encoding="utf-8")
    print("PASS: " + "; ".join(checks), flush=True)


if __name__ == "__main__":
    main()

"""Exercise the labelled demo on an explicitly selected Android test emulator.

Uses actual accessibility trees and actual touch input; no synthetic screenshots.
Requires the demo APK, running API, and a parked/unknown (not restricted) device.
"""
import argparse
from pathlib import Path
import re
import subprocess
import time
import xml.etree.ElementTree as ET


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--adb", required=True)
    parser.add_argument("--serial", required=True)
    parser.add_argument("--output", default="artifacts/evidence")
    parser.add_argument("--complete-first", action="store_true")
    parser.add_argument("--pause", type=float, default=0)
    args = parser.parse_args()
    output = Path(args.output)
    output.mkdir(parents=True, exist_ok=True)

    def adb(*items):
        return subprocess.run([args.adb, "-s", args.serial, *items], check=True,
                              capture_output=True, text=True, encoding="utf-8").stdout

    def nodes():
        adb("shell", "uiautomator", "dump", "/sdcard/omw-smoke.xml")
        return list(ET.fromstring(adb("shell", "cat", "/sdcard/omw-smoke.xml")).iter("node"))

    def find(label, tree):
        return next((n for n in tree if label in n.get("text", "")), None)

    def tap(node):
        assert node is not None, "Expected visible UI element was missing"
        assert node.get("enabled") == "true", f"Control disabled: {node.get('text')}"
        x1, y1, x2, y2 = map(int, re.findall(r"\d+", node.get("bounds", "")))
        adb("shell", "input", "tap", str((x1+x2)//2), str((y1+y2)//2))

    def screenshot(name):
        adb("shell", "screencap", "-p", "/sdcard/omw-smoke.png")
        adb("pull", "/sdcard/omw-smoke.png", str(output / name))

    def click_label(label, pane=1):
        for _ in range(8):
            tree = nodes()
            node = find(label, tree)
            if node is not None:
                tap(node)
                time.sleep(args.pause)
                return
            panes = [n for n in tree if n.get("class") == "android.widget.ScrollView"]
            assert len(panes) > pane, f"Missing control: {label}"
            x1, y1, x2, y2 = map(int, re.findall(r"\d+", panes[pane].get("bounds", "")))
            adb("shell", "input", "swipe", str((x1+x2)//2), str(y2-80), str((x1+x2)//2), str(y1+80), "350")
        raise AssertionError(f"Control not reachable: {label}")

    tree = nodes()
    switch = next((n for n in tree if n.get("class") == "android.widget.Switch"), None)
    assert switch is not None, "This smoke requires the explicitly labelled demo APK"
    if switch.get("checked") != "true":
        tap(switch)
    tree = nodes()
    fixture = find("예시 시나리오 불러오기", tree)
    assert fixture is not None, "Start the app at its initial screen before this smoke"
    tap(fixture)
    tree = nodes()
    assert find("FIXTURE", tree) is not None, "Fixture provenance must remain visible"
    for _ in range(10):
        calculate = find("남은 일정으로 동선 계산", tree)
        if calculate is not None:
            break
        # The pane dimensions come from the native app's actual ScrollView tree.
        panes = [n for n in tree if n.get("class") == "android.widget.ScrollView"]
        assert panes, "Expected workflow scroll pane"
        x1, y1, x2, y2 = map(int, re.findall(r"\d+", panes[0].get("bounds", "")))
        adb("shell", "input", "swipe", str((x1+x2)//2), str(y2-80),
            str((x1+x2)//2), str(y1+80), "350")
        tree = nodes()
    else:
        raise AssertionError("Plan action not reachable through workflow scroll")
    tap(calculate)
    tree = nodes()
    assert find("예시 조건에서 시간 충족", tree) is not None, "Fixture plan did not complete"
    screenshot("pleos-demo-plan.png")
    (output / "demo-plan-ui.xml").write_text(adb("shell", "cat", "/sdcard/omw-smoke.xml"), encoding="utf-8")
    print("PASS: installed demo -> explicit simulation -> HTTP fixture -> native route timeline")
    if args.complete_first:
        time.sleep(args.pause)
        click_label("다음 목적지 확인")
        click_label("목적지 승인")
        click_label("시뮬레이션 · 도착 이벤트 보내기")
        tree = nodes()
        assert find("할 일은 아직 완료되지 않았습니다", tree) is not None
        screenshot("pleos-demo-arrival-not-complete.png")
        click_label("이 할 일을 직접 완료 확인")
        click_label("완료했어요")
        click_label("남은 일정으로 동선 계산", pane=0)
        tree = nodes()
        assert find("01  세탁물 찾기", tree) is not None, "Completed errand must be omitted from remaining plan"
        screenshot("pleos-demo-replanned.png")
        print("PASS: approved simulation -> arrival is not completion -> explicit completion -> remaining plan")


if __name__ == "__main__":
    main()

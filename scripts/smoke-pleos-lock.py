"""Verify real Pleos UX restriction changes using its local Virtual Console.

Run only on an isolated test AVD. Always restores speed=0 / Park and closes its
own browser session. Endpoint names are read from the official console page.
"""
import argparse
import json
from pathlib import Path
import subprocess
import threading
import time
import urllib.request
import xml.etree.ElementTree as ET


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--adb", required=True)
    parser.add_argument("--serial", required=True)
    parser.add_argument("--output", default="artifacts/evidence")
    args = parser.parse_args()
    output = Path(args.output)
    output.mkdir(parents=True, exist_ok=True)
    stop = threading.Event()
    base = "http://127.0.0.1:8088"
    session = "/browser/session?client=onmyway-lock-verification"

    def request(path, method="GET"):
        with urllib.request.urlopen(urllib.request.Request(base+path, method=method), timeout=5) as response:
            return json.load(response)

    def heartbeat():
        while not stop.is_set():
            request(session, "POST")
            stop.wait(.5)

    def adb(*items):
        return subprocess.run([args.adb, "-s", args.serial, *items], check=True,
                              capture_output=True, text=True, encoding="utf-8").stdout

    request(session, "POST")
    thread = threading.Thread(target=heartbeat, daemon=True)
    thread.start()
    evidence = {}
    try:
        request("/vehicle/gear?gear=Drive", "PUT")
        request("/vehicle/controls?speed=30&accelerator=0&brake=0&steering=0", "PUT")
        time.sleep(2.5)
        state = request("/vehicle/state")
        evidence["gear"] = state["gear"]["actual"]["value"]
        evidence["speed_kph"] = state["controls"]["speed_kph"]
        adb("shell", "uiautomator", "dump", "/sdcard/omw-driving.xml")
        xml = adb("shell", "cat", "/sdcard/omw-driving.xml")
        (output / "pleos-driving-ui.xml").write_text(xml, encoding="utf-8")
        tree = list(ET.fromstring(xml).iter("node"))
        labels = [n.get("text", "") for n in tree]
        evidence["restriction_labels"] = [t for t in labels if "제한 중" in t or "잠금" in t or "주행" in t]
        unsafe_labels = {"이곳으로 안내", "집으로 안내", "목적지 승인", "저장", "할 일 확인하기", "동선 확인하기",
                         "음성으로 말하기", "예시 음성 흐름 체험", "이 내용 사용하기", "다시 말하기",
                         "질문에 답하기", "답변 반영하기", "18:30 전 수령 완료", "귀가 시간 정하기",
                         "방문할 장소 선택", "확인 근거 저장", "다른 장소 선택", "최신 시간으로 다시 계산",
                         "취소하고 다시 계산", "차량 내비 다시 연결"}
        evidence["enabled_destination_controls"] = [n.get("text") for n in tree
            if n.get("text", "") in unsafe_labels and n.get("enabled") == "true"]
        adb("shell", "screencap", "-p", "/sdcard/omw-driving.png")
        adb("pull", "/sdcard/omw-driving.png", str(output / "pleos-driving-lock.png"))
        assert evidence["gear"] == "Drive" and evidence["speed_kph"] == 30
        assert evidence["restriction_labels"], "Actual restriction/OS block must be visible"
        assert not evidence["enabled_destination_controls"], "Navigation approval must be disabled while restricted"
        assert "듣고 있어요" not in labels, "Voice session must be dismissed while driving"
        assert not any(n.get("class") == "android.widget.EditText" and n.get("enabled") == "true" for n in tree), "Editable dialogs must close or lock while driving"
        evidence["result"] = "pass"
        print(json.dumps(evidence, ensure_ascii=True))
    finally:
        request("/vehicle/controls?speed=0&accelerator=0&brake=0&steering=0", "PUT")
        request("/vehicle/gear?gear=Park", "PUT")
        parked = request("/vehicle/state")
        evidence["restored_gear"] = parked["gear"]["actual"]["value"]
        evidence["restored_speed_kph"] = parked["controls"]["speed_kph"]
        stop.set()
        thread.join(timeout=2)
        request(session+"&closing=1", "POST")
        (output / "pleos-driving-check.json").write_text(json.dumps(evidence, ensure_ascii=False, indent=2), encoding="utf-8")


if __name__ == "__main__":
    main()

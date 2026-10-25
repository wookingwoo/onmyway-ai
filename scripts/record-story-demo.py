"""Capture one explicit synthetic journey on an isolated official Pleos AVD.

No provider calls, app data clearing, fabricated UI, or real navigation. Raw media
and XML stay local. Every recording owns its PID and fails on early termination.
"""
import argparse
from contextlib import contextmanager
from hashlib import sha256
import json
from pathlib import Path
import re
import subprocess
import threading
import time
import urllib.request
import xml.etree.ElementTree as ET


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--adb", required=True)
    parser.add_argument("--serial", default="emulator-5556")
    parser.add_argument("--port", type=int, default=8001)
    parser.add_argument("--output", default="artifacts/evidence/story-demo")
    parser.add_argument("--resume-driving", action="store_true", help="Keep verified first five chapters; require the same pending synthetic laundry journey")
    args = parser.parse_args()
    out = Path(args.output).resolve()
    out.mkdir(parents=True, exist_ok=True)
    shots = out / "captures"
    shots.mkdir(exist_ok=True)
    records = []
    active = None
    stop = threading.Event()
    console = "http://127.0.0.1:8088"
    session = "/browser/session?client=onmyway-story-recording"

    def request(path, method="GET"):
        with urllib.request.urlopen(urllib.request.Request(console + path, method=method), timeout=5) as r:
            return json.load(r)

    def adb(*items, check=True, binary=False):
        p = subprocess.run([args.adb, "-s", args.serial, *map(str, items)], check=check,
                           capture_output=True, **({} if binary else {"text": True, "encoding": "utf-8"}))
        return p.stdout

    assert args.serial.startswith("emulator-")
    assert adb("emu", "avd", "name").startswith("OnMyWay_Pleos")
    with urllib.request.urlopen(f"http://127.0.0.1:{args.port}/health", timeout=5) as r:
        assert not any(json.load(r)["providers"].values()), "Recording requires all live providers disabled"
    assert not adb("shell", "pidof", "screenrecord", check=False).strip(), "Another capture is active"

    def heartbeat():
        while not stop.wait(.5):
            try:
                request(session, "POST")
            except OSError:
                stop.set()

    def tree():
        adb("shell", "uiautomator", "dump", "/sdcard/omw-story.xml")
        return ET.fromstring(adb("shell", "cat", "/sdcard/omw-story.xml"))

    def find(label, root, contains=False):
        return next((n for n in root.iter("node") if
                     (label in n.get("text", "") if contains else n.get("text") == label)), None)

    def bounds(node):
        return list(map(int, re.findall(r"\d+", node.get("bounds", ""))))

    def tap(node):
        assert node is not None and node.get("enabled") == "true", "Missing/disabled native action"
        x, y, right, bottom = bounds(node)
        assert right > x and bottom > y
        adb("shell", "input", "tap", (x + right) // 2, (y + bottom) // 2)

    def scroll(root, down=True):
        host = next((n for n in root.iter("node") if n.get("scrollable") == "true"
                     and n.get("package") == "ai.onmyway.car.demo"), None)
        if host is None:
            return False
        left, top, right, bottom = bounds(host)
        a, b = int(top + (bottom-top)*.8), int(top + (bottom-top)*.25)
        if not down:
            a, b = b, a
        adb("shell", "input", "swipe", (left+right)//2, a, (left+right)//2, b, 250)
        return True

    def wait(label, contains=False):
        deadline = time.monotonic() + 22
        while time.monotonic() < deadline:
            root = tree()
            node = find(label, root, contains)
            if node is not None:
                return root
            time.sleep(.2)
        raise AssertionError("Missing native UI: " + label)

    def click(label, contains=False):
        for down in [True]*8 + [False]*12:
            root = tree()
            node = find(label, root, contains)
            if node is not None and bounds(node)[2] > bounds(node)[0]:
                tap(node)
                return
            if not scroll(root, down):
                break
        tap(find(label, wait(label, contains), contains))

    def edit(label):
        for _ in range(10):
            root = tree()
            parents = {child: parent for parent in root.iter() for child in parent}
            node = find(label, root)
            while node is not None:
                action = find("수정", node)
                if action is not None and bounds(action)[2] > bounds(action)[0]:
                    tap(action)
                    return
                node = parents.get(node)
            assert scroll(root), "Missing native errand editor"
        raise AssertionError("Missing editor: " + label)

    def shot(name, expected, contains=False):
        root = wait(expected, contains)
        (shots / (name + ".xml")).write_text(ET.tostring(root, encoding="unicode"), encoding="utf-8")
        (shots / (name + ".png")).write_bytes(adb("exec-out", "screencap", "-p", binary=True))
        if active is not None:
            active["beats"].append({"name": name, "at": round(time.monotonic()-active["started"], 3), "expected": expected})
        time.sleep(1.2)
        print("CAPTURE: " + name, flush=True)
        return root

    @contextmanager
    def recording(name):
        nonlocal active
        assert re.fullmatch(r"[a-z-]+", name)
        remote = f"/sdcard/omw-story-{name}.mp4"
        command = [args.adb, "-s", args.serial, "shell",
                   f"screenrecord --size 1920x1080 --bit-rate 8000000 --time-limit 180 {remote} & recorder=$!; echo $recorder; wait $recorder"]
        process = subprocess.Popen(command, stdout=subprocess.PIPE, stderr=subprocess.PIPE,
                                   text=True, encoding="utf-8", creationflags=getattr(subprocess, "CREATE_NO_WINDOW", 0))
        pid = process.stdout.readline().strip()
        assert pid.isdigit()
        active = {"id": name, "raw": f"raw/{name}.mp4", "started": time.monotonic(), "beats": []}
        time.sleep(.7)
        try:
            yield
        finally:
            elapsed = time.monotonic()-active["started"]
            early = process.poll() is not None
            if not early:
                adb("shell", "kill", "-2", pid, check=False)
            process.communicate(timeout=18)
            target = out / active["raw"]
            target.parent.mkdir(exist_ok=True)
            adb("pull", remote, target)
            assert not early and elapsed < 178, "Truncated recording cannot be used"
            active["capture_seconds"] = round(elapsed, 3)
            active["sha256"] = sha256(target.read_bytes()).hexdigest()
            del active["started"]
            records.append(active)
            active = None
            (out / "capture-partial.json").write_text(json.dumps(records, ensure_ascii=False, indent=2), encoding="utf-8")

    request(session, "POST")
    thread = threading.Thread(target=heartbeat, daemon=True)
    thread.start()
    try:
        request("/vehicle/controls?speed=0&accelerator=0&brake=0&steering=0", "PUT")
        request("/vehicle/gear?gear=Park", "PUT")
        time.sleep(2)
        adb("shell", "am", "start", "--user", "current", "-n", "ai.onmyway.car.demo/ai.onmyway.car.MainActivity")
        root = tree()
        if args.resume_driving:
            previous = json.loads((out / "capture-partial.json").read_text(encoding="utf-8"))
            required = ["clarify", "venue", "baseline", "change", "decision"]
            records.extend(record for record in previous if record["id"] in required)
            assert [record["id"] for record in records] == required
            root = wait("도착 체험 · 모의 이벤트")
            assert find("예시 데이터", root, True) is not None
            assert find("데모 세탁소", root) is not None and find("18:53 / 19:00", root) is not None
        else:
            if find("모의 안내 켜기", root) is not None:
                click("모의 안내 켜기")
            click("설정"); click("연결 · 위치 고급 설정")
            editors = [n for n in tree().iter("node") if n.get("class") == "android.widget.EditText"]
            assert len(editors) == 6
            tap(editors[0]); adb("shell", "input", "keycombination", "113", "29")
            adb("shell", "input", "text", f"http://10.0.2.2:{args.port}")
            adb("shell", "input", "keyevent", "4")
            click("닫기"); click("돌아가기")
            root = tree()
            if find("가는 길에, 무엇을 할까요?", root) is None:
                assert find("예시 데이터", root, True) is not None, "Refusing to reset a live journey"
                click("설정"); click("새 일정 시작"); click("새로 시작")
            with recording("clarify"):
                click("질문과 답변 체험 · 예시 해석")
                shot("request", "예시 요청 해석 · 준비된 내용")
                click("질문에 답하기")
                shot("clarification", "예시 확인 질문")
                click("18:30 전 수령 완료")
                click("귀가 시간 정하기")
                shot("home-deadline", "집 도착 시간")
                click("적용")
                shot("confirmed", "동선 확인하기")
            with recording("venue"):
                edit("세탁물 찾기")
                click("방문 장소 선택 · 맡긴 점포 확인")
                shot("venue-choices", "방문 장소 선택")
                click("데모 세탁소")
                shot("venue-pinned", "방문 장소와 확인 근거")
                click("닫기")
                shot("review-pinned", "방문 장소 고정 · 데모 세탁소", True)
            with recording("baseline"):
                click("동선 확인하기")
                root = shot("baseline-plan", "다음은 이곳이에요")
                assert find("18:40 / 19:00", root) is not None
            with recording("change"):
                click("‹ 뒤로")
                edit("약 구매")
                for _ in range(4):
                    click("+ 5분")
                shot("consultation-thirty", "30분")
                click("저장")
                click("동선 확인하기")
                root = shot("revised-plan", "다음은 이곳이에요")
                assert find("세탁물 찾기", root) is not None
                assert find("데모 세탁소", root) is not None
                assert find("18:53 / 19:00", root) is not None
                assert find("포함되지 않은 할 일 1개", root, True) is not None
            with recording("decision"):
                click("계획 근거 자세히")
                shot("revised-explanation", "계획의 근거와 한계")
                click("닫기")
                click("전체 일정 · 확인할 정보")
                shot("revised-details", "일정과 데이터 확인")
                click("닫기")
                click("이곳으로 안내")
                shot("laundry-approval", "목적지 승인")
                click("목적지 승인")
        with recording("driving"):
            request("/vehicle/gear?gear=Drive", "PUT")
            request("/vehicle/controls?speed=30&accelerator=0&brake=0&steering=0", "PUT")
            time.sleep(2.5)
            state = request("/vehicle/state")
            assert state["gear"]["actual"]["value"] == "Drive" and state["controls"]["speed_kph"] == 30
            root = shot("driving-lock", "정차 후 이용해 주세요")
            assert not any(n.get("class") == "android.widget.EditText" and n.get("enabled") == "true" for n in root.iter("node"))
            assert not any(n.get("text") in {"이곳으로 안내", "집으로 안내", "목적지 승인", "할 일 완료"}
                           and n.get("enabled") == "true" for n in root.iter("node"))
            request("/vehicle/controls?speed=0&accelerator=0&brake=0&steering=0", "PUT")
            request("/vehicle/gear?gear=Park", "PUT")
            time.sleep(2)
        with recording("laundry"):
            click("도착 체험 · 모의 이벤트")
            shot("laundry-arrival", "할 일을 마쳤나요?")
            click("할 일 완료")
            shot("laundry-completion", "완료했어요")
            click("완료했어요")
            root = shot("automatic-next", "다음은 이곳이에요")
            assert find("약 구매", root) is not None and find("18:53 / 19:00", root) is not None
        with recording("medicine"):
            click("이곳으로 안내"); click("목적지 승인")
            click("도착 체험 · 모의 이벤트")
            shot("medicine-arrival", "할 일을 마쳤나요?")
            click("할 일 완료"); click("완료했어요")
            shot("home-preview", "이제 집으로 가요")
        with recording("home"):
            click("집으로 안내")
            shot("home-approval", "목적지 승인")
            click("목적지 승인"); click("도착 체험 · 모의 이벤트")
            shot("home-arrival", "집에 도착했어요")
        state = request("/vehicle/state")
        assert state["gear"]["actual"]["value"] == "Park" and state["controls"]["speed_kph"] == 0
        manifest = {"result": "pass", "scope": "actual native UI; explicit synthetic example; simulated guidance/arrival; actual AVD driving restriction",
                    "scenario": {"departure": "18:00", "deadline": "19:00", "pickup_deadline": "18:30", "baseline_home": "18:40",
                                 "pharmacy_minutes": 30, "revised_order": ["laundry", "medicine"], "optional_omission": "groceries", "revised_home": "18:53"},
                    "restored_gear": "Park", "restored_speed_kph": 0, "chapters": records}
        (out / "capture-manifest.json").write_text(json.dumps(manifest, ensure_ascii=False, indent=2), encoding="utf-8")
        print("PASS: one corrected-duration journey, mandatory laundry/medicine, optional milk omitted, 18:53 home, actual Drive lock and Park restore", flush=True)
    finally:
        request("/vehicle/controls?speed=0&accelerator=0&brake=0&steering=0", "PUT")
        request("/vehicle/gear?gear=Park", "PUT")
        stop.set(); thread.join(timeout=2)
        request(session + "&closing=1", "POST")


if __name__ == "__main__":
    main()

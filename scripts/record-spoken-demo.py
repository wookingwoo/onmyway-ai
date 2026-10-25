"""Capture one explicit synthetic journey on an isolated official Pleos AVD.

No provider calls, app data clearing, fabricated UI, or real navigation. Raw media
and XML stay local. Every recording owns its PID and fails on early termination.
"""
import argparse
from contextlib import contextmanager
from native_demo_audio import EmulatorAudio
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
    parser.add_argument("--port", type=int, default=8002)
    parser.add_argument("--output", default="artifacts/evidence/spoken-plan")
    parser.add_argument("--emulator-sdk", required=True)
    args = parser.parse_args()
    out = Path(args.output).resolve()
    out.mkdir(parents=True, exist_ok=True)
    shots = out / "captures"
    shots.mkdir(exist_ok=True)
    records = []
    active = None
    stop = threading.Event()
    console = "http://127.0.0.1:8088"
    session = "/browser/session?client=onmyway-spoken-recording"

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
        audio = EmulatorAudio(Path(args.emulator_sdk), args.serial)
        command = [args.adb, "-s", args.serial, "shell",
                   f"screenrecord --size 1920x1080 --bit-rate 8000000 --time-limit 180 {remote} & recorder=$!; echo $recorder; wait $recorder"]
        process = subprocess.Popen(command, stdout=subprocess.PIPE, stderr=subprocess.PIPE,
                                   text=True, encoding="utf-8", creationflags=getattr(subprocess, "CREATE_NO_WINDOW", 0))
        pid = process.stdout.readline().strip()
        wall_started = time.time()
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
            active["native_audio"] = f"audio/{name}-native.wav"
            active["speaker_capture"] = audio.finish(out / active["native_audio"], wall_started, elapsed)
            active["capture_seconds"] = round(elapsed, 3)
            active["sha256"] = sha256(target.read_bytes()).hexdigest()
            del active["started"]
            records.append(active)
            active = None
            (out / "capture-partial.json").write_text(json.dumps(records, ensure_ascii=False, indent=2), encoding="utf-8")

    def mark(name):
        active["beats"].append({"name": name, "at": round(time.monotonic()-active["started"], 3)})

    def prepared(name):
        path = out / "audio" / (name + ".mp3")
        assert path.is_file(), "Prepared dialogue audio must exist before capture"
        info = json.loads(subprocess.check_output(["ffprobe", "-v", "error", "-show_format", "-of", "json", str(path)]))
        seconds = float(info["format"]["duration"])
        active.setdefault("prepared", []).append({"id": name, "at": round(time.monotonic()-active["started"], 3), "seconds": seconds,
            "sha256": sha256(path.read_bytes()).hexdigest()})
        time.sleep(seconds + .5)

    def speech_done(previous):
        deadline = time.monotonic()+95
        started = False
        while time.monotonic() < deadline:
            log = adb("logcat", "-d", "-v", "brief", "-s", "OnMyWayVoice:I")
            delta = log[len(previous):] if log.startswith(previous) else log
            assert "tts-failed" not in delta, "Actual native TTS failed"
            if "tts-start" in delta and not started:
                mark("native-speech-start"); started = True
            if "tts-done" in delta:
                assert started
                mark("native-speech-done")
                time.sleep(.8)
                return
            time.sleep(.25)
        raise AssertionError("Native plan speech did not complete")

    def voice_approve():
        root = tree()
        if find("말 전달하기", root) is None:
            click("말로 대답하기")
        shot("spoken-approval", "말 전달하기")
        prepared("approval")
        before = adb("logcat", "-d", "-v", "brief", "-s", "OnMyWayVoice:I")
        mark("approval-submitted"); click("말 전달하기")
        shot("accepted", "안내 중 · 차량 내비게이션을 확인해 주세요")
        after = adb("logcat", "-d", "-v", "brief", "-s", "OnMyWayVoice:I")
        assert "voice-approved revision=" in after[len(before):], "Speech must enter the real guarded approval handler"

    request(session, "POST")
    thread = threading.Thread(target=heartbeat, daemon=True); thread.start()
    try:
        request("/vehicle/controls?speed=0&accelerator=0&brake=0&steering=0", "PUT")
        request("/vehicle/gear?gear=Park", "PUT"); time.sleep(2)
        adb("shell", "am", "start", "--user", "current", "-n", "ai.onmyway.car.demo/ai.onmyway.car.MainActivity")
        root = tree()
        if find("취소", root) is not None: click("취소")
        root = tree()
        click("설정")
        if find("새 일정 시작", tree()) is not None:
            assert find("샘플 데이터", root, True) is not None, "Refusing to reset a live journey"
            click("새 일정 시작"); click("새로 시작"); click("설정")
        click("연결 · 위치 고급 설정")
        editors = [n for n in tree().iter("node") if n.get("class") == "android.widget.EditText"]
        assert len(editors)==6
        tap(editors[0]); adb("shell", "input", "keycombination", "113", "29")
        adb("shell", "input", "text", f"http://10.0.2.2:{args.port}"); adb("shell", "input", "keyevent", "4")
        click("닫기")
        if find("샘플로 둘러보기", tree()) is not None: click("샘플로 둘러보기")
        click("돌아가기")
        assert find("샘플 데이터", tree(), True) is not None
        with recording("request"):
            shot("voice-home", "가는 길에, 무엇을 할까요?")
            click("말로 부탁하기"); shot("voice-request", "계획 준비하기")
            prepared("request")
            before=adb("logcat", "-d", "-v", "brief", "-s", "OnMyWayVoice:I")
            mark("request-submitted"); click("계획 준비하기")
            root=shot("initial-plan", "이렇게 들르고 갈까요?")
            assert find("18:40 / 19:00",root) is not None
            speech_done(before)
            shot("automatic-reply", "말 전달하기")
        with recording("refinement"):
            shot("spoken-refinement", "말 전달하기"); prepared("refinement")
            before=adb("logcat", "-d", "-v", "brief", "-s", "OnMyWayVoice:I")
            mark("refinement-submitted"); click("말 전달하기")
            root=shot("revised-plan", "이렇게 들르고 갈까요?")
            assert find("18:53 / 19:00",root) is not None and find("우유 구매는 다음에",root,True) is not None
            speech_done(before); shot("ready-to-approve", "말 전달하기")
        with recording("departure"):
            voice_approve()
            request("/vehicle/gear?gear=Drive","PUT")
            request("/vehicle/controls?speed=30&accelerator=0&brake=0&steering=0","PUT"); time.sleep(2)
            root=shot("driving-lock", "정차 후 이용해 주세요")
            assert not any(n.get("text") in {"말로 대답하기","확인하고 출발","완료했어요"} and n.get("enabled")=="true" for n in root.iter("node"))
            request("/vehicle/controls?speed=0&accelerator=0&brake=0&steering=0","PUT")
            request("/vehicle/gear?gear=Park","PUT");time.sleep(2)
        with recording("remaining"):
            click("설정");click("샘플 안내 · 도착 신호");shot("laundry-arrival","완료했어요")
            before=adb("logcat","-d","-v","brief","-s","OnMyWayVoice:I")
            mark("laundry-completed");click("완료했어요");shot("remaining-plan","다음은 이곳이에요")
            speech_done(before)
            assert find("말 전달하기",tree()) is None, "Touch completion cannot silently restart the microphone"
            voice_approve()
        with recording("home"):
            click("설정");click("샘플 안내 · 도착 신호");shot("medicine-arrival","완료했어요")
            before=adb("logcat","-d","-v","brief","-s","OnMyWayVoice:I")
            mark("medicine-completed");click("완료했어요");shot("home-plan","이제 집으로 가요")
            speech_done(before);voice_approve()
            click("설정");click("샘플 안내 · 도착 신호");shot("home-arrival","집에 도착했어요")
        state=request("/vehicle/state")
        assert state["gear"]["actual"]["value"]=="Park" and state["controls"]["speed_kph"]==0
        manifest={"result":"pass","scope":"Actual native Pleos ON_DEVICE speech and app UI; independently selected saved sample inputs; simulated navigation/arrival; actual AVD Drive restriction",
                  "restored_gear":"Park","restored_speed_kph":0,"chapters":records}
        (out/"capture-manifest.json").write_text(json.dumps(manifest,ensure_ascii=False,indent=2),encoding="utf-8")
        print("PASS: actual spoken explanation, saved refinement/guarded departure, remaining plan and separate home approval",flush=True)
    finally:
        request("/vehicle/controls?speed=0&accelerator=0&brake=0&steering=0","PUT")
        request("/vehicle/gear?gear=Park","PUT")
        stop.set();thread.join(timeout=2);request(session+"&closing=1","POST")


if __name__ == "__main__":
    main()

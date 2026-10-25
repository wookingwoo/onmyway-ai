"""Actual staged-UI smoke on the isolated Pleos emulator; no real navigation."""
import argparse
from pathlib import Path
import re
import subprocess
import time
import xml.etree.ElementTree as ET

def main():
    p = argparse.ArgumentParser()
    p.add_argument("--adb", required=True)
    p.add_argument("--serial", required=True)
    p.add_argument("--output", default="artifacts/evidence")
    p.add_argument("--complete-first", action="store_true")
    p.add_argument("--full-journey", action="store_true")
    p.add_argument("--boundaries-only", action="store_true")
    p.add_argument("--pause", type=float, default=0)
    a = p.parse_args()
    assert a.serial.startswith("emulator-"), "Use an isolated test emulator"
    out = Path(a.output); out.mkdir(parents=True, exist_ok=True)
    def adb(*items):
        return subprocess.run([a.adb, "-s", a.serial, *items], check=True,
            capture_output=True, text=True, encoding="utf-8").stdout
    assert "OnMyWay_Pleos" in adb("emu", "avd", "name")
    def nodes():
        adb("shell", "uiautomator", "dump", "/sdcard/omw-smoke.xml")
        return list(ET.fromstring(adb("shell", "cat", "/sdcard/omw-smoke.xml")).iter("node"))
    def find(label, tree, contains=False):
        return next((n for n in tree if (label in n.get("text", "") if contains else label == n.get("text", ""))), None)
    def wait(label, contains=False):
        for _ in range(8):
            n = find(label, nodes(), contains)
            if n is not None: return n
            time.sleep(.2)
        raise AssertionError(f"Missing visible UI element: {label}")
    def tap(n):
        assert n is not None and n.get("enabled") == "true", "Absent or disabled control"
        x1,y1,x2,y2 = map(int, re.findall(r"\d+", n.get("bounds", "")))
        assert x2>x1 and y2>y1
        adb("shell","input","tap",str((x1+x2)//2),str((y1+y2)//2))
        time.sleep(a.pause)
    def click(label): tap(wait(label))
    def shot(name):
        adb("shell","screencap","-p","/sdcard/omw-smoke.png")
        adb("pull","/sdcard/omw-smoke.png",str(out/name))

    tree = nodes()
    assert any(n.get("package") == "ai.onmyway.car.demo" for n in tree)
    if a.boundaries_only:
        assert find("예시 데이터", tree, True) is not None, "Start from a labelled fixture session"
        click("설정"); click("새 일정 시작"); click("새로 시작")
        click("설정"); click("연결 · 위치 고급 설정")
        editors = [n for n in nodes() if n.get("class") == "android.widget.EditText"]
        assert len(editors) == 6 and all(not n.get("text") for n in editors[1:5]), "Fixture coordinates leaked into new live drafts"
        click("닫기"); click("돌아가기")
        wait("가는 길에, 무엇을 할까요?")
        click("예시로 체험 · 모의 안내")
        wait("이렇게 들르고 갈까요?")
        tap(wait("집에 도착", contains=True))
        tap(wait("날짜 변경", contains=True)); click("24"); click("확인"); click("적용")
        wait("10월 24일", contains=True)
        shot("ux-date-review.png")
        print("PASS: fixture reset clears coordinates; native date editor changes the saved date", flush=True)
        click("수정")
        wait("할 일 수정")
        print("READY: editable dialog open for the actual driving-lock test", flush=True)
        return
    if find("가는 길에, 무엇을 할까요?", tree) is None:
        assert find("예시 데이터", tree, True) is not None, "Do not reset a non-fixture session"
        click("설정"); click("새 일정 시작"); click("새로 시작")
    wait("가는 길에, 무엇을 할까요?")
    assert find("출발 위도", nodes()) is None and find("서버 주소", nodes()) is None
    shot("ux-input.png")
    click("예시로 체험 · 모의 안내")
    wait("이렇게 들르고 갈까요?")
    tree = nodes()
    assert all(find(x, tree) is not None for x in ["약 구매", "세탁물 찾기", "우유 구매"])
    shot("ux-review.png")
    click("수정")
    click("+ 5분")
    wait("15분")
    click("저장")
    wait("필수 · 15분", contains=True)
    click("설정"); click("돌아가기")
    wait("이렇게 들르고 갈까요?")
    click("동선 확인하기")
    wait("다음은 이곳이에요")
    wait("18:45 / 19:00")
    assert find("예시 데이터", nodes(), True) is not None
    target = wait("이곳으로 안내")
    b = list(map(int,re.findall(r"\d+", target.get("bounds",""))))
    assert b[3]-b[1]>=76, "Primary touch target below 76dp on mdpi emulator"
    shot("ux-route.png")
    (out/"ux-route-ui.xml").write_text(adb("shell","cat","/sdcard/omw-smoke.xml"),encoding="utf-8")
    print("PASS: staged flow, hidden settings, actual dwell edit, large primary control",flush=True)
    if a.complete_first or a.full_journey:
        for i in range(3 if a.full_journey else 1):
            click("이곳으로 안내"); click("목적지 승인")
            click("도착 체험 · 모의 이벤트")
            wait("할 일을 마쳤나요?")
            assert find("도착과 할 일 완료는 달라요.", nodes(), True) is not None
            if i==0: shot("ux-arrival-not-complete.png")
            click("할 일 완료"); click("완료했어요")
            if i<2: wait("다음은 이곳이에요")
        print("PASS: approved simulated arrival, explicit completion, remaining-task replan",flush=True)
    if a.full_journey:
        wait("이제 집으로 가요")
        click("집으로 안내"); click("목적지 승인"); click("도착 체험 · 모의 이벤트")
        wait("집에 도착했어요"); shot("ux-home-arrival.png")
        print("PASS: separately approved final home leg and simulated arrival",flush=True)

if __name__ == "__main__":
    main()

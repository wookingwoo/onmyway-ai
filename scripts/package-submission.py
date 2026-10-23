"""Build a local anonymous review package; never uploads or submits anything."""
from hashlib import sha256
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import zipfile


ROOT = Path(__file__).resolve().parents[1]
OUTPUT = ROOT / "artifacts" / "package"


def inspect_text(name, data):
    text = data.decode("utf-8", errors="ignore")
    if re.search(r"sk-(?:proj-)?[A-Za-z0-9_-]{20,}", text):
        raise ValueError(f"Potential credential in {name}; inspect privately")
    forbidden_account = os.environ.get("OMW_ENTRANT_ACCOUNT", "").lower()
    if forbidden_account and forbidden_account in text.lower():
        raise ValueError(f"Entrant account identifier in {name}; remove before anonymous sharing")


def main():
    if not os.environ.get("OMW_ENTRANT_ACCOUNT"):
        raise ValueError("Set OMW_ENTRANT_ACCOUNT privately to scan the entrant account identifier")
    OUTPUT.mkdir(parents=True, exist_ok=True)
    source = OUTPUT / "05_source.zip"
    subprocess.run(["git", "archive", "--format=zip", f"--output={source}", "HEAD",
                    ".env.example", ".gitattributes", ".gitignore", ".github", "AGENTS.md",
                    "README.md", "apps", "services", "contracts", "scripts", "docs"],
                   cwd=ROOT, check=True)
    with zipfile.ZipFile(source) as archive:
        for item in archive.infolist():
            if item.is_dir():
                continue
            name = item.filename
            if Path(name).name == ".env" or name.lower().endswith((".jks", ".keystore")):
                raise ValueError(f"Private file included in source: {name}")
            if not name.lower().endswith(".jar"):
                inspect_text(name, archive.read(item))

    mapping = {
        "01_application_WORKING_DRAFT.docx": "artifacts/submission/OnMyWay_AI_application_WORKING_DRAFT.docx",
        "02_demo_release.apk": "apk/OnMyWay_AI_demo_release.apk",
        "03_fixture_demo.mp4": "artifacts/evidence/OnMyWay_AI_Pleos_fixture_demo.mp4",
        "04_pitch-ko.pptx": "artifacts/pitch/output/OnMyWay-AI-pitch-ko.pptx",
        "06_readiness.md": "docs/readiness.md",
        "07_email-draft-ko.md": "docs/submission/email-draft-ko.md",
        "08_live_release.apk": "apk/OnMyWay_AI_live_release.apk",
    }
    for name, relative in mapping.items():
        origin = ROOT / relative
        if not origin.is_file():
            raise FileNotFoundError(origin)
        shutil.copyfile(origin, OUTPUT / name)
        if origin.suffix in {".docx", ".pptx"}:
            with zipfile.ZipFile(origin) as archive:
                for item in archive.infolist():
                    if item.filename.endswith(".xml"):
                        inspect_text(item.filename, archive.read(item))

    note = """OnMyWay AI 검토용 패키지

현재 상태: PARTIALLY READY
실제 Pleos 앱 실행과 고정 데이터 시연을 검증했습니다.
실제 AI·장소·도로 API 호출과 실제 내비 경로 수락·도착은 아직 검증하지 않았습니다.

확인한 결과
- Python 테스트 25개 / Android 단위 테스트 13개 통과
- demo/live release APK 서명 검증과 Pleos 2.1.0 설치·실행
- 실제 Drive 30km/h에서 편집·목적지 승인 잠금, Park 0으로 복구
- 고정 데이터 계획 → 모의 안내·도착 → 직접 완료 확인 → 남은 일정 재계산

실행
05_source.zip을 풀고 scripts/setup-api.ps1을 실행하세요.
.env.example을 .env로 복사하고, 추후 서버에만 API 키와 모델을 설정하세요.
scripts/start-api.ps1로 서버를 켜고 Pleos 에뮬레이터에 02 APK를 설치하세요.
앱에서 명시적 시뮬레이션을 켠 뒤 예시 시나리오를 불러옵니다.
실제 모드는 08 APK이며 서버 API 설정과 실제 차량 상태가 필요합니다.

제출 전
01 문서의 본인 정보·개인정보 동의·서명은 사용자가 직접 확인합니다.
실 API와 실제 경로 안내 검증, 운영진 접근·익명성 확인이 남았습니다.
07 이메일은 초안이며 발송하지 않았습니다.
이 패키지는 검토용이며 최종 제출 완료를 의미하지 않습니다.
"""
    (OUTPUT / "00_READ_FIRST.txt").write_text(note, encoding="utf-8")
    names = ["00_READ_FIRST.txt", *mapping.keys(), "05_source.zip"]
    manifest = {name: {"bytes": (OUTPUT/name).stat().st_size,
                       "sha256": sha256((OUTPUT/name).read_bytes()).hexdigest()} for name in names}
    (OUTPUT / "SHA256.json").write_text(json.dumps(manifest, ensure_ascii=False, indent=2), encoding="utf-8")
    final = OUTPUT / "OnMyWay_AI_review_package.zip"
    with zipfile.ZipFile(final, "w", zipfile.ZIP_DEFLATED) as archive:
        for name in [*names, "SHA256.json"]:
            archive.write(OUTPUT/name, arcname=name)
    with zipfile.ZipFile(final) as archive:
        assert archive.testzip() is None
    print(json.dumps({"package": str(final.relative_to(ROOT)), "bytes": final.stat().st_size,
                      "sha256": sha256(final.read_bytes()).hexdigest(), "files": len(names)+1,
                      "credential_and_account_scan": "passed", "status": "PARTIALLY READY"}, indent=2))


if __name__ == "__main__":
    main()

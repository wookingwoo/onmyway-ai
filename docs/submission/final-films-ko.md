# OnMyWay AI 최종 Pleos 화면 녹화 본편과 쇼츠

제작·규정 확인: 2026-11-03 KST. 로컬 검토용 최종 편집본이며 공개 게시·공모전
접수는 수행하지 않았습니다. **사용자 수정 요청에 따라 실제 공식 Pleos 화면을
직접 녹화한 버전으로 교체했습니다.** 이전의 그래픽 화면 편집본은 최종 전달 대상에서 제외합니다.
본편과 쇼츠는 목적에 맞춰 별도로 연출했습니다.

## 바로 확인할 파일

저장소 루트 기준 경로입니다. 영상·음성·이미지·SRT·플레이어는 모두 로컬에만
보관하며 Git에는 제작 소스·대본·검증 요약만 넣습니다.

| 결과 | 로컬 파일 | 길이 · 화면 |
| --- | --- | --- |
| 본편 | `artifacts/final-films/pleos/OnMyWay_AI_Pleos_Final.mp4` | 2분 13.5초 · 1920×1080 |
| 쇼츠 | `artifacts/final-films/pleos/OnMyWay_AI_Pleos_Shorts_Final.mp4` | 50초 · 1080×1920 |
| 통합 플레이어 | `artifacts/final-films/pleos/player.html` | 두 영상·저장 링크 |
| 본편 자막 | `artifacts/final-films/pleos/OnMyWay_AI_Pleos_Final.srt` | 음성 타이밍 기준 43개 |
| 쇼츠 자막 | `artifacts/final-films/pleos/OnMyWay_AI_Pleos_Shorts_Final.srt` | 음성 타이밍 기준 23개 |
| 전체 장면표 | `pleos/main/`, `pleos/short/`의 `native-decoded-contact-sheet.jpg` | 로컬 검수 |
| 원본·검증 | `pleos/native-capture-manifest.json`, 각 편집 폴더의 `verification.json` | 해시·원본 구간·규격·음량·출처 |

영상에 한국어 자막이 이미 포함됩니다. 외부 플레이어에서 SRT까지 켜면 중복될 수
있습니다. 브라우저 검수는 최종 파일과 같은 해시의 영상을 소리를 켜고 끝까지
재생하는 방식으로 확인하며, 실제 확인 결과는 검증 요약과 progress.md에 남깁니다.

## 본편: 가는 길에, 할 일까지

정적인 기능 목록보다 한 운전자의 퇴근길을 따라갑니다. 그래파이트 화면과 민트색
동선, 넓은 여백, 구분되는 한국어 내레이터·운전자·비서로 제품의 사용감을 표현합니다.

| 시간 | 이야기 | 핵심 장면 |
| --- | --- | --- |
| 0:00–0:07 | 퇴근은 했는데, 할 일이 남았다 | 세탁물·약은 필수, 우유는 선택 |
| 0:07–0:27.5 | 목적지 말고 오늘 할 일을 말한다 | 자연어 요청 → 도착/수령 마감 확인 |
| 0:27.5–0:45 | 길 위 시간만 세지 않는다 | 이동·영업 대기·체류 → 18:40 귀가 예상 |
| 0:45–1:07.5 | 약국 상담 30분이 추가된다 | 전체 계획 19:05 → 우유 제외 18:53 |
| 1:07.5–1:21 | 결정은 운전자에게 있다 | 수정안 승인 → 다음 목적지 → 주행 잠금 |
| 1:21–1:48 | 도착과 완료는 다르다 | 수령 완료 → 다음 약국 → 약 수령 완료 |
| 1:48–1:57 | 마지막 목적지는 집이다 | 필수 두 용무 완료 → 별도 귀가 승인 |
| 1:57–2:13.5 | 매일 다시 쓸 이유 | 픽업·장보기·가족 용무 → 브랜드 엔딩 |

## 쇼츠: 퇴근했는데 2차 출근이요?

본편 축약본이 아니라 퇴근길 퀘스트 코미디입니다. 밝은 내레이터, 큰 문장,
짧은 대사와 컷, 직접 그린 우유 캐릭터를 사용합니다. 회사 퇴근과 오늘의 할 일
완료가 다른 현실을 훅으로 삼고, 우유의 등장·퇴장·재등장으로 반복 시청을 연결합니다.

| 시간 | 장면 |
| --- | --- |
| 0–6초 | “퇴근했는데 2차 출근이요?” → 필수 2개와 선택 1개 |
| 6–16초 | 집 마감 19:00 → 이동·대기·수령 계산 → 상담 총 30분 |
| 16–22.5초 | 모두 들르면 19:05 → 우유 “나도 데려가?” |
| 22.5–31초 | 필수 유지·우유 제외·18:53 → 우유의 합리적인 퇴장 |
| 31–41초 | 운전자 승인 → 주행 잠금 → 도착과 수령 완료 구분 |
| 41–48초 | 두 용무 완료 → 귀가 승인 → OnMyWay AI |
| 48–50초 | 우유 “내일은 나도?” → 첫 퀘스트 장면과 연결 |

게시용 제목 초안:

> 퇴근했는데 2차 출근이요? 우유는 눈치 챙겨 🥛

설명 초안:

> 세탁물은 꼭, 약도 꼭, 우유는… 시간이 되면.
> 집엔 7시까지인데 약국 상담 30분이 추가됐다면?
> 필수는 지키고, 선택은 조정하고. 가는 길에, 할 일까지. OnMyWay AI.
> 당신의 퇴근길 퀘스트는 몇 개?
>
> #HMG차량용앱개발공모전 #PleosAppMarket #퇴근길

## 음성·사운드

ElevenLabs 목록과 로컬 키의 실제 음성 조회로 사용 가능한 한국어 음성을 확인했고,
본편의 세 역할은 짧은 오디션을 생성한 뒤 역할·길이·타이밍 특성에 맞춰 선택했습니다.
청취자의 선호나 바이럴 성과를 측정했다고 주장하지 않습니다.

| 역할 | 목소리 | 사용 |
| --- | --- | --- |
| 차분한 내레이터 | Min-woo · Professional Korean Announcer | 본편 |
| 자연스러운 운전자 | Minjun · Gentle & Charming Korean | 본편·쇼츠 |
| 따뜻하고 명료한 비서 | Yun · Warm, Clear | 본편·쇼츠 |
| 밝은 내레이터 | KKC · Korean Bright Voice | 쇼츠 |
| 우유 캐릭터 | Aria · Bright & Youthful Korean | 쇼츠 |

모델은 `eleven_multilingual_v2`입니다. 제공자의 문자별 타이밍으로 자막을 배치하며,
영상·음성 경계를 넘어 대사를 자르지 않습니다. 배경음과 짧은 UI 톤은 직접 합성한
독자적인 코드·펄스 구성입니다. 기존 음악·밈 영상·타인 음성 복제는 사용하지 않았습니다.
혼합 음량은 약 -16 LUFS, true peak는 약 -1.5 dBTP입니다.

## 제작 범위와 실제 앱 증거

이번 파일은 공식 **Pleos Connect 2.1.0 · API34**의 `OnMyWay_Pleos` AVD에서
별도 `ai.onmyway.car.film` 네이티브 앱을 설치하고 실제 버튼을 눌러 녹화했습니다.
원본은 2560×1440 디스플레이를 Android screenrecord로 1920×1080에 저장했습니다.
본편은 차량 패널·시스템 바를 포함한 전체 화면을 유지하고, 쇼츠는 같은 실제
원본의 전체 화면·앱 확대 컷을 사용합니다. UI를 그림으로 대체한 장면은 없습니다.
우유 캐릭터와 쇼츠 문구는 제품 화면 밖의 설명·코미디 인서트입니다.

촬영용 화면과 시나리오는 사용자가 허용한 **네이티브 홍보 프로토타입**입니다.
지도는 앱의 Canvas에 그린 개념 동선이며 장소·시간·도착 신호는 작성한 시나리오입니다.
live/demo 앱의 런처와 기능은 변경하지 않았습니다. 실제 CarUxRestrictions를 읽으며,
실제 Virtual Console의 Drive/30 상태에서 OS 자체 주행 차단과 Park 복구를 녹화했습니다.
모든 계획 조작은 실제 정차 상태에서만 가능합니다. 영상에 별도 데모 배지를 넣지
않는 것은 사용자의 명시적 연출 지시입니다. 실시간 AI/STT/도로/내비 결과 또는
출시·입점 완료의 증거로 사용하지 않습니다.
비서 음성도 영상 편집용 ElevenLabs 출력이며 네이티브 Pleos TTS 실행 결과가 아닙니다.
실제 앱 구현·공식 AVD 검증 범위는 기존 README와 각 screenshots/README를 따릅니다.

시나리오 산술: 17:50 출발, 세탁소 이동8분·영업 대기2분·수령5분 → 18:05 완료.
약국 이동8분·기본 체류5분 → 18:18 완료. 마트 이동7분·체류7분·귀가8분 → 18:40.
약국 체류를 **총 30분**으로 바꾸면 동일한 세 용무 계획은 19:05입니다. 우유만
제외하면 약국 18:43 완료·직접 귀가10분 → 18:53입니다. 세탁물 18:30 완료 마감과
귀가 19:00 마감, 필수 두 용무, 현재 계획 승인과 별도 귀가 승인을 유지합니다.

## 공식 규정 확인

- [지원·제출 방법](https://appdevchallenge.auto-hmg.io/guide/submission): 목업·프로토타입·화면 녹화 인정. 본편 길이·코덱의 별도 제한은 확인한 페이지에 없었습니다.
- [숏폼 챌린지](https://appdevchallenge.auto-hmg.io/guide/shortform): 세로 30~60초. 필수 해시태그 포함 총 2~3개. Instagram 릴스 또는 YouTube Shorts 게시 후 링크 제출. 본상과 별도 심사이며 쇼츠 링크에는 본상 가산점이 없습니다.
- [심사·시상](https://appdevchallenge.auto-hmg.io/guide/judging-awards): 차량특화성·창의성·기술 활용성·사업성. 입점은 별도 검증·심사 대상이며 보장하지 않습니다.
- [참가 규정](https://appdevchallenge.auto-hmg.io/guide/rules): 2차 PT 전 동작 앱 개발 완료 필요. 1차 프로토타입 영상과 구별합니다.

공개 게시·권한 부여·최종 제출은 수행하지 않았습니다. 최종 게시 문구와 링크는
사용자가 확인하고 등록합니다. 공유 자료에는 참가자 이름·연락처·소속을 넣지 않았습니다.

## 재생성과 검수

로컬 도구: FFmpeg/FFprobe, Windows 맑은 고딕 폰트, Python 패키지
`scripts/requirements-final-films.txt`. 키는 루트의 ignored `.env`에만 둡니다.

```powershell
python -m pip install -r scripts/requirements-final-films.txt
python -X utf8 services/api/tools/generate_film_audio.py --track all
cd apps/automotive
.\gradlew.bat testFilmDebugUnitTest assembleFilmDebug
cd ../..
# Official Pleos AVD + installed film APK required. Does not clear other apps.
python -X utf8 scripts/record-native-film.py --adb <local-adb-path>
python -X utf8 scripts/build-native-films.py
python -m unittest discover -s scripts/tests -q
python -m http.server 8767 --bind 127.0.0.1 --directory artifacts/final-films
```

`--track all`의 음성 생성은 검증된 캐시를 재사용합니다. 입력이 달라지거나 부분
캐시·미완료 요청이 있으면 자동 재과금하지 않고 멈춥니다. 새로운 생성에는 실제
ElevenLabs 사용료가 발생합니다. API 실패를 다른 음성으로 대체하지 않습니다.

최종 규격·해시·실행한 검사는 [검증 요약](final-films-verification.json)과
[개발 기록](../progress.md)을 참조합니다. 영상 전체 디코딩과 실제 재생 확인은
주관적인 음색 선호·수상·실차 기능 검증을 대신하지 않습니다.

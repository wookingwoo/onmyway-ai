# OnMyWay AI Pleos 녹화본

공식 Pleos Connect 2.1.0 AVD에서 기존 OnMyWay 앱의 `MainActivity`를
직접 조작해 녹화했습니다. 본편은 차량 패널과 시스템 바를 모두 보여 줍니다. 쇼츠는
같은 녹화의 전체 화면과 앱 확대 컷을 교차 편집했습니다. 대사 자막은 앱 화면 밖에 배치했습니다.

| 결과 | 로컬 파일 | 길이 · 화면 |
| --- | --- | --- |
| 본편 | `artifacts/final-films/mainactivity/OnMyWay_AI_Pleos_Product.mp4` | 1분 37.5초 · 1920×1080 |
| 쇼츠 | `artifacts/final-films/mainactivity/OnMyWay_AI_Pleos_Product_Shorts.mp4` | 42.5초 · 1080×1920 |
| 함께 재생 | `artifacts/final-films/mainactivity/player.html` | 로컬 플레이어 |

두 영상의 SRT는 각 MP4 옆에 있습니다. [검증 요약](final-films-verification.json)에
최종 파일 해시, 실제 녹화 구간, 음성 모델, 디코딩과 재생 결과를 기록했습니다.

## 이야기와 연출

1. 세탁물과 약은 필수, 우유는 선택, 귀가는 19:00이 마감입니다. 사용자가 앱에서 저장된 예시 요청을 선택합니다.
2. 앱이 약국 → 세탁소 → 마트 순서를 계산합니다. 세탁물 수령 예정은 18:25, 귀가 예상은 18:40입니다.
3. 약국 상담 시간을 10분에서 30분으로 바꾸면 세탁소가 먼저 올라옵니다. 필수 두 건을 유지하고 우유를 미룬 계획의 귀가 예상은 18:53입니다.
4. 운전자가 계획을 승인합니다. AVD를 Drive/30으로 바꿔 실제 주행 잠금을 녹화하고 Park/0에서 이어갑니다.
5. 앱의 모의 도착 후 수령을 직접 완료합니다. 약 수령을 마친 다음 귀가를 별도로 승인합니다.

본편은 4번의 승인 직후 **별도 경로 예시 · 서울 63빌딩**을 보여 줍니다.
Pleos 기본 내비에서 공개 장소를 검색해 직접 안내를 시작했습니다. 실제 경로 후보,
경로선과 다음 회전 안내가 표시됐고 배경 지도 타일은 비어 있었습니다.
세탁소 승인은 앱에서 고른 예시 흐름이고, 63빌딩은 기본 내비에서 별도로 요청한 경로입니다.

촬영에는 기존 설치본 `ai.onmyway.car.demo`와 그 주간 테마를 사용했습니다. 장소와 일정은
앱의 명시적 예시이고, 도착은 모의 신호입니다. 기존 설치본의 해시는 개발 기록에 있는
10월 26일 서명 데모 APK와 일치합니다. 이번 재촬영은 해당 설치본의 예시 흐름에 대한 증거입니다.

음성은 [Eleven v4](https://elevenlabs.io/docs/eleven-api/choosing-the-right-model)의 한국어로
생성했습니다. BIN은 내레이션, Taehyung은 운전자, 전용 합성 음성은 차량 AI를 맡습니다.
차량 AI에만 짧은 시작 신호음과 디지털 음색을 더했고 첫 자막에 화자를 표시했습니다.
앱 조작 타이밍에 맞춘 편집 대사이며 자막은 제공자의 문자 타이밍을 사용합니다.
배경 음악 없이 대사를 중심에 두고 승인·변경·완료 장면에 짧은 전환음을 사용했습니다.

## 재촬영

공식 Pleos AVD, 기존 데모 APK, 공급자 키를 읽지 않는 로컬 API, adb, FFmpeg가 필요합니다.
로컬 Virtual Console을 `adb forward tcp:8088 tcp:8088`로 연결합니다.
본편의 내비 컷은 `navigation-check/public-route.mp4`와 같은 폴더의
`manifest.json`에 기록한 원본 해시·검수 구간을 사용합니다.

```powershell
python -X utf8 scripts/record-story-demo.py --adb <local-adb-path> --hold-seconds 9 --output artifacts/final-films/mainactivity/capture
python -X utf8 services/api/tools/generate_film_audio.py --mainactivity --track all
python -X utf8 scripts/build-native-films.py
python -m http.server 8767 --bind 127.0.0.1 --directory artifacts/final-films
```

영상 원본, 음성, APK와 플레이어는 로컬 보관입니다. 쇼츠는 공모전의 세로 30~60초 조건에
맞췄습니다. 게시 시 필수 해시태그 `#HMG차량용앱개발공모전`을 포함해 총 2~3개를 사용합니다.
[숏폼 챌린지 안내](https://appdevchallenge.auto-hmg.io/guide/shortform)

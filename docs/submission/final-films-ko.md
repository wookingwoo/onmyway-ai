# OnMyWay AI Pleos 녹화본

공식 Pleos Connect 2.1.0 AVD에서 기존 OnMyWay 앱과 Pleos 기본 내비게이션을
직접 조작해 녹화했습니다. 본편은 차량 패널과 시스템 바를 모두 보여 줍니다. 쇼츠는
같은 녹화의 전체 화면과 앱 확대 컷을 교차 편집했습니다. 대사 자막은 앱 화면 밖에 배치했습니다.

| 결과 | 로컬 파일 | 길이 · 화면 |
| --- | --- | --- |
| 본편 | `artifacts/final-films/mainactivity/OnMyWay_AI_Pleos_Product.mp4` | 1분 44.2초 · 1920×1080 |
| 쇼츠 | `artifacts/final-films/mainactivity/OnMyWay_AI_Pleos_Product_Shorts.mp4` | 59.5초 · 1080×1920 |
| 함께 재생 | `artifacts/final-films/mainactivity/player.html` | 로컬 플레이어 |

두 영상의 SRT는 각 MP4 옆에 있습니다. [검증 요약](final-films-verification.json)에
최종 파일 해시, 실제 녹화 구간, 음성 모델, 디코딩과 재생 결과를 기록했습니다.

## 이야기와 연출

1. 세탁물과 약은 필수, 우유는 선택, 귀가는 19:00이 마감입니다. 사용자가 앱에서 저장된 예시 요청을 선택합니다.
2. 앱이 약국 → 세탁소 → 마트 순서를 계산합니다. 세탁물 수령 예정은 18:25, 귀가 예상은 18:40입니다.
3. 약국 상담 시간을 10분에서 30분으로 바꾸면 세탁소가 먼저 올라옵니다. 필수 두 건을 유지하고 우유를 미룬 계획의 귀가 예상은 18:53입니다.
4. 운전자가 계획을 승인하고 정차 중 이동 현황에서 목적지와 계획 시각을 확인합니다.
5. 앱의 모의 도착 후 수령을 직접 완료합니다. 약 수령을 마친 다음 귀가를 별도로 승인합니다.

본편은 4번의 정차 화면에 이어 **별도 경로 예시 · 서울 63빌딩**을 보여 줍니다.
Pleos 기본 내비에서 공개 장소를 검색해 직접 안내를 시작했고, 두 영상에 Drive/30의
실제 회전 안내와 경로선을 담았습니다. 이 AVD의 시스템은 Drive에서 OnMyWay의
읽기 전용 화면 위에 주행 제한 안내를 표시했습니다. 기본 내비의 배경 지도 타일은 비어 있었습니다.
세탁소 승인은 앱에서 고른 예시 흐름이고, 63빌딩은 기본 내비에서 별도로 요청한 경로입니다.

촬영에는 이번에 빌드한 `ai.onmyway.car.demo`와 기존 주간 테마를 사용했습니다.
앱에서 고른 샘플 입력은 단어별로 나타납니다. 계획이 계산되면 `AI 안내` 카드에 실제
요약 문장이 표시되고, 앱이 안내를 읽는 동안 파형이 움직입니다. 장소와 일정은
앱의 명시적 예시이고, 도착은 모의 신호입니다.
계획과 완료 후 재계산 동안 진행 카드가 표시됩니다. 예시 흐름은 카드를 읽을 수 있도록
짧은 최소 표시 시간을 적용합니다. 실사용 모드는 실제 요청이 진행되는 동안 카드를 표시합니다.
이동 현황의 도착·귀가 시각은 승인 당시 계획을 기준으로 합니다.

정차 중 계획 화면의 주 버튼은 `이 계획으로 출발`입니다. 음성 변경과 음성 승인은
`음성 요청`에서 시작합니다. `다시 듣기`는 AI 안내 카드 안에, 일정 상세와 재계산은
`더보기`에 있습니다. 마지막 목적지는 `집으로 안내 시작`으로 따로 승인합니다.

음성은 [Eleven v4](https://elevenlabs.io/docs/eleven-api/choosing-the-right-model)의 한국어로
생성했습니다. BIN은 내레이션, Taehyung은 운전자, 전용 합성 음성은 차량 AI를 맡습니다.
차량 AI의 디지털 음색을 유지했습니다. 영상에는 앱 화면과 같은 문장을 Eleven v4
편집 음성으로 얹었고, 자막은 제공자의 문자 타이밍을 사용합니다. 입력을 열 때,
재계획 응답, 첫 도착, 직접 완료를 확인할 때에만 짧은 효과음을 배치했습니다.

## 재촬영

공식 Pleos AVD, 최신 데모 APK, 공급자 키를 읽지 않는 로컬 API, adb, FFmpeg가 필요합니다.
로컬 Virtual Console을 `adb forward tcp:8088 tcp:8088`로 연결합니다.
내비 컷은 `navigation-check/public-route.mp4`와 `public-drive.mp4`를 사용합니다.
같은 폴더의 `manifest.json`과 `drive-manifest.json`에 원본 해시와 실제 표시가 확인된 구간을
기록합니다. 앱 이동 요약의 Park 구간도 녹화 픽셀을 검수하고 `capture-manifest.json`에
`reviewed_parked_overview`로 시작과 끝을 기록합니다.

```powershell
python -X utf8 scripts/record-story-demo.py --adb <local-adb-path> --serial <pleos-serial> --hold-seconds 7 --voice-motion --moving-overview --output artifacts/final-films/mainactivity/cockpit-final-actions
python -X utf8 services/api/tools/generate_film_audio.py --mainactivity --track all
python -X utf8 scripts/build-native-films.py --capture artifacts/final-films/mainactivity/cockpit-final-actions
python -m http.server 8767 --bind 127.0.0.1 --directory artifacts/final-films
```

영상 원본, 음성, APK와 플레이어는 로컬 보관입니다. 쇼츠는 공모전의 세로 30~60초 조건에
맞췄습니다. 게시 시 필수 해시태그 `#HMG차량용앱개발공모전`을 포함해 총 2~3개를 사용합니다.
[숏폼 챌린지 안내](https://appdevchallenge.auto-hmg.io/guide/shortform)

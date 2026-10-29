# 스크린샷 테스트 규칙

공유 Compose UI의 Android/iOS·ko/en 시각 계약에 적용합니다.
코드 작성 절차와 Home 예제는 [스크린샷 작성 스킬](../../.agents/skills/caro-screenshot-testing/SKILL.md),
Golden·수동 실행·게시와 승인은 [스크린샷 CI](../ci/screenshot-test.md)가 원본입니다.

## 책임과 배치

| 위치 | 책임 |
| --- | --- |
| `feature/<module>/src/commonTest/` | 화면별 `***ScreenshotScenario.kt`의 상태·ID·렌더러·단언과 `ScreenshotCapture.kt`의 Kotlin 설정·캡처 모델 |
| `src/androidHostTest/` | 화면별 Android 진입점과 feature-local `ScreenshotAndroidCapture.kt` 실행 보조 |
| `src/iosSimulatorArm64Test/` | 화면별 iOS 진입점과 feature-local `ScreenshotIosCapture.kt` 실행 보조 |
| `scripts/screenshot/scenarios.json` | 모듈별 기대 논리 ID |
| `.github/workflows/screenshot-*.yml` | Bash·jq의 기대 파일명·Golden·결과·게시 검증과 댓글 생성 |

`caro.screenshot`은 Roborazzi와 테스트 의존성만 연결합니다. 언어·기기·variant·content 분류는 각 feature의 Kotlin에 직접 선언합니다.
언어·프로필 ID를 바꾸면 세 워크플로의 Bash·jq 기대 파일명 규칙도 함께 수정합니다. 설정 JSON·소스 생성은 사용하지 않습니다.
`caro.kmp.test`는 `withHostTest`, Android resources 포함과 JUnit 호스트 실행을 설정합니다.
플랫폼 실행 코드는 각 feature의 기존 테스트 소스셋에 둡니다. 다른 feature나 production 소스에 테스트 코드를 연결하지 않습니다.

## 공통 시나리오와 기대 목록

- 양쪽 플랫폼은 feature의 `commonTest`에 있는 상태·ID·고정 데이터·렌더러·의미 단언을 공유합니다.
- 화면별 Android·iOS 래퍼가 해당 `***ScreenshotScenario::scenarios`를 직접 전달합니다. ko/en Screen 복사본을 만들지 않습니다.
- ID 추가·삭제는 해당 화면의 목록과 registry에 함께 반영합니다. JSON 자체는 테스트를 생성하거나 실행하지 않습니다.
- 시간·날짜·난수·이미지·네트워크·애니메이션은 고정합니다. Screen에 상태·콜백을 전달합니다. production DI는 사용하지 않습니다.
- 언어별 새 case로 콜백 카운터를 격리합니다. 클릭 횟수·로딩 비활성화·작은 화면 스크롤·Dialog 콜백 단언을 보존합니다.

## 로케일·치수·파일명

파일명은 `<scenario-id>__<language>__<profile-id>.png`입니다. 플랫폼은 출력 경로·메타데이터에 둡니다.
ID는 영문·숫자로 시작합니다. 영문·숫자·점·밑줄·하이픈을 사용합니다. `__`, `_actual`·`_compare`·`_before`는 예약합니다.

기본 가상 캔버스는 Pixel 1280×2856 px/density3, iPhone 1206×2622 px/density3입니다.
Pixel의 논리 폭 426.666…dp를 정수 dp로 반올림해 물리 크기와 같다고 표현하지 않습니다.
Skiko Size는 px입니다. Density도 지정합니다. 물리 ppi를 Compose density로 사용하지 않습니다.
프로필 이름은 실제 Simulator 모델·OS 실행 증거가 아닙니다. 전체 기기의 시스템 바를 캡처하는 테스트도 아닙니다.

일반 root의 측정 px는 프로필 캔버스와 정확히 같아야 합니다. Dialog는 `isDialog()`로 선택합니다. content bounds를 적용합니다.
Android Dialog는 같은 semantics owner의 연결된 View가 같은 치수인지 단언한 뒤 View 캡처를 사용합니다.
임의 root 인덱스·여러 window 합성·후처리 crop/scale로 치수를 맞추지 않습니다.
`SCREENSHOT_GEOMETRY`는 Kotlin 테스트가 단언한 root·적용된 Compose density/fontScale의 로그입니다. CI는 이 로그나 PNG 내용·치수를 해석하지 않습니다.
메타데이터의 languages·profiles는 기대 파일명 라벨입니다. 실제 기기 모델·OS·치수·density·fontScale의 측정값이 아닙니다.

작은 화면 case만 `small-large-font` variant를 사용합니다. 320×480 논리 크기/fontScale1.5와 계정탈퇴 스크롤을 유지합니다.
모든 시나리오에 모든 variant를 곱하지 않습니다. 상호작용 중에는 clock을 진행합니다. 캡처 때는 일시정지 상태로 복원합니다.

Android Locale·LocaleList·qualifiers는 예외가 나도 복원합니다. iOS는 앱 도메인에 저장된 AppleLanguages만 보관합니다.
원래 key가 없으면 다시 제거합니다. 시스템 fallback을 앱 override로 저장하지 않습니다.
Home/Setting의 실제 리소스 ko/en 전환도 의미 단언으로 확인합니다. 고정 사용자 입력 문자열은 번역하지 않아도 됩니다.

## 검증과 보고

사용자가 지정한 실행 경계를 따릅니다. 로컬 캡처 중지 중에는 screenshot test·record·compare·render·capture를 실행하지 않습니다.
허용된 정적 검사·합성 CI fixture·컴파일과 실제 캡처 성공을 구분합니다. 목표 개수·실제 출력·미검증 항목을 따로 보고합니다.
기존 PNG가 남아 있어도 수정한 소스의 캡처 성공을 증명하지는 않습니다.

PNG는 `feature/<module>/build/outputs/roborazzi/<target>/`의 빌드 산출물입니다. 이미지·보고서는 커밋하지 않습니다.
record 후 compare 통과는 시각적 승인이 아닙니다. 원본 검토와 실제 실행·게시 결과를 구분합니다.
명령과 코드 예제는 [Home 참고자료](../../.agents/skills/caro-screenshot-testing/references/home-example.md)를 확인합니다.

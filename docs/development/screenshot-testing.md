# 스크린샷 테스트 규칙

공유 Compose UI의 Android/iOS·ko/en 시각 계약에 적용합니다.
코드 작성 절차와 Home 예제는 [스크린샷 작성 스킬](../../.agents/skills/caro-screenshot-testing/SKILL.md),
Golden·수동 실행·게시와 승인은 [스크린샷 CI](../ci/screenshot-test.md)가 원본입니다.

## 책임과 배치

| 위치 | 책임 |
| --- | --- |
| `core/screenshot-testing/src/commonMain/` | 공통 시나리오·캡처 대상·Variant·프로필·ko/en 확장·파일명·문자열 준비·Canvas·Geometry |
| `core/screenshot-testing/src/androidMain/` | Android Rule·Locale·Clock·Window 선택·Roborazzi 캡처 |
| `core/screenshot-testing/src/iosMain/` | Skiko 실행·Locale·AppleLanguages 복원·Roborazzi 캡처 |
| `feature/<module>/src/commonTest/` | 화면별 `***ScreenshotScenario.kt`의 Screen·State·Fixture·ID·렌더러·동작 단언 |
| `feature/<module>/src/androidHostTest/` | 화면별 Android 테스트 진입점 |
| `feature/<module>/src/iosSimulatorArm64Test/` | 화면별 iOS 테스트 진입점 |
| `scripts/screenshot/scenarios.json` | 모듈별 기대 논리 ID와 variant |
| `.github/workflows/screenshot-*.yml` | Bash·jq의 기대 파일명·Golden·결과·게시 검증과 댓글 생성 |

`caro.screenshot`은 Roborazzi 플러그인과 `commonTest`의 `:core:screenshot-testing` 의존성을 연결합니다.
플랫폼 테스트는 이 의존성의 Android/iOS 라이브러리 variant를 사용합니다.
Host Test에는 manifest와 JUnit Vintage를 유지합니다.
`caro.kmp.test`의 Kotest·JUnit Platform·Android resources 설정도 유지합니다.
공통 모듈은 다른 프로젝트 모듈에 의존하지 않습니다.
Feature와 앱의 main 소스셋은 이 모듈에 의존하지 않습니다.
테스트 인프라 모듈의 main은 재사용 라이브러리 배포 단위입니다. 앱 런타임에는 연결되지 않습니다.

기기·언어 값은 공통 모듈의 `ScreenshotProfile.kt`가 원본입니다. 설정 JSON·소스 생성은 사용하지 않습니다.
시나리오가 `variant = ScreenshotVariant.NARROW` 또는 `SMALL_LARGE_FONT`를 선언합니다. 기본값은 `DEFAULT`입니다.
공통 설정에 Feature ID 목록을 추가하지 않습니다. 캡처 대상은 `ScreenshotTarget.SCREEN`(기본값) 또는 `DIALOG`입니다.
새 Feature는 시나리오와 두 플랫폼 진입점만 작성합니다. 캡처·프로필·문자열·Geometry 코드를 복사하지 않습니다.

공통 모듈은 Android, `iosArm64()`, `iosSimulatorArm64()` 타깃을 선언합니다.
기본 계층의 `iosMain`은 두 iOS 타깃의 main에 연결됩니다.
`iosSimulatorArm64Test`는 simulator main과 commonTest에 연결됩니다.
앱 framework export가 있는 `caro.kmp.ios`는 적용하지 않습니다.
공통 모듈에는 Roborazzi Gradle 플러그인을 적용하지 않습니다. record/compare와 출력은 Feature 소유입니다.

## 공통 시나리오와 기대 목록

- 양쪽 플랫폼은 feature의 `commonTest`에 있는 상태·ID·고정 데이터·렌더러·의미 단언을 공유합니다.
- 화면별 Android·iOS 래퍼가 해당 `***ScreenshotScenario::scenarios`를 직접 전달합니다. ko/en Screen 복사본을 만들지 않습니다.
- ID 또는 variant 변경은 해당 화면의 목록과 registry에 함께 반영합니다. JSON 자체는 테스트를 생성하거나 실행하지 않습니다.
- 시간·날짜·난수·이미지·네트워크·애니메이션은 고정합니다. Screen에 상태·콜백을 전달합니다. production DI는 사용하지 않습니다.
- 언어별 새 case로 콜백 카운터를 격리합니다. 클릭 횟수·로딩 비활성화·작은 화면 스크롤·Dialog 콜백 단언을 보존합니다.

## 로케일·치수·파일명

파일명은 `<scenario-id>__<language>__<profile-id>.png`입니다. 플랫폼은 출력 경로·메타데이터에 둡니다.
ID는 영문·숫자로 시작합니다. 영문·숫자·점·밑줄·하이픈을 사용합니다. `__`, `_actual`·`_compare`·`_before`는 예약합니다.

기본 가상 캔버스는 Pixel 1280×2856 px/density3, iPhone 1206×2622 px/density3입니다.
Pixel의 논리 폭 426.666…dp를 정수 dp로 반올림해 물리 크기와 같다고 표현하지 않습니다.
Skiko Size는 px입니다. Density도 지정합니다. 물리 ppi를 Compose density로 사용하지 않습니다.
프로필 이름은 실제 Simulator 모델·OS 실행 증거가 아닙니다. 전체 기기의 시스템 바를 캡처하는 테스트도 아닙니다.

`SCREEN` root의 측정 px는 프로필 캔버스와 정확히 같아야 합니다.
Android의 `DIALOG`는 `isDialog()`로 유일한 노드를 선택합니다. iOS는 격리 렌더링한 콘텐츠의 태그로 노드를 선택합니다.
두 플랫폼의 PNG는 Dialog 콘텐츠만 포함합니다. 스크림은 포함하지 않습니다.
노드의 폭과 높이는 각각 1px 이상이어야 합니다. 각 값은 프로필 캔버스의 폭과 높이를 넘을 수 없습니다.
노드의 폭과 높이가 캔버스 전체 크기와 동시에 같을 수 없습니다. `target`이 노드 선택과 크기 검사 기준을 결정합니다.
Android Dialog는 아래 Window 선택 계약을 사용합니다.
임의 root 인덱스·여러 window 합성·후처리 crop/scale로 치수를 맞추지 않습니다.
`SCREENSHOT_GEOMETRY`는 Kotlin 테스트가 단언한 root·적용된 Compose density/fontScale의 로그입니다. CI는 이 로그나 PNG 내용·치수를 해석하지 않습니다.
메타데이터의 languages·profiles는 기대 파일명 라벨입니다. 실제 기기 모델·OS·치수·density·fontScale의 측정값이 아닙니다.

Setting의 작은 화면 case는 `small-large-font`를 사용합니다. 320×480 논리 크기/fontScale1.5와 계정탈퇴 스크롤을 유지합니다.
Library의 세 narrow case는 `narrow`로 320×700 논리 크기/fontScale1을 유지합니다. `english` ID는 고정 데이터 variant이며 리소스 로케일은 ko/en으로 실행합니다.
모든 시나리오에 모든 variant를 곱하지 않습니다. 상호작용 중에는 clock을 진행합니다. 캡처 때는 일시정지 상태로 복원합니다.

Android Locale·LocaleList·qualifiers는 예외가 나도 복원합니다. iOS는 앱 도메인에 저장된 AppleLanguages만 보관합니다.
원래 key가 없으면 다시 제거합니다. 시스템 fallback을 앱 override로 저장하지 않습니다.
모든 공통 시나리오는 `expectedStrings`에 대표 `StringResource`와 필요한 포맷 인자를 선언합니다.
각 플랫폼은 캡처용 로케일 적용 함수와 분리된 준비 단계에서 ko/en의 `ResourceEnvironment`를 확보합니다.
준비 중 변경한 로케일은 `finally`에서 복원합니다.
UI 렌더링 전에 명시적인 환경으로 `getString(environment, resource)`를 호출합니다.
선택한 리소스의 ko/en 결과가 같으면 번역과 기대 언어 환경을 확인하도록 실패시킵니다.
`beforeCapture`·`afterCapture`는 준비된 `ScreenshotStrings`를 리소스 키로 조회합니다. 문구·줄바꿈은 XML에서 관리합니다.
현재 화면의 로케일로 기대값을 다시 읽지 않습니다. 고정 사용자 입력 문자열은 번역하지 않아도 됩니다.

## Android Dialog의 View와 Window

[리뷰 #4438567795](https://github.com/whatever-x/Caro-Mobile/pull/197#discussion_r4438567795)는
Home 라이브러리 오류와 Setting 알림 권한 Dialog의 ko/en 캡처가 `NoMatchingViewException`으로 실패했다고 보고했습니다.
전체 스택 트레이스·실패 당시 View hierarchy는 댓글에 없습니다. 현재 PR head와 연결된 실행 목록과 최근 Screenshot PR 목록에서
해당 #197 스크린샷 실행을 찾지 못했습니다. 일반 CI의 성공은 Dialog 이미지 생성의 증거가 아닙니다.

확인한 소스 경로는 다음과 같습니다.

- 카탈로그의 Compose Multiplatform UI·test·JUnit4는 1.11.0, Android test manifest는 1.11.2입니다.
  게시된 [UI test 1.11.0 module metadata](https://repo1.maven.org/maven2/org/jetbrains/compose/ui/ui-test/1.11.0/ui-test-1.11.0.module)를 포함해
  UI·test·JUnit4의 Android variant는 AndroidX 1.11.1을 의존합니다.
  최종 선택 버전을 확인하는 Gradle 의존성 해석은 미실행입니다.
  비교한 [Multiplatform v1.11.0 소스](https://github.com/JetBrains/compose-multiplatform-core/blob/v1.11.0/compose/ui/ui/src/androidMain/kotlin/androidx/compose/ui/window/AndroidDialog.android.kt)와
  [AndroidX UI Android 1.11.1 게시 소스](https://dl.google.com/dl/android/maven2/androidx/compose/ui/ui-android/1.11.1/ui-android-1.11.1-sources.jar)의
  `AndroidDialog.android.kt` 모두 별도 Window의 `DialogLayout`에서 `dialog()` semantics를 만듭니다.
  Android의 semantics root는 `ViewRootForTest.view`로 소유 View에 연결됩니다.
  이 View의 부모 계층에 있는 `DialogWindowProvider`가 Dialog Window를 제공합니다. semantics node와 Window의 decor View는 같은 객체가 아닙니다.
- Roborazzi 1.75.0의 [Roborazzi.kt](https://github.com/takahirom/roborazzi/blob/1.75.0/roborazzi/src/main/java/com/github/takahirom/roborazzi/Roborazzi.kt)에서
  연결된 `View.captureRoboImage()`는 `onView(IsEqual(targetView))`로 위임합니다. 기존 View 경로도 Espresso를 사용하며 root를 지정하지 않습니다.
  Semantics 캡처는 여러 Window가 있으면 전체 화면 캡처로 전환할 수 있습니다. 따라서 Dialog를 이 경로로 바꾸지 않습니다.
- 현재 Android 실행 환경은 Robolectric 4.17·SDK 35·NATIVE graphics입니다. Espresso 기본 root 선택이 목표 Dialog Window와
  어긋났다는 것이 소스에 부합하는 추정 원인입니다. 당시 선택된 root는 로그·재현 없이 확정할 수 없습니다.

공통 Android 실행기는 `isDialog()`의 **같은 노드**를 측정하고 `ViewRootForTest.view`를 가져옵니다.
UI thread에서 부모 계층의 `DialogWindowProvider`가 유일한지 확인합니다.
해당 Window의 `decorView`와 `view.rootView`가 같은지 확인합니다. 이 decor는 Activity decor와 달라야 합니다.
소유 View는 Window에 연결되어 있어야 합니다. View의 px 크기는 노드의 크기와 정확히 같아야 합니다.
그 뒤 `onView(sameInstance(view)).inRoot(withDecorView(sameInstance(decor)))`의 **ViewInteraction** 캡처 API를 호출합니다.
기존과 같은 소유 View를 대상으로 하되 Espresso가 탐색할 Window를 명시합니다. decor 자체나 Activity·Dialog 합성 화면을 캡처하지 않습니다.

전제조건은 다음과 같습니다.

- Dialog semantics 노드가 하나여야 합니다.
- 해당 부모 계층에 Window provider가 있어야 합니다.
- owner View와 노드의 치수가 같아야 합니다.
- Espresso가 연결된 Window를 탐색할 수 있어야 합니다.

전제가 맞지 않으면 실패합니다. root 인덱스·fallback·다중 Window 합성·PNG crop/scale로 보정하지 않습니다.
iOS의 Skiko 캡처는 선택한 경계 안의 장면을 합성합니다. 스크림이 있는 화면에서는 노드만 선택해도 배경이 남을 수 있습니다.
iOS Dialog 시나리오는 `iosDialogContent`로 실제 화면과 같은 Dialog 콘텐츠를 격리 렌더링합니다. 같은 콜백을 사용합니다.
`LocalCaroDialogContentOnly`는 `CaroDialog`의 Window와 스크림을 생략합니다. 같은 콘텐츠를 렌더링합니다.
`iosDialogContentTag`로 콘텐츠를 선택합니다. 태그가 없거나 콘텐츠가 전체 캔버스 크기이면 검사는 실패합니다.
`ScreenshotDialogAndroidTest`는 같은 선택 함수가 Dialog owner를 찾는지 확인하는 회귀 테스트 소스이며 이미지 성공을 증명하지 않습니다.
수정된 Home/Setting ko/en의 실제 Roborazzi record/compare, PNG 치수·내용과 기존 Golden의 시각 비교는 실행 후 별도 확인해야 합니다.

## 검증과 보고

사용자가 지정한 실행 경계를 따릅니다. 로컬 캡처 중지 중에는 screenshot test·record·compare·render·capture를 실행하지 않습니다.
허용된 정적 검사·합성 CI fixture·컴파일과 실제 캡처 성공을 구분합니다. 목표 개수·실제 출력·미검증 항목을 따로 보고합니다.
기존 PNG가 남아 있어도 수정한 소스의 캡처 성공을 증명하지는 않습니다.

PNG는 `feature/<module>/build/outputs/roborazzi/<target>/`의 빌드 산출물입니다. 이미지·보고서는 커밋하지 않습니다.
record 후 compare 통과는 시각적 승인이 아닙니다. 원본 검토와 실제 실행·게시 결과를 구분합니다.
명령과 코드 예제는 [Home 참고자료](../../.agents/skills/caro-screenshot-testing/references/home-example.md)를 확인합니다.

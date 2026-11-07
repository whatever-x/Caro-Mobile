---
name: caro-screenshot-testing
description: >
  Caro-Mobile의 스크린샷 테스트 코드를 추가·작성·수정·삭제하거나 Android/iOS 및 ko/en 시나리오
  범위를 점검할 때 사용한다. 일반 문서 조회나 ViewModel 단위 테스트만 작성하는 요청에는 적용하지 않는다.
---

# Caro 스크린샷 테스트 작성

화면별 공통 시나리오를 정의한다. Android와 iOS는 해당 화면의 같은 목록을 ko/en으로 실행한다.
공통 API·프로필·캡처 실행기는 `:core:screenshot-testing`을 사용한다.
이 스킬의 작성 절차를 따른다. 공통 규칙과 Dialog Window 분석은 [개발자 지침](../../../docs/development/screenshot-testing.md)을 참조한다.

## 작성 절차

1. **실제 API와 기존 테스트를 읽는다.** 대상 feature의 `src/commonMain/**/*Screen.kt`, `mvi/*State.kt`,
   `src/commonTest/**/*ScreenshotScenario.kt`와 화면별 `*Screenshot*Test.kt`를 확인한다.
   공통 API는 `core/screenshot-testing/src/commonMain/kotlin/com/whatever/caro/core/screenshot/testing/`에서 확인한다.
   공통 모듈은 Feature나 앱 구성 요소에 의존하지 않는다.
   Feature의 State·Fixture·Screen·콜백 단언은 Feature에 둔다.
   Home은 `HomeScreen(state: HomeState, onIntent: (HomeIntent) -> Unit)`을 사용한다.
   콘텐츠 캡처의 `HomeState.isLoading`은 false로 지정한다(기본값 true). 다른 화면의 파라미터를 Home과 같다고 가정하지 않는다.
2. **시각 계약·고정 데이터·ID를 정한다.** 예: `home.home.with_deck`. 로딩·오류·빈 상태 등 구별할 상태만 추가한다.
   시간·날짜·난수·네트워크·이미지 로더·애니메이션의 변동을 제거한다. Screen에 상태와 콜백을 직접 전달한다.
   fixture 렌더링에 production DI·ViewModel·다른 feature를 연결하지 않는다.
3. **기대 ID를 등록한다.** `scripts/screenshot/scenarios.json`의 해당 모듈 배열에 `{"id": "...", "variant": "default"}` 항목을 추가하거나 삭제한다.
   `variant`는 `default`, `narrow`, `small-large-font` 중 하나를 사용한다. Kotlin 시나리오의 Variant와 일치시킨다.
   JSON은 기대 목록일 뿐 Kotlin 테스트를 생성하지 않는다. 등록된 모든 모듈을 통틀어 ID를 고유하게 작성한다.
   `__`, `_actual`·`_compare`·`_before` 접미사는 사용하지 않는다.
4. **화면별 시나리오를 구현한다.** feature의 `commonTest`에 있는 화면별 `***ScreenshotScenario.kt`를 수정한다.
   `com.whatever.caro.core.screenshot.testing`의 `ScreenshotScenario`·`ScreenshotString`을 import한다.
   ID·렌더러·expectedStrings·before/after 단언을 선언한다.
   해당 화면의 `scenarios()`가 이 목록을 반환하게 한다.
   기본 캡처 대상은 `ScreenshotTarget.SCREEN`, 기본 프로필 Variant는 `ScreenshotVariant.DEFAULT`다.
   필요한 case에만 `variant = ScreenshotVariant.NARROW` 또는 `SMALL_LARGE_FONT`를 선언한다.
   공통 설정에 Feature ID나 해당 Feature 클래스를 추가하지 않는다.
   새 렌더러는 `CaroTheme { Screen(state, onIntent) }`로 작성한다.
   콜백 횟수·비활성화·스크롤 도달 등 필요한 의미 단언도 공통 시나리오에 둔다. 언어마다 새 시나리오 인스턴스로
   카운터를 격리한다. ko/en Screen 복사본을 만들지 않는다.
5. **두 플랫폼을 배선한다.** Android `androidHostTest`는 `ParameterizedRobolectricTestRunner`에서
   화면 팩토리를 `screenshotCaptures`에 전달한다. 각 case에 새 Compose rule을 사용한다.
   Home은 `screenshotCaptures("android", HomeScreenshotScenario::scenarios)`를 사용한다.
   iOS 래퍼는 `captureIosScreenshots(HomeScreenshotScenario::scenarios)`처럼 같은 화면 팩토리를 전달한다.
   Feature에는 두 플랫폼 진입점만 둔다. 공통 패키지의 `ScreenshotCapture`·`screenshotCaptures`·
   `screenshotAndroidRule`·`captureAndroidScreenshot`·`captureIosScreenshots`를 필요한 위치에서 import한다.
   캡처 보조 코드를 Feature에 복사하지 않는다. `caro.kmp.test`와 `caro.screenshot`을 적용한다.
   `caro.screenshot`이 `commonTest`에 공통 모듈을 연결하므로 `commonMain` 의존성을 추가하지 않는다.
   공통 프로필·언어는 `ScreenshotProfile.kt`에 있다. 언어는 ko/en이다. 설정 JSON이나 코드 생성은 사용하지 않는다.
6. **파일명·로케일·치수를 지킨다.** 파일명은 `<scenario-id>__<language>__<profile-id>.png`로 만든다.
   플랫폼은 출력 경로에 둔다. Skiko Size는 px다. Density도 전달한다. Android host qualifiers는 공통 Kotlin 프로필로 계산한다.
   정확한 window px와 Compose density/fontScale을 적용한다. 일반 root의 측정값은 canvas px와 같아야 한다.
   언어·프로필 ID를 바꾸면 세 워크플로의 Bash·jq 기대 파일명 규칙도 함께 수정한다. CI는 파일·결과 목록을 대조한다.
   root·density/fontScale 단언은 Kotlin 테스트에 유지한다. CI가 PNG 치수나 geometry 로그를 검사한다고 보고하지 않는다.
   새 ID에 기존 Variant를 적용하면 registry의 `variant`를 Kotlin 선언과 일치시킨다.
   새 Variant가 필요하면 공통 enum·양 플랫폼 프로필과 세 워크플로의 profile 목록·metadata 검사를 함께 갱신한다.
   profile ID나 기기값 변경의 Golden 전환은 CI 정책을 따른다. 명시된 variant만 사용한다. Setting은 320×480/fontScale1.5와 계정탈퇴 스크롤, Library narrow는 320×700/fontScale1을 유지한다.
   Library의 `english` ID는 데이터 variant다. 리소스 로케일을 en으로 강제하지 않는다.
   Android Locale·LocaleList·qualifiers와 iOS의 **앱 도메인에 저장된** AppleLanguages 값을 finally에서 복원한다.
   시스템 fallback을 앱 설정으로 저장하지 않는다. 모든 공통 시나리오는 `expectedStrings`에
   `ScreenshotString(Res.string.key)`를 선언한다. 포맷 인자는 `formatArgs`에 둔다.
   플랫폼 보조 코드는 캡처용 로케일 함수와 분리해 ko/en `ResourceEnvironment`를 확보한다.
   준비 중 변경한 로케일을 복원한다.
   `prepareScreenshotStrings`가 UI 렌더링 전에 `getString(environment, resource)`로 기대값을 읽는다.
   선택한 리소스의 ko/en 값이 같으면 준비를 실패시킨다. 의미 있는 번역 차이가 있는 대표 문구를 선택한다.
   before/after 콜백은 `ScreenshotStrings`를 받아 `strings[Res.string.key]`로 조회한다.
   대표 문구는 `onNodeWithText(strings[Res.string.key]).assertIsDisplayed()`로 확인한다.
   현재 화면 로케일로 기대값을 다시 읽거나 번역 문구를 테스트에 복사하지 않는다. 고정 사용자 입력 데이터와 구분한다.
7. **Dialog를 명시한다.** 공통 case에 `target = ScreenshotTarget.DIALOG`를 지정한다.
   target이 플랫폼별 Dialog 콘텐츠 선택과 크기 검사 기준을 결정한다. 별도 ID 분류 목록은 없다.
   Android 공통 실행기는 해당 노드의 소유 View·부모의 `DialogWindowProvider`·decor 동일성·치수를 확인한다.
   `onView(sameInstance(view)).inRoot(withDecorView(sameInstance(decor)))`의 ViewInteraction을 캡처한다.
   iOS는 `iosDialogContent`로 실제 Dialog와 같은 콘텐츠를 스크림 없이 격리 렌더링한다. 같은 콜백을 사용한다.
   `iosDialogContentTag`로 콘텐츠를 선택한다. 화면이나 별도 UI 복사본으로 대체하지 않는다.
   연결된 View의 Roborazzi overload도 Espresso를 호출하므로 `view.captureRoboImage()`로 대체하지 않는다.
   전제조건이 맞지 않으면 실패시킨다. 임의 root 인덱스를 사용하지 않는다.
   여러 Window를 합성하거나 PNG를 crop/scale해서 맞추지 않는다.
   상호작용 중에는 clock을 진행시켜 스크롤을 완료한다. PNG 캡처 때는 일시정지 상태로 복원한다.
8. **허용된 검증과 한계를 보고한다.** 사용자 실행 경계를 먼저 적용한다. 로컬 스크린샷 중지 지시가 있으면
   record/compare/render/capture와 스크린샷 테스트를 실행하지 않는다. 빌드·컴파일·로컬 테스트·합성 CI·설치도
   금지된 요청에서는 실행하지 않는다. 허용된 정적 일관성 점검만 수행한다.
   실행하지 않은 항목은 미실행으로 보고한다.
   공통 계약·로케일 테스트는 공통 모듈, Feature 참조 단언은 해당 Feature에 유지한다.
   공통 모듈 변경 시 회귀 범위는 등록된 모든 Feature의 양 플랫폼·ko/en이다. 스킬 수정의 정적 검사와 실제 에이전트 로딩도 구분한다.
   목표 캡처 수와 실제 실행 결과를 구분한다. 캡처가 허용되면 양쪽 ko/en 파일명·측정 치수·원본을 확인한다.
   record 직후 compare 통과는 시각적 승인이 아니다. PNG·보고서는 커밋하지 않는다.

처음 시나리오를 작성하거나 화면별 목록·플랫폼 래퍼를 바꿀 때는
[Home 작성 예제와 명령](references/home-example.md)을 읽는다. Home 상태·렌더러·case·플랫폼 배선·출력 경로를 예시로 제공한다.
작성 절차는 개발자 문서 없이 따를 수 있다.

CI dispatch·exact-base Golden·PR 게시에는 [스크린샷 CI 정책](../../../docs/ci/screenshot-test.md)을 적용한다.
시나리오 수정만으로 dispatch나 게시 권한을 추론하지 않는다. 상태·이벤트 단위 테스트 선택은
[Compose UI 테스트 패턴](../compose-ui-testing-patterns/SKILL.md)을 참고한다.

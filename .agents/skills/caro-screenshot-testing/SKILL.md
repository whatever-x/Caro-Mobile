---
name: caro-screenshot-testing
description: >
  Caro-Mobile의 스크린샷 테스트 코드를 추가·작성·수정·삭제하거나 Android/iOS 및 ko/en 시나리오
  범위를 점검할 때 사용한다. 일반 문서 조회나 ViewModel 단위 테스트만 작성하는 요청에는 적용하지 않는다.
---

# Caro 스크린샷 테스트 작성

화면별 공통 시나리오를 정의한다. Android와 iOS는 해당 화면의 같은 목록을 ko/en으로 실행한다.
기기·언어 값은 각 feature의 `commonTest/ScreenshotCapture.kt`에 Kotlin으로 직접 선언한다.

## 작성 절차

1. **실제 API와 기존 테스트를 읽는다.** 대상 feature의 `src/commonMain/**/*Screen.kt`, `mvi/*State.kt`,
   `src/commonTest/**/*ScreenshotScenario.kt`, `ScreenshotCapture.kt`와 화면별 `*Screenshot*Test.kt`를 확인한다.
   Home은 `HomeScreen(state: HomeState, onIntent: (HomeIntent) -> Unit)`을 사용한다.
   콘텐츠 캡처의 `HomeState.isLoading`은 false로 지정한다(기본값 true). 다른 화면의 파라미터를 Home과 같다고 가정하지 않는다.
2. **시각 계약·고정 데이터·ID를 정한다.** 예: `home.home.with_deck`. 로딩·오류·빈 상태 등 구별할 상태만 추가한다.
   시간·날짜·난수·네트워크·이미지 로더·애니메이션의 변동을 제거한다. Screen에 상태와 콜백을 직접 전달한다.
   fixture 렌더링에 production DI·ViewModel·다른 feature를 연결하지 않는다.
3. **기대 ID를 등록한다.** `scripts/screenshot/scenarios.json`의 해당 모듈 배열에 ID를 추가하거나 삭제한다.
   JSON은 기대 목록일 뿐 Kotlin 테스트를 생성하지 않는다. 등록된 모든 모듈을 통틀어 ID를 고유하게 작성한다.
   `__`, `_actual`·`_compare`·`_before` 접미사는 사용하지 않는다.
4. **화면별 시나리오를 구현한다.** feature의 `commonTest`에 있는 화면별 `***ScreenshotScenario.kt`를 수정한다.
   `ScreenshotScenario`에 ID·렌더러·before/after 단언을 둔다. 해당 화면의 `scenarios()`가 이 목록을 반환하게 한다.
   새 렌더러는 `CaroTheme { Screen(state, onIntent) }`로 작성한다.
   콜백 횟수·비활성화·스크롤 도달 등 필요한 의미 단언도 공통 시나리오에 둔다. 언어마다 새 시나리오 인스턴스로
   카운터를 격리한다. ko/en Screen 복사본을 만들지 않는다.
5. **두 플랫폼을 배선한다.** Android `androidHostTest`는 `ParameterizedRobolectricTestRunner`에서
   화면 팩토리를 `screenshotCaptures`에 전달하고 각 case에 새 Compose rule을 사용한다.
   Home은 `screenshotCaptures("android", HomeScreenshotScenario::scenarios)`를 사용한다.
   iOS 래퍼는 `captureIosScreenshots(HomeScreenshotScenario::scenarios)`처럼 같은 화면 팩토리를 전달한다.
   보조 코드는 해당 feature의 플랫폼 소스셋에 둔다. `ScreenshotCapture.kt`에 `ScreenshotProfile`과
   `ScreenshotConfiguration`을 직접 선언한다. 언어는 `listOf("ko", "en")`이다. 설정 JSON이나 코드 생성은 사용하지 않는다.
6. **파일명·로케일·치수를 지킨다.** 파일명은 `<scenario-id>__<language>__<profile-id>.png`로 만든다.
   플랫폼은 출력 경로에 둔다. Skiko Size는 px다. Density도 전달한다. Android host qualifiers는 feature의 Kotlin 프로필로 계산한다.
   정확한 window px와 Compose density/fontScale을 적용한다. 일반 root의 측정값은 canvas px와 같아야 한다.
   언어·프로필 ID를 바꾸면 세 워크플로의 Bash·jq 기대 파일명 규칙도 함께 수정한다. CI는 파일·결과 목록을 대조한다.
   root·density/fontScale 단언은 Kotlin 테스트에 유지한다. CI가 PNG 치수나 geometry 로그를 검사한다고 보고하지 않는다.
   작은 화면에는 명시된 variant만 사용한다. 320×480 논리 크기/fontScale1.5와 계정탈퇴 스크롤을 유지한다.
   Android Locale·LocaleList·qualifiers와 iOS의 **앱 도메인에 저장된** AppleLanguages 값을 finally에서 복원한다.
   시스템 fallback을 앱 설정으로 저장하지 않는다. Home/Setting은 실제 리소스의 ko/en 텍스트도 단언한다.
7. **Dialog를 명시한다.** 공통 case에 `isDialog = true`를 지정한다. Dialog ID는 feature의 `ScreenshotConfiguration.contentSizedScenarios`에도 등록한다.
   `isDialog()`로 선택한 컴포넌트를 측정·캡처한다. Android에서는 같은 semantics owner의 View가 연결되고 치수가 같은지 확인한다.
   확인한 View를 Roborazzi로 캡처한다. 임의 root 인덱스, 여러 window 합성, 후처리 crop/scale로 맞추지 않는다.
   상호작용 중에는 clock을 진행시켜 스크롤을 완료한다. PNG 캡처 때는 일시정지 상태로 복원한다.
8. **허용된 검증과 한계를 보고한다.** 사용자 실행 경계를 먼저 적용한다. 로컬 스크린샷 중지 지시가 있으면
   record/compare/render/capture와 스크린샷 테스트를 실행하지 않는다. 허용된 컴파일·정적 검사·합성 CI fixture만 실행한다.
   목표 캡처 수와 실제 실행 결과를 구분한다. 캡처가 허용되면 양쪽 ko/en 파일명·측정 치수·원본을 확인한다.
   record 직후 compare 통과는 시각적 승인이 아니다. PNG·보고서는 커밋하지 않는다.

처음 시나리오를 작성하거나 화면별 목록·플랫폼 래퍼를 바꿀 때는
[Home 작성 예제와 명령](references/home-example.md)을 읽는다. Home 상태·렌더러·case·플랫폼 배선·출력 경로를 예시로 제공한다.
작성 절차는 개발자 문서 없이 따를 수 있다.

CI dispatch·exact-base Golden·PR 게시에는 [스크린샷 CI 정책](../../../docs/ci/screenshot-test.md)을 적용한다.
시나리오 수정만으로 dispatch나 게시 권한을 추론하지 않는다. 상태·이벤트 단위 테스트 선택은
[Compose UI 테스트 패턴](../compose-ui-testing-patterns/SKILL.md)을 참고한다.

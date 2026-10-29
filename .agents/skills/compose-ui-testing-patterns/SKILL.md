---
name: compose-ui-testing-patterns
description: >
  Compose UI/상태홀더 동작 테스트(상태 분기, 콜백/Intent 배선, sideEffect 검증, semantics)를
  작성·리뷰할 때 사용한다. "UI 테스트", "ViewModel 테스트", "Compose 테스트", "Turbine", "Kotest로 테스트",
  "상태 검증", "sideEffect 테스트" 맥락에서 발동. Caro의 Kotest + Mokkery + Turbine + KoinTest 스택에 맞춰 각색됨.
---

# Compose / 상태홀더 테스트 패턴

> 출처: chrisbanes/skills (Apache-2.0) · 벤더링 2026-06-09. **JUnit/Espresso/Truth 예시를 Caro 스택
> (Kotest FunSpec + KoinTest + Mokkery + Turbine + kotlinx-coroutines-test)로 전면 각색함.**
> Caro 컨벤션 단일 출처: [`../caro-conventions/SKILL.md`](../caro-conventions/SKILL.md)

## Caro 적용 노트

- Caro의 **1차 테스트 대상은 ViewModel/상태**다(`feature/*/src/commonTest`). MVI 분기·상태 전이·sideEffect를
  `commonTest`에서 검증하면 플랫폼 무관하게 빠르게 돈다.
- 스택: **Kotest** `FunSpec` + `init { }`, **KoinTest**(`KoinExtension`), **Mokkery**(repository 모킹),
  **Turbine**(`sideEffect`/flow), `StandardTestDispatcher` + `Dispatchers.setMain/resetMain` + `runTest`.
- Compose `composeTestRule`은 Android 호스트의 **Robolectric + JUnit4**에서도 사용한다. instrumented 전용으로
  분류하지 않는다. 현재 `androidHostTest`는 JUnit Platform과 Vintage 엔진으로 이 테스트를 실행한다.
- iOS UI 테스트는 `iosSimulatorArm64Test`의 Skiko Compose 테스트를 사용한다. 공통 시나리오의 의미와 플랫폼 실행 API를 구분한다.

## Core principle

동작을 증명하는 **가장 작은 계약**을 테스트한다. ViewModel 상태/sideEffect 테스트와 plain 상태기반 UI 테스트를
선호한다. lifecycle·네비게이션·DI·플랫폼 동작 자체가 검증 대상일 때만 통합 테스트를 추가한다.

## 테스트 대상 선택

| 증명할 것 | 테스트 형태 |
|---|---|
| Intent → 상태 전이, 분기, 로딩/에러 상태 | **ViewModel 단위 테스트**(Kotest + Mokkery) |
| 1회성 이벤트(네비게이션/토스트) 방출 | **ViewModel + Turbine**으로 `sideEffect` 검증 |
| repository 호출/매핑 | repository/datasource 단위 테스트(Mokkery) |
| 텍스트·버튼·로딩/에러 분기·조건부 content | **UI 동작 테스트**(`composeTestRule`의 Android 호스트 또는 iOS Skiko) |
| 시각 레이아웃·타이포·elevation | **스크린샷 테스트**([전용 스킬](../caro-screenshot-testing/SKILL.md)) |
| 네비게이션·lifecycle·DI 통합 | 통합 테스트 |

## ViewModel 상태 테스트 (Caro 기본)

`HomeViewModelTest` 패턴(`feature/home/src/commonTest/kotlin/HomeViewModelTest.kt`)을 따른다:

```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest : FunSpec(), KoinTest {
    init {
        extensions(KoinExtension(listOf(loginModule, testDataModule)))
        val dispatcher = StandardTestDispatcher()
        beforeTest { Dispatchers.setMain(dispatcher) }
        afterTest { Dispatchers.resetMain(); dispatcher.cancel() }

        test("구글 로그인 성공 시 isLoading=false 로 끝난다") {
            runTest {
                val vm: LoginViewModel = get()
                vm.intent(LoginIntent.ClickGoogleLoginButton(successResult))
                advanceUntilIdle()
                vm.state.value.isLoading shouldBe false
            }
        }
    }
}
```

핵심: `runTest { }` 안에서 `intent()` 호출 → `advanceUntilIdle()`로 `launch` 완료 대기 → `state.value` 단언.
`shouldBe`(Kotest matcher)를 사용한다. 성공뿐 아니라 오류·취소·빈 입력·경계값·중복 요청 등 실제 분기를 선택한다.
로딩 시작/종료, 데이터 유지 여부, 오류 이벤트처럼 외부에서 관찰할 수 있는 결과를 단언한다.
구현의 분기표를 테스트에 복사하지 않는다. 호출 한 번을 위해 전체 DI 그래프를 구성하지 않는다.
생성자로 의존성을 전달할 수 있으면 필요한 fake/mock만 사용한다. DI 바인딩 자체가 대상일 때 Koin 구성을 검증한다.

## sideEffect 검증 (Turbine)

1회성 이벤트는 `state`에 없으므로 Turbine으로 flow를 검증한다:

```kotlin
test("로그인 성공 시 NavigateHome sideEffect 방출") {
    runTest {
        val vm: LoginViewModel = get()
        vm.sideEffect.test {
            vm.intent(LoginIntent.ClickGoogleLoginButton(successResult))
            awaitItem() shouldBe LoginSideEffect.NavigateHome
            cancelAndIgnoreRemainingEvents()
        }
    }
}
```

## repository 모킹 (Mokkery)

```kotlin
val authRepository = mock<AuthRepository> {
    everySuspend { loginWithSocial(any(), any()) } returns fakeSession
}
// 호출 검증
verifySuspend { authRepository.loginWithSocial(SocialLoginType.GOOGLE, any()) }
```

## Compose UI 동작 테스트

상태홀더와 UI를 분리했다면 **plain UI composable(Screen)**을 테스트한다. ViewModel·DI·네비게이션을 구성할 필요는 없다:

```kotlin
composeTestRule.setContent {
    CaroTheme {
        ProfileScreen(
            state = ProfileState(name = "Ada", canSave = true),
            onIntent = { intent -> captured = intent },
        )
    }
}
composeTestRule.onNodeWithText("Ada").assertIsDisplayed()
composeTestRule.onNodeWithText("저장").performClick()
captured shouldBe ProfileIntent.ClickSave
```

- **semantics 우선**: `onNodeWithText`, `assertIsEnabled/assertIsNotEnabled`, `assertDoesNotExist`. 안정적 텍스트가
  없을 때만 test tag.
- **콜백/Intent를 캡처해 단언한다**. 노드 존재 외에 콜백 호출이나 상태 변경을 확인한다.
- **interaction 상태**(hover/press/focus)는 `MutableInteractionSource`를 주입해 `emit`. 포인터 이벤트를 시뮬레이션하지 않는다.
- 테스트에서도 `stringResource(Res.string...)`의 리소스 키와 일치하는 실제 문자열을 단언한다.

## 스크린샷과 통합 테스트의 경계

UI 동작 테스트는 표시 상태·활성화 여부·콜백 횟수·Intent 전달을 검증한다. 스크린샷 테스트는 간격·색·타이포 같은
시각 계약을 비교한다. 한 캡처가 두 역할을 함께 맡으면 동작 단언을 유지한다. PNG 존재만으로 동작을 증명하지 않는다.

스크린샷 시나리오·캡처 추가/수정/삭제와 플랫폼 범위 점검은 [caro-screenshot-testing](../caro-screenshot-testing/SKILL.md)을
사용한다. 작성 절차·코드·명령은 해당 스킬의 Home 참고자료, 공통 규칙은
[개발자 지침](../../../docs/development/screenshot-testing.md), CI·Golden·게시는
[스크린샷 CI](../../../docs/ci/screenshot-test.md)가 원본이다.

lifecycle·네비게이션·플랫폼 SDK·DI 조립의 연결 자체가 대상이면 통합 테스트를 추가한다. ViewModel 단위 테스트,
UI 동작 단언, PNG 비교 결과를 실제 로그인·서버 저장·스토어 동작의 완료로 해석하지 않는다.

## 흔한 실수

| 실수 | 수정 |
|---|---|
| 에러 행 테스트에 전체 앱 그래프 구성 | `state = Error`로 plain UI 테스트, 또는 ViewModel 단위 테스트 |
| ViewModel mock을 통해 클릭 동작 테스트 | Intent/콜백을 넘기고 호출 단언 |
| 단순 텍스트 존재에 스크린샷 | semantics 단언 |
| 간격/색/focus ring에 semantics | 스크린샷 테스트 |
| 곳곳에 test tag | 안정적이면 text/contentDescription 우선 |
| `runBlocking`으로 코루틴 테스트 | `runTest` + `advanceUntilIdle` ([`../kotlin-coroutines-structured-concurrency/SKILL.md`](../kotlin-coroutines-structured-concurrency/SKILL.md)) |
| `sideEffect`를 state 단언으로 검증 시도 | Turbine `sideEffect.test { awaitItem() }` |

## 리뷰 시 위험 신호

- 단순 렌더링에 production DI를 쓰는 테스트.
- 액션 후 콜백/상태 변경이 아니라 노드 존재만 확인하는 단언.
- `sideEffect` 방출을 검증하지 않고 네비게이션 동작을 "그냥 될 것"으로 가정.
- `advanceUntilIdle()` 없이 `launch` 결과를 단언(레이스).
- 스크린샷에 랜덤 날짜/시계/원격 이미지/라이브 데이터.

## 관련

- [`../compose-state-holder-ui-split/SKILL.md`](../compose-state-holder-ui-split/SKILL.md) — plain UI(Screen) 테스트 용이성.
- [`../kotlin-flow-state-event-modeling/SKILL.md`](../kotlin-flow-state-event-modeling/SKILL.md) — state/sideEffect 모델.
- [`../kotlin-coroutines-structured-concurrency/SKILL.md`](../kotlin-coroutines-structured-concurrency/SKILL.md) — runTest.
- [`../caro-conventions/SKILL.md`](../caro-conventions/SKILL.md) — Kotest/Mokkery/Turbine 스택.

---
name: compose-ui-testing-patterns
description: >
  Compose UI/상태홀더 동작 테스트(상태 분기, 콜백/Intent 배선, sideEffect 검증, semantics, 스크린샷)를
  작성·리뷰할 때 사용한다. "UI 테스트", "ViewModel 테스트", "Compose 테스트", "Turbine", "Kotest로 테스트",
  "상태 검증", "sideEffect 테스트" 맥락에서 발동. Caro의 Kotest + Mokkery + Turbine + KoinTest 스택에 맞춰 각색됨.
---

# Compose / 상태홀더 테스트 패턴

> 출처: chrisbanes/skills (Apache-2.0) · 벤더링 2026-06-09. **JUnit/Espresso/Truth 예시를 Caro 스택
> (Kotest FunSpec + KoinTest + Mokkery + Turbine + kotlinx-coroutines-test)로 전면 각색함.**
> Caro 컨벤션 단일 출처: [`../_conventions/SKILL.md`](../_conventions/SKILL.md)

## Caro 적용 노트

- Caro의 **1차 테스트 대상은 ViewModel/상태**다(`feature/*/src/commonTest`). MVI 분기·상태 전이·sideEffect를
  `commonTest`에서 검증하면 플랫폼 무관하게 빠르게 돈다.
- 스택: **Kotest** `FunSpec` + `init { }`, **KoinTest**(`KoinExtension`), **Mokkery**(repository 모킹),
  **Turbine**(`sideEffect`/flow), `StandardTestDispatcher` + `Dispatchers.setMain/resetMain` + `runTest`.
- Compose `composeTestRule` UI 테스트는 Android instrumented 경로라 현재 기본 흐름이 아니다. 가능한 한 로직을
  ViewModel/상태로 끌어내려 `commonTest`에서 검증하고, 진짜 레이아웃/시각 계약만 UI/스크린샷 테스트로.

## Core principle

동작을 증명하는 **가장 작은 계약**을 테스트한다. ViewModel 상태/sideEffect 테스트와 plain 상태기반 UI 테스트를
선호한다. lifecycle·네비게이션·DI·플랫폼 동작 자체가 검증 대상일 때만 통합 테스트를 추가한다.

## 테스트 대상 선택

| 증명할 것 | 테스트 형태 |
|---|---|
| Intent → 상태 전이, 분기, 로딩/에러 상태 | **ViewModel 단위 테스트**(Kotest + Mokkery) |
| 1회성 이벤트(네비게이션/토스트) 방출 | **ViewModel + Turbine**으로 `sideEffect` 검증 |
| repository 호출/매핑 | repository/datasource 단위 테스트(Mokkery) |
| 텍스트·버튼·로딩/에러 분기·조건부 content | plain UI Compose 테스트(`composeTestRule`, Android) |
| 시각 레이아웃·타이포·elevation | 스크린샷 테스트 |
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
`shouldBe`(Kotest matcher) 사용.

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

## Compose UI 테스트 (필요 시, Android)

상태홀더/UI 분리가 돼 있으면 **plain UI composable(Screen)**을 테스트한다 — ViewModel·DI·네비게이션 구성 불필요:

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
- **콜백/Intent는 캡처해 단언**: 노드 존재만이 아니라 콜백이 호출됐는지/상태가 바뀌었는지 확인.
- **interaction 상태**(hover/press/focus)는 `MutableInteractionSource`를 주입해 `emit`. 포인터 이벤트 시뮬레이션 금지.
- 문자열은 `stringResource(Res.string...)` 기반이므로 테스트에서도 리소스 키와 일치하는 실제 문자열로 단언.

## 스크린샷 테스트

semantics로 증명 못 하는 시각 계약(간격·테마 색·타이포·elevation)에만. 상태를 결정적으로 고정(고정 데이터,
클록/애니메이션 고정, 네트워크/이미지 로딩은 fake).

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
- [`../_conventions/SKILL.md`](../_conventions/SKILL.md) — Kotest/Mokkery/Turbine 스택.

---
name: compose-state-holder-ui-split
description: >
  Compose 화면 수준 composable이 ViewModel/component를 받아 상태·effect를 수집하고 네비게이션·콜백을 배선하면서
  동시에 레이아웃까지 그릴 때 사용한다. "Route/Screen 분리", "ViewModel을 화면에 직접", "미리보기가 안 돼",
  "상태홀더 분리", "순수 UI composable" 맥락에서 발동. Caro의 Route(배선) / Screen(순수 UI) 패턴과 정확히 일치.
---

# Compose: 상태홀더 / UI 분리

> 출처: chrisbanes/skills (Apache-2.0) · 벤더링 2026-06-09. 예시를 Caro의 Route/Screen + BaseViewModel 패턴으로 각색.
> Caro 컨벤션 단일 출처: [`../_conventions/SKILL.md`](../_conventions/SKILL.md)

## Caro 적용 노트

이 스킬은 Caro의 **Route ↔ Screen 분리 규칙 그 자체**다.

- **Route**(`<Name>Route.kt`): `viewModel.state.collectAsStateWithLifecycle()`, `LaunchedEffect`에서
  `viewModel.sideEffect.collect { }`, `NavigationDispatcher.emit(...)`. 의존성·flow·네비게이션 담당.
- **Screen**(`<Name>Screen.kt`): `@Composable internal fun XScreen(state: XState, onIntent: (XIntent) -> Unit)` —
  순수 UI. ViewModel·flow·네비게이션 없음. 미리보기·테스트 가능해야 함.

## Core principle

상태홀더 배선과 UI 렌더링을 분리한다. 상태홀더 composable은 ViewModel·flow·네비게이션·side effect와 대화한다.
UI composable은 plain 불변 상태 + 콜백을 받아 레이아웃을 기술한다. 화면을 미리보기·테스트·재사용(Android/iOS/CMP) 가능하게 한다.

## 언제 쓰나

Compose 화면이: ViewModel/component/navigator를 직접 받음, 레이아웃과 같은 함수에서 앱/비즈니스 상태나 effect를
수집함, state holder 전체를 자식에 넘김, DI/네비게이션/lifecycle 때문에 미리보기 어려움, 단순 레이아웃 분기 검증에
전체 앱 스택이 필요함.

## 패턴 (Caro)

```kotlin
// Route: 상태홀더 배선
@Composable
fun ProfileRoute(viewModel: ProfileViewModel, navDispatcher: NavigationDispatcher) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.sideEffect.collect { effect ->
            when (effect) {
                is ProfileSideEffect.NavigateBack -> navDispatcher.emit(NavCommand.Back)
            }
        }
    }

    ProfileScreen(state = state, onIntent = viewModel::intent)
}
```

```kotlin
// Screen: 순수 UI (상태홀더를 모름)
@Composable
internal fun ProfileScreen(
    state: ProfileState,
    onIntent: (ProfileIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    // 레이아웃만. CaroTheme 토큰, stringResource 사용.
}
```

## 분리 규칙

| 관심사 | Route(상태홀더) | Screen(UI) |
|---|---|---|
| ViewModel `state` 수집 | 예 | 아니오 |
| 1회성 `sideEffect` 수집 | 예 | 보통 아니오 |
| DI 객체 보유 | 예 | 아니오 |
| 불변 UI 상태 수신 | 보통 통과 | 예 |
| 사용자 이벤트 람다(`onIntent`) | 배선 | 호출 |
| 레이아웃·modifier·semantics | 최소 | 예 |
| UI-로컬 상태(스크롤·포커스·텍스트·애니메이션) | 가끔 seed | 예 |
| 미리보기/스크린샷 친화 | 아닐 수 있음 | 예 |

"UI composable에서 collect 금지"는 앱/비즈니스 상태·effect 스트림 얘기다. plain UI composable은
`rememberScrollState`, `rememberLazyListState`, `FocusRequester`, `TextFieldState`, `MutableInteractionSource` 같은
UI-로컬 프레임워크 상태는 여전히 가질 수 있다(예: `CaroTextField`).

## 무엇을 넘길까

- 진짜 상태가 있으면 여러 무관한 primitive보다 전용 `State` 객체(Caro의 `XState`).
- component 전체 대신 명시적 람다(`onIntent`, `onRetryClick`).
- UI에 비즈니스 규칙을 강제하는 도메인 모델은 UI 밖에. 필요하면 UI 모델로 매핑.
- 네비게이션은 콜백/Intent로. UI는 "뒤로 가기 눌림"이라 말하고 "route X로 가라"가 아니다 → Caro에선 SideEffect → Route.

## 흔한 실수

| 실수 | 수정 |
|---|---|
| `fun Screen(viewModel: VM)`에 레이아웃 전부 | state + 콜백 받는 순수 UI overload 추가 (Caro: Route/Screen 분리) |
| 자식 composable이 `viewModel`/`component`를 받음 | 필요한 state/콜백만 전달 |
| UI composable이 네비게이션 실행 | `onXClick`/Intent 노출, Route에서 emit |
| UI composable이 앱/비즈니스 flow를 collect | Route에서 수집해 값만 내려보냄 |
| 모든 작은 composable에 상태홀더 overload | 화면/섹션 경계에서만 분리, 매 `Row`마다 X |

## 적용 안 되는 경우

- 이미 plain 값/콜백만 받는 작은 일회성 composable.
- `Button`/`Card`/`CaroTextField` 같은 디자인시스템 primitive — 상태홀더가 아니라 슬롯·modifier를 노출
  ([`../compose-slot-api-pattern/SKILL.md`](../compose-slot-api-pattern/SKILL.md)).

## 관련

- [`../compose-ui-testing-patterns/SKILL.md`](../compose-ui-testing-patterns/SKILL.md) — 순수 UI composable 테스트.
- [`../compose-state-hoisting/SKILL.md`](../compose-state-hoisting/SKILL.md) — UI 상태/로직을 어디에 둘지.
- [`../compose-side-effects/SKILL.md`](../compose-side-effects/SKILL.md) — Route의 effect 수집.
- [`../_conventions/SKILL.md`](../_conventions/SKILL.md) — Caro Route/Screen 규칙.

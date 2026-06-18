---
name: compose-side-effects
description: >
  Compose에서 LaunchedEffect, DisposableEffect, SideEffect, rememberCoroutineScope, rememberUpdatedState,
  snapshotFlow, snackbar, navigation, focus 요청, analytics, 이벤트 Flow 수집을 작성·리뷰할 때 사용한다.
  "LaunchedEffect", "DisposableEffect", "sideEffect collect", "effect key", "Unit으로 둬도 돼?",
  "rememberCoroutineScope" 맥락에서 발동. Caro Route의 sideEffect 수집·스낵바·네비게이션에 적용.
---

# Compose: side effects

> 출처: chrisbanes/skills (Apache-2.0) · 벤더링 2026-06-09. Caro Route 패턴 노트 추가.
> Caro 컨벤션 단일 출처: [`../_conventions/SKILL.md`](../_conventions/SKILL.md)

## Caro 적용 노트

- Route에서 1회성 이벤트는 `LaunchedEffect`로 `viewModel.sideEffect.collect { }` 수집(`LoginRoute.kt` 참조).
- 스낵바·소셜로그인 같은 사용자 이벤트 기반 suspend 작업은 `rememberCoroutineScope`에서 launch.
- 화면 상태 자체는 effect로 imperatively 수집하지 말고 `state.collectAsStateWithLifecycle()`로
  ([`../compose-state-holder-ui-split/SKILL.md`](../compose-state-holder-ui-split/SKILL.md)).
- 비-suspend setter를 `LaunchedEffect`에 넣지 말 것 — 잘못된 effect 타입.

## Core principle

Composable body는 UI를 기술한다. recompose·skip·abandon될 수 있다. 바깥 세계를 바꾸는 작업은
그 작업의 lifecycle에 맞는 effect API에 넣는다.

## 가장 작은 effect 고르기

| 필요 | API |
|---|---|
| 성공적 recomposition마다 Compose 상태를 비-Compose 코드에 발행 | `SideEffect` |
| listener/콜백/observer/리소스 등록·해제 | `DisposableEffect(keys...)` |
| suspend·지연·keyed 1회성 작업 | `LaunchedEffect(keys...)` |
| 사용자 이벤트 콜백에서 suspend 작업 시작 | `rememberCoroutineScope()` |
| Compose snapshot 읽기를 코루틴 안 Flow로 | `LaunchedEffect` 안 `snapshotFlow { ... }` |

## Effect key

key는 재시작 정체성을 정의한다. key가 바뀌면 기존 effect가 취소/dispose되고 새로 시작한다.

```kotlin
// ✅ userId 바뀌면 수집 재시작
LaunchedEffect(userId) { repository.events(userId).collect { handle(it) } }

// ❌ Unit이 바뀌는 입력을 숨김 — 수집은 첫 userId를 계속 씀
LaunchedEffect(Unit) { repository.events(userId).collect { handle(it) } }
```

안정적·의미적 key 사용(`userId`, `screenId`). `state`/`viewModel`처럼 광범위한 객체를 한 속성만 중요할 때 쓰지 말 것.

> Caro 참고: Route의 `LaunchedEffect(Unit) { viewModel.sideEffect.collect { } }`는 sideEffect Flow가 화면 lifecycle
> 전체와 동일하므로 `Unit` 키가 맞다. 단 **수집 람다 안에서 바뀌는 입력을 캡처**하면 stale 위험 — `rememberUpdatedState` 검토.

## stale 캡처 피하기

재시작하면 안 되지만 최신 콜백/값이 필요한 장기 effect엔 `rememberUpdatedState`:

```kotlin
@Composable
fun Timeout(onTimeout: () -> Unit) {
    val latestOnTimeout by rememberUpdatedState(onTimeout)
    LaunchedEffect(Unit) { delay(1_000); latestOnTimeout() }
}
```

바뀐 값이 작업을 재시작해야 하면 `rememberUpdatedState`가 아니라 key로 만든다. 또한 `rememberUpdatedState`
delegate를 `remember {}` 블록이나 객체 생성자에서 **즉시 읽으면** 값이 1회만 캡처돼 갱신 안 됨 — 그 땐 `remember(key)` 사용.

## Flow 수집

이벤트/side-effect flow(스낵바·네비게이션·analytics)는 `LaunchedEffect`로:

```kotlin
LaunchedEffect(events) { events.collect { snackbarHostState.showSnackbar(it.message) } }
```

렌더 상태는 effect로 imperatively 수집해 로컬 상태로 옮기지 말 것 — 상태홀더 근처에서 수집해 plain 값으로 내려보냄
([`../compose-state-holder-ui-split/SKILL.md`](../compose-state-holder-ui-split/SKILL.md)).
Android/lifecycle 타깃에선 `collectAsStateWithLifecycle()`, 그 외엔 `collectAsState()`.

Compose 상태 읽기는 `snapshotFlow`:

```kotlin
LaunchedEffect(listState) {
    snapshotFlow { listState.firstVisibleItemIndex }
        .distinctUntilChanged()
        .collect { analytics.visibleIndex(it) }
}
```

`snapshotFlow { }.map { }`만 하고 terminal `collect`가 없으면 아무 일도 안 한다.

## 사용자 이벤트

클릭/제스처가 suspend 작업을 시작하면 `rememberCoroutineScope()`:

```kotlin
@Composable
fun SaveButton(snackbarHostState: SnackbarHostState) {
    val scope = rememberCoroutineScope()
    Button(onClick = { scope.launch { snackbarHostState.showSnackbar("Saved") } }) { Text("Save") }
}
```

`LaunchedEffect`를 트리거하려고 "event flag" 상태를 만들지 말 것. 클릭이 곧 이벤트다.

## 등록과 정리

```kotlin
DisposableEffect(owner, observer) {
    owner.lifecycle.addObserver(observer)
    onDispose { owner.lifecycle.removeObserver(observer) }
}
```

모든 등록 경로엔 대응하는 `onDispose` 정리가 있어야 한다.

## 흔한 실수

| 실수 | 진단 | 수정 |
|---|---|---|
| composable body에서 네트워크 요청 | composition 중 부수작업 | ViewModel/상태홀더로(Caro: `BaseViewModel.launch`); UI-소유 keyed 작업만 `LaunchedEffect` |
| body에서 analytics 속성 쓰기 | composition 중 부수작업 | 매 성공 recomposition 후면 `SideEffect` |
| `LaunchedEffect(Unit)`이 바뀌는 `id` 캡처 | key 누락 | `id`로 key, 또는 재시작 막아야 하면 `rememberUpdatedState` |
| `LaunchedEffect(...) { nonSuspendSetter() }` | 잘못된 effect 타입 | 보통 `SideEffect` |
| `LaunchedEffect`에서 listener 추가 후 정리 없음 | dispose 누락 | `DisposableEffect` |
| 클릭에서 `shouldShowSnackbar = true`로 launch | event flag 안티패턴 | 클릭 콜백에서 `rememberCoroutineScope()` |

## 리뷰 시 위험 신호

- composable body 코드에 대해 "이건 한 번만 실행돼".
- 바뀌는 파라미터가 있는 함수의 `LaunchedEffect(Unit)`.
- terminal collect 없는 effect 안 flow 체인.
- lifecycle을 모델링하지 않고 lint 침묵용으로 고른 effect key.
- key도 `rememberUpdatedState`도 없이 장기 effect에서 쓰는 콜백 람다.

## 관련

- [`../compose-state-authoring/SKILL.md`](../compose-state-authoring/SKILL.md) — 로컬 상태 작성.
- [`../compose-state-holder-ui-split/SKILL.md`](../compose-state-holder-ui-split/SKILL.md) — Route/Screen 분리, 상태 수집.
- [`../kotlin-flow-state-event-modeling/SKILL.md`](../kotlin-flow-state-event-modeling/SKILL.md) — 이벤트 flow 모델링.
- [`../_conventions/SKILL.md`](../_conventions/SKILL.md) — Caro Route sideEffect 수집.

---
name: compose-state-authoring
description: >
  Compose에서 @Composable 안의 bare local var, remember { mutableStateOf(...) },
  mutableStateListOf/mutableStateMapOf, @ReadOnlyComposable 을 작성·리뷰할 때 사용한다.
  "remember", "mutableStateOf", "상태가 리셋돼", "ReadOnlyComposable", "로컬 상태" 맥락에서 발동.
  Caro의 Screen/디자인시스템 컴포넌트의 로컬 UI 상태 작성에 적용.
---

# Compose 상태 작성

> 출처: chrisbanes/skills (Apache-2.0) · 벤더링 2026-06-09. 원문 유지.
> Caro 컨벤션 단일 출처: [`../_conventions/SKILL.md`](../_conventions/SKILL.md)

## Caro 적용 노트

- 화면 수준 상태는 `BaseViewModel`의 `state`/`reduce`에 둔다. 이 스킬은 **순수 UI 로컬 상태**
  (텍스트 포커스, 펼침 여부 등 Screen/컴포넌트 내부 상태)에만 적용된다.
- `CaroTextField`처럼 디자인시스템 컴포넌트는 `interactionSource = remember { MutableInteractionSource() }`,
  `collectIsFocusedAsState()`를 쓴다(이미 올바른 패턴).
- 비즈니스 데이터가 얽히면 이 스킬이 아니라 [`../compose-state-hoisting/SKILL.md`](../compose-state-hoisting/SKILL.md)로.

이 스킬이 다루는 범위: **로컬 UI 상태**(`remember { mutableStateOf(…) }`, `mutableStateListOf`/`mutableStateMapOf`)와
**`@ReadOnlyComposable`**. 코루틴 스코프/effect는 [`../compose-side-effects/SKILL.md`](../compose-side-effects/SKILL.md)로.

## Core principle

`@Composable`은 입력이 바뀌면 런타임이 다시 실행하는 함수다. 로컬 상태를 올바로 쓰려면 두 질문:

1. **가변 로컬 상태** — 내 `var`가 recomposition을 견디고 *동시에* recomposition을 유발하는가? 아니면 매번 리셋되고 쓰기가 보이지 않는다.
2. **이 composable은 어떤 종류인가?** — composition을 *변경*(노드 배치, slot 할당, `remember`)하나, 아니면 *읽기*만 하나? 읽기만 하면 `@ReadOnlyComposable`로 작업을 건너뛸 수 있다.

## 1. composable 안의 var 는 State-backed 여야

```kotlin
// ❌ BAD — 매 recomposition마다 count 리셋, 클릭이 UI에 반영 안 됨
@Composable
fun Counter() {
    var count = 0
    Button(onClick = { count++ }) { Text("$count") }
}

// ✅ GOOD — remember(생존) + mutableStateOf(유발)
@Composable
fun Counter() {
    var count by remember { mutableStateOf(0) }
    Button(onClick = { count++ }) { Text("$count") }
}
```

같은 규칙이 `Row { }`/`Column { }` 같은 **content 람다 안에도** 적용된다(그것도 `@Composable`).

컬렉션은 `mutableStateListOf`/`mutableStateMapOf`를 선호. `remember { mutableStateOf(mutableListOf()) }` 후
`list.add(x)`는 recompose하지 않는다(State setter를 안 거침) — 값을 교체(`state = state + x`)하거나 `mutableStateListOf` 사용.

### 합성 중 snapshot state 되쓰기(back-writing) 금지

```kotlin
// ❌ BAD — 매 합성마다 clear + putAll
val merged = remember { mutableStateMapOf<Key, ViewState>() }
merged.clear(); merged.putAll(parent); merged.putAll(overlay)

// ✅ GOOD — 입력으로부터 불변 스냅샷 remember
val merged = remember(parent, overlay) {
    if (overlay.isEmpty()) parent else parent + overlay
}
```

### 적용 안 되는 경우

- `remember { }` producer 블록 안(키 변경 시 1회 실행) — 로컬 `var` 정상.
- composable 밖으로 전달되는 비-`@Composable` 람다(`onClick = { var a = 0 }`) — 평범한 Kotlin.
- 평범한 헬퍼 함수.

## 2. @ReadOnlyComposable 계약

`@ReadOnlyComposable`은 composition state를 *읽기만* 한다고 선언한다 — `Text`/`Box`/`remember`/레이아웃 노드/effect 없음.
런타임이 그룹 할당을 건너뛸 수 있어 빠른 accessor(`CaroTheme.color.*` 같은 토큰 접근자)에 유용.

계약은 양방향:

- body의 모든 composable 호출이 `@ReadOnlyComposable`이거나(또는 호출 자체가 없으면) **추가**.
- 비-read-only composable을 하나라도 호출하면 **금지**.

```kotlin
// ✅ GOOD — composition local만 읽고 레이아웃/remember 없음
@Composable
@ReadOnlyComposable
fun appSpacing(): Dp = LocalDimensions.current.spacing

// ❌ BAD — read-only 선언인데 Box 배치
@Composable @ReadOnlyComposable
fun Header(): Int { Box {}; return 42 }
```

## Quick reference

| 증상 | 진단 | 수정 |
|---|---|---|
| `@Composable fun` body의 `var x = …` | recomposition-safe 아님 | `var x by remember { mutableStateOf(…) }` |
| content 람다 안 `var x = …` | 동일 | 동일 |
| `remember { mutableStateOf(list) }` 후 `.add(x)` 미반영 | State setter 우회 | `mutableStateListOf` 또는 값 교체 |
| body에서 `stateMap.clear(); putAll(...)` | composition→composition 되쓰기 | `remember(keys) { derivedSnapshot }` |
| `Text`/`Box`/`remember`/effect 없는 `@Composable fun` | `@ReadOnlyComposable` 후보 | 추가 |
| `Box {}` 호출하는 `@ReadOnlyComposable` | 계약 위반 | 제거 |

## 리뷰 시 위험 신호

| 생각 | 현실 |
|---|---|
| "작은 composable이라 bare var 괜찮아" | recomposition은 언제든 발생. 리셋은 비결정적 — 나중에 버그 리포트 하나. |
| "단순해 보여서 @ReadOnlyComposable 붙였어" | 기준은 "단순"이 아니라 "read-only 호출만 한다". |
| "그냥 리스트에 .add() 할래" | `mutableStateOf(List)`는 내부 변경을 관찰 안 함 — `mutableStateListOf`/값 교체. |

## 관련

- [`../compose-side-effects/SKILL.md`](../compose-side-effects/SKILL.md) — LaunchedEffect/DisposableEffect/rememberCoroutineScope 등.
- [`../compose-state-hoisting/SKILL.md`](../compose-state-hoisting/SKILL.md) — 상태를 어디에 둘지.
- [`../_conventions/SKILL.md`](../_conventions/SKILL.md) — Caro MVI/디자인시스템.

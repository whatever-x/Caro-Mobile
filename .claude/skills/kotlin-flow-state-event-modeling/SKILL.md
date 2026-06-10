---
name: kotlin-flow-state-event-modeling
description: >
  StateFlow, MutableStateFlow.update, SharedFlow, Channel, stateIn, SharingStarted, .value,
  receiveAsFlow, 1회성 이벤트, sentinel 초기값 등 Kotlin Flow 상태/이벤트 API를 작성·리뷰할 때 사용한다.
  "StateFlow", "SharedFlow", "Channel", "1회성 이벤트", "navigation 이벤트", "sentinel/placeholder 상태",
  "update vs value", "stateIn" 맥락에서 발동. Caro는 state=MutableStateFlow, sideEffect=Channel(BUFFERED) 패턴.
---

# Kotlin Flow: 상태/이벤트 모델링

> 출처: chrisbanes/skills (Apache-2.0) · 벤더링 2026-06-09. 원문 유지 + Caro(BaseViewModel) 매핑 추가.
> Caro 컨벤션 단일 출처: [`../_conventions/SKILL.md`](../_conventions/SKILL.md)

## Caro 적용 노트

`BaseViewModel`이 이미 올바른 선택을 내장한다:

- **상태**: `private val _state = MutableStateFlow(initial); val state = _state.asStateFlow()` — 항상 값이 있고 동기 `.value` 가능.
- **1회성 이벤트**: `private val _sideEffect = Channel<SE>(capacity = BUFFERED, onBufferOverflow = SUSPEND); val sideEffect = _sideEffect.receiveAsFlow()` — 정확히-한-소비자(Route) 핸드오프. **이것이 본 스킬이 권장하는 패턴이다.** 네비게이션/토스트에 `SharedFlow`를 새로 쓰지 말 것.
- 상태 변경은 `reduce { copy(...) }`. (아래 `update { }` 원칙과 동일 정신 — read/modify/write 직접 대입 회피.)

## Core principle

**replay·fan-out·동기 읽기 요구에 맞는 primitive를 고른다.** `StateFlow`, `SharedFlow`, `Channel` 기반 flow,
cold `Flow`는 버퍼링·누가 각 emission을 보는가·`.value` 존재 여부가 다르다. 잘못 고르면 이벤트를 잃거나,
sharing 코루틴을 누수하거나, 가짜 도메인 sentinel을 상태에 강제한다.

## 1회성 이벤트엔 SharedFlow보다 Channel

`SharedFlow` 기본값은 replay 버퍼가 없다. emission 순간에 아무도 collect 안 하면 이벤트는 사라진다.
**단일 UI 소비자**의 정확히-한-번 이벤트(네비게이션/스낵바)엔 버퍼드 `Channel`을 Flow로 노출하는 게 더 맞다:

```kotlin
// ❌ BAD
private val _navEvents = MutableSharedFlow<NavigationEvent>()
val navEvents: SharedFlow<NavigationEvent> = _navEvents.asSharedFlow()

// ✅ GOOD (Caro의 BaseViewModel.sideEffect가 정확히 이 형태)
private val _navEvents = Channel<NavigationEvent>(Channel.BUFFERED)
val navEvents: Flow<NavigationEvent> = _navEvents.receiveAsFlow()
```

`Channel.receiveAsFlow()`는 **fan-out(브로드캐스트 아님)**: 여러 collector면 각 이벤트는 하나에게만 간다.
모든 관찰자가 같은 이벤트를 봐야 하면 명시적 상태나 의도적으로 설정한 `SharedFlow`를 쓴다.

## sentinel 기본값으로 오염된 StateFlow

`StateFlow`는 초기값을 강제한다. 실제 값이 비동기일 때 가짜 도메인 값(`NoUser`, placeholder ID)을 만들면
모든 소비자가 그 sentinel을 진짜 데이터로 다뤄야 한다. → 부재/로딩/에러가 진짜 상태면 명시적으로 모델링
(`User?`, `sealed interface UiState`), 아니면 phasing(실제 값이 생길 때까지 노출 안 함).

## update { } 로 변경

`.value`를 읽고 다시 쓰는 대신 `MutableStateFlow.update { current -> ... }`를 선호한다. 여러 코루틴이 같은 상태를
변경할 때 lost update를 막는다.

```kotlin
// BAD — read/modify/write
_state.value = _state.value.copy(selectedId = id, details = details)

// GOOD — 최신 상태에서 변환
_state.update { it.copy(selectedId = id, details = details) }
```

update 람다는 재시도될 수 있으니, 현재 상태에 의존하지 않는 비싼 객체 생성은 블록 밖에서 한다.

> Caro 참고: ViewModel 안에서는 `reduce { copy(...) }`를 쓴다(`BaseViewModel`이 단일 변경 경로를 제공).
> repository/세션 등 ViewModel 밖의 `MutableStateFlow`에는 위 `update { }` 규칙을 직접 적용한다.

## 함수 안의 stateIn()

```kotlin
// ❌ BAD — 호출마다 새 sharing 코루틴
fun getPreferences(): StateFlow<Prefs> =
    repo.prefsFlow.stateIn(scope, SharingStarted.Eagerly, Prefs.Default)

// ✅ GOOD — 한 번 계산되는 공유 인스턴스
val preferences: StateFlow<Prefs> =
    repo.prefsFlow.stateIn(viewModelScope, SharingStarted.Eagerly, Prefs.Default)
```

## WhileSubscribed + 동기 .value

`WhileSubscribed(timeout)`는 활성 collector가 없으면 upstream을 끊고, 그동안 `.value`는 stale일 수 있다.
`.value`가 항상 신선해야 하면 `SharingStarted.Eagerly`나 명시적 초기화. stale/cached가 허용될 때만 `WhileSubscribed`.

## StateFlow.map 은 .value 를 잃는다

```kotlin
// ❌ BAD — 이제 평범한 Flow, name.value 컴파일 안 됨
val name: Flow<String> = userState.map { it.name }

// ✅ GOOD — .stateIn 으로 종료
val name: StateFlow<String> = userState.map { it.name }
    .stateIn(viewModelScope, SharingStarted.Eagerly, userState.value.name)
```

## 결정: 어떤 Flow 타입?

| 필요 | primitive |
|------|-----------|
| 항상 값이 있고, 비동기 collector와 동기 코드 둘 다 읽음 | `StateFlow` (`.value` 중요하면 `Eagerly`) |
| hot 스트림, 다중 구독, 동기 `.value` 불필요 | `SharedFlow` |
| 한 소비자, 정확히-한-번 핸드오프 | `Channel(BUFFERED).receiveAsFlow()` (Caro sideEffect) |
| cold 스트림, collect마다 한 소비자 | 평범한 `Flow` |

## Quick reference

| 증상 | 문제 | 수정 |
|---------|---------|-----|
| `MutableStateFlow<X>(FakeDomainValue)` | 잘못된 placeholder 기본값 | 부재 명시 모델링 / phasing |
| 단일소비자 nav/snackbar에 `MutableSharedFlow` | 손실 가능 이벤트 스트림 | `Channel(BUFFERED).receiveAsFlow()` |
| `fun foo() = flow.stateIn(...)` | 호출마다 sharing 코루틴 | `val`/공유 인스턴스로 |
| `WhileSubscribed` + 신선해야 하는 `.value` | stale/초기 데이터 | `Eagerly`/명시 초기화 |
| `stateFlow.map { }`를 상태로 소비 | `.value` 상실 | `.stateIn(...)`으로 종료 |
| `_state.value = _state.value.copy(...)` | 비원자적 RMW | `update { it.copy(...) }` (ViewModel은 `reduce`) |

## 리뷰 시 위험 신호

| 생각 | 현실 |
|---------|---------|
| "구독자가 여럿이라 SharedFlow가 필요해" | 다중 구독자는 시맨틱을 바꾼다. `Channel.receiveAsFlow()`는 브로드캐스트 아님. 의도적으로 선택. |
| "리소스 아끼려 WhileSubscribed 쓸래" | stale/초기 `.value`가 허용될 때만. 먼저 확인. |
| "데이터 로드 전까지 sentinel 쓸래" | 소비자가 진짜 도메인으로 취급. 명시 모델링/phasing 선호. |

## 관련

- [`../kotlin-coroutines-structured-concurrency/SKILL.md`](../kotlin-coroutines-structured-concurrency/SKILL.md) — 스코프 소유, 취소, runBlocking.
- [`../compose-side-effects/SKILL.md`](../compose-side-effects/SKILL.md) — Compose에서 이벤트 flow 수집.
- [`../compose-state-holder-ui-split/SKILL.md`](../compose-state-holder-ui-split/SKILL.md) — 상태홀더가 UI에 flow를 노출하는 지점.
- [`../_conventions/SKILL.md`](../_conventions/SKILL.md) — BaseViewModel state/sideEffect 형태.

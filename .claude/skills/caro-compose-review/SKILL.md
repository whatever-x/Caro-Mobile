---
name: caro-compose-review
description: >
  Caro-Mobile(KMP + Compose Multiplatform)의 PR/diff/파일을 Compose·Kotlin·MVI·KMP 컨벤션 정합성으로 리뷰한다.
  벤더링한 Compose/Kotlin/KMP 스킬들의 red-flag 체크리스트를 우리 규칙(MVI·BaseViewModel·Koin·Navigation3·
  Compose resources·expect/actual·Kotest)에 맞춰 적용한다. "컴포즈 리뷰", "Compose 리뷰", "UI 리뷰",
  "상태/사이드이펙트 점검", "MVI 리뷰", "/caro-compose-review" 같은 요청 시 사용한다. 직접 호출도 가능.
  일반 버그·정확성 리뷰는 code-review:code-review와 보완 관계(대체 아님).
---

# Caro Compose / KMP 컨벤션 리뷰

> Caro 컨벤션 단일 출처: [`../_conventions/SKILL.md`](../_conventions/SKILL.md)
> 근거 스킬: 벤더링한 `compose-*`, `kotlin-*` 스킬들(같은 `.claude/skills/` 디렉토리). 항목마다 해당 스킬을 인용한다.
> 실제 적용 예시: [`examples/pr-65-create-profile.md`](examples/pr-65-create-profile.md) (PR #65에 직접 돌린 결과).

## 목적과 경계

- **이 스킬**: Compose/Kotlin/MVI/KMP **컨벤션 정합성**(상태 위치, side-effect API, MVI 구조, 안정성, 슬롯, expect/actual,
  Compose resources, Koin DSL, Navigation3 등록, Kotest 패턴).
- **`code-review:code-review`**: 일반 버그·정확성·단순화. → 대체하지 말고 **함께** 쓴다.
- 코드를 수정하지 않고 **지적·근거 제시**가 기본. 사용자가 "고쳐줘"라고 하면 그때 수정.

## 사용 방법

1. 대상 diff 확보: `git diff origin/develop...HEAD`(PR) 또는 `git diff`(작업트리), 또는 사용자가 지정한 파일.
2. 변경된 `*.kt`만 추려서 아래 체크리스트로 점검. 각 발견은 `path/to/file:line` + 위반 규칙 + 근거 스킬 링크 + 제안.
3. 심각도 표기: **🔴 must-fix**(컨벤션 위반/버그 위험), **🟡 should-fix**(개선 권장), **🟢 nit**.
4. 위반이 없으면 "해당 영역 통과"라고 명시. 추측으로 만들어내지 말 것.

## 체크리스트

### A. MVI 구조 (`_conventions`)
- [ ] ViewModel이 `BaseViewModel<S, I, SE>`를 상속하고 `handleIntent`로 분기하는가.
- [ ] 상태 변경이 `reduce { copy(...) }`인가. `_state.value =` 직접 대입(🔴) 없는가.
- [ ] 1회성 이벤트(네비게이션/토스트)가 state 플래그가 아니라 `postSideEffect`인가.
- [ ] 비동기 작업이 별도 `CoroutineScope`가 아니라 `launch { }`(viewModelScope)인가. → `kotlin-coroutines-structured-concurrency`
- [ ] 파일 레이아웃이 `route/`, `mvi/`, `di/` 규칙을 따르는가.

### B. Route / Screen 분리 (`compose-state-holder-ui-split`)
- [ ] Screen이 `@Composable internal fun XScreen(state, onIntent)` 순수 UI인가. ViewModel/flow/네비게이션을 직접 받지 않는가(🔴).
- [ ] Route가 `collectAsStateWithLifecycle` + `LaunchedEffect { sideEffect.collect }` + `NavigationDispatcher.emit`를 담당하는가.
- [ ] 자식 composable에 ViewModel/component 전체를 넘기지 않는가.

### C. 상태 위치 / 작성 (`compose-state-hoisting`, `compose-state-authoring`)
- [ ] `@Composable` 본문의 `var`가 `remember { mutableStateOf() }`로 backing 되는가(🔴 아니면 리셋).
- [ ] 비즈니스 데이터/repository 입력이 로컬 remember가 아니라 ViewModel에 있는가.
- [ ] 컬렉션 상태가 `mutableStateListOf`/값 교체인가(`mutableStateOf(list).add` 아님).
- [ ] composition 중 snapshot state back-writing(매 합성 `clear()`/`putAll`)이 없는가.

### D. Side effects (`compose-side-effects`)
- [ ] `LaunchedEffect`의 key가 적절한가. 바뀌는 입력을 `Unit`으로 숨기지 않는가(🔴 stale).
- [ ] 사용자 클릭의 suspend 작업이 `rememberCoroutineScope`인가. event-flag 안티패턴 없는가.
- [ ] `DisposableEffect` 등록에 `onDispose` 정리가 있는가.
- [ ] composable 본문에서 네트워크/부수작업을 직접 실행하지 않는가.

### E. Flow / 동시성 (`kotlin-flow-state-event-modeling`, `kotlin-coroutines-structured-concurrency`)
- [ ] Repository/DataSource가 `suspend`만 노출하고 스코프를 저장하지 않는가(🔴).
- [ ] `try/catch`가 `CancellationException`을 재전파하는가.
- [ ] `runBlocking`이 앱/테스트 코드에 없는가(테스트는 `runTest`).
- [ ] sentinel placeholder를 진짜 도메인 값처럼 쓰지 않는가.

### F. 디자인시스템 / 슬롯 (`compose-slot-api-pattern`)
- [ ] 재사용 컴포넌트가 primitive content/`showXxx` 플래그 대신 `@Composable` 슬롯을 쓰는가.
- [ ] 색/타이포/간격/모양이 raw 값이 아니라 `CaroTheme.color/typography/spacing/shape` 토큰인가(🟡~🔴).
- [ ] 선택 슬롯이 `(@Composable () -> Unit)? = null`인가.

### G. Compose resources (`_conventions`)
- [ ] 사용자 노출 텍스트·접근성 라벨이 `stringResource(Res.string...)`인가. **하드코딩 문자열(🔴)** 없는가(임시 디버그 제외).
- [ ] 이미지가 `painterResource(Res.drawable...)`인가.

### H. Navigation3 (`_conventions`)
- [ ] 새 화면이 4단계를 모두 따랐는가: ① `@Serializable NavKey` 정의 ② `CaroApp.kt` polymorphic 등록
      ③ `NavigationModule.kt`의 `navigation<Key> { }` ④ Route/Screen/ViewModel/MVI.
- [ ] 파라미터가 `@Serializable Payload`로 전달되고, 파라미터 화면이 `koinViewModel { parametersOf(navKey) }`인가.
- [ ] feature 모듈끼리 직접 의존하지 않는가(navigator 경유).

> 4단계 등록 확인용 grep (`<Entry>`를 NavKey 이름으로 치환, 예 `CreateProfileEntry`):
> ```bash
> grep -rn '<Entry>' core/navigator/                         # ① NavKey 정의(@Serializable)
> grep -rn 'subclass(<Entry>' composeApp/                     # ② CaroApp.kt polymorphic 등록
> grep -rn 'navigation<<Entry>>' composeApp/.../NavigationModule.kt  # ③ navigation<Key> { }
> ```
> 셋 중 하나라도 결과가 없으면 등록 누락(🔴). 빌드 산출물 오탐을 줄이려면 `--include='*.kt'`를 붙인다.

### I. Koin DI (`_conventions`)
- [ ] 단순 바인딩이 단축 DSL(`viewModel<X>()`, `single<X>() bind Y::class`)인가.
- [ ] `named()`/빌더/비-생성자 로직일 때만 평범한 DSL을 쓰는가.

### J. 멀티플랫폼 (`kotlin-multiplatform-expect-actual`)
- [ ] `commonMain` 시그니처에 플랫폼 타입(`Context`, `Activity`, `Uri`, `UIViewController`)이 없는가(🔴).
- [ ] 플랫폼 분기가 `expect`/`actual` + `*.android.kt`/`*.ios.kt`인가.
- [ ] DI/테스트가 필요한 경계가 `expect class`가 아니라 인터페이스인가.

### K. 테스트 (`compose-ui-testing-patterns`)
- [ ] ViewModel 테스트가 Kotest `FunSpec` + `KoinTest` + `StandardTestDispatcher` + `runTest`/`advanceUntilIdle`인가.
- [ ] `sideEffect`를 Turbine으로 검증하는가(state 단언으로 우회 안 함).
- [ ] `advanceUntilIdle()` 없이 `launch` 결과를 단언하는 레이스가 없는가.

## 출력 형식

```text
## caro-compose-review 결과

### 🔴 Must-fix
- `feature/x/XScreen.kt:42` — 하드코딩 문자열 "저장". Compose resources 규칙 위반.
  근거: compose 리뷰 G / _conventions. 제안: stringResource(Res.string.x_save).

### 🟡 Should-fix
- ...

### 🟢 Nit
- ...

### 통과
- MVI 구조, Navigation3 등록: 위반 없음.
```

## 적용 안 되는 경우

- 비-Compose/비-Kotlin 변경(빌드 스크립트만, 문서만 등).
- 일반 로직 버그 헌팅은 `code-review:code-review` 또는 `/code-review`로.

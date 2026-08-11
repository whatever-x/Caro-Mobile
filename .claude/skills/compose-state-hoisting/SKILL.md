---
name: compose-state-hoisting
description: >
  Compose UI 상태/로직을 어디에 둘지(로컬 remember, hoisted 파라미터, plain state holder,
  화면 수준 ViewModel) 결정할 때 사용한다. "상태 끌어올리기", "state hoisting", "remember 어디",
  "이 상태 ViewModel로?", "state holder" 맥락에서 발동. Caro는 화면 상태=BaseViewModel, UI 로컬 상태=Screen/holder.
---

# Compose 상태 끌어올리기 (state hoisting)

> 출처: chrisbanes/skills (Apache-2.0) · 벤더링 2026-06-09. Android 전용 표현(Activity recreation)을 CMP-안전 표현으로 각색.
> Caro 컨벤션 단일 출처: [`../_conventions/SKILL.md`](../_conventions/SKILL.md)

## Caro 적용 노트

- **비즈니스 데이터·repository 호출·화면 UI 상태 생산은 `BaseViewModel`**(화면 수준 상태홀더)에 둔다.
  `state`/`reduce`/`postSideEffect`가 그 자리. → [`../compose-state-holder-ui-split/SKILL.md`](../compose-state-holder-ui-split/SKILL.md)
- 순수 UI 요소 상태(스크롤·포커스·펼침·텍스트 편집)는 Screen/컴포넌트 로컬에 둔다.
- repository 기반 화면 상태를 만드는 입력(예: 닉네임 중복확인을 부르는 텍스트)은 ViewModel로 올린다.
- **`CaroTextField`처럼 재사용 컴포넌트의 `interactionSource`, focus, 텍스트는 컴포넌트 로컬**에 두고
  hoisting은 정말 부모가 그 동작을 조율할 때만.

## Core principle

상태는 로직이 필요한 만큼만 끌어올린다. 단순 UI 요소 상태는 로컬에 두고, 공유되는 UI 요소 상태는 가장 낮은 공통
composable 소유자로 올리고, UI-전용 동작이 개념이 되면 plain state holder를 추출하고, 비즈니스 로직·앱 데이터가
얽히면 화면 상태홀더(Caro의 `BaseViewModel`)를 쓴다.

## 결정 가이드

| 상황 | 소유자 |
|---|---|
| 한 composable이 단순 상태를 읽고 씀 | `remember` / `rememberSaveable`로 로컬 유지 |
| 형제/부모 composable이 읽고 써야 함 | 가장 낮은 공통 조상으로 상태와 이벤트 hoist |
| 연관 UI 요소 상태 + UI 로직이 composable을 읽기/미리보기/테스트 어렵게 만듦 | composition에 remember된 plain state holder 클래스 추출 |
| repository 호출·영속·비즈니스 규칙·화면 UI 상태 생산이 얽힘 | 화면 수준 상태홀더(Caro `BaseViewModel`) |

## plain state holder 추출 트리거

다음이 여럿 성립하면 추출: 같은 콜백이 조율하는 다수의 `remember` 값, 스크롤/포커스/텍스트/선택/시트 상태가
`clear`/`submit`/`jumpToTop` 같은 명명 연산을 필요로 함, 파생 UI 플래그가 흩어짐, 자식이 개념상 소유하지 않는
메커니즘을 받음. **boolean 하나, 텍스트 필드 하나, 단순 show/hide엔 추출하지 말 것** — 의례는 관심사 분리가 아니다.

## 패턴

```kotlin
@Stable
class ProductSearchState(
    query: String,
    private val listState: LazyListState,
    private val focusRequester: FocusRequester,
) {
    var query by mutableStateOf(query); private set
    var filtersOpen by mutableStateOf(false); private set
    val canClear: Boolean get() = query.isNotEmpty()

    fun updateQuery(value: String) { query = value }
    fun clear() { query = ""; focusRequester.requestFocus() }
    suspend fun jumpToTop() { listState.animateScrollToItem(0) }
}

@Composable
fun rememberProductSearchState(
    initialQuery: String = "",
    listState: LazyListState = rememberLazyListState(),
    focusRequester: FocusRequester = remember { FocusRequester() },
): ProductSearchState = remember(listState, focusRequester) {
    ProductSearchState(initialQuery, listState, focusRequester)
}
```

부모가 같은 UI 동작을 조율해야 하면 holder를 기본값 있는 파라미터로 받는다.

## composition 소유권

`remember`로 만든 plain state holder는 composable lifecycle을 따른다. `LazyListState`, `FocusRequester`,
`PagerState`, `DrawerState`, `TextFieldState` 같은 Compose UI 객체의 좋은 집이다.

frame clock이 필요한 suspend UI 연산(스크롤/드로어 애니메이션)은 composition-스코프 코루틴
(`rememberCoroutineScope`, `LaunchedEffect`)에 둔다. **이런 호출을 `viewModelScope`로 옮기지 말 것.**

## 상태 저장

`rememberSaveable`이나 커스텀 `Saver`는 **구성 재생성(프로세스/화면 재생성)을 살아남아야 하는 값**
(쿼리 문자열, 선택된 필터 ID, 현재 탭 키)에만 쓴다.

> Caro/CMP 참고: Android에선 Activity/프로세스 재생성, 데스크탑·iOS 타깃에서도 동일한 "재생성 생존" 의미다.
> 화면 간 영속이 필요한 파라미터는 Navigation3의 `@Serializable NavKey`/`Payload`로 전달한다
> ([`../_conventions/SKILL.md`](../_conventions/SKILL.md)). `LazyListState`/`FocusRequester`/코루틴 스코프/콜백 같은
> 런타임 객체를 직접 저장하려 하지 말 것 — 동작을 재구성할 최소 직렬화 값만 저장.

## 흔한 실수

| 실수 | 수정 |
|---|---|
| "혹시 몰라" 모든 로컬 상태를 부모로 올림 | 실제로 읽고 쓰는 가장 낮은 소유자로만 |
| boolean 하나에 plain state holder 추출 | 단순 private UI 상태는 로컬 유지 |
| Compose state holder에 repository 호출/제품 규칙 | 화면 상태홀더(`BaseViewModel`)로 이동 |
| 텍스트/선택이 repository 기반 화면 상태를 구동하는데 로컬 유지 | 비즈니스 로직 있는 화면 상태홀더로 이동 |
| holder를 무관한 자식 깊이 전달 | 자식이 정말 holder 동작을 조율하지 않으면 plain 값/콜백 전달 |
| 애니메이션 suspend를 `viewModelScope`에서 호출 | composition-스코프 코루틴 사용 |

## 관련

- [`../compose-state-authoring/SKILL.md`](../compose-state-authoring/SKILL.md) — 올바른 로컬 remember/가변 상태.
- [`../compose-state-holder-ui-split/SKILL.md`](../compose-state-holder-ui-split/SKILL.md) — 화면 상태홀더 배선과 UI 렌더 분리.
- [`../compose-side-effects/SKILL.md`](../compose-side-effects/SKILL.md) — effect API와 composition-스코프 코루틴.
- [`../_conventions/SKILL.md`](../_conventions/SKILL.md) — Caro MVI/Navigation3.

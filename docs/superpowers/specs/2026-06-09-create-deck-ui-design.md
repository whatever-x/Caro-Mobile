# 덱 만들기 (Create Deck) UI — 설계 문서

- 작성일: 2026-06-09
- 브랜치: `feat/deck/create-deck` (worktree, based on `develop`)
- Figma: https://www.figma.com/design/PVw3ZFpRQf2JL3gv2AVOg9/Caro-Design?node-id=1859-7184
  - 대상 프레임: `2429:100206` "덱 만들기 - 설명 작성" (스티키 노트: "`홈 화면`의 덱 만들기와 동일")

## 1. 목표 / 범위

덱을 생성하는 입력 화면을 KMP(Compose Multiplatform) 기반으로 구현한다.

**범위 (확정):**
- UI + ViewModel + MVI 구현.
- 실제 덱 생성 서버 API는 아직 없음 → `ClickConfirm` 처리는 `// TODO: 덱 생성 API 연동` 주석과 함께 `NavigateBack` side effect로 마감. Repository/Remote 레이어는 이번 범위 제외.
- **생성 전용** (수정은 추후 동일 컴포넌트 재사용으로 확장).
- 홈 화면에 진입 트리거 추가 (현재 홈은 디버그 버튼 placeholder → 동일 스타일의 "덱 만들기" 버튼 1개).

**비범위:**
- 덱 수정/삭제, 카드 추가, 덱 목록, 서버 연동, 약관/권한.

## 2. 디자인 분석 (Figma)

화면 구성 (위→아래):
1. Status bar / Top Bar: leading 뒤로가기 아이콘 + center 타이틀.
2. Deck Create Content:
   - 덱 이름 입력 (라벨 "덱 이름" + 필수 `*`, 카운터 `n/50`, clear 아이콘).
   - 설명 입력 (라벨 "설명" + 필수 `*`, 멀티라인, 카운터 `n/500`).
   - TIP 섹션: "TIP" 라벨 + 불릿 "한 덱에 최대 200장의 카드를 담을 수 있어요."
3. Bottom Bar: 풀폭 CTA 버튼.
4. 키보드 영역 (`imePadding`으로 대응).

**디자인 토큰 매핑 (Figma → CaroTheme):** 모두 기존 디자인 시스템에 존재.
- spacing: xs=4, s=8, m=12, l=16, xl=24 → `CaroTheme.spacing.*`
- color: text/primary·accent·secondary·inverse, surface/primary·brand, border/secondary·brand → `CaroTheme.color.*`
- typography: Heading2(18, SemiBold) 타이틀, Body1 입력, Label1/Caption1 보조 → `CaroTheme.typography.*`

## 3. 아키텍처 (MVI, `feature/profile` 패턴 준수)

```
feature/deck/
└ src/commonMain/kotlin/com/whatever/caro/feature/deck/
   ├ CreateDeckRoute.kt        # state 수집 + side effect 처리 (NavigateBack)
   ├ CreateDeckScreen.kt       # stateless UI (state + onIntent)
   ├ CreateDeckViewModel.kt    # BaseViewModel<State, Intent, SideEffect>
   ├ DeckInputDefaults.kt      # 길이/카드 상수 (NAME_MAX=50, DESC_MAX=500, MAX_CARDS=200)
   ├ di/DeckModule.kt          # viewModel<CreateDeckViewModel>()
   └ mvi/
      ├ CreateDeckIntent.kt
      ├ CreateDeckState.kt
      └ CreateDeckSideEffect.kt
```

### 3.1 MVI 계약

- **Intent** (`sealed interface CreateDeckIntent : UiIntent`)
  - `UpdateName(name: String)`
  - `UpdateDescription(description: String)`
  - `ClickBack`
  - `ClickConfirm`
- **State** (`data class CreateDeckState : UiState`)
  - `name: String = ""`
  - `description: String = ""`
  - `isLoading: Boolean = false`
  - 파생 getter:
    - `nameCount: String = "${name.length}/${DeckInputDefaults.NAME_MAX}"`
    - `descriptionCount: String = "${description.length}/${DeckInputDefaults.DESC_MAX}"`
    - `isConfirmEnabled: Boolean = name.isNotBlank() && description.isNotBlank() && !isLoading`
- **SideEffect** (`sealed interface CreateDeckSideEffect : UiSideEffect`)
  - `NavigateBack`

### 3.2 ViewModel 동작

- `UpdateName` → `name.take(NAME_MAX)` 로 컷 후 `reduce { copy(name = ...) }`.
- `UpdateDescription` → `description.take(DESC_MAX)` 로 컷 후 `reduce { copy(description = ...) }`.
- `ClickBack` → `postSideEffect(NavigateBack)`.
- `ClickConfirm` → `isConfirmEnabled` 가드. `// TODO: 덱 생성 API 연동` 주석 후 `postSideEffect(NavigateBack)`.
- 별도 Validator 클래스는 두지 않는다(단순 길이 제한). CLAUDE.md 규칙: 단일 동작에 UseCase/Validator를 만들지 않는다.

### 3.3 화면 구조

```
CreateDeckRoute(viewModel, navDispatcher)
└ CreateDeckScreen(state, onIntent)
   Column(fillMaxSize, background.primary)
   ├ CaroTopBar(leading=ic_arrow_left_24 → ClickBack, center=title "덱 만들기")
   ├ Column(weight=1f, verticalScroll(rememberScrollState()), 좌우 패딩 xl)
   │  ├ DeckNameField        → CaroTextField (header 라벨+*, footer 카운터, clear trailingIcon)
   │  ├ DeckDescriptionField → CaroTextArea  (header 라벨+*, footer 카운터)
   │  └ DeckTipSection       → "TIP" 라벨 + 불릿 텍스트
   └ CreateDeckCtaButton(enabled=isConfirmEnabled, imePadding) → ClickConfirm
```

재사용 컴포넌트(이미 존재): `CaroTopBar`, `CaroTextField`, `CaroTextArea`.
CTA 버튼은 designsystem에 공용 컴포넌트가 없어 `feature/profile`과 동일하게 feature 내부 `private` 컴포넌트로 둔다(중복 추출은 별도 과제).

## 4. 네비게이션 / DI 배선

1. `core/navigator/.../entries/CreateDeckEntry.kt` — `@Serializable data object CreateDeckEntry : NavKey`.
2. `composeApp/.../CaroApp.kt` — `SavedStateConfiguration` polymorphic 에 `subclass(CreateDeckEntry::class, CreateDeckEntry.serializer())` 추가.
3. `composeApp/.../di/NavigationModule.kt` — `navigation<CreateDeckEntry> { CreateDeckRoute(viewModel = koinViewModel(), navDispatcher = get()) }`.
4. `composeApp/.../di/Koin.kt` — `deckModule` 등록.
5. 홈 트리거:
   - `feature/home`: `HomeIntent.ClickCreateDeck`, `HomeSideEffect.NavigateToCreateDeck`, `HomeViewModel` 분기, `HomeScreen` 버튼, `HomeRoute` 에서 `To(CreateDeckEntry)` 네비.

## 5. 모듈 / 빌드

- 신규 모듈 `:feature:deck` — `feature/profile/build.gradle.kts` 템플릿 복제 (caro.kmp / kmp.ios / kmp.android / cmp / feature / koin / kmp.test / kover), namespace `com.whatever.caro.feature.deck`. `caro.feature` 컨벤션이 designsystem·data·ui·viewmodel·navigator 를 자동 주입.
- `settings.gradle.kts` — `include(":feature:deck")`.
- `composeApp/build.gradle.kts` — `implementation(projects.feature.deck)`.

## 6. 문자열 리소스

`core/designsystem/src/commonMain/composeResources/values/strings.xml` (en) + `values-ko/strings.xml` (ko) 에 `<!-- deck -->` 섹션 추가:

| key | en | ko |
|---|---|---|
| `deck_title_create` | Create Deck | 덱 만들기 |
| `deck_content_description_back` | Go back | 뒤로 가기 |
| `deck_content_description_clear` | Clear | 지우기 |
| `deck_field_label_name` | Deck name | 덱 이름 |
| `deck_field_placeholder_name` | Enter a deck name | 덱 이름을 입력해주세요 |
| `deck_field_label_description` | Description | 설명 |
| `deck_field_placeholder_description` | Enter a deck description | 덱 설명을 입력해주세요 |
| `deck_field_required` | * | * |
| `deck_tip_label` | TIP | TIP |
| `deck_tip_max_cards` | You can add up to 200 cards per deck. | 한 덱에 최대 200장의 카드를 담을 수 있어요. |
| `deck_button_create` | Create | 만들기 |

> 모든 사용자 노출 문자열은 `stringResource(Res.string...)` 사용 (CLAUDE.md UI Strings 규칙).

## 7. Compose 성능 / UI 품질 (compose-performance-audit, compose-ui 스킬 반영)

- **Stateless 컴포넌트**: `CreateDeckScreen` 은 `state` + `onIntent: (CreateDeckIntent) -> Unit` 만 받음. 상태는 Route/ViewModel 에 호이스팅. `viewModel::intent` 메서드 레퍼런스 사용으로 콜백 안정성 확보.
- **Stable state**: State 는 `String`/`Boolean` 원시 타입만 → 기본 stable. 불안정 컬렉션(List/Map) 미사용. 파생값은 cheap getter.
- **정적 콘텐츠**: 리스트가 아니므로 `LazyColumn` 불필요 → `Column + verticalScroll(rememberScrollState())`. `imePadding` 으로 키보드 겹침 처리.
- **불필요 재구성 방지**: 입력 콜백을 `onIntent` 단일 경로로 통일. 내부 `private` 컴포넌트로 분리해 입력 변경 시 재구성 범위 최소화.
- **Modifier**: 각 컴포넌트는 `modifier: Modifier = Modifier` 를 첫 옵셔널 파라미터로 노출, 루트에 적용. 패딩/클립/배경 순서는 profile 패턴과 동일.
- **Preview**: public 컴포넌트(`CreateDeckScreen`)에 `@Preview`(빈 상태 / 입력된 상태) + 더미 state 추가.
- 하드코딩 색/치수 금지 → `CaroTheme.*` 토큰만 사용.

## 8. 테스트

- `feature/deck/src/commonTest/.../CreateDeckViewModelTest.kt` (Kotest FunSpec + Turbine):
  - 이름 입력이 50자에서 잘리는지.
  - 설명 입력이 500자에서 잘리는지.
  - 이름·설명 둘 다 채워질 때만 `isConfirmEnabled == true`.
  - `ClickBack` / `ClickConfirm` 시 `NavigateBack` side effect 방출.
- `caro.kover` 50% 라인 커버리지 충족.

## 9. 검증 기준 (Definition of Done)

1. `./gradlew spotlessApply` 통과.
2. `./gradlew :feature:deck:allTests` 통과.
3. `./gradlew :androidApp:assembleDevDebug` 빌드 성공.
4. 홈 → "덱 만들기" → 입력 → "만들기"/뒤로가기 시 정상 네비게이션 (수동 확인).

## 10. 가정 / 결정

- 이름·설명 **둘 다 필수**(Figma `*` 기준). 설명을 선택값으로 바꾸려면 `isConfirmEnabled` 한 줄 수정.
- CTA 라벨 "만들기", 타이틀 "덱 만들기".
- 홈 트리거는 현재 placeholder 홈에 맞춘 단순 버튼(추후 실제 홈 디자인 반영 시 교체).

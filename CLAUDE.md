# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Kotlin Multiplatform (KMP) mobile app targeting Android and iOS, using Compose Multiplatform for shared UI. Package namespace: `com.whatever.caro`.

## Build Commands

```bash
# Lint check (runs in CI on all PRs)
./gradlew spotlessCheck

# Auto-fix lint issues
./gradlew spotlessApply

# Build Android app
./gradlew :androidApp:assembleDevDebug

# Run tests (all modules)
./gradlew allTests

# Run tests for a specific module
./gradlew :feature:home:allTests

# Android host tests for a specific module
./gradlew :feature:home:testAndroidHostTest

# Code coverage report (Kover aggregation)
./gradlew koverHtmlReport
```

**Requirements:** JDK 17 (temurin), Android SDK with compileSdk 36.

## Architecture

### Module Structure

- **`:androidApp`** — Android entry point (MainActivity, CaroApplication with Koin init)
- **`:composeApp`** — Shared KMP module containing CaroApp, NavHost, and DI wiring. Depends on all core and feature modules.
- **`:core:*`** — Shared infrastructure modules:
  - `data` — Repository layer
  - `model` — Data classes (e.g., User)
  - `designsystem` — Theme/design tokens
  - `ui` — Shared composable utilities
  - `viewmodel` — Base ViewModel abstractions (MVI pattern)
  - `navigator` — Navigation keys, commands, and dispatcher
  - `remote` — Ktor HTTP client (OkHttp on Android, Darwin on iOS), DataSources
- **`:feature:*`** — Feature screens (home, login). Each feature module auto-depends on core modules via the `caro.feature` convention plugin.
- **`build-logic/convention`** — Custom Gradle plugins that standardize module configuration.

### Module Dependency Rules

```
:androidApp → :composeApp
:composeApp → :core:* + :feature:*
:feature:*  → auto-injected by caro.feature: designsystem, data, ui, viewmodel, navigator
:core:data  → :core:remote, :core:model
:core:remote → :core:model
```

Feature modules must NOT depend on each other. Cross-feature communication goes through `:core:navigator`.

### MVI Pattern (Intent / State / SideEffect)

ViewModels extend `BaseViewModel<State, Intent, SideEffect>` and follow the MVI pattern:

```
feature/<name>/
|-- route/
|   |-- <Name>Route.kt          # Composable entry point (collects state, handles side effects)
|-- <Name>Screen.kt             # Pure UI composable (receives state + intent callback)
|-- <Name>ViewModel.kt          # Extends BaseViewModel; registered in the feature's DI module
|-- di/
|   |-- <Name>Module.kt         # Koin `module { }` declaring viewModel/single bindings
|-- mvi/
    |-- <Name>Intent.kt         # Sealed interface of user actions
    |-- <Name>State.kt          # Data class representing UI state
    |-- <Name>SideEffect.kt     # Sealed interface for one-shot events
```

Key ViewModel helpers:
- `reduce { copy(...) }` — Mutate state
- `postSideEffect(effect)` — Emit one-shot side effect
- `launch { }` — Coroutine scope with built-in exception handling

Feature ViewModels may call repositories directly for simple one-to-one API actions. Do not introduce a feature UseCase just to wrap a single repository call; add a UseCase only when it owns reusable business rules, orchestration across multiple repositories, or meaningful transformation beyond basic UI mapping. Keep local UI validation helpers close to the feature and name them by behavior (e.g., `NicknameValidator`) rather than as API UseCases.

Repository contracts should expose domain-shaped primitives or models, not pass-through response wrappers created only for one screen. For example, a nickname availability check should return `Boolean` unless the backend actually returns structured reason data the app needs.

### UI Strings

User-visible Compose text and accessibility labels must use Compose resources (`stringResource(Res.string...)`). Avoid hardcoded UI strings in screens, routes, and components except temporary debug-only UI.

### Navigation (Navigation3)

Navigation keys are `@Serializable` data objects/classes extending `NavKey` in `:core:navigator`. Navigation commands flow through `NavigationDispatcher` using sealed `NavCommand` (Back, To, Replace, ResetTo).

**Adding a new screen requires:**
1. Define a `@Serializable` NavKey in `:core:navigator` (e.g., `data object SettingsEntry : NavKey`)
2. Register the key's polymorphic serializer in `CaroApp.kt`'s `SavedStateConfiguration`
3. Add a `navigation<Key> { }` entry in `composeApp/di/NavigationModule.kt`
4. Create the feature module with Route/Screen/ViewModel/MVI classes

**Passing parameters:** Declare the values directly as constructor properties on the NavKey `@Serializable data class` — do NOT wrap them in a nested or top-level `Payload` object (see `EditDeckEntry`, `EditProfileEntry`):

```kotlin
@Serializable
data class EditDeckEntry(
    val deckName: String,
    val deckDescription: String,
    val deckId: Long,
) : NavKey
```

In `NavigationModule.kt` the route lambda forwards each value individually via `parametersOf(navKey.deckId, navKey.deckName, navKey.deckDescription)`, and the ViewModel receives them as individual constructor parameters (`EditDeckViewModel(deckName, deckDescription, deckId, ...)`) — Koin's compiler plugin auto-routes those through `parametersOf` when resolving `viewModel<EditDeckViewModel>()`. Keep NavKey payloads flat: a NavKey should not forward its whole self (`parametersOf(navKey)`) unless the ViewModel actually declares the NavKey as a parameter.

### Dependency Injection (Koin)

DI uses the **Koin Kotlin compiler plugin** (`io.insert-koin.compiler.plugin`) — no KSP, no annotations. The plugin adds shortcut DSL functions under `org.koin.plugin.module.dsl` that auto-resolve constructor arguments via `get()` (registered beans) and `parametersOf(...)` (call-site parameters):

```kotlin
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.plugin.module.dsl.single
import org.koin.plugin.module.dsl.viewModel

val dataModule =
    module {
        single<AuthRepositoryImpl>() bind AuthRepository::class  // expose impl as interface
        single<FcmTokenRepositoryImpl>() bind FcmTokenRepository::class
    }

val homeModule =
    module {
        viewModel<HomeViewModel>()  // constructor args auto-resolved (incl. NavKey from parametersOf)
    }
```

Fall back to the plain DSL form (`single { ... }`, `viewModel { ... }`) only when the shortcut can't express the binding — i.e. when you need:
- a `named()` qualifier on the bean or its `get(named(...))` dependencies (see `NetworkModule`, `ApiModule`, `RemoteModule`)
- a configured builder (`Json { ... }`, `DataStoreFactory.create(producePath = { ... })`)
- logic that isn't just a constructor call

All modules are composed in `initKoin()` (`composeApp/src/commonMain/kotlin/com/whatever/caro/composeApp/di/Koin.kt`); `navEntryModule` is listed there alongside the core and feature modules.

### Convention Plugins (build-logic)

| Plugin ID | Purpose |
|---|---|
| `caro.kmp` | Base KMP setup (coroutines, Napier logging, `-Xexpect-actual-classes`) |
| `caro.kmp.android` | Android KMP target (compileSdk 36, minSdk 28, JVM 17) |
| `caro.kmp.ios` | iOS targets (x64, arm64, simulatorArm64) |
| `caro.cmp` | Compose Multiplatform deps (material3, resources, foundation) + compiler |
| `caro.feature` | Feature module: auto-adds core deps (designsystem, data, ui, viewmodel, navigator) + koin-compose-viewmodel |
| `caro.koin` | Applies the Koin Kotlin compiler plugin (`io.insert-koin.compiler.plugin`) + adds `koin-core` to commonMain. Enables shortcut DSL (`single<X>()`, `viewModel<X>()`). |
| `caro.kmp.test` | Test deps (Kotest, Mokkery, Turbine, coroutines-test, koin-test) |
| `caro.kover` | Kover coverage: applied to `:core:data` and `:feature:*`, enforces 50% line minimum |
| `caro.kotlin.serialization` | kotlinx.serialization plugin + JSON |
| `caro.android.application` | Android application configuration |

**New feature module `build.gradle.kts` template:**
```kotlin
plugins {
    id("caro.kmp")
    id("caro.kmp.ios")
    id("caro.kmp.android")
    id("caro.cmp")
    id("caro.feature")
    id("caro.koin")
    id("caro.kmp.test")
    id("caro.kover")
}
```

## Lint & Formatting

- **Spotless** with **ktlint 1.8.0** enforces formatting on all `*.kt` and `*.gradle.kts` files.
- `.editorconfig` exempts `@Composable` functions from ktlint's function naming rule.
- Always run `./gradlew spotlessApply` before committing to avoid CI failures.

## Git Ignore Policy

- `.gitignore` is authoritative. Never bypass it with `git add -f` or otherwise force-add an ignored file unless the user explicitly requests that exact path.
- `docs/` contains local design, specification, and implementation-plan artifacts. Keep these files local; never commit or push them.

## Testing Stack

- **Kotest** — FunSpec style with `init { }` block, JUnit5 runner on Android
- **Mokkery** — Multiplatform mocking
- **Turbine** — Flow testing
- **kotlinx-coroutines-test** — Coroutine test support
- **Koin Test** — `KoinTest` interface + `KoinExtension` for DI in tests
- Test sources: `src/commonTest/kotlin` (shared), `src/androidHostTest/kotlin` (Android-specific).

## CI

PR checks (`.github/workflows/caro-ci.yml`):
1. **lint-check** — Runs `spotlessCheck`, notifies Slack
2. **test-coverage** — Runs tests, generates Kover XML report, verifies coverage threshold, comments PR with coverage

## iOS

- Xcode project in `iosApp/`, consumes the `ComposeApp` static framework from KMP.
- Entry point: `iOSApp.swift` (SwiftUI) — calls `initKoin()` in `init()`.
- Fastlane configured for match signing profiles (dev, qa, prod).

## Platform-Specific Code

Use expect/actual declarations. Platform files follow naming convention: `Foo.android.kt` / `Foo.ios.kt`.
Example: `HttpClientEngineProvider` has common interface + platform-specific implementations (OkHttp for Android, Darwin for iOS).

## Skill(All Agents)

이 저장소에는 에이전트 중립적인 절차서가 `.claude/skills/` 아래에 있습니다.
Claude 외의 에이전트(Codex 등)는 자동 발견을 못 하므로, 아래 트리거가 보이면
해당 `SKILL.md`를 읽고 그 절차를 그대로 따르세요. 절차서·규칙·스크립트는
모두 에이전트 중립적입니다(YAML frontmatter만 Claude 전용이니 무시).

- **swagger-sync** → [`.claude/skills/swagger-sync/SKILL.md`](./.claude/skills/swagger-sync/SKILL.md)
    - 트리거: "DTO 동기화", "API 스펙 반영", "Swagger 업데이트", "DTO PR 만들어줘"
    - 내용: Swagger/OpenAPI 스펙 → Kotlin DTO + 태그별 Ktorfit API 인터페이스 생성,
      사내 규칙 적용 후 변경 시 브랜치 + 커밋까지.
    - 실행 스크립트: `scripts/*.sh`(mac/Linux), `scripts/*.ps1`(Windows)

- **_conventions** → [`.claude/skills/_conventions/SKILL.md`](./.claude/skills/_conventions/SKILL.md)
    - 트리거: "우리 컨벤션", "모듈 구조", "MVI 구조", "화면 추가 절차"
    - 내용: Caro 아키텍처 규칙 요약(모듈 의존·MVI·BaseViewModel·Koin·Navigation3·Compose
      resources·expect-actual·Kotest). 아래 컴포즈/코틀린 스킬이 "우리 컨벤션은?"의 근거로 삼는다.

- **caro-compose-review** → [`.claude/skills/caro-compose-review/SKILL.md`](./.claude/skills/caro-compose-review/SKILL.md)
    - 트리거: "컴포즈 리뷰", "Compose 리뷰", "UI 리뷰", "MVI 리뷰", "상태/사이드이펙트 점검"
    - 내용: PR/diff를 `_conventions` 기반 체크리스트(A~K)로 점검. 일반 버그 리뷰와 보완 관계.

- **Compose/Kotlin/KMP 스킬** (코딩·리뷰 중 해당 주제가 나오면 그 디렉토리의 `SKILL.md`를 읽고 따른다)
    - `kotlin-multiplatform-expect-actual` — "expect/actual", "플랫폼 분기", "commonMain 경계"
    - `kotlin-coroutines-structured-concurrency` — "코루틴 스코프", "취소", "runBlocking"
    - `kotlin-flow-state-event-modeling` — "StateFlow vs Channel", "이벤트 모델링", "sideEffect"
    - `compose-state-authoring` — "remember", "mutableStateOf", "상태 작성"
    - `compose-state-hoisting` — "상태 끌어올리기", "state hoisting", "remember 어디"
    - `compose-state-holder-ui-split` — "Route/Screen 분리", "상태홀더", "순수 UI"
    - `compose-side-effects` — "LaunchedEffect", "DisposableEffect", "부수효과"
    - `compose-recomposition-performance` — "리컴포지션", "성능", "안정성"
    - `compose-slot-api-pattern` — "슬롯 API", "재사용 컴포넌트", "디자인시스템 컴포넌트"
    - `compose-ui-testing-patterns` — "UI 테스트", "ViewModel 테스트", "Turbine"
    - 개요·출처: [`.claude/skills/README.md`](./.claude/skills/README.md)

## Creating Rule
Remote data source implementation classes should carry a `Remote` prefix when the interface name is generic enough to be confused with repository/data-layer types (e.g., `RemoteProfileDataSourceImpl : ProfileDataSource`).

상수 위치는 사용 범위로 결정한다. 한 화면(Screen)에서만 쓰는 UI 값은 그 Screen 파일의 `private const val`/`private val` 로 둔다. ViewModel·State·Test 등 feature 내부 여러 곳에서 공유하는 입력 제한·정책 값은 feature 패키지의 `internal object XxxLimits`(예: `DeckInputLimits`)로 분리한다. 서버 정책이나 도메인 규칙에 가까운 값은 domain/model 또는 repository 계층에서 관리한다.

Screen 전용의 작은 `private` Composable 은 같은 Screen 파일에 둔다. 파일이 커지거나 다른 화면에서 재사용할 여지가 있는 UI 조각만 feature 내부 `components/` 패키지로 분리한다. 현재 feature(profile, deck 등)는 한 파일 유지가 기준선이며, `components/` 도입 시 기존 화면도 함께 정리해 구조를 일관되게 맞춘다.

DTO ↔ 도메인 모델 매핑은 `:core:data` 의 `mapper` 패키지(`com.whatever.caro.core.data.mapper`)에 모은다. RepositoryImpl 내부에 `private fun XxxResponse.toModel()` 같은 인라인/`private` 확장함수로 매핑을 두지 않는다. 컨벤션: 파일명 `{Domain}Mapper.kt`, DTO(`XxxResponse`/`XxxResponseDto`)에 대한 `to{Model}` 확장함수(`internal`), enum·필드 헬퍼는 `private`, 매핑에만 쓰이는 상수(`FIELD_FRONT` 등)도 mapper 파일 top-level `private const val` 로 둔다(`AuthMapper.kt`·`DeckMapper.kt` 참고). `remote`/DataSource 레이어는 매핑하지 않고 DTO 를 그대로 반환하며, DTO→모델 변환은 항상 data(repository) 레이어의 mapper 에서 일어난다. mapper 는 `commonTest` 에 별도 단위 테스트(`{Domain}MapperTest.kt`)를 갖는다.

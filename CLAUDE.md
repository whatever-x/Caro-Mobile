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

**Passing parameters:** Use a `@Serializable data class` with a `Payload` (see `HomeEntry` pattern). The ViewModel declares the NavKey as a constructor parameter; in `NavigationModule.kt` the route lambda forwards it via `koinViewModel<HomeViewModel> { parametersOf(navKey) }`, and Koin's compiler plugin auto-routes that parameter through `parametersOf` when resolving `viewModel<HomeViewModel>()`.

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

## Creating Rule
Remote data source implementation classes should carry a `Remote` prefix when the interface name is generic enough to be confused with repository/data-layer types (e.g., `RemoteProfileDataSourceImpl : ProfileDataSource`).
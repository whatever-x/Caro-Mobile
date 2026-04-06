# AGENTS.md

This file provides guidance to Codex when working with code in this repository.

## Project Overview

Kotlin Multiplatform (KMP) mobile app targeting Android and iOS, using Compose Multiplatform for shared UI. Package namespace: `com.whatever.caro`.

## Build Commands

```bash
# Lint check (runs in CI on all PRs)
./gradlew spotlessCheck

# Auto-fix lint issues
./gradlew spotlessApply

# Build Android app
./gradlew :androidApp:assembleDebug

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
|-- <Name>ViewModel.kt          # @KoinViewModel, extends BaseViewModel
|-- di/
|   |-- <Name>Module.kt         # @Module @ComponentScan (auto-discovers annotated classes)
|-- mvi/
    |-- <Name>Intent.kt         # Sealed interface of user actions
    |-- <Name>State.kt          # Data class representing UI state
    |-- <Name>SideEffect.kt     # Sealed interface for one-shot events
```

Key ViewModel helpers:
- `reduce { copy(...) }` — Mutate state
- `postSideEffect(effect)` — Emit one-shot side effect
- `launch { }` — Coroutine scope with built-in exception handling

### Navigation (Navigation3)

Navigation keys are `@Serializable` data objects/classes extending `NavKey` in `:core:navigator`. Navigation commands flow through `NavigationDispatcher` using sealed `NavCommand` (Back, To, Replace, ResetTo).

**Adding a new screen requires:**
1. Define a `@Serializable` NavKey in `:core:navigator` (e.g., `data object SettingsEntry : NavKey`)
2. Register the key's polymorphic serializer in `CaroApp.kt`'s `SavedStateConfiguration`
3. Add a `navigation<Key> { }` entry in `composeApp/di/NavigationModule.kt`
4. Create the feature module with Route/Screen/ViewModel/MVI classes

**Passing parameters:** Use a `@Serializable data class` with a `Payload` (see `HomeEntry` pattern). Inject the NavKey into the ViewModel via `@InjectedParam`.

### Dependency Injection (Koin)

Koin uses annotation-based setup processed by KSP:
- **`@Module` + `@ComponentScan`** — Feature/core modules auto-discover annotated classes in their package
- **`@KoinViewModel`** — Registers ViewModels for Koin injection
- **`@Single(binds = [Interface::class])`** — Repository binding pattern
- **`@InjectedParam`** — Constructor parameter injection (used for NavKey in ViewModels)

All modules are composed in `AppModule` (`composeApp/di/`) using `@Configuration` with `includes = [...]`. The `navigationModule` is registered separately in `initKoin {}`.

### Convention Plugins (build-logic)

| Plugin ID | Purpose |
|---|---|
| `caro.kmp` | Base KMP setup (coroutines, Napier logging, `-Xexpect-actual-classes`) |
| `caro.kmp.android` | Android KMP target (compileSdk 36, minSdk 28, JVM 17) |
| `caro.kmp.ios` | iOS targets (x64, arm64, simulatorArm64) |
| `caro.cmp` | Compose Multiplatform deps (material3, resources, foundation) + compiler |
| `caro.feature` | Feature module: auto-adds core deps (designsystem, data, ui, viewmodel, navigator) + koin-compose-viewmodel |
| `caro.koin` | Koin DI with KSP annotation processing (all platform targets) |
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

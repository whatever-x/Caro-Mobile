# ExceptionFilter 필수 생성자 주입 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** `BaseViewModel`의 `KoinComponent`/`by inject()` 서비스 로케이터를 제거하고 `ExceptionFilter`를 필수 생성자 주입으로 전환하여 koin-compiler-plugin의 컴파일 타임 검증 경로 안에 넣는다.

**Architecture:** 스펙 `docs/superpowers/specs/2026-06-10-exception-filter-di-design.md` 참조. 정책(`CaroExceptionFilter`)은 composeApp이 소유, `core:viewmodel`은 `ExceptionFilter` 계약과 suppress 메커니즘만 소유하며 Koin 의존을 완전히 제거한다. 마이그레이션은 3단계 — ① `BaseViewModel`에 임시 기본값(`= ExceptionFilter.None`) 파라미터 추가 → ② feature VM 3개 마이그레이션 → ③ 기본값 제거(필수화) — 로 진행해 모든 커밋이 컴파일/테스트 green을 유지한다. **임시 기본값은 Task 5에서 반드시 제거된다(스펙의 최종 상태는 필수 파라미터). Task 2까지만 하고 멈추면 스펙 미달이다.**

**Tech Stack:** Kotlin Multiplatform, Koin (compiler plugin `io.insert-koin.compiler.plugin`), Kotest (FunSpec), Mokkery, kotlinx-coroutines-test, Spotless(ktlint)

**사전 확인된 사실:**
- `HomeViewModelTest`는 이 브랜치에서 **이미 실패 중** (`No definition found for type 'AuthRepository'`) — ExceptionFilter와 무관한 기존 결함. Task 1에서 수정한다.
- `LoginViewModelTest`에는 VM을 해석하는 테스트가 아직 없다 (Koin 셋업만 존재). ExceptionFilter 바인딩은 방어적으로 추가한다.
- `SplashViewModelTest`는 Koin 없이 `SplashViewModel()`을 직접 생성한다 (33행, 46행).
- `caro.kmp.test` 컨벤션 플러그인이 Kotest/Mokkery/koin-test/turbine/coroutines-test를 모두 제공한다.
- 모든 커밋 전 `./gradlew spotlessApply` 실행 (CI의 spotlessCheck 대비).

---

### Task 1: HomeViewModelTest 기존 결함 수정 (AuthRepository mock 바인딩)

**Files:**
- Modify: `feature/home/src/commonTest/kotlin/HomeViewModelTest.kt`

- [ ] **Step 1: 현재 테스트가 실패하는지 확인**

Run: `./gradlew :feature:home:testAndroidHostTest --console=plain`
Expected: FAIL — `org.koin.core.error.InstanceCreationException` / `No definition found for type 'com.whatever.caro.core.data.repository.AuthRepository'`

- [ ] **Step 2: 테스트 Koin 컨텍스트에 AuthRepository mock 바인딩 추가**

`HomeViewModelTest.kt`의 import에 3줄 추가:

```kotlin
import com.whatever.caro.core.data.repository.AuthRepository
import dev.mokkery.mock
import org.koin.dsl.module
```

26행의 extensions 호출을 다음으로 교체:

```kotlin
        extensions(
            KoinExtension(
                listOf(
                    homeModule,
                    module {
                        single<AuthRepository> { mock<AuthRepository>() }
                    },
                ),
            ),
        )
```

(현재 테스트 `init() 호출 시 navKey payload 로 state 갱신`은 repo 메서드를 호출하지 않으므로 stub 불필요 — `mock<AuthRepository>()`만으로 충분하다.)

- [ ] **Step 3: 테스트 통과 확인**

Run: `./gradlew :feature:home:testAndroidHostTest --console=plain`
Expected: PASS (1 test completed)

- [ ] **Step 4: Commit**

```bash
./gradlew spotlessApply
git add feature/home/src/commonTest/kotlin/HomeViewModelTest.kt
git commit -m "test: HomeViewModelTest에 누락된 AuthRepository mock 바인딩 추가

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

### Task 2: core:viewmodel — BaseViewModel 생성자 주입 전환 + Koin 제거 (TDD)

**Files:**
- Modify: `core/viewmodel/build.gradle.kts`
- Modify: `core/viewmodel/src/commonMain/kotlin/com/whatever/caro/core/viewmodel/ExceptionFilter.kt`
- Modify: `core/viewmodel/src/commonMain/kotlin/com/whatever/caro/core/viewmodel/BaseViewModel.kt`
- Test: `core/viewmodel/src/commonTest/kotlin/com/whatever/caro/core/viewmodel/BaseViewModelTest.kt` (신규)

- [ ] **Step 1: 테스트 인프라 추가 — build.gradle.kts에서 caro.koin 제거 + caro.kmp.test 추가**

`core/viewmodel/build.gradle.kts` 전체를 다음으로 교체:

```kotlin
plugins {
    id("caro.kmp")
    id("caro.kmp.ios")
    id("caro.kmp.android")
    id("caro.kmp.test")
}

kotlin {
    android {
        namespace = "com.whatever.caro.core.viewmodel"
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.jetbrains.androidx.lifecycle.viewmodel)
        }
    }
}
```

(변경점: `id("caro.koin")` 제거, `id("caro.kmp.test")` 추가. 이 시점에서 `BaseViewModel.kt`가 koin import 때문에 컴파일 깨지는 것은 정상 — Step 3에서 해소.)

- [ ] **Step 2: 실패하는 테스트 작성**

Create `core/viewmodel/src/commonTest/kotlin/com/whatever/caro/core/viewmodel/BaseViewModelTest.kt`:

```kotlin
package com.whatever.caro.core.viewmodel

import com.whatever.caro.core.viewmodel.contract.UiIntent
import com.whatever.caro.core.viewmodel.contract.UiSideEffect
import com.whatever.caro.core.viewmodel.contract.UiState
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class BaseViewModelTest : FunSpec() {
    private data object TestState : UiState

    private sealed interface TestIntent : UiIntent {
        data object Throw : TestIntent
    }

    private sealed interface TestSideEffect : UiSideEffect

    private class TestViewModel(
        exceptionFilter: ExceptionFilter,
    ) : BaseViewModel<TestState, TestIntent, TestSideEffect>(
            initialState = TestState,
            exceptionFilter = exceptionFilter,
        ) {
        var handledThrowable: Throwable? = null

        override suspend fun handleIntent(intent: TestIntent) {
            when (intent) {
                TestIntent.Throw -> throw IllegalStateException("boom")
            }
        }

        override fun handleClientException(throwable: Throwable) {
            handledThrowable = throwable
        }
    }

    init {
        val testDispatcher = StandardTestDispatcher()

        beforeTest {
            Dispatchers.setMain(testDispatcher)
        }

        afterTest {
            Dispatchers.resetMain()
            testDispatcher.cancel()
        }

        test("필터가 suppress=true 를 반환하면 handleClientException 이 호출되지 않는다") {
            runTest {
                val viewModel = TestViewModel(exceptionFilter = ExceptionFilter { true })

                viewModel.intent(TestIntent.Throw)
                advanceUntilIdle()

                viewModel.handledThrowable shouldBe null
            }
        }

        test("필터가 suppress=false 를 반환하면 handleClientException 으로 예외가 전달된다") {
            runTest {
                val viewModel = TestViewModel(exceptionFilter = ExceptionFilter.None)

                viewModel.intent(TestIntent.Throw)
                advanceUntilIdle()

                viewModel.handledThrowable!!.message shouldBe "boom"
            }
        }
    }
}
```

- [ ] **Step 3: 테스트가 컴파일 실패하는지 확인**

Run: `./gradlew :core:viewmodel:testAndroidHostTest --console=plain`
Expected: FAIL (컴파일 에러) — Step 1에서 `caro.koin`을 제거했으므로 commonMain의 `BaseViewModel.kt`가 koin import unresolved로 먼저 실패한다 (`Unresolved reference 'koin'` 류). 테스트 소스까지 컴파일이 진행되면 `Cannot find a parameter with this name: exceptionFilter`, `Unresolved reference 'None'`도 나온다. 어느 쪽이든 FAIL이면 정상.

- [ ] **Step 4: 구현 — ExceptionFilter.None 추가 + BaseViewModel 전환**

`ExceptionFilter.kt` 전체를 다음으로 교체:

```kotlin
package com.whatever.caro.core.viewmodel

fun interface ExceptionFilter {
    fun shouldSuppress(throwable: Throwable): Boolean

    companion object {
        val None = ExceptionFilter { false }
    }
}
```

`BaseViewModel.kt` 전체를 다음으로 교체 (변경점: `KoinComponent` 상속 제거, `by inject()` 제거, koin import 2개 제거, 생성자에 `exceptionFilter` 파라미터 추가 — **기본값은 Task 5에서 제거될 임시 마이그레이션 장치**):

```kotlin
package com.whatever.caro.core.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whatever.caro.core.viewmodel.contract.UiIntent
import com.whatever.caro.core.viewmodel.contract.UiSideEffect
import com.whatever.caro.core.viewmodel.contract.UiState
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

abstract class BaseViewModel<S : UiState, I : UiIntent, SE : UiSideEffect>(
    initialState: S,
    private val exceptionFilter: ExceptionFilter = ExceptionFilter.None,
) : ViewModel() {
    protected abstract suspend fun handleIntent(intent: I)

    private val _state = MutableStateFlow(initialState)
    val state = _state.asStateFlow()

    private val _sideEffect =
        Channel<SE>(
            capacity = Channel.BUFFERED,
            onBufferOverflow = BufferOverflow.SUSPEND,
        )
    val sideEffect = _sideEffect.receiveAsFlow()

    protected val currentState: S
        get() = _state.value

    protected val coroutineExceptionHandler =
        CoroutineExceptionHandler { _, throwable ->
            if (exceptionFilter.shouldSuppress(throwable)) {
                Napier.w(throwable = throwable) { "Suppressed by ExceptionFilter: ${throwable::class.simpleName}" }
            } else {
                handleClientException(throwable)
            }
        }

    fun intent(intent: I) {
        launch {
            handleIntent(intent)
        }
    }

    protected fun reduce(reduce: S.() -> S) {
        val state = currentState.reduce()
        _state.value = state
    }

    protected fun postSideEffect(sideEffect: SE) {
        viewModelScope.launch { _sideEffect.send(sideEffect) }
    }

    protected inline fun launch(
        context: CoroutineContext = EmptyCoroutineContext,
        start: CoroutineStart = CoroutineStart.DEFAULT,
        crossinline action: suspend CoroutineScope.() -> Unit,
    ): Job =
        viewModelScope.launch(context + coroutineExceptionHandler, start = start) {
            action()
        }

    open fun handleClientException(throwable: Throwable) {
        Napier.e { "handleClientException = ${throwable.message}" }
    }
}
```

- [ ] **Step 5: 테스트 통과 확인**

Run: `./gradlew :core:viewmodel:testAndroidHostTest --console=plain`
Expected: PASS (2 tests completed)

- [ ] **Step 6: 기존 feature 테스트가 여전히 green인지 확인 (기본값 덕분에 컴파일 유지)**

Run: `./gradlew :feature:home:testAndroidHostTest :feature:splash:testAndroidHostTest :feature:login:testAndroidHostTest --console=plain`
Expected: PASS (home 1, splash 2, login 0 tests)

- [ ] **Step 7: Commit**

```bash
./gradlew spotlessApply
git add core/viewmodel
git commit -m "refactor: BaseViewModel의 ExceptionFilter를 생성자 주입으로 전환하고 Koin 의존 제거

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

### Task 3: feature VM 3개 마이그레이션 (home → splash → login)

**Files:**
- Modify: `feature/home/src/commonMain/kotlin/com/whatever/caro/feature/home/HomeViewModel.kt:11-16`
- Modify: `feature/home/src/commonTest/kotlin/HomeViewModelTest.kt`
- Modify: `feature/splash/src/commonMain/kotlin/com/whatever/caro/feature/splash/SplashViewModel.kt:16-19`
- Modify: `feature/splash/src/commonTest/kotlin/SplashViewModelTest.kt:33,46`
- Modify: `feature/login/src/commonMain/kotlin/com/whatever/caro/feature/login/LoginViewModel.kt:16-19`
- Modify: `feature/login/src/commonTest/kotlin/LoginViewModelTest.kt`

(DI 모듈 `homeModule`/`splashModule`/`loginModule`은 변경 없음 — `viewModel<X>()`가 새 생성자 파라미터를 자동 해석한다.)

- [ ] **Step 1: HomeViewModel에 파라미터 추가**

`HomeViewModel.kt` import에 추가:

```kotlin
import com.whatever.caro.core.viewmodel.ExceptionFilter
```

클래스 선언부(11–16행)를 다음으로 교체:

```kotlin
class HomeViewModel(
    private val authRepository: AuthRepository,
    private val navKey: HomeEntry,
    exceptionFilter: ExceptionFilter,
) : BaseViewModel<HomeState, HomeIntent, HomeSideEffect>(
        initialState = HomeState(),
        exceptionFilter = exceptionFilter,
    ) {
```

- [ ] **Step 2: HomeViewModelTest에 ExceptionFilter 바인딩 추가**

`HomeViewModelTest.kt` import에 추가:

```kotlin
import com.whatever.caro.core.viewmodel.ExceptionFilter
```

Task 1에서 만든 테스트 module 블록을 다음으로 교체:

```kotlin
                    module {
                        single<AuthRepository> { mock<AuthRepository>() }
                        single<ExceptionFilter> { ExceptionFilter.None }
                    },
```

- [ ] **Step 3: home 테스트 통과 확인**

Run: `./gradlew :feature:home:testAndroidHostTest --console=plain`
Expected: PASS

- [ ] **Step 4: SplashViewModel에 파라미터 추가**

`SplashViewModel.kt` import에 추가:

```kotlin
import com.whatever.caro.core.viewmodel.ExceptionFilter
```

클래스 선언부(16–19행)를 다음으로 교체:

```kotlin
class SplashViewModel(
    exceptionFilter: ExceptionFilter,
) : BaseViewModel<SplashState, SplashIntent, SplashSideEffect>(
        initialState = SplashState(),
        exceptionFilter = exceptionFilter,
    ) {
```

- [ ] **Step 5: SplashViewModelTest의 직접 생성 2곳 수정**

`SplashViewModelTest.kt` import에 추가:

```kotlin
import com.whatever.caro.core.viewmodel.ExceptionFilter
```

33행과 46행의 `val viewModel = SplashViewModel()`을 두 곳 모두 다음으로 교체:

```kotlin
                val viewModel = SplashViewModel(exceptionFilter = ExceptionFilter.None)
```

- [ ] **Step 6: splash 테스트 통과 확인**

Run: `./gradlew :feature:splash:testAndroidHostTest --console=plain`
Expected: PASS (2 tests)

- [ ] **Step 7: LoginViewModel에 파라미터 추가**

`LoginViewModel.kt` import에 추가:

```kotlin
import com.whatever.caro.core.viewmodel.ExceptionFilter
```

클래스 선언부(16–19행)를 다음으로 교체:

```kotlin
class LoginViewModel(
    private val authRepository: AuthRepository,
    exceptionFilter: ExceptionFilter,
) : BaseViewModel<LoginState, LoginIntent, LoginSideEffect>(
        initialState = LoginState(),
        exceptionFilter = exceptionFilter,
    ) {
```

- [ ] **Step 8: LoginViewModelTest에 방어적 바인딩 추가**

`LoginViewModelTest.kt` import에 추가:

```kotlin
import com.whatever.caro.core.viewmodel.ExceptionFilter
import org.koin.dsl.module
```

17행의 extensions 호출을 다음으로 교체:

```kotlin
        extensions(
            KoinExtension(
                listOf(
                    loginModule,
                    module {
                        single<ExceptionFilter> { ExceptionFilter.None }
                    },
                ),
            ),
        )
```

- [ ] **Step 9: login 테스트 통과 확인**

Run: `./gradlew :feature:login:testAndroidHostTest --console=plain`
Expected: PASS (테스트 0개여도 컴파일 성공이 검증 포인트)

- [ ] **Step 10: Commit**

```bash
./gradlew spotlessApply
git add feature
git commit -m "refactor: feature ViewModel 3개에 ExceptionFilter 생성자 파라미터 추가

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

### Task 4: CaroExceptionFilter를 policy 패키지로 이동

**Files:**
- Move: `composeApp/src/commonMain/kotlin/com/whatever/caro/composeApp/di/CaroExceptionFilter.kt` → `composeApp/src/commonMain/kotlin/com/whatever/caro/composeApp/policy/CaroExceptionFilter.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/whatever/caro/composeApp/di/AppPolicyModule.kt`

- [ ] **Step 1: 파일 이동 및 패키지 변경**

```bash
mkdir -p composeApp/src/commonMain/kotlin/com/whatever/caro/composeApp/policy
git mv composeApp/src/commonMain/kotlin/com/whatever/caro/composeApp/di/CaroExceptionFilter.kt \
       composeApp/src/commonMain/kotlin/com/whatever/caro/composeApp/policy/CaroExceptionFilter.kt
```

이동한 파일의 내용을 다음으로 교체 (package 선언만 변경):

```kotlin
package com.whatever.caro.composeApp.policy

import com.whatever.caro.core.model.exception.SilentlyHandledException
import com.whatever.caro.core.viewmodel.ExceptionFilter

internal class CaroExceptionFilter : ExceptionFilter {
    override fun shouldSuppress(throwable: Throwable): Boolean = throwable is SilentlyHandledException
}
```

- [ ] **Step 2: AppPolicyModule import 갱신**

`AppPolicyModule.kt` 전체를 다음으로 교체:

```kotlin
package com.whatever.caro.composeApp.di

import com.whatever.caro.composeApp.policy.CaroExceptionFilter
import com.whatever.caro.core.viewmodel.ExceptionFilter
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.plugin.module.dsl.single

val appPolicyModule =
    module {
        single<CaroExceptionFilter>() bind ExceptionFilter::class
    }
```

- [ ] **Step 3: 앱 빌드로 통합 확인**

Run: `./gradlew :androidApp:assembleDevDebug --console=plain`
Expected: BUILD SUCCESSFUL — `viewModel<HomeViewModel>()` 등이 `appPolicyModule`의 `bind ExceptionFilter::class` 바인딩을 해석하는 전체 배선이 컴파일된다.

- [ ] **Step 4: Commit**

```bash
./gradlew spotlessApply
git add composeApp
git commit -m "refactor: CaroExceptionFilter를 di 패키지에서 policy 패키지로 이동

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

### Task 5: BaseViewModel 기본값 제거 — 필수 파라미터화 (스펙 최종 상태)

**Files:**
- Modify: `core/viewmodel/src/commonMain/kotlin/com/whatever/caro/core/viewmodel/BaseViewModel.kt:26`

- [ ] **Step 1: 기본값 제거**

`BaseViewModel.kt`의 생성자 선언에서:

```kotlin
    private val exceptionFilter: ExceptionFilter = ExceptionFilter.None,
```

을 다음으로 교체:

```kotlin
    private val exceptionFilter: ExceptionFilter,
```

- [ ] **Step 2: 전 모듈 컴파일 + 테스트로 누락 VM이 없는지 검증**

Run: `./gradlew :core:viewmodel:testAndroidHostTest :feature:home:testAndroidHostTest :feature:splash:testAndroidHostTest :feature:login:testAndroidHostTest --console=plain`
Expected: PASS — 이 시점부터 `exceptionFilter`를 전달하지 않는 새 ViewModel은 **컴파일 에러**가 된다 (스펙 목표 달성 지점).

- [ ] **Step 3: Commit**

```bash
./gradlew spotlessApply
git add core/viewmodel/src/commonMain/kotlin/com/whatever/caro/core/viewmodel/BaseViewModel.kt
git commit -m "refactor: BaseViewModel의 ExceptionFilter 기본값 제거로 주입 필수화

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

### Task 6: 최종 검증 (커밋 없음, 검증만)

- [ ] **Step 1: 린트 검증**

Run: `./gradlew spotlessCheck --console=plain`
Expected: BUILD SUCCESSFUL

- [ ] **Step 2: 전체 테스트 + 앱 빌드 검증**

Run: `./gradlew :core:viewmodel:testAndroidHostTest :feature:home:testAndroidHostTest :feature:splash:testAndroidHostTest :feature:login:testAndroidHostTest :androidApp:assembleDevDebug --console=plain`
Expected: BUILD SUCCESSFUL — core:viewmodel 2개, home 1개, splash 2개 테스트 전부 PASS

- [ ] **Step 3: 잔여 서비스 로케이터 흔적 없는지 확인**

Run: `grep -rn "KoinComponent\|by inject()" core/viewmodel/src`
Expected: 출력 없음 (exit code 1)

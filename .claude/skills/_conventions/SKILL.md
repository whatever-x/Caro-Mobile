---
name: _conventions
description: >
  Caro-Mobile(KMP + Compose Multiplatform, com.whatever.caro)의 아키텍처 컨벤션 요약(스킬 참조용, 원문은 CLAUDE.md).
  MVI(BaseViewModel)·Koin 컴파일러 플러그인·Navigation3·Compose resources·expect/actual·Kotest
  규칙을 요약한다. 다른 Compose/Kotlin/KMP 스킬이 "우리 컨벤션은?"을 물을 때 이 문서를 근거로 삼는다.
  "Caro 컨벤션", "우리 컨벤션", "모듈 구조", "MVI 구조", "화면 추가 절차" 같은 맥락에서 사용한다.
---

# Caro 컨벤션 (요약 · 스킬 참조용)

이 문서는 벤더링한 Compose/Kotlin/KMP 스킬들이 반복 인용하는 **우리 프로젝트 규칙**을 한곳에 모은 것이다.
권위 있는 원문은 루트 `CLAUDE.md`이며, 이 문서는 스킬 적용 시 빠르게 참조하기 위한 요약이다.
충돌 시 `CLAUDE.md`가 우선한다.

## 모듈 구조 & 의존 규칙

```text
:androidApp → :composeApp
:composeApp → :core:* + :feature:*
:feature:*  → caro.feature 가 자동 주입: designsystem, data, ui, viewmodel, navigator
:core:data  → :core:remote, :core:model
:core:remote → :core:model
```

- **feature 모듈끼리 직접 의존 금지.** 화면 간 통신은 `:core:navigator`를 경유한다.
- 공용 인프라는 `:core:*`, 화면은 `:feature:*`.

## MVI (Intent / State / SideEffect)

ViewModel은 `BaseViewModel<S, I, SE>`를 상속한다
(`core/viewmodel/.../BaseViewModel.kt`). 파일 레이아웃:

```text
feature/<name>/
├── route/<Name>Route.kt   # 상태 수집 + side effect 처리 (Composable 진입점)
├── <Name>Screen.kt        # 순수 UI (state + onIntent 콜백만 받음)
├── <Name>ViewModel.kt     # BaseViewModel 상속, DI 모듈에 등록
├── di/<Name>Module.kt     # Koin module { viewModel<...>() }
└── mvi/
    ├── <Name>Intent.kt     # sealed interface (사용자 액션)
    ├── <Name>State.kt      # data class (UiState)
    └── <Name>SideEffect.kt # sealed interface (1회성 이벤트)
```

`BaseViewModel`이 제공하는 것 (직접 다른 패턴을 만들지 말 것):

```kotlin
val state: StateFlow<S>          // _state = MutableStateFlow(initial).asStateFlow()
val sideEffect: Flow<SE>         // _sideEffect = Channel<SE>(BUFFERED).receiveAsFlow()

fun intent(intent: I)            // launch { handleIntent(intent) }
protected fun reduce(reduce: S.() -> S)          // 동기 상태 갱신: reduce { copy(...) }
protected fun postSideEffect(sideEffect: SE)     // 1회성 이벤트 emit
protected fun launch(...) : Job  // viewModelScope + coroutineExceptionHandler
open fun handleClientException(throwable: Throwable)  // 공통 에러 처리 훅
protected abstract suspend fun handleIntent(intent: I)
```

- **상태 변경은 `reduce { copy(...) }`만 사용.** `_state.value =` 직접 대입 금지.
- **1회성 이벤트(네비게이션/토스트)는 `postSideEffect`.** state에 이벤트 플래그를 넣지 말 것.
- **비동기 작업은 `launch { }`.** 별도 `CoroutineScope`를 ViewModel에 저장하지 말 것.
- Repository는 `suspend` 함수를 노출하고, ViewModel의 `launch`가 스코프를 소유한다.

ViewModel 예시 (`feature/login/.../LoginViewModel.kt`):

```kotlin
class LoginViewModel(
    private val authRepository: AuthRepository,
) : BaseViewModel<LoginState, LoginIntent, LoginSideEffect>(initialState = LoginState()) {

    override suspend fun handleIntent(intent: LoginIntent) { /* when(intent) ... */ }

    private fun requestLogin(provider: SocialLoginType, idToken: String) {
        launch {
            reduce { copy(isLoading = true) }
            authRepository.loginWithSocial(provider = provider, idToken = idToken)
            reduce { copy(isLoading = false) }
            postSideEffect(LoginSideEffect.NavigateHome)
        }
    }
}
```

## Route / Screen 분리

- **Route**: `viewModel.state.collectAsStateWithLifecycle()`로 상태 수집,
  `LaunchedEffect`에서 `viewModel.sideEffect.collect { }`로 1회성 이벤트 처리,
  `NavigationDispatcher`로 네비게이션 emit. (`feature/home/.../route/HomeRoute.kt`)
- **Screen**: `@Composable internal fun XScreen(state: XState, onIntent: (XIntent) -> Unit)` —
  의존성·flow 수집·네비게이션 없이 **순수 UI**. 미리보기/테스트 가능해야 함. (`feature/home/.../HomeScreen.kt`)

```kotlin
@Composable
fun HomeRoute(viewModel: HomeViewModel, navDispatcher: NavigationDispatcher) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        viewModel.sideEffect.collect { effect ->
            when (effect) {
                is HomeSideEffect.NavigateToProfile -> navDispatcher.emit(To(CreateProfileEntry))
            }
        }
    }
    HomeScreen(state = state, onIntent = viewModel::intent)
}
```

## Koin DI (컴파일러 플러그인)

KSP/어노테이션 없이 **Koin Kotlin 컴파일러 플러그인** 단축 DSL을 쓴다.

```kotlin
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.plugin.module.dsl.single
import org.koin.plugin.module.dsl.viewModel

val homeModule = module { viewModel<HomeViewModel>() }   // 생성자 인자 자동 해결(NavKey 포함)

val dataModule = module {
    single<AuthRepositoryImpl>() bind AuthRepository::class
}
```

평범한 DSL(`single { ... }`)은 `named()` qualifier, 빌더 설정(`Json { }`), 단순 생성자 호출이 아닐 때만 사용.

## Navigation3 — 화면 추가 4단계

1. `:core:navigator`에 `@Serializable`한 `NavKey` 정의 (예: `data object SettingsEntry : NavKey`).
   파라미터는 `@Serializable data class` + `Payload` 패턴.
2. `CaroApp.kt`의 `SavedStateConfiguration` polymorphic 블록에 `subclass(...)` 등록.
3. `composeApp/di/NavigationModule.kt`에 `navigation<Key> { }` 추가
   (파라미터 화면은 `koinViewModel<VM> { parametersOf(navKey) }`).
4. feature 모듈에 Route/Screen/ViewModel/MVI 작성.

네비게이션 명령은 `NavigationDispatcher`를 통해 `NavCommand`(Back/To/Replace/ResetTo)로 흐른다.
UI는 "어디로 가라"가 아니라 SideEffect → Route에서 emit.

## Compose Resources (문자열)

- 사용자 노출 텍스트·접근성 라벨은 **반드시** `stringResource(Res.string...)` /
  `painterResource(Res.drawable...)`. (`org.jetbrains.compose.resources`)
- Screen/Route/컴포넌트에 하드코딩 문자열 금지(임시 디버그 UI 제외).

## 디자인 시스템 토큰

`CaroTheme` CompositionLocal로 접근:
`CaroTheme.color.*`, `CaroTheme.typography.*`, `CaroTheme.spacing.*`, `CaroTheme.shape.*`.
raw `Color(...)`, 매직 `dp` 색상/간격 대신 토큰 사용. (`core/designsystem/.../components/CaroTextField.kt`)

## 멀티플랫폼 (expect/actual)

- 플랫폼 분기는 `expect`/`actual` + 파일명 규칙 `Foo.android.kt` / `Foo.ios.kt`.
- `commonMain` 시그니처에 플랫폼 타입(`Context`, `Activity`, `Uri`, `UIViewController`) 노출 금지.
- 엔진 예: `core/remote/.../network/HttpClientEngineProvider.{kt, android.kt, ios.kt}` (OkHttp / Darwin).
- DI·테스트 페이크·런타임 선택이 필요하면 `expect class` 대신 commonMain 인터페이스 + 플랫폼 바인딩.

## 테스트 스택

- **Kotest** `FunSpec` + `init { }`, **KoinTest** + `KoinExtension(listOf(module))`.
- 코루틴: `StandardTestDispatcher` + `Dispatchers.setMain/resetMain` + `runTest { advanceUntilIdle() }`.
- **Mokkery**(모킹), **Turbine**(flow), **kotlinx-coroutines-test**.
- 소스셋: `commonTest`(공유), `androidHostTest`(Android 전용).

```kotlin
class HomeViewModelTest : FunSpec(), KoinTest {
    init {
        extensions(KoinExtension(listOf(homeModule)))
        val dispatcher = StandardTestDispatcher()
        beforeTest { Dispatchers.setMain(dispatcher) }
        afterTest { Dispatchers.resetMain(); dispatcher.cancel() }

        test("...") { runTest {
            val vm: HomeViewModel = get { parametersOf(navKey) }
            vm.init(); advanceUntilIdle()
            vm.state.value shouldBe HomeState(...)
        } }
    }
}
```

## 네이밍 규칙

- Remote data source 구현체는 인터페이스명이 data 계층과 헷갈릴 만큼 일반적이면 `Remote` 프리픽스
  (예: `RemoteProfileDataSourceImpl : ProfileDataSource`).

## 관련 스킬

이 문서를 근거로 삼는 벤더 스킬: `compose-state-holder-ui-split`, `compose-side-effects`,
`kotlin-flow-state-event-modeling`, `kotlin-coroutines-structured-concurrency`,
`kotlin-multiplatform-expect-actual`, `compose-slot-api-pattern`, `compose-ui-testing-patterns` 등.
리뷰 시에는 `caro-compose-review`가 이 규칙들을 체크리스트로 적용한다.

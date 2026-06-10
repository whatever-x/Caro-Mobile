# ExceptionFilter 필수 생성자 주입 재설계

- 날짜: 2026-06-10
- 브랜치: `feat/error-handler`
- 상태: 승인됨

## 배경 / 문제

`feat/error-handler` 브랜치는 전역 에러 핸들링을 도입하면서 `core:viewmodel`에
`ExceptionFilter`(fun interface)를 정의하고, composeApp의 `CaroExceptionFilter`
(`throwable is SilentlyHandledException` 검사)를 `appPolicyModule`로 바인딩했다.

문제는 전달 방식이다. `BaseViewModel`이 `KoinComponent`를 구현하고
`private val exceptionFilter: ExceptionFilter by inject()`로 런타임에 조회한다.

- `by inject()`는 서비스 로케이터라서 koin-compiler-plugin의 생성자 해석 검증을
  완전히 우회한다. `appPolicyModule` 등록이 빠지면 모든 ViewModel이 런타임에 죽는다.
- 이 프로젝트는 koin-compiler-plugin으로 컴파일 타임 의존성 검증을 받는 것이
  DI 전략의 핵심인데, 이 지점만 런타임 의존성이 된다.
- `core:viewmodel`에 `caro.koin` 플러그인 의존이 새로 생겨 모듈 순수성이 깨졌다.
- VM 테스트들은 `KoinExtension(listOf(homeModule))`처럼 feature 모듈만 등록하므로,
  에러 경로 테스트에서 필터 해석이 `CoroutineExceptionHandler` 내부에서 런타임
  실패하는 지뢰가 있다.

## 목표

1. **정책은 composeApp이 결정** — 어떤 예외를 거를지(`CaroExceptionFilter`)는
   composeApp만 안다. `core:viewmodel`은 정책을 모른다. (순수성의 정의)
2. **컴파일 타임 의존성 검증** — `ExceptionFilter` 전달이 koin-compiler-plugin의
   검증 경로(생성자 해석) 안에 들어가야 한다. 누락 시 컴파일 에러.
3. **core:viewmodel의 Koin 비의존 복원** — `caro.koin` 플러그인 제거.

## 범위

ExceptionFilter 주입 경로만 재설계한다. `AuthSessionEventBus` 배치(core:model 계약,
core:data 구현, CaroApp 수집)와 예외 계층(`CaroException` 등)은 이 브랜치 설계
그대로 유지한다.

## 검토한 대안

| 방식 | 누락 시 실패 모드 | 판정 |
|---|---|---|
| 현재 (`by inject()`) | 모든 VM 런타임 크래시 | 제거 대상 |
| **① 필수 생성자 주입** | **컴파일 에러** | **채택** |
| ② 기본값 생성자 주입 (`= None`) | 해당 화면만 조용히 필터 꺼짐 | 기각 |
| ③ 정적 레지스트리 (전역 object) | 조용히 필터 꺼짐 + 테스트 오염 | 기각 |

②를 기각한 이유: `AuthInterceptorPlugin` 때문에 `TokenExpired`는 어느 VM의
코루틴에서든 발생할 수 있다. 필터 선언을 깜빡한 화면에서만 세션 만료가 에러 UI로
노출되는 비일관 동작이 생기고, 컴파일 에러도 런타임 에러도 없어 발견이 가장 늦다.

③을 기각한 이유: 초기화 순서 의존(할당 전 VM 생성 시 조용히 미적용), 전역 가변
상태로 인한 테스트 간 누수, 어떤 시그니처에도 의존성이 드러나지 않음. `by inject()`의
숨은 의존성 문제를 형태만 바꿔 유지하는 것이라 목표 2에 부합하지 않는다.

## 설계

### core:viewmodel

```kotlin
// ExceptionFilter.kt — None 추가 (테스트/명시적 무필터 용)
fun interface ExceptionFilter {
    fun shouldSuppress(throwable: Throwable): Boolean

    companion object {
        val None = ExceptionFilter { false }
    }
}

// BaseViewModel.kt — Koin 제거, 필수 파라미터로 전환
abstract class BaseViewModel<S : UiState, I : UiIntent, SE : UiSideEffect>(
    initialState: S,
    private val exceptionFilter: ExceptionFilter,
) : ViewModel() {
    // KoinComponent 제거, by inject() 제거
    // coroutineExceptionHandler의 suppress 분기 로직은 현재와 동일 유지
}
```

`core/viewmodel/build.gradle.kts`에서 `caro.koin` 플러그인을 제거한다.
core:viewmodel은 lifecycle-viewmodel(+Napier)만 의존하는 상태로 복귀한다.

### feature 모듈 (home, splash, login — VM 3개)

각 VM에 파라미터 1개 추가 + super 전달, VM당 정확히 2줄:

```kotlin
class HomeViewModel(
    private val authRepository: AuthRepository,
    private val navKey: HomeEntry,
    exceptionFilter: ExceptionFilter,          // 추가
) : BaseViewModel<HomeState, HomeIntent, HomeSideEffect>(
        initialState = HomeState(),
        exceptionFilter = exceptionFilter,     // 추가
    )
```

**DI 모듈은 변경 없음.** `viewModel<HomeViewModel>()`이 `appPolicyModule`의
`bind ExceptionFilter::class` 바인딩에서 자동 해석한다. `navKey`처럼
`parametersOf`로 오는 인자와 빈으로 오는 인자가 섞인 기존 패턴 그대로다.

### composeApp

- `appPolicyModule`, `CaroExceptionFilter` 유지 — 정책 위치가 곧 설계 의도.
- `CaroExceptionFilter`를 `di/` 패키지에서 `policy/` 패키지로 이동한다
  (`composeApp/policy/CaroExceptionFilter.kt`). DI 배선이 아니라 정책 구현체이기
  때문이다.

### 테스트

- Koin으로 VM을 해석하는 테스트(`HomeViewModelTest`, `LoginViewModelTest`)는
  `KoinExtension` 모듈 목록에 테스트용 바인딩을 추가한다:

  ```kotlin
  extensions(
      KoinExtension(
          listOf(
              homeModule,
              module { single<ExceptionFilter> { ExceptionFilter.None } },
          ),
      ),
  )
  ```

- VM을 직접 생성하는 테스트(`SplashViewModelTest`의 `SplashViewModel()`)는
  생성자에 `ExceptionFilter.None`을 직접 전달한다.
- suppress 동작 자체(필터가 true를 반환하면 `handleClientException` 미호출)는
  fake `ExceptionFilter`를 생성자에 직접 넘겨 Koin 없이 검증할 수 있다.

## 변경 후 전체 흐름 (기존 의도 유지)

```
API 401 → AuthInterceptorPlugin → refresh 실패
  → AuthSessionEventBus.publish(Expired) + throw TokenExpired(SilentlyHandledException)
  → VM coroutineExceptionHandler → CaroExceptionFilter.shouldSuppress = true → 로그만 남김
  → CaroApp이 Expired 수집 → NavCommand.ResetTo(LoginEntry)
```

에러 UI 노출 없이 로그인 화면으로 리셋되는 동작은 그대로이며, 의존성 경로만
런타임 조회 → 컴파일 타임 검증으로 바뀐다.

## 영향 파일 목록

| 파일 | 변경 |
|---|---|
| `core/viewmodel/.../ExceptionFilter.kt` | `None` companion 추가 |
| `core/viewmodel/.../BaseViewModel.kt` | KoinComponent/inject 제거, 생성자 파라미터 추가 |
| `core/viewmodel/build.gradle.kts` | `caro.koin` 플러그인 제거 |
| `feature/home/.../HomeViewModel.kt` | 파라미터 추가 + super 전달 |
| `feature/splash/.../SplashViewModel.kt` | 파라미터 추가 + super 전달 |
| `feature/login/.../LoginViewModel.kt` | 파라미터 추가 + super 전달 |
| `feature/{home,login}/src/commonTest/.../*ViewModelTest.kt` | 테스트 모듈에 `ExceptionFilter.None` 바인딩 추가 |
| `feature/splash/src/commonTest/.../SplashViewModelTest.kt` | 생성자에 `ExceptionFilter.None` 직접 전달 |
| `composeApp/.../di/CaroExceptionFilter.kt` | `policy/` 패키지로 이동 |
| `composeApp/.../di/AppPolicyModule.kt` | import 경로만 갱신 |

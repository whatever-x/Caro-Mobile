---
name: kotlin-coroutines-structured-concurrency
description: >
  CoroutineScope를 프로퍼티로 저장하거나, init/비-suspend API에서 launch 하거나, runBlocking을 쓰거나,
  suspend 호출 주변에서 광범위 예외를 catch 하는 Kotlin 코드를 작성·리뷰할 때 사용한다.
  "코루틴 스코프", "structured concurrency", "suspend로 바꿔", "runBlocking", "CancellationException",
  "fire-and-forget", "스코프 어디에" 맥락에서 발동. Caro는 Repository=suspend, ViewModel=BaseViewModel.launch{} 규칙.
---

# Kotlin 코루틴: structured concurrency

> 출처: chrisbanes/skills (Apache-2.0) · 벤더링 2026-06-09. 핵심 원칙을 유지하고 Android 전용 carve-out(ContentProvider/Hilt)은 축약, Caro 매핑을 추가함.
> Caro 컨벤션 단일 출처: [`../_conventions/SKILL.md`](../_conventions/SKILL.md)

## Caro 적용 노트

- **Repository / DataSource는 `suspend` 함수만 노출한다.** `CoroutineScope`를 생성자에 받거나 프로퍼티로 저장하지 않는다.
  (예: `AuthRepository.loginWithSocial(...)`는 `suspend`.)
- **UI ↔ 상태홀더 경계는 `BaseViewModel.launch { }`이다.** ViewModel은 `intent()` → `handleIntent()`에서
  `launch { repo.xxx(); reduce { copy(...) }; postSideEffect(...) }` 형태로 비-suspend UI 이벤트를 lifecycle 스코프 작업으로 번역한다.
  이는 본 스킬 §3의 정당한 carve-out이다.
- `BaseViewModel.launch`는 내부적으로 `viewModelScope.launch(context + coroutineExceptionHandler)`이므로
  ViewModel에서 별도 스코프를 만들지 말 것. 공통 에러는 `handleClientException` 훅이 받는다.
- 테스트는 `runBlocking`이 아니라 `runTest` + `StandardTestDispatcher`.

## Core principle

잘 구조화된 코루틴은 자족적인 비동기 작업 단위다 — 진입 하나, 종료 하나, 호출 지점에서 알려진 lifecycle에 묶임.

**스코프는 대개 callee의 프로퍼티가 아니라 caller의 lifecycle에 묶여야 한다.** 저장된 `CoroutineScope`는
강한 리뷰 신호다: 그 클래스가 취소·에러 보고·재시작·lifecycle을 소유함을 증명해야 한다. 대부분의 repository,
manager, use case, data source는 그걸 증명할 수 없으므로 `suspend` API를 노출해야 한다.

수정은 거의 항상 같다: **API를 `suspend`로 만들고 스코프는 caller가 소유하게 한다.**

## 언제 쓰나

Kotlin 코드를 작성·리뷰하다 다음을 볼 때:

- `private val scope: CoroutineScope` (생성자 인자를 프로퍼티로 저장)
- `init { scope.launch { ... } }`
- 본문이 `scope.launch { ... }`인 비-suspend public 함수
- suspend 가능한 앱 코드의 `runBlocking { ... }`, 또는 `runTest`를 써야 할 테스트의 `runBlocking`
- `runCatching { suspendCall() }` 또는 suspend 호출 주변의 `Exception`/`Throwable` catch에서 `CancellationException` 미재전파
- 취소를 재전파하지 않는 `catch (e: CancellationException)`

## 조용한-취소 버그

저장된 `CoroutineScope` 프로퍼티가 위험한 이유: **스코프가 한 번 취소되면 이후 그 위에서의 모든 `launch`는
예외도 로그도 없이 조용히 취소로 완료된다.** 작업이 그냥 안 일어난다. 진단하기 가장 어려운 코루틴 버그.

API가 `suspend`면 이 일은 불가능하다: caller의 스코프가 살아있거나(작업 실행) 호출 지점이 취소한다(caller가 안다).

## 안티패턴과 수정

### 1. 프로퍼티로 저장된 CoroutineScope

```kotlin
// ❌ BAD
class AuthRepositoryImpl(
    private val scope: CoroutineScope,
    private val dataSource: RemoteAuthDataSource,
) : AuthRepository {
    fun refresh() { scope.launch { _state.value = dataSource.fetchUser() } }
}

// ✅ GOOD (Caro: repository는 suspend만)
class AuthRepositoryImpl(
    private val dataSource: RemoteAuthDataSource,
) : AuthRepository {
    override suspend fun refresh(): User = dataSource.fetchUser()
}
```

Repository는 코루틴을 알 필요가 없다. 스코프·에러 처리·취소 시맨틱은 caller(ViewModel)가 정한다.

### 2. init 블록 launch

```kotlin
// ❌ BAD: 생성 시점 부수효과, 무한 작업
class UserSession(private val scope: CoroutineScope, private val ds: DataSource) {
    init { scope.launch { _user.value = ds.load() } }
}

// ✅ GOOD: 명시적 부트스트랩, caller가 suspension 소유
class UserSession(private val ds: DataSource) {
    private var _user: User? = null
    val user: User get() = checkNotNull(_user) { "Call init() first" }
    suspend fun init() { _user = ds.load() }
}
```

### 3. 비-UI 클래스의 fire-and-forget

비-UI 클래스(repository, manager, use case, data source)의 비-suspend public 함수가 클래스 소유 스코프로
launch 하는 것. caller는 결과·에러·취소를 못 받고 작업이 실행됐는지도 보장 못 한다 → `suspend`로 바꾼다.

#### Carve-out: UI ↔ 상태홀더 경계 (Caro의 BaseViewModel)

UI 프레임워크는 비-suspend다. Composable의 `onClick`은 suspend 불가. **상태홀더(Caro의 `BaseViewModel`)**가
1회성 UI 이벤트를 UI lifecycle에 묶인 비동기 작업으로 번역하는 경계다 — 그게 그 역할이다.

```kotlin
// ✅ GOOD — BaseViewModel이 비-suspend UI 이벤트를 자기 스코프로 흡수
class LoginViewModel(private val authRepository: AuthRepository) :
    BaseViewModel<LoginState, LoginIntent, LoginSideEffect>(LoginState()) {

    private fun requestLogin(provider: SocialLoginType, idToken: String) {
        launch {                                  // viewModelScope + 공통 예외 핸들러
            authRepository.loginWithSocial(provider, idToken)
            postSideEffect(LoginSideEffect.NavigateHome)
        }
    }
}
```

세 조건이 모두 성립할 때만 carve-out이다: (1) UI surface의 상태홀더, (2) lifecycle 묶인 스코프(`viewModelScope`),
(3) caller가 진짜 UI 이벤트. Repository/manager가 상태홀더를 통해 호출하는 건 해당 안 됨.

### 4. 주입되지 않고 내부 생성된 스코프

```kotlin
// ❌ BAD — 내부 생성 저장 스코프
class FooManager {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
}
```

lifecycle 소유자가 없고 영원히 산다. `suspend` API로 교체.

### 5. DI 바인딩 싱글톤/이니셜라이저가 launch

DI 바인딩 클래스가 생성자/`init`에서 코루틴을 launch 하면: 시작 시점 비결정적, lifecycle 관측 불가,
stop/restart 경로 없음, launch 지점 grep 불가. 

먼저 물어라: **이 백그라운드 루프 클래스가 존재할 필요가 있나?** 대개는 관측을 소비자 쪽으로 뒤집으면(Pattern 1)
클래스 자체가 사라진다. 정말 주기적이면 스케줄 작업(Pattern 2), 동기 API가 호출해오면 명시적 named launch 지점(Pattern 3).
`init`/`initialize()`에서 launch 하지 말 것. (이니셜라이저는 *등록*만 해야 한다.)

### 6. CancellationException 삼키기

suspend 호출 주변 `catch`가 `CancellationException`을 (직접 또는 `Exception`/`Throwable`로) 매치하고
재전파 안 하면, 보통 취소를 조용한 성공으로 바꾼다.

```kotlin
// ❌ BAD — CancellationException도 잡고 재전파 안 함
try { ds.load() } catch (e: Exception) { Napier.w { "load failed" } }

// ✅ 분리 catch
try { ds.load() }
catch (e: CancellationException) { throw e }
catch (e: Exception) { Napier.w { "load failed" } }

// ✅ 광범위 catch 안에서 조건부 재전파
try { ds.load() }
catch (e: Exception) {
    if (e is CancellationException) throw e
    Napier.w { "load failed" }
}

// ✅ ensureActive()
try { ds.load() }
catch (e: Exception) { currentCoroutineContext().ensureActive(); Napier.w { "load failed" } }
```

> Caro의 `BaseViewModel.launch`는 `coroutineExceptionHandler`를 달고 있어 일반 실패는 `handleClientException`로
> 흘려보낸다. 그래도 명시적 `try/catch`를 쓸 땐 `CancellationException`을 반드시 재전파할 것.

비-취소 서브타입(`IOException`, 자체 예외) catch는 괜찮다 — `CancellationException`을 상속하지 않는다.

### 7. runBlocking

`runBlocking`은 람다가 끝날 때까지 스레드를 멈춘다. suspend 가능/lifecycle 스코프 앱 경로에선 틀렸다.

```kotlin
// ❌ BAD — 호출 스레드를 막아 suspend로 다리 놓기
fun saveUser(user: User) { runBlocking { repository.save(user) } }

// ✅ GOOD — 함수를 suspend로
suspend fun saveUser(user: User) = repository.save(user)
```

테스트는 `runBlocking` 대신 `runTest`(가상 시간, `TestDispatcher`, 적절한 정리). 

> Caro 참고: Android의 `ContentProvider` carve-out 같은 동기 프레임워크 경계는 이 KMP 앱의 commonMain 코드와
> 무관하다. 불가피한 동기 경계가 생기면 본문을 최소화하고 즉시 suspend 코드로 진입.

## Quick reference

| 증상 | 안티패턴 | 수정 |
|---|---|---|
| 클래스가 `private val scope: CoroutineScope` | 저장 스코프 | 제거, public API를 `suspend`로 |
| `init { scope.launch { ... } }` | 생성 시점 launch | `suspend fun init()`으로 이동 |
| repository/manager에서 `fun foo() { scope.launch {...} }` | 비-UI fire-and-forget | `suspend fun foo()`, UI 상태홀더가 스코프 선택 |
| `BaseViewModel`에서 `fun onClick() { launch {...} }` | UI↔상태홀더 경계 | 그대로 유지 (§3 carve-out) |
| `try { suspendCall() } catch(Exception) {}` 재전파 없음 | 취소 삼킴 (§6) | `catch (e: CancellationException) { throw e }` 우선 |
| 앱 코드 `runBlocking { ... }` | 스레드 차단 다리 (§7) | caller를 `suspend`로 |
| 테스트 `runBlocking { ... }` | 실시간 다리 (§7) | `runTest { ... }` |

## When NOT to apply

- **UI 이벤트를 흡수하는 상태홀더**(Caro `BaseViewModel`의 `launch`) — 정상.
- **명시적 취소/에러 정책을 가진 lifecycle 소유자** — `close`/`cancel`/restart를 노출하면 스코프 소유 가능. 단 `init`에서 launch는 아님.
- **이미 suspend인 API**.
- **테스트의 의도적 `TestScope`**.

## 리뷰 시 위험 신호

| 생각 | 현실 |
|---|---|
| "스코프에 `CoroutineExceptionHandler`만 달면 돼" | 문제는 에러 처리가 아니라 스코프가 존재하면 안 된다는 것. |
| "데이터 준비되게 init에서 launch 해야 해" | 준비 안 된 상태를 읽는 게 버그. phasing 사용. |
| "caller가 suspend 다루기 싫어해" | 그럼 caller가 자기 스코프에서 fire-and-forget 선택. 대신 결정하지 말 것. |
| "작은 fire-and-forget일 뿐" | 조용한 취소는 모든 fire-and-forget을 잠재적 조용한 실패로 만든다. |
| "잡아서 로그했으니 괜찮아" | `CancellationException`을 재전파했나? 아니면 조용히 취소 해제됨. (§6) |

## 관련

- [`../kotlin-flow-state-event-modeling/SKILL.md`](../kotlin-flow-state-event-modeling/SKILL.md) — StateFlow/SharedFlow/Channel, 1회성 이벤트.
- [`../compose-side-effects/SKILL.md`](../compose-side-effects/SKILL.md) — Compose에서 effect/flow 수집.
- [`../_conventions/SKILL.md`](../_conventions/SKILL.md) — BaseViewModel.launch / Repository suspend 규칙.

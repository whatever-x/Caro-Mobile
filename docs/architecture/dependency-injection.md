# Koin 의존성 주입

[전체 아키텍처](../../ARCHITECTURE.md)의 상세 지침입니다.
이 문서의 주제에 해당하는 코드를 구현·변경·리뷰하기 전에 읽습니다.
공통 작업 규칙은 루트 [AGENTS.md](../../AGENTS.md)를 따릅니다. 빌드·테스트 지침은 [문서 탐색 목록](../index.md)에서 선택합니다.

의존성 주입(DI)은 **Koin Kotlin 컴파일러 플러그인**(`io.insert-koin.compiler.plugin`)을 사용합니다.
DI에 KSP나 어노테이션을 사용하지 않습니다.
플러그인은 `org.koin.plugin.module.dsl` 패키지에서 단축 DSL 함수를 제공합니다.
단축 DSL 함수는 다음 두 경로로 생성자 인자를 받습니다.

- `get()`은 등록된 의존성을 조회합니다.
- `parametersOf(...)`는 호출 지점에서 지정한 인자를 전달합니다.

```kotlin
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.plugin.module.dsl.single
import org.koin.plugin.module.dsl.viewModel

val dataModule =
    module {
        single<AuthRepositoryImpl>() bind AuthRepository::class  // 구현체를 인터페이스로 등록
        single<FcmTokenRepositoryImpl>() bind FcmTokenRepository::class
    }

val homeModule =
    module {
        viewModel<HomeViewModel>()  // 등록된 의존성을 조회하고 parametersOf의 값을 받아 생성자 인자를 해결
    }
```

단축 DSL로 바인딩을 표현할 수 없을 때만 일반 DSL 함수(`single { ... }`, `viewModel { ... }`)를 사용합니다.
다음 경우에 일반 DSL을 사용합니다.

- 등록할 의존성에 `named()` 한정자를 지정하는 경우
- `get(named(...))`로 의존성을 조회하는 경우
- 빌더 설정이 필요한 경우(`Json { ... }`, `DataStoreFactory.create(producePath = { ... })`)
- 단순 생성자 호출 이외의 로직이 필요한 경우

한정자 사용 예시는 `NetworkModule`, `ApiModule`, `RemoteModule`을 참고합니다.

전체 모듈은 다음 파일의 `initKoin()`에서 연결합니다.

`composeApp/src/commonMain/kotlin/com/whatever/caro/composeApp/di/Koin.kt`
모듈 목록에는 `navEntryModule`, core 모듈, feature 모듈이 포함됩니다.

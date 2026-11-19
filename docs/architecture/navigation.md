# Navigation3 내비게이션

[전체 아키텍처](../../ARCHITECTURE.md)의 상세 지침입니다.
이 문서의 주제에 해당하는 코드를 구현·변경·리뷰하기 전에 읽습니다.
공통 작업 규칙은 루트 [AGENTS.md](../../AGENTS.md)를 따릅니다. 빌드·테스트 지침은 [문서 탐색 목록](../index.md)에서 선택합니다.

내비게이션 키는 `:core:navigator`에 정의합니다.
키는 `NavKey`를 상속하는 data object 또는 data class로 선언합니다.
키에 `@Serializable`을 붙입니다.
내비게이션 명령은 `NavigationDispatcher`를 통해 전달합니다.
sealed 타입인 `NavCommand`의 `Back`, `To`, `Replace`, `ResetTo`를 사용합니다.

**새 화면 추가 절차:**

1. `@Serializable` NavKey를 `:core:navigator`에 정의합니다(예: `data object SettingsEntry : NavKey`).
2. `CaroApp.kt`의 `SavedStateConfiguration`에 해당 키의 다형성 직렬화기를 등록합니다.
3. [의존성 주입 지침](dependency-injection.md#koin-의존성-주입)에 따라 `navigation<Key> { }` 항목을 `composeApp/di/NavigationModule.kt`에 추가합니다.
4. [MVI와 UI 작성 지침](feature-ui.md#mvi와-ui-작성)에 따라 feature 모듈에 Route·Screen·ViewModel·MVI 클래스를 작성합니다.

새 feature 모듈을 추가하면 [컨벤션 플러그인 지침](convention-plugins.md#gradle-컨벤션-플러그인)도 확인합니다.

**파라미터 전달:**

전달할 각 값은 `@Serializable` NavKey data class의 생성자 프로퍼티로 선언합니다.
전달할 값을 중첩된 `Payload` 객체로 감싸지 않습니다.
파일 최상위의 `Payload` 객체로도 감싸지 않습니다.
기존 예시는 `EditDeckEntry`와 `EditProfileEntry`를 참고합니다.

```kotlin
@Serializable
data class EditDeckEntry(
    val deckName: String,
    val deckDescription: String,
    val deckId: Long,
) : NavKey
```

`NavigationModule.kt`의 라우트 람다에서 각 값을 `parametersOf`로 개별 전달합니다.
예: `parametersOf(navKey.deckId, navKey.deckName, navKey.deckDescription)`.
ViewModel은 `EditDeckViewModel(deckName, deckDescription, deckId, ...)`처럼 각 값을 별도의 생성자 인자로 받습니다.
Koin 컴파일러 플러그인은 `parametersOf`로 전달한 값을 받습니다.
플러그인은 이 값으로 `viewModel<EditDeckViewModel>()`의 인자를 해결합니다.
NavKey 파라미터는 평탄한 구조를 유지합니다.
ViewModel이 NavKey 인자를 선언한 경우에만 `parametersOf(navKey)`로 NavKey 전체를 전달합니다.

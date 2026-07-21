# Snackbar Action Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Show a Figma-matching `바로가기` action after deck creation and navigate to the newly created deck detail when the action is selected.

**Architecture:** The data layer returns the created `Deck`, the create feature emits it as a one-shot side effect, and the Route sends an app-wide snackbar message containing an optional action callback. The design-system snackbar uses Material's `SnackbarVisuals.actionLabel`, `SnackbarData.performAction()`, and `SnackbarResult.ActionPerformed` contract so callbacks run only for an explicit selection.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform Material 3, Navigation3, Kotlin coroutines/Flow, Kotest, Mokkery, Turbine, Gradle.

## Global Constraints

- Preserve all pre-existing uncommitted UI changes and stage only files listed by the current task.
- Keep user-visible text in Compose resources for both Korean and English.
- Use `CaroTheme` typography, color, spacing, and shape tokens; do not introduce raw color or spacing values.
- Existing snackbars without an action must retain their current behavior and layout without reserved action space.
- A snackbar timeout, replacement, or dismissal must not execute the action callback.
- Do not change snackbar duration, animation, or the general `DeckDetailEntry` payload contract.

---

### Task 1: Return the Created Deck from the Repository

**Files:**
- Modify: `core/data/src/commonTest/kotlin/com/whatever/caro/core/data/repository/deck/DeckRepositoryImplTest.kt`
- Modify: `core/data/src/commonMain/kotlin/com/whatever/caro/core/data/repository/deck/DeckRepository.kt`
- Modify: `core/data/src/commonMain/kotlin/com/whatever/caro/core/data/repository/deck/DeckRepositoryImpl.kt`
- Modify: `core/data/src/commonMain/kotlin/com/whatever/caro/core/data/mapper/DeckMapper.kt`
- Modify: `feature/learning/src/commonTest/kotlin/LearningViewModelTest.kt`

**Interfaces:**
- Consumes: `CreateDeckResponse(id, deckName, deckDescription)` from `DeckDataSource.createDeck`.
- Produces: `suspend fun DeckRepository.createDeck(name: String, description: String): Deck` and `internal fun CreateDeckResponse.toDeckModel(fallbackName: String, fallbackDescription: String): Deck`.

- [ ] **Step 1: Write failing repository tests**

Update the existing create test to assert the returned model and add invalid-ID/fallback coverage:

```kotlin
val result = repository.createDeck(name = "영어 단어", description = "일상 단어")

result shouldBe
    Deck(
        id = 1L,
        title = "영어 단어",
        description = "일상 단어",
        cardTotalCount = 0,
        todayLearningCount = 0,
        todayCompleteCount = 0,
        state = DeckState.NOT_STARTED,
    )
```

```kotlin
test("createDeck은 응답의 이름과 설명이 없으면 요청 값을 사용한다") {
    runTest {
        val dataSource = mock<DeckDataSource> {
            everySuspend { createDeck(any()) } returns
                CreateDeckResponse(id = 2L, deckName = null, deckDescription = null)
        }
        DeckRepositoryImpl(dataSource).createDeck("요청 이름", "요청 설명") shouldBe
            Deck(2L, "요청 이름", "요청 설명", 0, 0, 0, DeckState.NOT_STARTED)
    }
}

test("createDeck은 응답 id가 없으면 유효하지 않은 응답 예외를 던진다") {
    runTest {
        val dataSource = mock<DeckDataSource> {
            everySuspend { createDeck(any()) } returns
                CreateDeckResponse(id = null, deckName = "이름", deckDescription = "설명")
        }
        shouldThrow<CaroInvalidResponseException> {
            DeckRepositoryImpl(dataSource).createDeck("이름", "설명")
        }
    }
}
```

- [ ] **Step 2: Run the focused test and verify RED**

Run: `./gradlew :core:data:testAndroidHostTest --tests '*DeckRepositoryImplTest*'`

Expected: compilation/test failure because `createDeck` still returns `Unit` and no create-response mapper exists.

- [ ] **Step 3: Implement the repository contract and mapping**

Change the contract and implementation to return the mapped response:

```kotlin
suspend fun createDeck(name: String, description: String): Deck
```

```kotlin
return deckDataSource.createDeck(request).toDeckModel(
    fallbackName = name,
    fallbackDescription = description,
)
```

Add the mapper:

```kotlin
internal fun CreateDeckResponse.toDeckModel(
    fallbackName: String,
    fallbackDescription: String,
): Deck =
    Deck(
        id = id ?: throw CaroInvalidResponseException("CreateDeckResponse.id is null"),
        title = deckName ?: fallbackName,
        description = deckDescription ?: fallbackDescription,
        cardTotalCount = 0,
        todayLearningCount = 0,
        todayCompleteCount = 0,
        state = DeckState.NOT_STARTED,
    )
```

Update the `LearningViewModelTest` fake repository override to return a `Deck` with the submitted name/description and a stable test ID.

- [ ] **Step 4: Run the focused tests and verify GREEN**

Run: `./gradlew :core:data:testAndroidHostTest --tests '*DeckRepositoryImplTest*' :feature:learning:testAndroidHostTest`

Expected: all selected tests pass.

- [ ] **Step 5: Commit only Task 1 files**

```bash
git add core/data/src/commonMain/kotlin/com/whatever/caro/core/data/repository/deck/DeckRepository.kt core/data/src/commonMain/kotlin/com/whatever/caro/core/data/repository/deck/DeckRepositoryImpl.kt core/data/src/commonMain/kotlin/com/whatever/caro/core/data/mapper/DeckMapper.kt core/data/src/commonTest/kotlin/com/whatever/caro/core/data/repository/deck/DeckRepositoryImplTest.kt feature/learning/src/commonTest/kotlin/LearningViewModelTest.kt
git commit -m "refactor: return created deck from repository"
```

### Task 2: Emit the Created Deck from the Create Feature

**Files:**
- Modify: `feature/deck/src/commonTest/kotlin/CreateDeckViewModelTest.kt`
- Modify: `feature/deck/src/commonMain/kotlin/com/whatever/caro/feature/deck/create/CreateDeckViewModel.kt`
- Modify: `feature/deck/src/commonMain/kotlin/com/whatever/caro/feature/deck/create/mvi/CreateDeckSideEffect.kt`

**Interfaces:**
- Consumes: `DeckRepository.createDeck(...): Deck` from Task 1.
- Produces: `data class CreateDeckSideEffect.Created(val deck: Deck)` while retaining `NavigateBack` for explicit back navigation.

- [ ] **Step 1: Write the failing ViewModel test**

Replace the success mock's `Unit` with `createdDeck` and assert the new side effect:

```kotlin
val createdDeck = Deck(
    id = 7L,
    title = "영어 단어 2000개",
    description = "일상에서 많이 쓰는 단어",
    cardTotalCount = 0,
    todayLearningCount = 0,
    todayCompleteCount = 0,
    state = DeckState.NOT_STARTED,
)
val deckRepository = mock<DeckRepository> {
    everySuspend { createDeck(any(), any()) } returns createdDeck
}

viewModel.sideEffect.test {
    viewModel.intent(CreateDeckIntent.ClickConfirm)
    awaitItem() shouldBe CreateDeckSideEffect.Created(createdDeck)
}
```

Update the default test repository to return a stable `Deck` so unrelated tests compile.

- [ ] **Step 2: Run the focused test and verify RED**

Run: `./gradlew :feature:deck:testAndroidHostTest --tests '*CreateDeckViewModelTest*'`

Expected: compilation failure because `CreateDeckSideEffect.Created` does not exist.

- [ ] **Step 3: Implement the created side effect**

```kotlin
sealed interface CreateDeckSideEffect : UiSideEffect {
    data object NavigateBack : CreateDeckSideEffect
    data class Created(val deck: Deck) : CreateDeckSideEffect
    data object ShowError : CreateDeckSideEffect
}
```

Capture and emit the repository result:

```kotlin
val deck = deckRepository.createDeck(
    name = currentState.name,
    description = currentState.description,
)
postSideEffect(CreateDeckSideEffect.Created(deck))
```

- [ ] **Step 4: Run the focused test and verify GREEN**

Run: `./gradlew :feature:deck:testAndroidHostTest --tests '*CreateDeckViewModelTest*'`

Expected: all `CreateDeckViewModelTest` tests pass.

- [ ] **Step 5: Commit only Task 2 files**

```bash
git add feature/deck/src/commonMain/kotlin/com/whatever/caro/feature/deck/create/CreateDeckViewModel.kt feature/deck/src/commonMain/kotlin/com/whatever/caro/feature/deck/create/mvi/CreateDeckSideEffect.kt feature/deck/src/commonTest/kotlin/CreateDeckViewModelTest.kt
git commit -m "feat: emit created deck after creation"
```

### Task 3: Add the Snackbar Action Contract and Figma Layout

**Files:**
- Modify: `core/designsystem/build.gradle.kts`
- Create: `core/designsystem/src/commonTest/kotlin/com/whatever/caro/core/designsystem/components/SnackbarTest.kt`
- Modify: `core/designsystem/src/commonMain/kotlin/com/whatever/caro/core/designsystem/components/Snackbar.kt`
- Modify: `core/ui/src/commonMain/kotlin/com/whatever/caro/core/ui/snackbar/SnackBarMessage.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/whatever/caro/composeApp/CaroApp.kt`

**Interfaces:**
- Consumes: optional `actionLabel: String?` and `onAction: (suspend () -> Unit)?` from snackbar callers.
- Produces: `CaroSnackbarVisuals.actionLabel`, message-only compatibility, and callback execution only after `SnackbarResult.ActionPerformed`.

- [ ] **Step 1: Enable design-system tests and write failing action-result tests**

Apply `id("caro.kmp.test")` to `core/designsystem/build.gradle.kts`. Add tests using a real `SnackbarHostState`:

```kotlin
test("액션을 수행하면 onAction을 한 번 호출한다") {
    runTest {
        val hostState = SnackbarHostState()
        var calls = 0
        showSnackbarMessage(
            coroutineScope = this,
            snackbarHostState = hostState,
            message = "완료",
            actionLabel = "바로가기",
            onAction = { calls += 1 },
        )
        runCurrent()
        hostState.currentSnackbarData?.performAction()
        advanceUntilIdle()
        calls shouldBe 1
    }
}

test("액션 없이 닫히면 onAction을 호출하지 않는다") {
    runTest {
        val hostState = SnackbarHostState()
        var calls = 0
        showSnackbarMessage(
            coroutineScope = this,
            snackbarHostState = hostState,
            message = "완료",
            actionLabel = "바로가기",
            onAction = { calls += 1 },
        )
        runCurrent()
        hostState.currentSnackbarData?.dismiss()
        advanceUntilIdle()
        calls shouldBe 0
    }
}
```

- [ ] **Step 2: Run the focused test and verify RED**

Run: `./gradlew :core:designsystem:testAndroidHostTest --tests '*SnackbarTest*'`

Expected: compilation failure because `showSnackbarMessage` has no action parameters.

- [ ] **Step 3: Implement action propagation and rendering**

Add nullable action fields to `SnackBarMessage`, pass them through `CaroApp`, and extend `CaroSnackbarVisuals` with the standard label:

```kotlin
override val actionLabel: String? = null
```

Add `actionLabel: String? = null` and `onAction: (suspend () -> Unit)? = null` parameters to `showSnackbarMessage`; the helper keeps the callback outside the visual model while it awaits the snackbar result.

Await the Material result and call the callback only for an explicit action:

```kotlin
val result = snackbarHostState.showSnackbar(
    CaroSnackbarVisuals(message, style, duration, actionLabel),
)
if (result == SnackbarResult.ActionPerformed) onAction?.invoke()
```

Render a `Row(horizontalArrangement = Arrangement.spacedBy(CaroTheme.spacing.s))` with optional action text. Apply Figma tokens: horizontal `CaroTheme.spacing.l`, vertical `CaroTheme.spacing.m`, message `body2.medium`, action `label2.regular`, action color `CaroTheme.color.text.brand`, and `Role.Button` semantics. The click handler calls `snackbarData.performAction()`.

- [ ] **Step 4: Run the focused test and verify GREEN**

Run: `./gradlew :core:designsystem:testAndroidHostTest --tests '*SnackbarTest*'`

Expected: both snackbar action tests pass.

- [ ] **Step 5: Commit only Task 3 files**

```bash
git add core/designsystem/build.gradle.kts core/designsystem/src/commonTest/kotlin/com/whatever/caro/core/designsystem/components/SnackbarTest.kt core/designsystem/src/commonMain/kotlin/com/whatever/caro/core/designsystem/components/Snackbar.kt core/ui/src/commonMain/kotlin/com/whatever/caro/core/ui/snackbar/SnackBarMessage.kt composeApp/src/commonMain/kotlin/com/whatever/caro/composeApp/CaroApp.kt
git commit -m "feat: support snackbar actions"
```

### Task 4: Wire Deck Creation to the Snackbar Action

**Files:**
- Modify: `core/designsystem/src/commonMain/composeResources/values-ko/strings.xml`
- Modify: `core/designsystem/src/commonMain/composeResources/values/strings.xml`
- Modify: `feature/deck/src/commonMain/kotlin/com/whatever/caro/feature/deck/create/CreateDeckRoute.kt`

**Interfaces:**
- Consumes: `CreateDeckSideEffect.Created(deck)` from Task 2 and snackbar action fields from Task 3.
- Produces: back navigation followed by a success snackbar whose action emits `NavCommand.To(DeckDetailEntry(deck))`.

- [ ] **Step 1: Add localized resources**

Add Korean resources:

```xml
<string name="deck_snackbar_create_success">덱 생성이 완료되었습니다.</string>
<string name="deck_snackbar_action_open">바로가기</string>
```

Add English resources:

```xml
<string name="deck_snackbar_create_success">Deck created successfully.</string>
<string name="deck_snackbar_action_open">View deck</string>
```

- [ ] **Step 2: Wire the route**

Resolve both resources before the long-lived effect. Handle creation success as follows:

```kotlin
is CreateDeckSideEffect.Created -> {
    navDispatcher.emit(command = NavCommand.Back)
    snackbarController.show(
        SnackBarMessage(
            message = createSuccessMessage,
            actionLabel = openDeckActionLabel,
            onAction = {
                navDispatcher.emit(
                    command =
                        NavCommand.To(
                            DeckDetailEntry(
                                payload = DeckDetailEntry.Payload(deck = sideEffect.deck),
                            ),
                        ),
                )
            },
        ),
    )
}
```

Keep `NavigateBack` and `ShowError` behavior unchanged.

- [ ] **Step 3: Compile the integration**

Run: `./gradlew :feature:deck:testAndroidHostTest :androidApp:assembleDevDebug`

Expected: feature tests pass and the Android dev debug app assembles successfully.

- [ ] **Step 4: Run formatting without rewriting unrelated user files**

Run: `./gradlew spotlessCheck`

Expected: pass. If existing unrelated files fail, run Spotless only on files from Tasks 1-4 or apply equivalent formatting manually, then report the unrelated failures without modifying those files.

- [ ] **Step 5: Run final focused verification**

Run: `./gradlew :core:data:testAndroidHostTest :core:designsystem:testAndroidHostTest :feature:deck:testAndroidHostTest :feature:learning:testAndroidHostTest :androidApp:assembleDevDebug spotlessCheck`

Expected: build succeeds with zero test or formatting failures.

- [ ] **Step 6: Commit only Task 4 files**

```bash
git add core/designsystem/src/commonMain/composeResources/values-ko/strings.xml core/designsystem/src/commonMain/composeResources/values/strings.xml feature/deck/src/commonMain/kotlin/com/whatever/caro/feature/deck/create/CreateDeckRoute.kt
git commit -m "feat: open created deck from snackbar"
```

package com.whatever.caro.feature.importer.step

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.core.importparser.model.ParsedTable
import com.whatever.caro.core.model.importing.FieldMapping
import com.whatever.caro.feature.importer.ImportDeckScreen
import com.whatever.caro.feature.importer.component.isHeading
import com.whatever.caro.feature.importer.mvi.ImportDeckIntent
import com.whatever.caro.feature.importer.mvi.ImportDeckState
import com.whatever.caro.feature.importer.mvi.ImportLoading
import com.whatever.caro.feature.importer.mvi.ImportStep
import io.kotest.matchers.floats.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import kotlinx.collections.immutable.PersistentMap
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

// 항목 연결 단계는 Screen이 상태의 연결을 골라 넘기고 intent로 되돌려야 하므로 Screen을 통해 확인한다.
// 로딩 카드의 무한 반복 Lottie 때문에 자동 시계를 켜 두면 waitForIdle이 끝나지 않는 경우에는 시계를 직접 움직인다.
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "ko-rKR-w393dp-h852dp-xxhdpi")
class MappingStepWiringTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val intents = mutableListOf<ImportDeckIntent>()
    private var screenState by mutableStateOf(ImportDeckState())

    private val vocabTable = MappingStepTables.table(MappingStepTables.vocab())

    private fun show(
        state: ImportDeckState,
        manualClock: Boolean = false,
    ) {
        screenState = state
        if (manualClock) composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            CaroTheme {
                ImportDeckScreen(state = screenState, onIntent = { intents += it })
            }
        }
        if (manualClock) composeRule.mainClock.advanceTimeByFrame() else composeRule.waitForIdle()
    }

    private fun mappingState(
        table: ParsedTable = vocabTable,
        mappings: PersistentMap<String, FieldMapping> = MappingStepTables.defaultMappings(table),
        loading: ImportLoading? = null,
    ) = ImportDeckState(step = ImportStep.Mapping, table = table, mappings = mappings, loading = loading)

    private fun wideState() =
        mappingState(
            table = MappingStepTables.table(MappingStepTables.wide(fieldCount = 20)),
            mappings = MappingStepTables.wideMappings(),
        )

    private fun manyTypesTable() =
        MappingStepTables.table(*List(8) { index -> MappingStepTables.vocab(id = "type$index", name = "유형 $index") }.toTypedArray())

    private fun verticalOffset(): Float =
        composeRule
            .onNode(hasVerticalScrollRange)
            .fetchSemanticsNode()
            .config[SemanticsProperties.VerticalScrollAxisRange]
            .value()

    private fun scrollListUp() {
        composeRule.onNode(hasVerticalScrollRange).performTouchInput { swipeUp() }
        composeRule.waitForIdle()
    }

    private fun applyState(next: ImportDeckState) {
        composeRule.runOnIdle { screenState = next }
        // 상태 변경 알림을 비운 뒤 프레임을 움직여야 다시 그려지고 포커스 요청이 실행된다.
        composeRule.waitForIdle()
        composeRule.mainClock.advanceTimeBy(SETTLE_MILLIS)
    }

    @Test
    fun `항목 연결 단계는 안내 제목을 제목으로 읽히게 그린다`() {
        show(mappingState())

        composeRule.onNode(hasText("앞뒷면에 넣을 항목을 골라 주세요") and isHeading).assertIsDisplayed()
    }

    @Test
    fun `항목 연결 단계는 상태의 연결을 칩에 그린다`() {
        show(mappingState())

        composeRule.onNode(chosenChip("뜻", "뒷면", 1)).assertIsDisplayed()
    }

    @Test
    fun `항목 연결 단계에서 가져오기 줄을 누르면 그 유형 id로 가져오기 변경 intent를 보낸다`() {
        show(mappingState())

        composeRule.onNode(hasText("가져오기")).performClick()

        intents shouldBe listOf(ImportDeckIntent.ToggleNoteType(MappingStepTables.VOCAB_ID))
    }

    @Test
    fun `항목 연결 단계에서 칩을 누르면 유형과 면과 항목으로 항목 선택 intent를 보낸다`() {
        show(mappingState())

        composeRule.onNode(chosenChip("뜻", "뒷면", 1)).performClick()

        intents shouldBe listOf(ImportDeckIntent.ToggleField(MappingStepTables.VOCAB_ID, FieldMapping.BACK, "뜻"))
    }

    @Test
    fun `항목 연결 단계에서 미리보기를 누르면 앱 언어의 빈 항목 문구를 담아 다음 intent를 보낸다`() {
        show(mappingState())

        composeRule.onNodeWithText("미리보기").performClick()

        intents shouldBe listOf(ImportDeckIntent.ClickMappingNext(emptySideText = "빈 항목입니다"))
    }

    @Test
    fun `항목 연결 단계에서 고르기 시트의 줄을 누르면 항목 선택 intent를 보낸다`() {
        show(wideState())

        composeRule.onNode(isButtonRole and readsContaining("뒷면에 넣을 항목 고르기")).performClick()
        composeRule.onNode(chosenChip("뜻", "뒷면", 1)).performClick()

        intents shouldBe listOf(ImportDeckIntent.ToggleField(MappingStepTables.WIDE_ID, FieldMapping.BACK, "뜻"))
    }

    @Test
    fun `미리보기를 만드는 동안에는 항목 연결을 그려 둔 채 그 위에 미리보기를 만든다는 문구를 보여준다`() {
        show(mappingState(loading = ImportLoading.BuildingPreview), manualClock = true)

        composeRule.mainClock.advanceTimeBy(CARD_DELAY_MILLIS)

        composeRule.onNodeWithText("미리보기를 만들고 있어요").assertIsDisplayed()
        // 만드는 중에는 목록 semantics가 비워지므로 병합하지 않은 트리로 목록이 아래에 있는지 본다.
        composeRule.onNodeWithText("일본어 단어", useUnmergedTree = true).assertExists()
    }

    @Test
    fun `미리보기를 만들다 끝나도 고른 항목은 그대로 남아 있다`() {
        show(mappingState(loading = ImportLoading.BuildingPreview), manualClock = true)

        applyState(mappingState())

        composeRule.onNode(chosenChip("단어", "앞면", 1)).assertIsDisplayed()
        composeRule.onNode(chosenChip("읽기", "뒷면", 3)).assertIsDisplayed()
    }

    @Test
    fun `하위 덱 선택에서 읽기가 끝나 항목 연결로 넘어오면 항목 연결 제목에 포커스를 둔다`() {
        show(ImportDeckState(step = ImportStep.SubdeckSelect, loading = ImportLoading.Reparsing(fileName = "일본어.apkg")), manualClock = true)

        applyState(mappingState())

        composeRule.onNode(hasText("앞뒷면에 넣을 항목을 골라 주세요") and isHeading).assertIsFocused()
    }

    @Test
    fun `다른 단계로 갔다 같은 읽기 결과로 돌아오면 목록의 스크롤 위치를 보존한다`() {
        val table = manyTypesTable()
        show(mappingState(table = table))
        scrollListUp()
        verticalOffset() shouldBeGreaterThan 0f
        val scrolled = verticalOffset()

        composeRule.runOnIdle { screenState = mappingState(table = table).copy(step = ImportStep.Source) }
        composeRule.runOnIdle { screenState = mappingState(table = table) }
        composeRule.waitForIdle()

        verticalOffset() shouldBe scrolled
    }

    @Test
    fun `읽기 결과가 새로 바뀌면 목록의 스크롤 위치를 처음으로 돌린다`() {
        show(mappingState(table = manyTypesTable()))
        scrollListUp()
        verticalOffset() shouldBeGreaterThan 0f

        // 값이 같은 표는 같은 상태로 보아 다시 그리지 않으므로, 다시 읽은 결과는 값이 다른 표여야 한다.
        composeRule.runOnIdle { screenState = mappingState(table = manyTypesTable().copy(sourceName = "다른.apkg")) }
        composeRule.waitForIdle()

        verticalOffset() shouldBe 0f
    }

    @Test
    fun `고르기 시트를 연 채 상태를 저장했다 복원해도 화면이 다시 그려진다`() {
        screenState = wideState()
        val restorationTester = StateRestorationTester(composeRule)
        restorationTester.setContent {
            CaroTheme {
                ImportDeckScreen(state = screenState, onIntent = { intents += it })
            }
        }
        composeRule.onNode(isButtonRole and readsContaining("뒷면에 넣을 항목 고르기")).performClick()

        restorationTester.emulateSavedInstanceStateRestore()

        composeRule.onNodeWithContentDescription("뒤로 가기").assertExists()
    }

    private companion object {
        const val SETTLE_MILLIS = 48L
        const val CARD_DELAY_MILLIS = 250L
    }
}

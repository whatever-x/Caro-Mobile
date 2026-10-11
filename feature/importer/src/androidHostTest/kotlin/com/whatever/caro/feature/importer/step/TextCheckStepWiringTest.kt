package com.whatever.caro.feature.importer.step

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.core.importparser.model.ParsedTable
import com.whatever.caro.feature.importer.ImportDeckScreen
import com.whatever.caro.feature.importer.component.isHeading
import com.whatever.caro.feature.importer.mvi.ImportDeckIntent
import com.whatever.caro.feature.importer.mvi.ImportDeckState
import com.whatever.caro.feature.importer.mvi.ImportLoading
import com.whatever.caro.feature.importer.mvi.ImportSelection
import com.whatever.caro.feature.importer.mvi.ImportStep
import io.kotest.matchers.floats.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

// 텍스트 확인 단계는 Screen이 상태에서 구분자와 제목 행, 다시 나누는 중을 골라 넘겨야 하므로 Screen을 통해 intent와 보존 규칙까지 확인한다.
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "ko-rKR-w393dp-h852dp-xxhdpi")
class TextCheckStepWiringTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val intents = mutableListOf<ImportDeckIntent>()
    private var screenState by mutableStateOf(ImportDeckState())

    private val hasHorizontalScroll = SemanticsMatcher.keyIsDefined(SemanticsProperties.HorizontalScrollAxisRange)

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

    private fun textState(
        table: ParsedTable = TextCheckStepTables.text(fieldNames = TextCheckStepTables.HEADER_NAMES),
        selection: ImportSelection = ImportSelection(),
        pendingSelection: ImportSelection? = null,
        loading: ImportLoading? = null,
    ) = ImportDeckState(
        step = ImportStep.TextCheck,
        table = table,
        selection = selection,
        pendingSelection = pendingSelection,
        loading = loading,
    )

    private fun horizontalOffset(): Float =
        composeRule
            .onNode(hasHorizontalScroll)
            .fetchSemanticsNode()
            .config[SemanticsProperties.HorizontalScrollAxisRange]
            .value()

    private fun scrollTableLeft() {
        composeRule.onNode(hasHorizontalScroll).performTouchInput { swipeLeft() }
        composeRule.waitForIdle()
    }

    @Test
    fun `텍스트 확인 단계는 안내 제목을 제목으로 읽히게 그린다`() {
        show(textState())

        composeRule.onNode(hasText("텍스트가 어떻게 나뉘었는지 확인해 주세요") and isHeading).assertIsDisplayed()
    }

    @Test
    fun `텍스트 확인 단계에서 쉼표 칩을 누르면 쉼표로 구분자 선택 intent를 보낸다`() {
        show(textState())

        composeRule.onNodeWithText("쉼표").performClick()

        intents shouldBe listOf(ImportDeckIntent.SelectDelimiter(','))
    }

    @Test
    fun `텍스트 확인 단계에서 첫 줄은 제목을 누르면 반대 값으로 제목 행 변경 intent를 보낸다`() {
        show(textState(selection = ImportSelection(hasHeaderRow = false)))

        composeRule.onNode(hasText("첫 줄은 제목")).performClick()

        intents shouldBe listOf(ImportDeckIntent.ChangeHeaderRow(true))
    }

    @Test
    fun `텍스트 확인 단계에서 다음을 누르면 선택 다음 intent를 보낸다`() {
        show(textState())

        composeRule.onNodeWithText("다음").performClick()

        intents shouldBe listOf(ImportDeckIntent.ClickSelectionNext)
    }

    @Test
    fun `고른 구분자가 없으면 파서가 쓴 구분자를 칩에 그린다`() {
        show(textState(table = TextCheckStepTables.text(delimiter = ";")))

        composeRule.onNode(hasText("세미콜론")).assertIsSelected()
        composeRule.onNode(hasText("탭")).assertIsNotSelected()
    }

    @Test
    fun `고른 구분자는 파서가 쓴 구분자보다 먼저 칩에 그린다`() {
        show(textState(table = TextCheckStepTables.text(delimiter = "\t"), selection = ImportSelection(delimiter = ',')))

        composeRule.onNode(hasText("쉼표")).assertIsSelected()
        composeRule.onNode(hasText("탭")).assertIsNotSelected()
    }

    @Test
    fun `다시 나누는 동안에는 방금 고른 구분자를 칩에 그린다`() {
        show(
            textState(
                selection = ImportSelection(delimiter = '\t'),
                pendingSelection = ImportSelection(delimiter = ','),
                loading = ImportLoading.TextReparsing,
            ),
            manualClock = true,
        )

        composeRule.onNode(hasText("쉼표")).assertIsSelected()
        composeRule.onNode(hasText("탭")).assertIsNotSelected()
    }

    @Test
    fun `파서가 세로선을 썼으면 세로선 칩을 선택된 상태로 그린다`() {
        show(textState(table = TextCheckStepTables.text(delimiter = "|")))

        composeRule.onNode(hasText("세로선 |")).assertIsSelected()
    }

    @Test
    fun `제목 행을 고르지 않았으면 파서가 추정한 값을 보여준다`() {
        show(textState(table = TextCheckStepTables.text(hasHeaderRow = true), selection = ImportSelection(hasHeaderRow = null)))

        composeRule.onNode(hasText("첫 줄은 제목")).fetchSemanticsNode().config[SemanticsProperties.ToggleableState] shouldBe ToggleableState.On
    }

    @Test
    fun `고른 제목 행은 파서가 추정한 값보다 먼저 보여준다`() {
        show(textState(table = TextCheckStepTables.text(hasHeaderRow = true), selection = ImportSelection(hasHeaderRow = false)))

        composeRule.onNode(hasText("첫 줄은 제목")).fetchSemanticsNode().config[SemanticsProperties.ToggleableState] shouldBe ToggleableState.Off
    }

    @Test
    fun `다시 나누는 동안에는 화면 전체를 덮지 않아 안내 제목이 접근성 트리에 남는다`() {
        show(textState(loading = ImportLoading.TextReparsing), manualClock = true)

        composeRule.mainClock.advanceTimeBy(CARD_DELAY_MILLIS)

        composeRule.onNode(hasText("텍스트가 어떻게 나뉘었는지 확인해 주세요") and isHeading).assertIsDisplayed()
    }

    @Test
    fun `다시 나누는 동안에는 결과 표 자리에 다시 나누고 있어요를 보여준다`() {
        show(textState(loading = ImportLoading.TextReparsing), manualClock = true)

        composeRule.mainClock.advanceTimeBy(CARD_DELAY_MILLIS)

        composeRule.onNodeWithText("다시 나누고 있어요").assertIsDisplayed()
    }

    @Test
    fun `다시 나누는 동안에는 다음 버튼이 꺼진다`() {
        show(textState(loading = ImportLoading.TextReparsing), manualClock = true)

        composeRule.onNodeWithText("다음").assertIsNotEnabled()
    }

    @Test
    fun `다른 단계로 갔다 같은 결과로 돌아오면 표의 가로 스크롤 위치를 보존한다`() {
        val table = TextCheckStepTables.text(fieldNames = TextCheckStepTables.HEADER_NAMES)
        show(textState(table = table))
        scrollTableLeft()
        horizontalOffset() shouldBeGreaterThan 0f
        val scrolled = horizontalOffset()

        composeRule.runOnIdle { screenState = textState(table = table).copy(step = ImportStep.Source) }
        composeRule.runOnIdle { screenState = textState(table = table) }
        composeRule.waitForIdle()

        horizontalOffset() shouldBe scrolled
    }

    @Test
    fun `다시 나눠 결과가 바뀌면 표의 가로 스크롤 위치를 처음으로 돌린다`() {
        val table = TextCheckStepTables.text(fieldNames = TextCheckStepTables.HEADER_NAMES)
        show(textState(table = table))
        scrollTableLeft()
        horizontalOffset() shouldBeGreaterThan 0f

        // 값이 같은 표는 같은 상태로 보아 다시 그리지 않으므로, 다시 나눈 결과는 값이 다른 표여야 한다.
        val resplit = TextCheckStepTables.text(fieldNames = TextCheckStepTables.HEADER_NAMES, delimiter = ",")
        composeRule.runOnIdle { screenState = textState(table = resplit) }
        composeRule.waitForIdle()

        horizontalOffset() shouldBe 0f
    }

    private companion object {
        const val CARD_DELAY_MILLIS = 250L
    }
}

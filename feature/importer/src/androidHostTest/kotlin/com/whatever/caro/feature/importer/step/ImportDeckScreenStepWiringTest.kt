package com.whatever.caro.feature.importer.step

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.feature.importer.ImportDeckScreen
import com.whatever.caro.feature.importer.mvi.ImportDeckIntent
import com.whatever.caro.feature.importer.mvi.ImportDeckState
import com.whatever.caro.feature.importer.mvi.ImportSelection
import com.whatever.caro.feature.importer.mvi.ImportStep
import io.kotest.matchers.shouldBe
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentSetOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

// 단계 화면은 Screen이 단계에 맞게 골라 그려야 하므로 Screen을 통해 intent까지 이어지는지 확인한다.
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "ko-rKR-w393dp-h852dp-xxhdpi")
class ImportDeckScreenStepWiringTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val intents = mutableListOf<ImportDeckIntent>()

    private fun show(state: ImportDeckState) {
        composeRule.setContent {
            CaroTheme {
                ImportDeckScreen(state = state, onIntent = { intents += it })
            }
        }
        composeRule.waitForIdle()
    }

    @Test
    fun `원본 선택 단계에서 파일 카드를 누르면 파일 선택 intent를 보낸다`() {
        show(ImportDeckState(step = ImportStep.Source))

        composeRule.onNodeWithText("파일에서 가져오기").performClick()

        intents shouldBe listOf(ImportDeckIntent.ClickPickFile)
    }

    @Test
    fun `원본 선택 단계에서 붙여넣기 카드를 누르면 붙여넣기 intent를 보낸다`() {
        show(ImportDeckState(step = ImportStep.Source))

        composeRule.onNodeWithText("텍스트 붙여넣기").performClick()

        intents shouldBe listOf(ImportDeckIntent.ClickPaste)
    }

    @Test
    fun `붙여넣기 단계에서 글을 입력하면 글 갱신 intent를 보낸다`() {
        show(ImportDeckState(step = ImportStep.Paste, isPaste = true))

        composeRule.onNode(hasSetTextAction()).performTextInput("학교")

        intents shouldBe listOf(ImportDeckIntent.UpdatePasteText("학교"))
    }

    @Test
    fun `붙여넣기 단계에서 다음을 누르면 앱 언어 문구를 이름으로 담아 다음 intent를 보낸다`() {
        show(ImportDeckState(step = ImportStep.Paste, isPaste = true, pasteText = "학교\t학교"))

        composeRule.onNodeWithText("다음").performClick()

        (intents.single() as ImportDeckIntent.ClickPasteNext).sourceName shouldBe "붙여 넣은 카드"
    }

    @Test
    fun `하위 덱 선택 단계에서 하위 덱 줄을 누르면 그 id로 하위 덱 선택 변경 intent를 보낸다`() {
        show(subdeckState())

        composeRule.onNode(hasText("문법")).performClick()

        intents shouldBe listOf(ImportDeckIntent.ToggleSubdeck("2"))
    }

    @Test
    fun `하위 덱 선택 단계에서 전체 선택 줄을 누르면 전체 선택 변경 intent를 보낸다`() {
        show(subdeckState())

        composeRule.onNode(hasText("전체 선택")).performClick()

        intents shouldBe listOf(ImportDeckIntent.ToggleAllSubdecks)
    }

    @Test
    fun `하위 덱 선택 단계에서 다음을 누르면 선택 다음 intent를 보낸다`() {
        show(subdeckState())

        composeRule.onNodeWithText("다음").performClick()

        intents shouldBe listOf(ImportDeckIntent.ClickSelectionNext)
    }

    @Test
    fun `하위 덱 선택 단계는 상태의 선택을 줄에 그린다`() {
        show(subdeckState(selectedIds = persistentSetOf("1")))

        composeRule.onNode(hasText("전체 선택")).fetchSemanticsNode().config[SemanticsProperties.ToggleableState] shouldBe
            ToggleableState.Indeterminate
    }

    @Test
    fun `하위 덱 선택 단계는 앱 상한을 선택 요약에 보여준다`() {
        show(subdeckState())

        composeRule.onNodeWithText("/ 최대 10,000장").assertExists()
    }

    @Test
    fun `시트 선택 단계에서 시트 줄을 누르면 그 index로 시트 선택 intent를 보낸다`() {
        show(sheetState())

        composeRule.onNode(hasText("N4 단어")).performClick()

        intents shouldBe listOf(ImportDeckIntent.SelectSheet(1))
    }

    @Test
    fun `시트 선택 단계에서 첫 줄은 제목을 누르면 반대 값으로 제목 행 변경 intent를 보낸다`() {
        show(sheetState(selection = ImportSelection(hasHeaderRow = true)))

        composeRule.onNode(hasText("첫 줄은 제목")).performClick()

        intents shouldBe listOf(ImportDeckIntent.ChangeHeaderRow(false))
    }

    @Test
    fun `시트 선택 단계는 제목 행 선택이 아직 없으면 파서가 추정한 값을 보여준다`() {
        show(sheetState(selection = ImportSelection(hasHeaderRow = null), hasHeaderRowEstimate = true))

        composeRule.onNode(hasText("첫 줄은 제목")).fetchSemanticsNode().config[SemanticsProperties.ToggleableState] shouldBe
            ToggleableState.On
    }

    @Test
    fun `시트 선택 단계는 상태의 시트 선택을 줄에 그린다`() {
        show(sheetState(selection = ImportSelection(sheetIndex = 1, hasHeaderRow = true)))

        composeRule.onNode(hasText("N4 단어")).fetchSemanticsNode().config[SemanticsProperties.Selected] shouldBe true
    }

    @Test
    fun `시트 선택 단계에서 다음을 누르면 선택 다음 intent를 보낸다`() {
        show(sheetState())

        composeRule.onNodeWithText("다음").performClick()

        intents shouldBe listOf(ImportDeckIntent.ClickSelectionNext)
    }

    private fun subdeckState(selectedIds: PersistentSet<String> = persistentSetOf("1", "2")) =
        ImportDeckState(
            step = ImportStep.SubdeckSelect,
            table = SelectionStepTables.apkg(),
            selection = ImportSelection(subdeckIds = selectedIds),
        )

    private fun sheetState(
        selection: ImportSelection = ImportSelection(hasHeaderRow = true),
        hasHeaderRowEstimate: Boolean = true,
    ) = ImportDeckState(
        step = ImportStep.SheetSelect,
        table = SelectionStepTables.xlsx(hasHeaderRow = hasHeaderRowEstimate),
        selection = selection,
    )
}

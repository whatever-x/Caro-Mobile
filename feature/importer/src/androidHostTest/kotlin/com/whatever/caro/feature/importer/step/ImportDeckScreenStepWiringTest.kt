package com.whatever.caro.feature.importer.step

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.core.importparser.SpreadsheetDateStyle
import com.whatever.caro.feature.importer.ImportDeckScreen
import com.whatever.caro.feature.importer.mvi.ImportDeckIntent
import com.whatever.caro.feature.importer.mvi.ImportDeckState
import com.whatever.caro.feature.importer.mvi.ImportDialog
import com.whatever.caro.feature.importer.mvi.ImportStep
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

// 단계 본문은 Screen이 단계에 맞게 골라 그려야 하므로 Screen을 통해 intent까지 이어지는지 확인한다.
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
    fun `가져오기 방법 단계는 상단 제목이 가져오기다`() {
        show(ImportDeckState(step = ImportStep.Source))

        composeRule.onNodeWithText("가져오기").assertIsDisplayed()
    }

    @Test
    fun `가져오기 방법 단계에서 파일에서 가져오기를 누르면 파일 선택 intent를 보낸다`() {
        show(ImportDeckState(step = ImportStep.Source))

        composeRule.onNodeWithText("파일에서 가져오기").performClick()

        intents shouldBe listOf(ImportDeckIntent.ClickPickFile)
    }

    @Test
    fun `상단 뒤로 가기 버튼을 누르면 뒤로 가기 intent를 보낸다`() {
        show(ImportDeckState(step = ImportStep.Paste, isPaste = true))

        composeRule.onNodeWithContentDescription("뒤로 가기").performClick()

        intents shouldBe listOf(ImportDeckIntent.ClickBack)
    }

    @Test
    fun `붙여넣기 단계에서 글을 입력하면 글 갱신 intent를 보낸다`() {
        show(ImportDeckState(step = ImportStep.Paste, isPaste = true))

        composeRule.onNode(hasSetTextAction()).performTextInput("학교")

        intents shouldBe listOf(ImportDeckIntent.UpdatePasteText("학교"))
    }

    @Test
    fun `붙여넣기 단계에서 다음을 누르면 앱 언어의 덱 이름과 빈 면 문구와 날짜 표기를 담아 보낸다`() {
        show(ImportDeckState(step = ImportStep.Paste, isPaste = true, pasteText = "학교\t학교"))

        composeRule.onNodeWithText("다음").performClick()

        intents shouldBe
            listOf(
                ImportDeckIntent.ClickPasteNext(
                    sourceName = "붙여 넣은 카드",
                    dateStyle = SpreadsheetDateStyle.YEAR_MONTH_DAY,
                    emptySideText = "(비어 있음)",
                ),
            )
    }

    @Test
    fun `붙여넣기 단계는 상태가 넘쳤다고 하면 오류 문구를 보이고 다음을 끈다`() {
        show(ImportDeckState(step = ImportStep.Paste, isPaste = true, pasteText = "학교", isPasteOverflowed = true))

        composeRule.onNodeWithText("한 번에 1,000,000자까지 붙여 넣을 수 있어요. 나눠서 가져와 주세요.").assertIsDisplayed()
        composeRule.onNodeWithText("다음").assertIsNotEnabled()
    }

    @Test
    fun `미리보기 단계는 상단 제목이 미리보기다`() {
        show(ImportDeckState(step = ImportStep.Preview))

        composeRule.onNodeWithText("미리보기").assertIsDisplayed()
    }

    @Test
    fun `대화상자 상태가 있으면 대화상자를 그리고 버튼 intent를 보낸다`() {
        show(ImportDeckState(step = ImportStep.Preview, dialog = ImportDialog.DiscardConfirm))

        composeRule.onNodeWithText("돌아가기").performClick()

        intents shouldBe listOf(ImportDeckIntent.ConfirmDiscard)
    }
}

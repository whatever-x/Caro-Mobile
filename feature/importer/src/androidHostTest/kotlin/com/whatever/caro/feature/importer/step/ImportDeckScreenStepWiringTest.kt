package com.whatever.caro.feature.importer.step

import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.feature.importer.ImportDeckScreen
import com.whatever.caro.feature.importer.mvi.ImportDeckIntent
import com.whatever.caro.feature.importer.mvi.ImportDeckState
import com.whatever.caro.feature.importer.mvi.ImportStep
import io.kotest.matchers.shouldBe
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
}

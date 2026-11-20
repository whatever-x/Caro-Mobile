package com.whatever.caro.feature.importer.step

import androidx.activity.ComponentDialog
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.unit.dp
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.feature.importer.mvi.DeckInfoDraft
import com.whatever.caro.feature.importer.mvi.ImportDeckIntent
import com.whatever.caro.feature.importer.mvi.ImportDeckState
import com.whatever.caro.feature.importer.support.PreviewSamples
import io.kotest.matchers.floats.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowDialog

/** 덱 정보 시트를 미리보기 위에 열고, 입력은 ViewModel과 같은 함수로 상태에 반영해 시트가 상태를 따르는지 본다. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "ko-rKR-w393dp-h852dp-xxhdpi")
class DeckInfoSheetTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val host = PreviewScreenHost(composeRule)

    private fun preview(description: String = DESCRIPTION): ImportDeckState =
        PreviewSamples.state(PreviewSamples.ankiTable, PreviewSamples.ankiPreview).copy(deckName = NAME, deckDescription = description)

    private fun openSheet(description: String = DESCRIPTION) {
        host.show(preview(description))
        composeRule.onNodeWithContentDescription("덱 이름 변경").performClick()
        composeRule.waitForIdle()
    }

    private fun field(value: String) = composeRule.onNode(hasSetTextAction() and hasText(value))

    @Test
    fun `덱 이름 줄의 변경을 누르면 시트가 열리고 제목과 적용된 이름과 설명을 보인다`() {
        openSheet()

        composeRule.onNodeWithText("덱 정보").assertIsDisplayed()
        field(NAME).assertIsDisplayed()
        field(DESCRIPTION).assertIsDisplayed()
        host.intents shouldBe listOf(ImportDeckIntent.ClickChangeDeckInfo)
    }

    @Test
    fun `두 입력칸 머리글은 덱 이름과 설명이고 필수 표시와 상한이 붙은 글자 수를 보인다`() {
        openSheet()

        composeRule.onAllNodesWithText("*").fetchSemanticsNodes().size shouldBe 2
        listOf("설명", "10/50", "23/500").forEach { text -> composeRule.onNodeWithText(text).assertIsDisplayed() }
    }

    @Test
    fun `이름과 설명을 바꾸면 각각의 입력을 보내고 바뀐 값이 입력칸에 남는다`() {
        openSheet()

        field(NAME).performTextReplacement("TOEIC 3000")
        field(DESCRIPTION).performTextReplacement("토익 단어")

        host.intents.drop(1) shouldBe
            listOf(ImportDeckIntent.UpdateDeckInfoName("TOEIC 3000"), ImportDeckIntent.UpdateDeckInfoDescription("토익 단어"))
        field("TOEIC 3000").assertIsDisplayed()
        composeRule.onNodeWithText("10/50").assertIsDisplayed()
    }

    @Test
    fun `이름이나 설명이 공백뿐이면 적용이 꺼지고 둘 다 있으면 켜진다`() {
        openSheet()
        composeRule.onNodeWithText("적용").assertIsEnabled()

        field(NAME).performTextReplacement("   ")
        composeRule.onNodeWithText("적용").assertIsNotEnabled()
    }

    @Test
    fun `설명이 비어 있으면 적용이 꺼진다`() {
        openSheet(description = "")

        composeRule.onNodeWithText("적용").assertIsNotEnabled()
    }

    @Test
    fun `적용을 누르면 적용 입력을 한 번 보내고 시트가 닫혀 미리보기의 덱 이름이 바뀐다`() {
        openSheet()
        field(NAME).performTextReplacement("TOEIC 3000")

        composeRule.onNodeWithText("적용").performClick()
        composeRule.waitForIdle()

        host.intents.count { intent -> intent == ImportDeckIntent.ApplyDeckInfo } shouldBe 1
        composeRule.onNodeWithText("덱 정보").assertDoesNotExist()
        composeRule.onNodeWithText("TOEIC 3000").assertIsDisplayed()
    }

    @Test
    fun `시트 창에서 뒤로 가기를 누르면 바꾸지 않고 닫는 입력을 보내고 덱 이름은 그대로다`() {
        openSheet()
        field(NAME).performTextReplacement("TOEIC 3000")

        composeRule.runOnIdle { (ShadowDialog.getLatestDialog() as ComponentDialog).onBackPressedDispatcher.onBackPressed() }
        composeRule.waitForIdle()

        host.intents.last() shouldBe ImportDeckIntent.DismissDeckInfo
        composeRule.onNodeWithText("덱 정보").assertDoesNotExist()
        composeRule.onNodeWithText(NAME).assertIsDisplayed()
    }

    @Test
    fun `시트가 열리면 제목에 포커스를 두고 입력칸에는 두지 않는다`() {
        openSheet()

        composeRule.onNode(hasText("덱 정보") and SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading)).assertIsFocused()
        field(NAME).assertIsNotFocused()
        field(DESCRIPTION).assertIsNotFocused()
    }

    @Test
    fun `키보드로 높이가 줄면 입력칸만 스크롤되고 적용은 보인다`() {
        val draft = DeckInfoDraft(NAME, DESCRIPTION)
        composeRule.setContent {
            CaroTheme {
                Box(modifier = Modifier.size(width = 393.dp, height = SHORT_HEIGHT)) {
                    DeckInfoSheetContent(draft = draft, onNameChange = {}, onDescriptionChange = {}, onApply = {})
                }
            }
        }

        composeRule.verticalScrollMax() shouldBeGreaterThan 0f
        composeRule.onNodeWithText("적용").assertIsDisplayed()
        field(DESCRIPTION).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("적용").assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.VerticalScrollAxisRange))
    }

    private companion object {
        const val NAME = "JLPT N5 단어"
        const val DESCRIPTION = "JLPT N5 단어.apkg에서 가져왔어요"
        val SHORT_HEIGHT = 240.dp
    }
}

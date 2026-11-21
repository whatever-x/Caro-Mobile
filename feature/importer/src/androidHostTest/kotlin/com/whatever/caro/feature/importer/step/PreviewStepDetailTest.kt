package com.whatever.caro.feature.importer.step

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import com.whatever.caro.feature.importer.mvi.ImportDeckIntent
import com.whatever.caro.feature.importer.support.PreviewSamples
import com.whatever.caro.feature.importer.transform.ImportCard
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** 카드를 누르면 카드 전체 보기가 열리고 닫으면 연 카드로 돌아오는지 화면에서 확인한다. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "ko-rKR-w393dp-h852dp-xxhdpi")
class PreviewStepDetailTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val host = PreviewScreenHost(composeRule)
    private val japan = PreviewSamples.ankiCards[2]
    private val heading = SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading)

    @Test
    fun `카드를 누르면 앞면과 뒷면 제목 아래 줄임 없는 전체 글을 보이는 전체 보기가 열린다`() {
        host.show(PreviewSamples.state(PreviewSamples.ankiTable, PreviewSamples.ankiPreview))

        composeRule.onNode(cardItem(japan)).performClick()

        composeRule.onNode(isDialog()).assertIsDisplayed()
        composeRule.onNode(inDialog(hasText("앞면") and heading)).assertIsDisplayed()
        composeRule.onNode(inDialog(hasText("뒷면") and heading)).assertIsDisplayed()
        composeRule.onNode(inDialog(hasText(japan.front)), useUnmergedTree = true).assertExists()
        composeRule.onNode(inDialog(hasText(japan.back)), useUnmergedTree = true).assertExists()
        host.intents shouldBe listOf(ImportDeckIntent.ClickCard(japan))
    }

    @Test
    fun `닫기를 누르면 전체 보기를 닫고 연 카드로 포커스를 돌려준다`() {
        host.show(PreviewSamples.state(PreviewSamples.ankiTable, PreviewSamples.ankiPreview))

        composeRule.onNode(cardItem(japan)).performClick()
        composeRule.onNode(inDialog(hasText("닫기"))).performClick()
        composeRule.waitForIdle()

        composeRule.onNode(isDialog()).assertDoesNotExist()
        composeRule.onNode(cardItem(japan)).assertIsFocused()
        composeRule.onNode(cardItem(PreviewSamples.ankiCards[0])).assertIsNotFocused()
        host.intents shouldBe listOf(ImportDeckIntent.ClickCard(japan), ImportDeckIntent.CloseCardDetail)
    }

    @Test
    fun `카드 전체 보기를 닫아 포커스를 돌려받은 카드에서 Enter를 누르면 다시 열린다`() {
        host.show(PreviewSamples.state(PreviewSamples.ankiTable, PreviewSamples.ankiPreview))
        openAndCloseDetail(japan)

        composeRule.onNode(cardItem(japan)).performKeyInput { pressKey(Key.Enter) }

        composeRule.onNode(isDialog()).assertIsDisplayed()
        host.intents shouldBe
            listOf(ImportDeckIntent.ClickCard(japan), ImportDeckIntent.CloseCardDetail, ImportDeckIntent.ClickCard(japan))
    }

    @Test
    fun `포커스를 돌려받은 카드에서 Tab을 한 번 누르면 다음 카드로 넘어간다`() {
        host.show(PreviewSamples.state(PreviewSamples.ankiTable, PreviewSamples.ankiPreview))
        openAndCloseDetail(japan)
        host.useKeyboard()

        composeRule.onNode(cardItem(japan)).performKeyInput { pressKey(Key.Tab) }

        composeRule.onNode(cardItem(japan)).assertIsNotFocused()
        composeRule.onNode(cardItem(PreviewSamples.ankiCards[3])).assertIsFocused()
    }

    @Test
    fun `빈 면 문구로 채운 면도 전체 보기에 그대로 보인다`() {
        val read = PreviewSamples.ankiCards[1]
        host.show(PreviewSamples.state(PreviewSamples.ankiTable, PreviewSamples.ankiPreview, detailCard = read))

        composeRule.onNode(inDialog(hasText(PreviewSamples.EMPTY_SIDE)), useUnmergedTree = true).assertExists()
        composeRule.onNodeWithText("앞면").assertIsDisplayed()
    }

    private fun openAndCloseDetail(card: ImportCard) {
        composeRule.onNode(cardItem(card)).performClick()
        composeRule.onNode(inDialog(hasText("닫기"))).performClick()
        composeRule.waitForIdle()
        composeRule.onNode(cardItem(card)).assertIsFocused()
    }
}

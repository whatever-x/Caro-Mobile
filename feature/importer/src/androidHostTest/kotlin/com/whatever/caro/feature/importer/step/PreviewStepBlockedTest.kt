package com.whatever.caro.feature.importer.step

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import com.whatever.caro.core.importparser.model.ImportFormat
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

// 다음 버튼과 그 위 고정 사유 줄, 가져올 카드가 0장인 상태, 상한을 넘은 상태. 문구는 Figma 프레임의 표시 문구다.
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "ko-rKR-w393dp-h852dp-xxhdpi")
class PreviewStepBlockedTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val host by lazy { PreviewStepHost(composeRule) }

    private fun nextButton(): SemanticsNodeInteraction = composeRule.onNodeWithText("다음")

    private fun top(node: SemanticsNodeInteraction): Float = node.bounds().top.value

    private fun showEmpty() {
        host.show(preview = PreviewStepTables.preview(cards = emptyList(), excluded = 1_514))
    }

    private fun showLimitExceeded(
        format: ImportFormat,
        isPaste: Boolean = false,
    ) {
        host.show(preview = PreviewStepTables.preview(limitExceeded = true), format = format, isPaste = isPaste)
    }

    @Test
    fun `가져올 카드가 있으면 다음 버튼이 켜져 있다`() {
        host.show()

        nextButton().assertIsEnabled()
    }

    @Test
    fun `다음 버튼을 누르면 다음을 한 번 요청한다`() {
        host.show()

        nextButton().performClick()

        host.nextClicks shouldBe 1
    }

    @Test
    fun `막히지 않았으면 사유 줄도 상태 설명도 없다`() {
        host.show()

        composeRule.onAllNodes(isPoliteRegion and hasText(".", substring = true)).assertCountEquals(0)
        nextButton().assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.StateDescription))
    }

    @Test
    fun `가져올 카드가 0장이면 목록 자리에 빈 안내를 보여준다`() {
        showEmpty()

        composeRule.onNodeWithText("가져올 카드가 없어요.").assertIsDisplayed()
    }

    @Test
    fun `가져올 카드가 있으면 빈 안내가 없다`() {
        host.show()

        composeRule.onNodeWithText("가져올 카드가 없어요.").assertDoesNotExist()
    }

    @Test
    fun `가져올 카드가 0장이어도 요약과 카드 목록 제목은 그대로 보인다`() {
        showEmpty()

        composeRule.onNodeWithText("가져올 카드").assertIsDisplayed()
        composeRule.onNodeWithText("카드 목록").assertIsDisplayed()
    }

    @Test
    fun `가져올 카드가 0장이면 다음 버튼이 꺼진다`() {
        showEmpty()

        nextButton().assertIsNotEnabled()
    }

    @Test
    fun `가져올 카드가 0장이면 항목 연결에서 고르라는 사유를 다음 버튼 위에 보여준다`() {
        showEmpty()

        composeRule.onNode(hasText(EMPTY_REASON) and isPoliteRegion).assertIsDisplayed()
        (top(composeRule.onNodeWithText(EMPTY_REASON)) < top(nextButton())) shouldBe true
    }

    @Test
    fun `가져올 카드가 0장이면 꺼진 다음 버튼은 사유를 상태 설명으로 함께 읽는다`() {
        showEmpty()

        nextButton().fetchSemanticsNode().config[SemanticsProperties.StateDescription] shouldBe EMPTY_REASON
    }

    @Test
    fun `꺼진 다음 버튼을 눌러도 다음을 요청하지 않는다`() {
        showEmpty()

        nextButton().performClick()

        host.nextClicks shouldBe 0
    }

    @Test
    fun `엑셀이 상한을 넘으면 시트나 행을 나누라는 사유를 보여준다`() {
        showLimitExceeded(ImportFormat.XLSX)

        composeRule.onNode(hasText(XLSX_REASON) and isPoliteRegion).assertIsDisplayed()
    }

    @Test
    fun `텍스트 파일이 상한을 넘으면 파일을 줄 수로 나누라는 사유를 보여준다`() {
        showLimitExceeded(ImportFormat.TEXT)

        composeRule.onNode(hasText(TEXT_REASON) and isPoliteRegion).assertIsDisplayed()
    }

    @Test
    fun `붙여넣기가 상한을 넘으면 줄 수로 나눠 붙여 넣으라는 사유를 보여준다`() {
        showLimitExceeded(ImportFormat.TEXT, isPaste = true)

        composeRule.onNode(hasText(PASTE_REASON) and isPoliteRegion).assertIsDisplayed()
    }

    @Test
    fun `Anki가 상한을 넘으면 하위 덱을 줄이라는 사유를 보여준다`() {
        showLimitExceeded(ImportFormat.APKG)

        composeRule.onNode(hasText(APKG_REASON) and isPoliteRegion).assertIsDisplayed()
    }

    @Test
    fun `상한을 넘으면 다음 버튼이 꺼지고 사유를 상태 설명으로 함께 읽는다`() {
        showLimitExceeded(ImportFormat.XLSX)

        nextButton().assertIsNotEnabled()
        nextButton().fetchSemanticsNode().config[SemanticsProperties.StateDescription] shouldBe XLSX_REASON
    }

    @Test
    fun `상한을 넘어도 카드 목록은 그대로 보인다`() {
        showLimitExceeded(ImportFormat.XLSX)

        composeRule.onNode(cardItem(PreviewStepTables.SCHOOL)).assertIsDisplayed()
    }

    @Test
    fun `상한을 넘고 카드도 0장이면 상한 사유가 먼저다`() {
        host.show(preview = PreviewStepTables.preview(cards = emptyList(), limitExceeded = true), format = ImportFormat.XLSX)

        composeRule.onNodeWithText(XLSX_REASON).assertIsDisplayed()
        composeRule.onNodeWithText(EMPTY_REASON).assertDoesNotExist()
    }

    @Test
    fun `사유 줄은 목록과 함께 스크롤되지 않고 고정되어 있다`() {
        host.show(preview = PreviewStepTables.longPreview().copy(limitExceeded = true), format = ImportFormat.XLSX)
        val before = composeRule.onNodeWithText(XLSX_REASON).bounds()

        composeRule.onNode(hasVerticalScrollRange).performTouchInput { swipeUp() }
        composeRule.waitForIdle()

        composeRule.onNodeWithText(XLSX_REASON).bounds() shouldBe before
    }

    @Test
    fun `세로 스크롤 컨테이너는 막힌 상태에서도 하나다`() {
        showEmpty()

        composeRule.onAllNodes(hasVerticalScrollRange).assertCountEquals(1)
    }

    private companion object {
        const val EMPTY_REASON = "가져올 카드가 없어요. 항목 연결에서 내용이 있는 항목을 골라 주세요."
        const val XLSX_REASON = "한 번에 10,000장까지 가져올 수 있어요. 시트나 행을 나눠 여러 파일로 저장해 주세요."
        const val TEXT_REASON = "한 번에 10,000장까지 가져올 수 있어요. 파일을 10,000줄 이하로 나눠 다시 골라 주세요."
        const val PASTE_REASON = "한 번에 10,000장까지 가져올 수 있어요. 10,000줄 이하로 나눠 붙여 넣어 주세요."
        const val APKG_REASON = "고른 하위 덱을 줄이거나 Anki에서 하위 덱별로 나눠 내보내 주세요."
    }
}

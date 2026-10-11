package com.whatever.caro.core.ui.card

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import com.whatever.caro.core.designsystem.themes.CaroSpacing
import com.whatever.caro.core.designsystem.themes.CaroTheme
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

// 전체 보기 다이얼로그를 구역 컴포넌트로 분리해도 CaroFlashCard의 모양과 동작이 그대로인지 지킨다.
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "ko-rKR-w393dp-h852dp-xxhdpi")
class CaroFlashCardFullTextDialogTest {
    @get:Rule
    val composeRule = createComposeRule()

    private var isFlipped by mutableStateOf(false)

    private fun showCard(
        frontText: String,
        backText: String,
        flipped: Boolean = false,
    ) {
        isFlipped = flipped
        composeRule.setContent {
            CaroTheme {
                CaroFlashCard(
                    frontText = frontText,
                    backText = backText,
                    isFlipped = isFlipped,
                    flipHint = FLIP_HINT,
                    moreLabel = MORE_LABEL,
                    onFlip = {},
                    modifier = Modifier.size(CARD_WIDTH_DP.dp, CARD_HEIGHT_DP.dp),
                )
            }
        }
        composeRule.waitForIdle()
    }

    private fun openDialogFromLongFront() {
        showCard(frontText = LONG_TEXT, backText = SHORT_TEXT)
        composeRule.onNodeWithText(MORE_LABEL).performClick()
        composeRule.waitForIdle()
    }

    private fun dialogText(text: String): SemanticsNodeInteraction = composeRule.onNode(hasText(text) and hasAnyAncestor(isDialog()))

    private fun dialogRoot(): SemanticsNodeInteraction = composeRule.onNode(isDialog())

    // 스크롤 영역은 본문 노드가 직접 갖거나 본문을 감싼 컨테이너가 갖는다. 구조와 무관하게 다이얼로그 안의 유일한 스크롤 노드를 쓴다.
    private fun dialogScroller(): SemanticsNodeInteraction = composeRule.onNode(hasScrollAction() and hasAnyAncestor(isDialog()))

    @Test
    fun `앞면 글이 열 줄을 넘으면 더보기를 보여준다`() {
        showCard(frontText = LONG_TEXT, backText = SHORT_TEXT)

        composeRule.onNodeWithText(MORE_LABEL).assertIsDisplayed()
    }

    @Test
    fun `앞면 글이 짧으면 더보기를 보여주지 않는다`() {
        showCard(frontText = SHORT_TEXT, backText = SHORT_TEXT)

        composeRule.onNodeWithText(MORE_LABEL).assertDoesNotExist()
    }

    @Test
    fun `더보기를 누르기 전에는 다이얼로그가 없다`() {
        showCard(frontText = LONG_TEXT, backText = SHORT_TEXT)

        dialogRoot().assertDoesNotExist()
    }

    @Test
    fun `더보기를 누르면 앞면 전체 글을 다이얼로그에 보여준다`() {
        openDialogFromLongFront()

        dialogText(LONG_TEXT).assertIsDisplayed()
    }

    @Test
    fun `뒤집힌 카드에서 더보기를 누르면 뒷면 전체 글을 다이얼로그에 보여준다`() {
        showCard(frontText = SHORT_TEXT, backText = LONG_TEXT, flipped = true)

        composeRule.onNodeWithText(MORE_LABEL).performClick()
        composeRule.waitForIdle()

        dialogText(LONG_TEXT).assertIsDisplayed()
    }

    @Test
    fun `다이얼로그 밖을 누르면 닫힌다`() {
        openDialogFromLongFront()

        dialogRoot().performTouchInput { click(Offset(OUTSIDE_TAP_PX, OUTSIDE_TAP_PX)) }
        composeRule.waitForIdle()

        dialogRoot().assertDoesNotExist()
    }

    @Test
    fun `다이얼로그 안의 글을 눌러도 닫히지 않는다`() {
        openDialogFromLongFront()

        dialogText(LONG_TEXT).performClick()
        composeRule.waitForIdle()

        dialogText(LONG_TEXT).assertIsDisplayed()
    }

    @Test
    fun `카드를 뒤집으면 열려 있던 다이얼로그가 닫힌다`() {
        openDialogFromLongFront()

        composeRule.runOnIdle { isFlipped = true }
        composeRule.waitForIdle()

        dialogRoot().assertDoesNotExist()
    }

    @Test
    fun `다이얼로그의 긴 글은 스크롤할 수 있다`() {
        openDialogFromLongFront()

        dialogScroller().assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "ko-rKR-w600dp-h852dp-xxhdpi")
    fun `창이 충분히 크면 다이얼로그 카드 폭은 362dp다`() {
        openDialogFromLongFront()

        cardWidthDp() shouldBe (DIALOG_MAX_WIDTH_DP plusOrMinus SIZE_TOLERANCE_DP)
    }

    @Test
    fun `창이 충분히 크면 다이얼로그 카드 높이는 642dp다`() {
        openDialogFromLongFront()

        cardHeightDp() shouldBe (DIALOG_MAX_HEIGHT_DP plusOrMinus SIZE_TOLERANCE_DP)
    }

    @Test
    @Config(qualifiers = "ko-rKR-w300dp-h852dp-xxhdpi")
    fun `창 폭이 좁으면 다이얼로그 카드는 좌우 바깥 여백을 남기고 폭을 채운다`() {
        openDialogFromLongFront()

        val expected = NARROW_WINDOW_DP - 2 * spacing.l.value
        cardWidthDp() shouldBe (expected plusOrMinus SIZE_TOLERANCE_DP)
    }

    @Test
    @Config(qualifiers = "ko-rKR-w393dp-h500dp-xxhdpi")
    fun `창 높이가 낮으면 다이얼로그 카드는 상하 바깥 여백을 남기고 높이를 채운다`() {
        openDialogFromLongFront()

        val expected = SHORT_WINDOW_DP - 2 * spacing.l.value
        cardHeightDp() shouldBe (expected plusOrMinus SIZE_TOLERANCE_DP)
    }

    // 스크롤 노드는 카드 안쪽 여백을 뺀 영역이라 여백을 더해 카드 크기로 환산한다.
    private fun cardWidthDp(): Float {
        val body = dialogScroller().getUnclippedBoundsInRoot()
        return (body.right - body.left).value + 2 * spacing.s.value
    }

    private fun cardHeightDp(): Float {
        val body = dialogScroller().getUnclippedBoundsInRoot()
        return (body.bottom - body.top).value + 2 * spacing.xl4.value
    }

    private companion object {
        const val FLIP_HINT = "탭해서 뒤집기"
        const val MORE_LABEL = "More"
        const val SHORT_TEXT = "Apple"
        const val LONG_TEXT_REPEAT = 300
        val LONG_TEXT = "가나다라마바사 ".repeat(LONG_TEXT_REPEAT).trim()
        const val CARD_WIDTH_DP = 320
        const val CARD_HEIGHT_DP = 480
        const val OUTSIDE_TAP_PX = 4f
        const val DIALOG_MAX_WIDTH_DP = 362f
        const val DIALOG_MAX_HEIGHT_DP = 642f
        const val NARROW_WINDOW_DP = 300f
        const val SHORT_WINDOW_DP = 500f
        const val SIZE_TOLERANCE_DP = 0.5f
        val spacing = CaroSpacing.defaultSpacing()
    }
}

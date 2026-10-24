package com.whatever.caro.core.designsystem.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import com.whatever.caro.core.designsystem.foundation.CaroTipDotSize
import com.whatever.caro.core.designsystem.themes.CaroTheme
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w393dp-h852dp-xxhdpi")
class CaroTipSectionTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `TIP 라벨을 보여준다`() {
        composeRule.setContent { CaroTheme { CaroTipSection(lines = LINES) } }

        composeRule.onNodeWithText("TIP").assertIsDisplayed()
    }

    @Test
    fun `전달한 줄을 모두 보여준다`() {
        composeRule.setContent { CaroTheme { CaroTipSection(lines = LINES) } }

        LINES.forEach { line -> composeRule.onNodeWithText(line).assertIsDisplayed() }
    }

    @Test
    fun `줄은 전달한 순서대로 위에서 아래로 놓인다`() {
        composeRule.setContent { CaroTheme { CaroTipSection(lines = LINES) } }

        val tops =
            LINES.map { line ->
                composeRule
                    .onNodeWithText(line)
                    .getUnclippedBoundsInRoot()
                    .top.value
            }

        tops.zipWithNext().forEach { (upper, lower) -> upper shouldBeLessThan lower }
    }

    @Test
    fun `TIP 라벨은 첫 줄보다 위에 놓인다`() {
        composeRule.setContent { CaroTheme { CaroTipSection(lines = LINES) } }

        val labelTop =
            composeRule
                .onNodeWithText("TIP")
                .getUnclippedBoundsInRoot()
                .top.value
        val firstLineTop =
            composeRule
                .onNodeWithText(LINES.first())
                .getUnclippedBoundsInRoot()
                .top.value

        labelTop shouldBeLessThan firstLineTop
    }

    @Test
    fun `전달하지 않은 덱 안내 문구는 덧붙이지 않는다`() {
        composeRule.setContent { CaroTheme { CaroTipSection(lines = LINES) } }

        composeRule.onNodeWithText(DECK_TIP_LINE).assertDoesNotExist()
    }

    @Test
    fun `한 줄이면 점은 글자 줄의 가운데에 놓인다`() {
        val alignment = showSingleTipAndMeasureDot(SHORT_LINE)

        alignment.lineCount shouldBe 1
        alignment.dotCenter shouldBe (alignment.firstLineCenter plusOrMinus CENTER_TOLERANCE_PX)
    }

    @Test
    fun `줄이 여러 번 감겨도 점은 첫 줄의 가운데에 놓인다`() {
        val alignment = showSingleTipAndMeasureDot(LONG_LINE)

        alignment.lineCount shouldBeGreaterThan 1
        alignment.dotCenter shouldBe (alignment.firstLineCenter plusOrMinus CENTER_TOLERANCE_PX)
    }

    private class DotAlignment(
        val lineCount: Int,
        val firstLineCenter: Float,
        val dotCenter: Float,
    )

    private fun showSingleTipAndMeasureDot(line: String): DotAlignment {
        var dotColor = Color.Unspecified
        composeRule.setContent {
            CaroTheme {
                dotColor = CaroTheme.color.text.tertiary
                Box(modifier = Modifier.width(NARROW_WIDTH_DP.dp)) {
                    CaroTipSection(lines = listOf(line), modifier = Modifier.testTag(TAG))
                }
            }
        }
        composeRule.waitForIdle()

        val text = composeRule.onNodeWithText(line)
        val layout = text.textLayoutResult()
        val textTop = text.fetchSemanticsNode().boundsInRoot.top
        val dotRows =
            composeRule
                .onNodeWithTag(TAG)
                .renderPixels()
                .rowsWithColor(dotColor, leftWidthPx = with(composeRule.density) { CaroTipDotSize.roundToPx() })
        // 픽셀 행은 윗변이 포함되고 아랫변이 제외되므로 가운데는 마지막 행에 1을 더해 구한다.
        return DotAlignment(
            lineCount = layout.lineCount,
            firstLineCenter = textTop + (layout.getLineTop(0) + layout.getLineBottom(0)) / 2f,
            dotCenter = (dotRows.first() + dotRows.last() + 1) / 2f,
        )
    }

    private fun SemanticsNodeInteraction.textLayoutResult(): TextLayoutResult {
        val results = mutableListOf<TextLayoutResult>()
        fetchSemanticsNode().config[SemanticsActions.GetTextLayoutResult].action?.invoke(results)
        return results.single()
    }

    private companion object {
        const val TAG = "tip-section"
        const val NARROW_WIDTH_DP = 140
        const val CENTER_TOLERANCE_PX = 1.5f
        const val SHORT_LINE = "짧은 안내"
        const val LONG_LINE = "한 번에 추가할 수 있는 카드는 최대 이백 장이고 이 안내는 좁은 폭에서 여러 줄로 감겨야 한다"
        val LINES = listOf("첫째 안내", "둘째 안내", "셋째 안내")
        const val DECK_TIP_LINE = "You can add up to 200 cards at a time."
    }
}

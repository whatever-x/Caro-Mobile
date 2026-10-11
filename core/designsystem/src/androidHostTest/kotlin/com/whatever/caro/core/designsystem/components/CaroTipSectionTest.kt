package com.whatever.caro.core.designsystem.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.whatever.caro.core.designsystem.themes.CaroTheme
import io.kotest.matchers.comparables.shouldBeLessThan
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

    private companion object {
        val LINES = listOf("첫째 안내", "둘째 안내", "셋째 안내")
        const val DECK_TIP_LINE = "You can add up to 200 cards at a time."
    }
}

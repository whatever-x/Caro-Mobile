package com.whatever.caro.core.designsystem.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import com.whatever.caro.core.designsystem.themes.CaroTheme
import io.kotest.matchers.comparables.shouldBeLessThanOrEqualTo
import io.kotest.matchers.floats.plusOrMinus
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
class CaroFieldHeaderTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `필수 머리글은 전달한 라벨을 보여준다`() {
        composeRule.setContent { CaroTheme { CaroRequiredFieldHeader(label = LABEL) } }

        composeRule.onNodeWithText(LABEL).assertIsDisplayed()
    }

    @Test
    fun `필수 머리글은 필수 표시를 보여준다`() {
        composeRule.setContent { CaroTheme { CaroRequiredFieldHeader(label = LABEL) } }

        composeRule.onNodeWithText(REQUIRED_MARK).assertIsDisplayed()
    }

    @Test
    fun `필수 표시는 라벨의 오른쪽에 놓인다`() {
        composeRule.setContent { CaroTheme { CaroRequiredFieldHeader(label = LABEL) } }

        val labelRight = composeRule.onNodeWithText(LABEL).getUnclippedBoundsInRoot().right
        val markLeft = composeRule.onNodeWithText(REQUIRED_MARK).getUnclippedBoundsInRoot().left

        labelRight.value shouldBeLessThanOrEqualTo markLeft.value
    }

    @Test
    fun `글자 수 표시는 전달한 문구를 보여준다`() {
        composeRule.setContent { CaroTheme { CaroFieldCounter(count = COUNT) } }

        composeRule.onNodeWithText(COUNT).assertIsDisplayed()
    }

    @Test
    fun `글자 수 표시는 줄의 오른쪽 끝에 맞춘다`() {
        composeRule.setContent {
            CaroTheme {
                Box(modifier = Modifier.width(ROW_WIDTH_DP.dp)) {
                    CaroFieldCounter(count = COUNT)
                }
            }
        }

        val counterRight = composeRule.onNodeWithText(COUNT).getUnclippedBoundsInRoot().right

        counterRight.value shouldBe (ROW_WIDTH_DP.toFloat() plusOrMinus RIGHT_TOLERANCE_DP)
    }

    private companion object {
        const val LABEL = "덱 이름"
        const val REQUIRED_MARK = "*"
        const val COUNT = "3/50"
        const val ROW_WIDTH_DP = 300
        const val RIGHT_TOLERANCE_DP = 0.5f
    }
}

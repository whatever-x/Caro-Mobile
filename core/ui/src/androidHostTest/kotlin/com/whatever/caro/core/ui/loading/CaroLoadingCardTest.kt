package com.whatever.caro.core.ui.loading

import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.whatever.caro.core.designsystem.themes.CaroTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "ko-rKR-w393dp-h852dp-xxhdpi")
class CaroLoadingCardTest {
    @get:Rule
    val composeRule = createComposeRule()

    // 무한 반복 Lottie가 있어 자동 시계를 켜 두면 waitForIdle이 끝나지 않는다.
    private fun showCard(
        message: String,
        detail: String? = null,
    ) {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            CaroTheme {
                CaroLoadingCard(message = message, detail = detail)
            }
        }
        composeRule.mainClock.advanceTimeByFrame()
    }

    private fun textNodes() = composeRule.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.Text))

    private fun contentDescriptionNodes() = composeRule.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.ContentDescription))

    private val isPoliteLiveRegion = SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite)

    @Test
    fun `문구를 보여준다`() {
        showCard(message = MESSAGE)

        composeRule.onNodeWithText(MESSAGE).assertIsDisplayed()
    }

    @Test
    fun `부가 문구를 주면 함께 보여준다`() {
        showCard(message = MESSAGE, detail = DETAIL)

        composeRule.onNodeWithText(DETAIL).assertIsDisplayed()
    }

    @Test
    fun `부가 문구가 없으면 글자는 문구 하나뿐이다`() {
        showCard(message = MESSAGE)

        textNodes().assertCountEquals(1)
    }

    @Test
    fun `문구는 polite live region으로 노출된다`() {
        showCard(message = MESSAGE)

        composeRule.onNodeWithText(MESSAGE).assert(isPoliteLiveRegion)
    }

    @Test
    fun `부가 문구가 있어도 문구는 polite live region으로 노출된다`() {
        showCard(message = MESSAGE, detail = DETAIL)

        composeRule.onNodeWithText(MESSAGE).assert(isPoliteLiveRegion)
    }

    @Test
    fun `로딩 애니메이션은 접근성 설명을 따로 노출하지 않는다`() {
        showCard(message = MESSAGE, detail = DETAIL)

        contentDescriptionNodes().assertCountEquals(0)
    }

    private companion object {
        const val MESSAGE = "파일을 읽고 있어요"
        const val DETAIL = "TOEIC 3000.apkg"
    }
}

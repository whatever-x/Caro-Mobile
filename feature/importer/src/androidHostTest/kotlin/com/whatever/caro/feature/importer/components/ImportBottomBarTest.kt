package com.whatever.caro.feature.importer.components

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.whatever.caro.core.designsystem.themes.CaroTheme
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "ko-rKR-w393dp-h852dp-xxhdpi")
class ImportBottomBarTest {
    @get:Rule
    val composeRule = createComposeRule()

    private var clickCount = 0

    private fun show(
        enabled: Boolean,
        disabledReason: String? = null,
    ) {
        composeRule.setContent {
            CaroTheme {
                ImportBottomBar(
                    buttonText = BUTTON_TEXT,
                    onClick = { clickCount += 1 },
                    enabled = enabled,
                    disabledReason = disabledReason,
                )
            }
        }
    }

    @Test
    fun `꺼진 버튼은 사유를 화면에 그리지 않고 상태 설명으로만 읽는다`() {
        show(enabled = false, disabledReason = REASON)

        val button = composeRule.onNodeWithText(BUTTON_TEXT)
        button.assertIsNotEnabled()
        button.fetchSemanticsNode().config[SemanticsProperties.StateDescription] shouldBe REASON
        composeRule.onNodeWithText(REASON).assertDoesNotExist()
    }

    @Test
    fun `켜진 버튼은 상태 설명 없이 누르면 클릭을 보낸다`() {
        show(enabled = true)

        val button = composeRule.onNodeWithText(BUTTON_TEXT)
        button.assertIsEnabled()
        button.assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.StateDescription))
        button.performClick()

        clickCount shouldBe 1
    }

    private companion object {
        const val BUTTON_TEXT = "다음"
        const val REASON = "한 번에 10자까지 붙여 넣을 수 있어요. 나눠서 가져와 주세요."
    }
}

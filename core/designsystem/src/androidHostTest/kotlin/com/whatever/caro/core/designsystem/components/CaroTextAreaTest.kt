package com.whatever.caro.core.designsystem.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.whatever.caro.core.designsystem.themes.CaroTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w393dp-h852dp-xxhdpi")
class CaroTextAreaTest {
    @get:Rule
    val composeRule = createComposeRule()

    private var verticalPadding: Dp = 0.dp

    private fun field(): SemanticsNodeInteraction = composeRule.onNode(hasSetTextAction())

    private fun showFilledArea(parentHeight: Dp) {
        composeRule.setContent {
            CaroTheme {
                verticalPadding = CaroTheme.spacing.l
                Column(modifier = Modifier.height(parentHeight)) {
                    CaroTextArea(
                        value = "",
                        onValueChange = {},
                        modifier = Modifier.weight(1f),
                        fillHeight = true,
                    )
                }
            }
        }
        composeRule.waitForIdle()
    }

    @Test
    fun `fillHeight가 true이면 부모가 정한 높이에서 안쪽 여백을 뺀 높이를 채운다`() {
        showFilledArea(parentHeight = TALL_PARENT_DP.dp)

        field().assertHeightIsEqualTo(TALL_PARENT_DP.dp - verticalPadding * 2)
    }

    @Test
    fun `fillHeight가 true이면 부모가 최소 높이보다 낮아도 최소 높이를 강제하지 않는다`() {
        showFilledArea(parentHeight = SHORT_PARENT_DP.dp)

        field().assertHeightIsEqualTo(SHORT_PARENT_DP.dp - verticalPadding * 2)
    }

    private companion object {
        const val TALL_PARENT_DP = 300
        const val SHORT_PARENT_DP = 80
    }
}

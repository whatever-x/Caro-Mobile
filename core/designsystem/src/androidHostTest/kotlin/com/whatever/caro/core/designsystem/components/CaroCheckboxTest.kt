package com.whatever.caro.core.designsystem.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTouchHeightIsEqualTo
import androidx.compose.ui.test.assertTouchWidthIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.whatever.caro.core.designsystem.themes.CaroTheme
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "ko-rKR-w393dp-h852dp-xxhdpi")
class CaroCheckboxTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun showCheckbox(
        state: ToggleableState,
        onClick: (() -> Unit)? = {},
        enabled: Boolean = true,
    ) {
        composeRule.setContent { CaroTheme { CaroCheckbox(state = state, onClick = onClick, enabled = enabled) } }
        composeRule.waitForIdle()
    }

    private fun showDecorativeAndInteractive(state: ToggleableState) {
        composeRule.setContent {
            CaroTheme {
                Column {
                    Box(modifier = Modifier.testTag(DECORATIVE_TAG)) { CaroCheckbox(state = state, onClick = null) }
                    Box(modifier = Modifier.testTag(INTERACTIVE_TAG)) { CaroCheckbox(state = state, onClick = {}) }
                }
            }
        }
        composeRule.waitForIdle()
    }

    private fun showTaggedCheckbox(state: ToggleableState) {
        composeRule.setContent {
            CaroTheme {
                Box(modifier = Modifier.testTag(TAG)) { CaroCheckbox(state = state, onClick = null) }
            }
        }
        composeRule.waitForIdle()
    }

    private fun showAllStates() {
        composeRule.setContent {
            CaroTheme {
                Column {
                    STATE_TAGS.forEach { (state, tag) ->
                        Box(modifier = Modifier.testTag(tag)) { CaroCheckbox(state = state, onClick = null) }
                    }
                }
            }
        }
        composeRule.waitForIdle()
    }

    private fun checkbox(): SemanticsNodeInteraction =
        composeRule.onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.ToggleableState))

    private fun stateTag(state: ToggleableState): String = STATE_TAGS.getValue(state)

    @Test
    fun `선택 상태는 ToggleableState On으로 노출된다`() {
        showCheckbox(ToggleableState.On)

        checkbox().assert(SemanticsMatcher.expectValue(SemanticsProperties.ToggleableState, ToggleableState.On))
    }

    @Test
    fun `해제 상태는 ToggleableState Off로 노출된다`() {
        showCheckbox(ToggleableState.Off)

        checkbox().assert(SemanticsMatcher.expectValue(SemanticsProperties.ToggleableState, ToggleableState.Off))
    }

    @Test
    fun `일부 선택 상태는 ToggleableState Indeterminate로 노출된다`() {
        showCheckbox(ToggleableState.Indeterminate)

        checkbox().assert(
            SemanticsMatcher.expectValue(SemanticsProperties.ToggleableState, ToggleableState.Indeterminate),
        )
    }

    @Test
    fun `누를 수 있으면 Role Checkbox로 노출된다`() {
        showCheckbox(ToggleableState.Off)

        checkbox().assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox))
    }

    @Test
    fun `해제 상태에서 누르면 onClick을 한 번 호출한다`() {
        var clicks = 0
        showCheckbox(ToggleableState.Off, onClick = { clicks += 1 })

        checkbox().performClick()

        clicks shouldBe 1
    }

    @Test
    fun `일부 선택 상태에서 누르면 onClick을 한 번 호출한다`() {
        var clicks = 0
        showCheckbox(ToggleableState.Indeterminate, onClick = { clicks += 1 })

        checkbox().performClick()

        clicks shouldBe 1
    }

    @Test
    fun `onClick이 없으면 누르기 동작을 노출하지 않는다`() {
        showCheckbox(ToggleableState.On, onClick = null)

        composeRule
            .onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsActions.OnClick))
            .assertCountEquals(0)
    }

    @Test
    fun `일부 선택 상태의 상태 설명은 한국어로 일부 선택됨이다`() {
        showCheckbox(ToggleableState.Indeterminate)

        checkbox().assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, PARTIAL_KO))
    }

    @Test
    @Config(qualifiers = "en-rUS-w393dp-h852dp-xxhdpi")
    fun `일부 선택 상태의 상태 설명은 영어로 Partially selected다`() {
        showCheckbox(ToggleableState.Indeterminate)

        checkbox().assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, PARTIAL_EN))
    }

    @Test
    fun `선택 상태는 상태 설명을 따로 정하지 않는다`() {
        showCheckbox(ToggleableState.On)

        checkbox().assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.StateDescription))
    }

    @Test
    fun `해제 상태는 상태 설명을 따로 정하지 않는다`() {
        showCheckbox(ToggleableState.Off)

        checkbox().assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.StateDescription))
    }

    @Test
    fun `줄 안의 일부 선택 체크박스는 누를 수 없어도 줄을 일부 선택됨으로 읽게 한다`() {
        composeRule.setContent {
            CaroTheme {
                Row(modifier = Modifier.clickable {}) {
                    Text(text = ROW_LABEL)
                    CaroCheckbox(state = ToggleableState.Indeterminate, onClick = null)
                }
            }
        }
        composeRule.waitForIdle()

        composeRule
            .onNodeWithText(ROW_LABEL)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, PARTIAL_KO))
    }

    @Test
    fun `크기는 22dp 정사각형이다`() {
        showTaggedCheckbox(ToggleableState.On)

        composeRule.onNodeWithTag(TAG).assertWidthIsEqualTo(22.dp)
        composeRule.onNodeWithTag(TAG).assertHeightIsEqualTo(22.dp)
    }

    @Test
    fun `누를 수 있으면 누르는 영역은 48dp 정사각형이다`() {
        showCheckbox(ToggleableState.On)

        checkbox().assertWidthIsEqualTo(48.dp)
        checkbox().assertHeightIsEqualTo(48.dp)
        checkbox().assertTouchWidthIsEqualTo(48.dp)
        checkbox().assertTouchHeightIsEqualTo(48.dp)
    }

    @Test
    fun `누를 수 있어도 보이는 선택 모양은 장식용과 같은 크기다`() {
        showDecorativeAndInteractive(ToggleableState.On)

        val decorative = composeRule.onNodeWithTag(DECORATIVE_TAG).renderPixels()
        val interactive = composeRule.onNodeWithTag(INTERACTIVE_TAG).renderPixels()

        interactive.countPixels(BRAND_SURFACE) shouldBe decorative.countPixels(BRAND_SURFACE)
        interactive.countPixels(INVERSE_ICON) shouldBe decorative.countPixels(INVERSE_ICON)
    }

    @Test
    fun `누를 수 있어도 보이는 해제 테두리는 장식용과 같은 크기다`() {
        showDecorativeAndInteractive(ToggleableState.Off)

        val decorative = composeRule.onNodeWithTag(DECORATIVE_TAG).renderPixels()
        val interactive = composeRule.onNodeWithTag(INTERACTIVE_TAG).renderPixels()

        interactive.countPixels(BORDER_PRIMARY) shouldBe decorative.countPixels(BORDER_PRIMARY)
    }

    @Test
    fun `enabled 기본값은 true다`() {
        showCheckbox(ToggleableState.Off)

        checkbox().assertIsEnabled()
    }

    @Test
    fun `enabled가 false이면 비활성 상태로 노출된다`() {
        showCheckbox(ToggleableState.Off, enabled = false)

        checkbox().assertIsNotEnabled()
    }

    @Test
    fun `enabled가 false이면 눌러도 onClick을 호출하지 않는다`() {
        var clicks = 0
        showCheckbox(ToggleableState.Off, onClick = { clicks += 1 }, enabled = false)

        checkbox().performClick()

        clicks shouldBe 0
    }

    @Test
    fun `modifier는 상태 바로 다음 인자이고 장식용 크기에 적용된다`() {
        composeRule.setContent {
            CaroTheme { CaroCheckbox(ToggleableState.On, Modifier.testTag(TAG)) }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(TAG).assertWidthIsEqualTo(22.dp)
    }

    @Test
    fun `선택 상태는 brand 면 위에 inverse 체크를 그린다`() {
        showAllStates()

        val rendered = composeRule.onNodeWithTag(stateTag(ToggleableState.On)).renderPixels()

        rendered.surfaceColor() shouldBe BRAND_SURFACE
        rendered.countPixels(INVERSE_ICON) shouldBeGreaterThan 0
    }

    @Test
    fun `해제 상태는 brand 색을 그리지 않는다`() {
        showAllStates()

        val rendered = composeRule.onNodeWithTag(stateTag(ToggleableState.Off)).renderPixels()

        rendered.surfaceColor() shouldNotBe BRAND_SURFACE
        rendered.countPixels(BRAND_SURFACE) shouldBe 0
    }

    @Test
    fun `해제 상태는 primary 테두리를 그린다`() {
        showAllStates()

        val rendered = composeRule.onNodeWithTag(stateTag(ToggleableState.Off)).renderPixels()

        rendered.countPixels(BORDER_PRIMARY) shouldBeGreaterThan 0
    }

    @Test
    fun `일부 선택 모양은 해제 모양과 다르다`() {
        showAllStates()

        val partial = composeRule.onNodeWithTag(stateTag(ToggleableState.Indeterminate)).renderPixels()
        val off = composeRule.onNodeWithTag(stateTag(ToggleableState.Off)).renderPixels()

        partial.sameImageAs(off) shouldBe false
    }

    @Test
    fun `일부 선택 모양은 선택 모양과 다르다`() {
        showAllStates()

        val partial = composeRule.onNodeWithTag(stateTag(ToggleableState.Indeterminate)).renderPixels()
        val on = composeRule.onNodeWithTag(stateTag(ToggleableState.On)).renderPixels()

        partial.sameImageAs(on) shouldBe false
    }

    private companion object {
        const val TAG = "checkbox"
        const val DECORATIVE_TAG = "checkbox-decorative"
        const val INTERACTIVE_TAG = "checkbox-interactive"
        const val ROW_LABEL = "전체 선택"
        const val PARTIAL_KO = "일부 선택됨"
        const val PARTIAL_EN = "Partially selected"

        val STATE_TAGS =
            mapOf(
                ToggleableState.Off to "checkbox-off",
                ToggleableState.On to "checkbox-on",
                ToggleableState.Indeterminate to "checkbox-partial",
            )

        val BRAND_SURFACE = Color(0xFF4A6CF7)
        val INVERSE_ICON = Color(0xFFFFFFFF)
        val BORDER_PRIMARY = Color(0xFFF6F8FF)
    }
}

package com.whatever.caro.core.designsystem.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.whatever.caro.core.designsystem.themes.CaroTheme
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w393dp-h852dp-xxhdpi")
class CaroCtaButtonTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun button(): SemanticsNodeInteraction = composeRule.onNode(hasText(LABEL, substring = true))

    private fun showButton(
        enabled: Boolean = true,
        disabledReason: String? = null,
        onClick: () -> Unit = {},
    ) {
        composeRule.setContent {
            CaroTheme {
                CaroCtaButton(
                    enabled = enabled,
                    text = LABEL,
                    onClick = onClick,
                    modifier = Modifier.fillMaxWidth(),
                    disabledReason = disabledReason,
                )
            }
        }
        composeRule.waitForIdle()
    }

    private fun showButton(style: CaroCtaButtonStyle) {
        composeRule.setContent {
            CaroTheme {
                CaroCtaButton(
                    enabled = true,
                    text = LABEL,
                    onClick = {},
                    modifier = Modifier.fillMaxWidth(),
                    style = style,
                )
            }
        }
        composeRule.waitForIdle()
    }

    @Test
    fun `enabled를 생략하면 활성 버튼이다`() {
        var clicks = 0
        composeRule.setContent {
            CaroTheme { CaroCtaButton(text = LABEL, onClick = { clicks += 1 }) }
        }
        composeRule.waitForIdle()

        button().assertIsEnabled()
        button().performClick()
        clicks shouldBe 1
    }

    @Test
    fun `문구, onClick, modifier 순서의 위치 인자로 부를 수 있다`() {
        var clicks = 0
        composeRule.setContent {
            CaroTheme { CaroCtaButton(LABEL, { clicks += 1 }, Modifier.fillMaxWidth()) }
        }
        composeRule.waitForIdle()

        button().performClick()

        clicks shouldBe 1
    }

    @Test
    fun `활성 버튼은 Role Button으로 노출된다`() {
        showButton(enabled = true)

        button().assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
    }

    @Test
    fun `비활성 버튼도 Role Button으로 노출된다`() {
        showButton(enabled = false)

        button().assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
    }

    @Test
    fun `활성 버튼은 활성 상태로 노출된다`() {
        showButton(enabled = true)

        button().assertIsEnabled()
    }

    @Test
    fun `비활성 버튼은 비활성 상태로 노출된다`() {
        showButton(enabled = false)

        button().assertIsNotEnabled()
    }

    @Test
    fun `활성 버튼은 누르면 onClick을 한 번 호출한다`() {
        var clicks = 0
        showButton(enabled = true, onClick = { clicks += 1 })

        button().performClick()

        clicks shouldBe 1
    }

    @Test
    fun `비활성 버튼은 눌러도 onClick을 호출하지 않는다`() {
        var clicks = 0
        showButton(enabled = false, onClick = { clicks += 1 })

        button().performClick()

        clicks shouldBe 0
    }

    @Test
    fun `비활성이고 disabledReason이 있으면 버튼 문구와 이유를 함께 읽는다`() {
        showButton(enabled = false, disabledReason = REASON)

        val spoken = button().spokenText()

        spoken shouldContain LABEL
        spoken shouldContain REASON
    }

    @Test
    fun `활성이면 disabledReason이 있어도 이유를 읽지 않는다`() {
        showButton(enabled = true, disabledReason = REASON)

        button().spokenText() shouldNotContain REASON
    }

    @Test
    fun `disabledReason이 없으면 비활성이어도 상태 설명을 더하지 않는다`() {
        showButton(enabled = false, disabledReason = null)

        button().assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.StateDescription))
    }

    @Test
    fun `높이는 최소 56dp를 지킨다`() {
        showButton(enabled = true)

        button().assertHeightIsAtLeast(56.dp)
    }

    @Test
    fun `기본 스타일의 활성 버튼은 brand 면 위에 inverse 글자를 그린다`() {
        showButton(enabled = true)

        val rendered = button().renderPixels()

        rendered.surfaceColor() shouldBe BRAND_SURFACE
        rendered.countPixels(INVERSE_TEXT) shouldBeGreaterThan 0
    }

    @Test
    fun `Brand 스타일을 명시해도 기본 스타일과 같은 면 색을 그린다`() {
        showButton(CaroCtaButtonStyle.Brand)

        button().renderPixels().surfaceColor() shouldBe BRAND_SURFACE
    }

    @Test
    fun `비활성 버튼은 disabled 면 위에 disabled 글자를 그린다`() {
        showButton(enabled = false)

        val rendered = button().renderPixels()

        rendered.surfaceColor() shouldBe DISABLED_SURFACE
        rendered.countPixels(DISABLED_TEXT) shouldBeGreaterThan 0
    }

    @Test
    fun `Danger 스타일의 활성 버튼은 dangerous 면 위에 dangerous 글자를 그린다`() {
        showButton(CaroCtaButtonStyle.Danger)

        val rendered = button().renderPixels()

        rendered.surfaceColor() shouldBe DANGEROUS_SURFACE
        rendered.countPixels(DANGEROUS_TEXT) shouldBeGreaterThan 0
    }

    @Test
    fun `스타일은 Brand와 Danger 두 가지만 제공한다`() {
        CaroCtaButtonStyle.entries shouldContainExactly
            listOf(
                CaroCtaButtonStyle.Brand,
                CaroCtaButtonStyle.Danger,
            )
    }

    private fun SemanticsNodeInteraction.spokenText(): String {
        val config = fetchSemanticsNode().config
        val texts = config.getOrNull(SemanticsProperties.Text).orEmpty().map { it.text }
        val descriptions = config.getOrNull(SemanticsProperties.ContentDescription).orEmpty()
        val state = listOfNotNull(config.getOrNull(SemanticsProperties.StateDescription))
        return (texts + descriptions + state).joinToString(separator = " ")
    }

    private companion object {
        const val LABEL = "만들기"
        const val REASON = "이름을 입력해 주세요"

        val BRAND_SURFACE = Color(0xFF4A6CF7)
        val INVERSE_TEXT = Color(0xFFFFFFFF)
        val DISABLED_SURFACE = Color(0xFFEDF0FE)
        val DISABLED_TEXT = Color(0xFFC4C7CC)
        val DANGEROUS_SURFACE = Color(0xFFFFF2F1)
        val DANGEROUS_TEXT = Color(0xFFFF7A70)
    }
}

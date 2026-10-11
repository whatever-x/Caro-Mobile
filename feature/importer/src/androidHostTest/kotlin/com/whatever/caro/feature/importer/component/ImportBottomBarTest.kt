package com.whatever.caro.feature.importer.component

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
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
    private var shownReason by mutableStateOf<String?>(null)

    private val isPoliteLiveRegion = SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite)

    private fun show(
        enabled: Boolean = true,
        reason: String? = null,
    ) {
        shownReason = reason
        composeRule.setContent {
            CaroTheme {
                ImportBottomBar(
                    buttonText = BUTTON_TEXT,
                    onClick = { clickCount += 1 },
                    enabled = enabled,
                    reason = shownReason,
                )
            }
        }
    }

    @Test
    fun `사유가 있으면 줄로 보여주고 polite live region으로 알린다`() {
        show(enabled = false, reason = REASON)

        composeRule.onNodeWithText(REASON).assert(isPoliteLiveRegion)
    }

    @Test
    fun `사유가 없어도 live region 노드는 남아 있고 글자는 비어 있다`() {
        show()

        val texts = composeRule.onNode(isPoliteLiveRegion).fetchSemanticsNode().config[SemanticsProperties.Text]
        texts.joinToString("") { text -> text.text } shouldBe ""
    }

    @Test
    fun `사유가 생기면 같은 live region 노드의 글자만 바뀐다`() {
        show()
        val before = composeRule.onNode(isPoliteLiveRegion).fetchSemanticsNode().id

        composeRule.runOnIdle { shownReason = REASON }
        composeRule.waitForIdle()

        composeRule.onNode(isPoliteLiveRegion).fetchSemanticsNode().id shouldBe before
        composeRule.onNodeWithText(REASON).assert(isPoliteLiveRegion)
    }

    @Test
    fun `사유가 없으면 사유 줄은 높이를 차지하지 않는다`() {
        show()

        composeRule.onNode(isPoliteLiveRegion).assertHeightIsEqualTo(0.dp)
    }

    @Test
    fun `비활성 버튼은 사유를 상태 설명으로 함께 읽는다`() {
        show(enabled = false, reason = REASON)

        val button = composeRule.onNodeWithText(BUTTON_TEXT)
        button.assertIsNotEnabled()
        button.fetchSemanticsNode().config[SemanticsProperties.StateDescription] shouldBe REASON
    }

    @Test
    fun `활성 버튼은 상태 설명 없이 누르면 클릭을 보낸다`() {
        show(enabled = true)

        val button = composeRule.onNodeWithText(BUTTON_TEXT)
        button.assertIsEnabled()
        button.assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.StateDescription))
        button.performClick()

        clickCount shouldBe 1
    }

    private companion object {
        const val BUTTON_TEXT = "미리보기"
        const val REASON = "가져올 노트 유형을 하나 이상 골라 주세요."
    }
}

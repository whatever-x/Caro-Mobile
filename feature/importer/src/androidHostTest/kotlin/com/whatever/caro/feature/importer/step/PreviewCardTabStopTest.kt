package com.whatever.caro.feature.importer.step

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

// 키보드로 카드를 옮겨 다닐 때 카드마다 포커스가 한 번만 멈춰야 한다.
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "ko-rKR-w393dp-h852dp-xxhdpi")
class PreviewCardTabStopTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val host by lazy { PreviewStepHost(composeRule) }

    private fun focusedCardFront(): String? =
        composeRule
            .onAllNodes(isFocused())
            .fetchSemanticsNodes()
            .firstOrNull()
            ?.config
            ?.getOrNull(SemanticsProperties.Text)
            ?.firstOrNull()
            ?.text

    private fun pressTab() {
        composeRule.onRoot().performKeyInput { pressKey(Key.Tab) }
        composeRule.waitForIdle()
    }

    @Test
    fun `카드는 Tab으로 옮길 때 카드마다 한 번만 포커스가 멈춘다`() {
        host.show()
        composeRule.onNode(cardItem(PreviewStepTables.SCHOOL)).requestFocus()
        val visited = mutableListOf(focusedCardFront())

        repeat(TAB_PRESSES) {
            pressTab()
            visited += focusedCardFront()
        }

        visited shouldBe listOf("学校", "読む", "日本", "빈 항목입니다")
    }

    private companion object {
        const val TAB_PRESSES = 3
    }
}

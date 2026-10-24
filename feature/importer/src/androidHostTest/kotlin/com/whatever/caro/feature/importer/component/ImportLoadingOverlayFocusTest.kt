package com.whatever.caro.feature.importer.component

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.unit.dp
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.feature.importer.mvi.ImportLoading
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

// 읽는 동안 가려진 내용으로 키보드 Tab 이동이 들어가면 보이지 않는 요소를 조작하게 된다.
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "ko-rKR-w393dp-h852dp-xxhdpi")
class ImportLoadingOverlayFocusTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val outside = FocusRequester()

    private fun show(loading: ImportLoading?) {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            CaroTheme {
                Column {
                    Box(
                        modifier =
                            Modifier
                                .size(48.dp)
                                .testTag(OUTSIDE)
                                .focusRequester(outside)
                                .focusable(),
                    )
                    ImportLoadingOverlay(loading = loading) {
                        Box(modifier = Modifier.size(48.dp).testTag(INSIDE).focusable())
                    }
                }
            }
        }
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.runOnIdle { outside.requestFocus() }
        composeRule.mainClock.advanceTimeByFrame()
    }

    private fun pressTab() {
        composeRule.onNodeWithTag(OUTSIDE).performKeyInput { pressKey(Key.Tab) }
        composeRule.mainClock.advanceTimeByFrame()
    }

    @Test
    fun `읽지 않을 때는 Tab으로 내용 안의 요소에 포커스가 들어간다`() {
        show(loading = null)

        pressTab()

        composeRule.onNodeWithTag(INSIDE).assertIsFocused()
    }

    @Test
    fun `읽는 동안에는 Tab으로 가려진 내용 안의 요소에 포커스가 들어가지 않는다`() {
        show(loading = ImportLoading.Parsing(fileName = "deck.apkg"))

        pressTab()

        composeRule.onNodeWithTag(INSIDE, useUnmergedTree = true).assertIsNotFocused()
    }

    private companion object {
        const val OUTSIDE = "outside"
        const val INSIDE = "inside"
    }
}

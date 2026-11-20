package com.whatever.caro.feature.importer.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.feature.importer.mvi.ImportLoading
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

// 로딩 카드의 무한 반복 Lottie 때문에 자동 시계를 켜 두면 waitForIdle이 끝나지 않으므로 시계를 직접 움직인다.
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "ko-rKR-w393dp-h852dp-xxhdpi")
class ImportLoadingFocusTest {
    @get:Rule
    val composeRule = createComposeRule()

    private var loading by mutableStateOf<ImportLoading?>(null)

    private fun show(initial: ImportLoading?) {
        loading = initial
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            CaroTheme {
                ImportLoadingOverlay(loading = loading) {
                    ImportStepTitle(text = TITLE)
                }
            }
        }
        composeRule.mainClock.advanceTimeBy(FRAME_MILLIS)
    }

    private fun finishLoading() {
        composeRule.runOnIdle { loading = null }
        // 상태 변경 알림을 비운 뒤 프레임을 움직여야 다시 그려지고 포커스 요청이 실행된다.
        composeRule.waitForIdle()
        composeRule.mainClock.advanceTimeBy(SETTLE_MILLIS)
    }

    @Test
    fun `로딩이 끝나면 단계 제목에 포커스를 둔다`() {
        show(ImportLoading.ReadingFile(fileName = "deck.apkg"))

        finishLoading()

        composeRule.onNodeWithText(TITLE).assertIsFocused()
    }

    @Test
    fun `붙여 넣은 내용을 읽다가 끝나도 단계 제목에 포커스를 둔다`() {
        show(ImportLoading.ReadingPaste)

        finishLoading()

        composeRule.onNodeWithText(TITLE).assertIsFocused()
    }

    @Test
    fun `읽은 적이 없으면 단계 제목에 포커스를 두지 않는다`() {
        show(initial = null)

        composeRule.mainClock.advanceTimeBy(SETTLE_MILLIS)

        composeRule.onNodeWithText(TITLE).assertIsNotFocused()
    }

    private companion object {
        const val TITLE = "어떻게 가져올까요?"
        const val FRAME_MILLIS = 16L
        const val SETTLE_MILLIS = 48L
    }
}

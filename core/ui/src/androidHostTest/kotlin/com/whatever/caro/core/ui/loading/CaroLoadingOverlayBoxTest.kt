package com.whatever.caro.core.ui.loading

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import androidx.navigationevent.compose.rememberNavigationEventDispatcherOwner
import com.whatever.caro.core.designsystem.themes.CaroTheme
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

// 지연 상수를 공용 helper와 나눠 쓰는 리팩터 전후로 CaroLoadingOverlayBox의 입력 차단과 semantics 비우기가 같아야 한다.
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "ko-rKR-w393dp-h852dp-xxhdpi")
class CaroLoadingOverlayBoxTest {
    @get:Rule
    val composeRule = createComposeRule()

    private var isLoading by mutableStateOf(false)
    private var clickCount = 0

    // 스크림이 뜨면 무한 반복 Lottie가 돌아 자동 시계를 켜 두면 waitForIdle이 끝나지 않는다.
    private fun show(loading: Boolean) {
        isLoading = loading
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            // 로딩 중에는 뒤로 가기 핸들러가 붙는데, 테스트 호스트에는 dispatcher owner가 없어 직접 공급한다.
            CompositionLocalProvider(
                LocalNavigationEventDispatcherOwner provides rememberNavigationEventDispatcherOwner(parent = null),
            ) {
                CaroTheme {
                    CaroLoadingOverlayBox(isLoading = isLoading) {
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxSize()
                                    .clickable { clickCount += 1 },
                        ) {
                            BasicText(text = CONTENT_TEXT)
                        }
                    }
                }
            }
        }
        composeRule.mainClock.advanceTimeByFrame()
    }

    private fun tapRoot() {
        composeRule.onRoot().performTouchInput { click() }
    }

    @Test
    fun `로딩이 아니면 내용을 보여준다`() {
        show(loading = false)

        composeRule.onNodeWithText(CONTENT_TEXT).assertIsDisplayed()
    }

    @Test
    fun `로딩 중에는 내용이 접근성 트리에서 빠진다`() {
        show(loading = true)

        composeRule.onNodeWithText(CONTENT_TEXT).assertDoesNotExist()
    }

    @Test
    fun `로딩이 끝나면 내용이 접근성 트리로 돌아온다`() {
        show(loading = true)

        composeRule.runOnIdle { isLoading = false }
        // 로딩이 끝나면 무한 애니메이션이 없어 자동 시계로 idle을 기다릴 수 있다.
        composeRule.mainClock.autoAdvance = true
        composeRule.waitForIdle()

        composeRule.onNodeWithText(CONTENT_TEXT).assertExists()
    }

    @Test
    fun `로딩이 아니면 터치가 내용에 전달된다`() {
        show(loading = false)

        tapRoot()

        clickCount shouldBe 1
    }

    @Test
    fun `로딩 중에는 내용으로 가는 터치를 막는다`() {
        show(loading = true)

        tapRoot()

        clickCount shouldBe 0
    }

    private companion object {
        const val CONTENT_TEXT = "로딩 아래 내용"
    }
}

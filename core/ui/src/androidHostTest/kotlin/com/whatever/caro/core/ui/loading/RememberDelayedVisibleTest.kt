package com.whatever.caro.core.ui.loading

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

private const val PROBE_TEXT = "지연 뒤 보이는 내용"

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "ko-rKR-w393dp-h852dp-xxhdpi")
class RememberDelayedVisibleTest {
    @get:Rule
    val composeRule = createComposeRule()

    private var active by mutableStateOf(false)

    // 자동 시계를 끄지 않으면 가상 시간이 제멋대로 흘러 200ms 경계를 확인할 수 없다.
    private fun show(initialActive: Boolean) {
        active = initialActive
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent { DelayedProbe(active = active) }
    }

    private fun advance(millis: Long) = composeRule.mainClock.advanceTimeBy(millis)

    // 상태 쓰기가 recomposition에 반영되려면 시계를 움직이기 전에 한 번 idle을 거쳐야 한다.
    private fun changeActive(value: Boolean) {
        composeRule.runOnIdle { active = value }
        composeRule.waitForIdle()
    }

    private fun assertProbeHidden() = composeRule.onNodeWithText(PROBE_TEXT).assertDoesNotExist()

    private fun assertProbeShown() = composeRule.onNodeWithText(PROBE_TEXT).assertIsDisplayed()

    @Test
    fun `활성이 아니면 시간이 지나도 보이지 않는다`() {
        show(initialActive = false)

        advance(LONG_WAIT_MILLIS)

        assertProbeHidden()
    }

    @Test
    fun `활성이 되고 지연 시간이 지나기 전에는 보이지 않는다`() {
        show(initialActive = true)

        advance(BEFORE_DELAY_MILLIS)

        assertProbeHidden()
    }

    @Test
    fun `활성이 되고 지연 시간이 지나면 보인다`() {
        show(initialActive = true)

        advance(AFTER_DELAY_MILLIS)

        assertProbeShown()
    }

    @Test
    fun `지연 시간 안에 비활성이 되면 끝내 보이지 않는다`() {
        show(initialActive = true)
        advance(BEFORE_DELAY_MILLIS)

        changeActive(false)
        advance(LONG_WAIT_MILLIS)

        assertProbeHidden()
    }

    @Test
    fun `보인 뒤 비활성이 되면 바로 사라진다`() {
        show(initialActive = true)
        advance(AFTER_DELAY_MILLIS)

        changeActive(false)
        advance(FRAME_MILLIS)

        assertProbeHidden()
    }

    @Test
    fun `비활성 뒤 다시 활성이 되면 지연 시간을 처음부터 센다`() {
        show(initialActive = true)
        advance(AFTER_DELAY_MILLIS)
        changeActive(false)
        advance(FRAME_MILLIS)

        changeActive(true)
        advance(BEFORE_DELAY_MILLIS)

        assertProbeHidden()
    }

    @Test
    fun `비활성 뒤 다시 활성이 되고 지연 시간이 지나면 다시 보인다`() {
        show(initialActive = true)
        advance(AFTER_DELAY_MILLIS)
        changeActive(false)
        advance(FRAME_MILLIS)

        changeActive(true)
        advance(AFTER_DELAY_MILLIS)

        assertProbeShown()
    }

    private companion object {
        const val FRAME_MILLIS = 16L
        const val BEFORE_DELAY_MILLIS = 192L
        const val AFTER_DELAY_MILLIS = 240L
        const val LONG_WAIT_MILLIS = 1_000L
    }
}

@Composable
private fun DelayedProbe(active: Boolean) {
    if (rememberDelayedVisible(active)) {
        BasicText(text = PROBE_TEXT)
    }
}

package com.whatever.caro.feature.importer.step

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.click
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.feature.importer.ImportDeckScreen
import com.whatever.caro.feature.importer.mvi.ImportDeckIntent
import com.whatever.caro.feature.importer.mvi.ImportDeckState
import io.kotest.matchers.shouldBe

/** 설정 화면을 ImportDeckScreen으로 그리고 보낸 입력을 [intents]에 남긴다. 상태는 테스트가 [update]로 바꾼다. */
internal class SettingsScreenHost(
    private val composeRule: ComposeContentTestRule,
) {
    val intents = mutableListOf<ImportDeckIntent>()
    var state by mutableStateOf(ImportDeckState())
        private set

    fun show(
        initial: ImportDeckState,
        width: Dp = 393.dp,
        height: Dp = 852.dp,
        fontScale: Float = 1f,
    ) {
        state = initial
        composeRule.setContent {
            val baseDensity = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density = baseDensity.density, fontScale = fontScale)) {
                CaroTheme {
                    Box(modifier = Modifier.size(width, height)) {
                        ImportDeckScreen(state = state, onIntent = { intent -> intents += intent })
                    }
                }
            }
        }
        composeRule.waitForIdle()
    }

    fun update(next: ImportDeckState) {
        composeRule.runOnIdle { state = next }
        composeRule.waitForIdle()
    }

    /** 보이는 모양의 위와 아래에서 48dp 높이가 끝나기 1dp 전 지점을 차례로 누른다. */
    fun tapTouchEdges(visible: DpRect) {
        val reach = (MIN_TOUCH_HEIGHT - visible.height) / 2 - 1.dp
        (reach > 0.dp) shouldBe true
        val x = (visible.left + visible.right) / 2
        tapAt(x, visible.top - reach)
        tapAt(x, visible.bottom + reach)
    }

    /** 보이는 모양의 가운데에서 좌우로 48dp 폭이 끝나기 1dp 전 지점을 차례로 누른다. */
    fun tapTouchSides(visible: DpRect) {
        val reach = MIN_TOUCH_HEIGHT / 2 - 1.dp
        val x = (visible.left + visible.right) / 2
        val y = (visible.top + visible.bottom) / 2
        tapAt(x - reach, y)
        tapAt(x + reach, y)
    }

    private fun tapAt(
        x: Dp,
        y: Dp,
    ) {
        val position = with(composeRule.density) { Offset(x.toPx(), y.toPx()) }
        composeRule.onAllNodes(isRoot()).onFirst().performTouchInput { click(position) }
        composeRule.waitForIdle()
    }

    companion object {
        val MIN_TOUCH_HEIGHT = 48.dp
    }
}

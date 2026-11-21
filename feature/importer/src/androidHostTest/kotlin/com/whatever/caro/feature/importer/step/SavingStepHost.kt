package com.whatever.caro.feature.importer.step

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.feature.importer.mvi.SavingProgress
import com.whatever.caro.feature.importer.mvi.SavingStatus

/** 멈추기 요청만 기록한다. 저장 상태가 바뀐 뒤의 화면은 상태를 새로 받아 그려서 본다. */
internal class SavingStepHost(
    private val composeRule: ComposeContentTestRule,
) {
    var stopClicks = 0
        private set
    private var shown by mutableStateOf(progress())

    fun show(
        progress: SavingProgress = progress(),
        reduceMotion: Boolean = false,
        width: Dp = 393.dp,
        height: Dp = 800.dp,
        fontScale: Float = 1f,
    ) {
        shown = progress
        composeRule.setContent {
            val baseDensity = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density = baseDensity.density, fontScale = fontScale)) {
                CaroTheme {
                    Box(modifier = Modifier.size(width, height)) {
                        SavingStep(
                            progress = shown,
                            reduceMotion = reduceMotion,
                            onStop = { stopClicks += 1 },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        }
        composeRule.waitForIdle()
    }

    fun update(progress: SavingProgress) {
        composeRule.runOnIdle { shown = progress }
        composeRule.waitForIdle()
    }

    companion object {
        const val DECK_NAME = "JLPT N5 단어"
        const val TOTAL = 1_512
        const val SAVING = "카드를 저장하고 있어요"
        const val RETRYING = "연결이 끊겨 다시 시도하고 있어요"
        const val FINISHING = "저장을 마무리하고 있어요"
        const val STOPPING = "멈추고 있어요. 1,000장까지 저장했어요."
        const val WARNING = "멈추거나 앱을 닫으면 저장한 카드까지만 덱에 남아요."
        const val STOP = "가져오기 멈추기"

        fun progress(
            savedCards: Int = 1_000,
            status: SavingStatus = SavingStatus.Saving,
        ) = SavingProgress(deckName = DECK_NAME, totalCards = TOTAL, savedCards = savedCards, status = status)
    }
}

internal val hasProgressRange: SemanticsMatcher = SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo)

internal val isPoliteRegion: SemanticsMatcher = SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite)

/** 내용 설명과 글자 가운데 하나라도 [snippet]을 포함하면 그렇게 읽힌다고 본다. */
internal fun readsContaining(snippet: String): SemanticsMatcher =
    SemanticsMatcher("스크린 리더가 `$snippet`을 포함해 읽는다") { node ->
        val descriptions = node.config.getOrElse(SemanticsProperties.ContentDescription) { emptyList() }
        val texts = node.config.getOrElse(SemanticsProperties.Text) { emptyList() }.map { text -> text.text }
        (descriptions + texts).any { part -> snippet in part }
    }

/** 화면에 있는 세로 스크롤 가운데 가장 많이 움직일 수 있는 양이다. 스크롤 영역이 없으면 0이다. */
internal fun ComposeContentTestRule.verticalScrollMax(): Float =
    onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange))
        .fetchSemanticsNodes()
        .maxOfOrNull { node -> node.config[SemanticsProperties.VerticalScrollAxisRange].maxValue() } ?: 0f

/** 구간 끝점이 0이든 총 장수든 같은 비율이 나오도록 구간 길이로 나눈다. */
internal fun SemanticsNodeInteraction.progressFraction(): Float {
    val info = fetchSemanticsNode().config[SemanticsProperties.ProgressBarRangeInfo]
    return (info.current - info.range.start) / (info.range.endInclusive - info.range.start)
}

/** Robolectric에서 captureToImage는 draw 대기에서 멈추므로 view를 직접 그려 읽는다. 왼쪽 10% 지점의 세로 중앙은 글자가 닿지 않는 면 색이다. */
internal fun SemanticsNodeInteraction.surfaceColor(): Color {
    val node = fetchSemanticsNode()
    val view = (node.root as ViewRootForTest).view
    val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
    view.draw(Canvas(bitmap))
    val bounds = node.boundsInRoot
    return bitmap.asImageBitmap().toPixelMap()[(bounds.left + bounds.width * 0.1f).toInt(), bounds.center.y.toInt()]
}

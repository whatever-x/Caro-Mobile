package com.whatever.caro.feature.importer.step

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import caromobile.core.designsystem.generated.resources.Res
import caromobile.core.designsystem.generated.resources.import_a11y_saved_progress
import caromobile.core.designsystem.generated.resources.import_button_stop_import
import caromobile.core.designsystem.generated.resources.import_hint_saving_stop_warning
import caromobile.core.designsystem.generated.resources.import_label_saving_progress
import caromobile.core.designsystem.generated.resources.import_state_finishing
import caromobile.core.designsystem.generated.resources.import_state_retrying
import caromobile.core.designsystem.generated.resources.import_state_stopping
import caromobile.core.designsystem.generated.resources.import_subtitle_saving
import com.whatever.caro.core.designsystem.components.CaroCtaButtonStyle
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.core.util.NumberFormatter.formatWithComma
import com.whatever.caro.feature.importer.component.ImportBottomBar
import com.whatever.caro.feature.importer.logic.SavingProgressStyle
import com.whatever.caro.feature.importer.logic.savingProgressStyle
import com.whatever.caro.feature.importer.mvi.SavingProgress
import com.whatever.caro.feature.importer.mvi.SavingStatus
import org.jetbrains.compose.resources.stringResource

private val TrackHeight = 8.dp
private const val FLOW_SEGMENT_FRACTION = 0.2f
private const val FLOW_DURATION_MILLIS = 1_400

// 트랙 색 위에서 한 단계 옅은 brand 색으로 보이는 투명도다.
private const val FLOW_SEGMENT_ALPHA = 0.45f

/**
 * 평소에는 내용이 가운데에 놓이고 공간이 모자랄 때만 스크롤한다. [가져오기 멈추기]는 어느 경우에도 고정이다.
 * 멈추는 중에는 이미 멈춤을 요청한 뒤라 버튼을 막는다. 멈춤 확인은 ViewModel이 연다.
 */
@Composable
internal fun SavingStep(
    progress: SavingProgress,
    reduceMotion: Boolean,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = CaroTheme.spacing.xl2),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = progress.deckName,
                style = CaroTheme.typography.label1,
                color = CaroTheme.color.text.secondary,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(CaroTheme.spacing.s))
            Text(
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                text = statusText(progress),
                style = CaroTheme.typography.heading1,
                color = CaroTheme.color.text.primary,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(CaroTheme.spacing.xl2))
            SavingProgressBar(progress = progress, reduceMotion = reduceMotion)
            Spacer(modifier = Modifier.height(CaroTheme.spacing.xl2))
            Text(
                text = stringResource(Res.string.import_hint_saving_stop_warning),
                style = CaroTheme.typography.caption1.regular,
                color = CaroTheme.color.text.secondary,
                textAlign = TextAlign.Center,
            )
        }
        ImportBottomBar(
            buttonText = stringResource(Res.string.import_button_stop_import),
            onClick = onStop,
            enabled = progress.status != SavingStatus.Stopping,
            style = CaroCtaButtonStyle.Danger,
        )
    }
}

@Composable
private fun statusText(progress: SavingProgress): String =
    when (progress.status) {
        SavingStatus.Saving -> stringResource(Res.string.import_subtitle_saving)
        SavingStatus.WaitingForServer -> stringResource(Res.string.import_state_finishing)
        SavingStatus.Retrying -> stringResource(Res.string.import_state_retrying)
        SavingStatus.Stopping -> stringResource(Res.string.import_state_stopping, progress.savedCards.formatWithComma())
    }

/**
 * 막대와 장수 글자를 한 노드로 묶어야 "1,000 / 1,512장" 대신 "1,512장 중 1,000장 저장됨"으로 읽힌다.
 * 첫 응답 전에는 진행률을 알 수 없으므로 진행률 없는 진행 중으로 읽는다.
 */
@Composable
private fun SavingProgressBar(
    progress: SavingProgress,
    reduceMotion: Boolean,
) {
    val style = savingProgressStyle(progress, reduceMotion)
    val saved = progress.savedCards.formatWithComma()
    val total = progress.totalCards.formatWithComma()
    val announcement = stringResource(Res.string.import_a11y_saved_progress, total, saved)
    val fraction = if (progress.totalCards > 0) (progress.savedCards.toFloat() / progress.totalCards).coerceIn(0f, 1f) else 0f
    val rangeInfo =
        if (style == SavingProgressStyle.Determinate) {
            ProgressBarRangeInfo(fraction, 0f..1f)
        } else {
            ProgressBarRangeInfo.Indeterminate
        }

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = true) {
                    liveRegion = LiveRegionMode.Polite
                    contentDescription = announcement
                    progressBarRangeInfo = rangeInfo
                },
        verticalArrangement = Arrangement.spacedBy(CaroTheme.spacing.m),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ProgressTrack(style = style, fraction = fraction)
        val label = stringResource(Res.string.import_label_saving_progress, saved, total)
        val savedStart = label.indexOf(saved)
        val savedStyle =
            CaroTheme.typography.body1
                .toSpanStyle()
                .copy(color = CaroTheme.color.text.brand)
        Text(
            text =
                buildAnnotatedString {
                    append(label)
                    if (savedStart >= 0) addStyle(savedStyle, savedStart, savedStart + saved.length)
                },
            style = CaroTheme.typography.label1,
            color = CaroTheme.color.text.secondary,
        )
    }
}

@Composable
private fun ProgressTrack(
    style: SavingProgressStyle,
    fraction: Float,
) {
    val fillColor = CaroTheme.color.surface.brand
    val segmentColor = fillColor.copy(alpha = FLOW_SEGMENT_ALPHA)

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(TrackHeight)
                .clip(CaroTheme.shape.xxl)
                .background(CaroTheme.color.surface.tertiary),
    ) {
        when (style) {
            SavingProgressStyle.Determinate -> {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth(fraction)
                            .height(TrackHeight)
                            .background(fillColor),
                )
            }

            SavingProgressStyle.Static -> {
                FlowSegment(startProvider = { 0f }, color = segmentColor)
            }

            SavingProgressStyle.Indeterminate -> {
                val start =
                    rememberInfiniteTransition().animateFloat(
                        initialValue = -FLOW_SEGMENT_FRACTION,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(tween(FLOW_DURATION_MILLIS, easing = LinearEasing), RepeatMode.Restart),
                    )
                FlowSegment(startProvider = { start.value }, color = segmentColor)
            }
        }
    }
}

/** [startProvider]는 조각 왼쪽 끝의 위치를 막대 너비 비율로 준다. 그리기 단계에서만 읽어 매 프레임 다시 구성하지 않는다. */
@Composable
private fun FlowSegment(
    startProvider: () -> Float,
    color: Color,
) {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .drawBehind {
                    drawRect(
                        color = color,
                        topLeft = Offset(x = startProvider() * size.width, y = 0f),
                        size = Size(width = size.width * FLOW_SEGMENT_FRACTION, height = size.height),
                    )
                },
    )
}

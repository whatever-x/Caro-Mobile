package com.whatever.caro.feature.importer.step

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import caromobile.core.designsystem.generated.resources.Res
import caromobile.core.designsystem.generated.resources.import_label_first_card_preview
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.feature.importer.transform.ImportCard
import org.jetbrains.compose.resources.stringResource

// Figma의 skeleton 색(skeleton/secondary)은 검정 10%인데 같은 토큰이 없어 글자 색에 투명도를 준다.
private const val SKELETON_ALPHA = 0.1f
private val BorderWidth = 1.dp
private val SkeletonGap = 2.dp
private val FrontBarWidth = 120.dp
private val FrontBarHeight = 16.dp
private val FrontLineHeight = 21.dp
private val BackBarWidth = 72.dp
private val BackBarHeight = 10.dp
private val BackLineHeight = 14.dp

/**
 * 다시 나눈 결과가 오기 전에는 지난 카드가 맞는 것처럼 보이지 않게 [isLoading] 동안 skeleton을 보인다.
 * 누를 수 없는 카드라 앞뒷면 글자가 따로 읽히지 않도록 한 노드로 묶는다.
 */
@Composable
internal fun FirstCardSection(
    card: ImportCard?,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(CaroTheme.spacing.s)) {
        Text(
            text = stringResource(Res.string.import_label_first_card_preview),
            style = CaroTheme.typography.body2.semiBold,
            color = CaroTheme.color.text.primary,
        )
        when {
            isLoading -> {
                FirstCardSkeleton()
            }

            card != null -> {
                PreviewCardItem(
                    card = card,
                    typeName = null,
                    onClick = null,
                    modifier = Modifier.semantics(mergeDescendants = true) {},
                )
            }
        }
    }
}

@Composable
private fun FirstCardSkeleton() {
    val shape = CaroTheme.shape.m
    val bar =
        CaroTheme.color.text.primary
            .copy(alpha = SKELETON_ALPHA)

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clearAndSetSemantics {}
                .background(CaroTheme.color.surface.primary, shape)
                .border(BorderWidth, CaroTheme.color.border.secondary, shape)
                .padding(BorderWidth)
                .padding(CaroTheme.spacing.xl),
        verticalArrangement = Arrangement.spacedBy(SkeletonGap),
    ) {
        SkeletonBar(width = FrontBarWidth, barHeight = FrontBarHeight, lineHeight = FrontLineHeight, color = bar)
        SkeletonBar(width = BackBarWidth, barHeight = BackBarHeight, lineHeight = BackLineHeight, color = bar)
    }
}

/** 막대는 글자 줄 높이 안의 가운데에 두어 카드가 들어왔을 때 높이가 크게 바뀌지 않게 한다. */
@Composable
private fun SkeletonBar(
    width: Dp,
    barHeight: Dp,
    lineHeight: Dp,
    color: Color,
) {
    Box(modifier = Modifier.size(width, lineHeight).padding(vertical = (lineHeight - barHeight) / 2)) {
        Box(modifier = Modifier.size(width, barHeight).background(color, CaroTheme.shape.xs))
    }
}

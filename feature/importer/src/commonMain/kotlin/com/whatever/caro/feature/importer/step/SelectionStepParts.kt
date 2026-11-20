package com.whatever.caro.feature.importer.step

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import caromobile.core.designsystem.generated.resources.Res
import caromobile.core.designsystem.generated.resources.import_hint_header_row_off
import caromobile.core.designsystem.generated.resources.import_hint_header_row_on
import caromobile.core.designsystem.generated.resources.import_label_header_from_file
import caromobile.core.designsystem.generated.resources.import_label_header_row
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.feature.importer.components.ImportCheckbox
import com.whatever.caro.feature.importer.components.isLargeText
import org.jetbrains.compose.resources.stringResource

private val CardBorderWidth = 1.dp
private val CardEdgePadding = 4.dp
private val CardEdgeExtend = 24.dp
private val RowMinHeight = 50.dp
private val RowVerticalPadding = 14.dp
private val RowHorizontalPadding = 16.dp
private val DividerThickness = 1.dp
private val HeaderRowLineGap = 4.dp

// 체크와 설명 두 줄(40)에 위아래로 이만큼 더해야 누르는 높이가 48이 된다. 더한 만큼 겹쳐 놓아 시안의 간격은 그대로 둔다.
private val HeaderRowTouchExtension = 4.dp

// 설명 줄은 체크박스(22)와 간격(8)만큼 들여 체크 옆 글자와 시작을 맞춘다.
private val HeaderRowHintIndent = 30.dp

/** 목록 카드를 줄마다 아이템으로 나눠 그리므로 줄의 위치에 따라 둥근 모서리와 위아래 테두리 유무가 달라진다. */
internal enum class CardPosition(
    val isFirst: Boolean,
    val isLast: Boolean,
) {
    First(isFirst = true, isLast = false),
    Middle(isFirst = false, isLast = false),
    Last(isFirst = false, isLast = true),
    Only(isFirst = true, isLast = true),
    ;

    companion object {
        fun of(
            index: Int,
            lastIndex: Int,
        ): CardPosition =
            when {
                lastIndex == 0 -> Only
                index == 0 -> First
                index == lastIndex -> Last
                else -> Middle
            }
    }
}

/** 고정 하단 영역 위의 구분선이다. 높이는 항상 차지해 선이 생겨도 내용이 밀리지 않는다. */
@Composable
internal fun StepBottomDivider(
    isVisible: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(DividerThickness)
                .then(if (isVisible) Modifier.background(CaroTheme.color.surface.tertiary) else Modifier),
    )
}

/**
 * 파일에 열 이름이 적혀 있으면 첫 줄이 데이터라 바꿀 것이 없어 체크 대신 안내를 보인다.
 * 설명 줄까지 한 노드로 묶어야 지금 상태의 뜻이 체크와 함께 읽힌다.
 */
@Composable
internal fun HeaderRowSetting(
    checked: Boolean,
    columnsFromHeader: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (columnsFromHeader) {
        Text(
            text = stringResource(Res.string.import_label_header_from_file),
            modifier = modifier.fillMaxWidth(),
            style = CaroTheme.typography.body2.medium,
            color = CaroTheme.color.text.secondary,
        )
        return
    }
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .overlapVertically(HeaderRowTouchExtension)
                .toggleable(value = checked, role = Role.Checkbox, onValueChange = onCheckedChange)
                .padding(vertical = HeaderRowTouchExtension),
        verticalArrangement = Arrangement.spacedBy(HeaderRowLineGap),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(CaroTheme.spacing.s), verticalAlignment = Alignment.CenterVertically) {
            ImportCheckbox(state = if (checked) ToggleableState.On else ToggleableState.Off)
            Text(
                text = stringResource(Res.string.import_label_header_row),
                style = CaroTheme.typography.body2.medium,
                color = CaroTheme.color.text.primary,
            )
        }
        Text(
            text = stringResource(if (checked) Res.string.import_hint_header_row_on else Res.string.import_hint_header_row_off),
            modifier = Modifier.padding(start = HeaderRowHintIndent),
            style = CaroTheme.typography.caption1.regular,
            color = CaroTheme.color.text.secondary,
        )
    }
}

/** [interaction]은 줄 전체를 누르는 toggleable이나 selectable이고, [leading]에는 상태 semantics가 없어야 부모 줄의 상태와 겹쳐 읽히지 않는다. */
@Composable
internal fun SelectionCardRow(
    label: String,
    detail: String,
    labelMaxLines: Int,
    position: CardPosition,
    interaction: Modifier,
    leading: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    indent: Dp = 0.dp,
    showDivider: Boolean = false,
) {
    val isLargeText = isLargeText()
    val edgeInset = CardBorderWidth + CardEdgePadding

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = CaroTheme.spacing.xl2)
                .cardSegment(
                    position = position,
                    shape = CaroTheme.shape.m,
                    fill = CaroTheme.color.surface.primary,
                    line = CaroTheme.color.border.secondary,
                ).padding(
                    start = CardBorderWidth,
                    top = if (position.isFirst) edgeInset else 0.dp,
                    end = CardBorderWidth,
                    bottom = if (position.isLast) edgeInset else 0.dp,
                ),
    ) {
        if (showDivider) {
            Box(modifier = Modifier.fillMaxWidth().height(DividerThickness).background(CaroTheme.color.divider.secondary))
        }
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = RowMinHeight)
                    .then(interaction)
                    .padding(
                        start = RowHorizontalPadding + indent,
                        top = RowVerticalPadding,
                        end = RowHorizontalPadding,
                        bottom = RowVerticalPadding,
                    ),
            horizontalArrangement = Arrangement.spacedBy(CaroTheme.spacing.m),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            leading()
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(CaroTheme.spacing.xxs)) {
                Text(
                    text = label,
                    style = CaroTheme.typography.heading3,
                    color = CaroTheme.color.text.primary,
                    maxLines = labelMaxLines,
                    overflow = TextOverflow.Ellipsis,
                )
                if (isLargeText) SelectionRowDetail(detail)
            }
            if (!isLargeText) SelectionRowDetail(detail)
        }
    }
}

@Composable
private fun SelectionRowDetail(text: String) {
    Text(text = text, style = CaroTheme.typography.body2.medium, color = CaroTheme.color.text.secondary)
}

/**
 * LazyColumn 아이템은 하나의 테두리로 감쌀 수 없으므로, 아이템 경계 밖까지 늘린 둥근 사각형을 그린 뒤 아이템 영역만 잘라 이어 붙인다.
 * 늘린 변의 모서리는 잘려 나가고 첫 줄 위와 끝 줄 아래에만 가로 테두리가 남는다.
 */
private fun Modifier.cardSegment(
    position: CardPosition,
    shape: Shape,
    fill: Color,
    line: Color,
): Modifier =
    drawBehind {
        val border = CardBorderWidth.toPx()
        val extend = CardEdgeExtend.toPx()
        val top = if (position.isFirst) 0f else -extend
        val bottom = if (position.isLast) size.height else size.height + extend
        val innerTop = if (position.isFirst) top + border else top
        val innerBottom = if (position.isLast) bottom - border else bottom
        clipRect {
            translate(top = top) {
                drawOutline(shape.createOutline(Size(size.width, bottom - top), layoutDirection, this), line)
            }
            translate(left = border, top = innerTop) {
                drawOutline(shape.createOutline(Size(size.width - border * 2, innerBottom - innerTop), layoutDirection, this), fill)
            }
        }
    }

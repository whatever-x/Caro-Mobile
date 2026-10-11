package com.whatever.caro.feature.importer.step

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import caromobile.core.designsystem.generated.resources.Res
import caromobile.core.designsystem.generated.resources.ic_file_text_16
import caromobile.core.designsystem.generated.resources.import_label_header_row
import com.whatever.caro.core.designsystem.components.CaroCheckbox
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.feature.importer.component.ImportBottomBar
import com.whatever.caro.feature.importer.component.ImportStepTitle
import com.whatever.caro.feature.importer.logic.truncateFileName
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private val CardBorderWidth = 1.dp
private val CardEdgePadding = 4.dp
private val CardEdgeExtend = 24.dp
private val RowMinHeight = 50.dp
private val HeaderRowMinHeight = 48.dp
private val RowVerticalPadding = 14.dp
private val RowHorizontalPadding = 16.dp
private val FileIconSize = 16.dp
private val FileChipGap = 6.dp
private val DividerThickness = 1.dp
private const val LARGE_FONT_SCALE = 1.3f

/** 글자 크기 130% 이상에서 줄의 장수와 선택 요약을 아래로 내려 이름이 글자 단위로 끊기지 않게 한다. */
@Composable
internal fun isLargeText(): Boolean = LocalDensity.current.fontScale >= LARGE_FONT_SCALE

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

/**
 * 머리 블록도 [content]의 아이템으로 둬야 세로 스크롤 컨테이너가 하나다.
 * 내용이 고정 영역 위로 넘칠 때만 구분선을 보이고, 높이는 항상 차지해 선이 생겨도 목록이 밀리지 않는다.
 */
@Composable
internal fun SelectionStepFrame(
    buttonText: String,
    onNext: () -> Unit,
    enabled: Boolean,
    reason: String?,
    modifier: Modifier = Modifier,
    summary: (@Composable () -> Unit)? = null,
    content: LazyListScope.() -> Unit,
) {
    val listState = rememberLazyListState()
    val isScrollable = listState.canScrollForward || listState.canScrollBackward

    Column(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            state = listState,
            content = content,
        )
        StepBottomDivider(isVisible = isScrollable)
        ImportBottomBar(
            buttonText = buttonText,
            onClick = onNext,
            enabled = enabled,
            reason = reason,
            summary = summary,
        )
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

/** 줄 전체를 누르는 영역으로 두고 눌리는 높이는 48 이상이다. 보이는 체크박스는 22라 줄 높이와 별개다. */
@Composable
internal fun HeaderRowToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = HeaderRowMinHeight)
                .toggleable(value = checked, role = Role.Checkbox, onValueChange = onCheckedChange)
                .padding(horizontal = CaroTheme.spacing.xl2),
        horizontalArrangement = Arrangement.spacedBy(CaroTheme.spacing.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CaroCheckbox(state = if (checked) ToggleableState.On else ToggleableState.Off)
        Text(
            text = stringResource(Res.string.import_label_header_row),
            style = CaroTheme.typography.label1,
            color = CaroTheme.color.text.primary,
        )
    }
}

/** 제목은 [ImportStepTitle]이어야 읽기가 끝난 뒤 제목으로 포커스가 간다. */
@Composable
internal fun SelectionHeader(
    fileName: String,
    title: String,
    hint: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(
                    start = CaroTheme.spacing.xl2,
                    top = CaroTheme.spacing.xl,
                    end = CaroTheme.spacing.xl2,
                    bottom = CaroTheme.spacing.xl2,
                ),
        verticalArrangement = Arrangement.spacedBy(CaroTheme.spacing.s),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(FileChipGap),
        ) {
            Icon(
                modifier = Modifier.size(FileIconSize),
                painter = painterResource(Res.drawable.ic_file_text_16),
                contentDescription = null,
                tint = CaroTheme.color.icon.primary,
            )
            Text(
                text = truncateFileName(fileName),
                style = CaroTheme.typography.body2.semiBold,
                color = CaroTheme.color.text.secondary,
            )
        }
        ImportStepTitle(text = title)
        Text(
            text = hint,
            style = CaroTheme.typography.label1,
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
    caption: String? = null,
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
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(DividerThickness)
                        .background(CaroTheme.color.divider.secondary),
            )
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
                if (caption != null) {
                    Text(
                        text = caption,
                        style = CaroTheme.typography.caption1.regular,
                        color = CaroTheme.color.text.secondary,
                    )
                }
                if (isLargeText) SelectionRowDetail(detail)
            }
            if (!isLargeText) SelectionRowDetail(detail)
        }
    }
}

@Composable
private fun SelectionRowDetail(text: String) {
    Text(
        text = text,
        style = CaroTheme.typography.label1,
        color = CaroTheme.color.text.secondary,
    )
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
                drawOutline(
                    shape.createOutline(Size(size.width - border * 2, innerBottom - innerTop), layoutDirection, this),
                    fill,
                )
            }
        }
    }

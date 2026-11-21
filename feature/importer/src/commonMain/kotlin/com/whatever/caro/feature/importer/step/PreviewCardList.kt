package com.whatever.caro.feature.importer.step

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.offset
import caromobile.core.designsystem.generated.resources.Res
import caromobile.core.designsystem.generated.resources.import_chip_filter_all
import caromobile.core.designsystem.generated.resources.import_chip_filter_empty_filled
import caromobile.core.designsystem.generated.resources.import_chip_filter_media_removed
import caromobile.core.designsystem.generated.resources.import_chip_filter_truncated
import caromobile.core.designsystem.generated.resources.import_hint_filter_empty_side
import caromobile.core.designsystem.generated.resources.import_hint_filter_media_removed
import caromobile.core.designsystem.generated.resources.import_hint_filter_truncated
import caromobile.core.designsystem.generated.resources.import_hint_preview_edit_later
import caromobile.core.designsystem.generated.resources.import_label_change_media_removed
import caromobile.core.designsystem.generated.resources.import_label_change_truncated_back
import caromobile.core.designsystem.generated.resources.import_label_change_truncated_front
import caromobile.core.designsystem.generated.resources.import_title_card_list
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.core.model.card.CardInputLimits
import com.whatever.caro.core.util.NumberFormatter.formatWithComma
import com.whatever.caro.feature.importer.components.ImportFieldChip
import com.whatever.caro.feature.importer.components.ImportInfoTag
import com.whatever.caro.feature.importer.logic.CardFlag
import com.whatever.caro.feature.importer.logic.cardFlags
import com.whatever.caro.feature.importer.mvi.ImportPreviewFilter
import com.whatever.caro.feature.importer.transform.ImportCard
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

private const val FRONT_MAX_LINES = 2
private const val BACK_MAX_LINES = 3
private val BorderWidth = 1.dp
private val TypeNameMaxWidth = 125.dp

// 보이는 칩 높이가 24dp 이상이면 칩 둘레 48dp가 이만큼 넓힌 칩 줄 안에 든다.
private val ChipRowTouchExtension = 12.dp

/**
 * 머리는 위에 닿으면 붙어 남으므로 바탕을 칠해 아래로 지나가는 카드를 가린다.
 * 칩은 줄바꿈하지 않고 칩 줄만 가로로 밀어, 큰 글자에서도 붙은 머리가 화면을 덮지 않게 한다.
 */
@Composable
internal fun PreviewListHeader(
    filters: ImmutableList<ImportPreviewFilter>,
    counts: ImmutableMap<ImportPreviewFilter, Int>,
    selected: ImportPreviewFilter,
    mediaRemovedCount: Int,
    onSelect: (ImportPreviewFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    val hint = selected.hint(mediaRemovedCount)

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .background(CaroTheme.color.background.primary)
                .padding(bottom = CaroTheme.spacing.s),
        verticalArrangement = Arrangement.spacedBy(CaroTheme.spacing.s),
    ) {
        ListTitleRow()
        FilterChipRow(filters = filters, counts = counts, selected = selected, onSelect = onSelect)
        if (hint != null) {
            Text(
                text = hint,
                modifier = Modifier.padding(horizontal = CaroTheme.spacing.xl2),
                style = CaroTheme.typography.caption1.regular,
                color = CaroTheme.color.text.secondary,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ListTitleRow() {
    FlowRow(
        modifier = Modifier.fillMaxWidth().padding(horizontal = CaroTheme.spacing.xl2),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalArrangement = Arrangement.spacedBy(CaroTheme.spacing.xs),
        itemVerticalAlignment = Alignment.Bottom,
    ) {
        Text(
            text = stringResource(Res.string.import_title_card_list),
            modifier = Modifier.semantics { heading() },
            style = CaroTheme.typography.body2.semiBold,
            color = CaroTheme.color.text.primary,
        )
        Text(
            text = stringResource(Res.string.import_hint_preview_edit_later),
            style = CaroTheme.typography.caption1.regular,
            color = CaroTheme.color.text.secondary,
        )
    }
}

/**
 * 안쪽 여백을 스크롤 안에 두어 밀린 칩이 화면 오른쪽 끝까지 보인다.
 * 칩은 둘레 48dp까지 누름을 받지만 그 자리가 칩 줄 밖이면, 머리가 붙었을 때 아래로 지나가는 카드가 누름을 가져간다.
 * 그래서 칩 줄의 누르는 영역을 위아래로 넓히고, 넓힌 만큼 겹쳐 놓아 줄 높이는 보이는 칩 높이로 둔다.
 */
@Composable
private fun FilterChipRow(
    filters: ImmutableList<ImportPreviewFilter>,
    counts: ImmutableMap<ImportPreviewFilter, Int>,
    selected: ImportPreviewFilter,
    onSelect: (ImportPreviewFilter) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .overlapVertically(ChipRowTouchExtension)
                .horizontalScroll(rememberScrollState())
                .selectableGroup()
                .padding(horizontal = CaroTheme.spacing.xl2, vertical = ChipRowTouchExtension),
        horizontalArrangement = Arrangement.spacedBy(CaroTheme.spacing.s),
    ) {
        filters.forEach { filter ->
            ImportFieldChip(
                text = stringResource(filter.chipLabel()),
                count = (counts[filter] ?: 0).formatWithComma(),
                isSelected = filter == selected,
                onClick = { onSelect(filter) },
            )
        }
    }
}

/** 위아래로 [extension]만큼 더 큰 자리에 그리고, 부모에게는 그만큼 뺀 높이를 알린다. */
internal fun Modifier.overlapVertically(extension: Dp): Modifier =
    layout { measurable, constraints ->
        val extensionPx = extension.roundToPx()
        val placeable = measurable.measure(constraints.offset(vertical = extensionPx * 2))
        layout(placeable.width, placeable.height - extensionPx * 2) { placeable.place(0, -extensionPx) }
    }

/** 좌우로 [extension]만큼 더 큰 자리에 그리고, 부모에게는 그만큼 뺀 폭을 알린다. */
internal fun Modifier.overlapHorizontally(extension: Dp): Modifier =
    layout { measurable, constraints ->
        val extensionPx = extension.roundToPx()
        val placeable = measurable.measure(constraints.offset(horizontal = extensionPx * 2))
        layout(placeable.width - extensionPx * 2, placeable.height) { placeable.place(-extensionPx, 0) }
    }

/** [FilterChipRow]와 같은 이유로 칩 둘레 48dp가 칩 줄 안에 들게 한다. 가로 스크롤은 넓힌 자리를 잘라 내므로 가로로 미는 칩 줄에는 쓰지 않는다. */
internal fun Modifier.chipRowTouchArea(): Modifier = overlapVertically(ChipRowTouchExtension).padding(vertical = ChipRowTouchExtension)

/** 미디어 설명의 수는 칩의 카드 수가 아니라 지운 미디어 참조 수다. */
@Composable
private fun ImportPreviewFilter.hint(mediaRemovedCount: Int): String? =
    when (this) {
        ImportPreviewFilter.All -> {
            null
        }

        ImportPreviewFilter.EmptyReplaced -> {
            stringResource(Res.string.import_hint_filter_empty_side)
        }

        ImportPreviewFilter.Truncated -> {
            stringResource(
                Res.string.import_hint_filter_truncated,
                CardInputLimits.FIELD_MAX.formatWithComma(),
            )
        }

        ImportPreviewFilter.MediaRemoved -> {
            stringResource(Res.string.import_hint_filter_media_removed, mediaRemovedCount.formatWithComma())
        }
    }

/**
 * 줄임은 표시용이고 저장할 때의 글자 수 자르기와 별개다.
 * clickable의 focus target은 터치 모드에서 포커스 요청을 받지 않아, 전체 보기를 닫을 때 포커스를 돌려받도록 canFocus를 켠다.
 * focusable을 앞에 따로 두면 focusRequester가 그쪽을 잡아 Enter가 clickable에 닿지 않고, 키보드 탐색에 포커스 대상이 하나 더 생긴다.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun PreviewCardItem(
    card: ImportCard,
    typeName: String?,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val flags = remember(card) { cardFlags(card) }
    val interactionSource = remember { MutableInteractionSource() }

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(CaroTheme.shape.m)
                .background(CaroTheme.color.surface.primary)
                .border(BorderWidth, CaroTheme.color.border.secondary, CaroTheme.shape.m)
                .then(
                    if (onClick == null) {
                        Modifier
                    } else {
                        Modifier
                            .focusProperties { canFocus = true }
                            .clickable(interactionSource = interactionSource, indication = ripple(), role = Role.Button, onClick = onClick)
                    },
                ).padding(BorderWidth)
                .padding(CaroTheme.spacing.xl),
        verticalArrangement = Arrangement.spacedBy(CaroTheme.spacing.m),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(CaroTheme.spacing.xxs)) {
            CardFrontLine(front = card.front, typeName = typeName)
            Text(
                text = card.back,
                style = CaroTheme.typography.label2,
                color = CaroTheme.color.text.secondary,
                maxLines = BACK_MAX_LINES,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (flags.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(CaroTheme.spacing.xs),
                verticalArrangement = Arrangement.spacedBy(CaroTheme.spacing.xs),
            ) {
                flags.forEach { flag -> ImportInfoTag(text = stringResource(flag.label())) }
            }
        }
    }
}

/** Figma의 유형 이름 색(gray/400)과 같은 값의 글자 색 토큰이 없어 아이콘 색 토큰을 쓴다. */
@Composable
private fun CardFrontLine(
    front: String,
    typeName: String?,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(CaroTheme.spacing.s)) {
        Text(
            text = front,
            modifier = Modifier.weight(1f).alignByBaseline(),
            style = CaroTheme.typography.heading2,
            color = CaroTheme.color.text.primary,
            maxLines = FRONT_MAX_LINES,
            overflow = TextOverflow.Ellipsis,
        )
        if (typeName != null) {
            Text(
                text = typeName,
                modifier = Modifier.widthIn(max = TypeNameMaxWidth).alignByBaseline(),
                style = CaroTheme.typography.caption1.regular,
                color = CaroTheme.color.icon.quaternary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

internal fun ImportPreviewFilter.chipLabel(): StringResource =
    when (this) {
        ImportPreviewFilter.All -> Res.string.import_chip_filter_all
        ImportPreviewFilter.EmptyReplaced -> Res.string.import_chip_filter_empty_filled
        ImportPreviewFilter.Truncated -> Res.string.import_chip_filter_truncated
        ImportPreviewFilter.MediaRemoved -> Res.string.import_chip_filter_media_removed
    }

private fun CardFlag.label(): StringResource =
    when (this) {
        CardFlag.TruncatedFront -> Res.string.import_label_change_truncated_front
        CardFlag.TruncatedBack -> Res.string.import_label_change_truncated_back
        CardFlag.MediaRemoved -> Res.string.import_label_change_media_removed
    }

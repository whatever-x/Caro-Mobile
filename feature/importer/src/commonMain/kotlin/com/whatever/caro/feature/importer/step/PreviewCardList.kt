package com.whatever.caro.feature.importer.step

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import caromobile.core.designsystem.generated.resources.Res
import caromobile.core.designsystem.generated.resources.import_chip_filter_all
import caromobile.core.designsystem.generated.resources.import_chip_filter_empty_filled
import caromobile.core.designsystem.generated.resources.import_chip_filter_media_removed
import caromobile.core.designsystem.generated.resources.import_chip_filter_truncated
import caromobile.core.designsystem.generated.resources.import_hint_preview_empty_list
import caromobile.core.designsystem.generated.resources.import_label_change_empty_back
import caromobile.core.designsystem.generated.resources.import_label_change_empty_front
import caromobile.core.designsystem.generated.resources.import_label_change_media_removed
import caromobile.core.designsystem.generated.resources.import_label_change_truncated_back
import caromobile.core.designsystem.generated.resources.import_label_change_truncated_both
import caromobile.core.designsystem.generated.resources.import_label_change_truncated_front
import caromobile.core.designsystem.generated.resources.import_title_card_list
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.feature.importer.logic.ChangeBadge
import com.whatever.caro.feature.importer.logic.changeBadges
import com.whatever.caro.feature.importer.mvi.ImportPreviewFilter
import com.whatever.caro.feature.importer.transform.ImportCard
import kotlinx.collections.immutable.ImmutableList
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

private val BorderWidth = 1.dp
private val ChipHorizontalPadding = 12.dp
private val ChipVerticalPadding = 6.dp
private const val FRONT_MAX_LINES = 2
private const val BACK_MAX_LINES = 3

/**
 * 머리 높이는 고정되기 전과 후가 같아야 목록이 튀지 않으므로 구분선은 항상 높이를 차지하고, 고정되었을 때만 색이 나타난다.
 * 칩은 넘치면 칩 줄만 가로로 스크롤하고, 칩을 빼거나 줄바꿈하지 않아 큰 글자에서도 머리 높이가 같다.
 */
@Composable
internal fun PreviewListHeader(
    filters: ImmutableList<ImportPreviewFilter>,
    selected: ImportPreviewFilter,
    isPinned: Boolean,
    onSelect: (ImportPreviewFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth().background(CaroTheme.color.background.primary)) {
        Text(
            text = stringResource(Res.string.import_title_card_list),
            modifier =
                Modifier
                    .padding(horizontal = CaroTheme.spacing.xl2)
                    .padding(top = CaroTheme.spacing.m)
                    .semantics { heading() },
            style = CaroTheme.typography.body2.semiBold,
            color = CaroTheme.color.text.primary,
        )
        Row(
            modifier =
                Modifier
                    .padding(top = CaroTheme.spacing.m)
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .selectableGroup()
                    .padding(horizontal = CaroTheme.spacing.xl2),
            horizontalArrangement = Arrangement.spacedBy(CaroTheme.spacing.s),
        ) {
            filters.forEach { filter ->
                FilterChip(
                    text = stringResource(filter.chipLabel()),
                    isSelected = filter == selected,
                    onClick = { onSelect(filter) },
                )
            }
        }
        Spacer(modifier = Modifier.height(HeaderDividerGap))
        StepBottomDivider(isVisible = isPinned)
    }
}

/** 선택해도 테두리 두께가 같아 칩 높이와 글자 기준선이 흔들리지 않는다. */
@Composable
private fun FilterChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val shape = CaroTheme.shape.xxl
    Text(
        text = text,
        modifier =
            Modifier
                .clip(shape)
                .background(if (isSelected) CaroTheme.color.surface.brand else CaroTheme.color.surface.primary)
                .border(BorderWidth, if (isSelected) CaroTheme.color.border.brand else CaroTheme.color.border.secondary, shape)
                .selectable(selected = isSelected, role = Role.Tab, onClick = onClick)
                .padding(horizontal = ChipHorizontalPadding, vertical = ChipVerticalPadding),
        style = CaroTheme.typography.label1,
        color = if (isSelected) CaroTheme.color.text.inverse else CaroTheme.color.text.secondary,
    )
}

/**
 * 덱 상세 카드 값에 줄 수만 앞면 2줄, 뒷면 3줄로 늘렸다. 줄임은 표시용이고 500자 잘림과 별개다.
 * clickable만으로는 터치 모드에서 포커스 요청을 받지 않으므로 전체 보기를 닫을 때 돌아올 수 있게 focusable을 따로 둔다.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun PreviewCardItem(
    card: ImportCard,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val badges = remember(card) { changeBadges(card) }
    Column(
        modifier =
            modifier
                .padding(horizontal = CaroTheme.spacing.xl2)
                .padding(top = CaroTheme.spacing.m)
                .fillMaxWidth()
                .background(CaroTheme.color.surface.primary, CaroTheme.shape.m)
                .border(BorderWidth, CaroTheme.color.border.secondary, CaroTheme.shape.m)
                .clip(CaroTheme.shape.m)
                .focusable()
                .clickable(role = Role.Button, onClick = onClick)
                .padding(CaroTheme.spacing.xl),
    ) {
        Text(
            text = card.front,
            style = CaroTheme.typography.heading2,
            color = CaroTheme.color.text.primary,
            maxLines = FRONT_MAX_LINES,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(CaroTheme.spacing.xxs))
        Text(
            text = card.back,
            style = CaroTheme.typography.label2,
            color = CaroTheme.color.text.secondary,
            maxLines = BACK_MAX_LINES,
            overflow = TextOverflow.Ellipsis,
        )
        if (badges.isNotEmpty()) {
            FlowRow(
                modifier = Modifier.padding(top = CaroTheme.spacing.m),
                horizontalArrangement = Arrangement.spacedBy(CaroTheme.spacing.s),
                verticalArrangement = Arrangement.spacedBy(CaroTheme.spacing.s),
            ) {
                badges.forEach { badge -> ChangeBadgeLabel(badge) }
            }
        }
    }
}

/** 색이 아니라 문구로 구분하므로 모든 배지가 같은 중립색이다. */
@Composable
private fun ChangeBadgeLabel(badge: ChangeBadge) {
    Text(
        text = stringResource(badge.label()),
        modifier =
            Modifier
                .background(CaroTheme.color.surface.secondary, CaroTheme.shape.xxl)
                .padding(horizontal = CaroTheme.spacing.s, vertical = CaroTheme.spacing.xs),
        style = CaroTheme.typography.caption1.regular,
        color = CaroTheme.color.text.secondary,
    )
}

@Composable
internal fun PreviewEmptyList(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(Res.string.import_hint_preview_empty_list),
        modifier = modifier.fillMaxWidth().padding(horizontal = CaroTheme.spacing.xl2).padding(top = CaroTheme.spacing.m),
        style = CaroTheme.typography.label1,
        color = CaroTheme.color.text.secondary,
    )
}

internal fun ImportPreviewFilter.chipLabel(): StringResource =
    when (this) {
        ImportPreviewFilter.All -> Res.string.import_chip_filter_all
        ImportPreviewFilter.EmptyReplaced -> Res.string.import_chip_filter_empty_filled
        ImportPreviewFilter.Truncated -> Res.string.import_chip_filter_truncated
        ImportPreviewFilter.MediaRemoved -> Res.string.import_chip_filter_media_removed
    }

private fun ChangeBadge.label(): StringResource =
    when (this) {
        ChangeBadge.EmptyFront -> Res.string.import_label_change_empty_front
        ChangeBadge.EmptyBack -> Res.string.import_label_change_empty_back
        ChangeBadge.TruncatedFront -> Res.string.import_label_change_truncated_front
        ChangeBadge.TruncatedBack -> Res.string.import_label_change_truncated_back
        ChangeBadge.TruncatedBoth -> Res.string.import_label_change_truncated_both
        ChangeBadge.MediaRemoved -> Res.string.import_label_change_media_removed
    }

private val HeaderDividerGap = 11.dp

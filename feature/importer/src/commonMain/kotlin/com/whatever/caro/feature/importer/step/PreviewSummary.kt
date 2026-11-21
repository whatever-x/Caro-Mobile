package com.whatever.caro.feature.importer.step

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import caromobile.core.designsystem.generated.resources.Res
import caromobile.core.designsystem.generated.resources.ic_file_text_16
import caromobile.core.designsystem.generated.resources.ic_x_16
import caromobile.core.designsystem.generated.resources.import_banner_delimiter_check
import caromobile.core.designsystem.generated.resources.import_button_change_delimiter
import caromobile.core.designsystem.generated.resources.import_content_description_cards_to_import
import caromobile.core.designsystem.generated.resources.import_content_description_dismiss_banner
import caromobile.core.designsystem.generated.resources.import_label_card_count
import caromobile.core.designsystem.generated.resources.import_summary_importable_label
import caromobile.core.designsystem.generated.resources.import_value_paste_source
import caromobile.core.designsystem.generated.resources.import_warning_apkg_field_mismatch
import caromobile.core.designsystem.generated.resources.import_warning_encoding_cp949
import caromobile.core.designsystem.generated.resources.import_warning_xlsx_formula
import caromobile.core.designsystem.generated.resources.import_warning_xlsx_number_format
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.core.importparser.model.ParsedTable
import com.whatever.caro.core.util.NumberFormatter.formatWithComma
import com.whatever.caro.feature.importer.components.ImportStepTitle
import com.whatever.caro.feature.importer.logic.PreviewWarning
import com.whatever.caro.feature.importer.logic.PreviewWarningKind
import com.whatever.caro.feature.importer.logic.previewSettingRows
import com.whatever.caro.feature.importer.logic.previewWarnings
import com.whatever.caro.feature.importer.logic.truncateFileName
import com.whatever.caro.feature.importer.mvi.ImportDeckState
import com.whatever.caro.feature.importer.mvi.SettingsKind
import com.whatever.caro.feature.importer.transform.ImportPreview
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private const val BANNER_BACKGROUND_ALPHA = 0.16f
private const val BANNER_BORDER_ALPHA = 0.6f
private val BorderWidth = 1.dp
private val SmallIconSize = 16.dp
private val SourceIconGap = 6.dp
private val CloseTouchSize = 48.dp
private val CloseRippleRadius = 24.dp

// 닫기 아이콘(16)은 띠 안쪽 오른쪽 위에서 12 떨어져 있고, 글은 아이콘 앞 8에서 끝난다.
private val BannerTextEndPadding = 24.dp
private val CloseTouchOffset = 4.dp
private val BannerButtonVerticalPadding = 6.dp

// 보이는 버튼 높이가 24 이상이면 위아래로 이만큼 넓혀 누르는 높이가 48 이상이 된다. 아래로 넓힌 만큼은 띠 안쪽 여백 안에 든다.
private val BannerButtonTouchExtension = 12.dp

/** 목록 머리 앞의 요약은 아이템 하나로 묶는다. 띠와 경고 줄 유무에 따라 머리의 아이템 위치가 바뀌지 않아야 필터를 바꿀 때 머리 자리로 옮겨 간다. */
@Composable
internal fun PreviewSummary(
    state: ImportDeckState,
    table: ParsedTable,
    preview: ImportPreview,
    onDismissBanner: () -> Unit,
    onChangeSetting: (SettingsKind) -> Unit,
    onChangeDeckInfo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val settingRows =
        remember(table, state.selection, state.mappings, preview) {
            previewSettingRows(table, state.selection, state.mappings, preview).toImmutableList()
        }
    val warnings = remember(preview) { previewWarnings(preview.warnings).toImmutableList() }
    val bannerRowCount = state.delimiterBannerRowCount

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(CaroTheme.spacing.m)) {
        PreviewSource(fileName = if (state.isPaste) null else truncateFileName(table.sourceName), cardCount = preview.stats.cardCount)
        if (bannerRowCount != null) {
            DelimiterCheckBanner(
                rowCount = bannerRowCount,
                onDismiss = onDismissBanner,
                onChangeDelimiter = { onChangeSetting(SettingsKind.Delimiter) },
            )
        }
        PreviewSettingsBox(deckName = state.deckName, rows = settingRows, onChange = onChangeSetting, onChangeDeckInfo = onChangeDeckInfo)
        if (warnings.isNotEmpty()) PreviewWarningLines(warnings = warnings)
    }
}

/** [fileName]이 null이면 붙여넣기라 파일 아이콘 없이 붙여넣기 문구를 보인다. */
@Composable
private fun PreviewSource(
    fileName: String?,
    cardCount: Int,
) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(CaroTheme.spacing.s)) {
        Row(horizontalArrangement = Arrangement.spacedBy(SourceIconGap), verticalAlignment = Alignment.CenterVertically) {
            if (fileName != null) {
                Icon(
                    modifier = Modifier.size(SmallIconSize),
                    painter = painterResource(Res.drawable.ic_file_text_16),
                    contentDescription = null,
                    tint = CaroTheme.color.icon.primary,
                )
            }
            Text(
                text = fileName ?: stringResource(Res.string.import_value_paste_source),
                style = CaroTheme.typography.body2.semiBold,
                color = CaroTheme.color.text.secondary,
            )
        }
        CardCountTitle(cardCount = cardCount)
    }
}

/**
 * 로딩 오버레이가 걷히면 포커스가 이 제목으로 온다. 라벨은 제목 문장에 함께 읽히므로 따로 읽지 않는다.
 * 라벨과 장수의 어순은 언어마다 달라, 둘을 이어 붙이지 않고 장수 하나를 받는 접근성 문구로 읽는다.
 */
@Composable
private fun CardCountTitle(cardCount: Int) {
    val countText = cardCount.formatWithComma()

    Row(horizontalArrangement = Arrangement.spacedBy(CaroTheme.spacing.s)) {
        Text(
            text = stringResource(Res.string.import_summary_importable_label),
            modifier = Modifier.alignByBaseline().clearAndSetSemantics {},
            style = CaroTheme.typography.label1,
            color = CaroTheme.color.text.secondary,
        )
        ImportStepTitle(
            text = stringResource(Res.string.import_label_card_count, countText),
            modifier = Modifier.alignByBaseline(),
            style = CaroTheme.typography.display,
            readAs = stringResource(Res.string.import_content_description_cards_to_import, countText),
        )
    }
}

@Composable
private fun DelimiterCheckBanner(
    rowCount: Int,
    onDismiss: () -> Unit,
    onChangeDelimiter: () -> Unit,
) {
    Box(modifier = Modifier.warningBannerSurface()) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        start = CaroTheme.spacing.l,
                        top = CaroTheme.spacing.m,
                        end = CaroTheme.spacing.m,
                        bottom = CaroTheme.spacing.m,
                    ),
            verticalArrangement = Arrangement.spacedBy(CaroTheme.spacing.s),
        ) {
            Text(
                text = stringResource(Res.string.import_banner_delimiter_check, rowCount.formatWithComma()),
                modifier = Modifier.fillMaxWidth().padding(end = BannerTextEndPadding),
                style = CaroTheme.typography.caption1.regular,
                color = CaroTheme.color.text.primary,
            )
            ChangeDelimiterButton(onClick = onChangeDelimiter)
        }
        BannerCloseButton(onDismiss = onDismiss, modifier = Modifier.align(Alignment.TopEnd))
    }
}

/** 미리보기의 설정 확인 띠와 먼저 고칠 것 화면의 띠가 같은 바탕과 테두리를 써야 한다. */
@Composable
internal fun Modifier.warningBannerSurface(): Modifier {
    val warning = CaroTheme.color.text.warning
    val shape = CaroTheme.shape.s
    return fillMaxWidth()
        .background(warning.copy(alpha = BANNER_BACKGROUND_ALPHA), shape)
        .border(BorderWidth, warning.copy(alpha = BANNER_BORDER_ALPHA), shape)
        .padding(BorderWidth)
}

/**
 * 누르는 영역은 위아래로 넓혀 48 높이를 맞추고 넓힌 만큼 겹쳐 놓아 띠 높이는 Figma대로 둔다.
 * 눌림 표시는 넓힌 영역이 아니라 보이는 모양에 그려야 하므로 같은 소스를 모양의 indication으로 넘긴다.
 */
@Composable
private fun ChangeDelimiterButton(onClick: () -> Unit) {
    val shape = CaroTheme.shape.xxl
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier =
            Modifier
                .overlapVertically(BannerButtonTouchExtension)
                .clickable(interactionSource = interactionSource, indication = null, role = Role.Button, onClick = onClick)
                .padding(vertical = BannerButtonTouchExtension),
    ) {
        Text(
            text = stringResource(Res.string.import_button_change_delimiter),
            modifier =
                Modifier
                    .clip(shape)
                    .background(CaroTheme.color.surface.primary)
                    .border(BorderWidth, CaroTheme.color.border.secondary, shape)
                    .indication(interactionSource, ripple())
                    .padding(horizontal = CaroTheme.spacing.m, vertical = BannerButtonVerticalPadding),
            style = CaroTheme.typography.body2.semiBold,
            color = CaroTheme.color.text.primary,
        )
    }
}

/** 누르는 영역 48을 글 줄에 넣으면 띠가 Figma보다 높아지므로 아이콘 위에 겹쳐 둔다. */
@Composable
private fun BannerCloseButton(
    onDismiss: () -> Unit,
    modifier: Modifier,
) {
    Box(
        modifier =
            modifier
                .offset(x = CloseTouchOffset, y = -CloseTouchOffset)
                .size(CloseTouchSize)
                .clickable(
                    role = Role.Button,
                    indication = ripple(bounded = false, radius = CloseRippleRadius),
                    interactionSource = null,
                    onClick = onDismiss,
                ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            modifier = Modifier.size(SmallIconSize),
            painter = painterResource(Res.drawable.ic_x_16),
            contentDescription = stringResource(Res.string.import_content_description_dismiss_banner),
            tint = CaroTheme.color.icon.primary,
        )
    }
}

@Composable
private fun PreviewWarningLines(warnings: ImmutableList<PreviewWarning>) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(CaroTheme.spacing.xs)) {
        warnings.forEach { warning ->
            Text(
                text = warning.message(),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(CaroTheme.color.surface.secondary, CaroTheme.shape.s)
                        .padding(horizontal = CaroTheme.spacing.l, vertical = CaroTheme.spacing.m),
                style = CaroTheme.typography.caption1.regular,
                color = CaroTheme.color.text.secondary,
            )
        }
    }
}

@Composable
private fun PreviewWarning.message(): String {
    val countText = count.formatWithComma()
    return when (kind) {
        PreviewWarningKind.EncodingFallback -> stringResource(Res.string.import_warning_encoding_cp949)
        PreviewWarningKind.FormulaWithoutValue -> stringResource(Res.string.import_warning_xlsx_formula, countText)
        PreviewWarningKind.NumberFormatRaw -> stringResource(Res.string.import_warning_xlsx_number_format)
        PreviewWarningKind.FieldCountMismatch -> stringResource(Res.string.import_warning_apkg_field_mismatch, countText)
    }
}

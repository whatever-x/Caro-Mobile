package com.whatever.caro.feature.importer.step

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import caromobile.core.designsystem.generated.resources.Res
import caromobile.core.designsystem.generated.resources.import_hint_preview_edit_later
import caromobile.core.designsystem.generated.resources.import_label_card_count
import caromobile.core.designsystem.generated.resources.import_label_item_count
import caromobile.core.designsystem.generated.resources.import_label_stat_empty_filled
import caromobile.core.designsystem.generated.resources.import_label_stat_excluded
import caromobile.core.designsystem.generated.resources.import_label_stat_media_removed
import caromobile.core.designsystem.generated.resources.import_label_stat_truncated
import caromobile.core.designsystem.generated.resources.import_summary_importable_label
import caromobile.core.designsystem.generated.resources.import_summary_importable_over_limit
import caromobile.core.designsystem.generated.resources.import_warning_apkg_field_mismatch
import caromobile.core.designsystem.generated.resources.import_warning_apkg_image_occlusion
import caromobile.core.designsystem.generated.resources.import_warning_encoding_cp949
import caromobile.core.designsystem.generated.resources.import_warning_inconsistent_columns
import caromobile.core.designsystem.generated.resources.import_warning_xlsx_formula
import caromobile.core.designsystem.generated.resources.import_warning_xlsx_number_format
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.core.util.NumberFormatter.formatWithComma
import com.whatever.caro.feature.importer.component.ImportStepTitle
import com.whatever.caro.feature.importer.logic.PreviewWarning
import com.whatever.caro.feature.importer.logic.PreviewWarningKind
import com.whatever.caro.feature.importer.transform.ImportPreview
import kotlinx.collections.immutable.ImmutableList
import org.jetbrains.compose.resources.stringResource

private val SummaryBorderWidth = 1.dp
private val DividerThickness = 1.dp

/**
 * 장수 제목은 [ImportStepTitle]이어야 미리보기를 만든 뒤 이 제목으로 포커스가 온다. 포커스가 온 제목만 읽혀도 무엇의 장수인지 알도록 라벨을 붙여 읽는다.
 * 상한을 넘으면 읽은 만큼 센 장수 대신 상한을 보여주고, 아래 수치는 읽은 행까지만 센 값 그대로다.
 */
@Composable
internal fun PreviewSummary(
    preview: ImportPreview,
    maxCards: Int,
    modifier: Modifier = Modifier,
) {
    val stats = preview.stats
    val label = stringResource(Res.string.import_summary_importable_label)
    val countText =
        if (preview.limitExceeded) {
            stringResource(Res.string.import_summary_importable_over_limit, maxCards.formatWithComma())
        } else {
            stringResource(Res.string.import_label_card_count, stats.cardCount.formatWithComma())
        }

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(start = CaroTheme.spacing.xl2, top = CaroTheme.spacing.xl, end = CaroTheme.spacing.xl2)
                .background(CaroTheme.color.surface.primary, CaroTheme.shape.m)
                .border(SummaryBorderWidth, CaroTheme.color.border.secondary, CaroTheme.shape.m)
                .padding(SummaryBorderWidth)
                .padding(CaroTheme.spacing.xl),
    ) {
        Text(
            text = label,
            style = CaroTheme.typography.label1,
            color = CaroTheme.color.text.secondary,
        )
        Spacer(modifier = Modifier.height(CaroTheme.spacing.xs))
        ImportStepTitle(text = countText, style = CaroTheme.typography.display, readAs = "$label $countText")
        Spacer(modifier = Modifier.height(CaroTheme.spacing.l))
        Spacer(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(DividerThickness)
                    .background(CaroTheme.color.divider.secondary),
        )
        Spacer(modifier = Modifier.height(CaroTheme.spacing.l))
        Column(verticalArrangement = Arrangement.spacedBy(CaroTheme.spacing.s)) {
            val cardCount = Res.string.import_label_card_count
            StatRow(stringResource(Res.string.import_label_stat_excluded), stringResource(cardCount, stats.excludedCount.formatWithComma()))
            StatRow(
                stringResource(Res.string.import_label_stat_empty_filled),
                stringResource(cardCount, stats.emptyReplacedCount.formatWithComma()),
            )
            StatRow(
                stringResource(Res.string.import_label_stat_truncated),
                stringResource(cardCount, stats.truncatedCount.formatWithComma()),
            )
            StatRow(
                stringResource(Res.string.import_label_stat_media_removed),
                stringResource(Res.string.import_label_item_count, stats.mediaRemovedCount.formatWithComma()),
            )
        }
    }
}

/** 이름과 수치를 한 문장으로 읽게 병합한다. */
@Composable
private fun StatRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.spacedBy(CaroTheme.spacing.m),
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = CaroTheme.typography.label1,
            color = CaroTheme.color.text.primary,
        )
        Text(
            text = value,
            style = CaroTheme.typography.body2.semiBold,
            color = CaroTheme.color.text.primary,
        )
    }
}

@Composable
internal fun PreviewWarningLines(
    warnings: ImmutableList<PreviewWarning>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = CaroTheme.spacing.xl2).padding(top = CaroTheme.spacing.l),
        verticalArrangement = Arrangement.spacedBy(CaroTheme.spacing.xs),
    ) {
        warnings.forEach { warning ->
            Text(
                text = warning.message(),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(CaroTheme.color.surface.secondary, CaroTheme.shape.s)
                        .padding(horizontal = CaroTheme.spacing.l, vertical = CaroTheme.spacing.m),
                style = CaroTheme.typography.caption1.regular,
                color = CaroTheme.color.text.primary,
            )
        }
    }
}

@Composable
internal fun PreviewEditLaterHint(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(Res.string.import_hint_preview_edit_later),
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = CaroTheme.spacing.xl2)
                .padding(top = CaroTheme.spacing.l, bottom = CaroTheme.spacing.xl2),
        style = CaroTheme.typography.caption1.regular,
        color = CaroTheme.color.text.secondary,
    )
}

@Composable
private fun PreviewWarning.message(): String {
    val countText = (count ?: 0).formatWithComma()
    return when (kind) {
        PreviewWarningKind.EncodingFallback -> stringResource(Res.string.import_warning_encoding_cp949)
        PreviewWarningKind.InconsistentColumns -> stringResource(Res.string.import_warning_inconsistent_columns, countText)
        PreviewWarningKind.FormulaWithoutValue -> stringResource(Res.string.import_warning_xlsx_formula, countText)
        PreviewWarningKind.NumberFormatRaw -> stringResource(Res.string.import_warning_xlsx_number_format)
        PreviewWarningKind.FieldCountMismatch -> stringResource(Res.string.import_warning_apkg_field_mismatch, countText)
        PreviewWarningKind.ImageOcclusionExcluded -> stringResource(Res.string.import_warning_apkg_image_occlusion, countText)
    }
}

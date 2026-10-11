package com.whatever.caro.feature.importer.step

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import caromobile.core.designsystem.generated.resources.Res
import caromobile.core.designsystem.generated.resources.ic_arrow_right_24
import caromobile.core.designsystem.generated.resources.ic_edit_24
import caromobile.core.designsystem.generated.resources.ic_file_text_24
import caromobile.core.designsystem.generated.resources.import_chip_format_anki_text
import caromobile.core.designsystem.generated.resources.import_chip_format_apkg
import caromobile.core.designsystem.generated.resources.import_chip_format_quizlet
import caromobile.core.designsystem.generated.resources.import_chip_format_xlsx
import caromobile.core.designsystem.generated.resources.import_hint_source_file
import caromobile.core.designsystem.generated.resources.import_hint_source_paste
import caromobile.core.designsystem.generated.resources.import_label_format_anki_text
import caromobile.core.designsystem.generated.resources.import_label_format_apkg
import caromobile.core.designsystem.generated.resources.import_label_format_quizlet
import caromobile.core.designsystem.generated.resources.import_label_format_xlsx
import caromobile.core.designsystem.generated.resources.import_label_source_file
import caromobile.core.designsystem.generated.resources.import_label_source_paste
import caromobile.core.designsystem.generated.resources.import_label_supported_formats
import caromobile.core.designsystem.generated.resources.import_subtitle_source
import caromobile.core.designsystem.generated.resources.import_tip_max_cards
import caromobile.core.designsystem.generated.resources.import_tip_max_file_size
import caromobile.core.designsystem.generated.resources.import_tip_no_media
import caromobile.core.designsystem.generated.resources.import_tip_no_study_history
import com.whatever.caro.core.designsystem.components.CaroTipSection
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.core.util.NumberFormatter.formatWithComma
import com.whatever.caro.feature.importer.component.ImportStepTitle
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private val BorderWidth = 1.dp
private val CardIconBoxSize = 44.dp
private val IconSize = 24.dp
private val FormatRowMinHeight = 34.dp
private val FormatChipHorizontalPadding = 10.dp

private val SupportedFormats =
    listOf(
        Res.string.import_label_format_apkg to Res.string.import_chip_format_apkg,
        Res.string.import_label_format_anki_text to Res.string.import_chip_format_anki_text,
        Res.string.import_label_format_quizlet to Res.string.import_chip_format_quizlet,
        Res.string.import_label_format_xlsx to Res.string.import_chip_format_xlsx,
    )

/** 팁의 장수는 정책 상수가 아니라 [maxCards]를 따라야 상한을 낮출 때 문구가 함께 바뀐다. */
@Composable
internal fun SourceStep(
    maxCards: Int,
    onClickFile: () -> Unit,
    onClickPaste: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tipMaxCards = stringResource(Res.string.import_tip_max_cards, maxCards.formatWithComma())
    val tipMaxFileSize = stringResource(Res.string.import_tip_max_file_size)
    val tipNoMedia = stringResource(Res.string.import_tip_no_media)
    val tipNoStudyHistory = stringResource(Res.string.import_tip_no_study_history)
    val tipLines =
        remember(
            tipMaxCards,
            tipMaxFileSize,
            tipNoMedia,
            tipNoStudyHistory,
        ) { listOf(tipMaxCards, tipMaxFileSize, tipNoMedia, tipNoStudyHistory) }

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = CaroTheme.spacing.xl2, vertical = CaroTheme.spacing.xl),
        verticalArrangement = Arrangement.spacedBy(CaroTheme.spacing.xl2),
    ) {
        ImportStepTitle(text = stringResource(Res.string.import_subtitle_source))
        Column(verticalArrangement = Arrangement.spacedBy(CaroTheme.spacing.m)) {
            SourceCard(
                icon = Res.drawable.ic_file_text_24,
                label = stringResource(Res.string.import_label_source_file),
                hint = stringResource(Res.string.import_hint_source_file),
                onClick = onClickFile,
            )
            SourceCard(
                icon = Res.drawable.ic_edit_24,
                label = stringResource(Res.string.import_label_source_paste),
                hint = stringResource(Res.string.import_hint_source_paste),
                onClick = onClickPaste,
            )
        }
        SupportedFormatList()
        CaroTipSection(lines = tipLines)
    }
}

@Composable
private fun SourceCard(
    icon: DrawableResource,
    label: String,
    hint: String,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(CaroTheme.shape.m)
                .background(CaroTheme.color.surface.primary)
                .border(width = BorderWidth, color = CaroTheme.color.border.secondary, shape = CaroTheme.shape.m)
                .clickable(role = Role.Button, onClick = onClick)
                .padding(BorderWidth)
                .padding(CaroTheme.spacing.xl),
        horizontalArrangement = Arrangement.spacedBy(CaroTheme.spacing.l),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(CardIconBoxSize)
                    .clip(CaroTheme.shape.m)
                    .background(CaroTheme.color.surface.secondary),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                modifier = Modifier.size(IconSize),
                painter = painterResource(icon),
                contentDescription = null,
                tint = CaroTheme.color.icon.primary,
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(CaroTheme.spacing.xs),
        ) {
            Text(
                text = label,
                style = CaroTheme.typography.body1,
                color = CaroTheme.color.text.primary,
            )
            Text(
                text = hint,
                style = CaroTheme.typography.caption1.regular,
                color = CaroTheme.color.text.secondary,
            )
        }
        Icon(
            modifier = Modifier.size(IconSize),
            painter = painterResource(Res.drawable.ic_arrow_right_24),
            contentDescription = null,
            tint = CaroTheme.color.icon.primary,
        )
    }
}

@Composable
private fun SupportedFormatList() {
    Column(verticalArrangement = Arrangement.spacedBy(CaroTheme.spacing.s)) {
        Text(
            text = stringResource(Res.string.import_label_supported_formats),
            style = CaroTheme.typography.body2.semiBold,
            color = CaroTheme.color.text.primary,
        )
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(CaroTheme.shape.m)
                    .background(CaroTheme.color.surface.primary)
                    .border(width = BorderWidth, color = CaroTheme.color.border.secondary, shape = CaroTheme.shape.m)
                    .padding(BorderWidth)
                    .padding(horizontal = CaroTheme.spacing.xl, vertical = CaroTheme.spacing.s),
        ) {
            SupportedFormats.forEach { (label, chip) -> SupportedFormatRow(label = label, chip = chip) }
        }
    }
}

@Composable
private fun SupportedFormatRow(
    label: StringResource,
    chip: StringResource,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = FormatRowMinHeight)
                .semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.spacedBy(CaroTheme.spacing.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            modifier = Modifier.weight(1f),
            text = stringResource(label),
            style = CaroTheme.typography.label1,
            color = CaroTheme.color.text.primary,
        )
        Text(
            modifier =
                Modifier
                    .clip(CaroTheme.shape.xxl)
                    .background(CaroTheme.color.surface.tertiary)
                    .padding(horizontal = FormatChipHorizontalPadding, vertical = CaroTheme.spacing.xxs),
            text = stringResource(chip),
            style = CaroTheme.typography.caption1.regular,
            color = CaroTheme.color.text.brand,
        )
    }
}

package com.whatever.caro.feature.importer.step

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import caromobile.core.designsystem.generated.resources.Res
import caromobile.core.designsystem.generated.resources.ic_file_text_16
import caromobile.core.designsystem.generated.resources.import_banner_over_limit_first
import caromobile.core.designsystem.generated.resources.import_button_apply
import caromobile.core.designsystem.generated.resources.import_button_preview
import caromobile.core.designsystem.generated.resources.import_content_description_title_with_notice
import caromobile.core.designsystem.generated.resources.import_dialog_too_many_cards_body
import caromobile.core.designsystem.generated.resources.import_reason_mapping_no_type
import caromobile.core.designsystem.generated.resources.import_reason_preview_empty
import caromobile.core.designsystem.generated.resources.import_reason_sheet_empty
import caromobile.core.designsystem.generated.resources.import_reason_sheet_over_limit
import caromobile.core.designsystem.generated.resources.import_reason_subdeck_over_limit
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.core.importparser.model.ParsedTable
import com.whatever.caro.core.model.importing.ImportLimits
import com.whatever.caro.core.util.NumberFormatter.formatWithComma
import com.whatever.caro.feature.importer.components.ImportBottomBar
import com.whatever.caro.feature.importer.components.ImportStepTitle
import com.whatever.caro.feature.importer.logic.importableNoteTypes
import com.whatever.caro.feature.importer.logic.truncateFileName
import com.whatever.caro.feature.importer.mvi.ImportDeckIntent
import com.whatever.caro.feature.importer.mvi.ImportDeckState
import com.whatever.caro.feature.importer.mvi.ImportSelection
import com.whatever.caro.feature.importer.mvi.ImportStep
import com.whatever.caro.feature.importer.mvi.SettingsEntry
import com.whatever.caro.feature.importer.mvi.SettingsKind
import com.whatever.caro.feature.importer.mvi.SettingsReason
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private const val GATE_BANNER_KEY = "gate-banner"
private val FileIconSize = 16.dp
private val FileChipGap = 6.dp

/**
 * 설정 종류마다 목록 아이템만 다르고 하단 버튼과 사유 줄은 같다. 머리 블록도 목록 아이템으로 둬야 세로 스크롤 컨테이너가 하나다.
 * 다시 읽거나 다시 나누는 동안에도 방금 고른 값이 보여야 하므로 [ImportDeckState.pendingDraftSelection]을 먼저 쓴다.
 */
@Composable
internal fun SettingsStep(
    state: ImportDeckState,
    onIntent: (ImportDeckIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val settings = state.step as? ImportStep.Settings ?: return
    val table = state.draftTable ?: return
    val selection = state.pendingDraftSelection ?: state.draftSelection
    val gateNotice =
        if (settings.entry == SettingsEntry.Gate) {
            stringResource(Res.string.import_banner_over_limit_first, ImportLimits.MAX_CARDS.formatWithComma())
        } else {
            null
        }
    val summary: (@Composable () -> Unit)? =
        if (settings.kind == SettingsKind.Subdeck) {
            {
                SubdeckSummary(
                    table = table,
                    selectedIds = selection.subdeckIds,
                    isOverLimit = state.visibleSettingsReason == SettingsReason.SubdeckOverLimit,
                )
            }
        } else {
            null
        }

    SettingsFrame(
        gateNotice = gateNotice,
        isApplyEnabled = state.isSettingsApplyEnabled,
        reason = state.visibleSettingsReason?.let { reason -> reasonText(reason) },
        onApply = { onIntent(ImportDeckIntent.ClickApplySettings) },
        summary = summary,
        modifier = modifier,
    ) {
        settingsItems(
            kind = settings.kind,
            state = state,
            table = table,
            selection = selection,
            gateNotice = gateNotice,
            onIntent = onIntent,
        )
    }
}

private fun LazyListScope.settingsItems(
    kind: SettingsKind,
    state: ImportDeckState,
    table: ParsedTable,
    selection: ImportSelection,
    gateNotice: String?,
    onIntent: (ImportDeckIntent) -> Unit,
) {
    when (kind) {
        SettingsKind.Subdeck -> {
            subdeckItems(table = table, selectedIds = selection.subdeckIds, gateNotice = gateNotice, onIntent = onIntent)
        }

        SettingsKind.Sheet -> {
            sheetItems(table = table, selection = selection, gateNotice = gateNotice, onIntent = onIntent)
        }

        SettingsKind.Delimiter -> {
            val firstCard = importableNoteTypes(table).firstOrNull()?.let { type -> state.draftFirstCards[type.id] }
            textCheckItems(
                table = table,
                selection = selection,
                firstCard = firstCard,
                isResplitting = state.isResplitting,
                onIntent = onIntent,
            )
        }

        SettingsKind.CardLayout -> {
            mappingItems(table = table, mappings = state.draftMappings, firstCards = state.draftFirstCards, onIntent = onIntent)
        }
    }
}

/**
 * 제목은 [ImportStepTitle]이어야 덮개가 걷힌 뒤 제목으로 포커스가 간다.
 * [notice]는 제목보다 위에 있어 포커스가 건너뛰는 먼저 고칠 것 띠 문구라, 화면을 연 이유가 읽히도록 제목과 함께 읽는다.
 */
@Composable
internal fun SettingsHeader(
    fileName: String?,
    title: String,
    hint: String,
    modifier: Modifier = Modifier,
    notice: String? = null,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = CaroTheme.spacing.xl2).padding(bottom = CaroTheme.spacing.xl2),
        verticalArrangement = Arrangement.spacedBy(CaroTheme.spacing.s),
    ) {
        if (fileName != null) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(FileChipGap)) {
                Icon(
                    modifier = Modifier.size(FileIconSize),
                    painter = painterResource(Res.drawable.ic_file_text_16),
                    contentDescription = null,
                    tint = CaroTheme.color.icon.primary,
                )
                Text(text = truncateFileName(fileName), style = CaroTheme.typography.body2.semiBold, color = CaroTheme.color.text.secondary)
            }
        }
        ImportStepTitle(
            text = title,
            readAs =
                notice?.let { text ->
                    stringResource(Res.string.import_content_description_title_with_notice, title, text)
                },
        )
        Text(text = hint, style = CaroTheme.typography.label1, color = CaroTheme.color.text.secondary)
    }
}

/** 내용이 하단 고정 영역 밑으로 지나갈 수 있을 때만 구분선을 보인다. */
@Composable
private fun SettingsFrame(
    gateNotice: String?,
    isApplyEnabled: Boolean,
    reason: String?,
    onApply: () -> Unit,
    summary: (@Composable () -> Unit)?,
    modifier: Modifier,
    content: LazyListScope.() -> Unit,
) {
    val listState = rememberLazyListState()

    Column(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            state = listState,
            contentPadding = PaddingValues(top = CaroTheme.spacing.xl, bottom = CaroTheme.spacing.xl2),
        ) {
            if (gateNotice != null) {
                item(key = GATE_BANNER_KEY, contentType = GATE_BANNER_KEY) {
                    GateBanner(
                        text = gateNotice,
                        modifier = Modifier.padding(horizontal = CaroTheme.spacing.xl2).padding(bottom = CaroTheme.spacing.xl2),
                    )
                }
            }
            content()
        }
        StepBottomDivider(isVisible = listState.canScrollForward || listState.canScrollBackward)
        summary?.invoke()
        ReasonLine(reason = reason)
        ImportBottomBar(
            buttonText = stringResource(if (gateNotice != null) Res.string.import_button_preview else Res.string.import_button_apply),
            onClick = onApply,
            enabled = isApplyEnabled,
            disabledReason = reason,
        )
    }
}

/** 상한을 넘어 미리보기 전에 이 화면을 연 이유다. 고쳐야 넘어갈 수 있으므로 닫을 수 없다. */
@Composable
private fun GateBanner(
    text: String,
    modifier: Modifier,
) {
    Text(
        text = text,
        modifier =
            modifier
                .warningBannerSurface()
                .padding(horizontal = CaroTheme.spacing.l, vertical = CaroTheme.spacing.m),
        style = CaroTheme.typography.caption1.regular,
        color = CaroTheme.color.text.primary,
    )
}

/** 새로 생긴 노드에는 live region 알림이 가지 않을 수 있어 사유 줄을 늘 두고 글자만 바꾼다. 사유가 없으면 높이를 차지하지 않는다. */
@Composable
private fun ReasonLine(reason: String?) {
    Text(
        text = reason.orEmpty(),
        modifier =
            Modifier
                .fillMaxWidth()
                .then(
                    if (reason == null) {
                        Modifier.height(0.dp)
                    } else {
                        Modifier.padding(horizontal = CaroTheme.spacing.xl2, vertical = CaroTheme.spacing.s)
                    },
                ).semantics { liveRegion = LiveRegionMode.Polite },
        style = CaroTheme.typography.caption1.regular,
        color = CaroTheme.color.text.dangerous,
    )
}

/** 카드를 만들어 봐야 아는 상한 초과는 Figma에 따로 문구가 없어 너무 많다는 대화상자의 같은 뜻 문구를 쓴다. */
@Composable
private fun reasonText(reason: SettingsReason): String {
    val maxCards = ImportLimits.MAX_CARDS.formatWithComma()
    return when (reason) {
        SettingsReason.SubdeckOverLimit -> stringResource(Res.string.import_reason_subdeck_over_limit)
        SettingsReason.SheetEmpty -> stringResource(Res.string.import_reason_sheet_empty)
        SettingsReason.SheetOverLimit -> stringResource(Res.string.import_reason_sheet_over_limit, maxCards)
        SettingsReason.NoTypeEnabled -> stringResource(Res.string.import_reason_mapping_no_type)
        SettingsReason.NoCards -> stringResource(Res.string.import_reason_preview_empty)
        SettingsReason.TooManyCards -> stringResource(Res.string.import_dialog_too_many_cards_body, maxCards)
    }
}

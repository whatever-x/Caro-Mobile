package com.whatever.caro.feature.importer.step

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import caromobile.core.designsystem.generated.resources.Res
import caromobile.core.designsystem.generated.resources.import_a11y_filter_result
import caromobile.core.designsystem.generated.resources.import_button_close
import caromobile.core.designsystem.generated.resources.import_button_next
import caromobile.core.designsystem.generated.resources.import_label_side_back
import caromobile.core.designsystem.generated.resources.import_label_side_front
import caromobile.core.designsystem.generated.resources.import_reason_preview_empty
import caromobile.core.designsystem.generated.resources.import_reason_preview_over_limit_paste
import caromobile.core.designsystem.generated.resources.import_reason_preview_over_limit_text
import caromobile.core.designsystem.generated.resources.import_reason_preview_over_limit_xlsx
import caromobile.core.designsystem.generated.resources.import_reason_subdeck_over_limit
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.core.importparser.model.ImportFormat
import com.whatever.caro.core.ui.card.CaroFullTextDialog
import com.whatever.caro.core.ui.card.CaroFullTextSection
import com.whatever.caro.core.util.NumberFormatter.formatWithComma
import com.whatever.caro.feature.importer.component.ImportBottomBar
import com.whatever.caro.feature.importer.logic.PreviewWarning
import com.whatever.caro.feature.importer.logic.filterCards
import com.whatever.caro.feature.importer.logic.previewWarnings
import com.whatever.caro.feature.importer.logic.visibleFilters
import com.whatever.caro.feature.importer.mvi.ImportPreviewFilter
import com.whatever.caro.feature.importer.transform.ImportCard
import com.whatever.caro.feature.importer.transform.ImportPreview
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import kotlin.math.max

private const val SUMMARY_KEY = "summary"
private const val WARNINGS_KEY = "warnings"
private const val HINT_KEY = "hint"
private const val HEADER_KEY = "header"
private const val EMPTY_KEY = "empty"
private const val FILLER_KEY = "filler"

// 요약, 안내 줄 다음이 머리다. 경고 줄이 있으면 요약과 안내 줄 사이에 한 칸 더 있다.
private const val HEADER_INDEX = 2
private val LiveRegionSize = 1.dp

/**
 * 요약부터 카드까지 LazyColumn 하나라 세로 스크롤 컨테이너가 하나고, 가로 스크롤은 필터 칩 줄뿐이다.
 * 열린 카드 전체 보기는 [detailCard]로 받아 이 화면이 그린다.
 */
@Composable
internal fun PreviewStep(
    preview: ImportPreview,
    format: ImportFormat,
    isPaste: Boolean,
    filter: ImportPreviewFilter,
    detailCard: ImportCard?,
    maxCards: Int,
    onSelectFilter: (ImportPreviewFilter) -> Unit,
    onClickCard: (ImportCard) -> Unit,
    onDismissDetail: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val filters = remember(preview) { visibleFilters(preview).toImmutableList() }
    val warnings = remember(preview) { previewWarnings(preview.warnings).toImmutableList() }
    val cards = remember(preview, filter) { filterCards(preview.cards, filter) }
    val headerIndex = if (warnings.isEmpty()) HEADER_INDEX else HEADER_INDEX + 1
    val listState = rememberLazyListState()
    val reason = previewBlockReason(preview, format, isPaste, maxCards)
    val announcement = rememberFilterChangeAnnouncement(filter, resultCount = cards.size, listState, headerIndex)
    val returnFocus = remember { FocusRequester() }
    val openedIndex = rememberOpenedCardIndex(detailCard, returnFocus)

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            PreviewList(
                preview = preview,
                maxCards = maxCards,
                filters = filters,
                filter = filter,
                cards = cards,
                warnings = warnings,
                headerIndex = headerIndex,
                listState = listState,
                returnFocus = returnFocus,
                openedIndex = openedIndex,
                onSelectFilter = onSelectFilter,
                onClickCard = onClickCard,
                modifier = Modifier.weight(1f),
            )
            StepBottomDivider(isVisible = hasHiddenContent(listState))
            ImportBottomBar(
                buttonText = stringResource(Res.string.import_button_next),
                onClick = onNext,
                enabled = reason == null,
                reason = reason,
            )
        }
        // 화면에 보이는 변화가 없는 필터 결과를 스크린 리더로만 알린다. 첫 구성에서는 만들지 않아 처음부터 읽히지 않는다.
        if (announcement != null) {
            Box(
                modifier =
                    Modifier
                        .size(LiveRegionSize)
                        .semantics {
                            liveRegion = LiveRegionMode.Polite
                            contentDescription = announcement
                        },
            )
        }
    }

    if (detailCard != null) CardDetailDialog(card = detailCard, onDismiss = onDismissDetail)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PreviewList(
    preview: ImportPreview,
    maxCards: Int,
    filters: ImmutableList<ImportPreviewFilter>,
    filter: ImportPreviewFilter,
    cards: List<ImportCard>,
    warnings: ImmutableList<PreviewWarning>,
    headerIndex: Int,
    listState: LazyListState,
    returnFocus: FocusRequester,
    openedIndex: Int?,
    onSelectFilter: (ImportPreviewFilter) -> Unit,
    onClickCard: (ImportCard) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    var headerHeightPx by remember { mutableIntStateOf(0) }
    val fillPx = rememberTrailingFillPx(listState, cards.size, headerHeightPx)
    val isPinned by remember(listState, headerIndex) { derivedStateOf { listState.firstVisibleItemIndex >= headerIndex } }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        state = listState,
        contentPadding = PaddingValues(bottom = CaroTheme.spacing.m),
    ) {
        item(key = SUMMARY_KEY) { PreviewSummary(preview = preview, maxCards = maxCards) }
        if (warnings.isNotEmpty()) item(key = WARNINGS_KEY) { PreviewWarningLines(warnings = warnings) }
        item(key = HINT_KEY) { PreviewEditLaterHint() }
        stickyHeader(key = HEADER_KEY) {
            PreviewListHeader(
                filters = filters,
                selected = filter,
                isPinned = isPinned,
                onSelect = onSelectFilter,
                modifier = Modifier.onSizeChanged { size -> headerHeightPx = size.height },
            )
        }
        if (cards.isEmpty()) {
            item(key = EMPTY_KEY) { PreviewEmptyList() }
        } else {
            items(cards, key = { card -> card.sourceIndex }) { card ->
                PreviewCardItem(
                    card = card,
                    onClick = { onClickCard(card) },
                    modifier = if (card.sourceIndex == openedIndex) Modifier.focusRequester(returnFocus) else Modifier,
                )
            }
        }
        if (fillPx > 0) item(key = FILLER_KEY) { Spacer(modifier = Modifier.height(with(density) { fillPx.toDp() })) }
    }
}

/**
 * 머리를 맨 위에 붙이려면 머리 아래 내용이 목록 높이 이상이어야 하므로 카드가 적을 때 모자란 만큼을 끝에 채운다.
 * 카드 높이는 한 줄씩일 때의 하한으로 세어 실제보다 조금 더 채울 수 있고, 카드가 없으면 채우지 않는다.
 */
@Composable
private fun rememberTrailingFillPx(
    listState: LazyListState,
    cardCount: Int,
    headerHeightPx: Int,
): Int {
    val density = LocalDensity.current
    val viewportPx by remember(listState) { derivedStateOf { listState.layoutInfo.viewportSize.height } }
    if (cardCount == 0) return 0
    val minCardPx =
        with(density) {
            (CaroTheme.spacing.m + CaroTheme.spacing.xl * 2 + CaroTheme.spacing.xxs).toPx() +
                CaroTheme.typography.heading2.fontSize
                    .toPx() +
                CaroTheme.typography.label2.fontSize
                    .toPx()
        }
    return max(0, viewportPx - headerHeightPx - (cardCount * minCardPx).toInt())
}

/** 채움 아이템을 뺀 마지막 내용이 아래 고정 영역에 가려졌는지다. */
@Composable
private fun hasHiddenContent(listState: LazyListState): Boolean {
    val isHidden by remember(listState) {
        derivedStateOf {
            val info = listState.layoutInfo
            val last = info.visibleItemsInfo.filter { item -> item.key != FILLER_KEY }.maxByOrNull { item -> item.index }
            val lastContentIndex = info.totalItemsCount - 1 - if (info.visibleItemsInfo.any { item -> item.key == FILLER_KEY }) 1 else 0
            last == null || last.index < lastContentIndex || last.offset + last.size > info.viewportEndOffset
        }
    }
    return isHidden
}

/**
 * 필터가 바뀌면 머리가 맨 위에 붙게 올리고 결과 장수를 알린다. 요약까지 올라가면 방금 누른 칩이 손가락 아래에서 움직인다.
 * 이 화면으로 돌아와 처음 구성될 때는 이미 보이던 필터이므로 올리지도 알리지도 않는다.
 */
@Composable
private fun rememberFilterChangeAnnouncement(
    filter: ImportPreviewFilter,
    resultCount: Int,
    listState: LazyListState,
    headerIndex: Int,
): String? {
    var shownFilter by remember { mutableStateOf(filter) }
    var announcement by remember { mutableStateOf<String?>(null) }
    val text =
        stringResource(
            Res.string.import_a11y_filter_result,
            stringResource(filter.chipLabel()),
            resultCount.formatWithComma(),
        )

    LaunchedEffect(filter) {
        if (filter == shownFilter) return@LaunchedEffect
        shownFilter = filter
        announcement = text
        listState.scrollToItem(headerIndex)
    }
    return announcement
}

@Composable
private fun previewBlockReason(
    preview: ImportPreview,
    format: ImportFormat,
    isPaste: Boolean,
    maxCards: Int,
): String? =
    when {
        preview.limitExceeded -> stringResource(overLimitReason(format, isPaste), maxCards.formatWithComma())
        preview.cards.isEmpty() -> stringResource(Res.string.import_reason_preview_empty)
        else -> null
    }

/** 하위 덱 선택에서 상한을 막는 Anki는 이 화면에서 넘지 않지만 같은 화면 규칙으로 안내를 둔다. */
private fun overLimitReason(
    format: ImportFormat,
    isPaste: Boolean,
): StringResource =
    when {
        format == ImportFormat.XLSX -> Res.string.import_reason_preview_over_limit_xlsx
        format == ImportFormat.TEXT && isPaste -> Res.string.import_reason_preview_over_limit_paste
        format == ImportFormat.TEXT -> Res.string.import_reason_preview_over_limit_text
        else -> Res.string.import_reason_subdeck_over_limit
    }

@Composable
private fun CardDetailDialog(
    card: ImportCard,
    onDismiss: () -> Unit,
) {
    val frontTitle = stringResource(Res.string.import_label_side_front)
    val backTitle = stringResource(Res.string.import_label_side_back)
    val sections =
        remember(card, frontTitle, backTitle) {
            listOf(CaroFullTextSection(frontTitle, card.front), CaroFullTextSection(backTitle, card.back))
        }
    CaroFullTextDialog(sections = sections, onDismiss = onDismiss, closeLabel = stringResource(Res.string.import_button_close))
}

/**
 * 전체 보기를 닫으면 연 카드 항목으로 포커스를 돌려준다. 돌려줄 항목은 반환한 위치의 카드에만 [returnFocus]가 붙는다.
 * 닫기 요청을 처리하는 시점에는 팝업 창이 아직 붙어 있어, 창을 걷은 다음 구성에서 포커스를 요청한다.
 */
@Composable
private fun rememberOpenedCardIndex(
    detailCard: ImportCard?,
    returnFocus: FocusRequester,
): Int? {
    var openedIndex by remember { mutableStateOf<Int?>(null) }
    var wasOpen by remember { mutableStateOf(false) }

    LaunchedEffect(detailCard) {
        if (detailCard != null) {
            openedIndex = detailCard.sourceIndex
            wasOpen = true
        } else if (wasOpen) {
            wasOpen = false
            returnFocus.requestFocus()
        }
    }
    return openedIndex
}

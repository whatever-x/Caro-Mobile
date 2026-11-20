package com.whatever.caro.feature.importer.step

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import caromobile.core.designsystem.generated.resources.Res
import caromobile.core.designsystem.generated.resources.import_button_close
import caromobile.core.designsystem.generated.resources.import_button_import
import caromobile.core.designsystem.generated.resources.import_content_description_filter_result
import caromobile.core.designsystem.generated.resources.import_label_side_back
import caromobile.core.designsystem.generated.resources.import_label_side_front
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.core.importparser.model.ParsedTable
import com.whatever.caro.core.ui.card.CaroFullTextDialog
import com.whatever.caro.core.ui.card.CaroFullTextSection
import com.whatever.caro.core.util.NumberFormatter.formatWithComma
import com.whatever.caro.feature.importer.components.ImportBottomBar
import com.whatever.caro.feature.importer.logic.cardTypeName
import com.whatever.caro.feature.importer.logic.cardTypeNames
import com.whatever.caro.feature.importer.logic.visibleFilters
import com.whatever.caro.feature.importer.mvi.DeckInfoDraft
import com.whatever.caro.feature.importer.mvi.ImportDeckIntent
import com.whatever.caro.feature.importer.mvi.ImportDeckState
import com.whatever.caro.feature.importer.mvi.ImportPreviewFilter
import com.whatever.caro.feature.importer.transform.ImportCard
import com.whatever.caro.feature.importer.transform.ImportPreview
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource

private const val SUMMARY_KEY = "summary"
private const val HEADER_KEY = "header"
private const val CARD_TYPE = "card"

// 요약은 아이템 하나라 목록 머리는 늘 두 번째 아이템이다.
private const val HEADER_INDEX = 1
private const val ANNOUNCEMENT_CLEAR_MILLIS = 3_000L
private val LiveRegionSize = 1.dp

/** 요약부터 카드까지 LazyColumn 하나라 세로 스크롤 컨테이너가 하나고, 가로 스크롤은 필터 칩 줄뿐이다. */
@Composable
internal fun PreviewStep(
    state: ImportDeckState,
    onIntent: (ImportDeckIntent) -> Unit,
    modifier: Modifier = Modifier,
    importFocusRequester: FocusRequester? = null,
) {
    val table = state.table ?: return
    val preview = state.preview ?: return
    val listState = rememberLazyListState()
    val announcement = rememberFilterChangeAnnouncement(state.previewFilter, state.filteredCards.size, listState)
    val returnFocus = remember { FocusRequester() }
    val openedIndex = rememberOpenedCardIndex(state.detailCard, returnFocus)

    Column(modifier = modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            PreviewList(
                state = state,
                table = table,
                preview = preview,
                listState = listState,
                openedIndex = openedIndex,
                returnFocus = returnFocus,
                onIntent = onIntent,
            )
            // 화면에 보이는 변화가 없는 필터 결과를 스크린 리더로만 알린다. 새로 생긴 노드에는 알림이 가지 않을 수 있어 처음부터 두고 설명만 바꾼다.
            Box(
                modifier =
                    Modifier
                        .size(LiveRegionSize)
                        .semantics {
                            liveRegion = LiveRegionMode.Polite
                            contentDescription = announcement.orEmpty()
                        },
            )
        }
        StepBottomDivider(isVisible = listState.canScrollForward || listState.canScrollBackward)
        ImportBottomBar(
            buttonText = stringResource(Res.string.import_button_import),
            onClick = { onIntent(ImportDeckIntent.ClickImport) },
            focusRequester = importFocusRequester,
        )
    }

    state.detailCard?.let { card -> CardDetailDialog(card = card, onDismissRequest = { onIntent(ImportDeckIntent.CloseCardDetail) }) }
    state.deckInfoDraft?.let { draft -> PreviewDeckInfoSheet(draft = draft, onIntent = onIntent) }
}

@Composable
private fun PreviewDeckInfoSheet(
    draft: DeckInfoDraft,
    onIntent: (ImportDeckIntent) -> Unit,
) {
    DeckInfoSheet(
        draft = draft,
        onNameChange = { name -> onIntent(ImportDeckIntent.UpdateDeckInfoName(name)) },
        onDescriptionChange = { description -> onIntent(ImportDeckIntent.UpdateDeckInfoDescription(description)) },
        onApply = { onIntent(ImportDeckIntent.ApplyDeckInfo) },
        onDismissRequest = { onIntent(ImportDeckIntent.DismissDeckInfo) },
    )
}

/** [returnFocus]는 전체 보기를 연 카드([openedIndex])에만 붙어, 닫은 뒤 그 카드가 포커스를 돌려받는다. */
@Composable
private fun PreviewList(
    state: ImportDeckState,
    table: ParsedTable,
    preview: ImportPreview,
    listState: LazyListState,
    openedIndex: Int?,
    returnFocus: FocusRequester,
    onIntent: (ImportDeckIntent) -> Unit,
) {
    val filters = remember(state.filterCounts) { visibleFilters(state.filterCounts).toImmutableList() }
    val typeNames = remember(table) { cardTypeNames(table) }
    val itemModifier = Modifier.padding(horizontal = CaroTheme.spacing.xl2)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(top = CaroTheme.spacing.l, bottom = CaroTheme.spacing.xl2),
        verticalArrangement = Arrangement.spacedBy(CaroTheme.spacing.m),
    ) {
        item(key = SUMMARY_KEY, contentType = SUMMARY_KEY) {
            PreviewSummary(
                state = state,
                table = table,
                preview = preview,
                onDismissBanner = { onIntent(ImportDeckIntent.DismissDelimiterBanner) },
                onChangeSetting = { kind -> onIntent(ImportDeckIntent.ClickChangeSetting(kind)) },
                onChangeDeckInfo = { onIntent(ImportDeckIntent.ClickChangeDeckInfo) },
                modifier = itemModifier,
            )
        }
        stickyHeader(key = HEADER_KEY, contentType = HEADER_KEY) {
            PreviewListHeader(
                filters = filters,
                counts = state.filterCounts,
                selected = state.previewFilter,
                mediaRemovedCount = preview.stats.mediaRemovedCount,
                onSelect = { filter -> onIntent(ImportDeckIntent.SelectPreviewFilter(filter)) },
            )
        }
        items(state.filteredCards, key = { card -> card.sourceIndex }, contentType = { CARD_TYPE }) { card ->
            PreviewCardItem(
                card = card,
                typeName = cardTypeName(table, typeNames, card),
                onClick = { onIntent(ImportDeckIntent.ClickCard(card)) },
                modifier = if (card.sourceIndex == openedIndex) itemModifier.focusRequester(returnFocus) else itemModifier,
            )
        }
    }
}

/**
 * 머리가 위에 붙어 있을 때 필터를 바꾸면 걸러진 첫 카드가 머리 바로 아래에 오도록 머리 위치로 옮긴다.
 * 머리가 붙기 전이면 방금 누른 칩이 손가락 아래에서 움직이지 않게 그대로 둔다.
 * 이 화면으로 돌아와 처음 구성될 때는 이미 보이던 필터이므로 옮기지도 알리지도 않는다.
 * 알린 문장을 남겨 두면 스크린 리더로 훑을 때 목록 위에 지난 결과가 읽히므로, 알림이 전달될 시간이 지나면 비운다.
 */
@Composable
private fun rememberFilterChangeAnnouncement(
    filter: ImportPreviewFilter,
    resultCount: Int,
    listState: LazyListState,
): String? {
    var shownFilter by remember { mutableStateOf(filter) }
    var announcement by remember { mutableStateOf<String?>(null) }
    val text =
        stringResource(
            Res.string.import_content_description_filter_result,
            stringResource(filter.chipLabel()),
            resultCount.formatWithComma(),
        )

    LaunchedEffect(filter) {
        if (filter == shownFilter) return@LaunchedEffect
        shownFilter = filter
        announcement = text
        if (listState.firstVisibleItemIndex >= HEADER_INDEX) listState.scrollToItem(HEADER_INDEX)
        delay(ANNOUNCEMENT_CLEAR_MILLIS)
        announcement = null
    }
    return announcement
}

/**
 * 전체 보기를 닫으면 연 카드 항목으로 포커스를 돌려준다.
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

@Composable
private fun CardDetailDialog(
    card: ImportCard,
    onDismissRequest: () -> Unit,
) {
    val frontTitle = stringResource(Res.string.import_label_side_front)
    val backTitle = stringResource(Res.string.import_label_side_back)
    val sections =
        remember(card, frontTitle, backTitle) {
            listOf(CaroFullTextSection(frontTitle, card.front), CaroFullTextSection(backTitle, card.back))
        }
    CaroFullTextDialog(
        sections = sections,
        onDismissRequest = onDismissRequest,
        closeLabel = stringResource(Res.string.import_button_close),
    )
}

package com.whatever.caro.feature.importer.mvi

import com.whatever.caro.core.importparser.model.ParsedTable
import com.whatever.caro.core.model.importing.FieldMapping
import com.whatever.caro.core.viewmodel.contract.UiState
import com.whatever.caro.feature.importer.logic.needsDelimiterCheck
import com.whatever.caro.feature.importer.transform.ImportCard
import com.whatever.caro.feature.importer.transform.ImportPreview
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf

/**
 * [table]은 큰 파싱 결과라 rememberSaveable에 넣지 않는다.
 * [isPasteOverflowed]는 길이 초과 오류 상태다. 상한을 넘겨 붙여 넣으면 [pasteText]의 뒤가 이미 잘려 있어, 일부를 지우거나 바꿔 상한 이하로 돌아와도 풀리지 않는다.
 * [mappings]는 미리보기 계산이 받는 모양대로 noteTypeId를 키로 둔다.
 * [filterCounts]가 0인 칩은 화면이 숨긴다.
 */
data class ImportDeckState(
    val step: ImportStep = ImportStep.Source,
    val loading: ImportLoading? = null,
    val isPickerOpen: Boolean = false,
    val isPaste: Boolean = false,
    val pasteText: String = "",
    val isPasteOverflowed: Boolean = false,
    val table: ParsedTable? = null,
    val selection: ImportSelection = ImportSelection(),
    val mappings: ImmutableMap<String, FieldMapping> = persistentMapOf(),
    val preview: ImportPreview? = null,
    val previewFilter: ImportPreviewFilter = ImportPreviewFilter.All,
    val filterCounts: ImmutableMap<ImportPreviewFilter, Int> = persistentMapOf(),
    val filteredCards: ImmutableList<ImportCard> = persistentListOf(),
    val detailCard: ImportCard? = null,
    val isDelimiterBannerDismissed: Boolean = false,
    val deckName: String = "",
    val dialog: ImportDialog? = null,
) : UiState {
    val isBusy: Boolean
        get() = loading != null || isPickerOpen || dialog != null || detailCard != null

    val isPasteNextEnabled: Boolean
        get() = pasteText.isNotEmpty() && !isPasteOverflowed

    /** 설정 확인 띠에 넣을 칸으로 나뉘지 않은 줄 수다. 띠를 보이지 않으면 null이다. */
    val delimiterBannerRowCount: Int?
        get() =
            table
                ?.takeIf { source -> step == ImportStep.Preview && !isDelimiterBannerDismissed && needsDelimiterCheck(source) }
                ?.layout
                ?.singleFieldRowCount
}

/** 선언 순서가 칩이 놓이는 순서다. */
enum class ImportPreviewFilter {
    All,
    EmptyReplaced,
    Truncated,
    MediaRemoved,
}

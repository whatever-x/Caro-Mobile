package com.whatever.caro.feature.importer.mvi

import com.whatever.caro.core.importparser.model.ParsedTable
import com.whatever.caro.core.model.importing.FieldMapping
import com.whatever.caro.core.viewmodel.contract.UiState
import com.whatever.caro.feature.importer.transform.ImportPreview
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf

/**
 * [table]은 단계가 공유하는 큰 파싱 결과라 rememberSaveable에 넣지 않는다.
 * [pendingSelection]은 다시 읽는 동안 사용자가 고른 값이다. 읽는 중에 이어서 바꾸는 선택이 먼저 고른 값을 잃지 않으려면 이 값에서 시작해야 한다.
 * [mappings]는 미리보기 계산이 받는 모양대로 noteTypeId를 키로 둔다.
 */
data class ImportDeckState(
    val step: ImportStep = ImportStep.Source,
    val loading: ImportLoading? = null,
    val isPickerOpen: Boolean = false,
    val isPaste: Boolean = false,
    val pasteText: String = "",
    val table: ParsedTable? = null,
    val selection: ImportSelection = ImportSelection(),
    val pendingSelection: ImportSelection? = null,
    val mappings: PersistentMap<String, FieldMapping> = persistentMapOf(),
    val preview: ImportPreview? = null,
    val previewFilter: ImportPreviewFilter = ImportPreviewFilter.All,
    val deckName: String = "",
    val deckDescription: String = "",
    val saving: SavingProgress? = null,
    val dialog: ImportDialog? = null,
) : UiState {
    val isBusy: Boolean
        get() = loading != null || isPickerOpen || saving != null || step == ImportStep.CreatingDeck

    /** 결과 표 자리에만 그리는 [ImportLoading.TextReparsing]을 뺀, 단계 위를 덮는 읽기 표시. */
    val overlayLoading: ImportLoading?
        get() = loading.takeUnless { it == ImportLoading.TextReparsing }

    val isDeckInfoConfirmEnabled: Boolean
        get() = deckName.isNotBlank() && deckDescription.isNotBlank() && !isBusy
}

enum class ImportPreviewFilter {
    All,
    EmptyReplaced,
    Truncated,
    MediaRemoved,
}

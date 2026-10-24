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
 * [isPasteOverflowed]는 상한을 넘겨 붙여 넣어 [pasteText]의 뒤가 이미 잘렸다는 뜻이다. 일부를 지워 상한 이하로 돌아와도 잘린 부분은 돌아오지 않는다.
 * [mappings]는 미리보기 계산이 받는 모양대로 noteTypeId를 키로 둔다.
 * [sourceVersion]은 새 자료를 읽을 때만 바뀌고, [mappingInputVersion]은 항목 연결의 입력인 읽기 결과가 바뀔 때 바뀐다.
 * 화면은 두 값으로 단계별 스크롤 같은 보존 상태의 키를 정하므로, 같은 단계 안의 다시 읽기는 값을 바꾸지 않거나 연결 입력만 바꿔야 한다.
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
    val pendingSelection: ImportSelection? = null,
    val mappings: PersistentMap<String, FieldMapping> = persistentMapOf(),
    val preview: ImportPreview? = null,
    val previewFilter: ImportPreviewFilter = ImportPreviewFilter.All,
    val deckName: String = "",
    val deckDescription: String = "",
    val saving: SavingProgress? = null,
    val dialog: ImportDialog? = null,
    val sourceVersion: Int = 0,
    val mappingInputVersion: Int = 0,
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

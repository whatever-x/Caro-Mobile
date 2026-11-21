package com.whatever.caro.feature.importer.logic

import com.whatever.caro.core.importparser.model.ParsedTable
import com.whatever.caro.core.model.importing.FieldMapping
import com.whatever.caro.feature.importer.mvi.ImportDeckState
import com.whatever.caro.feature.importer.mvi.ImportPreviewFilter
import com.whatever.caro.feature.importer.mvi.ImportSelection
import com.whatever.caro.feature.importer.mvi.ImportStep
import com.whatever.caro.feature.importer.transform.ImportPreview
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableMap

/** 첫 미리보기와 설정 [적용]이 같은 값으로 미리보기에 들어가야 칩별 카드 수와 걸러진 목록이 새 카드와 어긋나지 않는다. */
internal fun ImportDeckState.showingPreview(
    table: ParsedTable,
    selection: ImportSelection,
    mappings: Map<String, FieldMapping>,
    preview: ImportPreview,
    deckName: String,
): ImportDeckState =
    copy(
        step = ImportStep.Preview,
        loading = null,
        table = table,
        selection = selection,
        mappings = mappings.toImmutableMap(),
        preview = preview,
        previewFilter = ImportPreviewFilter.All,
        filterCounts = previewFilterCounts(preview.cards),
        filteredCards = preview.cards,
        detailCard = null,
        deckName = deckName,
    ).withoutDraft()

internal fun ImportDeckState.withoutDraft(): ImportDeckState =
    copy(
        draftTable = null,
        draftSelection = ImportSelection(),
        pendingDraftSelection = null,
        draftMappings = persistentMapOf(),
        draftFirstCards = persistentMapOf(),
        settingsReason = null,
    )

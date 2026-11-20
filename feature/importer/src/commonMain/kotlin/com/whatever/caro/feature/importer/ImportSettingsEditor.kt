package com.whatever.caro.feature.importer

import com.whatever.caro.core.importparser.model.ParsedTable
import com.whatever.caro.core.model.importing.FieldMapping
import com.whatever.caro.feature.importer.logic.appliedPreviewReason
import com.whatever.caro.feature.importer.logic.carryOverMappings
import com.whatever.caro.feature.importer.logic.deckNamePrefill
import com.whatever.caro.feature.importer.logic.draftSettingsReason
import com.whatever.caro.feature.importer.logic.firstPreviewCards
import com.whatever.caro.feature.importer.logic.isSettingsKindAvailable
import com.whatever.caro.feature.importer.logic.resolveDelimiter
import com.whatever.caro.feature.importer.logic.sheetSummaryReason
import com.whatever.caro.feature.importer.logic.showingPreview
import com.whatever.caro.feature.importer.logic.toggleField
import com.whatever.caro.feature.importer.logic.toggledAllSubdecks
import com.whatever.caro.feature.importer.logic.withFirstPreviewCard
import com.whatever.caro.feature.importer.logic.withoutDraft
import com.whatever.caro.feature.importer.mvi.ImportDeckState
import com.whatever.caro.feature.importer.mvi.ImportDialog
import com.whatever.caro.feature.importer.mvi.ImportLoading
import com.whatever.caro.feature.importer.mvi.ImportSelection
import com.whatever.caro.feature.importer.mvi.ImportStep
import com.whatever.caro.feature.importer.mvi.SettingsEntry
import com.whatever.caro.feature.importer.mvi.SettingsKind
import com.whatever.caro.feature.importer.mvi.SettingsReason
import com.whatever.caro.feature.importer.source.ImportDispatchers
import com.whatever.caro.feature.importer.source.ImportLimitsConfig
import com.whatever.caro.feature.importer.source.ImportReadController
import com.whatever.caro.feature.importer.source.ImportReadResult
import com.whatever.caro.feature.importer.source.ImportReadSource
import com.whatever.caro.feature.importer.source.ImportReadStage
import com.whatever.caro.feature.importer.transform.ImportCard
import com.whatever.caro.feature.importer.transform.ImportPreview
import com.whatever.caro.feature.importer.transform.ImportPreviewBuilder
import com.whatever.caro.feature.importer.transform.defaultFieldMappings
import io.github.aakira.napier.Napier
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

private val MappingTargets = setOf(FieldMapping.FRONT, FieldMapping.BACK)

/** BaseViewModel의 reduce와 launch는 protected라, 설정 흐름은 ViewModel이 구현한 이 interface로 상태를 바꾸고 작업을 시작한다. */
internal interface ImportSettingsHost {
    val state: ImportDeckState

    fun reduce(transform: ImportDeckState.() -> ImportDeckState)

    fun launch(block: suspend () -> Unit): Job
}

/** 초안 다시 읽기는 [ImportReadStage.Draft]로 읽는다. 사용자가 그 화면에 머물러 다시 시도하므로 실패해도 원본을 남겨야 한다. */
internal class ImportSettingsEditor(
    private val host: ImportSettingsHost,
    private val readController: ImportReadController,
    private val previewBuilder: ImportPreviewBuilder,
    private val limits: ImportLimitsConfig,
    private val dispatchers: ImportDispatchers,
) {
    private class SettingsDraft(
        val table: ParsedTable,
        val selection: ImportSelection,
        val mappings: Map<String, FieldMapping>,
        val firstCards: ImmutableMap<String, ImportCard>,
        val reason: SettingsReason?,
    )

    // 다시 읽기와 [적용]은 한 번에 하나만 돈다. 새 처리를 시작하거나 멈출 때 앞 처리를 취소해야 늦게 끝난 결과가 새 초안을 덮어쓰지 않는다.
    private var job: Job? = null

    fun openFromPreview(kind: SettingsKind) {
        val state = host.state
        val table = state.table ?: return
        if (state.step != ImportStep.Preview || state.deckInfoDraft != null || !isSettingsKindAvailable(kind, table)) return
        val draft = draftOf(kind, table, state.selection, state.mappings)
        host.reduce { copy(step = ImportStep.Settings(kind, SettingsEntry.FromPreview)).withDraft(draft) }
    }

    fun openGate(
        kind: SettingsKind,
        table: ParsedTable,
        selection: ImportSelection,
    ) {
        val draft = draftOf(kind, table, selection, defaultFieldMappings(table))
        host.reduce { copy(step = ImportStep.Settings(kind, SettingsEntry.Gate), loading = null).withDraft(draft) }
    }

    /** 바꾼 내용 없이 미리보기로 돌아간다. 처리 중이면 [stop]이 먼저다. */
    fun leave() {
        host.reduce { copy(step = ImportStep.Preview).withoutDraft() }
    }

    /** 처리만 멈추고 화면과 초안은 그대로 둔다. 다시 읽는 중에 고른 값은 읽기가 끝나지 않았으므로 버린다. */
    fun stop() {
        cancelWork()
        host.reduce { withoutPendingWork() }
    }

    fun cancelWork() {
        job?.cancel()
        job = null
        readController.stop()
    }

    fun toggleSubdeck(id: String) {
        val table = draftTableOf(SettingsKind.Subdeck) ?: return
        if (table.subdecks.none { subdeck -> subdeck.id == id }) return
        val ids = host.state.draftSelection.subdeckIds
        updateSelection(SettingsKind.Subdeck, table) { copy(subdeckIds = (if (id in ids) ids - id else ids + id).toImmutableSet()) }
    }

    fun toggleAllSubdecks() {
        val table = draftTableOf(SettingsKind.Subdeck) ?: return
        updateSelection(SettingsKind.Subdeck, table) { copy(subdeckIds = toggledAllSubdecks(table, subdeckIds)) }
    }

    /** 요약 행 수로 막히는 시트는 읽어도 고를 수 없으므로 다시 읽지 않는다. 시트를 바꾸면 첫 줄 판정을 새 시트 기준으로 다시 받는다. */
    fun selectSheet(index: Int) {
        val table = draftTableOf(SettingsKind.Sheet) ?: return
        val selection = host.state.draftSelection
        if (index == selection.sheetIndex || table.sheets.none { sheet -> sheet.index == index }) return
        val candidate = selection.copy(sheetIndex = index, hasHeaderRow = null)
        if (sheetSummaryReason(table, index, limits.maxCards) != null) {
            updateSelection(SettingsKind.Sheet, table) { candidate }
        } else {
            rereadDraft(SettingsKind.Sheet, candidate)
        }
    }

    fun selectDelimiter(delimiter: Char) {
        val table = draftTableOf(SettingsKind.Delimiter) ?: return
        val base = host.state.let { state -> state.pendingDraftSelection ?: state.draftSelection }
        if (delimiter == resolveDelimiter(base.delimiter, table.layout)) return
        rereadDraft(SettingsKind.Delimiter, base.copy(delimiter = delimiter))
    }

    /** Anki 텍스트 머리말이 열 이름을 준 표는 첫 줄이 데이터라 화면이 체크 대신 안내를 보이므로 받지 않는다. */
    fun changeHeaderRow(hasHeaderRow: Boolean) {
        val state = host.state
        val kind =
            (state.step as? ImportStep.Settings)?.kind?.takeIf { kind ->
                kind == SettingsKind.Sheet || kind == SettingsKind.Delimiter
            }
        val table = state.draftTable
        if (kind == null || table == null || table.layout?.columnsFromHeader == true) return
        val base = state.pendingDraftSelection ?: state.draftSelection
        if (hasHeaderRow == (base.hasHeaderRow ?: table.layout?.hasHeaderRow)) return
        val candidate = base.copy(hasHeaderRow = hasHeaderRow)
        if (kind == SettingsKind.Sheet && sheetSummaryReason(table, candidate.sheetIndex, limits.maxCards) != null) {
            updateSelection(kind, table) { candidate }
        } else {
            rereadDraft(kind, candidate)
        }
    }

    fun toggleNoteType(noteTypeId: String) {
        val table = draftTableOf(SettingsKind.CardLayout) ?: return
        val mapping = host.state.draftMappings[noteTypeId] ?: return
        updateMapping(table, mapping.copy(excluded = !mapping.excluded))
    }

    fun toggleField(
        noteTypeId: String,
        target: String,
        fieldName: String,
    ) {
        val table = draftTableOf(SettingsKind.CardLayout) ?: return
        val mapping = host.state.draftMappings[noteTypeId] ?: return
        val type = table.noteTypes.firstOrNull { candidate -> candidate.id == noteTypeId } ?: return
        if (target !in MappingTargets || fieldName !in type.fieldNames) return
        updateMapping(table, toggleField(mapping, target, fieldName))
    }

    /** 원본이 없으면 다시 읽기가 멈춘 읽기와 같은 결과를 돌려줘 덮개가 남으므로 덮개를 띄우기 전에 확인한다. */
    fun apply() {
        val state = host.state
        val kind = (state.step as? ImportStep.Settings)?.kind ?: return
        val table = state.draftTable ?: return
        val source = readController.source ?: return
        if (!state.isSettingsApplyEnabled) return
        host.reduce { copy(loading = ImportLoading.BuildingPreview(source.fileName())) }
        job?.cancel()
        job = host.launch { applyDraft(kind, table, state.draftSelection, state.draftMappings) }
    }

    /** 하위 덱은 요약만 바뀌어 있으므로 고른 하위 덱의 행을 이때 읽는다. 다른 설정은 이미 읽어 둔 초안 표로 만든다. */
    private suspend fun applyDraft(
        kind: SettingsKind,
        draftTable: ParsedTable,
        selection: ImportSelection,
        draftMappings: Map<String, FieldMapping>,
    ) {
        val table = if (kind == SettingsKind.Subdeck) readSubdecks(selection) ?: return else draftTable
        computeCatching {
            val mappings = carryOverMappings(draftTable, draftMappings, table)
            val preview = build(table, mappings)
            val reason = appliedPreviewReason(kind, preview)
            host.reduce {
                if (reason != null) {
                    copy(loading = null, settingsReason = reason)
                } else {
                    val name = if (isDeckInfoEdited) deckName else deckNamePrefill(table, isPaste, selection)
                    showingPreview(table, selection, mappings, preview, name)
                }
            }
        }
    }

    private suspend fun readSubdecks(selection: ImportSelection): ParsedTable? =
        when (val result = readController.reread(ImportReadStage.Draft, subdeckIds = selection.subdeckIds)) {
            is ImportReadResult.Success -> {
                result.table
            }

            is ImportReadResult.Failure -> {
                fail()
                null
            }

            ImportReadResult.Cancelled -> {
                null
            }
        }

    private suspend fun build(
        table: ParsedTable,
        mappings: Map<String, FieldMapping>,
    ): ImportPreview = withContext(dispatchers.default) { previewBuilder.build(table, mappings, host.state.emptySideText, limits.maxCards) }

    /**
     * OutOfMemoryError 같은 Error도 받아야 덮개와 다시 나누는 중 표시가 걷힌다. Main에서 하는 카드 구성, 첫 카드, 덱 이름 계산도 같은 이유로 이 안에서 받는다.
     * 이 처리가 취소되지 않았는데 계산 안쪽에서 나온 취소를 다시 던지면 덮개가 남는다.
     */
    private suspend fun <T> computeCatching(block: suspend () -> T): T? =
        try {
            block()
        } catch (cancellation: CancellationException) {
            currentCoroutineContext().ensureActive()
            fail(cancellation)
            null
        } catch (error: Throwable) {
            fail(error)
            null
        }

    private fun rereadDraft(
        kind: SettingsKind,
        candidate: ImportSelection,
    ) {
        val source = readController.source ?: return
        job?.cancel()
        val loading = if (kind == SettingsKind.Sheet) ImportLoading.ReadingFile(source.fileName()) else null
        host.reduce { copy(loading = loading, pendingDraftSelection = candidate) }
        job =
            host.launch {
                val result =
                    readController.reread(
                        ImportReadStage.Draft,
                        sheetIndex = candidate.sheetIndex,
                        delimiter = candidate.delimiter,
                        hasHeaderRow = candidate.hasHeaderRow,
                    )
                when (result) {
                    is ImportReadResult.Success -> acceptDraftRead(kind, candidate, result.table)
                    is ImportReadResult.Failure -> fail()
                    ImportReadResult.Cancelled -> Unit
                }
            }
    }

    private suspend fun acceptDraftRead(
        kind: SettingsKind,
        candidate: ImportSelection,
        table: ParsedTable,
    ) {
        val state = host.state
        val previous = state.draftTable ?: table
        val draft = computeCatching { draftOf(kind, table, candidate, carryOverMappings(previous, state.draftMappings, table)) } ?: return
        host.reduce { copy(loading = null, pendingDraftSelection = null).withDraft(draft) }
    }

    /** 읽기 실패는 읽기 제어가 원인을 남기므로 계산 실패만 기록한다. 초안은 직전 값으로 둔다. */
    private fun fail(error: Throwable? = null) {
        error?.let { cause -> Napier.e(throwable = cause) { "import settings preview failed" } }
        host.reduce { withoutPendingWork().copy(dialog = ImportDialog.PreviewFailed) }
    }

    private fun ImportDeckState.withoutPendingWork(): ImportDeckState = copy(loading = null, pendingDraftSelection = null)

    private fun updateSelection(
        kind: SettingsKind,
        table: ParsedTable,
        transform: ImportSelection.() -> ImportSelection,
    ) {
        val state = host.state
        val draft = draftOf(kind, table, state.draftSelection.transform(), state.draftMappings)
        host.reduce { withDraft(draft) }
    }

    private fun updateMapping(
        table: ParsedTable,
        mapping: FieldMapping,
    ) {
        val state = host.state
        val mappings = state.draftMappings + (mapping.noteTypeId to mapping)
        val draft =
            SettingsDraft(
                table = table,
                selection = state.draftSelection,
                mappings = mappings,
                firstCards = state.draftFirstCards.withFirstPreviewCard(table, mapping, state.emptySideText),
                reason = draftSettingsReason(SettingsKind.CardLayout, table, state.draftSelection, mappings, limits.maxCards),
            )
        host.reduce { withDraft(draft) }
    }

    private fun draftTableOf(kind: SettingsKind): ParsedTable? =
        host.state.takeIf { state -> (state.step as? ImportStep.Settings)?.kind == kind }?.draftTable

    private fun draftOf(
        kind: SettingsKind,
        table: ParsedTable,
        selection: ImportSelection,
        mappings: Map<String, FieldMapping>,
    ): SettingsDraft =
        SettingsDraft(
            table = table,
            selection = selection,
            mappings = mappings,
            firstCards = if (kind.showsFirstCard()) firstPreviewCards(table, mappings, host.state.emptySideText) else persistentMapOf(),
            reason = draftSettingsReason(kind, table, selection, mappings, limits.maxCards),
        )

    private fun ImportDeckState.withDraft(draft: SettingsDraft): ImportDeckState =
        copy(
            draftTable = draft.table,
            draftSelection = draft.selection,
            draftMappings = draft.mappings.toImmutableMap(),
            draftFirstCards = draft.firstCards,
            settingsReason = draft.reason,
        )
}

private fun SettingsKind.showsFirstCard(): Boolean = this == SettingsKind.Delimiter || this == SettingsKind.CardLayout

private fun ImportReadSource.fileName(): String? = (this as? ImportReadSource.File)?.name

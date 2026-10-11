package com.whatever.caro.feature.importer

import com.whatever.caro.core.importparser.SpreadsheetDateStyle
import com.whatever.caro.core.importparser.model.ImportFormat
import com.whatever.caro.core.importparser.model.ParsedTable
import com.whatever.caro.core.viewmodel.BaseViewModel
import com.whatever.caro.core.viewmodel.ExceptionFilter
import com.whatever.caro.feature.importer.logic.classifyPick
import com.whatever.caro.feature.importer.logic.isEmptyImport
import com.whatever.caro.feature.importer.logic.isIgnoredWhileBusy
import com.whatever.caro.feature.importer.logic.isReadFailure
import com.whatever.caro.feature.importer.logic.isSelectableSubdeck
import com.whatever.caro.feature.importer.logic.isSheetEmpty
import com.whatever.caro.feature.importer.logic.pasteBlockReason
import com.whatever.caro.feature.importer.logic.pickFailedDialog
import com.whatever.caro.feature.importer.logic.readFailureDialog
import com.whatever.caro.feature.importer.logic.summarizeSubdeckSelection
import com.whatever.caro.feature.importer.logic.toggleAllSubdecks
import com.whatever.caro.feature.importer.mvi.ImportDeckIntent
import com.whatever.caro.feature.importer.mvi.ImportDeckSideEffect
import com.whatever.caro.feature.importer.mvi.ImportDeckState
import com.whatever.caro.feature.importer.mvi.ImportDialog
import com.whatever.caro.feature.importer.mvi.ImportDismissPolicy
import com.whatever.caro.feature.importer.mvi.ImportLoading
import com.whatever.caro.feature.importer.mvi.ImportSelection
import com.whatever.caro.feature.importer.mvi.ImportStep
import com.whatever.caro.feature.importer.platform.PickResult
import com.whatever.caro.feature.importer.platform.PickedImportFile
import com.whatever.caro.feature.importer.source.ImportCopyResult
import com.whatever.caro.feature.importer.source.ImportDispatchers
import com.whatever.caro.feature.importer.source.ImportFileCopy
import com.whatever.caro.feature.importer.source.ImportLimitsConfig
import com.whatever.caro.feature.importer.source.ImportReadSource
import com.whatever.caro.feature.importer.source.ImportSourceReader
import com.whatever.caro.feature.importer.source.ImportTempStorage
import com.whatever.caro.feature.importer.transform.defaultFieldMappings
import kotlinx.collections.immutable.toPersistentMap
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

private const val APKG_EXTENSION = "apkg"

class ImportDeckViewModel internal constructor(
    private val sourceReader: ImportSourceReader,
    private val tempStorage: ImportTempStorage,
    private val fileCopy: ImportFileCopy,
    private val limits: ImportLimitsConfig,
    private val dispatchers: ImportDispatchers,
    exceptionFilter: ExceptionFilter,
) : BaseViewModel<ImportDeckState, ImportDeckIntent, ImportDeckSideEffect>(ImportDeckState(), exceptionFilter) {
    private var source: ImportReadSource? = null
    private var readJob: Job? = null
    private var generation = 0

    init {
        // 비동기로 지우면 첫 복사와 겹칠 수 있어, 생성 시점에 이전 실행이 남긴 파일을 동기로 지운다.
        tempStorage.clearAll()
    }

    override suspend fun handleIntent(intent: ImportDeckIntent) {
        if (isIgnoredWhileBusy(currentState, intent)) return
        when (intent) {
            ImportDeckIntent.ClickBack -> onClickBack()
            ImportDeckIntent.DismissDialog -> onDismissDialog()
            ImportDeckIntent.ClickPickFile -> onClickPickFile()
            ImportDeckIntent.ClickPaste -> onClickPaste()
            is ImportDeckIntent.FilePicked -> onFilePicked(intent)
            is ImportDeckIntent.UpdatePasteText -> onUpdatePasteText(intent.text)
            is ImportDeckIntent.ClickPasteNext -> onClickPasteNext(intent)
            is ImportDeckIntent.ToggleSubdeck -> onToggleSubdeck(intent.id)
            ImportDeckIntent.ToggleAllSubdecks -> onToggleAllSubdecks()
            is ImportDeckIntent.SelectSheet -> onSelectSheet(intent.index)
            is ImportDeckIntent.SelectDelimiter -> onSelectDelimiter(intent.delimiter)
            is ImportDeckIntent.ChangeHeaderRow -> onChangeHeaderRow(intent.hasHeaderRow)
            ImportDeckIntent.ClickSelectionNext -> onClickSelectionNext()
            ImportDeckIntent.ConfirmDiscard -> onConfirmDiscard()
            else -> Unit
        }
    }

    override fun onCleared() {
        tempStorage.clearAll()
        super.onCleared()
    }

    private fun onClickBack() {
        val state = currentState
        when (state.loading) {
            is ImportLoading.Parsing -> {
                abortFirstRead()
            }

            null -> {
                goBackFromStep(state)
            }

            // 다시 읽기는 취소해도 선택을 확정하기 전이라 바꾸기 전 선택이 그대로 남는다.
            else -> {
                cancelRead()
                reduce { copy(loading = null) }
            }
        }
    }

    private fun goBackFromStep(state: ImportDeckState) {
        when (state.step) {
            ImportStep.Source -> postSideEffect(ImportDeckSideEffect.NavigateBack)
            ImportStep.Paste -> reduce { copy(step = ImportStep.Source, isPaste = false) }
            ImportStep.TextCheck -> if (state.isPaste) leaveReadResult() else reduce { copy(dialog = ImportDialog.DiscardConfirm) }
            ImportStep.SubdeckSelect, ImportStep.SheetSelect -> reduce { copy(dialog = ImportDialog.DiscardConfirm) }
            else -> Unit
        }
    }

    private fun abortFirstRead() {
        cancelRead()
        clearTempAsync()
        leaveReadResult()
    }

    private fun leaveReadResult() {
        source = null
        reduce { backToEntry() }
    }

    private fun onDismissDialog() {
        val state = currentState
        val dialog = state.dialog ?: return
        if (dialog.dismissPolicy == ImportDismissPolicy.NotDismissible) return
        // 다시 읽기에서 실패했을 때만 표가 남아 있다. 확인하면 읽은 내용을 버리고 처음 단계로 돌아간다.
        if (dialog.isReadFailure() && state.table != null) leaveReadResult() else reduce { copy(dialog = null) }
    }

    private fun onConfirmDiscard() {
        if (currentState.dialog != ImportDialog.DiscardConfirm) return
        cancelRead()
        source = null
        clearTempAsync()
        reduce { ImportDeckState(pasteText = pasteText) }
    }

    private suspend fun onClickPickFile() {
        if (currentState.step != ImportStep.Source) return
        reduce { copy(isPickerOpen = true) }
        withContext(dispatchers.io) { tempStorage.clearAll() }
        postSideEffect(ImportDeckSideEffect.OpenFilePicker)
    }

    private fun onClickPaste() {
        if (currentState.step != ImportStep.Source) return
        reduce { copy(step = ImportStep.Paste, isPaste = true) }
    }

    private fun onUpdatePasteText(text: String) {
        if (currentState.step != ImportStep.Paste) return
        reduce { copy(pasteText = text) }
    }

    private fun onFilePicked(intent: ImportDeckIntent.FilePicked) {
        when (val result = intent.result) {
            PickResult.Cancelled -> {
                reduce { copy(isPickerOpen = false) }
            }

            // 선택기가 크기 초과로 판정한 결과이므로 보고한 크기가 상한 이하여도 초과로 취급한다.
            is PickResult.TooLarge -> {
                val size = maxOf(result.sizeBytes, limits.maxFileBytes + 1)
                reduce { copy(isPickerOpen = false, dialog = classifyPick(result.displayName, size, limits.maxFileBytes)) }
            }

            is PickResult.Failed -> {
                reduce { copy(isPickerOpen = false, dialog = pickFailedDialog(extension = "")) }
            }

            is PickResult.Picked -> {
                readPickedFile(result.file, intent.dateStyle)
            }
        }
    }

    private fun onClickPasteNext(intent: ImportDeckIntent.ClickPasteNext) {
        val state = currentState
        if (state.step != ImportStep.Paste || pasteBlockReason(state.pasteText, limits.maxPasteChars) != null) return
        val paste = ImportReadSource.Paste(state.pasteText, intent.sourceName, intent.dateStyle)
        source = paste
        reduce { copy(loading = ImportLoading.Parsing(fileName = null)) }
        startRead(read = { paste.read(sourceReader, dispatchers) }, onRead = ::onFirstRead)
    }

    private fun readPickedFile(
        file: PickedImportFile,
        dateStyle: SpreadsheetDateStyle,
    ) {
        source = null
        startRead(
            read = {
                val copied = copyPicked(file, dateStyle)
                // 빈 집합은 하위 덱 요약만 읽으라는 뜻이다. 파서가 null이면 모든 하위 덱의 행까지 읽는다.
                copied?.read(sourceReader, dispatchers, subdeckIds = if (copied.extension == APKG_EXTENSION) emptySet() else null)
            },
            onRead = ::onFirstRead,
        )
    }

    /** 통과하지 못하면 대화상자를 띄우고 null을 돌려준다. 핸들은 결과와 상관없이 정리한다. */
    private suspend fun copyPicked(
        file: PickedImportFile,
        dateStyle: SpreadsheetDateStyle,
    ): ImportReadSource.File? {
        try {
            val (name, size) = withContext(dispatchers.io) { file.displayName() to file.sizeBytes() }
            val rejection = classifyPick(name, size, limits.maxFileBytes)
            if (rejection != null) {
                failRead(rejection)
                return null
            }
            reduce { copy(isPickerOpen = false, loading = ImportLoading.Parsing(name)) }
            val destination = withContext(dispatchers.io) { tempStorage.newFile() }
            val copySource = ImportReadSource.File(destination, name, dateStyle)
            source = copySource
            val copied = withContext(dispatchers.io) { fileCopy.copy(file.openSource(), destination, limits.maxFileBytes) }
            if (copied is ImportCopyResult.TooLarge) {
                failRead(ImportDialog.FileTooLarge(copySource.extension))
                return null
            }
            return copySource
        } finally {
            withContext(NonCancellable + dispatchers.io) { file.discard() }
        }
    }

    private fun onFirstRead(table: ParsedTable) {
        if (isEmptyImport(table)) {
            failRead(ImportDialog.EmptyResult(currentState.isPaste))
            return
        }
        val (step, selection) =
            when (table.format) {
                ImportFormat.APKG -> {
                    ImportStep.SubdeckSelect to ImportSelection(subdeckIds = table.subdecks.map { subdeck -> subdeck.id }.toPersistentSet())
                }

                ImportFormat.XLSX -> {
                    ImportStep.SheetSelect to ImportSelection(hasHeaderRow = table.layout?.hasHeaderRow)
                }

                ImportFormat.TEXT -> {
                    ImportStep.TextCheck to ImportSelection()
                }
            }
        reduce { copy(loading = null, step = step, table = table, selection = selection) }
    }

    private fun onToggleSubdeck(id: String) {
        val state = currentState
        val table = state.table ?: return
        if (state.step != ImportStep.SubdeckSelect || !isSelectableSubdeck(table, id)) return
        val ids = state.selection.subdeckIds
        reduce { copy(selection = selection.copy(subdeckIds = if (id in ids) ids.remove(id) else ids.add(id))) }
    }

    private fun onToggleAllSubdecks() {
        val state = currentState
        val table = state.table ?: return
        if (state.step != ImportStep.SubdeckSelect) return
        reduce { copy(selection = selection.copy(subdeckIds = toggleAllSubdecks(table, selection.subdeckIds))) }
    }

    private fun onSelectSheet(index: Int) {
        val state = currentState
        val table = state.table ?: return
        if (state.step != ImportStep.SheetSelect || index == state.selection.sheetIndex || table.sheets.none { it.index == index }) return
        // 시트를 바꾸면 제목 행 추정을 새 시트 기준으로 다시 받는다.
        rereadSelection(state.selection.copy(sheetIndex = index, hasHeaderRow = null)) { reread ->
            copy(hasHeaderRow = reread.layout?.hasHeaderRow)
        }
    }

    private fun onSelectDelimiter(delimiter: Char) {
        val state = currentState
        if (state.step != ImportStep.TextCheck) return
        rereadSelection(state.selection.copy(delimiter = delimiter))
    }

    private fun onChangeHeaderRow(hasHeaderRow: Boolean) {
        val state = currentState
        if (state.step != ImportStep.SheetSelect && state.step != ImportStep.TextCheck) return
        rereadSelection(state.selection.copy(hasHeaderRow = hasHeaderRow))
    }

    private fun onClickSelectionNext() {
        val state = currentState
        val table = state.table ?: return
        when {
            state.step == ImportStep.SubdeckSelect -> rereadSubdecks(table, state.selection)
            state.step == ImportStep.SheetSelect && !isSheetEmpty(table, state.selection.sheetIndex) -> moveToMapping(table)
            state.step == ImportStep.TextCheck -> moveToMapping(table)
        }
    }

    private fun rereadSubdecks(
        table: ParsedTable,
        selection: ImportSelection,
    ) {
        val read = source ?: return
        val summary = summarizeSubdeckSelection(table, selection.subdeckIds, limits.maxCards)
        // 빈 선택은 요약만 읽는 호출과 같아져 행을 받을 수 없다.
        if (summary.selectedTotal == 0 || summary.isOverLimit) return
        reduce { copy(loading = ImportLoading.Reparsing(read.name)) }
        startRead(
            read = { read.read(sourceReader, dispatchers, subdeckIds = selection.subdeckIds) },
            onRead = { reread ->
                reduce {
                    copy(
                        loading = null,
                        step = ImportStep.Mapping,
                        table = reread,
                        mappings = defaultFieldMappings(reread).toPersistentMap(),
                    )
                }
            },
        )
    }

    /** [candidate]는 읽기가 성공했을 때만 [ImportDeckState.selection]이 된다. [confirm]은 읽은 표로 확정할 값을 고친다. */
    private fun rereadSelection(
        candidate: ImportSelection,
        confirm: ImportSelection.(ParsedTable) -> ImportSelection = { this },
    ) {
        val read = source ?: return
        val loading = if (currentState.step == ImportStep.TextCheck) ImportLoading.TextReparsing else ImportLoading.Reparsing(read.name)
        reduce { copy(loading = loading) }
        startRead(
            read = {
                read.read(
                    sourceReader,
                    dispatchers,
                    sheetIndex = candidate.sheetIndex,
                    delimiter = candidate.delimiter,
                    hasHeaderRow = candidate.hasHeaderRow,
                )
            },
            onRead = { reread -> reduce { copy(loading = null, table = reread, selection = candidate.confirm(reread)) } },
        )
    }

    private fun moveToMapping(table: ParsedTable) {
        reduce { copy(step = ImportStep.Mapping, mappings = defaultFieldMappings(table).toPersistentMap()) }
    }

    /**
     * 이전 읽기를 취소하고 새 읽기를 시작한다. 읽기를 시작한 쪽에서 [ImportDeckState.loading]을 먼저 정해 입력을 바로 막는다.
     * [read]가 null을 돌려주면 이미 대화상자로 끝낸 것이다.
     */
    private fun startRead(
        read: suspend () -> ParsedTable?,
        onRead: (ParsedTable) -> Unit,
    ) {
        readJob?.cancel()
        generation += 1
        val ticket = generation
        readJob =
            launch {
                try {
                    val table = read() ?: return@launch
                    if (ticket == generation) onRead(table)
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (error: Exception) {
                    if (ticket == generation) failRead(readFailureDialog(error, source?.extension.orEmpty(), currentState.isPaste))
                }
            }
    }

    private fun cancelRead() {
        readJob?.cancel()
        readJob = null
        generation += 1
    }

    private fun failRead(dialog: ImportDialog) {
        source = null
        clearTempAsync()
        reduce { copy(loading = null, isPickerOpen = false, dialog = dialog) }
    }

    private fun clearTempAsync() {
        launch(dispatchers.io) { tempStorage.clearAll() }
    }
}

/** 붙여넣기는 입력을 유지한 채 붙여넣기 단계로, 파일은 처음 단계로 돌아간다. */
private fun ImportDeckState.backToEntry(): ImportDeckState =
    ImportDeckState(step = if (isPaste) ImportStep.Paste else ImportStep.Source, isPaste = isPaste, pasteText = pasteText)

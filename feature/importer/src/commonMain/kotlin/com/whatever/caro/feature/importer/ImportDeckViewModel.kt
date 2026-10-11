package com.whatever.caro.feature.importer

import com.whatever.caro.core.importparser.ImportParseException
import com.whatever.caro.core.importparser.SpreadsheetDateStyle
import com.whatever.caro.core.importparser.model.ImportFormat
import com.whatever.caro.core.importparser.model.ParsedTable
import com.whatever.caro.core.model.deck.Deck
import com.whatever.caro.core.model.deck.DeckInputLimits
import com.whatever.caro.core.model.importing.FieldMapping
import com.whatever.caro.core.viewmodel.BaseViewModel
import com.whatever.caro.core.viewmodel.ExceptionFilter
import com.whatever.caro.feature.importer.logic.classifyPick
import com.whatever.caro.feature.importer.logic.deckInfoPrefill
import com.whatever.caro.feature.importer.logic.importableNoteTypes
import com.whatever.caro.feature.importer.logic.isEmptyImport
import com.whatever.caro.feature.importer.logic.isIgnoredWhileBusy
import com.whatever.caro.feature.importer.logic.isReadFailure
import com.whatever.caro.feature.importer.logic.isSelectableSubdeck
import com.whatever.caro.feature.importer.logic.isSheetEmpty
import com.whatever.caro.feature.importer.logic.mappingBlockReason
import com.whatever.caro.feature.importer.logic.nextPasteOverflowed
import com.whatever.caro.feature.importer.logic.pasteBlockReason
import com.whatever.caro.feature.importer.logic.pickFailedDialog
import com.whatever.caro.feature.importer.logic.previewBlockReason
import com.whatever.caro.feature.importer.logic.readFailureDialog
import com.whatever.caro.feature.importer.logic.sanitizeDisplayName
import com.whatever.caro.feature.importer.logic.selectionStep
import com.whatever.caro.feature.importer.logic.summarizeSubdeckSelection
import com.whatever.caro.feature.importer.logic.takeWithoutSplittingSurrogate
import com.whatever.caro.feature.importer.logic.toggleAllSubdecks
import com.whatever.caro.feature.importer.logic.toggleField
import com.whatever.caro.feature.importer.mvi.ImportDeckIntent
import com.whatever.caro.feature.importer.mvi.ImportDeckSideEffect
import com.whatever.caro.feature.importer.mvi.ImportDeckState
import com.whatever.caro.feature.importer.mvi.ImportDialog
import com.whatever.caro.feature.importer.mvi.ImportDismissPolicy
import com.whatever.caro.feature.importer.mvi.ImportLoading
import com.whatever.caro.feature.importer.mvi.ImportPreviewFilter
import com.whatever.caro.feature.importer.mvi.ImportSelection
import com.whatever.caro.feature.importer.mvi.ImportStep
import com.whatever.caro.feature.importer.mvi.SavingProgress
import com.whatever.caro.feature.importer.platform.PickResult
import com.whatever.caro.feature.importer.platform.PickedImportFile
import com.whatever.caro.feature.importer.save.DeckCreationResult
import com.whatever.caro.feature.importer.save.ImportSaveCoordinator
import com.whatever.caro.feature.importer.save.ImportSaveCoordinatorFactory
import com.whatever.caro.feature.importer.source.ImportCopyResult
import com.whatever.caro.feature.importer.source.ImportDispatchers
import com.whatever.caro.feature.importer.source.ImportFileCopy
import com.whatever.caro.feature.importer.source.ImportLimitsConfig
import com.whatever.caro.feature.importer.source.ImportReadSource
import com.whatever.caro.feature.importer.source.ImportSourceReader
import com.whatever.caro.feature.importer.source.ImportTempStorage
import com.whatever.caro.feature.importer.transform.ImportCard
import com.whatever.caro.feature.importer.transform.ImportPreviewBuilder
import com.whatever.caro.feature.importer.transform.defaultFieldMappings
import com.whatever.caro.feature.importer.transform.toCardContent
import io.github.aakira.napier.Napier
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toPersistentMap
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val APKG_EXTENSION = "apkg"

// 이름이나 설명 값을 거절한 서버 코드. 그 밖의 거절은 입력을 고쳐도 소용없어 안내 문구가 다르다.
private val DECK_INPUT_ERROR_CODES = setOf("C001", "C002")

private val MAPPING_TARGETS = setOf(FieldMapping.FRONT, FieldMapping.BACK)

class ImportDeckViewModel internal constructor(
    private val sourceReader: ImportSourceReader,
    private val tempStorage: ImportTempStorage,
    private val fileCopy: ImportFileCopy,
    private val limits: ImportLimitsConfig,
    private val dispatchers: ImportDispatchers,
    private val saveCoordinatorFactory: ImportSaveCoordinatorFactory,
    exceptionFilter: ExceptionFilter,
) : BaseViewModel<ImportDeckState, ImportDeckIntent, ImportDeckSideEffect>(ImportDeckState(), exceptionFilter) {
    private var source: ImportReadSource? = null
    private var readJob: Job? = null
    private var generation = 0
    private val previewBuilder = ImportPreviewBuilder()

    // 선택 단계에서 돌아왔을 때 고친 연결을 이어 쓸지 가리는 기준이다. 연결을 만든 시점의 노트 유형별 항목 이름을 둔다.
    private var mappedFieldNames: Map<String, List<String>> = emptyMap()

    // 덱 정보 미리 채움은 이름이 달라질 수 있는 읽기(첫 읽기, 하위 덱 행 읽기, 시트 변경)가 성공했을 때만 내린다.
    // 구분자와 제목 행 변경은 이름이 같으므로 내리지 않아야 사용자가 고친 이름이 남는다.
    private var isDeckInfoPrefilled = false

    private var saveSession: ImportSaveSession? = null

    // 화면이 단계별 보존 상태의 키로 쓰는 번호다. 상태가 초기화돼도 이전 번호와 겹치면 지난 자료의 스크롤이 되살아나므로 상태 밖에서 센다.
    private var inputVersion = 0

    private val saveHost =
        object : ImportSaveHost {
            override val state: ImportDeckState get() = currentState

            override fun reduce(transform: ImportDeckState.() -> ImportDeckState) = this@ImportDeckViewModel.reduce(transform)

            override fun postSideEffect(effect: ImportDeckSideEffect) = this@ImportDeckViewModel.postSideEffect(effect)

            override fun launch(block: suspend () -> Unit) {
                this@ImportDeckViewModel.launch { block() }
            }
        }

    init {
        // 이 화면의 디렉터리는 건드리지 않으므로 첫 복사와 겹치지 않는다. 디스크 작업이라 main에서 하지 않는다.
        launch(dispatchers.io) { tempStorage.clearStale() }
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
            is ImportDeckIntent.ToggleNoteType -> onToggleNoteType(intent.noteTypeId)
            is ImportDeckIntent.ToggleField -> onToggleField(intent)
            is ImportDeckIntent.ClickMappingNext -> onClickMappingNext(intent.emptySideText)
            is ImportDeckIntent.SelectPreviewFilter -> onSelectPreviewFilter(intent.filter)
            is ImportDeckIntent.ClickPreviewNext -> onClickPreviewNext(intent)
            is ImportDeckIntent.UpdateDeckName -> onUpdateDeckName(intent.name)
            is ImportDeckIntent.UpdateDeckDescription -> onUpdateDeckDescription(intent.description)
            ImportDeckIntent.ClickCreateDeck -> onClickCreateDeck()
            ImportDeckIntent.RetryCreateDeck -> onRetryCreateDeck()
            ImportDeckIntent.ConfirmDiscard -> onConfirmDiscard()
            ImportDeckIntent.ClickStop -> saveSession?.requestStop()
            ImportDeckIntent.ConfirmStop -> saveSession?.confirmStop()
            ImportDeckIntent.RetrySave -> saveSession?.retry()
            ImportDeckIntent.ClickViewDeck -> saveSession?.viewDeck()
            ImportDeckIntent.ClickGoHome -> saveSession?.goHome()
            is ImportDeckIntent.ClickCard -> onClickCard(intent.card)
        }
    }

    override fun onCleared() {
        // viewModelScope는 이미 취소됐고, 디스크 작업을 main에서 하지 않으려고 별도 scope에서 지운다.
        val reading = readJob
        CoroutineScope(dispatchers.io).launch {
            reading?.join()
            tempStorage.clearAll()
        }
        super.onCleared()
    }

    private fun onClickBack() {
        val state = currentState
        // 대화상자의 닫기 규칙을 거쳐야 닫을 수 없는 대화상자가 뒤로 가기에 닫히지 않는다.
        if (state.dialog != null) {
            onDismissDialog()
            return
        }
        when (state.loading) {
            is ImportLoading.Parsing -> {
                abortFirstRead()
            }

            // 선택한 파일의 이름과 크기를 읽는 동안에는 아직 loading이 없어, 그대로 두면 읽기를 취소하지 않고 화면을 나간다.
            null -> {
                if (state.step == ImportStep.Source && readJob?.isActive == true) abortFirstRead() else goBackFromStep(state)
            }

            // 다시 읽기는 취소해도 선택을 확정하기 전이라 바꾸기 전 선택이 그대로 남는다.
            else -> {
                cancelRead()
                reduce { copy(loading = null, pendingSelection = null) }
            }
        }
    }

    private fun goBackFromStep(state: ImportDeckState) {
        when (state.step) {
            ImportStep.Source -> postSideEffect(ImportDeckSideEffect.NavigateBack)
            ImportStep.Paste -> reduce { copy(step = ImportStep.Source, isPaste = false) }
            ImportStep.TextCheck -> if (state.isPaste) leaveReadResult() else reduce { copy(dialog = ImportDialog.DiscardConfirm) }
            ImportStep.SubdeckSelect, ImportStep.SheetSelect -> reduce { copy(dialog = ImportDialog.DiscardConfirm) }
            ImportStep.Mapping -> state.table?.let { table -> reduce { copy(step = selectionStep(table.format)) } }
            ImportStep.Preview -> reduce { copy(step = ImportStep.Mapping, preview = null, previewFilter = ImportPreviewFilter.All) }
            ImportStep.DeckInfo -> reduce { copy(step = ImportStep.Preview) }
            ImportStep.Saving -> saveSession?.requestStop()
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
        // 상한을 1자 넘긴 값까지만 두어도 너무 김 판정은 같고, 거대한 클립보드 글이 레이아웃과 메모리를 잡지 않는다.
        // 서로게이트 쌍 가운데서 자르면 길이가 상한 이하로 돌아가 잘린 글이 통과하므로, 쌍을 보존하지 않고 그대로 자른다.
        // 자른 글은 일부를 지워 상한 이하로 돌아와도 뒷부분이 없으므로 넘친 표시를 따로 남긴다.
        reduce {
            copy(
                pasteText = text.take(limits.maxPasteChars + 1),
                isPasteOverflowed = nextPasteOverflowed(pasteText, isPasteOverflowed, text, limits.maxPasteChars),
            )
        }
    }

    private suspend fun onFilePicked(intent: ImportDeckIntent.FilePicked) {
        // 프로세스가 죽은 뒤 다시 전달된 결과도 받아야 하므로 isPickerOpen이 아니라 단계로 거른다.
        if (currentState.step != ImportStep.Source) {
            (intent.result as? PickResult.Picked)?.let { picked -> withContext(NonCancellable + dispatchers.io) { picked.file.discard() } }
            return
        }
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
                Napier.w(throwable = result.cause) { "import file pick failed" }
                reduce { copy(isPickerOpen = false, dialog = pickFailedDialog(extension = "")) }
            }

            is PickResult.Picked -> {
                readPickedFile(result.file, intent.dateStyle)
            }
        }
    }

    private fun onClickPasteNext(intent: ImportDeckIntent.ClickPasteNext) {
        val state = currentState
        if (state.step != ImportStep.Paste ||
            pasteBlockReason(state.pasteText, limits.maxPasteChars, state.isPasteOverflowed) != null
        ) {
            return
        }
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
            val (name, size) = withContext(dispatchers.io) { sanitizeDisplayName(file.displayName()) to file.sizeBytes() }
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
        isDeckInfoPrefilled = false
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
        val version = nextInputVersion()
        reduce {
            copy(
                loading = null,
                step = step,
                table = table,
                selection = selection,
                sourceVersion = version,
                mappingInputVersion = version,
            )
        }
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
        rereadSelection((state.pendingSelection ?: state.selection).copy(delimiter = delimiter))
    }

    private fun onChangeHeaderRow(hasHeaderRow: Boolean) {
        val state = currentState
        if (state.step != ImportStep.SheetSelect && state.step != ImportStep.TextCheck) return
        rereadSelection((state.pendingSelection ?: state.selection).copy(hasHeaderRow = hasHeaderRow))
    }

    private fun onClickSelectionNext() {
        val state = currentState
        val table = state.table ?: return
        when {
            state.step == ImportStep.SubdeckSelect -> rereadSubdecks(table, state.selection)
            state.step == ImportStep.SheetSelect && !isSheetEmpty(table, state.selection.sheetIndex) -> enterMapping(table)
            state.step == ImportStep.TextCheck -> enterMapping(table)
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
                isDeckInfoPrefilled = false
                enterMapping(reread, isNewInput = true)
            },
        )
    }

    /** [candidate]는 읽기가 성공했을 때만 [ImportDeckState.selection]이 되고 그때까지 [ImportDeckState.pendingSelection]에 둔다. [confirm]은 읽은 표로 확정할 값을 고친다. */
    private fun rereadSelection(
        candidate: ImportSelection,
        confirm: ImportSelection.(ParsedTable) -> ImportSelection = { this },
    ) {
        val read = source ?: return
        val loading = if (currentState.step == ImportStep.TextCheck) ImportLoading.TextReparsing else ImportLoading.Reparsing(read.name)
        reduce { copy(loading = loading, pendingSelection = candidate) }
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
            onRead = { reread ->
                // 시트 이름이 덱 이름에 들어가므로 시트가 바뀐 경우에만 덱 정보를 다시 채운다.
                if (candidate.sheetIndex != currentState.selection.sheetIndex) isDeckInfoPrefilled = false
                val version = nextInputVersion()
                reduce {
                    copy(
                        loading = null,
                        table = reread,
                        selection = candidate.confirm(reread),
                        pendingSelection = null,
                        mappingInputVersion = version,
                    )
                }
            },
        )
    }

    /**
     * 항목 이름이 달라진 유형에 옛 연결을 쓰면 없는 항목을 가리켜 빈 카드가 되므로, 유형별 항목 이름이 모두 같을 때만 고친 연결을 이어 쓴다.
     * [isNewInput]은 방금 다시 읽은 표라는 뜻이라 항목 연결 화면의 보존 상태를 버리게 한다.
     */
    private fun enterMapping(
        table: ParsedTable,
        isNewInput: Boolean = false,
    ) {
        val fieldNames = importableNoteTypes(table).associate { type -> type.id to type.fieldNames }
        val keepEdits = currentState.mappings.isNotEmpty() && fieldNames == mappedFieldNames
        mappedFieldNames = fieldNames
        val version = if (isNewInput) nextInputVersion() else null
        reduce {
            copy(
                loading = null,
                step = ImportStep.Mapping,
                table = table,
                mappings = if (keepEdits) mappings else defaultFieldMappings(table).toPersistentMap(),
                mappingInputVersion = version ?: mappingInputVersion,
            )
        }
    }

    private fun onToggleNoteType(noteTypeId: String) {
        val state = currentState
        val mapping = state.mappings[noteTypeId]
        if (state.step != ImportStep.Mapping || mapping == null) return
        reduce { copy(mappings = mappings.put(noteTypeId, mapping.copy(excluded = !mapping.excluded))) }
    }

    private fun onToggleField(intent: ImportDeckIntent.ToggleField) {
        val state = currentState
        val table = state.table ?: return
        val mapping = state.mappings[intent.noteTypeId]
        val type = importableNoteTypes(table).firstOrNull { candidate -> candidate.id == intent.noteTypeId }
        if (state.step != ImportStep.Mapping || mapping == null || type == null) return
        if (intent.target !in MAPPING_TARGETS || intent.fieldName !in type.fieldNames) return
        val toggled = toggleField(mapping, intent.target, intent.fieldName).mapping
        reduce { copy(mappings = mappings.put(intent.noteTypeId, toggled)) }
    }

    private fun onClickMappingNext(emptySideText: String) {
        val state = currentState
        val table = state.table ?: return
        if (state.step != ImportStep.Mapping || mappingBlockReason(table, state.mappings) != null) return
        reduce { copy(loading = ImportLoading.BuildingPreview) }
        val ticket = nextTicket()
        readJob =
            launch {
                try {
                    val preview =
                        withContext(dispatchers.default) {
                            previewBuilder.build(table, state.mappings, emptySideText, limits.maxCards)
                        }
                    if (ticket == generation) {
                        reduce {
                            copy(
                                loading = null,
                                step = ImportStep.Preview,
                                preview = preview,
                                previewFilter = ImportPreviewFilter.All,
                            )
                        }
                    }
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (error: Exception) {
                    // 오버레이가 남으면 뒤로 가기를 뺀 모든 입력이 막힌다. 연결 단계에 머물러 다시 누를 수 있게 한다.
                    Napier.e(throwable = error) { "import preview build failed" }
                    if (ticket == generation) reduce { copy(loading = null, dialog = ImportDialog.PreviewFailed) }
                }
            }
    }

    private fun onSelectPreviewFilter(filter: ImportPreviewFilter) {
        if (currentState.step != ImportStep.Preview) return
        reduce { copy(previewFilter = filter) }
    }

    private fun onClickCard(card: ImportCard) {
        val state = currentState
        if (state.step != ImportStep.Preview || state.dialog != null) return
        reduce { copy(dialog = ImportDialog.CardDetail(card)) }
    }

    private fun onClickPreviewNext(intent: ImportDeckIntent.ClickPreviewNext) {
        val state = currentState
        val table = state.table ?: return
        val preview = state.preview ?: return
        if (state.step != ImportStep.Preview || previewBlockReason(preview, table.format, state.isPaste) != null) return
        if (isDeckInfoPrefilled) {
            reduce { copy(step = ImportStep.DeckInfo) }
            return
        }
        val prefill = deckInfoPrefill(table, state.isPaste, state.selection, intent.fileDescriptionTemplate, intent.pasteDescription)
        isDeckInfoPrefilled = true
        reduce { copy(step = ImportStep.DeckInfo, deckName = prefill.name, deckDescription = prefill.description) }
    }

    private fun onUpdateDeckName(name: String) {
        if (currentState.step != ImportStep.DeckInfo) return
        reduce { copy(deckName = name.takeWithoutSplittingSurrogate(DeckInputLimits.NAME_MAX)) }
    }

    private fun onUpdateDeckDescription(description: String) {
        if (currentState.step != ImportStep.DeckInfo) return
        reduce { copy(deckDescription = description.takeWithoutSplittingSurrogate(DeckInputLimits.DESC_MAX)) }
    }

    private suspend fun onClickCreateDeck() {
        val state = currentState
        if (state.step != ImportStep.DeckInfo || !state.isDeckInfoConfirmEnabled) return
        createDeck()
    }

    private suspend fun onRetryCreateDeck() {
        if (currentState.dialog != ImportDialog.DeckCreateFailed) return
        reduce { copy(dialog = null) }
        createDeck()
    }

    /** 단계를 먼저 바꿔 연달아 누른 입력을 [isIgnoredWhileBusy]가 막게 한다. 자동으로 다시 보내지 않는다. */
    private suspend fun createDeck() {
        val state = currentState
        val cards = state.preview?.cards ?: return
        reduce { copy(step = ImportStep.CreatingDeck) }
        val coordinator = saveCoordinatorFactory.create()
        when (val result = coordinator.createDeck(state.deckName, state.deckDescription)) {
            is DeckCreationResult.Created -> {
                startSaving(coordinator, result.deck, state.deckName, cards)
            }

            is DeckCreationResult.Failed -> {
                Napier.w(throwable = result.cause) { "import deck creation failed" }
                reduce { copy(step = ImportStep.DeckInfo, dialog = ImportDialog.DeckCreateFailed) }
            }

            is DeckCreationResult.Rejected -> {
                reduce { copy(step = ImportStep.DeckInfo) }
                postSideEffect(rejectionSideEffect(result.code))
            }
        }
    }

    private suspend fun startSaving(
        coordinator: ImportSaveCoordinator,
        deck: Deck,
        deckName: String,
        cards: List<ImportCard>,
    ) {
        // 변환과 묶음 나누기가 최대 1만 장의 모든 글자를 훑으므로 main에서 하지 않는다.
        val chunks = withContext(dispatchers.default) { coordinator.planChunks(cards.map(ImportCard::toCardContent)) }
        // 저장을 시작하면 이전 단계로 돌아갈 수 없으므로, 네트워크를 쓰는 동안 읽은 표와 미리보기, 원본 사본을 들고 있지 않는다.
        source = null
        clearTempAsync()
        reduce {
            copy(
                step = ImportStep.Saving,
                table = null,
                preview = null,
                mappings = persistentMapOf(),
                saving = SavingProgress(deckName = deckName, totalCards = cards.size),
            )
        }
        ImportSaveSession(coordinator, deck, chunks, saveHost).also { session -> saveSession = session }.start()
    }

    /**
     * 이전 읽기를 취소하고 새 읽기를 시작한다. 읽기를 시작한 쪽에서 [ImportDeckState.loading]을 먼저 정해 입력을 바로 막는다.
     * [read]가 null을 돌려주면 이미 대화상자로 끝낸 것이다.
     */
    private fun startRead(
        read: suspend () -> ParsedTable?,
        onRead: (ParsedTable) -> Unit,
    ) {
        val previous = readJob
        val ticket = nextTicket()
        readJob =
            launch {
                // 취소한 이전 읽기도 블로킹 단계에서는 계속 돌므로, 그 읽기가 끝난 뒤에 시작해야 표와 임시 파일이 겹쳐 쌓이지 않는다.
                previous?.join()
                try {
                    val table = read() ?: return@launch
                    if (ticket == generation) onRead(table)
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (error: Exception) {
                    if (ticket == generation) {
                        val extension = source?.extension.orEmpty()
                        val isPaste = currentState.isPaste
                        // 파서가 알려 준 오류는 사용자 안내로 충분하다. 그 밖의 원인은 기록하되 파일과 붙여 넣은 내용은 남기지 않는다.
                        if (error !is ImportParseException) {
                            Napier.w(
                                throwable = error,
                            ) { "import read failed ext=$extension paste=$isPaste" }
                        }
                        failRead(readFailureDialog(error, extension, isPaste))
                    }
                }
            }
    }

    private fun nextInputVersion(): Int {
        inputVersion += 1
        return inputVersion
    }

    /** 이전 읽기나 미리보기 계산을 취소하고, 늦게 끝난 결과를 버리는 데 쓸 새 번호를 돌려준다. */
    private fun nextTicket(): Int {
        readJob?.cancel()
        generation += 1
        return generation
    }

    // readJob을 비우지 않는다. 취소한 읽기가 끝나기를 다음 읽기와 임시 파일 정리가 기다려야 한다.
    private fun cancelRead() {
        nextTicket()
    }

    private fun failRead(dialog: ImportDialog) {
        source = null
        clearTempAsync()
        reduce { copy(loading = null, isPickerOpen = false, pendingSelection = null, dialog = dialog) }
    }

    // 취소한 읽기가 아직 파일을 쓰고 있을 수 있으므로 그 읽기가 끝난 뒤에 지워야 정리 뒤에 임시 디렉터리가 다시 생기지 않는다.
    private fun clearTempAsync() {
        val reading = readJob
        launch(dispatchers.io) {
            reading?.join()
            tempStorage.clearAll()
        }
    }
}

private fun rejectionSideEffect(code: String): ImportDeckSideEffect =
    if (code in DECK_INPUT_ERROR_CODES) ImportDeckSideEffect.ShowDeckInputError else ImportDeckSideEffect.ShowDeckCreateError

/** 붙여넣기는 입력을 유지한 채 붙여넣기 단계로, 파일은 처음 단계로 돌아간다. */
private fun ImportDeckState.backToEntry(): ImportDeckState =
    ImportDeckState(step = if (isPaste) ImportStep.Paste else ImportStep.Source, isPaste = isPaste, pasteText = pasteText)

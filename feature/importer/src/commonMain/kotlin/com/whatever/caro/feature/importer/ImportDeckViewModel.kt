package com.whatever.caro.feature.importer

import com.whatever.caro.core.importparser.SpreadsheetDateStyle
import com.whatever.caro.core.importparser.model.ImportFormat
import com.whatever.caro.core.importparser.model.ParsedTable
import com.whatever.caro.core.model.deck.Deck
import com.whatever.caro.core.model.deck.DeckInputLimits
import com.whatever.caro.core.viewmodel.BaseViewModel
import com.whatever.caro.core.viewmodel.ExceptionFilter
import com.whatever.caro.feature.importer.logic.SaveOutcomeAction
import com.whatever.caro.feature.importer.logic.classifyPick
import com.whatever.caro.feature.importer.logic.deckInfoPrefill
import com.whatever.caro.feature.importer.logic.importableNoteTypes
import com.whatever.caro.feature.importer.logic.isEmptyImport
import com.whatever.caro.feature.importer.logic.isIgnoredWhileBusy
import com.whatever.caro.feature.importer.logic.isReadFailure
import com.whatever.caro.feature.importer.logic.isSelectableSubdeck
import com.whatever.caro.feature.importer.logic.isSheetEmpty
import com.whatever.caro.feature.importer.logic.mappingBlockReason
import com.whatever.caro.feature.importer.logic.pasteBlockReason
import com.whatever.caro.feature.importer.logic.pickFailedDialog
import com.whatever.caro.feature.importer.logic.previewBlockReason
import com.whatever.caro.feature.importer.logic.readFailureDialog
import com.whatever.caro.feature.importer.logic.resolveSaveOutcome
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
import com.whatever.caro.feature.importer.mvi.SavingStatus
import com.whatever.caro.feature.importer.platform.PickResult
import com.whatever.caro.feature.importer.platform.PickedImportFile
import com.whatever.caro.feature.importer.save.ChunkFailure
import com.whatever.caro.feature.importer.save.DeckCreationResult
import com.whatever.caro.feature.importer.save.ImportChunk
import com.whatever.caro.feature.importer.save.ImportSaveCoordinator
import com.whatever.caro.feature.importer.save.ImportSaveCoordinatorFactory
import com.whatever.caro.feature.importer.save.ImportSaveOutcome
import com.whatever.caro.feature.importer.save.ImportSaveProgress
import com.whatever.caro.feature.importer.save.UserRetry
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
import kotlinx.collections.immutable.toPersistentMap
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

private const val APKG_EXTENSION = "apkg"

// 이름이나 설명 값을 거절한 서버 코드. 그 밖의 거절은 입력을 고쳐도 소용없어 안내 문구가 다르다.
private val DECK_INPUT_ERROR_CODES = setOf("C001", "C002")

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

    // 덱 정보 미리 채움은 읽은 원본마다 한 번이다. 새로 읽으면 다시 채워야 하므로 읽기 성공 시점에 내린다.
    private var isDeckInfoPrefilled = false

    // 저장 흐름 하나(덱 만들기부터 이동까지)가 쓰는 값이다. 묶음 목록과 키는 화면에 보이지 않으므로 상태에 넣지 않는다.
    private var saveCoordinator: ImportSaveCoordinator? = null
    private var savedDeck: Deck? = null
    private var chunks: List<ImportChunk> = emptyList()

    // 이전 저장 호출들이 덱에 저장한 장수다. 다시 시도한 호출의 결과에는 이 값을 더해 쓴다.
    private var savedCards = 0
    private var stopRequested = false

    // 실패 대화상자를 띄운 채 사용자 선택을 기다리는 중인 저장. 이동하거나 다시 시도하면 비워 같은 선택이 두 번 처리되지 않게 한다.
    private var stalledSave: StalledSave? = null

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
            ImportDeckIntent.ClickStop -> requestStop()
            ImportDeckIntent.ConfirmStop -> onConfirmStop()
            ImportDeckIntent.RetrySave -> onRetrySave()
            ImportDeckIntent.ClickViewDeck -> onClickViewDeck()
            ImportDeckIntent.ClickGoHome -> onClickGoHome()
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
            ImportStep.Mapping -> state.table?.let { table -> reduce { copy(step = selectionStep(table.format)) } }
            ImportStep.Preview -> reduce { copy(step = ImportStep.Mapping, preview = null, previewFilter = ImportPreviewFilter.All) }
            ImportStep.DeckInfo -> reduce { copy(step = ImportStep.Preview) }
            ImportStep.Saving -> requestStop()
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
            onRead = ::enterMapping,
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

    /** 항목 이름이 달라진 유형에 옛 연결을 쓰면 없는 항목을 가리켜 빈 카드가 되므로, 유형별 항목 이름이 모두 같을 때만 고친 연결을 이어 쓴다. */
    private fun enterMapping(table: ParsedTable) {
        val fieldNames = importableNoteTypes(table).associate { type -> type.id to type.fieldNames }
        val keepEdits = currentState.mappings.isNotEmpty() && fieldNames == mappedFieldNames
        mappedFieldNames = fieldNames
        reduce {
            copy(
                loading = null,
                step = ImportStep.Mapping,
                table = table,
                mappings = if (keepEdits) mappings else defaultFieldMappings(table).toPersistentMap(),
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
        val mapping = state.mappings[intent.noteTypeId]
        if (state.step != ImportStep.Mapping || mapping == null) return
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
                val preview =
                    withContext(dispatchers.default) {
                        previewBuilder.build(table, state.mappings, emptySideText, limits.maxCards)
                    }
                if (ticket == generation) {
                    reduce { copy(loading = null, step = ImportStep.Preview, preview = preview, previewFilter = ImportPreviewFilter.All) }
                }
            }
    }

    private fun onSelectPreviewFilter(filter: ImportPreviewFilter) {
        if (currentState.step != ImportStep.Preview) return
        reduce { copy(previewFilter = filter) }
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
                reduce { copy(step = ImportStep.DeckInfo, dialog = ImportDialog.DeckCreateFailed) }
            }

            is DeckCreationResult.Rejected -> {
                reduce { copy(step = ImportStep.DeckInfo) }
                postSideEffect(rejectionSideEffect(result.code))
            }
        }
    }

    private fun startSaving(
        coordinator: ImportSaveCoordinator,
        deck: Deck,
        deckName: String,
        cards: List<ImportCard>,
    ) {
        saveCoordinator = coordinator
        savedDeck = deck
        chunks = coordinator.planChunks(cards.map(ImportCard::toCardContent))
        savedCards = 0
        stopRequested = false
        stalledSave = null
        reduce { copy(step = ImportStep.Saving, saving = SavingProgress(deckName = deckName, totalCards = cards.size)) }
        runSave(fromIndex = 0)
    }

    private fun runSave(fromIndex: Int) {
        val coordinator = saveCoordinator ?: return
        val deck = savedDeck ?: return
        val alreadySaved = savedCards
        var savedInRun = 0
        launch {
            val outcome =
                coordinator.saveChunks(
                    deckId = deck.id,
                    chunks = chunks,
                    fromIndex = fromIndex,
                    stopRequested = { stopRequested },
                    onProgress = { progress ->
                        when (progress) {
                            is ImportSaveProgress.ChunkSaved -> {
                                savedInRun += progress.cardCount
                                reduce { withSavingStatus(SavingStatus.Saving, alreadySaved + savedInRun) }
                            }

                            is ImportSaveProgress.Retrying -> {
                                reduce { withSavingStatus(SavingStatus.Retrying) }
                            }

                            is ImportSaveProgress.WaitingForServer -> {
                                reduce { withSavingStatus(SavingStatus.WaitingForServer) }
                            }
                        }
                    },
                )
            onSaveOutcome(outcome, alreadySaved)
        }
    }

    /** 결과의 장수는 이번 호출 몫이다. 조정 객체가 장수 불일치 묶음의 만들어진 장수도 이미 더했으므로 따로 더하지 않는다. */
    private fun onSaveOutcome(
        outcome: ImportSaveOutcome,
        alreadySaved: Int,
    ) {
        savedCards = alreadySaved + outcome.savedCards
        when (val action = resolveSaveOutcome(outcome, alreadySaved, isStopping = stopRequested)) {
            is SaveOutcomeAction.NavigateToDeckDetail -> {
                navigateToDeckDetail(action.saved, action.completed)
            }

            is SaveOutcomeAction.ShowDialog -> {
                stalledSave = (outcome as? ImportSaveOutcome.Stopped)?.let { stopped -> StalledSave(stopped.chunkIndex, stopped.failure) }
                reduce { copy(dialog = action.dialog).withSavingStatus(SavingStatus.Saving, savedCards) }
            }
        }
    }

    private fun navigateToDeckDetail(
        saved: Int,
        completed: Boolean,
    ) {
        val deck = savedDeck ?: return
        stalledSave = null
        postSideEffect(
            ImportDeckSideEffect.NavigateToDeckDetail(
                deck = deck.copy(cardTotalCount = saved),
                saved = saved,
                total = chunks.sumOf { chunk -> chunk.cards.size },
                completed = completed,
            ),
        )
    }

    /** 멈추는 중이거나 다른 대화상자가 떠 있으면 확인을 다시 열지 않는다. */
    private fun requestStop() {
        val state = currentState
        val progress = state.saving ?: return
        if (progress.status == SavingStatus.Stopping || state.dialog != null) return
        reduce { copy(dialog = ImportDialog.StopConfirm) }
    }

    private fun onConfirmStop() {
        if (currentState.dialog != ImportDialog.StopConfirm) return
        stopRequested = true
        reduce { copy(dialog = null).withSavingStatus(SavingStatus.Stopping) }
    }

    /** 새 키가 필요한 실패는 멈춘 묶음만 키를 바꾸고, 같은 키로 보낼 실패는 묶음을 그대로 다시 보낸다. 같은 조정 객체를 써야 첫 전송 시각이 이어진다. */
    private fun onRetrySave() {
        val stalled = stalledSave ?: return
        val coordinator = saveCoordinator ?: return
        val index = stalled.chunkIndex
        val userRetry = stalled.failure.userRetry
        if (userRetry == UserRetry.NONE) return
        if (userRetry == UserRetry.NEW_KEY) chunks = chunks.take(index) + coordinator.renewKey(chunks[index]) + chunks.drop(index + 1)
        stalledSave = null
        reduce { copy(dialog = null).withSavingStatus(SavingStatus.Saving) }
        runSave(fromIndex = index)
    }

    private fun onClickViewDeck() {
        val stalled = stalledSave ?: return
        if (stalled.failure is ChunkFailure.DeckUnavailable) return
        navigateToDeckDetail(savedCards, completed = false)
    }

    private fun onClickGoHome() {
        if (stalledSave?.failure !is ChunkFailure.DeckUnavailable) return
        stalledSave = null
        postSideEffect(ImportDeckSideEffect.NavigateHome)
    }

    /**
     * 이전 읽기를 취소하고 새 읽기를 시작한다. 읽기를 시작한 쪽에서 [ImportDeckState.loading]을 먼저 정해 입력을 바로 막는다.
     * [read]가 null을 돌려주면 이미 대화상자로 끝낸 것이다.
     */
    private fun startRead(
        read: suspend () -> ParsedTable?,
        onRead: (ParsedTable) -> Unit,
    ) {
        val ticket = nextTicket()
        readJob =
            launch {
                try {
                    val table = read() ?: return@launch
                    if (ticket == generation) {
                        isDeckInfoPrefilled = false
                        onRead(table)
                    }
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (error: Exception) {
                    if (ticket == generation) failRead(readFailureDialog(error, source?.extension.orEmpty(), currentState.isPaste))
                }
            }
    }

    /** 이전 읽기나 미리보기 계산을 취소하고, 늦게 끝난 결과를 버리는 데 쓸 새 번호를 돌려준다. */
    private fun nextTicket(): Int {
        readJob?.cancel()
        generation += 1
        return generation
    }

    private fun cancelRead() {
        nextTicket()
        readJob = null
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

/** [chunkIndex] 묶음부터 저장하지 못했다. */
private class StalledSave(
    val chunkIndex: Int,
    val failure: ChunkFailure,
)

/** 멈추는 중에는 진행 이벤트가 상태를 덮어쓰지 않는다. [savedCards]가 null이면 저장한 장수는 그대로 둔다. */
private fun ImportDeckState.withSavingStatus(
    status: SavingStatus,
    savedCards: Int? = null,
): ImportDeckState =
    copy(
        saving =
            saving?.let { progress ->
                progress.copy(
                    savedCards = savedCards ?: progress.savedCards,
                    status = if (progress.status == SavingStatus.Stopping) progress.status else status,
                )
            },
    )

private fun rejectionSideEffect(code: String): ImportDeckSideEffect =
    if (code in DECK_INPUT_ERROR_CODES) ImportDeckSideEffect.ShowDeckInputError else ImportDeckSideEffect.ShowDeckCreateError

/** 붙여넣기는 입력을 유지한 채 붙여넣기 단계로, 파일은 처음 단계로 돌아간다. */
private fun ImportDeckState.backToEntry(): ImportDeckState =
    ImportDeckState(step = if (isPaste) ImportStep.Paste else ImportStep.Source, isPaste = isPaste, pasteText = pasteText)

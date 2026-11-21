package com.whatever.caro.feature.importer

import androidx.lifecycle.viewModelScope
import com.whatever.caro.core.importparser.model.ImportFormat
import com.whatever.caro.core.importparser.model.ParsedSheet
import com.whatever.caro.core.importparser.model.ParsedTable
import com.whatever.caro.core.viewmodel.BaseViewModel
import com.whatever.caro.core.viewmodel.ExceptionFilter
import com.whatever.caro.feature.importer.logic.ImportPreflight
import com.whatever.caro.feature.importer.logic.applyingDeckInfo
import com.whatever.caro.feature.importer.logic.deckDescriptionPrefill
import com.whatever.caro.feature.importer.logic.deckNamePrefill
import com.whatever.caro.feature.importer.logic.filterCards
import com.whatever.caro.feature.importer.logic.nextPasteOverflowed
import com.whatever.caro.feature.importer.logic.noCardsKind
import com.whatever.caro.feature.importer.logic.openingDeckInfo
import com.whatever.caro.feature.importer.logic.pickFailedDialog
import com.whatever.caro.feature.importer.logic.preflight
import com.whatever.caro.feature.importer.logic.readFailureDialog
import com.whatever.caro.feature.importer.logic.showingPreview
import com.whatever.caro.feature.importer.logic.tooLargePickDialog
import com.whatever.caro.feature.importer.logic.withDeckInfoDescription
import com.whatever.caro.feature.importer.logic.withDeckInfoName
import com.whatever.caro.feature.importer.mvi.ImportDeckIntent
import com.whatever.caro.feature.importer.mvi.ImportDeckSideEffect
import com.whatever.caro.feature.importer.mvi.ImportDeckState
import com.whatever.caro.feature.importer.mvi.ImportDialog
import com.whatever.caro.feature.importer.mvi.ImportLoading
import com.whatever.caro.feature.importer.mvi.ImportPreviewFilter
import com.whatever.caro.feature.importer.mvi.ImportSelection
import com.whatever.caro.feature.importer.mvi.ImportStep
import com.whatever.caro.feature.importer.mvi.SettingsEntry
import com.whatever.caro.feature.importer.mvi.SettingsKind
import com.whatever.caro.feature.importer.mvi.dismissingDialog
import com.whatever.caro.feature.importer.mvi.isAcceptedWhileBusy
import com.whatever.caro.feature.importer.platform.PickResult
import com.whatever.caro.feature.importer.save.ImportSaveCoordinatorFactory
import com.whatever.caro.feature.importer.source.ImportDispatchers
import com.whatever.caro.feature.importer.source.ImportFileCopy
import com.whatever.caro.feature.importer.source.ImportLimitsConfig
import com.whatever.caro.feature.importer.source.ImportReadController
import com.whatever.caro.feature.importer.source.ImportReadResult
import com.whatever.caro.feature.importer.source.ImportReadStage
import com.whatever.caro.feature.importer.source.ImportSourceReader
import com.whatever.caro.feature.importer.source.ImportTempStorage
import com.whatever.caro.feature.importer.transform.ImportCard
import com.whatever.caro.feature.importer.transform.ImportPreviewBuilder
import com.whatever.caro.feature.importer.transform.defaultFieldMappings
import io.github.aakira.napier.Napier
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

class ImportDeckViewModel internal constructor(
    sourceReader: ImportSourceReader,
    tempStorage: ImportTempStorage,
    fileCopy: ImportFileCopy,
    private val limits: ImportLimitsConfig,
    private val dispatchers: ImportDispatchers,
    saveCoordinatorFactory: ImportSaveCoordinatorFactory,
    exceptionFilter: ExceptionFilter,
) : BaseViewModel<ImportDeckState, ImportDeckIntent, ImportDeckSideEffect>(ImportDeckState(), exceptionFilter) {
    private val readController = ImportReadController(viewModelScope, sourceReader, tempStorage, fileCopy, limits, dispatchers)
    private val previewBuilder = ImportPreviewBuilder()

    // readController.stop()은 진행 중인 읽기 하나만 무효화한다.
    // 첫 읽기부터 미리보기까지 이어지는 이 처리를 함께 취소해야 멈춘 뒤에 받아 둔 결과로 다음 읽기나 미리보기 계산을 시작하지 않는다.
    private var readFlow: Job? = null

    private val host = Host()
    private val settings = ImportSettingsEditor(host, readController, previewBuilder, limits, dispatchers)
    private val saveSession = ImportSaveSession(host, saveCoordinatorFactory, dispatchers)

    override suspend fun handleIntent(intent: ImportDeckIntent) {
        if (currentState.isBusy && !intent.isAcceptedWhileBusy) return
        when (intent) {
            ImportDeckIntent.ClickBack -> onClickBack()
            ImportDeckIntent.DismissDialog -> reduce { dismissingDialog() }
            ImportDeckIntent.ConfirmDiscard -> onConfirmDiscard()
            ImportDeckIntent.ClickPickFile -> onClickPickFile()
            ImportDeckIntent.ClickPaste -> onClickPaste()
            is ImportDeckIntent.FilePicked -> onFilePicked(intent)
            is ImportDeckIntent.UpdatePasteText -> onUpdatePasteText(intent.text)
            is ImportDeckIntent.ClickPasteNext -> onClickPasteNext(intent)
            is ImportDeckIntent.SelectPreviewFilter -> onSelectPreviewFilter(intent.filter)
            is ImportDeckIntent.ClickCard -> onClickCard(intent.card)
            ImportDeckIntent.CloseCardDetail -> reduce { copy(detailCard = null) }
            ImportDeckIntent.DismissDelimiterBanner -> reduce { copy(isDelimiterBannerDismissed = true) }
            is ImportDeckIntent.ClickChangeSetting -> settings.openFromPreview(intent.kind)
            is ImportDeckIntent.ToggleSubdeck -> settings.toggleSubdeck(intent.id)
            ImportDeckIntent.ToggleAllSubdecks -> settings.toggleAllSubdecks()
            is ImportDeckIntent.SelectSheet -> settings.selectSheet(intent.index)
            is ImportDeckIntent.SelectDelimiter -> settings.selectDelimiter(intent.delimiter)
            is ImportDeckIntent.ChangeHeaderRow -> settings.changeHeaderRow(intent.hasHeaderRow)
            is ImportDeckIntent.ToggleNoteType -> settings.toggleNoteType(intent.noteTypeId)
            is ImportDeckIntent.ToggleField -> settings.toggleField(intent.noteTypeId, intent.target, intent.fieldName)
            ImportDeckIntent.ClickApplySettings -> settings.apply()
            ImportDeckIntent.ClickChangeDeckInfo -> reduce { openingDeckInfo() }
            is ImportDeckIntent.UpdateDeckInfoName -> reduce { withDeckInfoName(intent.name) }
            is ImportDeckIntent.UpdateDeckInfoDescription -> reduce { withDeckInfoDescription(intent.description) }
            ImportDeckIntent.ApplyDeckInfo -> reduce { applyingDeckInfo() }
            ImportDeckIntent.DismissDeckInfo -> reduce { copy(deckInfoDraft = null) }
            ImportDeckIntent.ClickImport -> saveSession.startImport()
            ImportDeckIntent.RetryCreateDeck -> saveSession.retryCreateDeck()
            ImportDeckIntent.ClickStop -> saveSession.requestStop()
            ImportDeckIntent.ConfirmStop -> saveSession.confirmStop()
            ImportDeckIntent.RetrySave -> saveSession.retry()
            ImportDeckIntent.ClickViewDeck -> saveSession.viewDeck()
            ImportDeckIntent.ClickGoHome -> saveSession.goHome()
        }
    }

    override fun onCleared() {
        readController.close()
        super.onCleared()
    }

    /** 설정 화면의 처리는 원본을 남긴 채 그 처리만 멈추고, 첫 읽기는 원본까지 버린다. */
    private fun onClickBack() {
        val state = currentState
        when {
            state.dialog != null -> reduce { dismissingDialog() }
            state.detailCard != null -> reduce { copy(detailCard = null) }
            state.deckInfoDraft != null -> reduce { copy(deckInfoDraft = null) }
            state.loading == ImportLoading.CreatingDeck -> Unit
            state.step is ImportStep.Settings && (state.loading != null || state.pendingDraftSelection != null) -> settings.stop()
            state.loading != null -> discardSource()
            else -> goBackFromStep(state)
        }
    }

    private fun goBackFromStep(state: ImportDeckState) {
        when (val step = state.step) {
            ImportStep.Source -> {
                postSideEffect(ImportDeckSideEffect.NavigateBack)
            }

            ImportStep.Paste -> {
                reduce { copy(step = ImportStep.Source, isPaste = false) }
            }

            ImportStep.Preview -> {
                if (state.isPaste) discardSource() else reduce { copy(dialog = ImportDialog.DiscardConfirm) }
            }

            is ImportStep.Settings -> {
                when (step.entry) {
                    SettingsEntry.FromPreview -> settings.leave()
                    SettingsEntry.Gate -> reduce { copy(dialog = ImportDialog.DiscardConfirm) }
                }
            }

            ImportStep.Saving -> {
                saveSession.requestStop()
            }
        }
    }

    private fun onConfirmDiscard() {
        if (currentState.dialog == ImportDialog.DiscardConfirm) discardSource()
    }

    private fun onClickPickFile() {
        if (currentState.step != ImportStep.Source) return
        reduce { copy(isPickerOpen = true) }
        postSideEffect(ImportDeckSideEffect.OpenFilePicker)
    }

    private fun onClickPaste() {
        if (currentState.step == ImportStep.Source) reduce { copy(step = ImportStep.Paste, isPaste = true) }
    }

    private fun onClickCard(card: ImportCard) {
        if (currentState.step == ImportStep.Preview && currentState.deckInfoDraft == null) reduce { copy(detailCard = card) }
    }

    private suspend fun onFilePicked(intent: ImportDeckIntent.FilePicked) {
        val state = currentState
        // 프로세스가 죽은 뒤 다시 전달된 결과도 받아야 하므로 isPickerOpen이 아니라 단계와 처리 중 여부로 거른다.
        if (state.step != ImportStep.Source || state.loading != null || state.dialog != null) {
            (intent.result as? PickResult.Picked)?.let { picked -> withContext(NonCancellable + dispatchers.io) { picked.file.discard() } }
            return
        }
        when (val result = intent.result) {
            PickResult.Cancelled -> {
                reduce { copy(isPickerOpen = false) }
            }

            is PickResult.TooLarge -> {
                reduce { copy(isPickerOpen = false, dialog = tooLargePickDialog(result.displayName)) }
            }

            is PickResult.Failed -> {
                Napier.w(throwable = result.cause) { "import file pick failed" }
                reduce { copy(isPickerOpen = false, dialog = pickFailedDialog()) }
            }

            is PickResult.Picked -> {
                startRead(ImportLoading.ReadingFile(fileName = null), intent.emptySideText, intent.descriptionTemplate) {
                    readController.readPickedFile(result.file, intent.dateStyle, onNamed = ::showFileName)
                }
            }
        }
    }

    private fun showFileName(name: String) {
        if (currentState.loading is ImportLoading.ReadingFile) reduce { copy(loading = ImportLoading.ReadingFile(name)) }
    }

    private fun onUpdatePasteText(text: String) {
        if (currentState.step != ImportStep.Paste) return
        // 상한을 1자 넘긴 값까지만 두어도 길이 초과 판정은 같고, 거대한 클립보드 글이 레이아웃과 메모리를 잡지 않는다.
        reduce {
            copy(
                pasteText = text.take(limits.maxPasteChars + 1),
                isPasteOverflowed = nextPasteOverflowed(pasteText, isPasteOverflowed, text, limits.maxPasteChars),
            )
        }
    }

    private fun onClickPasteNext(intent: ImportDeckIntent.ClickPasteNext) {
        val state = currentState
        if (state.step != ImportStep.Paste || !state.isPasteNextEnabled) return
        startRead(ImportLoading.ReadingPaste, intent.emptySideText, intent.description) {
            readController.readPaste(state.pasteText, intent.sourceName, intent.dateStyle)
        }
    }

    private fun onSelectPreviewFilter(filter: ImportPreviewFilter) {
        val preview = currentState.preview ?: return
        reduce { copy(previewFilter = filter, filteredCards = filterCards(preview.cards, filter)) }
    }

    /** 첫 일시 중단 전에 고른 파일을 읽기에 넘기도록 바로 실행한다. 넘기기 전에 멈추면 선택기가 남긴 사본을 정리할 곳이 없다. */
    private fun startRead(
        loading: ImportLoading,
        emptySideText: String,
        descriptionText: String,
        read: suspend () -> ImportReadResult,
    ) {
        reduce { copy(loading = loading, isPickerOpen = false, emptySideText = emptySideText) }
        readFlow = launch(start = CoroutineStart.UNDISPATCHED) { judgeFirstRead(read(), emptySideText, descriptionText) }
    }

    private suspend fun judgeFirstRead(
        result: ImportReadResult,
        emptySideText: String,
        descriptionText: String,
    ) {
        val table = tableOrFail(result) ?: return
        // 설명 기본값은 설정과 무관하므로 먼저 고칠 것 화면을 거쳐 미리보기를 처음 여는 경우에도 쓰도록 여기서 한 번 채운다.
        reduce { copy(deckDescription = deckDescriptionPrefill(table.sourceName, isPaste, descriptionText)) }
        when (val check = preflight(table, limits.maxCards)) {
            ImportPreflight.Ok -> {
                openPreview(fullTable(table) ?: return, sheetIndex = 0, emptySideText)
            }

            is ImportPreflight.PickSheet -> {
                openFirstDataSheet(table, check.candidates, emptySideText)
            }

            ImportPreflight.SubdeckFirst -> {
                val allSubdecks = table.subdecks.map { subdeck -> subdeck.id }.toImmutableSet()
                settings.openGate(SettingsKind.Subdeck, table, ImportSelection(subdeckIds = allSubdecks))
            }

            ImportPreflight.SheetFirst -> {
                settings.openGate(SettingsKind.Sheet, table, ImportSelection(sheetIndex = 0))
            }

            ImportPreflight.TooMany -> {
                discardSource(ImportDialog.TooManyCards(currentState.isPaste))
            }
        }
    }

    /** Anki 첫 읽기는 하위 덱 요약만 읽으므로 로딩 오버레이를 유지한 채 모든 하위 덱의 행을 한 번 더 읽는다. */
    private suspend fun fullTable(table: ParsedTable): ParsedTable? =
        if (table.format == ImportFormat.APKG) tableOrFail(readController.reread(ImportReadStage.First)) else table

    /** 시트 행 수는 시트를 읽지 않고 센 값이라 다시 읽으면 0행일 수 있다. 데이터 행이 있는 시트가 없으면 카드 0장 판정으로 넘긴다. */
    private suspend fun openFirstDataSheet(
        table: ParsedTable,
        candidates: List<ParsedSheet>,
        emptySideText: String,
    ) {
        for (sheet in candidates) {
            val reread = tableOrFail(readController.reread(ImportReadStage.First, sheetIndex = sheet.index)) ?: return
            if (reread.rows.isEmpty()) continue
            when (preflight(reread, limits.maxCards, sheet.index)) {
                ImportPreflight.Ok -> openPreview(reread, sheet.index, emptySideText)
                ImportPreflight.SheetFirst -> settings.openGate(SettingsKind.Sheet, reread, ImportSelection(sheetIndex = sheet.index))
                else -> discardSource(ImportDialog.TooManyCards(currentState.isPaste))
            }
            return
        }
        openPreview(table, sheetIndex = 0, emptySideText)
    }

    /**
     * OutOfMemoryError 같은 Error도 받아야 로딩 오버레이가 걷힌다. 코루틴 예외 처리기는 기록만 남겨 뒤로 가기 말고는 입력이 모두 막힌다.
     * Main에서 하는 기본 연결, 칩별 카드 수, 덱 이름 계산도 같은 이유로 이 안에서 받는다.
     */
    private suspend fun openPreview(
        table: ParsedTable,
        sheetIndex: Int,
        emptySideText: String,
    ) {
        try {
            showPreview(table, sheetIndex, emptySideText)
        } catch (cancellation: CancellationException) {
            // 이 처리가 취소되지 않았는데 계산 안쪽에서 나온 취소를 다시 던지면 로딩 오버레이가 남는다.
            currentCoroutineContext().ensureActive()
            failPreview(cancellation)
        } catch (error: Throwable) {
            failPreview(error)
        }
    }

    private suspend fun showPreview(
        table: ParsedTable,
        sheetIndex: Int,
        emptySideText: String,
    ) {
        val mappings = defaultFieldMappings(table)
        val preview = withContext(dispatchers.default) { previewBuilder.build(table, mappings, emptySideText, limits.maxCards) }
        val noCards = noCardsKind(table, preview)
        if (noCards != null) {
            discardSource(ImportDialog.NoCards(noCards))
            return
        }
        val selection = ImportSelection(subdeckIds = table.subdecks.map { subdeck -> subdeck.id }.toImmutableSet(), sheetIndex = sheetIndex)
        reduce { showingPreview(table, selection, mappings, preview, deckNamePrefill(table, isPaste, selection)) }
    }

    private fun failPreview(error: Throwable) {
        Napier.e(throwable = error) { "import preview build failed" }
        discardSource(readFailureDialog(error, readController.source?.extension.orEmpty(), currentState.isPaste))
    }

    /** 멈춘 읽기의 결과는 새 처리의 화면을 덮어쓰지 않게 버린다. */
    private fun tableOrFail(result: ImportReadResult): ParsedTable? =
        when (result) {
            is ImportReadResult.Success -> {
                result.table
            }

            is ImportReadResult.Failure -> {
                discardSource(readFailureDialog(result.error, result.extension, currentState.isPaste))
                null
            }

            ImportReadResult.Cancelled -> {
                null
            }
        }

    /**
     * 읽기를 멈추고 원본과 임시 파일을 버린다. 붙여넣기는 입력을 남긴 채 Paste로, 파일은 Source로 간다.
     * [readFlow] 안에서 부르면 부른 코루틴도 함께 취소되므로, 부른 뒤에 일시 중단하는 코드를 두면 그 지점에서 멈춰 뒤가 실행되지 않는다.
     */
    private fun discardSource(dialog: ImportDialog? = null) {
        readFlow?.cancel()
        settings.cancelWork()
        readController.discard()
        reduce {
            ImportDeckState(
                step = if (isPaste) ImportStep.Paste else ImportStep.Source,
                isPaste = isPaste,
                pasteText = pasteText,
                isPasteOverflowed = isPasteOverflowed,
                dialog = dialog,
            )
        }
    }

    private inner class Host :
        ImportSettingsHost,
        ImportSaveHost {
        override val state: ImportDeckState get() = currentState

        override fun reduce(transform: ImportDeckState.() -> ImportDeckState) = this@ImportDeckViewModel.reduce(transform)

        override fun postSideEffect(effect: ImportDeckSideEffect) = this@ImportDeckViewModel.postSideEffect(effect)

        override fun launch(block: suspend () -> Unit): Job = this@ImportDeckViewModel.launch { block() }

        override fun releaseSource() = readController.discard()
    }
}

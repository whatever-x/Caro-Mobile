package com.whatever.caro.feature.importer.support

import com.whatever.caro.core.data.repository.card.CardRepository
import com.whatever.caro.core.data.repository.deck.DeckRepository
import com.whatever.caro.core.importparser.ImportParser
import com.whatever.caro.core.importparser.SpreadsheetDateStyle
import com.whatever.caro.core.viewmodel.ExceptionFilter
import com.whatever.caro.feature.importer.ImportDeckViewModel
import com.whatever.caro.feature.importer.mvi.ImportDeckIntent
import com.whatever.caro.feature.importer.mvi.ImportDeckState
import com.whatever.caro.feature.importer.platform.PickResult
import com.whatever.caro.feature.importer.platform.PickedImportFile
import com.whatever.caro.feature.importer.save.ImportSaveCoordinator
import com.whatever.caro.feature.importer.save.ImportSaveCoordinatorFactory
import com.whatever.caro.feature.importer.source.ImportDispatchers
import com.whatever.caro.feature.importer.source.ImportLimitsConfig
import com.whatever.caro.feature.importer.source.ParserImportSourceReader
import dev.mokkery.mock
import io.kotest.core.spec.style.FunSpec
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import okio.FileSystem

internal class ImportViewModelHarness(
    val viewModel: ImportDeckViewModel,
    val reader: FakeImportSourceReader,
    val tempStorage: FakeImportTempStorage,
    val deckRepository: DeckRepository,
    val cardRepository: CardRepository,
) {
    val state: ImportDeckState get() = viewModel.state.value

    fun send(intent: ImportDeckIntent) = viewModel.intent(intent)

    fun pickResult(
        result: PickResult,
        dateStyle: SpreadsheetDateStyle = SpreadsheetDateStyle.YEAR_MONTH_DAY,
    ) = send(ImportDeckIntent.FilePicked(result, dateStyle))

    fun pick(
        file: PickedImportFile,
        dateStyle: SpreadsheetDateStyle = SpreadsheetDateStyle.YEAR_MONTH_DAY,
    ) = pickResult(PickResult.Picked(file), dateStyle)

    fun pasteNext(
        text: String,
        sourceName: String = PASTE_SOURCE_NAME,
        dateStyle: SpreadsheetDateStyle = SpreadsheetDateStyle.YEAR_MONTH_DAY,
    ) {
        send(ImportDeckIntent.ClickPaste)
        send(ImportDeckIntent.UpdatePasteText(text))
        send(ImportDeckIntent.ClickPasteNext(sourceName, dateStyle))
    }

    companion object {
        const val PASTE_SOURCE_NAME = "붙여 넣은 카드"
    }
}

internal object ImportViewModelFactory {
    fun create(
        dispatcher: CoroutineDispatcher,
        reader: FakeImportSourceReader = FakeImportSourceReader(),
        tempStorage: FakeImportTempStorage = FakeImportTempStorage.create(),
        limits: ImportLimitsConfig = ImportLimitsConfig(),
        deckRepository: DeckRepository = mock<DeckRepository>(),
        cardRepository: CardRepository = mock<CardRepository>(),
        defaultDispatcher: CoroutineDispatcher = dispatcher,
        saveCoordinatorFactory: ImportSaveCoordinatorFactory? = null,
        exceptionFilter: ExceptionFilter = ExceptionFilter.None,
    ): ImportViewModelHarness =
        ImportViewModelHarness(
            viewModel =
                ImportDeckViewModel(
                    sourceReader = reader,
                    tempStorage = tempStorage.storage,
                    fileCopy = tempStorage.fileCopy,
                    limits = limits,
                    dispatchers = ImportDispatchers(io = dispatcher, default = defaultDispatcher),
                    saveCoordinatorFactory =
                        saveCoordinatorFactory ?: ImportSaveCoordinatorFactory { ImportSaveCoordinator(deckRepository, cardRepository) },
                    exceptionFilter = exceptionFilter,
                ),
            reader = reader,
            tempStorage = tempStorage,
            deckRepository = deckRepository,
            cardRepository = cardRepository,
        )
}

/**
 * Main, io, default를 한 테스트 디스패처로 묶어 advanceUntilIdle 하나로 모든 작업을 가상 시간에 맞춘다.
 * 실제 Dispatchers.IO나 Default를 쓰면 대기 중인 읽기가 언제 끝날지 알 수 없어 순서가 흔들린다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class ImportViewModelRig {
    val dispatcher = StandardTestDispatcher()
    private val sandboxes = mutableListOf<FakeImportTempStorage>()

    fun installOn(spec: FunSpec) {
        spec.beforeTest { Dispatchers.setMain(dispatcher) }
        spec.afterTest {
            Dispatchers.resetMain()
            sandboxes.forEach(FakeImportTempStorage::dispose)
            sandboxes.clear()
        }
    }

    fun create(
        reader: FakeImportSourceReader = FakeImportSourceReader(),
        tempStorage: FakeImportTempStorage = FakeImportTempStorage.create(),
        limits: ImportLimitsConfig = ImportLimitsConfig(),
        deckRepository: DeckRepository = mock<DeckRepository>(),
        cardRepository: CardRepository = mock<CardRepository>(),
        defaultDispatcher: CoroutineDispatcher = dispatcher,
        saveCoordinatorFactory: ImportSaveCoordinatorFactory? = null,
        exceptionFilter: ExceptionFilter = ExceptionFilter.None,
        ioDispatcher: CoroutineDispatcher = dispatcher,
    ): ImportViewModelHarness {
        sandboxes += tempStorage
        return ImportViewModelFactory.create(
            ioDispatcher,
            reader,
            tempStorage,
            limits,
            deckRepository,
            cardRepository,
            defaultDispatcher,
            saveCoordinatorFactory,
            exceptionFilter,
        )
    }

    /** 텍스트와 xlsx를 실제 파서로 읽는 리더다. Android 호스트에는 Anki용 네이티브가 없어 apkg는 이 리더로 읽지 않는다. */
    fun realParserReader(limits: ImportLimitsConfig = ImportLimitsConfig()): FakeImportSourceReader =
        FakeImportSourceReader(fallback = ParserImportSourceReader(ImportParser(FileSystem.SYSTEM), limits))
}

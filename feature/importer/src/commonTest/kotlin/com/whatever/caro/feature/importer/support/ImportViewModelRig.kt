package com.whatever.caro.feature.importer.support

import com.whatever.caro.core.importparser.ImportParser
import com.whatever.caro.core.importparser.SpreadsheetDateStyle
import com.whatever.caro.core.viewmodel.ExceptionFilter
import com.whatever.caro.feature.importer.ImportDeckViewModel
import com.whatever.caro.feature.importer.mvi.ImportDeckIntent
import com.whatever.caro.feature.importer.mvi.ImportDeckState
import com.whatever.caro.feature.importer.platform.PickResult
import com.whatever.caro.feature.importer.platform.PickedImportFile
import com.whatever.caro.feature.importer.source.ImportDispatchers
import com.whatever.caro.feature.importer.source.ImportLimitsConfig
import com.whatever.caro.feature.importer.source.ParserImportSourceReader
import io.kotest.core.spec.style.FunSpec
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import okio.FileSystem

internal class ImportViewModelHarness(
    val viewModel: ImportDeckViewModel,
    val reader: FakeImportSourceReader,
    val tempStorage: FakeImportTempStorage,
) {
    val state: ImportDeckState get() = viewModel.state.value

    fun send(intent: ImportDeckIntent) = viewModel.intent(intent)

    fun pickResult(result: PickResult) = send(ImportDeckIntent.FilePicked(result, DATE_STYLE, EMPTY_SIDE_TEXT))

    fun pick(file: PickedImportFile) = pickResult(PickResult.Picked(file))

    fun pickNamed(fileName: String) = pick(FakePickedImportFile.ofSize(fileName, size = 100))

    fun pasteNext(text: String) {
        send(ImportDeckIntent.ClickPaste)
        send(ImportDeckIntent.UpdatePasteText(text))
        send(ImportDeckIntent.ClickPasteNext(PASTE_SOURCE_NAME, DATE_STYLE, EMPTY_SIDE_TEXT))
    }

    companion object {
        const val PASTE_SOURCE_NAME = "붙여 넣은 카드"
        const val EMPTY_SIDE_TEXT = "(비어 있음)"
        val DATE_STYLE = SpreadsheetDateStyle.YEAR_MONTH_DAY
    }
}

/**
 * Main, io, default를 한 테스트 디스패처로 묶어 advanceUntilIdle 하나로 모든 작업을 가상 시간에 맞춘다.
 * 실제 Dispatchers.IO나 Default를 쓰면 대기 중인 읽기가 언제 끝날지 알 수 없어 순서가 흔들린다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class ImportViewModelRig {
    // 테스트마다 새로 만들어 앞 테스트에서 열지 않은 게이트에 걸린 작업이 다음 테스트의 가상 시간에 끼지 않게 한다.
    lateinit var dispatcher: TestDispatcher
        private set
    private val sandboxes = mutableListOf<FakeImportTempStorage>()

    fun installOn(spec: FunSpec) {
        spec.beforeTest {
            dispatcher = StandardTestDispatcher()
            Dispatchers.setMain(dispatcher)
        }
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
        io: CoroutineDispatcher = dispatcher,
        default: CoroutineDispatcher = dispatcher,
    ): ImportViewModelHarness {
        sandboxes += tempStorage
        val viewModel =
            ImportDeckViewModel(
                sourceReader = reader,
                tempStorage = tempStorage.storage,
                fileCopy = tempStorage.fileCopy,
                limits = limits,
                dispatchers = ImportDispatchers(io = io, default = default),
                exceptionFilter = ExceptionFilter.None,
            )
        return ImportViewModelHarness(viewModel, reader, tempStorage)
    }

    /** 텍스트와 xlsx를 실제 파서로 읽는 리더다. Android 호스트에는 Anki용 네이티브가 없어 apkg는 이 리더로 읽지 않는다. */
    fun realParserReader(limits: ImportLimitsConfig = ImportLimitsConfig()): FakeImportSourceReader =
        FakeImportSourceReader(fallback = ParserImportSourceReader(ImportParser(FileSystem.SYSTEM), limits))
}

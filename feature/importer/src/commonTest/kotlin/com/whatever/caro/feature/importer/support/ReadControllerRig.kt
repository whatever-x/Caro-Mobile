package com.whatever.caro.feature.importer.support

import com.whatever.caro.core.importparser.ImportParser
import com.whatever.caro.core.importparser.SpreadsheetDateStyle
import com.whatever.caro.feature.importer.platform.PickedImportFile
import com.whatever.caro.feature.importer.source.ImportDispatchers
import com.whatever.caro.feature.importer.source.ImportLimitsConfig
import com.whatever.caro.feature.importer.source.ImportReadController
import com.whatever.caro.feature.importer.source.ImportReadResult
import com.whatever.caro.feature.importer.source.ImportReadStage
import com.whatever.caro.feature.importer.source.ParserImportSourceReader
import io.kotest.core.spec.style.FunSpec
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import okio.FileSystem

/** 테스트가 ViewModel 자리에서 읽기를 부른다. 결과를 기다리는 동안 다른 호출을 보낼 수 있게 호출마다 따로 코루틴을 띄운다. */
internal class ReadControllerHarness(
    val controller: ImportReadController,
    val reader: FakeImportSourceReader,
    val tempStorage: FakeImportTempStorage,
    private val callerScope: CoroutineScope,
) {
    fun pick(
        file: PickedImportFile,
        onNamed: (String) -> Unit = {},
    ): Deferred<ImportReadResult> = callerScope.async { controller.readPickedFile(file, DATE_STYLE, onNamed) }

    fun paste(text: String): Deferred<ImportReadResult> = callerScope.async { controller.readPaste(text, PASTE_SOURCE_NAME, DATE_STYLE) }

    fun reread(
        stage: ImportReadStage,
        subdeckIds: Set<String>? = null,
        sheetIndex: Int = 0,
        delimiter: Char? = null,
        hasHeaderRow: Boolean? = null,
    ): Deferred<ImportReadResult> = callerScope.async { controller.reread(stage, subdeckIds, sheetIndex, delimiter, hasHeaderRow) }

    companion object {
        const val PASTE_SOURCE_NAME = "붙여 넣은 카드"
        val DATE_STYLE = SpreadsheetDateStyle.YEAR_MONTH_DAY
    }
}

/**
 * scope, io, default를 한 테스트 디스패처로 묶어 advanceUntilIdle 하나로 모든 작업을 가상 시간에 맞춘다.
 * 실제 Dispatchers.IO나 Default를 쓰면 대기 중인 읽기가 언제 끝날지 알 수 없어 순서가 흔들린다.
 * 읽기와 호출은 TestScope 밖의 scope에 둔다. TestScope의 자식이면 열지 않은 게이트에 테스트가 끝나지 않고, backgroundScope의 작업은 advanceUntilIdle이 돌리지 않는다.
 */
internal class ReadControllerRig {
    // 테스트마다 새로 만들어 앞 테스트가 남긴 예약 작업과 가상 시간이 다음 테스트로 넘어가지 않게 한다.
    lateinit var dispatcher: TestDispatcher
        private set
    private val sandboxes = mutableListOf<FakeImportTempStorage>()
    private val scopes = mutableListOf<CoroutineScope>()

    fun installOn(spec: FunSpec) {
        spec.beforeTest { dispatcher = StandardTestDispatcher() }
        spec.afterTest {
            scopes.forEach { scope -> scope.cancel() }
            scopes.clear()
            sandboxes.forEach(FakeImportTempStorage::dispose)
            sandboxes.clear()
        }
    }

    fun newScope(scopeDispatcher: CoroutineDispatcher = dispatcher): CoroutineScope =
        CoroutineScope(scopeDispatcher + SupervisorJob()).also { scope -> scopes += scope }

    fun create(
        reader: FakeImportSourceReader = FakeImportSourceReader(),
        tempStorage: FakeImportTempStorage = FakeImportTempStorage.create(),
        limits: ImportLimitsConfig = ImportLimitsConfig(),
        io: CoroutineDispatcher = dispatcher,
        default: CoroutineDispatcher = dispatcher,
        scope: CoroutineScope = newScope(),
        callerScope: CoroutineScope = scope,
    ): ReadControllerHarness {
        sandboxes += tempStorage
        val controller =
            ImportReadController(
                scope = scope,
                sourceReader = reader,
                tempStorage = tempStorage.storage,
                fileCopy = tempStorage.fileCopy,
                limits = limits,
                dispatchers = ImportDispatchers(io = io, default = default),
            )
        return ReadControllerHarness(controller, reader, tempStorage, callerScope)
    }

    /** 텍스트와 xlsx를 실제 파서로 읽는 리더다. Android 호스트에는 Anki용 네이티브가 없어 apkg는 이 리더로 읽지 않는다. */
    fun realParserReader(limits: ImportLimitsConfig = ImportLimitsConfig()): FakeImportSourceReader =
        FakeImportSourceReader(fallback = ParserImportSourceReader(ImportParser(FileSystem.SYSTEM), limits))
}

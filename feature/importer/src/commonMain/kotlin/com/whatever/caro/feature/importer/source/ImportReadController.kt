package com.whatever.caro.feature.importer.source

import com.whatever.caro.core.importparser.ImportParseErrorKind
import com.whatever.caro.core.importparser.ImportParseException
import com.whatever.caro.core.importparser.ImportSourceName
import com.whatever.caro.core.importparser.SpreadsheetDateStyle
import com.whatever.caro.core.importparser.model.ParsedTable
import com.whatever.caro.feature.importer.logic.sanitizeDisplayName
import com.whatever.caro.feature.importer.platform.PickedImportFile
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val APKG_EXTENSION = "apkg"

internal sealed interface ImportReadResult {
    data class Success(
        val table: ParsedTable,
    ) : ImportReadResult

    /** 읽기 전에 막은 파일도 파서와 같은 [ImportParseException]으로 담아 한 가지 규칙으로 안내 문구를 고르게 한다. */
    data class Failure(
        val error: Throwable,
        val extension: String,
    ) : ImportReadResult

    /** 멈췄거나 더 새 읽기가 시작돼 쓰지 않는 결과다. 받은 쪽이 상태를 바꾸면 새 처리의 화면을 덮어쓴다. */
    data object Cancelled : ImportReadResult
}

/** 실패했을 때 원본을 정리할지 가른다. */
internal enum class ImportReadStage {
    /** 첫 읽기와, 그 결과로 판정하려고 같은 원본을 이어 읽는 경우다. 실패하면 원본과 임시 파일을 정리한다. */
    First,

    /** 설정 화면이 초안을 위해 다시 읽는 경우다. 사용자가 그 화면에 머물러 다시 시도하므로 실패해도 원본을 남긴다. */
    Draft,
}

/**
 * 상태를 lock 없이 다루므로 공개 함수는 [scope]의 dispatcher(Main)에서만 부른다. io와 default에서는 블로킹 작업만 하고 상태를 건드리지 않는다.
 * 읽기 작업은 호출한 코루틴이 아니라 [scope]에 둔다. 호출한 코루틴에 두면 같은 코루틴 안에서 이어 읽을 때 새 읽기가 자기 자신을 기다리며 끝나지 않는다.
 * [stop]은 그때 진행 중인 읽기 하나만 무효화한다. 결과를 받은 뒤 같은 원본을 이어 읽는 호출한 쪽은 그 사이 사용자가 멈췄는지 자기 상태를 다시 확인해야 한다. 확인하지 않으면 멈춘 뒤에 새 읽기가 시작된다.
 */
internal class ImportReadController(
    private val scope: CoroutineScope,
    private val sourceReader: ImportSourceReader,
    private val tempStorage: ImportTempStorage,
    private val fileCopy: ImportFileCopy,
    private val limits: ImportLimitsConfig,
    private val dispatchers: ImportDispatchers,
) {
    var source: ImportReadSource? = null
        private set

    // 취소한 읽기도 끝날 때까지 비우지 않는다. 다음 읽기와 임시 파일 정리가 그 읽기를 기다려야 한다.
    private var readJob: Job? = null

    // 다음 읽기가 기다릴 마지막 정리다. 정리끼리도 차례로 돌아야 다음 읽기가 마지막 정리 하나만 기다려도 된다.
    private var cleanupJob: Job? = null
    private var generation = 0

    init {
        // 이 화면의 디렉터리는 건드리지 않으므로 첫 복사와 겹치지 않는다.
        scope.launch(dispatchers.io) { clearIgnoringFailure(tempStorage::clearStale) }
    }

    suspend fun readPickedFile(
        file: PickedImportFile,
        dateStyle: SpreadsheetDateStyle,
    ): ImportReadResult {
        discardSource()
        return startRead(
            stage = ImportReadStage.First,
            // 앞선 읽기를 기다리다 취소되면 copyAndRead의 정리에 닿지 못해 선택기가 남긴 사본이 남는다.
            onCancelledBeforeRead = { withContext(dispatchers.io) { file.discard() } },
        ) { copyAndRead(file, dateStyle) }
    }

    suspend fun readPaste(
        text: String,
        sourceName: String,
        dateStyle: SpreadsheetDateStyle,
    ): ImportReadResult {
        val paste = ImportReadSource.Paste(text, sourceName, dateStyle)
        discardSource()
        source = paste
        return startRead(ImportReadStage.First) { ImportReadResult.Success(paste.read(sourceReader, dispatchers)) }
    }

    /** 원본이 없을 때 돌려주는 [ImportReadResult.Cancelled]는 멈춘 읽기와 구분되지 않으므로, 로딩 오버레이를 띄우기 전에 [source]가 있는지 확인한다. */
    suspend fun reread(
        stage: ImportReadStage,
        subdeckIds: Set<String>? = null,
        sheetIndex: Int = 0,
        delimiter: Char? = null,
        hasHeaderRow: Boolean? = null,
    ): ImportReadResult {
        val read = source ?: return ImportReadResult.Cancelled
        return startRead(stage) {
            ImportReadResult.Success(read.read(sourceReader, dispatchers, subdeckIds, sheetIndex, delimiter, hasHeaderRow))
        }
    }

    /**
     * 원본은 남긴다. 다시 읽기를 멈춘 뒤 사용자가 같은 화면에서 이어서 바꿀 수 있어야 하기 때문이다.
     * 고른 파일의 복사가 끝나기 전에 멈추면 원본으로 쓸 사본이 없으므로 원본이 비워진다.
     */
    fun stop() {
        readJob?.cancel()
        generation += 1
    }

    fun discard() {
        stop()
        discardSource()
    }

    /**
     * [scope]가 이미 취소된 뒤에도 불린다. 취소된 scope의 자식은 시작하지 못하므로 정리를 부모 없는 scope에 띄운다.
     * 이 정리는 취소할 곳이 없어, 취소에 응하지 않는 읽기가 끝날 때까지 기다렸다가 임시 디렉터리를 지우고 끝난다.
     */
    fun close() {
        stop()
        source = null
        val reading = readJob
        val cleaning = cleanupJob
        CoroutineScope(dispatchers.io).launch {
            listOfNotNull(reading, cleaning).joinAll()
            clearIgnoringFailure(tempStorage::clearAll)
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private suspend fun startRead(
        stage: ImportReadStage,
        onCancelledBeforeRead: suspend () -> Unit = {},
        read: suspend () -> ImportReadResult,
    ): ImportReadResult {
        val pending = listOfNotNull(readJob, cleanupJob)
        stop()
        val ticket = generation
        // 시작 전에 취소돼도 몸체가 돌아야 앞 작업을 기다리고 선택기 사본을 정리한다. 기본 시작이면 몸체 없이 바로 끝나 다음 읽기가 앞 작업과 겹친다.
        val job =
            scope.async(start = CoroutineStart.ATOMIC) {
                try {
                    awaitTempFileWork(pending)
                } catch (cancellation: CancellationException) {
                    withContext(NonCancellable) { onCancelledBeforeRead() }
                    throw cancellation
                }
                val result = readCatching(read)
                if (ticket == generation && result is ImportReadResult.Failure) onFailure(result, stage)
                result
            }
        readJob = job
        return awaitLatest(job, ticket)
    }

    /**
     * 취소한 읽기도 블로킹 단계에서는 계속 돌고 정리는 root 전체를 지우므로, 둘 다 끝난 뒤에 시작해야 표와 임시 파일이 겹쳐 쌓이지 않고 새 사본이 지워지지 않는다.
     * 기다리는 중에 취소돼도 앞 작업이 끝날 때까지 남는다. 먼저 끝나면 이 읽기를 기다리던 다음 읽기와 정리가 앞 작업과 겹쳐 돈다.
     */
    private suspend fun awaitTempFileWork(pending: List<Job>) {
        withContext(NonCancellable) { pending.joinAll() }
        currentCoroutineContext().ensureActive()
    }

    private suspend fun readCatching(read: suspend () -> ImportReadResult): ImportReadResult =
        try {
            read()
        } catch (cancellation: CancellationException) {
            // 이 읽기가 취소되지 않았는데 리더 안쪽에서 나온 취소를 Cancelled로 돌려주면 호출한 쪽이 아무것도 하지 않아 로딩 오버레이가 남는다.
            currentCoroutineContext().ensureActive()
            ImportReadResult.Failure(cancellation, source?.extension.orEmpty())
        } catch (error: Throwable) {
            // OutOfMemoryError 같은 Error도 실패로 돌려줘야 호출한 쪽이 로딩 오버레이를 닫는다.
            ImportReadResult.Failure(error, source?.extension.orEmpty())
        }

    private fun onFailure(
        failure: ImportReadResult.Failure,
        stage: ImportReadStage,
    ) {
        // 파서가 알려 준 오류는 사용자 안내로 충분하다. 그 밖의 원인은 기록하되 파일과 붙여 넣은 내용은 남기지 않는다.
        val error = failure.error
        if (error !is ImportParseException) {
            val message = "import read failed ext=${failure.extension} paste=${source is ImportReadSource.Paste}"
            if (error is Exception) Napier.w(throwable = error) { message } else Napier.e(throwable = error) { message }
        }
        if (stage == ImportReadStage.First) discardSource()
    }

    /** 읽기가 끝난 뒤 호출한 쪽이 결과를 받기 전에 멈추거나 새 읽기를 시작했을 수 있어 받은 뒤에 한 번 더 확인한다. */
    private suspend fun awaitLatest(
        job: Deferred<ImportReadResult>,
        ticket: Int,
    ): ImportReadResult {
        val result =
            try {
                job.await()
            } catch (cancellation: CancellationException) {
                // 호출한 코루틴이 취소됐으면 아무도 받지 않을 읽기가 계속 돌지 않게 멈춘다.
                if (!currentCoroutineContext().isActive) {
                    if (ticket == generation) stop()
                    throw cancellation
                }
                return ImportReadResult.Cancelled
            }
        return if (ticket == generation) result else ImportReadResult.Cancelled
    }

    /** 선택기 사본은 결과와 상관없이 읽기 전에 정리한다. 앱 사본을 만든 뒤에는 쓰지 않는다. */
    private suspend fun copyAndRead(
        file: PickedImportFile,
        dateStyle: SpreadsheetDateStyle,
    ): ImportReadResult {
        val copySource =
            try {
                val (name, size) = withContext(dispatchers.io) { sanitizeDisplayName(file.displayName()) to file.sizeBytes() }
                rejectBeforeCopy(name, size)?.let { rejection -> return rejection }
                val destination = withContext(dispatchers.io) { tempStorage.newFile() }
                val copy = ImportReadSource.File(destination, name, dateStyle)
                source = copy
                val copied =
                    try {
                        withContext(dispatchers.io) { fileCopy.copy(file.openSource(), destination, limits.maxFileBytes) }
                    } catch (cancellation: CancellationException) {
                        // 멈춘 복사의 사본은 원본으로 쓸 수 없다. 복사를 마친 직후 취소되면 사본이 남으므로 원본을 비울 때 함께 지운다.
                        // 그 사이 다른 원본으로 바뀌었으면 그쪽의 정리가 이 사본도 지운다.
                        if (source === copy) {
                            source = null
                            withContext(NonCancellable + dispatchers.io) { tempStorage.delete(destination) }
                        }
                        throw cancellation
                    }
                if (copied is ImportCopyResult.TooLarge) return tooLarge(copy.extension)
                copy
            } finally {
                withContext(NonCancellable + dispatchers.io) { file.discard() }
            }
        // 빈 집합은 하위 덱 요약만 읽으라는 뜻이다. `subdeckIds`가 null이면 파서는 모든 하위 덱의 행까지 읽는다.
        val subdeckIds = if (copySource.extension == APKG_EXTENSION) emptySet<String>() else null
        return ImportReadResult.Success(copySource.read(sourceReader, dispatchers, subdeckIds = subdeckIds))
    }

    /**
     * 확장자를 크기보다 먼저 본다. 지원하지 않는 확장자의 큰 파일이 크기 안내로 가면 줄여도 다시 막히기 때문이다.
     * 크기를 모르면(null) 통과시키고 복사하면서 센다.
     */
    private fun rejectBeforeCopy(
        name: String,
        size: Long?,
    ): ImportReadResult.Failure? {
        val extension = name.substringAfterLast('.', missingDelimiterValue = "").lowercase()
        return when {
            ImportSourceName.supportedExtension(name) == null -> {
                ImportReadResult.Failure(ImportParseException(ImportParseErrorKind.UNSUPPORTED_FILE, "unsupported extension"), extension)
            }

            size != null && size > limits.maxFileBytes -> {
                tooLarge(extension)
            }

            else -> {
                null
            }
        }
    }

    private fun tooLarge(extension: String): ImportReadResult.Failure =
        ImportReadResult.Failure(ImportParseException(ImportParseErrorKind.FILE_TOO_LARGE, "file over limit"), extension)

    private fun discardSource() {
        source = null
        clearTempAsync()
    }

    // 취소한 읽기가 아직 파일을 쓰고 있을 수 있으므로 그 읽기가 끝난 뒤에 지워야 정리 뒤에 임시 디렉터리가 다시 생기지 않는다.
    private fun clearTempAsync() {
        val reading = readJob
        val previousCleanup = cleanupJob
        cleanupJob =
            scope.launch(dispatchers.io) {
                listOfNotNull(reading, previousCleanup).joinAll()
                clearIgnoringFailure(tempStorage::clearAll)
            }
    }

    // 정리 함수는 IOException만 삼킨다. 그 밖의 예외가 정리 작업 밖으로 나가면 scope의 다른 작업이 취소되거나 앱이 끝나므로 기록만 남긴다.
    // 예외 메시지에 사용자가 고른 파일 이름이 담길 수 있어 예외 종류만 남긴다.
    private fun clearIgnoringFailure(clear: () -> Unit) {
        try {
            clear()
        } catch (error: Exception) {
            Napier.w { "import temp cleanup failed type=${error::class.simpleName}" }
        }
    }
}

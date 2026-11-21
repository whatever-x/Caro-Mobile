package com.whatever.caro.feature.importer.source

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import okio.Buffer
import okio.BufferedSink
import okio.FileSystem
import okio.Path
import okio.Source
import okio.buffer
import okio.use

internal sealed interface ImportCopyResult {
    data class Copied(
        val bytes: Long,
    ) : ImportCopyResult

    data object TooLarge : ImportCopyResult
}

/** 원본 크기를 미리 알 수 없는 선택기도 있어 복사하면서 바이트를 센다. */
internal class ImportFileCopy(
    private val fileSystem: FileSystem,
) {
    /**
     * [source]를 [destination]으로 복사하고 [source]를 닫는다. [maxBytes]를 넘기면 나머지를 읽지 않고 멈춘다.
     * 상한 초과, 취소, 읽기 실패 어느 경우에도 쓰던 부분 파일을 남기지 않는다.
     */
    suspend fun copy(
        source: Source,
        destination: Path,
        maxBytes: Long,
    ): ImportCopyResult {
        var completed = false
        try {
            val result =
                source.use { input ->
                    fileSystem.sink(destination).buffer().use { output -> copyBounded(input, output, maxBytes) }
                }
            completed = result is ImportCopyResult.Copied
            return result
        } finally {
            if (!completed) fileSystem.delete(destination, mustExist = false)
        }
    }

    private suspend fun copyBounded(
        input: Source,
        output: BufferedSink,
        maxBytes: Long,
    ): ImportCopyResult {
        val chunk = Buffer()
        var total = 0L
        while (true) {
            currentCoroutineContext().ensureActive()
            // 상한보다 1바이트까지만 요청해 초과를 알아본 즉시 읽기를 멈춘다.
            val read = input.read(chunk, minOf(CHUNK_BYTES, maxBytes + 1 - total))
            if (read == -1L) return ImportCopyResult.Copied(total)
            total += read
            if (total > maxBytes) return ImportCopyResult.TooLarge
            output.write(chunk, read)
        }
    }

    private companion object {
        const val CHUNK_BYTES = 64L * 1024
    }
}

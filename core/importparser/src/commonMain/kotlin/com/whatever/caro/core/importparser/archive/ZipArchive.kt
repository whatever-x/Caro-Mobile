package com.whatever.caro.core.importparser.archive

import com.whatever.caro.core.importparser.ArchiveLimits
import com.whatever.caro.core.importparser.ImportParseErrorKind
import com.whatever.caro.core.importparser.ImportParseException
import okio.Buffer
import okio.BufferedSource
import okio.FileSystem
import okio.ForwardingSource
import okio.IOException
import okio.Path
import okio.Path.Companion.toPath
import okio.Source
import okio.buffer
import okio.openZip

/** 정해진 이름의 항목만 읽고 항목 이름으로 파일을 만들지 않으므로 경로 탈출은 생기지 않는다. */
internal class ZipArchive private constructor(
    private val entries: FileSystem,
    private val entryByteLimit: Long,
    private val totalByteLimit: Long,
) {
    private var totalBytes = 0L

    fun contains(name: String): Boolean = entries.exists(entryPath(name))

    /** 풀어낸 바이트를 세다가 상한을 넘으면 멈춘다. 압축 비율 폭탄은 헤더 크기 값을 믿지 않아야 막힌다. */
    fun open(name: String): BufferedSource =
        try {
            ByteLimitedSource(entries.source(entryPath(name)), entryByteLimit, ::countTotal).buffer()
        } catch (exception: IOException) {
            throw ImportParseException(ImportParseErrorKind.CORRUPT_FILE, "cannot open zip entry $name", exception)
        }

    fun limitSource(source: Source): Source = ByteLimitedSource(source, entryByteLimit, ::countTotal)

    /** 항목을 여러 번 읽거나 여러 항목을 읽어도 한 파일에서 풀어낸 바이트 합계로 한 번 더 막는다. */
    private fun countTotal(byteCount: Long) {
        totalBytes += byteCount
        if (totalBytes > totalByteLimit) {
            throw ImportParseException(ImportParseErrorKind.ARCHIVE_LIMIT_EXCEEDED, "expanded archive over $totalByteLimit bytes")
        }
    }

    private fun entryPath(name: String): Path = ROOT / name.removePrefix("/")

    companion object {
        private val ROOT = "/".toPath()

        // 아주 작은 파일은 정상 Anki 컬렉션도 압축 비율이 커 보이므로 이 크기까지는 비율 상한을 적용하지 않는다.
        private const val RATIO_FREE_BYTES = 16L * 1024 * 1024

        fun open(
            fileSystem: FileSystem,
            path: Path,
            archiveBytes: Long,
            limits: ArchiveLimits,
        ): ZipArchive {
            // Okio는 중앙 디렉터리 전체를 메모리에 올리므로 끝 레코드가 밝힌 항목 수로 먼저 거른다.
            ZipEndRecord.claimedEntryCount(fileSystem, path, archiveBytes)?.let { claimed ->
                if (claimed > limits.maxEntries) throw entriesExceeded(limits)
            }
            val entries =
                try {
                    fileSystem.openZip(path)
                } catch (exception: IOException) {
                    throw ImportParseException(ImportParseErrorKind.CORRUPT_FILE, "zip damaged", exception)
                }
            val entryCount = entries.listRecursively(ROOT).take(limits.maxEntries + 1).count()
            if (entryCount > limits.maxEntries) throw entriesExceeded(limits)
            val ratioBytes = maxOf(RATIO_FREE_BYTES, archiveBytes * limits.maxCompressionRatio)
            return ZipArchive(entries, minOf(limits.maxEntryBytes, ratioBytes), limits.maxTotalBytes)
        }

        private fun entriesExceeded(limits: ArchiveLimits) =
            ImportParseException(ImportParseErrorKind.ARCHIVE_LIMIT_EXCEEDED, "zip entries over ${limits.maxEntries}")
    }
}

private class ByteLimitedSource(
    delegate: Source,
    private val limit: Long,
    private val onRead: (Long) -> Unit,
) : ForwardingSource(delegate) {
    private var total = 0L

    override fun read(
        sink: Buffer,
        byteCount: Long,
    ): Long {
        val read = super.read(sink, byteCount)
        if (read > 0) {
            onRead(read)
            total += read
            if (total > limit) {
                throw ImportParseException(ImportParseErrorKind.ARCHIVE_LIMIT_EXCEEDED, "expanded entry over $limit bytes")
            }
        }
        return read
    }
}

package com.whatever.caro.feature.importer.support

import com.whatever.caro.feature.importer.FIXTURE_ROOT
import com.whatever.caro.feature.importer.ImportFixtures
import com.whatever.caro.feature.importer.platform.PickedImportFile
import okio.Buffer
import okio.ByteString
import okio.ByteString.Companion.toByteString
import okio.Path.Companion.toPath
import okio.Source
import okio.Timeout

/**
 * [reportedSize]는 실제 내용 크기와 따로 정할 수 있다. 100MB 경계는 큰 바이트열을 만들지 않고 보고 값만으로 확인한다.
 * [onRead]는 복사 반복문이 도는 도중에 끼어들어 취소 시점을 만드는 데 쓴다.
 */
internal class FakePickedImportFile(
    private val name: String,
    val content: ByteString,
    private val reportedSize: Long? = content.size.toLong(),
    private val onRead: (reads: Int) -> Unit = {},
) : PickedImportFile {
    var discardCount: Int = 0
        private set

    override fun displayName(): String = name

    override fun sizeBytes(): Long? = reportedSize

    override fun openSource(): Source = ChunkedSource(Buffer().write(content), onRead)

    override fun discard() {
        discardCount += 1
    }

    // 한 번에 돌려주는 바이트를 8KiB로 제한해 복사 반복문이 여러 번 돌게 한다.
    private class ChunkedSource(
        private val buffer: Buffer,
        private val onRead: (reads: Int) -> Unit,
    ) : Source {
        private var reads = 0

        override fun read(
            sink: Buffer,
            byteCount: Long,
        ): Long {
            if (buffer.exhausted()) return -1
            val chunk = minOf(byteCount, buffer.size, CHUNK_BYTES)
            sink.write(buffer, chunk)
            reads += 1
            onRead(reads)
            return chunk
        }

        override fun timeout(): Timeout = Timeout.NONE

        override fun close() = Unit
    }

    companion object {
        private const val CHUNK_BYTES = 8_192L

        fun ofSize(
            name: String,
            size: Int,
            reportedSize: Long? = size.toLong(),
            onRead: (reads: Int) -> Unit = {},
        ): FakePickedImportFile =
            FakePickedImportFile(
                name = name,
                content = ByteArray(size) { index -> (index % 251).toByte() }.toByteString(),
                reportedSize = reportedSize,
                onRead = onRead,
            )

        fun fromFixture(relativePath: String): FakePickedImportFile =
            FakePickedImportFile(
                name = relativePath.substringAfterLast('/'),
                content = ImportFixtures.fileSystem.read(FIXTURE_ROOT.toPath() / relativePath) { readByteString() },
            )
    }
}

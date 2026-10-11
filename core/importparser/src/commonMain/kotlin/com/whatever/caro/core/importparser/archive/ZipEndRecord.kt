package com.whatever.caro.core.importparser.archive

import okio.Buffer
import okio.FileHandle
import okio.FileSystem
import okio.Path
import okio.use

/** 끝 레코드가 밝힌 중앙 디렉터리의 항목 수와 바이트 크기. [bytes]는 적힌 크기와 시작 위치부터 끝 레코드까지의 거리 중 큰 값이다. */
internal class CentralDirectoryClaim(
    val entries: Long,
    val bytes: Long,
)

/** zip 끝 레코드(와 ZIP64 끝 레코드)에 적힌 중앙 디렉터리 규모를 읽는다. 끝 레코드를 찾지 못하면 null이다. */
internal object ZipEndRecord {
    private const val END_SIGNATURE = 0x06054b50
    private const val ZIP64_LOCATOR_SIGNATURE = 0x07064b50
    private const val ZIP64_END_SIGNATURE = 0x06064b50
    private const val END_RECORD_BYTES = 22
    private const val MAX_COMMENT_BYTES = 0xFFFF
    private const val TOTAL_ENTRIES_OFFSET = 10
    private const val ZIP64_LOCATOR_BYTES = 20
    private const val ZIP64_END_OFFSET_IN_LOCATOR = 8L
    private const val ZIP64_TOTAL_ENTRIES_OFFSET = 32L
    private const val ZIP64_READ_BYTES = 56L
    private const val SHORT_MASK = 0xFFFFL
    private const val INT_MASK = 0xFFFFFFFFL
    private const val BYTE_MASK = 0xFF

    /**
     * Okio는 16비트 항목 수와 상관없이 ZIP64 locator가 있으면 ZIP64 레코드를 따르므로 같은 규칙으로 읽어야 상한 확인을 우회하지 못한다.
     * Okio는 적힌 크기를 쓰지 않고 시작 위치부터 항목 수만큼 읽으므로, 크기를 줄여 적은 파일을 잡으려고 끝 레코드까지의 거리도 함께 잰다.
     */
    fun claim(
        fileSystem: FileSystem,
        path: Path,
        fileBytes: Long,
    ): CentralDirectoryClaim? =
        fileSystem.openReadOnly(path).use { handle ->
            val tailStart = maxOf(0L, fileBytes - END_RECORD_BYTES - MAX_COMMENT_BYTES)
            val tail = ByteArray((fileBytes - tailStart).toInt())
            handle.read(tailStart, tail, 0, tail.size)
            val endIndex = lastSignatureIndex(tail) ?: return null
            val endOffset = tailStart + endIndex
            val record = Buffer().write(tail, endIndex + TOTAL_ENTRIES_OFFSET, END_RECORD_BYTES - TOTAL_ENTRIES_OFFSET)
            val entries = record.readShortLe().toLong() and SHORT_MASK
            val declaredBytes = record.readIntLe().toLong() and INT_MASK
            val start = record.readIntLe().toLong() and INT_MASK
            zip64Claim(handle, endOffset) ?: claimOf(entries, declaredBytes, start, endOffset)
        }

    private fun claimOf(
        entries: Long,
        declaredBytes: Long,
        start: Long,
        endOffset: Long,
    ) = CentralDirectoryClaim(entries, maxOf(declaredBytes, endOffset - start))

    private fun lastSignatureIndex(tail: ByteArray): Int? {
        for (index in tail.size - END_RECORD_BYTES downTo 0) {
            if (intLe(tail, index) == END_SIGNATURE) return index
        }
        return null
    }

    private fun intLe(
        bytes: ByteArray,
        offset: Int,
    ): Int =
        (0 until Int.SIZE_BYTES).fold(0) { value, index ->
            value or ((bytes[offset + index].toInt() and BYTE_MASK) shl (index * Byte.SIZE_BITS))
        }

    private fun zip64Claim(
        handle: FileHandle,
        endRecordOffset: Long,
    ): CentralDirectoryClaim? {
        val locatorOffset = endRecordOffset - ZIP64_LOCATOR_BYTES
        if (locatorOffset < 0) return null
        val locator = Buffer().also { handle.read(locatorOffset, it, ZIP64_LOCATOR_BYTES.toLong()) }
        if (locator.size < ZIP64_LOCATOR_BYTES || locator.readIntLe() != ZIP64_LOCATOR_SIGNATURE) return null
        locator.skip(ZIP64_END_OFFSET_IN_LOCATOR - Int.SIZE_BYTES)
        val zip64EndOffset = locator.readLongLe()
        if (zip64EndOffset !in 0..endRecordOffset) return null
        val record = Buffer().also { handle.read(zip64EndOffset, it, ZIP64_READ_BYTES) }
        if (record.size < ZIP64_READ_BYTES || record.readIntLe() != ZIP64_END_SIGNATURE) return null
        record.skip(ZIP64_TOTAL_ENTRIES_OFFSET - Int.SIZE_BYTES)
        val entries = record.readLongLe()
        val declaredBytes = record.readLongLe()
        val start = record.readLongLe()
        return claimOf(entries, declaredBytes, start, zip64EndOffset)
    }
}

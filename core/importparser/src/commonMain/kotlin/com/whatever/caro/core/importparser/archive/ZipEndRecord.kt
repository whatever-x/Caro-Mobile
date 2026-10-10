package com.whatever.caro.core.importparser.archive

import okio.Buffer
import okio.FileSystem
import okio.Path
import okio.use

/** zip 끝 레코드(와 ZIP64 끝 레코드)에 적힌 전체 항목 수를 읽는다. 끝 레코드를 찾지 못하면 null이다. */
internal object ZipEndRecord {
    private const val END_SIGNATURE = 0x06054b50
    private const val ZIP64_LOCATOR_SIGNATURE = 0x07064b50
    private const val ZIP64_END_SIGNATURE = 0x06064b50
    private const val END_RECORD_BYTES = 22
    private const val MAX_COMMENT_BYTES = 0xFFFF
    private const val TOTAL_ENTRIES_OFFSET = 10
    private const val ZIP64_MARKER = 0xFFFF
    private const val ZIP64_LOCATOR_BYTES = 20
    private const val ZIP64_END_OFFSET_IN_LOCATOR = 8L
    private const val ZIP64_TOTAL_ENTRIES_OFFSET = 32L
    private const val SHORT_MASK = 0xFFFF
    private const val BYTE_MASK = 0xFF

    fun claimedEntryCount(
        fileSystem: FileSystem,
        path: Path,
        fileBytes: Long,
    ): Long? =
        fileSystem.openReadOnly(path).use { handle ->
            val tailStart = maxOf(0L, fileBytes - END_RECORD_BYTES - MAX_COMMENT_BYTES)
            val tail = ByteArray((fileBytes - tailStart).toInt())
            handle.read(tailStart, tail, 0, tail.size)
            val endIndex = lastSignatureIndex(tail) ?: return null
            val total = Buffer().write(tail, endIndex + TOTAL_ENTRIES_OFFSET, 2).readShortLe().toInt() and SHORT_MASK
            if (total != ZIP64_MARKER) return total.toLong()
            zip64EntryCount(handle, tailStart + endIndex) ?: total.toLong()
        }

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

    private fun zip64EntryCount(
        handle: okio.FileHandle,
        endRecordOffset: Long,
    ): Long? {
        val locatorOffset = endRecordOffset - ZIP64_LOCATOR_BYTES
        if (locatorOffset < 0) return null
        val locator = Buffer().also { handle.read(locatorOffset, it, ZIP64_LOCATOR_BYTES.toLong()) }
        if (locator.size < ZIP64_LOCATOR_BYTES || locator.readIntLe() != ZIP64_LOCATOR_SIGNATURE) return null
        locator.skip(ZIP64_END_OFFSET_IN_LOCATOR - Int.SIZE_BYTES)
        val zip64EndOffset = locator.readLongLe()
        if (zip64EndOffset !in 0..endRecordOffset) return null
        val record = Buffer().also { handle.read(zip64EndOffset, it, ZIP64_TOTAL_ENTRIES_OFFSET + Long.SIZE_BYTES) }
        if (record.size < ZIP64_TOTAL_ENTRIES_OFFSET + Long.SIZE_BYTES || record.readIntLe() != ZIP64_END_SIGNATURE) return null
        record.skip(ZIP64_TOTAL_ENTRIES_OFFSET - Int.SIZE_BYTES)
        return record.readLongLe()
    }
}

package com.whatever.caro.core.importparser.archive

import com.whatever.caro.core.importparser.ImportParseErrorKind
import com.whatever.caro.core.importparser.ImportParseException
import okio.Buffer
import okio.FileHandle
import okio.FileSystem
import okio.Path
import okio.use

/** 끝 레코드가 밝힌 중앙 디렉터리의 항목 수와 바이트 크기. [bytes]는 적힌 크기와 시작 위치부터 파일 끝까지의 거리 중 큰 값이다. */
internal class CentralDirectoryClaim(
    val entries: Long,
    val bytes: Long,
)

/**
 * zip 끝 레코드(와 ZIP64 끝 레코드)에 적힌 중앙 디렉터리 규모를 읽는다. 끝 레코드를 찾지 못하면 null이다.
 * Okio가 열 수 없는 위치를 가리키면 손상 파일 예외를 던진다.
 */
internal object ZipEndRecord {
    private const val END_SIGNATURE = 0x06054b50
    private const val ZIP64_LOCATOR_SIGNATURE = 0x07064b50
    private const val ZIP64_END_SIGNATURE = 0x06064b50
    private const val END_RECORD_BYTES = 22

    // Okio가 끝 레코드를 찾는 범위다. 코멘트 최대 길이(65,535)보다 1바이트 넓다.
    private const val SCAN_BYTES = 0x10000
    private const val TOTAL_ENTRIES_OFFSET = 10
    private const val ZIP64_LOCATOR_BYTES = 20
    private const val ZIP64_END_OFFSET_IN_LOCATOR = 8L
    private const val ZIP64_TOTAL_ENTRIES_OFFSET = 32L
    private const val ZIP64_READ_BYTES = 56L
    private const val SHORT_MASK = 0xFFFFL
    private const val INT_MASK = 0xFFFFFFFFL
    private const val BYTE_MASK = 0xFF

    /**
     * 끝 레코드를 찾는 범위와 ZIP64 레코드를 따르는 규칙이 Okio와 정확히 같아야 한다. 다르면 이 판정만 비껴가고 Okio는 중앙 디렉터리를 전부 메모리에 올린다.
     * Okio는 적힌 크기를 쓰지 않고 시작 위치부터 항목 수만큼 읽으므로, 크기를 줄여 적은 파일을 잡으려고 시작 위치부터 파일 끝까지의 거리도 함께 잰다.
     */
    fun claim(
        fileSystem: FileSystem,
        path: Path,
        fileBytes: Long,
    ): CentralDirectoryClaim? =
        fileSystem.openReadOnly(path).use { handle ->
            val tailStart = maxOf(0L, fileBytes - END_RECORD_BYTES - SCAN_BYTES)
            val tail = ByteArray((fileBytes - tailStart).toInt())
            handle.read(tailStart, tail, 0, tail.size)
            val endIndex = lastSignatureIndex(tail) ?: return null
            val endOffset = tailStart + endIndex
            val record = Buffer().write(tail, endIndex + TOTAL_ENTRIES_OFFSET, END_RECORD_BYTES - TOTAL_ENTRIES_OFFSET)
            val entries = record.readShortLe().toLong() and SHORT_MASK
            val declaredBytes = record.readIntLe().toLong() and INT_MASK
            val start = record.readIntLe().toLong() and INT_MASK
            zip64Claim(handle, endOffset, fileBytes) ?: claimOf(entries, declaredBytes, start, fileBytes)
        }

    // Okio는 위치에 읽을 길이를 더한 값이 Long 범위를 넘으면 0바이트만 읽고 끝나지 않으므로, 파일 밖 시작 위치는 Okio에 넘기기 전에 막는다.
    private fun claimOf(
        entries: Long,
        declaredBytes: Long,
        start: Long,
        fileBytes: Long,
    ): CentralDirectoryClaim {
        if (start !in 0..fileBytes) throw corrupt("central directory start outside file")
        return CentralDirectoryClaim(entries, maxOf(declaredBytes, fileBytes - start))
    }

    private fun corrupt(reason: String) = ImportParseException(ImportParseErrorKind.CORRUPT_FILE, "zip damaged: $reason")

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

    // Okio는 locator가 가리키는 위치를 범위 검사 없이 따르고 읽지 못하면 열기에 실패하므로, 읽을 수 없는 ZIP64 레코드는 손상 파일로 본다.
    private fun zip64Claim(
        handle: FileHandle,
        endRecordOffset: Long,
        fileBytes: Long,
    ): CentralDirectoryClaim? {
        val locatorOffset = endRecordOffset - ZIP64_LOCATOR_BYTES
        if (locatorOffset <= 0) return null
        val locator = Buffer().also { handle.read(locatorOffset, it, ZIP64_LOCATOR_BYTES.toLong()) }
        if (locator.size < ZIP64_LOCATOR_BYTES || locator.readIntLe() != ZIP64_LOCATOR_SIGNATURE) return null
        locator.skip(ZIP64_END_OFFSET_IN_LOCATOR - Int.SIZE_BYTES)
        val zip64EndOffset = locator.readLongLe()
        if (zip64EndOffset !in 0..fileBytes - ZIP64_READ_BYTES) throw corrupt("zip64 end record outside file")
        val record = Buffer().also { handle.read(zip64EndOffset, it, ZIP64_READ_BYTES) }
        if (record.size < ZIP64_READ_BYTES || record.readIntLe() != ZIP64_END_SIGNATURE) throw corrupt("zip64 end record unreadable")
        record.skip(ZIP64_TOTAL_ENTRIES_OFFSET - Int.SIZE_BYTES)
        val entries = record.readLongLe()
        val declaredBytes = record.readLongLe()
        val start = record.readLongLe()
        return claimOf(entries, declaredBytes, start, fileBytes)
    }
}

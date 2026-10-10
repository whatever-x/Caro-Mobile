package com.whatever.caro.core.importparser.text

import com.whatever.caro.core.importparser.ImportParseErrorKind
import com.whatever.caro.core.importparser.ImportParseException
import okio.Buffer
import okio.BufferedSource
import okio.ByteString.Companion.decodeHex

internal class InvalidTextEncodingException : Exception()

/**
 * 파일을 블록 단위로 읽어 마지막 줄 끝(LF 또는 CR)에서 잘라 디코딩한다.
 * UTF-8과 CP949 모두 0x0A, 0x0D가 다중 바이트 문자 안에 나오지 않아 줄 끝에서 자르면 글자가 갈리지 않는다.
 *
 * @property splitInsideLine 줄 끝이 [maxChunkBytes] 안에 없을 때 문자 경계에서 자를지 여부.
 *   UTF-8만 바이트로 문자 경계를 알 수 있다. CP949는 둘째 바이트가 ASCII 범위와 겹쳐 줄 안에서 자를 수 없으므로 그때는 값이 너무 김으로 멈춘다.
 */
internal class EncodedTextChunks(
    private val source: BufferedSource,
    private val decode: (ByteArray) -> String?,
    private val splitInsideLine: Boolean = false,
    private val blockBytes: Long = BLOCK_BYTES,
    private val maxChunkBytes: Long = MAX_CHUNK_BYTES,
) : Iterator<String> {
    private val pending = Buffer()
    private var scanned = 0L
    private var lastLineEnd = -1L
    private var started = false
    private var done = false

    override fun hasNext(): Boolean = !done

    override fun next(): String {
        if (!started) skipUtf8ByteOrderMark()
        while (true) {
            val read = source.read(pending, minOf(blockBytes, maxChunkBytes - pending.size).coerceAtLeast(1))
            if (read == -1L) {
                done = true
                return take(pending.size)
            }
            findLineEnds()
            when {
                lastLineEnd >= 0 -> return take(lastLineEnd + 1)
                pending.size < maxChunkBytes -> continue
                splitInsideLine -> return take(utf8Boundary())
                else -> throw ImportParseException(ImportParseErrorKind.CONTENT_TOO_LONG, "line over $maxChunkBytes bytes")
            }
        }
    }

    private fun skipUtf8ByteOrderMark() {
        started = true
        if (source.rangeEquals(0, UTF8_BOM)) source.skip(UTF8_BOM.size.toLong())
    }

    /** 새로 읽은 부분만 훑어 다시 읽을 때마다 앞부분을 반복해서 찾지 않는다. */
    private fun findLineEnds() {
        lastLineEnd = maxOf(lastLineEnd, lastIndexFromScanned(LINE_FEED), lastIndexFromScanned(CARRIAGE_RETURN))
        scanned = pending.size
    }

    private fun lastIndexFromScanned(byte: Byte): Long {
        var last = -1L
        var index = pending.indexOf(byte, scanned)
        while (index >= 0) {
            last = index
            index = pending.indexOf(byte, index + 1)
        }
        return last
    }

    /** 끝 문자가 다 들어 있으면 전체 길이를, 잘린 다중 바이트 문자가 있으면 그 문자 앞까지의 길이를 돌려준다. */
    private fun utf8Boundary(): Long {
        var start = pending.size - 1
        while (start > 0 && isContinuation(pending[start])) start--
        return if (start + utf8Length(pending[start]) <= pending.size) pending.size else maxOf(start, 1L)
    }

    private fun take(byteCount: Long): String {
        val bytes = pending.readByteArray(byteCount)
        scanned = pending.size
        lastLineEnd = -1L
        return decode(bytes) ?: throw InvalidTextEncodingException()
    }

    private fun isContinuation(byte: Byte): Boolean = byte.toInt() and UTF8_CONTINUATION_MASK == UTF8_CONTINUATION

    private fun utf8Length(lead: Byte): Int {
        val value = lead.toInt() and BYTE_MASK
        return when {
            value < UTF8_TWO_BYTE_LEAD -> 1
            value < UTF8_THREE_BYTE_LEAD -> 2
            value < UTF8_FOUR_BYTE_LEAD -> 3
            else -> 4
        }
    }

    private companion object {
        const val BLOCK_BYTES = 64L * 1024
        const val MAX_CHUNK_BYTES = 4L * 1024 * 1024
        const val LINE_FEED: Byte = 0x0A
        const val CARRIAGE_RETURN: Byte = 0x0D
        const val BYTE_MASK = 0xFF
        const val UTF8_CONTINUATION_MASK = 0xC0
        const val UTF8_CONTINUATION = 0x80
        const val UTF8_TWO_BYTE_LEAD = 0xC0
        const val UTF8_THREE_BYTE_LEAD = 0xE0
        const val UTF8_FOUR_BYTE_LEAD = 0xF0
        val UTF8_BOM = "efbbbf".decodeHex()
    }
}

internal fun decodeStrictUtf8(bytes: ByteArray): String? =
    try {
        bytes.decodeToString(throwOnInvalidSequence = true)
    } catch (_: CharacterCodingException) {
        null
    }

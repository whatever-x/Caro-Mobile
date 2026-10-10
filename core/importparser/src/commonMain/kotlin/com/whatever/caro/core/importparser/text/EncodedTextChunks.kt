package com.whatever.caro.core.importparser.text

import okio.Buffer
import okio.BufferedSource
import okio.ByteString.Companion.decodeHex

internal class InvalidTextEncodingException : Exception()

/**
 * 파일을 블록 단위로 읽어 마지막 줄바꿈에서 잘라 디코딩한다.
 * UTF-8과 CP949 모두 0x0A가 다중 바이트 문자 안에 나오지 않아 줄바꿈 경계에서 자르면 글자가 갈리지 않는다.
 */
internal class EncodedTextChunks(
    private val source: BufferedSource,
    private val decode: (ByteArray) -> String?,
) : Iterator<String> {
    private val pending = Buffer()
    private var started = false
    private var done = false

    override fun hasNext(): Boolean = !done

    override fun next(): String {
        if (!started) skipUtf8ByteOrderMark()
        while (true) {
            val read = source.read(pending, BLOCK_BYTES)
            if (read == -1L) {
                done = true
                return decodeOrThrow(pending.readByteArray())
            }
            val lastNewline = lastNewlineIndex()
            if (lastNewline >= 0) return decodeOrThrow(pending.readByteArray(lastNewline + 1))
        }
    }

    private fun skipUtf8ByteOrderMark() {
        started = true
        if (source.rangeEquals(0, UTF8_BOM)) source.skip(UTF8_BOM.size.toLong())
    }

    private fun lastNewlineIndex(): Long {
        var last = -1L
        var index = pending.indexOf(LINE_FEED)
        while (index >= 0) {
            last = index
            index = pending.indexOf(LINE_FEED, index + 1)
        }
        return last
    }

    private fun decodeOrThrow(bytes: ByteArray): String = decode(bytes) ?: throw InvalidTextEncodingException()

    private companion object {
        const val BLOCK_BYTES = 64L * 1024
        const val LINE_FEED: Byte = 0x0A
        val UTF8_BOM = "efbbbf".decodeHex()
    }
}

internal fun decodeStrictUtf8(bytes: ByteArray): String? =
    try {
        bytes.decodeToString(throwOnInvalidSequence = true)
    } catch (_: CharacterCodingException) {
        null
    }

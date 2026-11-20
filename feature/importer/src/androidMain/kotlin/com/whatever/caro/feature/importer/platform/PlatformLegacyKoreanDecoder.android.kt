package com.whatever.caro.feature.importer.platform

import com.whatever.caro.core.importparser.LegacyKoreanDecoder
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction

// Android ICU가 두 이름 중 무엇을 제공하는지 기기마다 확인되지 않아 차례로 찾는다.
private val CHARSET_NAMES = listOf("x-windows-949", "MS949")

internal actual fun platformLegacyKoreanDecoder(): LegacyKoreanDecoder? {
    val name = CHARSET_NAMES.firstOrNull { candidate -> runCatching { Charset.isSupported(candidate) }.getOrDefault(false) } ?: return null
    val charset = Charset.forName(name)
    return LegacyKoreanDecoder { bytes -> decodeStrict(charset, bytes) }
}

private fun decodeStrict(
    charset: Charset,
    bytes: ByteArray,
): String? =
    try {
        charset
            .newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
            .decode(ByteBuffer.wrap(bytes))
            .toString()
    } catch (_: CharacterCodingException) {
        null
    }

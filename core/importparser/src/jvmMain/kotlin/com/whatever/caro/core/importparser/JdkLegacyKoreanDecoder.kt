package com.whatever.caro.core.importparser

import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction

/** 서버 파싱과 JVM 테스트에서 쓰는 JDK 문자셋 기반 CP949 디코더. */
class JdkLegacyKoreanDecoder private constructor(
    private val charset: Charset,
) : LegacyKoreanDecoder {
    override fun decodeOrNull(bytes: ByteArray): String? =
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

    companion object {
        private val CHARSET_NAMES = listOf("x-windows-949", "MS949")

        fun createOrNull(): JdkLegacyKoreanDecoder? =
            CHARSET_NAMES
                .firstOrNull { name -> Charset.isSupported(name) }
                ?.let { name -> JdkLegacyKoreanDecoder(Charset.forName(name)) }
    }
}

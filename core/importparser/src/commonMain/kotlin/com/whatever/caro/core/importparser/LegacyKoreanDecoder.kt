package com.whatever.caro.core.importparser

/**
 * UTF-8이 아닌 한국어 텍스트를 CP949로 읽는 디코더. 플랫폼마다 구현이 달라 호출자가 넣는다.
 *
 * 잘못된 바이트가 하나라도 있으면 null을 돌려준다. 대체 문자를 넣거나 EUC-KR로 낮춰 읽으면 확장 한글이 사라진다.
 */
fun interface LegacyKoreanDecoder {
    fun decodeOrNull(bytes: ByteArray): String?
}

package com.whatever.caro.core.importparser.xlsx

/** XML에 미리 정의된 엔티티와 숫자 문자 참조만 해석한다. DTD를 거부하므로 다른 이름의 엔티티는 정의될 수 없다. */
internal object XmlEntities {
    private const val HEX_RADIX = 16
    private const val DECIMAL_RADIX = 10
    private const val MAX_ENTITY_LENGTH = 12
    private val PREDEFINED =
        mapOf(
            "lt" to "<",
            "gt" to ">",
            "amp" to "&",
            "quot" to "\"",
            "apos" to "'",
        )

    fun decode(raw: String): String {
        if ('&' !in raw) return raw
        val out = StringBuilder(raw.length)
        var index = 0
        while (index < raw.length) {
            val end = if (raw[index] == '&') raw.indexOf(';', index) else -1
            val replacement = if (end > index && end - index <= MAX_ENTITY_LENGTH) resolve(raw.substring(index + 1, end)) else null
            if (replacement == null) {
                out.append(raw[index])
                index++
            } else {
                out.append(replacement)
                index = end + 1
            }
        }
        return out.toString()
    }

    private fun resolve(name: String): String? =
        when {
            name.startsWith("#x") || name.startsWith("#X") -> codePoint(name.substring(2), HEX_RADIX)
            name.startsWith("#") -> codePoint(name.substring(1), DECIMAL_RADIX)
            else -> PREDEFINED[name]
        }

    private fun codePoint(
        digits: String,
        radix: Int,
    ): String? {
        val value = digits.toIntOrNull(radix) ?: return null
        return runCatching { codePointToString(value) }.getOrNull()
    }
}

private const val MAX_CODE_POINT = 0x10FFFF
private const val MIN_SUPPLEMENTARY_CODE_POINT = 0x10000
private const val SURROGATE_BITS = 10
private const val LOW_SURROGATE_MASK = 0x3FF

internal fun codePointToString(codePoint: Int): String {
    val surrogates = Char.MIN_SURROGATE.code..Char.MAX_SURROGATE.code
    require(codePoint in 0..MAX_CODE_POINT && codePoint !in surrogates) { "invalid code point $codePoint" }
    if (codePoint < MIN_SUPPLEMENTARY_CODE_POINT) return codePoint.toChar().toString()
    val offset = codePoint - MIN_SUPPLEMENTARY_CODE_POINT
    val high = Char.MIN_HIGH_SURROGATE.code + (offset shr SURROGATE_BITS)
    val low = Char.MIN_LOW_SURROGATE.code + (offset and LOW_SURROGATE_MASK)
    return charArrayOf(high.toChar(), low.toChar()).concatToString()
}

internal object XmlAttributes {
    /** 시작 태그 본문의 [from] 위치부터 `name="value"` 쌍을 읽는다. 이름은 접두사를 뗀다. */
    fun parse(
        tag: String,
        from: Int,
    ): Map<String, String> {
        val attributes = LinkedHashMap<String, String>()
        var index = from
        while (true) {
            index = skipWhitespace(tag, index)
            val equals = tag.indexOf('=', index)
            if (index >= tag.length || equals < 0) return attributes
            val name = tag.substring(index, equals).trim()
            val quoteIndex = skipWhitespace(tag, equals + 1)
            if (quoteIndex >= tag.length) return attributes
            val quote = tag[quoteIndex]
            val close = tag.indexOf(quote, quoteIndex + 1)
            if (close < 0) return attributes
            attributes[localName(name)] = XmlEntities.decode(tag.substring(quoteIndex + 1, close))
            index = close + 1
        }
    }

    private fun skipWhitespace(
        text: String,
        from: Int,
    ): Int {
        var index = from
        while (index < text.length && text[index].isWhitespace()) index++
        return index
    }
}

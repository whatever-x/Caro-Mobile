package com.whatever.caro.feature.importer.transform

/**
 * HTML 엔티티를 글자로 바꾼다. 이름 엔티티는 ISO-8859-1 범위와 자주 쓰는 문장 부호만 안다.
 * 표에 없는 이름은 글자를 잃지 않도록 원문 그대로 남긴다.
 */
internal object HtmlEntities {
    private val ENTITY = Regex("&(?:#(\\d{1,7});?|#[xX]([0-9a-fA-F]{1,6});?|([A-Za-z][A-Za-z0-9]{1,31});)")
    private const val HEX_RADIX = 16
    private const val REPLACEMENT_CHARACTER = "�"
    private const val MAX_CODE_POINT = 0x10FFFF
    private const val MIN_SUPPLEMENTARY_CODE_POINT = 0x10000
    private const val SURROGATE_BITS = 10
    private const val LOW_SURROGATE_MASK = 0x3FF
    private const val LATIN1_FIRST_ENTITY = 0xA0

    private val LATIN1_NAMES =
        (
            "nbsp iexcl cent pound curren yen brvbar sect uml copy ordf laquo not shy reg macr " +
                "deg plusmn sup2 sup3 acute micro para middot cedil sup1 ordm raquo frac14 frac12 frac34 iquest " +
                "Agrave Aacute Acirc Atilde Auml Aring AElig Ccedil Egrave Eacute Ecirc Euml Igrave Iacute Icirc Iuml " +
                "ETH Ntilde Ograve Oacute Ocirc Otilde Ouml times Oslash Ugrave Uacute Ucirc Uuml Yacute THORN szlig " +
                "agrave aacute acirc atilde auml aring aelig ccedil egrave eacute ecirc euml igrave iacute icirc iuml " +
                "eth ntilde ograve oacute ocirc otilde ouml divide oslash ugrave uacute ucirc uuml yacute thorn yuml"
        ).split(' ')

    private val NAMED: Map<String, String> =
        LATIN1_NAMES.withIndex().associate { (index, name) -> name to (LATIN1_FIRST_ENTITY + index).toChar().toString() } +
            mapOf(
                "amp" to "&",
                "lt" to "<",
                "gt" to ">",
                "quot" to "\"",
                "apos" to "'",
                "ensp" to " ",
                "emsp" to " ",
                "thinsp" to " ",
                "zwnj" to "‌",
                "zwj" to "‍",
                "ndash" to "–",
                "mdash" to "—",
                "lsquo" to "‘",
                "rsquo" to "’",
                "ldquo" to "“",
                "rdquo" to "”",
                "bull" to "•",
                "hellip" to "…",
                "euro" to "€",
                "trade" to "™",
                "larr" to "←",
                "rarr" to "→",
                "uarr" to "↑",
                "darr" to "↓",
            )

    fun decode(value: String): String {
        if ('&' !in value) return value
        return ENTITY.replace(value) { match ->
            val (decimal, hex, name) = match.destructured
            when {
                decimal.isNotEmpty() -> fromCodePoint(decimal.toInt())
                hex.isNotEmpty() -> fromCodePoint(hex.toInt(HEX_RADIX))
                else -> NAMED[name] ?: match.value
            }
        }
    }

    private fun fromCodePoint(codePoint: Int): String {
        val surrogates = Char.MIN_SURROGATE.code..Char.MAX_SURROGATE.code
        if (codePoint <= 0 || codePoint > MAX_CODE_POINT || codePoint in surrogates) return REPLACEMENT_CHARACTER
        if (codePoint < MIN_SUPPLEMENTARY_CODE_POINT) return codePoint.toChar().toString()
        val offset = codePoint - MIN_SUPPLEMENTARY_CODE_POINT
        val high = Char.MIN_HIGH_SURROGATE.code + (offset shr SURROGATE_BITS)
        val low = Char.MIN_LOW_SURROGATE.code + (offset and LOW_SURROGATE_MASK)
        return charArrayOf(high.toChar(), low.toChar()).concatToString()
    }
}

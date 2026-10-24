package com.whatever.caro.feature.importer.regex

private const val SHORTHAND_ESCAPES = "dDsSwW"
private const val CHAR_ESCAPES = "nrtf"
private const val ASSERTION_ESCAPES = "bBAZz"
private const val UNICODE_ESCAPE_DIGITS = 4
private const val HEX_ESCAPE_DIGITS = 2
private const val HEX_RADIX = 16
private const val ESCAPE_PREFIX_LENGTH = 2
private val GENERAL_CATEGORY = Regex("""[LMNPSZC][a-z]?""")

// literal은 클래스 범위의 끝점이 될 수 있는 한 글자이고, 집합이나 단언이면 null이다.
internal class ParsedEscape(
    val length: Int,
    val literal: Int?,
    val problem: String?,
)

// \p는 JVM과 ICU가 이름 해석이 다르므로 일반 범주(L, Lu 등)만 받고, \b 같은 단언은 클래스 안에서 뜻이 갈려 받지 않는다.
internal fun parseEscape(
    pattern: String,
    at: Int,
    inClass: Boolean,
): ParsedEscape {
    val next = pattern.getOrNull(at + 1) ?: return ParsedEscape(1, null, "끝에 남은 '\\'")
    return when {
        next in SHORTHAND_ESCAPES -> {
            ParsedEscape(ESCAPE_PREFIX_LENGTH, null, null)
        }

        next in CHAR_ESCAPES -> {
            ParsedEscape(ESCAPE_PREFIX_LENGTH, controlCode(next), null)
        }

        next in ASSERTION_ESCAPES -> {
            ParsedEscape(ESCAPE_PREFIX_LENGTH, null, if (inClass) "클래스 안의 '\\$next'는 뜻이 갈린다" else null)
        }

        next in '1'..'9' && !inClass -> {
            ParsedEscape(ESCAPE_PREFIX_LENGTH, null, null)
        }

        next == 'u' -> {
            hexEscape(pattern, at, UNICODE_ESCAPE_DIGITS)
        }

        next == 'x' -> {
            hexEscape(pattern, at, HEX_ESCAPE_DIGITS)
        }

        next == 'p' || next == 'P' -> {
            propertyEscape(pattern, at)
        }

        next.isLetterOrDigit() -> {
            ParsedEscape(ESCAPE_PREFIX_LENGTH, null, "이식성이 없는 이스케이프 '\\$next'")
        }

        else -> {
            ParsedEscape(ESCAPE_PREFIX_LENGTH, next.code, null)
        }
    }
}

private fun hexEscape(
    pattern: String,
    at: Int,
    digits: Int,
): ParsedEscape {
    val hex = pattern.drop(at + ESCAPE_PREFIX_LENGTH).take(digits)
    return if (hex.length == digits && hex.all { it.digitToIntOrNull(HEX_RADIX) != null }) {
        ParsedEscape(ESCAPE_PREFIX_LENGTH + digits, hex.toInt(HEX_RADIX), null)
    } else {
        ParsedEscape(ESCAPE_PREFIX_LENGTH, null, "'\\${pattern[at + 1]}' 뒤에는 16진수 ${digits}자리가 필요하다")
    }
}

private fun propertyEscape(
    pattern: String,
    at: Int,
): ParsedEscape {
    val close = pattern.indexOf('}', at + ESCAPE_PREFIX_LENGTH)
    val name =
        if (pattern.getOrNull(at + ESCAPE_PREFIX_LENGTH) == '{' &&
            close > 0
        ) {
            pattern.substring(at + ESCAPE_PREFIX_LENGTH + 1, close)
        } else {
            null
        }
    return if (name != null && GENERAL_CATEGORY.matches(name)) {
        ParsedEscape(close + 1 - at, null, null)
    } else {
        ParsedEscape(ESCAPE_PREFIX_LENGTH, null, "\\p는 {L}, {Lu} 같은 일반 범주 이름만 쓸 수 있다")
    }
}

private fun controlCode(c: Char): Int =
    when (c) {
        'n' -> '\n'.code
        'r' -> '\r'.code
        't' -> '\t'.code
        else -> '\u000C'.code
    }

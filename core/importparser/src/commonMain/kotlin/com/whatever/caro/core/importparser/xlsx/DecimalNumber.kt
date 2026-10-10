package com.whatever.caro.core.importparser.xlsx

/**
 * 셀에 저장된 10진 문자열을 그대로 반올림하는 수. Double로 바꿔 계산하면 플랫폼마다 문자열 변환 결과가 달라질 수 있어
 * 기기와 서버가 같은 표시를 내도록 자릿수 문자열로 계산한다. 값은 `digits × 10^exponent`이다.
 */
internal data class DecimalNumber(
    val negative: Boolean,
    val digits: String,
    val exponent: Int,
) {
    val isZero: Boolean get() = digits.all { digit -> digit == '0' }

    fun shift(power: Int): DecimalNumber = copy(exponent = exponent + power)

    /** 유효숫자 [count]자리로 반올림한다. 0.5는 0에서 먼 쪽으로 올린다(Excel 표시와 같다). */
    fun roundToSignificant(count: Int): DecimalNumber = if (digits.length <= count) this else dropDigits(digits.length - count)

    /** 소수점 아래 [scale]자리로 반올림한다. */
    fun roundToScale(scale: Int): DecimalNumber = if (exponent >= -scale) this else dropDigits(-scale - exponent)

    fun toPlainString(
        fractionDigits: Int? = null,
        grouping: Boolean = false,
    ): String {
        val (integerPart, rawFraction) = split()
        val fraction =
            when (fractionDigits) {
                null -> rawFraction.trimEnd('0')
                else -> rawFraction.padEnd(fractionDigits, '0').take(fractionDigits)
            }
        val integer = if (grouping) group(integerPart) else integerPart
        val sign = if (negative && !isZero) "-" else ""
        return sign + integer + if (fraction.isEmpty()) "" else ".$fraction"
    }

    private fun dropDigits(count: Int): DecimalNumber {
        val kept = digits.dropLast(count)
        val roundUp = digits.getOrNull(digits.length - count)?.let { digit -> digit >= '5' } ?: false
        val rounded = if (roundUp) increment(kept.ifEmpty { "0" }) else kept.ifEmpty { "0" }
        return copy(digits = rounded, exponent = exponent + count)
    }

    private fun split(): Pair<String, String> {
        if (exponent >= 0) return normalizeInteger(digits + "0".repeat(exponent)) to ""
        val fractionLength = -exponent
        return if (digits.length > fractionLength) {
            normalizeInteger(digits.dropLast(fractionLength)) to digits.takeLast(fractionLength)
        } else {
            "0" to "0".repeat(fractionLength - digits.length) + digits
        }
    }

    companion object {
        private val PATTERN = Regex("^([+-])?(\\d*)(?:\\.(\\d*))?(?:[eE]([+-]?\\d+))?$")
        private const val GROUP_SIZE = 3

        fun parse(text: String): DecimalNumber? {
            val match = PATTERN.matchEntire(text.trim()) ?: return null
            val (sign, integer, fraction, exponent) = match.destructured
            if (integer.isEmpty() && fraction.isEmpty()) return null
            val scale = exponent.ifEmpty { "0" }.toIntOrNull() ?: return null
            return DecimalNumber(sign == "-", normalizeInteger(integer + fraction), scale - fraction.length)
        }

        private fun normalizeInteger(value: String): String = value.trimStart('0').ifEmpty { "0" }

        private fun increment(value: String): String {
            val chars = value.toCharArray()
            for (index in chars.indices.reversed()) {
                if (chars[index] != '9') {
                    chars[index] = chars[index] + 1
                    return chars.concatToString()
                }
                chars[index] = '0'
            }
            return "1" + chars.concatToString()
        }

        private fun group(integer: String): String =
            integer
                .reversed()
                .chunked(GROUP_SIZE)
                .joinToString(",")
                .reversed()
    }
}

package com.whatever.caro.feature.importer.regex

private const val RAW_QUOTE = "\"\"\""

// 문자열 안의 괄호와 쉼표를 구분 문자로 오인하지 않도록 문자열 리터럴을 통째로 건너뛰며 읽는다.
internal object ExpressionText {
    /** open 위치의 '('부터 짝이 맞는 ')'까지를 최상위 쉼표로 나눈 인자와 ')' 다음 위치. 짝을 찾지 못하면 null. */
    fun readArguments(
        text: String,
        open: Int,
    ): Pair<List<String>, Int>? {
        val arguments = mutableListOf<String>()
        var depth = 0
        var argumentStart = open + 1
        var i = open
        while (i < text.length) {
            when (text[i]) {
                '"' -> {
                    i = skipString(text, i) - 1
                }

                '\'' -> {
                    i = skipChar(text, i) - 1
                }

                '(', '[', '{' -> {
                    depth++
                }

                ')', ']', '}' -> {
                    depth--
                    if (depth == 0) return (arguments + text.substring(argumentStart, i).trim()) to i + 1
                }

                ',' -> {
                    if (depth == 1) {
                        arguments += text.substring(argumentStart, i).trim()
                        argumentStart = i + 1
                    }
                }
            }
            i++
        }
        return null
    }

    /** from 위치부터 한 식의 끝까지. 줄이 +로 끝나거나 다음 줄이 +로 시작하면 이어서 읽는다. */
    fun readStatement(
        text: String,
        from: Int,
    ): String {
        var depth = 0
        var i = from
        while (i < text.length) {
            when (text[i]) {
                '"' -> i = skipString(text, i) - 1
                '\'' -> i = skipChar(text, i) - 1
                '(', '[', '{' -> depth++
                ')', ']', '}' -> depth--
                '\n' -> if (depth <= 0 && !continues(text, from, i)) return text.substring(from, i).trim()
            }
            i++
        }
        return text.substring(from).trim()
    }

    private fun continues(
        text: String,
        from: Int,
        newline: Int,
    ): Boolean {
        val before = text.substring(from, newline).trimEnd()
        val after = text.substring(newline).trimStart()
        return before.endsWith("+") || after.startsWith("+") || after.startsWith(".")
    }

    /** start의 따옴표로 시작하는 문자열 리터럴 바로 다음 위치. */
    fun skipString(
        text: String,
        start: Int,
    ): Int {
        val raw = text.startsWith(RAW_QUOTE, start)
        var i = start + if (raw) RAW_QUOTE.length else 1
        while (i < text.length) {
            when {
                raw && text.startsWith(RAW_QUOTE, i) -> return i + RAW_QUOTE.length
                !raw && text[i] == '"' -> return i + 1
                !raw && text[i] == '\\' -> i += 2
                text.startsWith("\${", i) -> i = skipTemplate(text, i + 2)
                else -> i++
            }
        }
        return text.length
    }

    private fun skipTemplate(
        text: String,
        from: Int,
    ): Int {
        var depth = 1
        var i = from
        while (i < text.length && depth > 0) {
            when (text[i]) {
                '"' -> i = skipString(text, i) - 1
                '{' -> depth++
                '}' -> depth--
            }
            i++
        }
        return i
    }

    private fun skipChar(
        text: String,
        start: Int,
    ): Int {
        val close = if (text.getOrNull(start + 1) == '\\') start + 3 else start + 2
        return if (text.getOrNull(close) == '\'') close + 1 else start + 1
    }
}

package com.whatever.caro.feature.importer.regex

private const val MAX_DEPTH = 8
private const val UNICODE_ESCAPE_DIGITS = 4
private const val HEX_RADIX = 16
private const val RAW_QUOTE = "\"\"\""
private val SIMPLE_ESCAPES = mapOf('n' to '\n', 't' to '\t', 'r' to '\r', 'b' to '\b', '\\' to '\\', '"' to '"', '\'' to '\'', '$' to '$')
private val IDENTIFIER_PATH = Regex("""[A-Za-z_]\w*(?:\.(?!replace\()[A-Za-z_]\w*)*""")
private val IDENTIFIER = Regex("""[A-Za-z_]\w*""")

private class Unresolved(
    message: String,
) : RuntimeException(message)

// 리터럴, 이어붙이기(+), 상수 참조와 보간, "…".replace("a", "b")만 푼다.
internal class ExpressionEvaluator(
    private val constants: Map<String, String>,
) {
    fun evaluate(expression: String): String = evaluate(expression, depth = 0)

    private fun evaluate(
        expression: String,
        depth: Int,
    ): String {
        if (depth > MAX_DEPTH) throw Unresolved("상수 참조가 순환하거나 너무 깊다: $expression")
        val cursor = Cursor(expression)
        val result = StringBuilder()
        do {
            result.append(term(cursor, depth))
            cursor.skipSpaces()
        } while (cursor.consume('+'))
        if (!cursor.atEnd()) throw Unresolved("지원하지 않는 식: $expression")
        return result.toString()
    }

    private fun term(
        cursor: Cursor,
        depth: Int,
    ): String {
        cursor.skipSpaces()
        var value =
            when {
                cursor.startsWith(RAW_QUOTE) -> stringBody(cursor.takeString(), raw = true, depth)
                cursor.startsWith("\"") -> stringBody(cursor.takeString(), raw = false, depth)
                else -> reference(cursor.takeMatch(IDENTIFIER_PATH) ?: throw Unresolved("지원하지 않는 식: ${cursor.rest()}"), depth)
            }
        while (cursor.startsWithAfterSpaces(".replace(")) {
            cursor.skipSpaces()
            val arguments = cursor.takeArguments().orEmpty()
            if (arguments.size != 2) throw Unresolved("replace 인자는 두 개여야 한다: ${cursor.rest()}")
            value = value.replace(evaluate(arguments[0], depth + 1), evaluate(arguments[1], depth + 1))
        }
        return value
    }

    private fun reference(
        path: String,
        depth: Int,
    ): String {
        val name = path.substringAfterLast('.')
        val expression = constants[name] ?: throw Unresolved("상수 ${name}의 정의를 찾지 못했다")
        return evaluate(expression, depth + 1)
    }

    private fun stringBody(
        literal: String,
        raw: Boolean,
        depth: Int,
    ): String {
        val quote = if (raw) RAW_QUOTE else "\""
        val body = literal.substring(quote.length, literal.length - quote.length)
        val out = StringBuilder()
        var i = 0
        while (i < body.length) {
            val c = body[i]
            when {
                c == '\\' && !raw -> {
                    i += appendEscape(body, i, out)
                }

                c == '$' && body.getOrNull(i + 1) == '{' -> {
                    i += appendBlockTemplate(body, i, out, depth)
                }

                c == '$' && body.getOrNull(i + 1)?.let { it.isLetter() || it == '_' } == true -> {
                    i += appendTemplate(body, i, out, depth)
                }

                else -> {
                    out.append(c)
                    i++
                }
            }
        }
        return out.toString()
    }

    private fun appendEscape(
        body: String,
        at: Int,
        out: StringBuilder,
    ): Int {
        val next = body.getOrNull(at + 1) ?: throw Unresolved("끝에 남은 '\\': $body")
        if (next == 'u') {
            val hex = body.drop(at + 2).take(UNICODE_ESCAPE_DIGITS)
            out.append(hex.toIntOrNull(HEX_RADIX)?.toChar() ?: throw Unresolved("잘못된 \\u 이스케이프: $body"))
            return 2 + UNICODE_ESCAPE_DIGITS
        }
        out.append(SIMPLE_ESCAPES[next] ?: throw Unresolved("지원하지 않는 문자열 이스케이프 '\\$next': $body"))
        return 2
    }

    private fun appendTemplate(
        body: String,
        at: Int,
        out: StringBuilder,
        depth: Int,
    ): Int {
        val name = IDENTIFIER.matchAt(body, at + 1)?.value ?: throw Unresolved("보간 이름을 읽지 못했다: $body")
        out.append(reference(name, depth))
        return 1 + name.length
    }

    private fun appendBlockTemplate(
        body: String,
        at: Int,
        out: StringBuilder,
        depth: Int,
    ): Int {
        val inner = body.substring(at + 2).substringBefore('}')
        if (!IDENTIFIER_PATH.matches(inner)) throw Unresolved("지원하지 않는 보간 식: \${$inner}")
        out.append(reference(inner, depth))
        return 2 + inner.length + 1
    }
}

private class Cursor(
    private val text: String,
) {
    private var index = 0

    fun atEnd() = index >= text.length

    fun rest() = text.substring(index)

    fun skipSpaces() {
        while (index < text.length && text[index].isWhitespace()) index++
    }

    fun consume(c: Char): Boolean {
        skipSpaces()
        if (text.getOrNull(index) != c) return false
        index++
        return true
    }

    fun startsWith(prefix: String) = text.startsWith(prefix, index)

    fun startsWithAfterSpaces(prefix: String): Boolean {
        var i = index
        while (i < text.length && text[i].isWhitespace()) i++
        return text.startsWith(prefix, i)
    }

    fun takeString(): String {
        val end = ExpressionText.skipString(text, index)
        return text.substring(index, end).also { index = end }
    }

    fun takeMatch(pattern: Regex): String? = pattern.matchAt(text, index)?.value?.also { index += it.length }

    fun takeArguments(): List<String>? {
        val (arguments, end) = ExpressionText.readArguments(text, text.indexOf('(', index)) ?: return null
        index = end
        return arguments
    }
}

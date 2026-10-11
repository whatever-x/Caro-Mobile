package com.whatever.caro.feature.importer.regex

// Android는 ICU 정규식 엔진을 쓰고 호스트 JVM 테스트는 java.util.regex를 쓰므로, JVM이 받아주는 문법이 기기에서 PatternSyntaxException이 된다.
// 이 검사기는 JVM, ICU, Kotlin/Native가 같은 뜻으로 받는 문법의 부분집합만 통과시킨다.
internal object IcuRegexChecker {
    private val ALLOWED_OPTIONS = setOf("IGNORE_CASE", "MULTILINE", "DOT_MATCHES_ALL")

    fun violations(
        pattern: String,
        options: Collection<String> = emptyList(),
    ): List<String> = options.filter { it !in ALLOWED_OPTIONS }.map { "지원하지 않는 RegexOption $it" } + PatternScan(pattern).run()
}

private enum class Position { START, ATOM, QUANTIFIER, SUFFIXED }

private val INTERVAL = Regex("""\{(\d+)(?:(,)(\d*))?}""")
private val GROUP_NAME = Regex("""[A-Za-z][A-Za-z0-9]*""")
private const val SAFE_INLINE_FLAGS = "ims-"
private const val LOOKBEHIND_PREFIX_LENGTH = 4
private const val GROUP_PREFIX_LENGTH = 3

private class PatternScan(
    private val pattern: String,
) {
    private val problems = mutableListOf<String>()
    private val groups = ArrayDeque<Boolean>()
    private var index = 0
    private var position = Position.START

    fun run(): List<String> {
        while (index < pattern.length) step()
        if (groups.isNotEmpty()) problem(pattern.length, "닫히지 않은 그룹")
        return problems
    }

    private fun problem(
        at: Int,
        message: String,
    ) {
        problems += "index $at: $message"
    }

    private fun step() {
        when (val c = pattern[index]) {
            '\\' -> {
                escape()
            }

            '[' -> {
                characterClass()
            }

            '(' -> {
                openGroup()
            }

            ')' -> {
                closeGroup()
            }

            '|', '^' -> {
                advance(Position.START)
            }

            '*', '+', '?' -> {
                quantifier(c)
            }

            '{' -> {
                interval()
            }

            '}' -> {
                problem(index, "이스케이프되지 않은 '}'는 ICU에서 문법 오류다")
                advance(Position.ATOM)
            }

            else -> {
                advance(Position.ATOM)
            }
        }
    }

    private fun advance(next: Position) {
        index++
        position = next
    }

    private fun closeGroup() {
        if (groups.isEmpty()) problem(index, "짝이 없는 ')'") else groups.removeLast()
        advance(Position.ATOM)
    }

    private fun inLookBehind() = groups.any { it }

    private fun quantifier(c: Char) {
        val next =
            when {
                position == Position.ATOM -> {
                    if (c != '?' && inLookBehind()) problem(index, "lookbehind 안의 무제한 수량자 '$c'")
                    Position.QUANTIFIER
                }

                position == Position.QUANTIFIER && c != '*' -> {
                    Position.SUFFIXED
                }

                position == Position.START -> {
                    problem(index, "수량자 '$c'가 붙을 대상이 없다")
                    Position.ATOM
                }

                else -> {
                    problem(index, "수량자가 연속된다")
                    Position.SUFFIXED
                }
            }
        advance(next)
    }

    private fun interval() {
        val match = INTERVAL.matchAt(pattern, index)
        if (match == null) {
            problem(index, "수량자 모양이 아닌 이스케이프되지 않은 '{'")
            advance(Position.ATOM)
            return
        }
        val min = match.groupValues[1].toLongOrNull()
        val max = match.groupValues[3].toLongOrNull()
        val unbounded = match.groupValues[2].isNotEmpty() && match.groupValues[3].isEmpty()
        when {
            position != Position.ATOM -> problem(index, "수량자 '${match.value}'가 붙을 대상이 없거나 연속된다")
            min == null || (match.groupValues[3].isNotEmpty() && max == null) -> problem(index, "수량자 숫자가 너무 크다")
            max != null && min > max -> problem(index, "수량자 범위가 거꾸로다")
            unbounded && inLookBehind() -> problem(index, "lookbehind 안의 무제한 수량자 '${match.value}'")
        }
        index += match.value.length
        position = Position.QUANTIFIER
    }

    private fun escape() {
        val parsed = parseEscape(pattern, index, inClass = false)
        parsed.problem?.let { problem(index, it) }
        index += parsed.length
        position = Position.ATOM
    }

    private fun openGroup() {
        val at = index
        if (pattern.getOrNull(at + 1) != '?') {
            push(at, 1, lookBehind = false)
            return
        }
        val rest = pattern.substring(at + 2)
        when {
            rest.firstOrNull()?.let { it in ":=!>" } == true -> push(at, GROUP_PREFIX_LENGTH, lookBehind = false)
            rest.startsWith("<=") || rest.startsWith("<!") -> push(at, LOOKBEHIND_PREFIX_LENGTH, lookBehind = true)
            rest.startsWith("<") -> namedGroup(at, rest)
            else -> inlineFlags(at, rest)
        }
    }

    private fun namedGroup(
        at: Int,
        rest: String,
    ) {
        val end = rest.indexOf('>')
        if (end < 0 || !GROUP_NAME.matches(rest.substring(1, end))) problem(at, "이름 있는 그룹 문법이 올바르지 않다")
        push(at, 2 + maxOf(end, 0) + 1, lookBehind = false)
    }

    private fun inlineFlags(
        at: Int,
        rest: String,
    ) {
        val flags = rest.takeWhile { it != ':' && it != ')' }
        val terminator = rest.getOrNull(flags.length)
        if (flags.isEmpty() || terminator == null || flags.any { it !in SAFE_INLINE_FLAGS }) {
            problem(at, "지원하지 않는 그룹 또는 인라인 플래그 문법 '(?$flags${terminator ?: ""}'")
            push(at, 2, lookBehind = false)
        } else if (terminator == ')') {
            index = at + 2 + flags.length + 1
            position = Position.START
        } else {
            push(at, 2 + flags.length + 1, lookBehind = false)
        }
    }

    private fun push(
        at: Int,
        length: Int,
        lookBehind: Boolean,
    ) {
        groups.addLast(lookBehind)
        index = at + length
        position = Position.START
    }

    private fun characterClass() {
        val scan = ClassScan(pattern, index)
        scan.run()
        scan.problems.forEach { (at, message) -> problem(at, message) }
        index = scan.end
        position = Position.ATOM
    }
}

private class ClassScan(
    private val pattern: String,
    start: Int,
) {
    val problems = mutableListOf<Pair<Int, String>>()
    var end: Int = start + 1
        private set
    private var previous: Int? = null
    private var rangePending = false
    private var empty = true

    fun run() {
        if (pattern.getOrNull(end) == '^') end++
        if (pattern.getOrNull(end) == ']') problems += end to "클래스 첫 글자가 이스케이프되지 않은 ']'"
        while (true) {
            val c = pattern.getOrNull(end)
            if (c == null) {
                problems += end to "닫히지 않은 문자 클래스"
                return
            }
            if (c == ']') {
                end++
                return
            }
            element(c)
        }
    }

    private fun element(c: Char) {
        when {
            c == '[' -> {
                reject("클래스 안의 이스케이프되지 않은 '['는 중첩 클래스나 POSIX 클래스로 해석이 갈린다")
            }

            c == '&' && pattern.getOrNull(end + 1) == '&' -> {
                reject("클래스 교집합 '&&'")
            }

            c == '{' || c == '}' -> {
                reject("클래스 안의 이스케이프되지 않은 '$c'")
            }

            c == '\\' -> {
                escape()
            }

            c == '-' -> {
                dash()
            }

            else -> {
                val code = pattern.codePointAt(end)
                end += Character.charCount(code)
                literal(code)
            }
        }
    }

    private fun reject(message: String) {
        problems += end to message
        end++
        forget()
    }

    private fun forget() {
        previous = null
        rangePending = false
        empty = false
    }

    private fun escape() {
        val parsed = parseEscape(pattern, end, inClass = true)
        val at = end
        end += parsed.length
        when {
            parsed.problem != null -> {
                problems += at to parsed.problem
                forget()
            }

            parsed.literal != null -> {
                literal(parsed.literal)
            }

            else -> {
                set(at)
            }
        }
    }

    private fun dash() {
        val next = pattern.getOrNull(end + 1)
        when {
            next == '-' -> {
                reject("클래스 차집합 '--'")
            }

            empty || next == ']' -> {
                end++
                literal('-'.code)
            }

            previous == null || rangePending -> {
                reject("문자 집합이나 범위 뒤, 또는 범위 끝에 오는 '-'")
            }

            else -> {
                end++
                rangePending = true
            }
        }
    }

    private fun set(at: Int) {
        if (rangePending) problems += at to "범위 끝에 문자 집합이 올 수 없다"
        forget()
    }

    private fun literal(code: Int) {
        val from = previous
        if (rangePending) {
            if (from != null && from > code) problems += end to "클래스 범위가 거꾸로다"
            previous = null
            rangePending = false
        } else {
            previous = code
        }
        empty = false
    }
}

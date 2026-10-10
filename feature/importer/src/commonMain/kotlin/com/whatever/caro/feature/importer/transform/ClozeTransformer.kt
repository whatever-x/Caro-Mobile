package com.whatever.caro.feature.importer.transform

/**
 * Anki 빈칸 채우기 표기 `{{c<번호 목록>::<본문>[::<힌트>]}}`를 노트당 카드 1장으로 바꾼다.
 * 모든 번호를 한 카드에서 가리고, 중첩을 허용하며, 닫히지 않은 표기는 글자 그대로 둔다.
 */
internal object ClozeTransformer {
    private val CLOZE_MARK = Regex("\\{\\{c\\d+(?:,\\d+)*::")
    private val CLOZE_TOKEN = Regex("\\{\\{c\\d+(?:,\\d+)*::|}}")
    private const val CLOSE = "}}"
    private const val HINT_SEPARATOR = "::"
    private const val HIDDEN_WITHOUT_HINT = "..."

    private sealed interface Node {
        data class Text(
            val value: String,
        ) : Node

        class Cloze(
            val open: String,
        ) : Node {
            val children = mutableListOf<Node>()
            var hint: String? = null
        }
    }

    fun hasClozeMarkup(value: String): Boolean = CLOZE_MARK.containsMatchIn(value)

    /** 가장 바깥 빈칸마다 힌트를 대괄호로, 힌트가 없으면 `[...]`로 가린다. */
    fun front(value: String): String = render(parse(value), revealed = false)

    /** 빈칸 표기를 걷어내고 본문으로 채운 문장. 중첩 빈칸도 모두 채운다. */
    fun back(value: String): String = render(parse(value), revealed = true)

    private fun parse(value: String): List<Node> {
        val output = mutableListOf<Node>()
        val stack = ArrayDeque<Node.Cloze>()
        tokens(value).forEach { token ->
            when {
                token.startsWith("{{c") -> stack.addLast(Node.Cloze(token))
                token == CLOSE -> close(stack, output)
                stack.isNotEmpty() -> appendInside(stack.last(), token)
                else -> output += Node.Text(token)
            }
        }
        while (stack.isNotEmpty()) {
            val unclosed = stack.removeLast()
            (stack.lastOrNull()?.children ?: output) += Node.Text(literal(unclosed))
        }
        return output
    }

    private fun close(
        stack: ArrayDeque<Node.Cloze>,
        output: MutableList<Node>,
    ) {
        val closed = stack.removeLastOrNull()
        if (closed == null) {
            output += Node.Text(CLOSE)
        } else {
            (stack.lastOrNull()?.children ?: output) += closed
        }
    }

    /** 본문 뒤 첫 `::` 이후 전체가 힌트다(Anki와 같다). */
    private fun appendInside(
        cloze: Node.Cloze,
        token: String,
    ) {
        val separator = token.indexOf(HINT_SEPARATOR)
        if (separator < 0) {
            cloze.children += Node.Text(token)
        } else {
            cloze.hint = token.substring(separator + HINT_SEPARATOR.length)
            cloze.children += Node.Text(token.substring(0, separator))
        }
    }

    private fun tokens(value: String): List<String> {
        val tokens = mutableListOf<String>()
        var position = 0
        CLOZE_TOKEN.findAll(value).forEach { match ->
            if (match.range.first > position) tokens += value.substring(position, match.range.first)
            tokens += match.value
            position = match.range.last + 1
        }
        if (position < value.length) tokens += value.substring(position)
        return tokens
    }

    private fun literal(cloze: Node.Cloze): String = cloze.open + raw(cloze.children) + (cloze.hint?.let { HINT_SEPARATOR + it } ?: "")

    private fun raw(nodes: List<Node>): String =
        nodes.joinToString("") { node ->
            when (node) {
                is Node.Text -> node.value
                is Node.Cloze -> literal(node) + CLOSE
            }
        }

    private fun render(
        nodes: List<Node>,
        revealed: Boolean,
    ): String =
        nodes.joinToString("") { node ->
            when (node) {
                is Node.Text -> node.value
                is Node.Cloze -> if (revealed) render(node.children, revealed = true) else "[${node.hint ?: HIDDEN_WITHOUT_HINT}]"
            }
        }
}

package com.whatever.caro.feature.importer.transform

/**
 * HTML 주석과 `<style>`, `<script>` 블록을 내용째 지우며 결과는 가장 앞에서 시작하는 블록부터 지우는 정규식 교대와 같다.
 * 닫는 표시가 없는 여는 표시가 많으면 정규식은 여는 표시마다 값 끝까지 다시 훑어 제곱 시간이 걸린다.
 * 그래서 마지막 닫는 표시 뒤에서 끝나는 여는 표시는 더 찾지 않고 값을 한 번만 훑는다.
 */
internal object HtmlBlockStripper {
    private class BlockType(
        val open: Regex,
        val close: Regex,
    )

    private val TYPES =
        listOf(
            BlockType(Regex("<!--"), Regex("-->")),
            BlockType(Regex("<style\\b[^<>]*>", RegexOption.IGNORE_CASE), Regex("</style\\s*>", RegexOption.IGNORE_CASE)),
            BlockType(Regex("<script\\b[^<>]*>", RegexOption.IGNORE_CASE), Regex("</script\\s*>", RegexOption.IGNORE_CASE)),
        )

    private class Scan(
        val type: BlockType,
        val lastCloseStart: Int,
    ) {
        var next: MatchResult? = null
        var exhausted = lastCloseStart < 0
    }

    fun strip(value: String): String {
        val scans =
            TYPES.map { type ->
                Scan(
                    type,
                    type.close
                        .findAll(value)
                        .lastOrNull()
                        ?.range
                        ?.first ?: -1,
                )
            }
        val out = StringBuilder(value.length)
        var position = 0
        while (true) {
            val opener =
                scans
                    .mapNotNull { scan ->
                        nextOpener(scan, value, position)?.let { scan to it }
                    }.minByOrNull { (_, match) -> match.range.first }
            val (scan, match) = opener ?: break
            val close = scan.type.close.find(value, match.range.last + 1) ?: error("closer exists after lastCloseStart")
            out.append(value, position, match.range.first)
            position = close.range.last + 1
        }
        out.append(value, position, value.length)
        return out.toString()
    }

    /** 이전에 찾은 여는 표시가 아직 앞에 있으면 다시 찾지 않아 같은 구간을 두 번 훑지 않는다. */
    private fun nextOpener(
        scan: Scan,
        value: String,
        position: Int,
    ): MatchResult? {
        if (scan.exhausted) return null
        val cached = scan.next?.takeIf { it.range.first >= position }
        val match = cached ?: scan.type.open.find(value, position)
        scan.next = match
        if (match == null || match.range.last + 1 > scan.lastCloseStart) {
            scan.exhausted = true
            return null
        }
        return match
    }
}

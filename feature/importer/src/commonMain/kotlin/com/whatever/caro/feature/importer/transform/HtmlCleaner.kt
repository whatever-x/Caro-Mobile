package com.whatever.caro.feature.importer.transform

import com.whatever.caro.core.importparser.HtmlSignal
import com.whatever.caro.core.importparser.model.HtmlMode

/**
 * 지우는 태그는 파서의 HTML 신호 목록과 주석뿐이라 `x<y and y>z`나 목록 밖 태그는 글자로 남는다.
 * 정리 순서를 바꾸면 블록 태그 묶음과 `<br>`의 줄바꿈 수가 달라진다.
 */
internal object HtmlCleaner {
    private const val SOUND_OPEN = "[sound:"
    private const val SOUND_CLOSE = ']'
    private val MEDIA_TAG = Regex("<(/?)(${HtmlSignal.MEDIA_TAG_NAMES})(?=[\\s/>])([^<>]*)>", RegexOption.IGNORE_CASE)
    private val MEDIA_CONTAINERS = setOf("audio", "video", "object", "iframe")

    // 닫는 태그를 생략한 rt, rp는 다음 rt·rp·rb, </ruby>, 값 끝에서 끝난 것으로 본다.
    private const val RUBY_PART_END = "(?:</%s\\s*>|(?=<r[tbp](?=[\\s/>])|</ruby\\s*>)|$)"
    private val RP_ELEMENT = Regex("<rp(?=[\\s/>])[^<>]*>[\\s\\S]*?" + RUBY_PART_END.replace("%s", "rp"), RegexOption.IGNORE_CASE)
    private val RT_ELEMENT = Regex("<rt(?=[\\s/>])[^<>]*>([\\s\\S]*?)" + RUBY_PART_END.replace("%s", "rt"), RegexOption.IGNORE_CASE)
    private val RUBY_OR_RB_TAG = Regex("</?(?:ruby|rb)(?=[\\s/>])[^<>]*>", RegexOption.IGNORE_CASE)
    private const val BLOCK_TAG = "</?(?:div|p|ul|ol|li|h[1-6]|hr|table)(?=[\\s/>])[^<>]*>"
    private val BLOCK_RUN = Regex("$BLOCK_TAG(?:[ \\t]*$BLOCK_TAG)*", RegexOption.IGNORE_CASE)
    private val BR_TAG = Regex("</?br(?=[\\s/>])[^<>]*>", RegexOption.IGNORE_CASE)
    private val RAW_NEWLINE = Regex("\\r\\n|\\r|\\n")
    private val EXTRA_BLANK_LINES = Regex("\\n{3,}")
    private const val NO_BREAK_SPACE = '\u00A0'

    fun clean(
        value: String,
        htmlMode: HtmlMode,
    ): CleanedField {
        val (withoutSound, sounds) = stripSounds(value)
        if (htmlMode == HtmlMode.PLAIN) return CleanedField(withoutSound, sounds)
        val (withoutMedia, media) = stripMedia(HtmlBlockStripper.strip(withoutSound))
        val lines =
            rubyToText(withoutMedia)
                .let { text -> RAW_NEWLINE.replace(text, " ") }
                .let { text -> BLOCK_RUN.replace(text, "\n") }
                .let { text -> BR_TAG.replace(text, "\n") }
                .let { text -> HtmlSignal.KNOWN_TAG.replace(text, "") }
                .let(HtmlEntities::decode)
                .replace(NO_BREAK_SPACE, ' ')
                .split('\n')
                .joinToString("\n") { line -> line.trimEnd() }
        return CleanedField(EXTRA_BLANK_LINES.replace(lines, "\n\n").trim(), sounds + media)
    }

    /** 닫는 `]`가 더 없으면 뒤의 `[sound:`도 닫힐 수 없으므로 거기서 멈춰 한 번만 훑는다. */
    private fun stripSounds(value: String): Pair<String, Int> {
        val out = StringBuilder(value.length)
        var position = 0
        var count = 0
        while (true) {
            val start = value.indexOf(SOUND_OPEN, position)
            val end = if (start < 0) -1 else value.indexOf(SOUND_CLOSE, start + SOUND_OPEN.length)
            if (end < 0) break
            out.append(value, position, start)
            position = end + 1
            count++
        }
        out.append(value, position, value.length)
        return out.toString() to count
    }

    /** 다른 미디어 요소 안에 있지 않은 미디어 시작 태그만 1개로 센다. `<video><source></video>`는 1개다. */
    private fun stripMedia(value: String): Pair<String, Int> {
        var count = 0
        var depth = 0
        val stripped =
            MEDIA_TAG.replace(value) { match ->
                val (closing, rawName, rest) = match.destructured
                val name = rawName.lowercase()
                if (closing.isNotEmpty()) {
                    if (name in MEDIA_CONTAINERS) depth = maxOf(0, depth - 1)
                } else {
                    if (depth == 0) count++
                    if (name in MEDIA_CONTAINERS && !rest.trimEnd().endsWith("/")) depth++
                }
                ""
            }
        return stripped to count
    }

    private fun rubyToText(value: String): String {
        val withoutParentheses = RP_ELEMENT.replace(value, "")
        val withReadings =
            RT_ELEMENT.replace(withoutParentheses) { match ->
                val reading = match.groupValues[1].trim()
                // 태그를 뺀 읽기가 비면 괄호를 만들지 않는다.
                if (HtmlSignal.KNOWN_TAG.replace(reading, "").isBlank()) "" else "($reading)"
            }
        return RUBY_OR_RB_TAG.replace(withReadings, "")
    }
}

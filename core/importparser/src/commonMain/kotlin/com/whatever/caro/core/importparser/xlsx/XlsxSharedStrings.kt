package com.whatever.caro.core.importparser.xlsx

import com.whatever.caro.core.importparser.archive.ZipArchive

/**
 * 공유 문자열 표에서 [needed] 번호만 보관한다. 큰 통합 문서의 표 전체를 메모리에 올리지 않기 위해서다.
 * `<rPh>`(발음 표기) 안의 글자는 화면에 보이지 않으므로 뺀다.
 */
internal fun readSharedStrings(
    archive: ZipArchive,
    part: String?,
    needed: Set<Int>,
): Map<Int, String> {
    if (part == null || needed.isEmpty() || !archive.contains(part)) return emptyMap()
    val collector = SharedStringCollector(needed)
    archive.forEachEvent(part) { event -> if (collector.accept(event)) return collector.strings }
    return collector.strings
}

private class SharedStringCollector(
    private val needed: Set<Int>,
) {
    val strings = HashMap<Int, String>()
    private val lastNeeded = needed.max()
    private var index = -1
    private var current: StringBuilder? = null
    private var inPhonetic = false
    private var inText = false

    /** 필요한 마지막 번호까지 모았으면 true를 돌려준다. */
    fun accept(event: XmlEvent): Boolean {
        when (event) {
            is XmlEvent.Start -> start(event.name)
            is XmlEvent.Text -> if (inText) current?.append(event.text)
            is XmlEvent.End -> return end(event.name)
        }
        return false
    }

    private fun start(name: String) {
        when (name) {
            "si" -> {
                index++
                current = if (index in needed) StringBuilder() else null
            }

            "rPh" -> {
                inPhonetic = true
            }

            "t" -> {
                inText = !inPhonetic
            }
        }
    }

    private fun end(name: String): Boolean {
        when (name) {
            "si" -> {
                current?.let { text -> strings[index] = text.toString() }
                return index >= lastNeeded
            }

            "rPh" -> {
                inPhonetic = false
            }

            "t" -> {
                inText = false
            }
        }
        return false
    }
}

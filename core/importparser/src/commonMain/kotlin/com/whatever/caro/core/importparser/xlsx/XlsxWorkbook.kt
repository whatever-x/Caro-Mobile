package com.whatever.caro.core.importparser.xlsx

import com.whatever.caro.core.importparser.archive.ZipArchive
import okio.use

internal data class XlsxSheetRef(
    val name: String,
    val part: String?,
)

/** 시트 목록, 날짜 기준, 셀 서식 번호처럼 시트를 읽기 전에 필요한 통합 문서 정보. */
internal data class XlsxWorkbook(
    val sheets: List<XlsxSheetRef>,
    val date1904: Boolean,
    val sharedStringsPart: String?,
    val stylesPart: String?,
) {
    companion object {
        private const val WORKBOOK_PART = "xl/workbook.xml"
        private const val RELATIONS_PART = "xl/_rels/workbook.xml.rels"
        private const val PART_BASE = "xl/"
        private const val SHARED_STRINGS_TYPE = "/sharedStrings"
        private const val STYLES_TYPE = "/styles"

        fun read(archive: ZipArchive): XlsxWorkbook {
            val relations = readRelations(archive)
            var date1904 = false
            val sheets = mutableListOf<XlsxSheetRef>()
            archive.forEachEvent(WORKBOOK_PART) { event ->
                if (event !is XmlEvent.Start) return@forEachEvent
                when (event.name) {
                    "workbookPr" -> {
                        date1904 = event.attributes["date1904"].let { it == "1" || it == "true" }
                    }

                    "sheet" -> {
                        val part = event.attributes["id"]?.let(relations::get)?.target
                        sheets += XlsxSheetRef(event.attributes["name"].orEmpty(), part)
                    }
                }
            }
            return XlsxWorkbook(
                sheets = sheets,
                date1904 = date1904,
                sharedStringsPart = relations.values.firstOrNull { it.type.endsWith(SHARED_STRINGS_TYPE) }?.target,
                stylesPart = relations.values.firstOrNull { it.type.endsWith(STYLES_TYPE) }?.target,
            )
        }

        private data class Relation(
            val type: String,
            val target: String,
        )

        private fun readRelations(archive: ZipArchive): Map<String, Relation> {
            if (!archive.contains(RELATIONS_PART)) return emptyMap()
            val relations = LinkedHashMap<String, Relation>()
            archive.forEachEvent(RELATIONS_PART) { event ->
                if (event is XmlEvent.Start && event.name == "Relationship") {
                    val id = event.attributes["Id"] ?: return@forEachEvent
                    val target = event.attributes["Target"] ?: return@forEachEvent
                    relations[id] = Relation(event.attributes["Type"].orEmpty(), resolvePart(target))
                }
            }
            return relations
        }

        private fun resolvePart(target: String): String = if (target.startsWith("/")) target.removePrefix("/") else PART_BASE + target
    }
}

/** `cellXfs`의 순서대로 셀 서식 번호를 읽는다. 셀의 `s` 속성이 이 목록의 위치다. */
internal fun readCellFormatIds(
    archive: ZipArchive,
    stylesPart: String?,
): List<Int> {
    if (stylesPart == null || !archive.contains(stylesPart)) return emptyList()
    val ids = mutableListOf<Int>()
    var inCellFormats = false
    archive.forEachEvent(stylesPart) { event ->
        when {
            event is XmlEvent.Start && event.name == "cellXfs" -> inCellFormats = true
            event is XmlEvent.End && event.name == "cellXfs" -> inCellFormats = false
            event is XmlEvent.Start && event.name == "xf" && inCellFormats -> ids += event.attributes["numFmtId"]?.toIntOrNull() ?: 0
        }
    }
    return ids
}

internal inline fun ZipArchive.forEachEvent(
    part: String,
    action: (XmlEvent) -> Unit,
) {
    open(part).use { source ->
        val reader = XmlPullReader(source)
        while (true) action(reader.next() ?: break)
    }
}

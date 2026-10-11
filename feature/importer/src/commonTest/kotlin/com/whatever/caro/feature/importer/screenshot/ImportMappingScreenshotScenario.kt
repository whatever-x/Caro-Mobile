package com.whatever.caro.feature.importer.screenshot

import androidx.compose.runtime.Composable
import com.whatever.caro.core.importparser.model.HtmlMode
import com.whatever.caro.core.importparser.model.ImportFormat
import com.whatever.caro.core.importparser.model.ParsedNoteType
import com.whatever.caro.core.importparser.model.ParsedTable
import com.whatever.caro.core.model.importing.FieldMapping
import com.whatever.caro.feature.importer.mvi.ImportDeckState
import com.whatever.caro.feature.importer.mvi.ImportStep
import kotlinx.collections.immutable.persistentMapOf

internal object ImportMappingScreenshotScenario {
    const val DEFAULT_ID = "importer.mapping.default"
    const val CLOZE_ID = "importer.mapping.cloze"

    private val VOCAB_TYPE =
        ParsedNoteType(
            id = "vocab",
            name = "일본어 단어",
            fieldNames = listOf("단어", "뜻", "예문", "읽기"),
            isCloze = false,
            noteCount = 1_204,
        )
    private val CLOZE_TYPE =
        ParsedNoteType(
            id = "cloze",
            name = "빈칸 채우기",
            fieldNames = listOf("텍스트", "뒷면 추가"),
            isCloze = true,
            noteCount = 110,
        )

    private val VOCAB_MAPPING =
        FieldMapping(
            noteTypeId = "vocab",
            targets = mapOf(FieldMapping.FRONT to listOf("단어"), FieldMapping.BACK to listOf("뜻", "예문", "읽기")),
        )
    private val BASIC_TYPE =
        ParsedNoteType(
            id = "basic",
            name = "기본",
            fieldNames = listOf("앞면", "뒷면"),
            isCloze = false,
            noteCount = 200,
        )

    private val CLOZE_MAPPING =
        FieldMapping(
            noteTypeId = "cloze",
            targets = mapOf(FieldMapping.FRONT to listOf("텍스트"), FieldMapping.BACK to listOf("뒷면 추가")),
        )

    private val BASIC_MAPPING =
        FieldMapping(
            noteTypeId = "basic",
            targets = mapOf(FieldMapping.FRONT to listOf("앞면"), FieldMapping.BACK to listOf("뒷면")),
        )

    @Composable
    fun RenderDefault() {
        ImportScreenshotScenario.Render(
            ImportDeckState(
                step = ImportStep.Mapping,
                table = tableOf(VOCAB_TYPE, CLOZE_TYPE, BASIC_TYPE),
                mappings = persistentMapOf("vocab" to VOCAB_MAPPING, "cloze" to CLOZE_MAPPING, "basic" to BASIC_MAPPING),
            ),
        )
    }

    @Composable
    fun RenderCloze() {
        ImportScreenshotScenario.Render(
            ImportDeckState(
                step = ImportStep.Mapping,
                table = tableOf(CLOZE_TYPE),
                mappings = persistentMapOf("cloze" to CLOZE_MAPPING),
            ),
        )
    }

    private fun tableOf(vararg noteTypes: ParsedNoteType): ParsedTable =
        ParsedTable(
            format = ImportFormat.APKG,
            sourceName = "일본어.apkg",
            htmlMode = HtmlMode.HTML,
            noteTypes = noteTypes.toList(),
            rows = emptyList(),
        )
}

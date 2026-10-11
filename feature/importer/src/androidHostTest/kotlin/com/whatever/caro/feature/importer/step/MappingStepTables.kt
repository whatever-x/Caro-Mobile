package com.whatever.caro.feature.importer.step

import com.whatever.caro.core.importparser.model.HtmlMode
import com.whatever.caro.core.importparser.model.ImportFormat
import com.whatever.caro.core.importparser.model.ParsedNoteType
import com.whatever.caro.core.importparser.model.ParsedTable
import com.whatever.caro.core.model.importing.FieldMapping
import com.whatever.caro.feature.importer.transform.defaultFieldMappings
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.toPersistentMap

/** 항목 연결 화면 테스트가 쓰는 읽기 결과다. 연결 화면은 노트 유형 요약만 보므로 rows는 비운다. 이름과 장수는 Figma 프레임의 표시 값이다. */
internal object MappingStepTables {
    const val VOCAB_ID = "vocab"
    const val VOCAB_NAME = "일본어 단어"
    const val CLOZE_ID = "cloze"
    const val CLOZE_NAME = "빈칸 채우기"
    const val IMAGE_OCCLUSION_ID = "io"
    const val IMAGE_OCCLUSION_NAME = "Image Occlusion"
    const val WIDE_ID = "wide"

    val VOCAB_FIELDS = listOf("단어", "뜻", "예문", "읽기")
    val CLOZE_FIELDS = listOf("텍스트", "뒷면 추가")

    /** 앞 여섯 개는 Figma 항목 고르기 시트의 항목 이름이고 나머지는 번호가 붙는다. */
    fun wideFieldNames(count: Int): List<String> = (listOf("단어", "뜻", "예문", "읽기", "품사", "한자") + (7..count).map { n -> "항목 $n" }).take(count)

    fun vocab(
        id: String = VOCAB_ID,
        name: String = VOCAB_NAME,
        fieldNames: List<String> = VOCAB_FIELDS,
        noteCount: Int = 1_204,
        selectedNoteCount: Int? = null,
    ) = ParsedNoteType(
        id = id,
        name = name,
        fieldNames = fieldNames,
        isCloze = false,
        noteCount = noteCount,
        selectedNoteCount = selectedNoteCount,
    )

    fun wide(
        fieldCount: Int,
        id: String = WIDE_ID,
        name: String = VOCAB_NAME,
        isCloze: Boolean = false,
        fieldNames: List<String> = wideFieldNames(fieldCount),
    ) = ParsedNoteType(
        id = id,
        name = name,
        fieldNames = fieldNames,
        isCloze = isCloze,
        noteCount = 1_204,
    )

    fun cloze(
        id: String = CLOZE_ID,
        name: String = CLOZE_NAME,
        fieldNames: List<String> = CLOZE_FIELDS,
        noteCount: Int = 110,
    ) = ParsedNoteType(id = id, name = name, fieldNames = fieldNames, isCloze = true, noteCount = noteCount)

    fun imageOcclusion(
        id: String = IMAGE_OCCLUSION_ID,
        noteCount: Int = 40,
        selectedNoteCount: Int? = null,
    ) = ParsedNoteType(
        id = id,
        name = IMAGE_OCCLUSION_NAME,
        fieldNames = listOf("Image", "Header"),
        isCloze = false,
        noteCount = noteCount,
        isImageOcclusion = true,
        selectedNoteCount = selectedNoteCount,
    )

    fun table(vararg noteTypes: ParsedNoteType): ParsedTable =
        ParsedTable(
            format = ImportFormat.APKG,
            sourceName = "일본어.apkg",
            htmlMode = HtmlMode.HTML,
            noteTypes = noteTypes.toList(),
            rows = emptyList(),
        )

    /** 상태가 처음 만드는 연결이다. 첫 항목은 앞면, 나머지는 뒷면에 놓이고 Image Occlusion은 빠진다. */
    fun defaultMappings(table: ParsedTable): PersistentMap<String, FieldMapping> = defaultFieldMappings(table).toPersistentMap()

    fun mapping(
        noteTypeId: String,
        front: List<String>,
        back: List<String>,
        excluded: Boolean = false,
    ) = FieldMapping(
        noteTypeId = noteTypeId,
        targets = mapOf(FieldMapping.FRONT to front, FieldMapping.BACK to back),
        excluded = excluded,
    )

    fun mappingsOf(vararg mappings: FieldMapping): PersistentMap<String, FieldMapping> =
        mappings.associateBy { mapping -> mapping.noteTypeId }.toPersistentMap()

    /** Figma 항목이 많은 유형 프레임의 연결이다. 앞면은 단어 하나이고 뒷면은 뜻, 예문, 읽기, 품사 넷이다. */
    fun wideMappings(fieldCount: Int = 20): PersistentMap<String, FieldMapping> {
        val names = wideFieldNames(fieldCount)
        return mappingsOf(mapping(WIDE_ID, front = names.take(1), back = names.slice(1..4)))
    }
}

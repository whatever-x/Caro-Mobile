package com.whatever.caro.feature.importer.logic

import com.whatever.caro.core.importparser.model.ParsedNoteType
import com.whatever.caro.core.importparser.model.ParsedTable
import com.whatever.caro.core.model.importing.FieldMapping
import com.whatever.caro.feature.importer.support.ImportTestTables
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class MappingEditorTest : FunSpec() {
    init {
        context("항목 고르기") {
            test("뒷면의 b를 풀면 a는 1번째, c는 2번째로 당겨진다") {
                val picked = pick(connection(), FieldMapping.BACK, "a", "b", "c")

                val result = toggleField(picked, FieldMapping.BACK, "b")

                result.backFields shouldBe listOf("a", "c")
            }

            test("푼 항목을 다시 고르면 고른 항목 맨 뒤 번호가 된다") {
                val released = toggleField(connection(back = listOf("a", "b")), FieldMapping.BACK, "a")

                toggleField(released, FieldMapping.BACK, "a").backFields shouldBe listOf("b", "a")
            }

            test("가져오기 켜고 끔은 항목을 골라도 그대로다") {
                val result = toggleField(connection(front = listOf("a")).copy(excluded = true), FieldMapping.BACK, "b")

                result.excluded shouldBe true
            }
        }

        context("다른 면 항목 옮기기") {
            test("뒷면에서 앞면 항목을 고르면 앞면에서 빼고 뒷면 맨 뒤에 넣는다") {
                val result = toggleField(connection(front = listOf("a"), back = listOf("b", "c")), FieldMapping.BACK, "a")

                result.frontFields shouldBe emptyList()
                result.backFields shouldBe listOf("b", "c", "a")
            }

            test("앞면에서 뒷면 항목을 고르면 반대 방향으로 옮긴다") {
                val result = toggleField(connection(front = listOf("a"), back = listOf("b", "c")), FieldMapping.FRONT, "c")

                result.frontFields shouldBe listOf("a", "c")
                result.backFields shouldBe listOf("b")
            }

            test("항목이 빠진 면에 남은 항목은 번호가 당겨진다") {
                val result = toggleField(connection(front = listOf("a", "b", "c")), FieldMapping.BACK, "b")

                result.frontFields shouldBe listOf("a", "c")
                result.backFields shouldBe listOf("b")
            }

            test("한 항목은 어느 순서로 눌러도 한 면에만 놓인다") {
                var mapping = connection(front = listOf("a", "b"), back = listOf("c"))
                listOf(
                    FieldMapping.BACK to "a",
                    FieldMapping.FRONT to "c",
                    FieldMapping.FRONT to "a",
                    FieldMapping.BACK to "b",
                ).forEach { (target, name) -> mapping = toggleField(mapping, target, name) }

                val placed = mapping.frontFields + mapping.backFields
                placed.toSet().size shouldBe placed.size
            }
        }

        context("항목 연결 목록") {
            test("모든 하위 덱을 고르면 노트 유형이 원래 순서대로 모두 나온다") {
                val table = ImportTestTables.apkg("modern_anki21b_main.apkg", "default")

                mappingListNoteTypes(table).map { it.id } shouldBe listOf(BASIC, REVERSED, VOCAB, CLOZE)
            }

            test("고른 하위 덱에 노트가 없는 유형은 나오지 않는다") {
                val grammarOnly = ImportTestTables.apkg("modern_anki21b_main.apkg", "selection-0")

                mappingListNoteTypes(grammarOnly).map { it.id } shouldBe listOf(BASIC, REVERSED, CLOZE)
            }

            test("Image Occlusion 유형은 고른 범위에 그 노트가 있으면 나온다") {
                val table = ImportTestTables.apkg("modern_image_occlusion.apkg", "default")

                mappingListNoteTypes(table).map { it.id } shouldBe listOf(IO_BASIC, IO_OCCLUSION)
            }

            test("Image Occlusion 유형은 고른 범위에 그 노트가 없으면 나오지 않는다") {
                val table = ImportTestTables.apkg("modern_image_occlusion.apkg", "default").withSelectedCount(IO_OCCLUSION, 0)

                mappingListNoteTypes(table).map { it.id } shouldBe listOf(IO_BASIC)
            }

            test("고른 범위의 노트 수를 모르면 전체 노트 수로 판단한다") {
                val table = ImportTestTables.apkg("modern_anki21b_main.apkg", "selection-0")
                mappingListNoteTypes(table).map { it.id } shouldBe listOf(BASIC, REVERSED, CLOZE)

                mappingListNoteTypes(table.withSelectedCount(VOCAB, null)).map { it.id } shouldBe listOf(BASIC, REVERSED, VOCAB, CLOZE)
            }
        }
    }

    private fun connection(
        front: List<String> = emptyList(),
        back: List<String> = emptyList(),
    ) = FieldMapping(noteTypeId = "t", targets = mapOf(FieldMapping.FRONT to front, FieldMapping.BACK to back))

    private fun pick(
        start: FieldMapping,
        target: String,
        vararg names: String,
    ): FieldMapping = names.fold(start) { mapping, name -> toggleField(mapping, target, name) }

    private fun ParsedTable.withSelectedCount(
        noteTypeId: String,
        count: Int?,
    ): ParsedTable =
        copy(
            noteTypes =
                noteTypes.map { type: ParsedNoteType ->
                    if (type.id ==
                        noteTypeId
                    ) {
                        type.copy(selectedNoteCount = count)
                    } else {
                        type
                    }
                },
        )

    private companion object {
        const val BASIC = "1700000001"
        const val REVERSED = "1700000002"
        const val VOCAB = "1700000003"
        const val CLOZE = "1700000004"
        const val IO_BASIC = "1791602016919"
        const val IO_OCCLUSION = "1791602016924"
    }
}

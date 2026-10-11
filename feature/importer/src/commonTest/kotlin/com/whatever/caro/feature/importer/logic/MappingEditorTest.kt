package com.whatever.caro.feature.importer.logic

import com.whatever.caro.core.importparser.model.ParsedNoteType
import com.whatever.caro.core.importparser.model.ParsedTable
import com.whatever.caro.core.model.importing.FieldMapping
import com.whatever.caro.feature.importer.support.ImportTestTables
import com.whatever.caro.feature.importer.transform.defaultFieldMappings
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class MappingEditorTest : FunSpec() {
    init {
        context("항목 고르기") {
            test("뒷면에서 a, b, c를 차례로 고르면 고른 순서대로 1, 2, 3번째가 된다") {
                val mapping = pick(connection(), FieldMapping.BACK, "a", "b", "c")

                mapping.backFields shouldBe listOf("a", "b", "c")
            }

            test("뒷면의 b를 풀면 a는 1번째, c는 2번째로 당겨진다") {
                val picked = pick(connection(), FieldMapping.BACK, "a", "b", "c")

                val result = toggleField(picked, FieldMapping.BACK, "b")

                result.mapping.backFields shouldBe listOf("a", "c")
            }

            test("처음 고르는 항목은 이동 정보가 없다") {
                toggleField(connection(), FieldMapping.BACK, "a").move shouldBe null
            }

            test("이미 고른 항목을 다시 누르면 이동 정보 없이 선택만 푼다") {
                val result = toggleField(connection(back = listOf("a", "b")), FieldMapping.BACK, "a")

                result.move shouldBe null
                result.mapping.backFields shouldBe listOf("b")
            }

            test("푼 항목을 다시 고르면 고른 항목 맨 뒤 번호가 된다") {
                val released = toggleField(connection(back = listOf("a", "b")), FieldMapping.BACK, "a").mapping

                toggleField(released, FieldMapping.BACK, "a").mapping.backFields shouldBe listOf("b", "a")
            }

            test("가져오기 켜고 끔은 항목을 골라도 그대로다") {
                val result = toggleField(connection(front = listOf("a")).copy(excluded = true), FieldMapping.BACK, "b")

                result.mapping.excluded shouldBe true
            }

            test("구역 키가 없던 연결에도 항목을 고를 수 있다") {
                val result = toggleField(FieldMapping(noteTypeId = "t", targets = emptyMap()), FieldMapping.BACK, "a")

                result.mapping.backFields shouldBe listOf("a")
                result.mapping.frontFields shouldBe emptyList()
            }

            test("받은 연결은 바꾸지 않고 새 연결을 돌려준다") {
                val original = connection(front = listOf("a"), back = listOf("b"))
                val snapshot = original.copy()

                toggleField(original, FieldMapping.BACK, "a")

                original shouldBe snapshot
            }
        }

        context("다른 면 항목 옮기기") {
            test("뒷면에서 앞면 항목을 고르면 앞면에서 빼고 뒷면 맨 뒤에 넣는다") {
                val result = toggleField(connection(front = listOf("a"), back = listOf("b", "c")), FieldMapping.BACK, "a")

                result.mapping.frontFields shouldBe emptyList()
                result.mapping.backFields shouldBe listOf("b", "c", "a")
            }

            test("옮기면 빠진 면, 들어간 면, 들어간 번호를 이동 정보로 돌려준다") {
                val result = toggleField(connection(front = listOf("a"), back = listOf("b", "c")), FieldMapping.BACK, "a")

                result.move shouldBe FieldMove(fromTarget = FieldMapping.FRONT, toTarget = FieldMapping.BACK, position = 3)
            }

            test("앞면에서 뒷면 항목을 고르면 반대 방향으로 옮기고 이동 정보도 반대다") {
                val result = toggleField(connection(front = listOf("a"), back = listOf("b", "c")), FieldMapping.FRONT, "c")

                result.mapping.frontFields shouldBe listOf("a", "c")
                result.mapping.backFields shouldBe listOf("b")
                result.move shouldBe FieldMove(fromTarget = FieldMapping.BACK, toTarget = FieldMapping.FRONT, position = 2)
            }

            test("항목이 빠진 면에 남은 항목은 번호가 당겨진다") {
                val result = toggleField(connection(front = listOf("a", "b", "c")), FieldMapping.BACK, "b")

                result.mapping.frontFields shouldBe listOf("a", "c")
                result.mapping.backFields shouldBe listOf("b")
            }

            test("한 항목은 어느 순서로 눌러도 한 면에만 놓인다") {
                var mapping = connection(front = listOf("a", "b"), back = listOf("c"))
                listOf(
                    FieldMapping.BACK to "a",
                    FieldMapping.FRONT to "c",
                    FieldMapping.FRONT to "a",
                    FieldMapping.BACK to "b",
                ).forEach { (target, name) -> mapping = toggleField(mapping, target, name).mapping }

                val placed = mapping.frontFields + mapping.backFields
                placed.toSet().size shouldBe placed.size
            }
        }

        context("빈칸 유형") {
            test("빈칸 유형도 앞면 구역을 빈칸 문장, 뒷면 구역을 추가 설명으로 쓴다") {
                val mapping = clozeMapping()

                mapping.frontFields shouldBe listOf("Text")
                mapping.backFields shouldBe listOf("Back Extra")
            }

            test("빈칸 문장에도 항목을 여러 개 고르면 고른 순서대로 쌓인다") {
                val result = toggleField(clozeMapping(), FieldMapping.FRONT, "Back Extra")

                result.mapping.frontFields shouldBe listOf("Text", "Back Extra")
                result.mapping.backFields shouldBe emptyList()
                result.move shouldBe FieldMove(fromTarget = FieldMapping.BACK, toTarget = FieldMapping.FRONT, position = 2)
            }

            test("추가 설명에 빈칸 문장 항목을 고르면 추가 설명 맨 뒤로 옮긴다") {
                val result = toggleField(clozeMapping(), FieldMapping.BACK, "Text")

                result.mapping.frontFields shouldBe emptyList()
                result.mapping.backFields shouldBe listOf("Back Extra", "Text")
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

            test("고른 범위가 빈칸 유형뿐이면 빈칸 유형만 나온다") {
                val clozeOnly = ImportTestTables.apkg("legacy_edge_cases.apkg", "selection-1")

                mappingListNoteTypes(clozeOnly).map { it.id } shouldBe listOf(CLOZE)
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
                val table = ImportTestTables.apkg("legacy_edge_cases.apkg", "default")
                mappingListNoteTypes(table).map { it.id } shouldBe listOf(BASIC)

                mappingListNoteTypes(table.withSelectedCount(CLOZE, null)).map { it.id } shouldBe listOf(BASIC, CLOZE)
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
    ): FieldMapping = names.fold(start) { mapping, name -> toggleField(mapping, target, name).mapping }

    private fun clozeMapping(): FieldMapping {
        val table = ImportTestTables.apkg("legacy_edge_cases.apkg", "all")
        return defaultFieldMappings(table).getValue(CLOZE)
    }

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

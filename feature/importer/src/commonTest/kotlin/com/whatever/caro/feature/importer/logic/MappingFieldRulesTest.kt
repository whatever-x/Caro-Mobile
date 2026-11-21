package com.whatever.caro.feature.importer.logic

import com.whatever.caro.core.importparser.model.ParsedNoteType
import com.whatever.caro.core.model.importing.FieldMapping
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class MappingFieldRulesTest : FunSpec() {
    init {
        context("항목 고르기 시트로 바꾸는 기준") {
            test("항목이 12개면 칩으로 그린다") {
                usesFieldPicker(noteType(fieldCount = 12)) shouldBe false
            }

            test("항목이 13개가 되면 고르기 시트를 쓴다") {
                usesFieldPicker(noteType(fieldCount = 13)) shouldBe true
            }
        }

        context("고른 항목 요약") {
            test("하나만 골랐으면 그 이름만 보여준다") {
                summarizeChosenFields(listOf("단어")) shouldBe ChosenFieldsSummary(names = listOf("단어"), moreCount = 0)
            }

            test("넷을 골랐으면 앞의 셋만 보여주고 1개가 남는다") {
                summarizeChosenFields(listOf("뜻", "예문", "읽기", "품사")) shouldBe
                    ChosenFieldsSummary(names = listOf("뜻", "예문", "읽기"), moreCount = 1)
            }

            test("보여주는 이름은 고른 순서를 따른다") {
                summarizeChosenFields(listOf("읽기", "뜻", "단어", "예문")).names shouldBe listOf("읽기", "뜻", "단어")
            }
        }

        context("시트 줄") {
            val names = listOf("단어", "뜻", "예문", "읽기", "품사", "한자")
            val mapping = mapping(front = listOf("단어"), back = listOf("뜻", "예문", "읽기", "품사"))

            test("뒷면 시트는 뒷면에 고른 항목에 고른 순서대로 1부터 번호를 붙인다") {
                val rows = pickerRows(names, mapping, FieldMapping.BACK)

                rows.map { row -> row.name to row.order } shouldBe
                    listOf("단어" to null, "뜻" to 1, "예문" to 2, "읽기" to 3, "품사" to 4, "한자" to null)
            }

            test("뒷면 시트는 앞면에 놓인 항목에만 다른 면 표시를 한다") {
                val rows = pickerRows(names, mapping, FieldMapping.BACK)

                rows.filter { row -> row.isInOtherSide }.map { row -> row.name } shouldBe listOf("단어")
            }

            test("앞면 시트는 앞면에 고른 항목에 번호를 붙이고 뒷면에 놓인 항목에 다른 면 표시를 한다") {
                val rows = pickerRows(names, mapping, FieldMapping.FRONT)

                rows shouldBe
                    listOf(
                        PickerRow(name = "단어", order = 1, isInOtherSide = false),
                        PickerRow(name = "뜻", order = null, isInOtherSide = true),
                        PickerRow(name = "예문", order = null, isInOtherSide = true),
                        PickerRow(name = "읽기", order = null, isInOtherSide = true),
                        PickerRow(name = "품사", order = null, isInOtherSide = true),
                        PickerRow(name = "한자", order = null, isInOtherSide = false),
                    )
            }

            test("번호는 고른 순서를 따라 원본 순서와 달라질 수 있다") {
                val reordered = mapping(front = emptyList(), back = listOf("읽기", "뜻"))

                pickerRows(names, reordered, FieldMapping.BACK)
                    .filter { row -> row.order != null }
                    .associate { row -> row.name to row.order } shouldBe mapOf("읽기" to 1, "뜻" to 2)
            }
        }
    }

    private fun mapping(
        front: List<String>,
        back: List<String>,
    ) = FieldMapping(noteTypeId = "t", targets = mapOf(FieldMapping.FRONT to front, FieldMapping.BACK to back))

    private fun noteType(fieldCount: Int) =
        ParsedNoteType(
            id = "t",
            name = "유형",
            fieldNames = List(fieldCount) { index -> "항목 ${index + 1}" },
            isCloze = false,
            noteCount = 10,
        )
}

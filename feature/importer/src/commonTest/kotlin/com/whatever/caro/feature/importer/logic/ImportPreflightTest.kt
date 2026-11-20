package com.whatever.caro.feature.importer.logic

import com.whatever.caro.core.importparser.model.ParsedSheet
import com.whatever.caro.core.importparser.model.ParsedSubdeck
import com.whatever.caro.core.importparser.model.ParsedTable
import com.whatever.caro.core.model.importing.ImportLimits
import com.whatever.caro.feature.importer.support.ImportTestTables
import com.whatever.caro.feature.importer.transform.ImportPreviewBuilder
import com.whatever.caro.feature.importer.transform.defaultFieldMappings
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ImportPreflightTest : FunSpec() {
    init {
        context("엑셀") {
            test("첫 시트가 제목 행과 10,000줄이면 Ok다") {
                preflight(book(MAX_CARDS, 3)) shouldBe ImportPreflight.Ok
            }

            test("첫 시트가 10,001줄이고 후보 시트가 있으면 SheetFirst다") {
                preflight(book(MAX_CARDS + 1, 3)) shouldBe ImportPreflight.SheetFirst
            }

            test("첫 시트와 값 있는 행이 있는 다른 시트가 모두 상한을 넘으면 TooMany다") {
                preflight(book(MAX_CARDS + 1, MAX_CARDS + 2, 0)) shouldBe ImportPreflight.TooMany
            }

            test("첫 시트에 데이터 행이 없으면 후보 시트를 시트 순서대로 담은 PickSheet다") {
                val table = book(0, 0, 4, MAX_CARDS + 2, 2, titleRow = false)

                preflight(table) shouldBe ImportPreflight.PickSheet(listOf(table.sheets[2], table.sheets[4]))
            }

            test("첫 시트에 제목 행만 있어도 PickSheet다") {
                val table = book(0, 3)

                preflight(table) shouldBe ImportPreflight.PickSheet(listOf(table.sheets[1]))
            }

            test("제목 행만 있는 다른 시트도 값 있는 행이 있으므로 후보다") {
                val table = book(0, 1, titleRow = false)

                preflight(table) shouldBe ImportPreflight.PickSheet(listOf(table.sheets[1]))
            }

            test("첫 시트가 비고 값 있는 행이 있는 다른 시트가 모두 상한을 넘으면 TooMany다") {
                preflight(book(0, 0, MAX_CARDS + 2, titleRow = false)) shouldBe ImportPreflight.TooMany
            }

            test("모든 시트가 비면 Ok이고 이어지는 카드 0장 판정에서 NoContent가 된다") {
                val table = book(0, 0, 0, titleRow = false).copy(noteTypes = emptyList())

                preflight(table) shouldBe ImportPreflight.Ok
                val preview = ImportPreviewBuilder().build(table, defaultFieldMappings(table), EMPTY_SIDE)
                noCardsKind(table, preview) shouldBe NoCardsKind.NoContent
            }

            test("후보로 다시 읽은 시트가 0행이면 그 시트를 빼고 남은 후보로 PickSheet다") {
                val table = book(0, 1, 0, 5, titleRow = false)

                preflight(table, sheetIndex = 1) shouldBe ImportPreflight.PickSheet(listOf(table.sheets[3]))
            }

            test("후보 시트는 제목 행을 포함해 값 있는 행이 1 이상 10,001 이하인 시트다") {
                listOf(0 to false, 1 to true, MAX_CARDS + 1 to true, MAX_CARDS + 2 to false).forEach { (rowCount, expected) ->
                    isCandidateSheet(ParsedSheet(index = 0, name = "시트", rowCount = rowCount)) shouldBe expected
                }
            }
        }

        context("Anki") {
            test("Image Occlusion을 뺀 노트 합계가 10,000이면 Ok다") {
                preflight(anki(noteCounts = listOf(6_000, 4_500), imageOcclusionCounts = listOf(500, 0))) shouldBe ImportPreflight.Ok
            }

            test("Image Occlusion을 뺀 노트 합계가 10,000을 넘으면 SubdeckFirst다") {
                preflight(anki(noteCounts = listOf(6_000, 4_501), imageOcclusionCounts = listOf(500, 0))) shouldBe
                    ImportPreflight.SubdeckFirst
            }

            test("합계가 상한을 넘고 하위 덱이 하나뿐이면 고를 수 있는 조합이 없어 TooMany다") {
                preflight(anki(noteCounts = listOf(10_001), imageOcclusionCounts = listOf(0))) shouldBe ImportPreflight.TooMany
            }

            test("혼자서 상한 안에 드는 하위 덱이 하나라도 있으면 SubdeckFirst다") {
                preflight(anki(noteCounts = listOf(10_001, 1), imageOcclusionCounts = listOf(0, 0))) shouldBe ImportPreflight.SubdeckFirst
            }

            test("합계가 상한을 넘어도 노트가 정확히 상한인 하위 덱이 있으면 SubdeckFirst다") {
                preflight(anki(noteCounts = listOf(MAX_CARDS, MAX_CARDS + 1), imageOcclusionCounts = listOf(0, 0))) shouldBe
                    ImportPreflight.SubdeckFirst
            }

            test("Image Occlusion만 있는 하위 덱은 고를 수 있는 하위 덱으로 세지 않는다") {
                preflight(anki(noteCounts = listOf(10_001, 300), imageOcclusionCounts = listOf(0, 300))) shouldBe ImportPreflight.TooMany
            }

            test("하위 덱 요약만 읽은 10,001노트 파일은 SubdeckFirst다") {
                preflight(ImportTestTables.apkgSummary("large_10001_notes_modern.apkg")) shouldBe ImportPreflight.SubdeckFirst
            }
        }

        context("텍스트와 붙여넣기") {
            test("파서가 상한에서 읽기를 멈췄으면 보관한 행이 상한 이하여도 TooMany다") {
                val pasted = ImportTestTables.text(sourceName = "붙여 넣은 카드", rows = MAX_CARDS).copy(rowLimitReached = true)

                preflight(pasted) shouldBe ImportPreflight.TooMany
            }
        }

        test("낮춘 상한을 주면 그 상한으로 판정한다") {
            val lowered = 5_000

            preflight(ImportTestTables.text(rows = lowered), maxCards = lowered) shouldBe ImportPreflight.Ok
            preflight(ImportTestTables.text(rows = lowered + 1), maxCards = lowered) shouldBe ImportPreflight.TooMany
        }
    }

    /**
     * 파서가 읽은 모양을 흉내 낸 책이다. 첫 시트의 행 수에는 제목 행이 들어가고, 데이터 행이 상한 + 1이면 읽기를 멈춘다.
     * [otherRowCounts]는 두 번째 시트부터의 값 있는 행 수다.
     */
    private fun book(
        firstDataRows: Int,
        vararg otherRowCounts: Int,
        titleRow: Boolean = true,
    ): ParsedTable {
        val table = ImportTestTables.xlsx(sheetRows = listOf(firstDataRows) + otherRowCounts.toList(), hasHeaderRow = titleRow)
        val firstRowCount = firstDataRows + if (titleRow) 1 else 0
        return table.copy(
            sheets = listOf(table.sheets.first().copy(rowCount = firstRowCount)) + table.sheets.drop(1),
            rowLimitReached = firstDataRows > MAX_CARDS,
        )
    }

    private fun anki(
        noteCounts: List<Int>,
        imageOcclusionCounts: List<Int>,
    ): ParsedTable =
        ImportTestTables.apkgSummary("modern_anki21b_main.apkg").copy(
            subdecks =
                noteCounts.zip(imageOcclusionCounts).mapIndexed { index, (notes, imageOcclusion) ->
                    ParsedSubdeck(id = "deck-$index", path = "덱::$index", noteCount = notes, imageOcclusionNoteCount = imageOcclusion)
                },
        )

    private companion object {
        const val MAX_CARDS = ImportLimits.MAX_CARDS
        const val EMPTY_SIDE = "(비어 있음)"
    }
}

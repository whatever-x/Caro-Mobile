package com.whatever.caro.feature.importer.logic

import com.whatever.caro.core.importparser.model.HtmlMode
import com.whatever.caro.core.importparser.model.ImportFormat
import com.whatever.caro.core.importparser.model.ParsedNoteType
import com.whatever.caro.core.importparser.model.ParsedTable
import com.whatever.caro.core.model.importing.FieldMapping
import com.whatever.caro.feature.importer.transform.ImportCard
import com.whatever.caro.feature.importer.transform.ImportPreview
import com.whatever.caro.feature.importer.transform.ImportPreviewStats
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.collections.immutable.toImmutableList

class ImportSelectionRulesMappingTest : FunSpec() {
    init {
        context("가져올 수 있는 노트 유형") {
            test("Image Occlusion 유형은 뺀다") {
                val table =
                    apkgTable(
                        noteTypes =
                            listOf(
                                noteType("basic", noteCount = 10),
                                noteType("io", noteCount = 40, isImageOcclusion = true),
                            ),
                    )

                importableNoteTypes(table).map { it.id } shouldBe listOf("basic")
            }

            test("고른 범위에 노트가 없는 유형은 뺀다") {
                val table =
                    apkgTable(
                        noteTypes =
                            listOf(
                                noteType("basic", noteCount = 10, selectedNoteCount = 10),
                                noteType("outside", noteCount = 5, selectedNoteCount = 0),
                            ),
                    )

                importableNoteTypes(table).map { it.id } shouldBe listOf("basic")
            }

            test("고른 범위의 노트 수를 모르면 노트 수로 판단해 유형을 남긴다") {
                val table = apkgTable(noteTypes = listOf(noteType("basic", noteCount = 3, selectedNoteCount = null)))

                importableNoteTypes(table).map { it.id } shouldBe listOf("basic")
            }

            test("유형의 원래 순서를 지킨다") {
                val table = apkgTable(noteTypes = listOf(noteType("b", noteCount = 1), noteType("a", noteCount = 1)))

                importableNoteTypes(table).map { it.id } shouldBe listOf("b", "a")
            }
        }

        context("미리보기를 막는 연결 이유") {
            val front = mapOf(FieldMapping.FRONT to listOf("앞면"))
            val both = mapOf(FieldMapping.FRONT to listOf("앞면"), FieldMapping.BACK to listOf("뒷면"))

            test("켠 유형이 하나도 없으면 유형을 고르라는 이유다") {
                val table = apkgTable(noteTypes = listOf(noteType("a", noteCount = 1), noteType("b", noteCount = 1)))
                val mappings =
                    mapOf(
                        "a" to FieldMapping("a", both, excluded = true),
                        "b" to FieldMapping("b", both, excluded = true),
                    )

                mappingBlockReason(table, mappings) shouldBe MappingBlockReason.NoTypeEnabled
            }

            test("켠 유형이 모두 앞면과 뒷면에 고른 항목이 없으면 항목을 고르라는 이유다") {
                val table = apkgTable(noteTypes = listOf(noteType("a", noteCount = 1), noteType("b", noteCount = 1)))
                val mappings =
                    mapOf(
                        "a" to FieldMapping("a", emptyMap()),
                        "b" to FieldMapping("b", mapOf(FieldMapping.FRONT to emptyList(), FieldMapping.BACK to emptyList())),
                    )

                mappingBlockReason(table, mappings) shouldBe MappingBlockReason.NoFieldSelected
            }

            test("한쪽 면만 비어 있는 유형은 막지 않는다") {
                val table = apkgTable(noteTypes = listOf(noteType("a", noteCount = 1)))

                mappingBlockReason(table, mapOf("a" to FieldMapping("a", front))) shouldBe null
            }

            test("항목이 빈 유형이 있어도 다른 켠 유형에 고른 항목이 있으면 막지 않는다") {
                val table = apkgTable(noteTypes = listOf(noteType("a", noteCount = 1), noteType("b", noteCount = 1)))
                val mappings = mapOf("a" to FieldMapping("a", emptyMap()), "b" to FieldMapping("b", both))

                mappingBlockReason(table, mappings) shouldBe null
            }

            test("켠 유형이 없으면 끈 유형의 항목 여부와 관계없이 유형을 고르라는 이유가 먼저다") {
                val table = apkgTable(noteTypes = listOf(noteType("a", noteCount = 1), noteType("b", noteCount = 1)))
                val mappings = mapOf("a" to FieldMapping("a", both, excluded = true), "b" to FieldMapping("b", emptyMap(), excluded = true))

                mappingBlockReason(table, mappings) shouldBe MappingBlockReason.NoTypeEnabled
            }

            test("가져올 수 있는 유형이 Image Occlusion뿐이면 가져올 유형이 없다는 이유다") {
                val table = apkgTable(noteTypes = listOf(noteType("io", noteCount = 40, isImageOcclusion = true)))

                mappingBlockReason(table, emptyMap()) shouldBe MappingBlockReason.NoImportableType
            }

            test("고른 범위의 유형이 Image Occlusion뿐이면 범위 밖 일반 유형이 있어도 가져올 유형이 없다는 이유다") {
                val table =
                    apkgTable(
                        noteTypes =
                            listOf(
                                noteType("basic", noteCount = 5, selectedNoteCount = 0),
                                noteType("io", noteCount = 40, isImageOcclusion = true, selectedNoteCount = 40),
                            ),
                    )

                mappingBlockReason(table, mapOf("basic" to FieldMapping("basic", both))) shouldBe MappingBlockReason.NoImportableType
            }

            test("고른 범위에 노트가 없는 유형은 켜져 있고 항목이 있어도 이유 계산에서 빠진다") {
                val table =
                    apkgTable(
                        noteTypes =
                            listOf(
                                noteType("outside", noteCount = 5, selectedNoteCount = 0),
                                noteType("inside", noteCount = 9, selectedNoteCount = 9),
                            ),
                    )
                val mappings =
                    mapOf(
                        "outside" to FieldMapping("outside", both),
                        "inside" to FieldMapping("inside", both, excluded = true),
                    )

                mappingBlockReason(table, mappings) shouldBe MappingBlockReason.NoTypeEnabled
            }

            test("켠 유형에 고른 항목이 있으면 막지 않는다") {
                val table = apkgTable(noteTypes = listOf(noteType("a", noteCount = 1)))

                mappingBlockReason(table, mapOf("a" to FieldMapping("a", both))) shouldBe null
            }
        }

        context("미리보기 차단 이유") {
            test("가져올 카드가 0장이면 카드 없음 이유다") {
                previewBlockReason(preview(cardCount = 0), ImportFormat.TEXT, isPaste = false) shouldBe PreviewBlockReason.NoCards
            }

            test("카드가 있고 상한을 넘지 않으면 막지 않는다") {
                previewBlockReason(preview(cardCount = 3), ImportFormat.XLSX, isPaste = false) shouldBe null
            }

            test("텍스트 파일이 상한을 넘으면 파일을 줄 수로 나누라는 이유다") {
                previewBlockReason(preview(cardCount = 3, limitExceeded = true), ImportFormat.TEXT, isPaste = false) shouldBe
                    PreviewBlockReason.TooManyTextFile
            }

            test("붙여넣기가 상한을 넘으면 붙여넣기용 이유다") {
                previewBlockReason(preview(cardCount = 3, limitExceeded = true), ImportFormat.TEXT, isPaste = true) shouldBe
                    PreviewBlockReason.TooManyPaste
            }

            test("엑셀이 상한을 넘으면 시트나 행을 나누라는 이유다") {
                previewBlockReason(preview(cardCount = 3, limitExceeded = true), ImportFormat.XLSX, isPaste = false) shouldBe
                    PreviewBlockReason.TooManyXlsx
            }

            test("Anki가 상한을 넘으면 하위 덱을 줄이라는 이유다") {
                previewBlockReason(preview(cardCount = 3, limitExceeded = true), ImportFormat.APKG, isPaste = false) shouldBe
                    PreviewBlockReason.TooManyApkg
            }
        }
    }

    private fun noteType(
        id: String,
        noteCount: Int,
        isImageOcclusion: Boolean = false,
        selectedNoteCount: Int? = null,
    ) = ParsedNoteType(
        id = id,
        name = id,
        fieldNames = listOf("앞면", "뒷면"),
        isCloze = false,
        noteCount = noteCount,
        isImageOcclusion = isImageOcclusion,
        selectedNoteCount = selectedNoteCount,
    )

    private fun apkgTable(noteTypes: List<ParsedNoteType>) =
        ParsedTable(
            format = ImportFormat.APKG,
            sourceName = "deck.apkg",
            htmlMode = HtmlMode.HTML,
            noteTypes = noteTypes,
            rows = emptyList(),
        )

    private fun preview(
        cardCount: Int,
        limitExceeded: Boolean = false,
    ) = ImportPreview(
        cards = List(cardCount) { index -> card(index) }.toImmutableList(),
        stats = ImportPreviewStats(cardCount, 0, 0, 0, 0),
        warnings = emptyList(),
        limitExceeded = limitExceeded,
    )

    private fun card(index: Int) =
        ImportCard(
            sourceIndex = index,
            front = "앞$index",
            back = "뒤$index",
            frontReplaced = false,
            backReplaced = false,
            frontTruncated = false,
            backTruncated = false,
            mediaRemoved = 0,
        )
}

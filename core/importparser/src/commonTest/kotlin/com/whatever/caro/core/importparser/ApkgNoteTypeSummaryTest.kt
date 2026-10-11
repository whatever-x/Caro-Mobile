package com.whatever.caro.core.importparser

import com.whatever.caro.core.importparser.model.ParsedSubdeck
import com.whatever.caro.core.importparser.model.ParsedTable
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/** 네이티브 SQLite와 zstd가 필요해 Android 호스트에서는 모든 테스트가 꺼진다. */
class ApkgNoteTypeSummaryTest : FunSpec() {
    init {
        test("Image Occlusion 유형만 isImageOcclusion이 true다").config(enabled = nativeArchiveLibrariesLoadable) {
            val table = Fixtures.parse(IMAGE_OCCLUSION)

            table.noteTypes.associate { type -> type.name to type.isImageOcclusion } shouldBe
                mapOf("Basic" to false, "Image Occlusion" to true)
        }

        test("Image Occlusion 노트도 선택 범위 노트 수에 센다").config(enabled = nativeArchiveLibrariesLoadable) {
            val table = Fixtures.parse(IMAGE_OCCLUSION)

            table.selectedCounts() shouldBe mapOf("Basic" to 2, "Image Occlusion" to 2)
        }

        test("APKG 결과의 layout은 null이다").config(enabled = nativeArchiveLibrariesLoadable) {
            Fixtures.parse(IMAGE_OCCLUSION).layout shouldBe null
        }

        test("고른 하위 덱에 있는 노트만 유형별로 세고 그 덱에 없는 유형은 0이다").config(enabled = nativeArchiveLibrariesLoadable) {
            val table = parseSelecting(EDGE_CASES, listOf("Edge"))

            table.selectedCounts() shouldBe mapOf("Basic" to 4, "Cloze" to 0)
        }

        test("덱 없음만 고르면 덱 없음 노트만 센다").config(enabled = nativeArchiveLibrariesLoadable) {
            val table = parseSelecting(EDGE_CASES, listOf(null))

            table.selectedCounts() shouldBe mapOf("Basic" to 0, "Cloze" to 1)
        }

        test("하위 덱과 덱 없음을 함께 고르면 둘을 합쳐 센다").config(enabled = nativeArchiveLibrariesLoadable) {
            val table = parseSelecting(EDGE_CASES, listOf("Edge", null))

            table.selectedCounts() shouldBe mapOf("Basic" to 4, "Cloze" to 1)
        }

        test("subdeckIds가 null이면 카드가 있는 하위 덱 전체를 기준으로 세므로 덱 없음 노트는 빠진다")
            .config(enabled = nativeArchiveLibrariesLoadable) {
                val table = Fixtures.parse(EDGE_CASES)

                table.selectedCounts() shouldBe mapOf("Basic" to 4, "Cloze" to 0)
            }

        test("빈 선택으로 읽으면 모든 유형의 선택 범위 노트 수가 0이다").config(enabled = nativeArchiveLibrariesLoadable) {
            val table = Fixtures.parse(EDGE_CASES, Fixtures.options(subdeckIds = emptySet()))

            table.selectedCounts() shouldBe mapOf("Basic" to 0, "Cloze" to 0)
        }

        test("선택 범위를 좁혀도 noteCount는 컬렉션 전체 값 그대로다").config(enabled = nativeArchiveLibrariesLoadable) {
            val table = parseSelecting(EDGE_CASES, listOf("Edge"))

            table.noteTypes.associate { type -> type.name to type.noteCount } shouldBe mapOf("Basic" to 4, "Cloze" to 1)
        }

        listOf(
            listOf("Big::Part1") to 5_000,
            listOf("Big::Part1", "Big::Part2") to 10_000,
            listOf("Big::Part3") to 1,
            listOf("Big::Part1", "Big::Part2", "Big::Part3") to 10_001,
        ).forEach { (paths, expected) ->
            test("대용량 파일에서 ${paths.joinToString()}를 고르면 선택 범위 노트 수는 ${expected}이다").config(enabled = nativeArchiveLibrariesLoadable) {
                val table = parseSelecting(LARGE, paths)

                table.selectedCounts() shouldBe mapOf("Basic" to expected)
            }
        }

        test("maxRows가 작아 행이 잘려도 선택 범위 노트 수는 잘리지 않는다").config(enabled = nativeArchiveLibrariesLoadable) {
            val ids = idsOf(LARGE, listOf("Big::Part1"))

            val table = Fixtures.parse(LARGE, Fixtures.options(subdeckIds = ids, maxRows = 10))

            table.selectedCounts() shouldBe mapOf("Basic" to 5_000)
        }
    }

    private fun ParsedTable.selectedCounts(): Map<String, Int?> = noteTypes.associate { type -> type.name to type.selectedNoteCount }

    private suspend fun idsOf(
        fixture: String,
        paths: List<String?>,
    ): Set<String> {
        val subdecks = Fixtures.parse(fixture).subdecks
        return paths.map { path -> path?.let { subdecks.first { subdeck -> subdeck.path == path }.id } ?: ParsedSubdeck.NO_DECK_ID }.toSet()
    }

    private suspend fun parseSelecting(
        fixture: String,
        paths: List<String?>,
    ): ParsedTable = Fixtures.parse(fixture, Fixtures.options(subdeckIds = idsOf(fixture, paths)))

    private companion object {
        const val IMAGE_OCCLUSION = "apkg/modern_image_occlusion.apkg"
        const val EDGE_CASES = "apkg/legacy_edge_cases.apkg"
        const val LARGE = "apkg/large_10001_notes_modern.apkg"
    }
}

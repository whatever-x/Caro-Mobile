package com.whatever.caro.core.importparser

import com.whatever.caro.core.importparser.model.ParseWarningCode
import com.whatever.caro.core.importparser.model.ParsedTable
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import okio.Path

/** 네이티브 SQLite와 zstd가 필요해 Android 호스트에서는 모든 테스트가 꺼진다. */
class ApkgNoteTypeSummaryTest : FunSpec() {
    init {
        test("Image Occlusion 노트도 선택 범위 노트 수에 센다").config(enabled = nativeArchiveLibrariesLoadable) {
            val table = Fixtures.parse(IMAGE_OCCLUSION)

            table.selectedCounts() shouldBe mapOf("Basic" to 2, "Image Occlusion" to 2)
        }

        test("고른 하위 덱에 있는 노트만 유형별로 세고 그 덱에 없는 유형은 0이다").config(enabled = nativeArchiveLibrariesLoadable) {
            val table = parseSelecting(MAIN, listOf("Lang::Vocab::N5"))

            table.selectedCounts() shouldBe mapOf("Basic" to 0, "Basic (and reversed card)" to 0, "Vocab" to 504, "Cloze" to 0)
        }

        test("알 수 없는 하위 덱 id만 고르면 아무 노트도 고르지 않는다").config(enabled = nativeArchiveLibrariesLoadable) {
            val table = Fixtures.parse(EDGE_CASES, Fixtures.options(subdeckIds = setOf(UNKNOWN_SUBDECK_ID)))

            table.rows.shouldBeEmpty()
            table.selectedCounts() shouldBe mapOf("Basic" to 0)
        }

        test("하위 덱과 알 수 없는 id를 함께 고르면 하위 덱 노트만 고른다").config(enabled = nativeArchiveLibrariesLoadable) {
            val ids = idsOf(EDGE_CASES, listOf("Edge")) + UNKNOWN_SUBDECK_ID

            val table = Fixtures.parse(EDGE_CASES, Fixtures.options(subdeckIds = ids))

            table.rows.size shouldBe 4
            table.selectedCounts() shouldBe mapOf("Basic" to 4)
        }

        test("빈 선택으로 읽으면 모든 유형의 선택 범위 노트 수가 0이다").config(enabled = nativeArchiveLibrariesLoadable) {
            val table = Fixtures.parse(EDGE_CASES, Fixtures.options(subdeckIds = emptySet()))

            table.selectedCounts() shouldBe mapOf("Basic" to 0)
        }

        test("선택 범위를 좁혀도 noteCount는 덱에 속한 노트 수 그대로다").config(enabled = nativeArchiveLibrariesLoadable) {
            val table = parseSelecting(MAIN, listOf("Lang::Vocab::N5"))

            table.noteTypes.associate { type -> type.name to type.noteCount } shouldBe
                mapOf("Basic" to 303, "Basic (and reversed card)" to 102, "Vocab" to 504, "Cloze" to 308)
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

        test("카드가 두 덱에 흩어진 Image Occlusion 노트는 노트가 속한 덱을 고를 때만 카드를 모두 센다")
            .config(enabled = nativeArchiveLibrariesLoadable) {
                val path = spreadCardsPackage()

                val noteDeck = parsePackage(path, setOf(DECK_E))
                val otherDeck = parsePackage(path, setOf(DECK_D))

                noteDeck.selectedCardCounts() shouldBe mapOf("Image Occlusion" to 3, "Basic" to 1)
                noteDeck.imageOcclusionWarningCount() shouldBe 3
                otherDeck.selectedCardCounts() shouldBe mapOf("Image Occlusion" to 0, "Basic" to 1)
                otherDeck.imageOcclusionWarningCount() shouldBe null
            }

        test("maxRows에 닿아 행 읽기를 멈춰도 Image Occlusion 카드 수는 잘리지 않는다").config(enabled = nativeArchiveLibrariesLoadable) {
            val path =
                mixedPackage(
                    "io-after-row-limit.apkg",
                    notes =
                        listOf(
                            "(1, $BASIC, 'b1')",
                            "(2, $BASIC, 'b2')",
                            "(3, $IMAGE_OCCLUSION_TYPE, 'io1')",
                            "(4, $IMAGE_OCCLUSION_TYPE, 'io2')",
                        ),
                    cards =
                        listOf(
                            "(1, $DECK_D, 0, 0)",
                            "(2, $DECK_D, 0, 0)",
                            "(3, $DECK_D, 0, 0)",
                            "(3, $DECK_D, 0, 1)",
                            "(4, $DECK_D, 0, 0)",
                            "(4, $DECK_D, 0, 1)",
                        ),
                )

            val table = parsePackage(path, subdeckIds = null, maxRows = 1)

            table.rows.size shouldBe 1
            table.rowLimitReached shouldBe true
            table.imageOcclusionWarningCount() shouldBe 4
        }

        test("Image Occlusion 노트와 일반 노트가 섞인 하위 덱은 선택과 무관하게 두 수를 따로 준다").config(enabled = nativeArchiveLibrariesLoadable) {
            val table = parsePackage(spreadCardsPackage(), setOf(DECK_D))

            table.subdeckCounts().getValue("E") shouldBe (2 to 1)
        }

        test("Image Occlusion 노트가 속하지 않은 하위 덱은 그 노트의 카드가 있어도 Image Occlusion 노트 수가 0이다")
            .config(enabled = nativeArchiveLibrariesLoadable) {
                val table = parsePackage(spreadCardsPackage(), subdeckIds = null)

                table.subdeckCounts().getValue("D") shouldBe (1 to 0)
            }

        test("카드의 덱이 모두 덱 목록에 없는 노트는 하위 덱, 유형, 행 어디에도 넣지 않는다").config(enabled = nativeArchiveLibrariesLoadable) {
            val path =
                mixedPackage(
                    "unknown-deck-only.apkg",
                    notes = listOf("(1, $BASIC, 'in deck')", "(2, $BASIC, 'unknown deck')"),
                    cards = listOf("(1, $DECK_D, 0, 0)", "(2, $UNKNOWN_DECK, 0, 0)", "(2, $UNKNOWN_DECK, 0, 1)"),
                )

            val table = parsePackage(path, subdeckIds = null)

            table.subdeckCounts() shouldBe mapOf("D" to (1 to 0))
            table.noteTypes.associate { type -> type.name to type.noteCount } shouldBe mapOf("Basic" to 1)
            table.rows.size shouldBe 1
        }

        test("ord가 가장 작은 카드의 덱이 덱 목록에 없으면 다음 카드의 덱이 노트의 덱이고 카드는 모두 센다")
            .config(enabled = nativeArchiveLibrariesLoadable) {
                val path =
                    mixedPackage(
                        "first-card-unknown-deck.apkg",
                        notes = listOf("(1, $BASIC, 'note')"),
                        cards = listOf("(1, $UNKNOWN_DECK, 0, 0)", "(1, $DECK_E, 0, 1)"),
                    )

                val table = parsePackage(path, subdeckIds = null)

                table.subdeckCounts() shouldBe mapOf("E" to (1 to 0))
                table.selectedCardCounts() shouldBe mapOf("Basic" to 2)
                table.rows.single().subdeckId shouldBe DECK_E
            }

        test("노트 행이 없는 카드와 유형을 알 수 없는 노트는 하위 덱 노트 수에 넣지 않는다").config(enabled = nativeArchiveLibrariesLoadable) {
            val path =
                mixedPackage(
                    "orphan-card-and-unknown-type.apkg",
                    notes = listOf("(1, $BASIC, 'known')", "(2, $UNKNOWN_TYPE, 'unknown type')"),
                    cards = listOf("(1, $DECK_D, 0, 0)", "(2, $DECK_D, 0, 0)", "(3, $DECK_D, 0, 0)", "(4, $DECK_E, 0, 0)"),
                )

            val table = parsePackage(path, subdeckIds = null)

            table.subdeckCounts() shouldBe mapOf("D" to (1 to 0))
            table.rows.size shouldBe 1
        }

        test("유형의 노트 일부만 덱에 속하면 덱에 속한 노트만 noteCount로 센다").config(enabled = nativeArchiveLibrariesLoadable) {
            val path =
                mixedPackage(
                    "partly-without-deck.apkg",
                    notes = listOf("(1, $BASIC, 'with card')", "(2, $BASIC, 'without card')"),
                    cards = listOf("(1, $DECK_D, 0, 0)"),
                )

            val table = parsePackage(path, subdeckIds = null)

            table.noteTypes.associate { type -> type.name to type.noteCount } shouldBe mapOf("Basic" to 1)
            table.selectedCounts() shouldBe mapOf("Basic" to 1)
        }
    }

    private fun ParsedTable.selectedCounts(): Map<String, Int?> = noteTypes.associate { type -> type.name to type.selectedNoteCount }

    private fun ParsedTable.selectedCardCounts(): Map<String, Int?> = noteTypes.associate { type -> type.name to type.selectedCardCount }

    private fun ParsedTable.subdeckCounts(): Map<String, Pair<Int, Int>> =
        subdecks.associate { subdeck -> subdeck.path to (subdeck.noteCount to subdeck.imageOcclusionNoteCount) }

    private fun ParsedTable.imageOcclusionWarningCount(): Int? =
        warnings.firstOrNull { warning -> warning.code == ParseWarningCode.APKG_IMAGE_OCCLUSION_EXCLUDED }?.count

    // 노트 1(Image Occlusion)은 ord 0 카드가 덱 E에 있고 나머지 두 카드는 덱 D에 있다. 노트 2(Basic)는 덱 D, 노트 3(Basic)은 덱 E에 있다.
    private fun spreadCardsPackage(): Path =
        mixedPackage(
            "io-spread-cards.apkg",
            notes = listOf("(1, $IMAGE_OCCLUSION_TYPE, 'io')", "(2, $BASIC, 'basic d')", "(3, $BASIC, 'basic e')"),
            cards = listOf("(1, $DECK_E, 0, 0)", "(1, $DECK_D, 0, 1)", "(1, $DECK_D, 0, 2)", "(2, $DECK_D, 0, 0)", "(3, $DECK_E, 0, 0)"),
        )

    private fun mixedPackage(
        name: String,
        notes: List<String>,
        cards: List<String>,
    ): Path {
        Fixtures.fileSystem.createDirectories(Fixtures.tempRoot)
        val path = Fixtures.tempRoot / "input-$name"
        TestAnki.legacy(
            path,
            fieldCount = 1,
            notes = emptyList(),
            notesAndCardsSql =
                listOf(
                    "CREATE TABLE notes (id INTEGER, mid INTEGER, flds TEXT)",
                    "CREATE TABLE cards (nid INTEGER, did INTEGER, odid INTEGER, ord INTEGER)",
                    "INSERT INTO notes VALUES ${notes.joinToString()}",
                    "INSERT INTO cards VALUES ${cards.joinToString()}",
                ),
            extraSql = listOf("UPDATE col SET models = '$MIXED_MODELS', decks = '$MIXED_DECKS'"),
        )
        return path
    }

    private suspend fun parsePackage(
        path: Path,
        subdeckIds: Set<String>?,
        maxRows: Int = Fixtures.MAX_ROWS,
    ): ParsedTable =
        ImportParser(Fixtures.fileSystem).parseFile(path, path.name, Fixtures.options(subdeckIds = subdeckIds, maxRows = maxRows))

    private suspend fun idsOf(
        fixture: String,
        paths: List<String>,
    ): Set<String> {
        val subdecks = Fixtures.parse(fixture).subdecks
        return paths.map { path -> subdecks.first { subdeck -> subdeck.path == path }.id }.toSet()
    }

    private suspend fun parseSelecting(
        fixture: String,
        paths: List<String>,
    ): ParsedTable = Fixtures.parse(fixture, Fixtures.options(subdeckIds = idsOf(fixture, paths)))

    private companion object {
        const val IMAGE_OCCLUSION = "apkg/modern_image_occlusion.apkg"
        const val EDGE_CASES = "apkg/legacy_edge_cases.apkg"
        const val MAIN = "apkg/modern_anki21b_main.apkg"
        const val LARGE = "apkg/large_10001_notes_modern.apkg"
        const val UNKNOWN_SUBDECK_ID = "none"

        const val IMAGE_OCCLUSION_TYPE = 1
        const val BASIC = 2
        const val DECK_D = "1"
        const val DECK_E = "2"
        const val UNKNOWN_DECK = "3"
        const val UNKNOWN_TYPE = 9
        const val MIXED_MODELS =
            """{"$IMAGE_OCCLUSION_TYPE":{"name":"Image Occlusion","type":1,"originalStockKind":6,""" +
                """"flds":[{"name":"Occlusion","ord":0}]},""" +
                """"$BASIC":{"name":"Basic","type":0,"flds":[{"name":"Front","ord":0}]}}"""
        const val MIXED_DECKS = """{"$DECK_D":{"name":"D"},"$DECK_E":{"name":"E"}}"""
    }
}

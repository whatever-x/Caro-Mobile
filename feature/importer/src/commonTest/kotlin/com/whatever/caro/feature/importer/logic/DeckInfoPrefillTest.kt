package com.whatever.caro.feature.importer.logic

import com.whatever.caro.core.importparser.model.HtmlMode
import com.whatever.caro.core.importparser.model.ImportFormat
import com.whatever.caro.core.importparser.model.ParsedNoteType
import com.whatever.caro.core.importparser.model.ParsedRow
import com.whatever.caro.core.importparser.model.ParsedSheet
import com.whatever.caro.core.importparser.model.ParsedSubdeck
import com.whatever.caro.core.importparser.model.ParsedTable
import com.whatever.caro.feature.importer.mvi.ImportSelection
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain
import kotlinx.collections.immutable.toPersistentSet

class DeckInfoPrefillTest : FunSpec() {
    init {
        context("Anki 덱 이름") {
            test("고른 하위 덱의 공통 상위 덱 이름을 쓴다") {
                val table = apkgTable("TOEIC 3000.apkg", "1" to "TOEIC::Day 01", "2" to "TOEIC::Day 02")

                prefill(table, ids = setOf("1", "2")).name shouldBe "TOEIC"
            }

            test("공통 상위가 둘 이상의 단계면 경로 구분자가 없는 마지막 이름만 쓴다") {
                val table = apkgTable("lang.apkg", "1" to "Lang::JP::단어", "2" to "Lang::JP::문법")

                prefill(table, ids = setOf("1", "2")).name shouldBe "JP"
            }

            test("공통 상위가 없고 최상위 덱이 하나면 그 덱 이름을 쓴다") {
                val table = apkgTable("jlpt.apkg", "1" to "JLPT N5")

                prefill(table, ids = setOf("1")).name shouldBe "JLPT N5"
            }

            test("상위 덱에도 노트가 있어 공통 상위가 없어도 최상위 덱이 하나면 그 이름을 쓴다") {
                val table = apkgTable("jlpt.apkg", "1" to "JLPT N5", "2" to "JLPT N5::단어")

                prefill(table, ids = setOf("1", "2")).name shouldBe "JLPT N5"
            }

            test("최상위 덱이 여럿이면 확장자를 뺀 파일 이름을 쓴다") {
                val table = apkgTable("My Decks.apkg", "1" to "Japanese::N5", "2" to "Korean::TOPIK")

                prefill(table, ids = setOf("1", "2")).name shouldBe "My Decks"
            }

            test("서로 다른 최상위 덱의 단일 덱 둘도 최상위가 여럿이라 파일 이름을 쓴다") {
                val table = apkgTable("two.apkg", "1" to "Japanese", "2" to "Korean")

                prefill(table, ids = setOf("1", "2")).name shouldBe "two"
            }

            test("고르지 않은 하위 덱은 최상위 덱 개수에 넣지 않는다") {
                val table = apkgTable("My Decks.apkg", "1" to "Japanese::N5", "2" to "Japanese::N4", "3" to "Korean::TOPIK")

                prefill(table, ids = setOf("1", "2")).name shouldBe "Japanese"
            }

            test("고른 하위 덱의 최상위 덱이 하나로 줄어들면 파일 이름이 아니라 그 이름을 쓴다") {
                val table = apkgTable("My Decks.apkg", "1" to "Japanese::N5", "2" to "Korean::TOPIK")

                prefill(table, ids = setOf("2")).name shouldBe "Korean"
            }

            test("덱 없음만 고르면 고른 덱이 없으므로 파일 이름을 쓴다") {
                val table = apkgTable("loose.apkg", "1" to "JLPT N5")

                prefill(table, ids = setOf(ParsedSubdeck.NO_DECK_ID)).name shouldBe "loose"
            }

            test("덱 없음을 함께 골라도 최상위 덱 판단에는 넣지 않는다") {
                val table = apkgTable("loose.apkg", "1" to "JLPT N5")

                prefill(table, ids = setOf("1", ParsedSubdeck.NO_DECK_ID)).name shouldBe "JLPT N5"
            }

            test("파일 이름에 점이 여러 개여도 마지막 확장자만 뺀다") {
                val table = apkgTable("TOEIC.v2.apkg", "1" to "Japanese::N5", "2" to "Korean::TOPIK")

                prefill(table, ids = setOf("1", "2")).name shouldBe "TOEIC.v2"
            }

            test("Anki 설명은 확장자를 포함한 파일 이름을 넣은 템플릿이다") {
                val table = apkgTable("TOEIC 3000.apkg", "1" to "TOEIC::Day 01", "2" to "TOEIC::Day 02")

                prefill(table, ids = setOf("1", "2")).description shouldBe "TOEIC 3000.apkg에서 가져왔어요"
            }
        }

        context("엑셀 덱 이름") {
            test("시트가 여러 개면 파일 이름과 시트 이름을 ' - '로 잇는다") {
                val table = xlsxTable("단어장.xlsx", ParsedSheet(0, "Sheet1", 320), ParsedSheet(1, "동사", 120))

                prefill(table, sheetIndex = 1).name shouldBe "단어장 - 동사"
            }

            test("시트가 하나면 확장자를 뺀 파일 이름만 쓴다") {
                val table = xlsxTable("단어장.xlsx", ParsedSheet(0, "Sheet1", 320))

                prefill(table).name shouldBe "단어장"
            }

            test("엑셀 설명은 확장자를 포함한 파일 이름을 넣은 템플릿이다") {
                val table = xlsxTable("단어장.xlsx", ParsedSheet(0, "Sheet1", 320), ParsedSheet(1, "동사", 120))

                prefill(table, sheetIndex = 1).description shouldBe "단어장.xlsx에서 가져왔어요"
            }
        }

        context("텍스트 파일 덱 이름") {
            test("확장자를 뺀 파일 이름을 쓴다") {
                prefill(textTable("quizlet.txt")).name shouldBe "quizlet"
            }

            test("설명은 확장자를 포함한 파일 이름을 넣은 템플릿이다") {
                prefill(textTable("quizlet.txt")).description shouldBe "quizlet.txt에서 가져왔어요"
            }
        }

        context("붙여넣기 덱 이름") {
            test("이름은 넘겨받은 sourceName이다") {
                prefill(pasteTable(), isPaste = true).name shouldBe "붙여 넣은 카드"
            }

            test("설명은 넘겨받은 고정 문구다") {
                prefill(pasteTable(), isPaste = true).description shouldBe PASTE_DESCRIPTION
            }

            test("붙여 넣은 내용은 설명에 쓰이지 않는다") {
                prefill(pasteTable(secret = "비밀 단어"), isPaste = true).description shouldNotContain "비밀 단어"
            }

            test("붙여 넣은 내용은 이름에 쓰이지 않는다") {
                prefill(pasteTable(secret = "비밀 단어"), isPaste = true).name shouldNotContain "비밀 단어"
            }
        }

        context("길이 제한") {
            test("이름은 50자에서 자른다") {
                prefill(textTable("a".repeat(80) + ".txt")).name shouldBe "a".repeat(50)
            }

            test("정확히 50자인 이름은 그대로 둔다") {
                prefill(textTable("b".repeat(50) + ".txt")).name shouldBe "b".repeat(50)
            }

            test("붙여넣기 이름도 50자에서 자른다") {
                prefill(pasteTable(sourceName = "p".repeat(80)), isPaste = true).name shouldBe "p".repeat(50)
            }

            test("설명은 500자에서 자른다") {
                val template = "x".repeat(600) + "{file}"

                prefill(textTable("a.txt"), fileTemplate = template).description shouldBe "x".repeat(500)
            }

            test("정확히 500자인 설명은 그대로 둔다") {
                val template = "y".repeat(495) + "{file}"

                prefill(textTable("a.txt"), fileTemplate = template).description shouldBe "y".repeat(495) + "a.txt"
            }
        }
    }

    private fun prefill(
        table: ParsedTable,
        isPaste: Boolean = false,
        ids: Set<String> = emptySet(),
        sheetIndex: Int = 0,
        fileTemplate: String = FILE_TEMPLATE,
    ): DeckInfoPrefill =
        deckInfoPrefill(
            table = table,
            isPaste = isPaste,
            selection = ImportSelection(subdeckIds = ids.toPersistentSet(), sheetIndex = sheetIndex),
            fileDescriptionTemplate = fileTemplate,
            pasteDescription = PASTE_DESCRIPTION,
        )

    private fun apkgTable(
        sourceName: String,
        vararg decks: Pair<String, String>,
    ) = ParsedTable(
        format = ImportFormat.APKG,
        sourceName = sourceName,
        htmlMode = HtmlMode.HTML,
        subdecks = decks.map { (id, path) -> ParsedSubdeck(id = id, path = path, noteCount = 10) },
        noteTypes = listOf(noteType("basic")),
        rows = emptyList(),
    )

    private fun xlsxTable(
        sourceName: String,
        vararg sheets: ParsedSheet,
    ) = ParsedTable(
        format = ImportFormat.XLSX,
        sourceName = sourceName,
        htmlMode = HtmlMode.PLAIN,
        sheets = sheets.toList(),
        noteTypes = listOf(noteType("xlsx")),
        rows = emptyList(),
    )

    private fun textTable(sourceName: String) =
        ParsedTable(
            format = ImportFormat.TEXT,
            sourceName = sourceName,
            htmlMode = HtmlMode.PLAIN,
            noteTypes = listOf(noteType("text")),
            rows = emptyList(),
        )

    private fun pasteTable(
        sourceName: String = "붙여 넣은 카드",
        secret: String = "apple",
    ) = ParsedTable(
        format = ImportFormat.TEXT,
        sourceName = sourceName,
        htmlMode = HtmlMode.PLAIN,
        noteTypes = listOf(noteType("text")),
        rows = listOf(ParsedRow(noteTypeId = "text", fields = mapOf("열 1" to secret, "열 2" to "뜻"))),
    )

    private fun noteType(id: String) =
        ParsedNoteType(id = id, name = id, fieldNames = listOf("열 1", "열 2"), isCloze = false, noteCount = 10)

    private companion object {
        const val FILE_TEMPLATE = "{file}에서 가져왔어요"
        const val PASTE_DESCRIPTION = "붙여 넣은 내용으로 만들었어요"
    }
}

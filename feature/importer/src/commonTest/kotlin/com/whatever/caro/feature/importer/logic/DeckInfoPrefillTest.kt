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
import kotlinx.collections.immutable.toImmutableSet

class DeckInfoPrefillTest : FunSpec() {
    init {
        context("Anki 덱 이름") {
            test("고른 하위 덱의 공통 상위 덱 이름을 쓴다") {
                val table = apkgTable("TOEIC 3000.apkg", "1" to "TOEIC::Day 01", "2" to "TOEIC::Day 02")

                name(table, ids = setOf("1", "2")) shouldBe "TOEIC"
            }

            test("공통 상위가 둘 이상의 단계면 경로 구분자가 없는 마지막 이름만 쓴다") {
                val table = apkgTable("lang.apkg", "1" to "Lang::JP::단어", "2" to "Lang::JP::문법")

                name(table, ids = setOf("1", "2")) shouldBe "JP"
            }

            test("경로 깊이가 서로 달라도 부모 경로의 공통 앞부분에서 마지막 이름을 쓴다") {
                val table = apkgTable("lang.apkg", "1" to "Lang::JP::단어::N5", "2" to "Lang::JP::문법")

                name(table, ids = setOf("1", "2")) shouldBe "JP"
            }

            test("공통 상위가 없고 최상위 덱이 하나면 그 덱 이름을 쓴다") {
                val table = apkgTable("jlpt.apkg", "1" to "JLPT N5")

                name(table, ids = setOf("1")) shouldBe "JLPT N5"
            }

            test("상위 덱에도 노트가 있어 공통 상위가 없어도 최상위 덱이 하나면 그 이름을 쓴다") {
                val table = apkgTable("jlpt.apkg", "1" to "JLPT N5", "2" to "JLPT N5::단어")

                name(table, ids = setOf("1", "2")) shouldBe "JLPT N5"
            }

            test("경로 조각 단위로 비교하므로 글자만 겹치는 최상위 덱은 같은 덱이 아니다") {
                val table = apkgTable("words.apkg", "1" to "Japan::N5", "2" to "Japanese::N4")

                name(table, ids = setOf("1", "2")) shouldBe "words"
            }

            test("최상위 덱이 여럿이면 확장자를 뺀 파일 이름을 쓴다") {
                val table = apkgTable("My Decks.apkg", "1" to "Japanese::N5", "2" to "Korean::TOPIK")

                name(table, ids = setOf("1", "2")) shouldBe "My Decks"
            }

            test("서로 다른 최상위 덱의 단일 덱 둘도 최상위가 여럿이라 파일 이름을 쓴다") {
                val table = apkgTable("two.apkg", "1" to "Japanese", "2" to "Korean")

                name(table, ids = setOf("1", "2")) shouldBe "two"
            }

            test("고르지 않은 하위 덱은 최상위 덱 개수에 넣지 않는다") {
                val table = apkgTable("My Decks.apkg", "1" to "Japanese::N5", "2" to "Japanese::N4", "3" to "Korean::TOPIK")

                name(table, ids = setOf("1", "2")) shouldBe "Japanese"
            }

            test("고른 하위 덱이 없으면 확장자를 뺀 파일 이름을 쓴다") {
                val table = apkgTable("empty.apkg")

                name(table, ids = emptySet()) shouldBe "empty"
            }

            test("파일 이름에 점이 여러 개여도 마지막 확장자만 뺀다") {
                val table = apkgTable("TOEIC.v2.apkg", "1" to "Japanese::N5", "2" to "Korean::TOPIK")

                name(table, ids = setOf("1", "2")) shouldBe "TOEIC.v2"
            }
        }

        context("엑셀 덱 이름") {
            test("시트가 여러 개면 파일 이름과 고른 시트 이름을 ' - '로 잇는다") {
                val table = xlsxTable("단어장.xlsx", ParsedSheet(0, "Sheet1", 320), ParsedSheet(1, "동사", 120))

                name(table, sheetIndex = 1) shouldBe "단어장 - 동사"
            }

            test("시트가 하나면 확장자를 뺀 파일 이름만 쓴다") {
                val table = xlsxTable("단어장.xlsx", ParsedSheet(0, "Sheet1", 320))

                name(table) shouldBe "단어장"
            }
        }

        context("텍스트 파일 덱 이름") {
            test("확장자를 뺀 파일 이름을 쓴다") {
                name(textTable("quizlet.txt")) shouldBe "quizlet"
            }
        }

        context("붙여넣기 덱 이름") {
            test("이름은 넘겨받은 sourceName이다") {
                name(pasteTable(), isPaste = true) shouldBe "붙여 넣은 카드"
            }

            test("붙여 넣은 내용은 이름에 쓰이지 않는다") {
                name(pasteTable(secret = "비밀 단어"), isPaste = true) shouldNotContain "비밀 단어"
            }
        }

        context("길이 제한") {
            test("이름은 50자에서 자른다") {
                name(textTable("a".repeat(80) + ".txt")) shouldBe "a".repeat(50)
            }

            test("정확히 50자인 이름은 그대로 둔다") {
                name(textTable("b".repeat(50) + ".txt")) shouldBe "b".repeat(50)
            }

            test("붙여넣기 이름도 50자에서 자른다") {
                name(pasteTable(sourceName = "p".repeat(80)), isPaste = true) shouldBe "p".repeat(50)
            }

            test("50자 경계가 서로게이트 쌍 가운데면 쌍 앞에서 자른다") {
                name(textTable("a".repeat(49) + "😀.txt")) shouldBe "a".repeat(49)
            }
        }

        context("덱 설명") {
            test("파일은 틀의 파일 이름 자리에 확장자까지 넣는다") {
                deckDescriptionPrefill("JLPT N5 단어.apkg", isPaste = false, text = DESCRIPTION_TEMPLATE) shouldBe "JLPT N5 단어.apkg에서 가져왔어요"
            }

            test("붙여넣기는 넘겨받은 설명을 그대로 쓰고 붙여 넣은 내용은 쓰지 않는다") {
                deckDescriptionPrefill("붙여 넣은 카드", isPaste = true, text = PASTE_DESCRIPTION) shouldBe PASTE_DESCRIPTION
            }

            test("파일 이름은 120자로 줄여 틀 뒷부분이 잘리지 않게 한다") {
                val description = deckDescriptionPrefill("c".repeat(200) + ".txt", isPaste = false, text = DESCRIPTION_TEMPLATE)

                description.length shouldBe 120 + "에서 가져왔어요".length
                description.endsWith(".txt에서 가져왔어요") shouldBe true
            }

            test("긴 틀은 설명을 500자에서 자른다") {
                val template = FILE_NAME_PLACEHOLDER + "d".repeat(600)

                deckDescriptionPrefill("words.txt", isPaste = false, text = template).length shouldBe 500
            }
        }
    }

    private fun name(
        table: ParsedTable,
        isPaste: Boolean = false,
        ids: Set<String> = emptySet(),
        sheetIndex: Int = 0,
    ): String = deckNamePrefill(table, isPaste, ImportSelection(subdeckIds = ids.toImmutableSet(), sheetIndex = sheetIndex))

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
}

private const val DESCRIPTION_TEMPLATE = "{file}에서 가져왔어요"
private const val PASTE_DESCRIPTION = "붙여 넣은 내용으로 만들었어요"

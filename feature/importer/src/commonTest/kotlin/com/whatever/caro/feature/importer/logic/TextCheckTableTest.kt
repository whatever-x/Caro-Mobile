package com.whatever.caro.feature.importer.logic

import com.whatever.caro.core.importparser.model.HtmlMode
import com.whatever.caro.core.importparser.model.ImportFormat
import com.whatever.caro.core.importparser.model.ParsedLayout
import com.whatever.caro.core.importparser.model.ParsedNoteType
import com.whatever.caro.core.importparser.model.ParsedRow
import com.whatever.caro.core.importparser.model.ParsedTable
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldEndWith

private const val NOTE_TYPE_ID = "text"

private fun textTable(
    fieldNames: List<String>,
    rows: List<List<String>>,
    layout: ParsedLayout? = ParsedLayout(delimiter = "\t", hasHeaderRow = false),
): ParsedTable =
    ParsedTable(
        format = ImportFormat.TEXT,
        sourceName = "words.txt",
        htmlMode = HtmlMode.PLAIN,
        noteTypes =
            listOf(
                ParsedNoteType(
                    id = NOTE_TYPE_ID,
                    name = "words.txt",
                    fieldNames = fieldNames,
                    isCloze = false,
                    noteCount = rows.size,
                ),
            ),
        rows =
            rows.map { values -> ParsedRow(noteTypeId = NOTE_TYPE_ID, fields = fieldNames.zip(values).toMap()) },
        layout = layout,
    )

private fun generatedNames(count: Int): List<String> = List(count) { index -> "열 ${index + 1}" }

private fun rowsOf(
    rowCount: Int,
    columnCount: Int,
): List<List<String>> = List(rowCount) { row -> List(columnCount) { column -> "r${row + 1}c${column + 1}" } }

class TextCheckTableTest : FunSpec() {
    init {
        context("표 정보") {
            test("4열 10줄이면 열 4, 보이는 열 4, 줄 10, 보이는 줄 4를 준다") {
                val table = buildTextCheckTable(textTable(generatedNames(4), rowsOf(rowCount = 10, columnCount = 4)))

                table.info shouldBe TextCheckInfo(columnCount = 4, shownColumnCount = 4, rowCount = 10, shownRowCount = 4)
            }

            test("2열 120줄이어도 보이는 줄은 4줄이다") {
                val table = buildTextCheckTable(textTable(generatedNames(2), rowsOf(rowCount = 120, columnCount = 2)))

                table.info shouldBe TextCheckInfo(columnCount = 2, shownColumnCount = 2, rowCount = 120, shownRowCount = 4)
            }

            test("줄이 4줄보다 적으면 있는 줄만큼 보인다") {
                val table = buildTextCheckTable(textTable(generatedNames(3), rowsOf(rowCount = 3, columnCount = 3)))

                table.info shouldBe TextCheckInfo(columnCount = 3, shownColumnCount = 3, rowCount = 3, shownRowCount = 3)
            }

            test("제목 줄만 있는 입력은 열 수만 있고 줄이 0이다") {
                val table = buildTextCheckTable(textTable(listOf("단어", "뜻"), rows = emptyList()))

                table.info shouldBe TextCheckInfo(columnCount = 2, shownColumnCount = 2, rowCount = 0, shownRowCount = 0)
                table.rows shouldBe emptyList()
            }

            test("25열 표는 정보 인자 25, 20, 줄 수, 4를 준다") {
                val table = buildTextCheckTable(textTable(generatedNames(25), rowsOf(rowCount = 10, columnCount = 25)))

                table.info shouldBe TextCheckInfo(columnCount = 25, shownColumnCount = 20, rowCount = 10, shownRowCount = 4)
            }

            test("25열 표는 나머지 열을 항목 연결에서 고른다는 안내를 붙인다") {
                val table = buildTextCheckTable(textTable(generatedNames(25), rowsOf(rowCount = 10, columnCount = 25)))

                table.hasHiddenColumns shouldBe true
            }

            test("열이 정확히 20개면 안내를 붙이지 않는다") {
                val table = buildTextCheckTable(textTable(generatedNames(20), rowsOf(rowCount = 10, columnCount = 20)))

                table.hasHiddenColumns shouldBe false
            }

            test("열이 21개면 20열까지만 보이고 안내를 붙인다") {
                val table = buildTextCheckTable(textTable(generatedNames(21), rowsOf(rowCount = 10, columnCount = 21)))

                listOf(table.info.shownColumnCount, table.hasHiddenColumns) shouldBe listOf(20, true)
            }

            test("보이는 열이 20개를 넘지 않게 머리와 모든 줄을 20칸으로 자른다") {
                val table = buildTextCheckTable(textTable(generatedNames(25), rowsOf(rowCount = 10, columnCount = 25)))

                listOf(table.headers.size) + table.rows.map { row -> row.size } shouldBe listOf(20, 20, 20, 20, 20)
            }
        }

        context("본문 줄") {
            test("데이터 처음 4줄만 준다") {
                val rows = rowsOf(rowCount = 10, columnCount = 2)

                buildTextCheckTable(textTable(generatedNames(2), rows)).rows shouldBe rows.take(4)
            }

            test("값은 항목 이름 순서로 놓는다") {
                val table =
                    buildTextCheckTable(
                        textTable(
                            fieldNames = listOf("단어", "뜻", "읽기"),
                            rows = listOf(listOf("学校", "학교", "がっこう")),
                        ),
                    )

                table.rows shouldBe listOf(listOf("学校", "학교", "がっこう"))
            }

            test("행에 없는 항목은 빈 칸으로 채운다") {
                val sparse =
                    textTable(listOf("단어", "뜻", "읽기"), rows = emptyList()).copy(
                        rows = listOf(ParsedRow(noteTypeId = NOTE_TYPE_ID, fields = mapOf("단어" to "学校"))),
                    )

                buildTextCheckTable(sparse).rows shouldBe listOf(listOf("学校", "", ""))
            }

            test("200자를 넘는 셀은 200자로 줄이고 말줄임을 붙인다") {
                val table = buildTextCheckTable(textTable(listOf("단어"), rows = listOf(listOf("가".repeat(300)))))

                val cell = table.rows.single().single()
                cell.length shouldBe 200
                cell shouldEndWith "…"
            }

            test("정확히 200자인 셀은 그대로 둔다") {
                val original = "가".repeat(200)

                buildTextCheckTable(textTable(listOf("단어"), rows = listOf(listOf(original)))).rows shouldBe listOf(listOf(original))
            }
        }

        context("머리") {
            test("이름이 있고 겹치지 않는 머리는 그 이름으로 그린다") {
                val table = buildTextCheckTable(textTable(listOf("단어", "뜻"), rowsOf(rowCount = 1, columnCount = 2)))

                table.headers shouldBe listOf(TextCheckHeader.Named("단어"), TextCheckHeader.Named("뜻"))
            }

            test("이름이 비어 있는 머리는 열 N으로 그린다") {
                val table = buildTextCheckTable(textTable(listOf("", "뜻", "  "), rowsOf(rowCount = 1, columnCount = 3)))

                table.headers shouldBe
                    listOf(TextCheckHeader.Numbered(1), TextCheckHeader.Named("뜻"), TextCheckHeader.Numbered(3))
            }

            test("이름이 겹치는 머리는 모두 열 N으로 그린다") {
                val table = buildTextCheckTable(textTable(listOf("단어", "단어", "뜻"), rowsOf(rowCount = 1, columnCount = 3)))

                table.headers shouldBe
                    listOf(TextCheckHeader.Numbered(1), TextCheckHeader.Numbered(2), TextCheckHeader.Named("뜻"))
            }

            test("파서가 만든 열 N 이름은 화면 언어로 그리도록 열 N으로 돌려준다") {
                val table = buildTextCheckTable(textTable(generatedNames(3), rowsOf(rowCount = 1, columnCount = 3)))

                table.headers shouldBe
                    listOf(TextCheckHeader.Numbered(1), TextCheckHeader.Numbered(2), TextCheckHeader.Numbered(3))
            }

            test("파서가 번호를 붙여 구분한 열 N (2) 이름도 그 위치의 열 N으로 돌려준다") {
                val table = buildTextCheckTable(textTable(listOf("열 2", "열 2 (2)"), rowsOf(rowCount = 1, columnCount = 2)))

                table.headers shouldBe listOf(TextCheckHeader.Named("열 2"), TextCheckHeader.Numbered(2))
            }

            test("다른 위치의 열 N 이름은 사용자가 쓴 제목이므로 그대로 그린다") {
                val table = buildTextCheckTable(textTable(listOf("단어", "열 5"), rowsOf(rowCount = 1, columnCount = 2)))

                table.headers shouldBe listOf(TextCheckHeader.Named("단어"), TextCheckHeader.Named("열 5"))
            }

            test("200자를 넘는 머리 이름은 셀과 같이 200자로 줄인다") {
                val table = buildTextCheckTable(textTable(listOf("가".repeat(300)), rowsOf(rowCount = 1, columnCount = 1)))

                val header = table.headers.single() as TextCheckHeader.Named
                header.text.length shouldBe 200
                header.text shouldEndWith "…"
            }
        }

        context("구분자 칩") {
            test("탭이면 탭 쉼표 세미콜론 세 칩을 주고 탭만 선택이다") {
                delimiterChips('\t') shouldBe
                    listOf(
                        DelimiterChip('\t', DelimiterKind.TAB, isSelected = true),
                        DelimiterChip(',', DelimiterKind.COMMA, isSelected = false),
                        DelimiterChip(';', DelimiterKind.SEMICOLON, isSelected = false),
                    )
            }

            test("쉼표면 쉼표 칩만 선택이다") {
                delimiterChips(',').map { chip -> chip.isSelected } shouldBe listOf(false, true, false)
            }

            test("세미콜론이면 세미콜론 칩만 선택이다") {
                delimiterChips(';').map { chip -> chip.isSelected } shouldBe listOf(false, false, true)
            }

            test("세로선은 PIPE 칩을 선택된 상태로 하나 더 준다") {
                delimiterChips('|') shouldBe
                    listOf(
                        DelimiterChip('\t', DelimiterKind.TAB, isSelected = false),
                        DelimiterChip(',', DelimiterKind.COMMA, isSelected = false),
                        DelimiterChip(';', DelimiterKind.SEMICOLON, isSelected = false),
                        DelimiterChip('|', DelimiterKind.PIPE, isSelected = true),
                    )
            }

            test("공백은 SPACE 칩을 선택된 상태로 하나 더 준다") {
                delimiterChips(' ').last() shouldBe DelimiterChip(' ', DelimiterKind.SPACE, isSelected = true)
            }

            test("콜론은 COLON 칩을 선택된 상태로 하나 더 준다") {
                delimiterChips(':').last() shouldBe DelimiterChip(':', DelimiterKind.COLON, isSelected = true)
            }

            test("이름이 없는 문자는 그 문자 자체를 칩으로 하나 더 주고 선택 상태로 둔다") {
                delimiterChips('#').last() shouldBe DelimiterChip('#', kind = null, isSelected = true)
            }

            test("어떤 구분자든 선택된 칩은 정확히 하나다") {
                listOf('\t', ',', ';', '|', ' ', ':', '#').map { delimiter ->
                    delimiterChips(delimiter).count { chip -> chip.isSelected }
                } shouldBe List(7) { 1 }
            }

            test("탭 쉼표 세미콜론이 아닌 구분자는 칩을 정확히 하나만 더한다") {
                listOf('|', ' ', ':', '#').map { delimiter -> delimiterChips(delimiter).size } shouldBe List(4) { 4 }
            }
        }

        context("구분자 결정") {
            test("사용자가 고른 구분자가 파서가 쓴 구분자보다 먼저다") {
                resolveDelimiter(selected = ',', layout = ParsedLayout(delimiter = "\t", hasHeaderRow = false)) shouldBe ','
            }

            test("고른 구분자가 없으면 파서가 쓴 구분자를 쓴다") {
                resolveDelimiter(selected = null, layout = ParsedLayout(delimiter = ";", hasHeaderRow = false)) shouldBe ';'
            }

            test("고른 구분자도 읽은 구분자도 없으면 탭이다") {
                resolveDelimiter(selected = null, layout = null) shouldBe '\t'
            }

            test("읽은 구분자가 없는 배치는 탭이다") {
                resolveDelimiter(selected = null, layout = ParsedLayout(delimiter = null, hasHeaderRow = false)) shouldBe '\t'
            }
        }
    }
}

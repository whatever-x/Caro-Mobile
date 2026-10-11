package com.whatever.caro.core.importparser

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.whatever.caro.core.importparser.archive.withTempWorkspace
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import okio.FileSystem
import okio.Path

class ParserBudgetTest : FunSpec() {
    init {
        context("Anki 항목 수와 값 길이") {
            test("구버전 노트 유형의 항목 수가 열 상한을 넘으면 열이 너무 많음으로 멈춘다").config(enabled = nativeArchiveLibrariesLoadable) {
                val path = temp("many-fields-legacy.apkg")
                TestAnki.legacy(path, fieldCount = 6, notes = listOf("a\u001fb"))

                shouldThrow<ImportParseException> { parse(path, Fixtures.options(maxColumns = 5)) }.kind shouldBe
                    ImportParseErrorKind.TOO_MANY_COLUMNS
            }

            test("최신 노트 유형의 항목 수가 열 상한을 넘으면 열이 너무 많음으로 멈춘다").config(enabled = nativeArchiveLibrariesLoadable) {
                val path = temp("many-fields-modern.apkg")
                TestAnki.modern(path, fieldCount = 6, notes = listOf("a\u001fb"))

                shouldThrow<ImportParseException> { parse(path, Fixtures.options(maxColumns = 5)) }.kind shouldBe
                    ImportParseErrorKind.TOO_MANY_COLUMNS
            }

            test("노트 값 전체 길이가 값 길이 상한을 넘으면 멈춘다").config(enabled = nativeArchiveLibrariesLoadable) {
                val path = temp("long-flds.apkg")
                TestAnki.legacy(path, fieldCount = 2, notes = listOf("x".repeat(50) + "\u001fy"))

                shouldThrow<ImportParseException> { parse(path, Fixtures.options(maxFieldChars = 20)) }.kind shouldBe
                    ImportParseErrorKind.CONTENT_TOO_LONG
            }

            test("값이 NUL로 시작해 SQLite length()가 0이어도 바이트 길이가 상한의 3배를 넘으면 멈춘다")
                .config(enabled = nativeArchiveLibrariesLoadable) {
                    val path = temp("nul-flds-bytes.apkg")
                    TestAnki.legacy(path, fieldCount = 1, notes = emptyList(), notesAndCardsSql = nulNotesSql(1_000))

                    shouldThrow<ImportParseException> { parse(path, Fixtures.options(maxFieldChars = 100)) }.kind shouldBe
                        ImportParseErrorKind.CONTENT_TOO_LONG
                }

            test("값이 NUL로 시작하고 바이트 길이가 상한의 3배 안이어도 읽은 글자 수가 상한을 넘으면 멈춘다")
                .config(enabled = nativeArchiveLibrariesLoadable && keepsEmbeddedNul()) {
                    val path = temp("nul-flds-chars.apkg")
                    TestAnki.legacy(path, fieldCount = 1, notes = emptyList(), notesAndCardsSql = nulNotesSql(200))

                    shouldThrow<ImportParseException> { parse(path, Fixtures.options(maxFieldChars = 100)) }.kind shouldBe
                        ImportParseErrorKind.CONTENT_TOO_LONG
                }

            test("글자당 바이트가 큰 한글 노트도 글자 수가 상한과 같으면 읽는다").config(enabled = nativeArchiveLibrariesLoadable) {
                val path = temp("korean-flds.apkg")
                TestAnki.legacy(path, fieldCount = 1, notes = listOf("가".repeat(100)))

                parse(path, Fixtures.options(maxFieldChars = 100)).rows.size shouldBe 1
            }

            test("최신 노트 유형 config가 크기 상한을 넘으면 멈춘다").config(enabled = nativeArchiveLibrariesLoadable) {
                val path = temp("huge-config.apkg")
                TestAnki.modern(path, fieldCount = 1, notes = listOf("a"), noteTypeConfigSql = "zeroblob(${9 * 1024 * 1024})")

                shouldThrow<ImportParseException> { parse(path, Fixtures.options()) }.kind shouldBe ImportParseErrorKind.CONTENT_TOO_LONG
            }
        }

        context("전체 칸 수 예산") {
            test("텍스트의 값 있는 칸이 예산을 넘으면 읽는 중에 멈춘다") {
                val error = shouldThrow<ImportParseException> { parseText("a\tb\nc\td\ne\tf", Fixtures.options(maxCells = 5)) }

                error.kind shouldBe ImportParseErrorKind.CONTENT_TOO_LONG
            }

            test("행 수 x 항목 수로 만들 칸이 예산을 넘으면 행 값을 만들기 전에 멈춘다") {
                val error = shouldThrow<ImportParseException> { parseText("a\tb\tc\td\n1\n2\n3", Fixtures.options(maxCells = 10)) }

                error.kind shouldBe ImportParseErrorKind.CONTENT_TOO_LONG
            }

            test("엑셀의 값 있는 칸이 예산을 넘으면 멈춘다") {
                val path = temp("cells.xlsx")
                val rows = (1..3).joinToString("") { row -> "<row r=\"$row\">${inlineCell("A$row", "a")}${inlineCell("B$row", "b")}</row>" }
                TestZip.xlsx(path, TestZip.sheet(rows))

                shouldThrow<ImportParseException> { parse(path, Fixtures.options(maxCells = 5)) }.kind shouldBe
                    ImportParseErrorKind.CONTENT_TOO_LONG
            }

            test("Anki 행 값 칸이 예산을 넘으면 멈춘다").config(enabled = nativeArchiveLibrariesLoadable) {
                val path = temp("cells.apkg")
                TestAnki.legacy(path, fieldCount = 2, notes = List(3) { "a\u001fb" })

                shouldThrow<ImportParseException> { parse(path, Fixtures.options(maxCells = 5)) }.kind shouldBe
                    ImportParseErrorKind.CONTENT_TOO_LONG
            }

            test("텍스트의 값 있는 칸이 예산과 같으면 읽는다") {
                val table = parseText("a\tb\nc\td\ne\tf", Fixtures.options(maxCells = 6, hasHeaderRow = false))

                table.rows.size shouldBe 3
            }

            test("엑셀의 값 있는 칸이 예산과 같으면 읽는다") {
                val path = temp("cells-at-budget.xlsx")
                val rows = (1..3).joinToString("") { row -> "<row r=\"$row\">${inlineCell("A$row", "a")}${inlineCell("B$row", "b")}</row>" }
                TestZip.xlsx(path, TestZip.sheet(rows))

                parse(path, Fixtures.options(maxCells = 6, hasHeaderRow = false)).rows.size shouldBe 3
            }

            test("Anki 행 값 칸이 예산과 같으면 읽는다").config(enabled = nativeArchiveLibrariesLoadable) {
                val path = temp("cells-at-budget.apkg")
                TestAnki.legacy(path, fieldCount = 2, notes = List(3) { "a\u001fb" })

                parse(path, Fixtures.options(maxCells = 6)).rows.size shouldBe 3
            }

            test("엑셀은 상한 밖 열의 셀을 만나는 즉시 멈춰 뒤의 셀을 쌓지 않는다") {
                val path = temp("far-then-many.xlsx")
                val many =
                    (2..4).joinToString(
                        "",
                    ) { row -> "<row r=\"$row\">${(0 until 4).joinToString("") { inlineCell("${'A' + it}$row", "v") }}</row>" }
                TestZip.xlsx(path, TestZip.sheet("<row r=\"1\">${inlineCell("G1", "x")}</row>$many"))

                val error = shouldThrow<ImportParseException> { parse(path, Fixtures.options(maxColumns = 5, maxCells = 5)) }

                error.kind shouldBe ImportParseErrorKind.TOO_MANY_COLUMNS
            }
        }

        context("전체 글자 수 예산") {
            test("텍스트의 값 글자 수 합계가 예산을 넘으면 읽는 중에 멈추고 같으면 읽는다") {
                val text = "aaaaa\tbbbbb\ncccc\tdddd"

                parseText(text, Fixtures.options(maxTotalChars = 18)).rows.size shouldBe 2
                shouldThrow<ImportParseException> { parseText(text, Fixtures.options(maxTotalChars = 17)) }.kind shouldBe
                    ImportParseErrorKind.CONTENT_TOO_LONG
            }

            test("엑셀의 셀 글자 수 합계가 예산을 넘으면 멈춘다") {
                val path = temp("chars.xlsx")
                val rows = (1..3).joinToString("") { row -> "<row r=\"$row\">${inlineCell("A$row", "abcde")}</row>" }
                TestZip.xlsx(path, TestZip.sheet(rows))

                parse(path, Fixtures.options(maxTotalChars = 15)).rows.size shouldBe 3
                shouldThrow<ImportParseException> { parse(path, Fixtures.options(maxTotalChars = 14)) }.kind shouldBe
                    ImportParseErrorKind.CONTENT_TOO_LONG
            }

            test("엑셀 셀 하나의 글이 주석이나 CDATA로 나뉘어도 합쳐서 값 길이 상한을 넘으면 멈춘다") {
                val comment = "<c r=\"A1\" t=\"inlineStr\"><is><t>aaaa<!--x-->aaaa<!--x-->aaaa</t></is></c>"
                val cdata = "<c r=\"A1\" t=\"inlineStr\"><is><t><![CDATA[aaaa]]><![CDATA[aaaa]]><![CDATA[aaaa]]></t></is></c>"
                listOf(comment, cdata).forEachIndexed { index, cell ->
                    val path = temp("split-$index.xlsx")
                    TestZip.xlsx(path, TestZip.sheet("<row r=\"1\">$cell</row>"))

                    shouldThrow<ImportParseException> { parse(path, Fixtures.options(maxFieldChars = 10)) }.kind shouldBe
                        ImportParseErrorKind.CONTENT_TOO_LONG
                }
            }

            test("엑셀 공유 문자열 하나가 나뉜 글로 값 길이 상한을 넘으면 멈춘다") {
                val path = temp("split-shared.xlsx")
                TestZip.xlsx(
                    path,
                    TestZip.sheet("<row r=\"1\">${shared("A1", 0)}</row>"),
                    sharedStrings = "<si><t>aaaa<!--x-->aaaa<!--x-->aaaa</t></si>",
                )

                shouldThrow<ImportParseException> { parse(path, Fixtures.options(maxFieldChars = 10)) }.kind shouldBe
                    ImportParseErrorKind.CONTENT_TOO_LONG
            }

            test("엑셀 공유 문자열 글자 수 합계가 예산을 넘으면 멈춘다") {
                val path = temp("shared-total.xlsx")
                val rows = (0..2).joinToString("") { index -> "<row r=\"${index + 1}\">${shared("A${index + 1}", index)}</row>" }
                TestZip.xlsx(
                    path,
                    TestZip.sheet(rows),
                    sharedStrings = "<si><t>aaaaaa</t></si><si><t>bbbbbb</t></si><si><t>cccccc</t></si>",
                )

                shouldThrow<ImportParseException> { parse(path, Fixtures.options(maxTotalChars = 17)) }.kind shouldBe
                    ImportParseErrorKind.CONTENT_TOO_LONG
            }

            test("같은 공유 문자열을 여러 칸이 참조해도 칸마다 글자 수를 센다") {
                val path = temp("shared-repeat.xlsx")
                val rows = (1..4).joinToString("") { row -> "<row r=\"$row\">${shared("A$row", 0)}</row>" }
                TestZip.xlsx(path, TestZip.sheet(rows), sharedStrings = "<si><t>aaaaaa</t></si>")

                parse(path, Fixtures.options(maxTotalChars = 24)).rows.size shouldBe 4
                shouldThrow<ImportParseException> { parse(path, Fixtures.options(maxTotalChars = 23)) }.kind shouldBe
                    ImportParseErrorKind.CONTENT_TOO_LONG
            }

            test("NUL이 든 Anki 노트도 읽은 글자 수 합계가 예산을 넘으면 멈춘다").config(enabled = nativeArchiveLibrariesLoadable && keepsEmbeddedNul()) {
                val path = temp("nul-chars.apkg")
                TestAnki.legacy(path, fieldCount = 1, notes = emptyList(), notesAndCardsSql = nulNotesSql(9, 9, 9))

                parse(path, Fixtures.options(maxTotalChars = 30)).rows.size shouldBe 3
                shouldThrow<ImportParseException> { parse(path, Fixtures.options(maxTotalChars = 29)) }.kind shouldBe
                    ImportParseErrorKind.CONTENT_TOO_LONG
            }

            test("Anki 노트 글자 수 합계가 예산을 넘으면 멈춘다").config(enabled = nativeArchiveLibrariesLoadable) {
                val path = temp("chars.apkg")
                TestAnki.legacy(path, fieldCount = 2, notes = List(3) { "xxxxxxxxxx\u001fy" })

                parse(path, Fixtures.options(maxTotalChars = 36)).rows.size shouldBe 3
                shouldThrow<ImportParseException> { parse(path, Fixtures.options(maxTotalChars = 35)) }.kind shouldBe
                    ImportParseErrorKind.CONTENT_TOO_LONG
            }
        }

        test("텍스트 끝에 빈 값만 이어지는 구분자는 열 상한에 넣지 않는다") {
            val trailing = "\t".repeat(2_000)
            val table = parseText("a\tb$trailing\nc\td$trailing", Fixtures.options(maxColumns = 1_000))

            table.noteTypes
                .single()
                .fieldNames.size shouldBe 2
            shouldThrow<ImportParseException> {
                parseText("a" + "\t".repeat(1_500) + "far", Fixtures.options(maxColumns = 1_000))
            }.kind shouldBe ImportParseErrorKind.TOO_MANY_COLUMNS
        }

        test("엑셀 행 상한 도달은 빈 행을 걸러내기 전의 읽은 행 수로 판정한다") {
            val path = temp("empty-shared.xlsx")
            val rows = listOf(inlineCell("A1", "x"), shared("A2", 0), shared("A3", 0), inlineCell("A4", "y"), inlineCell("A5", "z"))
            TestZip.xlsx(
                path,
                TestZip.sheet(
                    rows
                        .mapIndexed {
                            index,
                            cell,
                            ->
                            "<row r=\"${index + 1}\">$cell</row>"
                        }.joinToString(""),
                ),
                sharedStrings = "<si><t></t></si>",
            )

            val table = parse(path, Fixtures.options(maxRows = 3))

            table.rows.size shouldBe 2
            table.rowLimitReached shouldBe true
        }

        test("블록 안에서 바로 돌아가도 임시 작업 디렉터리를 지운다") {
            val parent = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "caro-importparser-return-test"
            Fixtures.fileSystem.deleteRecursively(parent, mustExist = false)

            returnFromWorkspace(parent)

            Fixtures.fileSystem.list(parent).filter { it.name.startsWith("importparser-") } shouldBe emptyList()
        }
    }

    private fun returnFromWorkspace(parent: Path): Path {
        withTempWorkspace(Fixtures.fileSystem, parent) { directory -> return directory }
    }

    // iOS 드라이버는 getText가 첫 NUL에서 끊긴 문자열을 돌려주므로, 읽은 글자 수는 NUL이 값에 남는 플랫폼에서만 시험한다.
    private fun keepsEmbeddedNul(): Boolean =
        BundledSQLiteDriver().open(":memory:").use { connection ->
            connection.prepare("SELECT CAST(x'610062' AS TEXT)").use { statement ->
                statement.step()
                statement.getText(0).length == 3
            }
        }

    // 값 앞에 내장 NUL(1글자)을 붙이고 길이를 [lengths]로 정한 노트를 만든다. SQLite length()는 NUL 앞까지만 세어 0이 된다.
    private fun nulNotesSql(vararg lengths: Int): List<String> =
        listOf(
            "CREATE TABLE notes (id INTEGER, mid INTEGER, flds TEXT)",
            "CREATE TABLE cards (nid INTEGER, did INTEGER, odid INTEGER, ord INTEGER)",
        ) +
            lengths.flatMapIndexed { index, length ->
                listOf(
                    "INSERT INTO notes VALUES (${index + 1}, 1, CAST(x'00' AS TEXT) || printf('%.*c', $length, 'x'))",
                    "INSERT INTO cards VALUES (${index + 1}, 1, 0, 0)",
                )
            }

    private fun inlineCell(
        reference: String,
        text: String,
    ) = "<c r=\"$reference\" t=\"inlineStr\"><is><t>$text</t></is></c>"

    private fun shared(
        reference: String,
        index: Int,
    ) = "<c r=\"$reference\" t=\"s\"><v>$index</v></c>"

    private fun temp(name: String): Path {
        Fixtures.fileSystem.createDirectories(Fixtures.tempRoot)
        return Fixtures.tempRoot / "input-$name"
    }

    private suspend fun parse(
        path: Path,
        options: ParseOptions,
    ) = ImportParser(Fixtures.fileSystem).parseFile(path, path.name.removePrefix("input-"), options)

    private suspend fun parseText(
        text: String,
        options: ParseOptions,
    ) = ImportParser(Fixtures.fileSystem).parseText(text, "p", options)
}

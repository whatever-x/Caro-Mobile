package com.whatever.caro.core.importparser

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteDriver
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import com.whatever.caro.core.importparser.apkg.AnkiDatabase
import com.whatever.caro.core.importparser.archive.ZipArchive
import com.whatever.caro.core.importparser.archive.withTempWorkspace
import com.whatever.caro.core.importparser.model.ParsedSubdeck
import com.whatever.caro.core.importparser.model.ParsedTable
import com.whatever.caro.core.importparser.table.ColumnNames
import com.whatever.caro.core.importparser.text.EncodedTextChunks
import com.whatever.caro.core.importparser.text.decodeStrictUtf8
import com.whatever.caro.core.importparser.xlsx.XlsxNumberFormat
import com.whatever.caro.core.importparser.xlsx.XlsxSheetReader
import com.whatever.caro.core.importparser.xlsx.XmlPullReader
import com.whatever.caro.core.importparser.xlsx.forEachEvent
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldBeUnique
import io.kotest.matchers.ints.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.job
import okio.Buffer
import okio.FileSystem
import okio.ForwardingFileSystem
import okio.IOException
import okio.Path

class ParserHardeningTest : FunSpec() {
    init {
        context("행 상한 도달 표시") {
            test("보관한 행이 maxRows에 닿으면 rowLimitReached가 참이고 못 미치면 거짓이다") {
                val parser = ImportParser(Fixtures.fileSystem)

                parser.parseText("a\tb\nc\td", "p", Fixtures.options(maxRows = 3)).rowLimitReached shouldBe false
                val reached = parser.parseText("a\tb\nc\td\ne\tf\ng\th", "p", Fixtures.options(maxRows = 3))
                reached.rows.size shouldBe 3
                reached.rowLimitReached shouldBe true
            }

            test("엑셀도 데이터 행이 maxRows에 닿으면 rowLimitReached가 참이다") {
                val table = Fixtures.parse("xlsx/wordlist_multi_sheet.xlsx", Fixtures.options(maxRows = 2))

                table.rows.size shouldBe 2
                table.rowLimitReached shouldBe true
            }

            test("Anki 행은 maxRows까지만 보관하고 rowLimitReached를 켠다").config(enabled = nativeArchiveLibrariesLoadable) {
                val table = Fixtures.parse("apkg/large_10001_notes_modern.apkg", Fixtures.options(maxRows = 100))

                table.rows.size shouldBe 100
                table.rowLimitReached shouldBe true
                val part3 = table.subdecks.first { it.path == "Big::Part3" }.id
                Fixtures.parse("apkg/large_10001_notes_modern.apkg", Fixtures.options(subdeckIds = setOf(part3))).rowLimitReached shouldBe
                    false
            }

            test("엑셀 시트 행 수는 maxRows + 1에서 세기를 멈춘다") {
                val table = Fixtures.parse("xlsx/wordlist_multi_sheet.xlsx", Fixtures.options(maxRows = 2))

                table.sheets.map { it.rowCount } shouldBe listOf(3, 3, 0)
            }
        }

        context("작은 악성 파일") {
            test("지수가 지나치게 큰 숫자는 원래 값으로 두고 서식 경고 대상이다") {
                val format = XlsxNumberFormat(date1904 = false, dateStyle = SpreadsheetDateStyle.YEAR_MONTH_DAY)

                format.format("1E999999999", 0).raw shouldBe true
                format.format("1E-999999999", 4).raw shouldBe true
                format.format("1E300", 0).raw shouldBe false
            }

            test("셀 주소 열 문자는 3자, XFD까지만 받고 넘치면 순서대로 센 열을 쓴다") {
                XlsxSheetReader.columnIndex("XFD1") shouldBe 16_383
                XlsxSheetReader.columnIndex("XFE1") shouldBe null
                XlsxSheetReader.columnIndex("AAAA1") shouldBe null
                XlsxSheetReader.columnIndex("ZZZZZZZZZZZZZZZZ1") shouldBe null
            }

            test("텍스트 열 수가 상한을 넘으면 열이 너무 많음으로 멈춘다") {
                val line = List(6) { "v$it" }.joinToString("\t")

                val error =
                    shouldThrow<ImportParseException> {
                        ImportParser(Fixtures.fileSystem).parseText(line, "p", Fixtures.options(maxColumns = 5))
                    }

                error.kind shouldBe ImportParseErrorKind.TOO_MANY_COLUMNS
            }

            test("엑셀 값이 상한 밖 열에 있으면 열이 너무 많음으로 멈추고 빈 셀은 열 수에 넣지 않는다") {
                val far = temp("far.xlsx")
                TestZip.xlsx(
                    far,
                    TestZip.sheet(
                        "<row r=\"1\"><c r=\"A1\" t=\"inlineStr\"><is><t>a</t></is></c><c r=\"G1\" t=\"inlineStr\"><is><t>x</t></is></c></row>",
                    ),
                )
                val emptyFar = temp("empty-far.xlsx")
                TestZip.xlsx(
                    emptyFar,
                    TestZip.sheet(
                        "<row r=\"1\"><c r=\"A1\" t=\"inlineStr\"><is><t>a</t></is></c><c r=\"XFD1\" t=\"inlineStr\"><is><t></t></is></c></row>",
                    ),
                )

                val error = shouldThrow<ImportParseException> { parseFile(far, "far.xlsx", Fixtures.options(maxColumns = 5)) }
                val table = parseFile(emptyFar, "empty-far.xlsx", Fixtures.options(maxColumns = 5))

                error.kind shouldBe ImportParseErrorKind.TOO_MANY_COLUMNS
                table.noteTypes
                    .single()
                    .fieldNames.size shouldBe 1
            }

            test("닫히지 않은 따옴표처럼 한 값이 상한을 넘으면 값이 너무 김으로 멈춘다") {
                val error =
                    shouldThrow<ImportParseException> {
                        ImportParser(Fixtures.fileSystem).parseText("\"" + "x".repeat(50), "p", Fixtures.options(maxFieldChars = 10))
                    }

                error.kind shouldBe ImportParseErrorKind.CONTENT_TOO_LONG
            }
        }

        context("상한 전 메모리") {
            test("줄바꿈 없는 UTF-8 파일도 정해진 크기 조각으로 나눠 글자를 가르지 않고 읽는다") {
                val text = "한글abc".repeat(40)
                val chunks =
                    EncodedTextChunks(
                        Buffer().writeUtf8(text),
                        ::decodeStrictUtf8,
                        splitInsideLine = true,
                        blockBytes = 7,
                        maxChunkBytes = 16,
                    )

                val decoded = chunks.asSequence().toList()

                decoded.joinToString("") shouldBe text
                decoded.forEach { chunk -> chunk.encodeToByteArray().size shouldBeLessThanOrEqual 16 }
            }

            test("CR만 쓴 파일은 CR에서 자르고, 줄 안을 자를 수 없는 CP949는 상한을 넘으면 멈춘다") {
                val crOnly =
                    EncodedTextChunks(
                        Buffer().writeUtf8("a\rb\rc"),
                        ::decodeStrictUtf8,
                        splitInsideLine = false,
                        blockBytes = 2,
                        maxChunkBytes = 16,
                    )
                crOnly.asSequence().joinToString("") shouldBe "a\rb\rc"

                val longLine =
                    EncodedTextChunks(
                        Buffer().writeUtf8("x".repeat(40)),
                        ::decodeStrictUtf8,
                        splitInsideLine = false,
                        blockBytes = 4,
                        maxChunkBytes = 16,
                    )
                shouldThrow<ImportParseException> { longLine.asSequence().toList() }.kind shouldBe ImportParseErrorKind.CONTENT_TOO_LONG
            }

            test("XML 글자와 태그, CDATA가 상한을 넘으면 값이 너무 김으로 멈추고 닫히지 않은 주석은 끝까지 버리며 읽는다") {
                fun reader(xml: String) = XmlPullReader(Buffer().writeUtf8(xml), maxTextBytes = 16, maxTagBytes = 16)

                shouldThrow<ImportParseException> { reader("<t>" + "x".repeat(40) + "</t>").drain() }.kind shouldBe
                    ImportParseErrorKind.CONTENT_TOO_LONG
                shouldThrow<ImportParseException> { reader("<t a=\"" + "x".repeat(40) + "\"/>").drain() }.kind shouldBe
                    ImportParseErrorKind.CONTENT_TOO_LONG
                shouldThrow<ImportParseException> { reader("<![CDATA[" + "x".repeat(40) + "]]>").drain() }.kind shouldBe
                    ImportParseErrorKind.CONTENT_TOO_LONG
                shouldThrow<ImportParseException> { reader("<!--" + "x".repeat(40)).drain() }.kind shouldBe
                    ImportParseErrorKind.CORRUPT_FILE
                reader("<!-- a --->" + "<t>ok</t>").drain() shouldBe 3
            }

            test("meta 항목이 64KiB를 넘으면 손상 파일이다") {
                val path = temp("big-meta.apkg")
                TestZip.write(path, listOf("meta" to ByteArray(70 * 1024), "collection.anki2" to ByteArray(1)))

                shouldThrow<ImportParseException> { parseFile(path, "big-meta.apkg") }.kind shouldBe ImportParseErrorKind.CORRUPT_FILE
            }

            test("중앙 디렉터리를 읽기 전에 끝 레코드의 항목 수로 상한을 판정한다") {
                val path = temp("claims-many.xlsx")
                TestZip.write(path, listOf("xl/workbook.xml" to "<workbook/>".encodeToByteArray()), claimedEntryCount = 60_000)

                val error =
                    shouldThrow<ImportParseException> {
                        parseFile(path, "claims-many.xlsx", Fixtures.options(archiveLimits = ArchiveLimits(maxEntries = 1_000)))
                    }

                error.kind shouldBe ImportParseErrorKind.ARCHIVE_LIMIT_EXCEEDED
            }

            test("16비트 항목 수가 작아도 ZIP64 끝 레코드가 큰 항목 수를 적으면 중앙 디렉터리를 읽기 전에 멈춘다") {
                val path = temp("zip64-claims-many.xlsx")
                TestZip.write(path, listOf("xl/workbook.xml" to "<workbook/>".encodeToByteArray()), zip64EntryCount = 200_000)

                val error = shouldThrow<ImportParseException> { parseFile(path, "zip64-claims-many.xlsx") }

                error.kind shouldBe ImportParseErrorKind.ARCHIVE_LIMIT_EXCEEDED
            }

            test("항목 수가 상한 안인 정상 ZIP64 끝 레코드는 그대로 읽는다") {
                val path = temp("zip64-valid.xlsx")
                TestZip.xlsx(
                    path,
                    TestZip.sheet("<row r=\"1\"><c r=\"A1\" t=\"inlineStr\"><is><t>a</t></is></c></row>"),
                    zip64EntryCount = 3,
                )

                parseFile(path, "zip64-valid.xlsx").rows.size shouldBe 1
            }

            test("중앙 디렉터리 크기가 상한을 넘으면 압축 상한 초과로 멈춘다") {
                val path = temp("big-central.xlsx")
                TestZip.xlsx(path, TestZip.sheet(""))

                val error =
                    shouldThrow<ImportParseException> {
                        parseFile(path, "big-central.xlsx", Fixtures.options(archiveLimits = ArchiveLimits(maxCentralDirectoryBytes = 100)))
                    }

                error.kind shouldBe ImportParseErrorKind.ARCHIVE_LIMIT_EXCEEDED
            }

            test("끝 레코드가 중앙 디렉터리 크기를 줄여 적어도 실제 크기로 판정한다") {
                val path = temp("lying-central.xlsx")
                TestZip.xlsx(path, TestZip.sheet(""), claimedCentralDirectoryBytes = 0)

                val error =
                    shouldThrow<ImportParseException> {
                        parseFile(
                            path,
                            "lying-central.xlsx",
                            Fixtures.options(archiveLimits = ArchiveLimits(maxCentralDirectoryBytes = 100)),
                        )
                    }

                error.kind shouldBe ImportParseErrorKind.ARCHIVE_LIMIT_EXCEEDED
            }

            test("항목마다 상한 안이어도 한 압축 파일에서 풀어낸 바이트 합계가 상한을 넘으면 멈춘다") {
                val limits = ArchiveLimits(maxTotalBytes = 2_000)

                val error =
                    shouldThrow<ImportParseException> {
                        Fixtures.parse(
                            "xlsx/wordlist_multi_sheet.xlsx",
                            Fixtures.options(archiveLimits = limits),
                        )
                    }

                error.kind shouldBe ImportParseErrorKind.ARCHIVE_LIMIT_EXCEEDED
            }
        }

        context("취소") {
            test("압축 항목을 읽는 중 취소되면 다음 읽기에서 멈춘다") {
                val path = temp("cancel-entry.xlsx")
                TestZip.xlsx(path, TestZip.sheet("<row r=\"1\"/>") + "x".repeat(100_000))
                var checks = 0
                val archive =
                    ZipArchive.open(
                        Fixtures.fileSystem,
                        path,
                        Fixtures.fileSystem.metadata(path).size ?: error("size unknown"),
                        ArchiveLimits(),
                        checkCancelled = { if (++checks > 1) throw CancellationException("stop") },
                    )

                shouldThrow<CancellationException> { archive.open("xl/worksheets/sheet1.xml").readUtf8() }
            }

            test("XML 이벤트를 읽는 반복문은 이벤트마다 취소를 확인한다") {
                val path = temp("cancel-events.xlsx")
                TestZip.xlsx(path, TestZip.sheet("<row r=\"1\"/>".repeat(200)))
                var checks = 0
                val archive =
                    ZipArchive.open(
                        Fixtures.fileSystem,
                        path,
                        Fixtures.fileSystem.metadata(path).size ?: error("size unknown"),
                        ArchiveLimits(),
                        checkCancelled = { if (++checks > 50) throw CancellationException("stop") },
                    )

                shouldThrow<CancellationException> { archive.forEachEvent("xl/worksheets/sheet1.xml") { } }
            }

            test("Anki 행을 읽는 SQLite 반복문도 취소되면 다음 행에서 멈추고 임시 파일을 남기지 않는다").config(enabled = nativeArchiveLibrariesLoadable) {
                val path = temp("cancel-sqlite.apkg")
                TestAnki.legacy(path, fieldCount = 2, notes = List(5) { "a\u001fb" })
                var parsed: ParsedTable? = null

                try {
                    coroutineScope {
                        val job = coroutineContext.job
                        val driver = CancelOnPrepareDriver(BundledSQLiteDriver()) { sql -> if ("FROM cards" in sql) job.cancel() }
                        parsed =
                            ImportParser(
                                Fixtures.fileSystem,
                            ).parseFile(path, "cancel-sqlite.apkg", Fixtures.options().copy(sqliteDriver = driver))
                    }
                } catch (_: CancellationException) {
                    // 취소로 끝나는 것이 기대 동작이다.
                }

                parsed shouldBe null
                Fixtures.fileSystem
                    .list(Fixtures.tempRoot)
                    .filter { entry -> entry.name.startsWith("importparser-") }
                    .shouldBeEmpty()
            }
        }

        context("SQLite와 임시 파일") {
            test("Anki DB 연결은 셀 크기 검사를 켜고 메모리 매핑을 끈다").config(enabled = nativeArchiveLibrariesLoadable) {
                val path = temp("pragma.sqlite")
                Fixtures.fileSystem.delete(path, mustExist = false)
                BundledSQLiteDriver().open(path.toString()).use { connection -> connection.execSQL("CREATE TABLE t(x)") }

                AnkiDatabase.open(BundledSQLiteDriver(), path, rewriteModernSchema = true, verifyIntegrity = false).use { database ->
                    database.query("PRAGMA cell_size_check") { if (it.step()) it.getLong(0) else -1L } shouldBe 1L
                    database.query("PRAGMA mmap_size") { if (it.step()) it.getLong(0) else -1L } shouldBe 0L
                }
            }

            test("오래 남은 importparser 임시 디렉터리는 다음 파싱 시작 때 지운다") {
                val parent = isolatedRoot / "stale-cleanup"
                val leftOver = parent / "importparser-left-over"
                Fixtures.fileSystem.createDirectories(leftOver)
                val createdAt = Fixtures.fileSystem.metadata(leftOver).lastModifiedAtMillis ?: error("mtime unknown")

                withTempWorkspace(Fixtures.fileSystem, parent, nowMillis = createdAt + STALE_MILLIS) { }

                Fixtures.fileSystem.list(parent) shouldBe emptyList()
            }

            test("임시 디렉터리를 지우다 실패해도 원래 예외를 가리지 않는다") {
                val failingDelete =
                    object : ForwardingFileSystem(FileSystem.SYSTEM) {
                        override fun delete(
                            path: Path,
                            mustExist: Boolean,
                        ): Unit = throw IOException("delete failed")
                    }

                val error =
                    shouldThrow<IllegalStateException> {
                        withTempWorkspace(failingDelete, isolatedRoot / "mask") { error("original") }
                    }

                error.message shouldBe "original"
                Fixtures.fileSystem.deleteRecursively(isolatedRoot / "mask")
            }
        }

        test("생성한 항목 이름이 실제 제목과 겹치지 않게 한다") {
            ColumnNames.fieldNames(listOf("열 2", ""), 2).shouldBeUnique()
            ColumnNames.fieldNames(listOf("", "열 1", "열 1"), 3).shouldBeUnique()
        }

        test("Anki 카드가 없는 노트만 고른 선택에서도 행 상한 규칙이 같다").config(enabled = nativeArchiveLibrariesLoadable) {
            val table =
                Fixtures.parse(
                    "apkg/legacy_edge_cases.apkg",
                    Fixtures.options(subdeckIds = setOf(ParsedSubdeck.NO_DECK_ID), maxRows = 1),
                )

            table.rows.size shouldBe 1
            table.rowLimitReached shouldBe true
        }
    }

    private fun temp(name: String): Path {
        Fixtures.fileSystem.createDirectories(Fixtures.tempRoot)
        return Fixtures.tempRoot / "input-$name"
    }

    private suspend fun parseFile(
        path: Path,
        name: String,
        options: ParseOptions = Fixtures.options(),
    ) = ImportParser(Fixtures.fileSystem).parseFile(path, name, options)

    private fun XmlPullReader.drain(): Int = generateSequence { next() }.count()

    private companion object {
        const val STALE_MILLIS = 2L * 60 * 60 * 1000

        // 다른 테스트가 파서 임시 디렉터리를 비웠는지 확인하는 위치와 섞이지 않게 따로 둔다.
        val isolatedRoot: Path = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "caro-importparser-workspace-test"
    }
}

/** 쿼리를 준비하는 시점에 [onPrepare]를 불러 읽기 도중 취소를 재현한다. */
private class CancelOnPrepareDriver(
    private val delegate: SQLiteDriver,
    private val onPrepare: (String) -> Unit,
) : SQLiteDriver {
    override fun open(fileName: String): SQLiteConnection {
        val connection = delegate.open(fileName)
        return object : SQLiteConnection by connection {
            override fun prepare(sql: String) = connection.prepare(sql).also { onPrepare(sql) }
        }
    }
}

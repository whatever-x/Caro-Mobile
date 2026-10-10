package com.whatever.caro.core.importparser

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import okio.ByteString.Companion.decodeHex
import okio.Path

class ImportParserSafetyTest : FunSpec() {
    init {
        test("파일 크기가 상한과 같으면 읽고 1바이트라도 넘으면 내용을 읽기 전에 멈춘다") {
            val fixture = "text/tsv_no_header.tsv"
            val size = Fixtures.fileSystem.metadata(Fixtures.path(fixture)).size ?: error("size unknown")

            Fixtures.parse(fixture, Fixtures.options(maxFileBytes = size)).rows.size shouldBe 3
            val error = shouldThrow<ImportParseException> { Fixtures.parse(fixture, Fixtures.options(maxFileBytes = size - 1)) }
            error.kind shouldBe ImportParseErrorKind.FILE_TOO_LARGE
        }

        test("지원하지 않는 확장자는 내용과 관계없이 멈춘다") {
            val error = shouldThrow<ImportParseException> { parseBytes("deck.colpkg", "504b0304") }

            error.kind shouldBe ImportParseErrorKind.UNSUPPORTED_FILE
        }

        test("OLE 시그니처(.xls, 암호 걸린 xlsx)는 지원하지 않는 파일이다") {
            val error = shouldThrow<ImportParseException> { parseBytes("words.xlsx", "d0cf11e0a1b11ae1") }

            error.kind shouldBe ImportParseErrorKind.UNSUPPORTED_FILE
        }

        test("zip이 아닌 xlsx는 손상 파일이다") {
            val error = shouldThrow<ImportParseException> { parseBytes("words.xlsx", "68656c6c6f") }

            error.kind shouldBe ImportParseErrorKind.CORRUPT_FILE
        }

        test("빈 텍스트 파일은 행이 없는 결과다") {
            val table = parseBytes("empty.txt", "")

            table.rows.shouldBeEmpty()
            table.noteTypes.shouldBeEmpty()
        }

        test("zip 항목 수가 상한을 넘으면 압축 상한 초과로 멈춘다") {
            val limits = ArchiveLimits(maxEntries = 1)

            val error =
                shouldThrow<ImportParseException> {
                    Fixtures.parse("xlsx/date1904.xlsx", Fixtures.options(archiveLimits = limits))
                }

            error.kind shouldBe ImportParseErrorKind.ARCHIVE_LIMIT_EXCEEDED
        }

        test("한 항목을 푼 바이트가 상한을 넘으면 압축 상한 초과로 멈춘다").config(enabled = nativeArchiveLibrariesLoadable) {
            val limits = ArchiveLimits(maxEntryBytes = 1024)

            val error =
                shouldThrow<ImportParseException> {
                    Fixtures.parse("apkg/modern_anki21b_main.apkg", Fixtures.options(archiveLimits = limits))
                }

            error.kind shouldBe ImportParseErrorKind.ARCHIVE_LIMIT_EXCEEDED
        }

        test("Anki 패키지를 읽은 뒤 임시 디렉터리에 파서가 만든 파일을 남기지 않는다").config(enabled = nativeArchiveLibrariesLoadable) {
            Fixtures.parse("apkg/modern_anki21b_main.apkg")
            shouldThrow<ImportParseException> {
                Fixtures.parse("apkg/modern_anki21b_main.apkg", Fixtures.options(archiveLimits = ArchiveLimits(maxEntryBytes = 1024)))
            }

            Fixtures.fileSystem
                .list(Fixtures.tempRoot)
                .filterNot(::isTestInput)
                .shouldBeEmpty()
        }

        test("무결성 검사를 켜도 정상 최신·구버전 패키지를 거절하지 않는다").config(enabled = nativeArchiveLibrariesLoadable) {
            val options = Fixtures.options(verifyIntegrity = true)

            Fixtures.parse("apkg/modern_anki21b_main.apkg", options).rows.size shouldBe MAIN_NOTES
            Fixtures.parse("apkg/legacy2_anki21_main.apkg", options).rows.size shouldBe MAIN_NOTES
            Fixtures.parse("apkg/legacy_anki2_genanki_main.apkg", options).rows.size shouldBe MAIN_NOTES
        }
    }

    private suspend fun parseBytes(
        name: String,
        hex: String,
    ) = run {
        Fixtures.fileSystem.createDirectories(Fixtures.tempRoot)
        val path = Fixtures.tempRoot / "$TEST_INPUT_PREFIX$name"
        Fixtures.fileSystem.write(path) { write(hex.decodeHex()) }
        try {
            ImportParser(Fixtures.fileSystem).parseFile(path, name, Fixtures.options())
        } finally {
            Fixtures.fileSystem.delete(path)
        }
    }

    private fun isTestInput(path: Path): Boolean = path.name.startsWith(TEST_INPUT_PREFIX)

    private companion object {
        const val TEST_INPUT_PREFIX = "input-"
        const val MAIN_NOTES = 1217
    }
}

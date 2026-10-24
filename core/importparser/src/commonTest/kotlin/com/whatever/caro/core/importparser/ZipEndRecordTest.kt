package com.whatever.caro.core.importparser

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import okio.Buffer
import okio.Path

/** 중앙 디렉터리를 Okio가 메모리에 올리기 전에 거르는 판정은 Okio가 끝 레코드를 찾고 따르는 규칙과 같아야 우회되지 않는다. */
class ZipEndRecordTest : FunSpec() {
    init {
        context("Okio와 같은 끝 레코드 발견 규칙") {
            test("ZIP64 끝 레코드가 일반 끝 레코드 뒤 코멘트 영역에 있어도 그 항목 수로 판정한다") {
                val path = temp("zip64-in-comment.xlsx")
                TestZip.write(
                    path,
                    listOf("xl/workbook.xml" to "<workbook/>".encodeToByteArray()),
                    claimedEntryCount = 0,
                    zip64EntryCount = 200_000,
                    zip64Placement = TestZip.Zip64Placement.IN_COMMENT,
                )

                shouldThrow<ImportParseException> { parse(path) }.kind shouldBe ImportParseErrorKind.ARCHIVE_LIMIT_EXCEEDED
            }

            test("일반 끝 레코드가 파일 끝에서 65,536바이트 앞에 있어도 찾아 항목 수로 판정한다") {
                val path = temp("far-end-record.xlsx")
                TestZip.write(
                    path,
                    listOf("xl/workbook.xml" to "<workbook/>".encodeToByteArray()),
                    claimedEntryCount = 60_000,
                    trailingBytes = 65_536,
                )

                val error = shouldThrow<ImportParseException> { parse(path, ArchiveLimits(maxEntries = 1_000)) }

                error.kind shouldBe ImportParseErrorKind.ARCHIVE_LIMIT_EXCEEDED
            }

            test("한 바이트 더 앞은 Okio도 끝 레코드를 찾지 못하는 범위라 손상 파일이다") {
                val path = temp("too-far-end-record.xlsx")
                TestZip.write(
                    path,
                    listOf("xl/workbook.xml" to "<workbook/>".encodeToByteArray()),
                    claimedEntryCount = 60_000,
                    trailingBytes = 65_537,
                )

                val error = shouldThrow<ImportParseException> { parse(path, ArchiveLimits(maxEntries = 1_000)) }

                error.kind shouldBe ImportParseErrorKind.CORRUPT_FILE
            }

            test("ZIP64 끝 레코드가 중앙 디렉터리 앞에 있고 크기를 줄여 적어도 파일 끝까지의 거리로 판정한다") {
                val path = temp("zip64-before-central.xlsx")
                TestZip.xlsx(
                    path,
                    TestZip.sheet(""),
                    zip64EntryCount = 3,
                    claimedCentralDirectoryBytes = 0,
                    zip64Placement = TestZip.Zip64Placement.BEFORE_CENTRAL_DIRECTORY,
                    zip64ClaimedCentralDirectoryBytes = 0,
                )

                val error = shouldThrow<ImportParseException> { parse(path, ArchiveLimits(maxCentralDirectoryBytes = 100)) }

                error.kind shouldBe ImportParseErrorKind.ARCHIVE_LIMIT_EXCEEDED
            }

            test("ZIP64 끝 레코드가 코멘트 영역에 있는 정상 파일은 그대로 읽는다") {
                val path = temp("zip64-in-comment-valid.xlsx")
                TestZip.xlsx(
                    path,
                    TestZip.sheet("<row r=\"1\"><c r=\"A1\" t=\"inlineStr\"><is><t>a</t></is></c></row>"),
                    zip64EntryCount = 3,
                    zip64Placement = TestZip.Zip64Placement.IN_COMMENT,
                )

                parse(path).rows.size shouldBe 1
            }
        }

        context("읽을 수 없는 ZIP64 끝 레코드") {
            listOf(Long.MAX_VALUE, -1L, Long.MIN_VALUE).forEach { target ->
                test("locator가 $target 위치를 가리키면 Okio도 열지 못하므로 손상 파일로 멈춘다") {
                    val path = temp("zip64-locator-$target.xlsx")
                    TestZip.xlsx(path, TestZip.sheet(""), zip64EntryCount = 3)
                    overwriteLongFromEnd(path, LOCATOR_RECORD_OFFSET_FROM_END, target)

                    shouldThrow<ImportParseException> { parse(path) }.kind shouldBe ImportParseErrorKind.CORRUPT_FILE
                }
            }

            test("locator가 가리키는 곳이 ZIP64 끝 레코드가 아니면 손상 파일로 멈춘다") {
                val path = temp("zip64-locator-wrong.xlsx")
                TestZip.xlsx(path, TestZip.sheet(""), zip64EntryCount = 3)
                overwriteLongFromEnd(path, LOCATOR_RECORD_OFFSET_FROM_END, 0L)

                shouldThrow<ImportParseException> { parse(path) }.kind shouldBe ImportParseErrorKind.CORRUPT_FILE
            }

            listOf(Long.MAX_VALUE, -1L, Long.MIN_VALUE).forEach { target ->
                test("ZIP64 끝 레코드의 중앙 디렉터리 시작 위치가 $target 이면 Okio가 읽기 전에 손상 파일로 멈춘다") {
                    val path = temp("zip64-central-$target.xlsx")
                    TestZip.xlsx(path, TestZip.sheet(""), zip64EntryCount = 3)
                    overwriteLongFromEnd(path, CENTRAL_OFFSET_FROM_END, target)

                    shouldThrow<ImportParseException> { parse(path) }.kind shouldBe ImportParseErrorKind.CORRUPT_FILE
                }
            }
        }
    }

    /** 끝 레코드에 코멘트가 없는 표준 배치 파일에서 끝으로부터 [bytesFromEnd]바이트 앞에서 시작하는 8바이트 칸을 [value]로 바꾼다. */
    private fun overwriteLongFromEnd(
        path: Path,
        bytesFromEnd: Int,
        value: Long,
    ) {
        val bytes = Fixtures.fileSystem.read(path) { readByteArray() }
        Buffer().writeLongLe(value).readByteArray().copyInto(bytes, bytes.size - bytesFromEnd)
        Fixtures.fileSystem.write(path) { write(bytes) }
    }

    private fun temp(name: String): Path {
        Fixtures.fileSystem.createDirectories(Fixtures.tempRoot)
        return Fixtures.tempRoot / "input-$name"
    }

    private suspend fun parse(
        path: Path,
        limits: ArchiveLimits = ArchiveLimits(),
    ) = ImportParser(Fixtures.fileSystem).parseFile(path, path.name.removePrefix("input-"), Fixtures.options(archiveLimits = limits))

    private companion object {
        // 표준 배치의 끝은 [ZIP64 끝 레코드 56][locator 20][일반 끝 레코드 22] 순서다. locator는 8번째 바이트부터, ZIP64 끝 레코드는 48번째 바이트부터 각각 위치를 적는다.
        const val LOCATOR_RECORD_OFFSET_FROM_END = 22 + 20 - 8
        const val CENTRAL_OFFSET_FROM_END = 22 + 20 + 56 - 48
    }
}

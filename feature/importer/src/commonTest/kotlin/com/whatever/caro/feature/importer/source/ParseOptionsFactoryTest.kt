package com.whatever.caro.feature.importer.source

import com.whatever.caro.core.importparser.ArchiveLimits
import com.whatever.caro.core.importparser.SpreadsheetDateStyle
import com.whatever.caro.core.model.importing.ImportLimits
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.longs.shouldBeLessThan
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import okio.FileSystem

class ParseOptionsFactoryTest : FunSpec() {
    init {
        test("maxRows는 장수 상한보다 1 크게 넣어 상한 초과를 알아볼 수 있게 한다") {
            val options = buildParseOptions(limits = ImportLimitsConfig(maxCards = 10_000), dateStyle = DATE_STYLE)

            options.maxRows shouldBe 10_001
        }

        test("장수 상한을 5,000으로 낮추면 maxRows는 5,001이다") {
            val options = buildParseOptions(limits = ImportLimitsConfig(maxCards = 5_000), dateStyle = DATE_STYLE)

            options.maxRows shouldBe 5_001
        }

        test("maxFileBytes는 한도 설정 값을 그대로 넘긴다") {
            val options = buildParseOptions(limits = ImportLimitsConfig(maxFileBytes = 12_345L), dateStyle = DATE_STYLE)

            options.maxFileBytes shouldBe 12_345L
        }

        test("임시 디렉터리는 시스템 임시 디렉터리의 caro-import 아래다") {
            val root = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "caro-import"

            val tempDirectory = buildParseOptions(limits = ImportLimitsConfig(), dateStyle = DATE_STYLE).tempDirectory

            generateSequence(tempDirectory) { path -> path.parent }.contains(root) shouldBe true
        }

        test("임시 디렉터리를 넘기면 그 디렉터리를 그대로 쓴다") {
            val sessionDirectory = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "caro-import" / "session-1a2b"

            val options = buildParseOptions(limits = ImportLimitsConfig(), dateStyle = DATE_STYLE, tempDirectory = sessionDirectory)

            options.tempDirectory shouldBe sessionDirectory
        }

        test("CP949 디코더를 넣어 UTF-8이 아닌 한국어 파일이 인코딩 오류로 멈추지 않게 한다") {
            val options = buildParseOptions(limits = ImportLimitsConfig(), dateStyle = DATE_STYLE)

            options.legacyKoreanDecoder.shouldNotBeNull()
        }

        test("압축 파일 상한은 저사양 기기 메모리에 맞춰 서버 기본값보다 낮다") {
            val limits = buildParseOptions(limits = ImportLimitsConfig(), dateStyle = DATE_STYLE).archiveLimits

            limits.maxEntryBytes shouldBe 192L * 1024 * 1024
            limits.maxTotalBytes shouldBe 256L * 1024 * 1024
            limits.maxEntryBytes shouldBeLessThan ArchiveLimits.DEFAULT_MAX_ENTRY_BYTES
            limits.maxTotalBytes shouldBeLessThan ArchiveLimits.DEFAULT_MAX_TOTAL_BYTES
        }

        test("표 하나가 보관할 글자 수 합계 상한을 1천만 자로 둔다") {
            val options = buildParseOptions(limits = ImportLimitsConfig(), dateStyle = DATE_STYLE)

            options.maxTotalChars shouldBe 10_000_000L
        }

        test("표 하나가 보관할 값 있는 칸 수 상한을 25만 칸으로 둔다") {
            val options = buildParseOptions(limits = ImportLimitsConfig(), dateStyle = DATE_STYLE)

            options.maxCells shouldBe 250_000
        }

        test("dateStyle은 넘긴 앱 언어 값을 따른다") {
            SpreadsheetDateStyle.entries.forEach { style ->
                val options = buildParseOptions(limits = ImportLimitsConfig(), dateStyle = style)

                options.dateStyle shouldBe style
            }
        }

        test("하위 덱, 시트, 구분자, 제목 행 선택은 그대로 넘어간다") {
            val options =
                buildParseOptions(
                    limits = ImportLimitsConfig(),
                    dateStyle = DATE_STYLE,
                    subdeckIds = setOf("1", "2"),
                    sheetIndex = 3,
                    delimiter = ';',
                    hasHeaderRow = true,
                )

            options.subdeckIds shouldBe setOf("1", "2")
            options.sheetIndex shouldBe 3
            options.delimiter shouldBe ';'
            options.hasHeaderRow shouldBe true
        }

        test("빈 하위 덱 집합은 null로 바꾸지 않고 요약만 읽도록 그대로 넘긴다") {
            val options = buildParseOptions(limits = ImportLimitsConfig(), dateStyle = DATE_STYLE, subdeckIds = emptySet())

            options.subdeckIds shouldBe emptySet()
        }

        test("선택을 넘기지 않으면 하위 덱 전체, 첫 시트, 자동 판별로 읽는다") {
            val options = buildParseOptions(limits = ImportLimitsConfig(), dateStyle = DATE_STYLE)

            options.subdeckIds.shouldBeNull()
            options.sheetIndex shouldBe 0
            options.delimiter.shouldBeNull()
            options.hasHeaderRow.shouldBeNull()
        }

        test("기본 한도는 정책 상수와 같다") {
            val limits = ImportLimitsConfig()

            limits.maxCards shouldBe ImportLimits.MAX_CARDS
            limits.maxFileBytes shouldBe ImportLimits.MAX_FILE_BYTES
            limits.maxPasteChars shouldBe ImportLimits.MAX_PASTE_CHARS
        }
    }

    private companion object {
        val DATE_STYLE = SpreadsheetDateStyle.YEAR_MONTH_DAY
    }
}

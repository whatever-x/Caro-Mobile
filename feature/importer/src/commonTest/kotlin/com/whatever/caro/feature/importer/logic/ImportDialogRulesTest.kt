package com.whatever.caro.feature.importer.logic

import com.whatever.caro.core.importparser.ImportParseErrorKind
import com.whatever.caro.core.model.importing.ImportLimits
import com.whatever.caro.feature.importer.mvi.ImportDialog
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ImportDialogRulesTest : FunSpec() {
    init {
        context("선택 결과 분류") {
            test("200MB인 big.xls는 크기가 아니라 지원하지 않는 확장자라 지원하지 않는 파일이다") {
                classifyPick("big.xls", sizeBytes = 200 * MB, maxFileBytes = ImportLimits.MAX_FILE_BYTES) shouldBe
                    ImportDialog.ParseError(ImportParseErrorKind.UNSUPPORTED_FILE, extension = "xls", isPaste = false)
            }

            test("확장자가 없으면 크기와 상관없이 지원하지 않는 파일이다") {
                classifyPick("README", sizeBytes = 200 * MB, maxFileBytes = ImportLimits.MAX_FILE_BYTES) shouldBe
                    ImportDialog.ParseError(ImportParseErrorKind.UNSUPPORTED_FILE, extension = "", isPaste = false)
            }

            test("지원하지 않는 확장자는 작아도 지원하지 않는 파일이다") {
                classifyPick("slides.pdf", sizeBytes = 10, maxFileBytes = ImportLimits.MAX_FILE_BYTES) shouldBe
                    ImportDialog.ParseError(ImportParseErrorKind.UNSUPPORTED_FILE, extension = "pdf", isPaste = false)
            }

            test("104,857,600바이트인 apkg는 통과한다") {
                classifyPick("deck.apkg", sizeBytes = 104_857_600L, maxFileBytes = ImportLimits.MAX_FILE_BYTES) shouldBe null
            }

            test("104,857,601바이트인 apkg는 파일이 너무 크다") {
                classifyPick("deck.apkg", sizeBytes = 104_857_601L, maxFileBytes = ImportLimits.MAX_FILE_BYTES) shouldBe
                    ImportDialog.FileTooLarge(extension = "apkg")
            }

            test("큰 xlsx는 파일이 너무 크다") {
                classifyPick("words.xlsx", sizeBytes = 200 * MB, maxFileBytes = ImportLimits.MAX_FILE_BYTES) shouldBe
                    ImportDialog.FileTooLarge(extension = "xlsx")
            }

            test("큰 텍스트 파일은 파일이 너무 크다") {
                classifyPick("words.csv", sizeBytes = 200 * MB, maxFileBytes = ImportLimits.MAX_FILE_BYTES) shouldBe
                    ImportDialog.FileTooLarge(extension = "csv")
            }

            test("확장자 대소문자는 구분하지 않고 소문자로 돌려준다") {
                classifyPick("DECK.APKG", sizeBytes = 200 * MB, maxFileBytes = ImportLimits.MAX_FILE_BYTES) shouldBe
                    ImportDialog.FileTooLarge(extension = "apkg")
            }

            test("크기를 모르는 지원 파일은 복사하면서 세므로 통과한다") {
                classifyPick("deck.apkg", sizeBytes = null, maxFileBytes = ImportLimits.MAX_FILE_BYTES) shouldBe null
            }

            test("크기를 모르는 지원하지 않는 파일도 확장자로 먼저 막는다") {
                classifyPick("big.xls", sizeBytes = null, maxFileBytes = ImportLimits.MAX_FILE_BYTES) shouldBe
                    ImportDialog.ParseError(ImportParseErrorKind.UNSUPPORTED_FILE, extension = "xls", isPaste = false)
            }

            test("주입한 한도를 기준으로 판단한다") {
                classifyPick("a.txt", sizeBytes = 1_001L, maxFileBytes = 1_000L) shouldBe ImportDialog.FileTooLarge(extension = "txt")
            }
        }

        context("파서 오류를 대화상자로 바꾸기") {
            test("파일이 너무 크다는 파서 오류는 파일 크기 대화상자다") {
                parseErrorDialog(ImportParseErrorKind.FILE_TOO_LARGE, extension = "apkg", isPaste = false) shouldBe
                    ImportDialog.FileTooLarge(extension = "apkg")
            }

            test("그 밖의 파서 오류는 종류와 확장자와 붙여넣기 여부를 담은 파싱 오류 대화상자다") {
                parseErrorDialog(ImportParseErrorKind.CORRUPT_FILE, extension = "xlsx", isPaste = false) shouldBe
                    ImportDialog.ParseError(ImportParseErrorKind.CORRUPT_FILE, extension = "xlsx", isPaste = false)
            }

            test("붙여넣기 오류는 붙여넣기 여부를 담는다") {
                parseErrorDialog(ImportParseErrorKind.CONTENT_TOO_LONG, extension = "", isPaste = true) shouldBe
                    ImportDialog.ParseError(ImportParseErrorKind.CONTENT_TOO_LONG, extension = "", isPaste = true)
            }
        }

        context("지원하지 않는 파일 문구") {
            test("xls는 xlsx로 저장하라는 본문이다") {
                copyOf(ImportParseErrorKind.UNSUPPORTED_FILE, "xls") shouldBe
                    ParseErrorCopy(ParseErrorTitle.Unsupported, ParseErrorBody.UnsupportedXls)
            }

            test("열 수 없는 xlsx는 암호를 풀고 다시 저장하라는 본문이다") {
                copyOf(ImportParseErrorKind.UNSUPPORTED_FILE, "xlsx") shouldBe
                    ParseErrorCopy(ParseErrorTitle.Unsupported, ParseErrorBody.UnsupportedXlsx)
            }

            test("지원하지 않는 Anki 패키지는 이전 버전 호환 옵션을 안내하는 본문이다") {
                copyOf(ImportParseErrorKind.UNSUPPORTED_FILE, "apkg") shouldBe
                    ParseErrorCopy(ParseErrorTitle.Unsupported, ParseErrorBody.UnsupportedApkg)
            }

            test("텍스트 확장자와 확장자 없음과 그 밖의 확장자는 지원하는 파일을 안내하는 본문이다") {
                listOf("txt", "csv", "tsv", "", "pdf").forEach { extension ->
                    copyOf(ImportParseErrorKind.UNSUPPORTED_FILE, extension) shouldBe
                        ParseErrorCopy(ParseErrorTitle.Unsupported, ParseErrorBody.UnsupportedGeneric)
                }
            }
        }

        context("손상 파일 문구") {
            test("손상된 apkg는 Anki에서 다시 내보내라는 본문이다") {
                copyOf(ImportParseErrorKind.CORRUPT_FILE, "apkg") shouldBe
                    ParseErrorCopy(ParseErrorTitle.Corrupt, ParseErrorBody.CorruptApkg)
            }

            test("손상된 xlsx는 Excel에서 다시 저장하라는 본문이다") {
                copyOf(ImportParseErrorKind.CORRUPT_FILE, "xlsx") shouldBe
                    ParseErrorCopy(ParseErrorTitle.Corrupt, ParseErrorBody.CorruptXlsx)
            }

            test("손상된 텍스트 파일은 손상 파일이 아니라 파일을 읽지 못했어요 문구다") {
                listOf("txt", "csv", "tsv").forEach { extension ->
                    copyOf(ImportParseErrorKind.CORRUPT_FILE, extension) shouldBe
                        ParseErrorCopy(ParseErrorTitle.ReadFailedFile, ParseErrorBody.ReadFailedFile)
                }
            }
        }

        context("인코딩 판별 불가 문구") {
            test("텍스트 파일은 UTF-8로 저장하라는 문구다") {
                listOf("txt", "csv", "tsv").forEach { extension ->
                    copyOf(ImportParseErrorKind.ENCODING_UNDETERMINED, extension) shouldBe
                        ParseErrorCopy(ParseErrorTitle.UnreadableText, ParseErrorBody.EncodingUndetermined)
                }
            }
        }

        context("파일을 읽지 못했어요 문구") {
            test("압축 한도 초과와 내용 길이 초과는 확장자와 상관없이 파일을 나누라는 문구다") {
                listOf(ImportParseErrorKind.ARCHIVE_LIMIT_EXCEEDED, ImportParseErrorKind.CONTENT_TOO_LONG).forEach { kind ->
                    FILE_EXTENSIONS.forEach { extension ->
                        copyOf(kind, extension) shouldBe ParseErrorCopy(ParseErrorTitle.ReadFailedFile, ParseErrorBody.ReadFailedFile)
                    }
                }
            }

            test("열이 너무 많으면 파일 제목에 열 많음 본문을 쓴다") {
                FILE_EXTENSIONS.forEach { extension ->
                    copyOf(ImportParseErrorKind.TOO_MANY_COLUMNS, extension) shouldBe
                        ParseErrorCopy(ParseErrorTitle.ReadFailedFile, ParseErrorBody.TooManyColumns)
                }
            }
        }

        context("붙여넣기 문구") {
            test("열이 너무 많은 경우를 뺀 모든 오류는 내용을 읽지 못했어요 문구다") {
                val kinds = ImportParseErrorKind.entries - ImportParseErrorKind.FILE_TOO_LARGE - ImportParseErrorKind.TOO_MANY_COLUMNS
                kinds.forEach { kind ->
                    listOf("", "txt").forEach { extension ->
                        copyOf(kind, extension, isPaste = true) shouldBe
                            ParseErrorCopy(ParseErrorTitle.ReadFailedPaste, ParseErrorBody.ReadFailedPaste)
                    }
                }
            }

            test("열이 너무 많으면 붙여넣기 제목에 열 많음 본문을 쓴다") {
                copyOf(ImportParseErrorKind.TOO_MANY_COLUMNS, "", isPaste = true) shouldBe
                    ParseErrorCopy(ParseErrorTitle.ReadFailedPaste, ParseErrorBody.TooManyColumns)
            }
        }

        context("선택기 실패") {
            test("선택기 실패는 확장자와 상관없이 파일을 읽지 못했어요 문구다") {
                listOf("apkg", "xlsx", "txt", "").forEach { extension ->
                    val dialog = pickFailedDialog(extension)

                    parseErrorCopy(dialog) shouldBe ParseErrorCopy(ParseErrorTitle.ReadFailedFile, ParseErrorBody.ReadFailedFile)
                }
            }

            test("선택기 실패는 붙여넣기가 아니다") {
                pickFailedDialog("apkg").isPaste shouldBe false
            }
        }
    }

    private fun copyOf(
        kind: ImportParseErrorKind,
        extension: String,
        isPaste: Boolean = false,
    ): ParseErrorCopy = parseErrorCopy(ImportDialog.ParseError(kind, extension, isPaste))

    private companion object {
        const val MB = 1024L * 1024L
        val FILE_EXTENSIONS = listOf("apkg", "xlsx", "txt", "csv", "tsv")
    }
}

package com.whatever.caro.feature.importer.logic

import com.whatever.caro.core.importparser.ImportParseErrorKind
import com.whatever.caro.core.importparser.ImportParseException
import com.whatever.caro.feature.importer.mvi.ImportDialog
import com.whatever.caro.feature.importer.support.ForcedError
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ImportDialogRulesTest : FunSpec() {
    init {
        context("선택기가 크기 초과로 판정한 결과") {
            test("200MB인 big.xls는 크기가 아니라 지원하지 않는 확장자라 지원하지 않는 파일이다") {
                tooLargePickDialog("big.xls") shouldBe
                    ImportDialog.ParseError(ImportParseErrorKind.UNSUPPORTED_FILE, "xls", isPaste = false)
            }

            test("확장자가 없으면 지원하지 않는 파일이다") {
                tooLargePickDialog("README") shouldBe ImportDialog.ParseError(ImportParseErrorKind.UNSUPPORTED_FILE, "", isPaste = false)
            }

            test("지원하는 확장자면 파일이 너무 크다") {
                listOf("deck.apkg" to "apkg", "words.xlsx" to "xlsx", "words.csv" to "csv").forEach { (name, extension) ->
                    tooLargePickDialog(name) shouldBe ImportDialog.FileTooLarge(extension)
                }
            }

            test("확장자 대소문자는 구분하지 않고 소문자로 돌려준다") {
                tooLargePickDialog("DECK.APKG") shouldBe ImportDialog.FileTooLarge("apkg")
            }

            test("이름 끝의 방향 제어 문자는 지운 뒤 확장자를 판정한다") {
                tooLargePickDialog("deck.apkg‎") shouldBe ImportDialog.FileTooLarge("apkg")
            }
        }

        context("읽기 실패를 대화상자로 바꾸기") {
            test("파일이 너무 크다는 파서 오류는 파일 크기 대화상자다") {
                readFailureDialog(parseError(ImportParseErrorKind.FILE_TOO_LARGE), "apkg", isPaste = false) shouldBe
                    ImportDialog.FileTooLarge("apkg")
            }

            test("그 밖의 파서 오류는 종류와 확장자와 붙여넣기 여부를 그대로 담는다") {
                readFailureDialog(parseError(ImportParseErrorKind.CORRUPT_FILE), "xlsx", isPaste = false) shouldBe
                    ImportDialog.ParseError(ImportParseErrorKind.CORRUPT_FILE, "xlsx", isPaste = false)
                readFailureDialog(parseError(ImportParseErrorKind.CONTENT_TOO_LONG), "", isPaste = true) shouldBe
                    ImportDialog.ParseError(ImportParseErrorKind.CONTENT_TOO_LONG, "", isPaste = true)
            }

            test("파서 밖의 읽기 실패는 파일을 읽지 못했다는 문구가 나오는 종류로 보낸다") {
                val dialog = readFailureDialog(ForcedError(), "apkg", isPaste = false) as ImportDialog.ParseError

                parseErrorCopy(dialog) shouldBe ParseErrorCopy(ParseErrorTitle.ReadFailedFile, ParseErrorBody.ReadFailedFile)
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

            test("손상된 xlsx는 엑셀에서 다시 저장하라는 본문이다") {
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
                    copyOf(kind, "", isPaste = true) shouldBe
                        ParseErrorCopy(ParseErrorTitle.ReadFailedPaste, ParseErrorBody.ReadFailedPaste)
                }
            }

            test("열이 너무 많으면 붙여넣기 제목에 열 많음 본문을 쓴다") {
                copyOf(ImportParseErrorKind.TOO_MANY_COLUMNS, "", isPaste = true) shouldBe
                    ParseErrorCopy(ParseErrorTitle.ReadFailedPaste, ParseErrorBody.TooManyColumns)
            }
        }

        context("선택기 실패") {
            test("선택기 실패는 붙여넣기가 아니고 파일을 읽지 못했어요 문구다") {
                val dialog = pickFailedDialog()

                dialog.isPaste shouldBe false
                parseErrorCopy(dialog) shouldBe ParseErrorCopy(ParseErrorTitle.ReadFailedFile, ParseErrorBody.ReadFailedFile)
            }
        }
    }

    private fun parseError(kind: ImportParseErrorKind) = ImportParseException(kind, "forced")

    private fun copyOf(
        kind: ImportParseErrorKind,
        extension: String,
        isPaste: Boolean = false,
    ): ParseErrorCopy = parseErrorCopy(ImportDialog.ParseError(kind, extension, isPaste))

    private companion object {
        val FILE_EXTENSIONS = listOf("apkg", "xlsx", "txt", "csv", "tsv")
    }
}

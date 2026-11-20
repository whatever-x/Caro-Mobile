package com.whatever.caro.feature.importer.logic

import com.whatever.caro.core.importparser.model.ParseWarning
import com.whatever.caro.core.importparser.model.ParseWarningCode
import com.whatever.caro.feature.importer.mvi.ImportPreviewFilter
import com.whatever.caro.feature.importer.support.PreviewSamples
import com.whatever.caro.feature.importer.support.PreviewSamples.card
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class PreviewPresentationTest : FunSpec() {
    init {
        context("카드 표시") {
            test("빈 면을 채운 카드는 값이 빈 면 문구라 따로 표시하지 않는다") {
                cardFlags(card(0, "앞", PreviewSamples.EMPTY_SIDE, frontReplaced = true, backReplaced = true)) shouldBe emptyList()
            }

            test("앞면만 넘으면 앞면 글자 초과다") {
                cardFlags(card(0, "앞", "뒤", frontTruncated = true)) shouldBe listOf(CardFlag.TruncatedFront)
            }

            test("앞뒤 모두 넘으면 앞면과 뒷면 글자 초과 두 개다") {
                cardFlags(card(0, "앞", "뒤", frontTruncated = true, backTruncated = true)) shouldBe
                    listOf(CardFlag.TruncatedFront, CardFlag.TruncatedBack)
            }

            test("지운 미디어가 여럿이어도 미디어 누락은 하나이고 글자 초과 뒤에 온다") {
                cardFlags(card(0, "앞", "뒤", backTruncated = true, mediaRemoved = 3)) shouldBe
                    listOf(CardFlag.TruncatedBack, CardFlag.MediaRemoved)
            }
        }

        context("필터 칩") {
            test("카드가 0장인 칩은 숨기고 선언 순서를 지킨다") {
                val counts =
                    mapOf(
                        ImportPreviewFilter.MediaRemoved to 2,
                        ImportPreviewFilter.EmptyReplaced to 0,
                        ImportPreviewFilter.Truncated to 1,
                        ImportPreviewFilter.All to 4,
                    )

                visibleFilters(counts) shouldBe
                    listOf(ImportPreviewFilter.All, ImportPreviewFilter.Truncated, ImportPreviewFilter.MediaRemoved)
            }
        }

        context("경고 줄") {
            test("예전 한글 형식, 값 없는 수식, 숫자 서식, 항목 수 불일치 순서로 쌓는다") {
                val warnings =
                    listOf(
                        ParseWarning(ParseWarningCode.APKG_FIELD_COUNT_MISMATCH, count = 3),
                        ParseWarning(ParseWarningCode.XLSX_NUMBER_FORMAT_RAW),
                        ParseWarning(ParseWarningCode.XLSX_FORMULA_WITHOUT_VALUE, count = 2),
                        ParseWarning(ParseWarningCode.ENCODING_FALLBACK_CP949),
                    )

                previewWarnings(warnings) shouldBe
                    listOf(
                        PreviewWarning(PreviewWarningKind.EncodingFallback, count = 0),
                        PreviewWarning(PreviewWarningKind.FormulaWithoutValue, count = 2),
                        PreviewWarning(PreviewWarningKind.NumberFormatRaw, count = 0),
                        PreviewWarning(PreviewWarningKind.FieldCountMismatch, count = 3),
                    )
            }

            test("칸 수가 다른 줄, Image Occlusion 제외, 알 수 없는 코드는 경고 줄로 보이지 않는다") {
                val warnings =
                    listOf(
                        ParseWarning(ParseWarningCode.TEXT_INCONSISTENT_COLUMNS, count = 4),
                        ParseWarning(ParseWarningCode.APKG_IMAGE_OCCLUSION_EXCLUDED, count = 12),
                        ParseWarning("ROWS_OMITTED", count = 1),
                    )

                previewWarnings(warnings) shouldBe emptyList()
            }
        }

        context("카드 유형 이름") {
            test("카드 유형이 2개 이상이면 카드의 원본 행으로 유형 이름을 찾는다") {
                val names = cardTypeNames(PreviewSamples.ankiTable)

                cardTypeName(PreviewSamples.ankiTable, names, PreviewSamples.ankiCards[0]) shouldBe PreviewSamples.ANKI_TYPE
                cardTypeName(PreviewSamples.ankiTable, names, PreviewSamples.ankiCards[2]) shouldBe PreviewSamples.BASIC_TYPE
            }

            test("Image Occlusion을 빼고 유형이 하나면 이름을 붙이지 않는다") {
                val table =
                    PreviewSamples.ankiTable.let {
                        it.copy(
                            noteTypes =
                                it.noteTypes.filterNot { type ->
                                    type.name ==
                                        PreviewSamples.BASIC_TYPE
                                },
                        )
                    }

                cardTypeName(table, cardTypeNames(table), PreviewSamples.ankiCards[0]) shouldBe null
            }
        }
    }
}

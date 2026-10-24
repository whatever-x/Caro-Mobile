package com.whatever.caro.feature.importer.logic

import com.whatever.caro.core.importparser.model.ParseWarning
import com.whatever.caro.core.importparser.model.ParseWarningCode
import com.whatever.caro.feature.importer.mvi.ImportPreviewFilter
import com.whatever.caro.feature.importer.transform.ImportCard
import com.whatever.caro.feature.importer.transform.ImportPreview
import com.whatever.caro.feature.importer.transform.ImportPreviewStats
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.collections.immutable.toImmutableList

class PreviewPresentationTest : FunSpec() {
    init {
        context("카드 아래에 보이는 바뀐 내용 배지") {
            test("바뀐 것이 없는 카드는 배지가 없다") {
                changeBadges(card()) shouldBe emptyList()
            }

            test("앞면을 채웠으면 빈 면 채움 앞면 배지다") {
                changeBadges(card(frontReplaced = true)) shouldBe listOf(ChangeBadge.EmptyFront)
            }

            test("뒷면을 채웠으면 빈 면 채움 뒷면 배지다") {
                changeBadges(card(backReplaced = true)) shouldBe listOf(ChangeBadge.EmptyBack)
            }

            test("앞면만 잘렸으면 잘린 내용 앞면 배지다") {
                changeBadges(card(frontTruncated = true)) shouldBe listOf(ChangeBadge.TruncatedFront)
            }

            test("뒷면만 잘렸으면 잘린 내용 뒷면 배지다") {
                changeBadges(card(backTruncated = true)) shouldBe listOf(ChangeBadge.TruncatedBack)
            }

            test("두 면이 모두 잘렸으면 앞뒷면 배지 하나로 합친다") {
                changeBadges(card(frontTruncated = true, backTruncated = true)) shouldBe listOf(ChangeBadge.TruncatedBoth)
            }

            test("미디어를 지웠으면 미디어 제거 배지다") {
                changeBadges(card(mediaRemoved = 1)) shouldBe listOf(ChangeBadge.MediaRemoved)
            }

            test("지운 미디어가 여럿이어도 미디어 제거 배지는 하나다") {
                changeBadges(card(mediaRemoved = 5)) shouldBe listOf(ChangeBadge.MediaRemoved)
            }

            test("앞면을 채우고 뒷면이 잘리고 미디어를 지웠으면 세 배지를 이 순서로 준다") {
                changeBadges(card(frontReplaced = true, backTruncated = true, mediaRemoved = 2)) shouldBe
                    listOf(ChangeBadge.EmptyFront, ChangeBadge.TruncatedBack, ChangeBadge.MediaRemoved)
            }

            test("앞면을 채우고 두 면이 잘리고 미디어를 지운 카드는 앞뒷면 배지를 포함한 세 배지다") {
                changeBadges(card(frontReplaced = true, frontTruncated = true, backTruncated = true, mediaRemoved = 1)) shouldBe
                    listOf(ChangeBadge.EmptyFront, ChangeBadge.TruncatedBoth, ChangeBadge.MediaRemoved)
            }
        }

        context("보이는 필터 칩") {
            test("가져올 카드가 없어도 전체 칩은 보인다") {
                visibleFilters(preview()) shouldBe listOf(ImportPreviewFilter.All)
            }

            test("바뀐 카드가 없으면 전체 칩만 보인다") {
                visibleFilters(preview(card(), card(index = 1))) shouldBe listOf(ImportPreviewFilter.All)
            }

            test("앞면을 채운 카드가 있으면 빈 면 채움 칩이 보인다") {
                visibleFilters(preview(card(frontReplaced = true))) shouldBe
                    listOf(ImportPreviewFilter.All, ImportPreviewFilter.EmptyReplaced)
            }

            test("뒷면을 채운 카드가 있어도 빈 면 채움 칩이 보인다") {
                visibleFilters(preview(card(backReplaced = true))) shouldBe
                    listOf(ImportPreviewFilter.All, ImportPreviewFilter.EmptyReplaced)
            }

            test("한 면이라도 잘린 카드가 있으면 잘린 내용 칩이 보인다") {
                visibleFilters(preview(card(backTruncated = true))) shouldBe
                    listOf(ImportPreviewFilter.All, ImportPreviewFilter.Truncated)
            }

            test("미디어를 지운 카드가 있으면 미디어 제거 칩이 보인다") {
                visibleFilters(preview(card(mediaRemoved = 3))) shouldBe
                    listOf(ImportPreviewFilter.All, ImportPreviewFilter.MediaRemoved)
            }

            test("미디어가 없는 텍스트 원본은 미디어 제거 칩을 숨긴다") {
                val textSource = preview(card(frontReplaced = true), card(index = 1, backTruncated = true), card(index = 2))

                visibleFilters(textSource) shouldBe
                    listOf(ImportPreviewFilter.All, ImportPreviewFilter.EmptyReplaced, ImportPreviewFilter.Truncated)
            }

            test("네 종류가 모두 있으면 전체 빈 면 채움 잘린 내용 미디어 제거 순서다") {
                val all =
                    preview(
                        card(backReplaced = true),
                        card(index = 1, backTruncated = true),
                        card(index = 2, mediaRemoved = 1),
                    )

                visibleFilters(all) shouldBe
                    listOf(
                        ImportPreviewFilter.All,
                        ImportPreviewFilter.EmptyReplaced,
                        ImportPreviewFilter.Truncated,
                        ImportPreviewFilter.MediaRemoved,
                    )
            }
        }

        context("필터를 적용한 카드") {
            val plain = card(index = 0)
            val emptyFront = card(index = 1, frontReplaced = true)
            val truncatedBack = card(index = 2, backTruncated = true)
            val mediaRemoved = card(index = 3, mediaRemoved = 2)
            val mixed = card(index = 4, backReplaced = true, frontTruncated = true, mediaRemoved = 1)
            val cards = listOf(plain, emptyFront, truncatedBack, mediaRemoved, mixed)

            test("전체는 모든 카드를 원본 순서로 돌려준다") {
                filterCards(cards, ImportPreviewFilter.All) shouldBe cards
            }

            test("빈 면 채움은 앞면이나 뒷면을 채운 카드만 돌려준다") {
                filterCards(cards, ImportPreviewFilter.EmptyReplaced) shouldBe listOf(emptyFront, mixed)
            }

            test("잘린 내용은 앞면이나 뒷면이 잘린 카드만 돌려준다") {
                filterCards(cards, ImportPreviewFilter.Truncated) shouldBe listOf(truncatedBack, mixed)
            }

            test("미디어 제거는 미디어를 지운 카드만 돌려준다") {
                filterCards(cards, ImportPreviewFilter.MediaRemoved) shouldBe listOf(mediaRemoved, mixed)
            }

            test("해당하는 카드가 없으면 빈 목록이다") {
                filterCards(listOf(plain), ImportPreviewFilter.Truncated) shouldBe emptyList()
            }

            test("카드가 없으면 어떤 필터든 빈 목록이다") {
                filterCards(emptyList(), ImportPreviewFilter.All) shouldBe emptyList()
            }
        }

        context("미리보기 경고") {
            test("경고가 없으면 아무것도 보이지 않는다") {
                previewWarnings(emptyList()) shouldBe emptyList()
            }

            test("경고 코드 표 순서대로 돌려준다") {
                val shuffled =
                    listOf(
                        ParseWarning(ParseWarningCode.APKG_IMAGE_OCCLUSION_EXCLUDED, count = 1),
                        ParseWarning(ParseWarningCode.XLSX_NUMBER_FORMAT_RAW),
                        ParseWarning(ParseWarningCode.APKG_FIELD_COUNT_MISMATCH, count = 2),
                        ParseWarning(ParseWarningCode.TEXT_INCONSISTENT_COLUMNS, count = 3),
                        ParseWarning(ParseWarningCode.ENCODING_FALLBACK_CP949),
                        ParseWarning(ParseWarningCode.XLSX_FORMULA_WITHOUT_VALUE, count = 4),
                    )

                previewWarnings(shuffled).map { warning -> warning.kind } shouldBe
                    listOf(
                        PreviewWarningKind.EncodingFallback,
                        PreviewWarningKind.InconsistentColumns,
                        PreviewWarningKind.FormulaWithoutValue,
                        PreviewWarningKind.NumberFormatRaw,
                        PreviewWarningKind.FieldCountMismatch,
                        PreviewWarningKind.ImageOcclusionExcluded,
                    )
            }

            test("경고의 개수를 문구 자리에 쓰도록 그대로 넘긴다") {
                previewWarnings(listOf(ParseWarning(ParseWarningCode.TEXT_INCONSISTENT_COLUMNS, count = 12))) shouldBe
                    listOf(PreviewWarning(PreviewWarningKind.InconsistentColumns, count = 12))
            }

            test("개수가 없는 경고는 개수 없이 돌려준다") {
                previewWarnings(listOf(ParseWarning(ParseWarningCode.ENCODING_FALLBACK_CP949))) shouldBe
                    listOf(PreviewWarning(PreviewWarningKind.EncodingFallback, count = null))
            }

            test("서버 전용 ROWS_OMITTED는 보이지 않는다") {
                previewWarnings(listOf(ParseWarning("ROWS_OMITTED", count = 500))) shouldBe emptyList()
            }

            test("문구가 없는 알 수 없는 코드는 보이지 않고 나머지는 그대로 보인다") {
                val warnings =
                    listOf(
                        ParseWarning("SOMETHING_NEW", count = 1),
                        ParseWarning(ParseWarningCode.XLSX_NUMBER_FORMAT_RAW),
                    )

                previewWarnings(warnings) shouldBe listOf(PreviewWarning(PreviewWarningKind.NumberFormatRaw, count = null))
            }
        }
    }

    private fun card(
        index: Int = 0,
        frontReplaced: Boolean = false,
        backReplaced: Boolean = false,
        frontTruncated: Boolean = false,
        backTruncated: Boolean = false,
        mediaRemoved: Int = 0,
    ) = ImportCard(
        sourceIndex = index,
        front = "앞$index",
        back = "뒤$index",
        frontReplaced = frontReplaced,
        backReplaced = backReplaced,
        frontTruncated = frontTruncated,
        backTruncated = backTruncated,
        mediaRemoved = mediaRemoved,
    )

    /** 통계는 카드에서 세어 카드와 어긋나지 않게 한다. 보이는 칩을 통계로 정하든 카드로 정하든 같은 결과여야 한다. */
    private fun preview(vararg cards: ImportCard) =
        ImportPreview(
            cards = cards.toList().toImmutableList(),
            stats =
                ImportPreviewStats(
                    cardCount = cards.size,
                    excludedCount = 0,
                    emptyReplacedCount = cards.count { card -> card.frontReplaced || card.backReplaced },
                    truncatedCount = cards.count { card -> card.frontTruncated || card.backTruncated },
                    mediaRemovedCount = cards.sumOf { card -> card.mediaRemoved },
                ),
            warnings = emptyList(),
            limitExceeded = false,
        )
}

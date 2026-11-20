package com.whatever.caro.feature.importer.logic

import com.whatever.caro.feature.importer.mvi.ImportPreviewFilter
import com.whatever.caro.feature.importer.transform.ImportCard
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.collections.immutable.persistentListOf

class PreviewFiltersTest : FunSpec() {
    init {
        context("칩마다의 카드 수") {
            test("전체는 모든 카드 수이고 나머지는 해당하는 카드 수다") {
                previewFilterCounts(CARDS) shouldBe
                    mapOf(
                        ImportPreviewFilter.All to 5,
                        ImportPreviewFilter.EmptyReplaced to 2,
                        ImportPreviewFilter.Truncated to 2,
                        ImportPreviewFilter.MediaRemoved to 1,
                    )
            }

            test("지운 미디어가 여럿인 카드도 한 장으로 센다") {
                previewFilterCounts(listOf(card(0, mediaRemoved = 3)))[ImportPreviewFilter.MediaRemoved] shouldBe 1
            }
        }

        context("고른 칩으로 거른 목록") {
            test("빈 면은 앞면이나 뒷면을 채운 카드만 돌려준다") {
                filterCards(CARDS, ImportPreviewFilter.EmptyReplaced).map { it.sourceIndex } shouldBe listOf(1, 2)
            }

            test("미디어 누락은 미디어를 지운 카드만 돌려준다") {
                filterCards(CARDS, ImportPreviewFilter.MediaRemoved).map { it.sourceIndex } shouldBe listOf(4)
            }
        }
    }

    private companion object {
        val CARDS =
            persistentListOf(
                card(0),
                card(1, frontReplaced = true),
                card(2, backReplaced = true, frontTruncated = true),
                card(3, backTruncated = true),
                card(4, mediaRemoved = 2),
            )

        fun card(
            index: Int,
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
    }
}

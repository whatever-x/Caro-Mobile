package com.whatever.caro.feature.importer.save

import com.whatever.caro.core.model.card.CardContent
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeUnique
import io.kotest.matchers.ints.shouldBeLessThanOrEqual
import io.kotest.matchers.longs.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe

class ImportChunkerTest : FunSpec() {
    init {
        test("장수 상한으로 원본 순서를 지켜 나누고 묶음마다 다른 키를 붙인다") {
            val cards = List(2_500) { index -> CardContent("앞$index", "뒤$index") }

            val chunks = ImportChunker.split(cards, maxItems = 1_000, maxBytes = Long.MAX_VALUE, newKey = keys())

            chunks.map { it.cards.size } shouldBe listOf(1_000, 1_000, 500)
            chunks.flatMap { it.cards } shouldBe cards
            chunks.map { it.index } shouldBe listOf(0, 1, 2)
            chunks.map { it.idempotencyKey }.shouldBeUnique()
        }

        test("직렬화 본문 추정 바이트가 상한을 넘지 않게 장수보다 먼저 나눈다") {
            val worst = CardContent("가".repeat(500), "나".repeat(500))
            val cards = List(1_000) { worst }

            val chunks = ImportChunker.split(cards, maxItems = 1_000, maxBytes = MAX_BYTES, newKey = keys())

            chunks.size shouldBe 2
            chunks.forEach { chunk -> ImportChunker.estimatedRequestBytes(chunk.cards) shouldBeLessThanOrEqual MAX_BYTES }
            chunks.sumOf { it.cards.size } shouldBe 1_000
        }

        test("따옴표, 역슬래시, 제어 문자, 서로게이트 쌍은 실제 JSON 바이트보다 작게 추정하지 않는다") {
            val card = CardContent("\"\\\n\u0001", "😀한a")
            val json = "{\"items\":[{\"fields\":{\"front\":\"\\\"\\\\\\n\\u0001\",\"back\":\"😀한a\"},\"cardType\":\"BASIC\"}]}"

            json.encodeToByteArray().size.toLong() shouldBeLessThanOrEqual ImportChunker.estimatedRequestBytes(listOf(card))
        }

        test("한 장이 바이트 상한을 넘어도 그 카드만 담은 묶음으로 보낸다") {
            val cards = listOf(CardContent("a", "b"), CardContent("가".repeat(500), "나".repeat(500)), CardContent("c", "d"))

            val chunks = ImportChunker.split(cards, maxItems = 1_000, maxBytes = 1_000, newKey = keys())

            chunks.map { it.cards.size } shouldBe listOf(1, 1, 1)
        }

        test("묶음은 나눈 뒤 원본 목록이 바뀌어도 같은 내용을 유지한다") {
            val source = mutableListOf(CardContent("a", "b"))

            val chunk = ImportChunker.split(source, maxItems = 10, maxBytes = Long.MAX_VALUE, newKey = keys()).single()
            source[0] = CardContent("changed", "changed")

            chunk.cards shouldBe listOf(CardContent("a", "b"))
        }

        test("묶음 크기 상한 하나에 들어가는 장수는 서버 상한 이하다") {
            ImportSaveLimits.CHUNK_SIZE shouldBeLessThanOrEqual 1_000
        }
    }

    private fun keys(): () -> String {
        var next = 0
        return { "key-${next++}" }
    }

    private companion object {
        const val MAX_BYTES = 2L * 1024 * 1024
    }
}

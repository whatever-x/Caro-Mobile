package com.whatever.caro.core.data.mapper

import com.whatever.caro.core.model.card.CardContent
import com.whatever.caro.core.remote.dto.card.response.CardResponse
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class CardMapperTest : FunSpec() {
    init {
        test("toModel은 응답의 front/back 필드를 Card 모델로 매핑한다") {
            val response =
                CardResponse(
                    cardId = 7L,
                    fields = mapOf("front" to "apple", "back" to "사과"),
                )

            val card = response.toModel()

            card?.id shouldBe 7L
            card?.content?.front shouldBe "apple"
            card?.content?.back shouldBe "사과"
        }

        test("toModel은 cardId가 null이면 null을 반환한다") {
            val response = CardResponse(cardId = null, fields = emptyMap())

            response.toModel() shouldBe null
        }

        test("toModel은 fields가 null이면 front/back을 빈 문자열로 대체한다") {
            val response = CardResponse(cardId = 1L, fields = null)

            val card = response.toModel()

            card?.content?.front shouldBe ""
            card?.content?.back shouldBe ""
        }

        test("toFields는 CardContent를 front/back 필드 맵으로 변환한다") {
            val content = CardContent(front = "hello", back = "안녕")

            content.toFields() shouldBe mapOf("front" to "hello", "back" to "안녕")
        }
    }
}

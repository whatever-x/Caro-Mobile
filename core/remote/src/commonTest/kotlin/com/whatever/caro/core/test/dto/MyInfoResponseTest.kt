package com.whatever.caro.core.test.dto

import com.whatever.caro.core.remote.dto.user.response.MyInfoResponse
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json

class MyInfoResponseTest : FunSpec() {
    init {
        val json = Json { explicitNulls = false }

        test("성인 여부가 없거나 null이면 null로 읽는다") {
            json.decodeFromString<MyInfoResponse>("{}").isAdult shouldBe null
            json.decodeFromString<MyInfoResponse>("""{"isAdult":null}""").isAdult shouldBe null
        }

        test("성인 여부 true와 false를 그대로 읽는다") {
            json.decodeFromString<MyInfoResponse>("""{"isAdult":true}""").isAdult shouldBe true
            json.decodeFromString<MyInfoResponse>("""{"isAdult":false}""").isAdult shouldBe false
        }
    }
}

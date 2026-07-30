package com.whatever.caro.core.data.mapper

import com.whatever.caro.core.remote.dto.user.response.MyInfoResponse
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class MyInfoMapperTest : FunSpec() {
    init {
        test("toMyNickname은 응답의 nickname을 반환한다") {
            MyInfoResponse(
                nickname = "캐로",
                email = "caro@example.com",
                loginPlatform = null,
            ).toMyNickname() shouldBe "캐로"
        }

        test("toMyNickname은 null nickname을 빈 문자열로 변환한다") {
            MyInfoResponse(
                nickname = null,
                email = null,
                loginPlatform = null,
            ).toMyNickname() shouldBe ""
        }
    }
}

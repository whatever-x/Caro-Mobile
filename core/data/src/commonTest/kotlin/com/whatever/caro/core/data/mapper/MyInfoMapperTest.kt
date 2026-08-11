package com.whatever.caro.core.data.mapper

import com.whatever.caro.core.model.auth.SocialLoginType
import com.whatever.caro.core.model.profile.MyInfo
import com.whatever.caro.core.remote.dto.user.response.MyInfoResponse
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class MyInfoMapperTest : FunSpec() {
    init {
        test("toMyInfo는 응답의 전체 사용자 정보를 반환한다") {
            MyInfoResponse(
                nickname = "캐로",
                email = "caro@example.com",
                loginPlatform = SocialLoginType.GOOGLE,
            ).toMyInfo() shouldBe
                MyInfo(
                    nickname = "캐로",
                    email = "caro@example.com",
                    socialLoginType = SocialLoginType.GOOGLE,
                )
        }

        test("toMyInfo는 nullable 필드를 화면 기본값으로 정규화한다") {
            MyInfoResponse(
                nickname = null,
                email = null,
                loginPlatform = null,
            ).toMyInfo() shouldBe
                MyInfo(
                    nickname = "",
                    email = "",
                    socialLoginType = SocialLoginType.NONE,
                )
        }

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

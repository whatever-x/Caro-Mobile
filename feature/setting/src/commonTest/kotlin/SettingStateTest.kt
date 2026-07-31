package com.whatever.caro.feature.setting

import com.whatever.caro.feature.setting.mvi.SettingState
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue

class SettingStateTest : FunSpec() {
    init {
        test("소셜 로그인 타입이 없어도 조회된 닉네임이 있으면 사용자 정보를 노출한다") {
            SettingState(
                isLoading = false,
                nickname = "캐로",
            ).isUserInfoVisible.shouldBeTrue()
        }

        test("로딩이 끝나고 조회된 닉네임이 없으면 사용자 정보를 노출하지 않는다") {
            SettingState(
                isLoading = false,
                nickname = "",
            ).isUserInfoVisible.shouldBeFalse()
        }

        test("초기 로딩 중에는 사용자 정보 스켈레톤 영역을 노출한다") {
            SettingState().isUserInfoVisible.shouldBeTrue()
        }
    }
}

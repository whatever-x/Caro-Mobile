package com.whatever.caro.feature.home

import com.whatever.caro.feature.home.mvi.HomeState
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue

class HomeStateTest : FunSpec() {
    init {
        test("첫 렌더부터 홈 본문을 노출하지 않는다") {
            HomeState().isLoadedContentVisible.shouldBeFalse()
        }

        test("초기 로딩이 끝나면 홈 본문을 노출한다") {
            HomeState(isLoading = false).isLoadedContentVisible.shouldBeTrue()
        }
    }
}

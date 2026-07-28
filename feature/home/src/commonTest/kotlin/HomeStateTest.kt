package com.whatever.caro.feature.home

import com.whatever.caro.feature.home.mvi.HomeState
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue

class HomeStateTest :
    FunSpec({
        test("초기 로딩 중에는 홈 본문을 노출하지 않는다") {
            HomeState(isLoading = true).isLoadedContentVisible.shouldBeFalse()
        }

        test("초기 로딩이 끝나면 홈 본문을 노출한다") {
            HomeState(isLoading = false).isLoadedContentVisible.shouldBeTrue()
        }
    })

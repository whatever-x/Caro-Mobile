package com.whatever.caro.composeApp

import com.whatever.caro.core.navigator.entries.CreateProfileEntry
import com.whatever.caro.core.navigator.entries.HomeEntry
import com.whatever.caro.core.navigator.entries.LoginEntry
import com.whatever.caro.core.navigator.entries.SplashEntry
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class SystemBarBackgroundTest : FunSpec() {
    init {
        test("Splash와 Login은 상하단 모두 브랜드 배경을 사용한다") {
            systemBarBackgroundRoles(SplashEntry) shouldBe
                SystemBarBackgroundRoles(
                    statusBar = SystemBarBackgroundRole.Brand,
                    navigationBar = SystemBarBackgroundRole.Brand,
                )
            systemBarBackgroundRoles(LoginEntry) shouldBe
                SystemBarBackgroundRoles(
                    statusBar = SystemBarBackgroundRole.Brand,
                    navigationBar = SystemBarBackgroundRole.Brand,
                )
        }

        test("Home은 상단 브랜드와 하단 기본 배경을 사용한다") {
            systemBarBackgroundRoles(HomeEntry) shouldBe
                SystemBarBackgroundRoles(
                    statusBar = SystemBarBackgroundRole.Brand,
                    navigationBar = SystemBarBackgroundRole.Primary,
                )
        }

        test("그 외 목적지는 상하단 모두 기본 배경을 사용한다") {
            systemBarBackgroundRoles(CreateProfileEntry) shouldBe
                SystemBarBackgroundRoles(
                    statusBar = SystemBarBackgroundRole.Primary,
                    navigationBar = SystemBarBackgroundRole.Primary,
                )
            systemBarBackgroundRoles(null) shouldBe
                SystemBarBackgroundRoles(
                    statusBar = SystemBarBackgroundRole.Primary,
                    navigationBar = SystemBarBackgroundRole.Primary,
                )
        }
    }
}

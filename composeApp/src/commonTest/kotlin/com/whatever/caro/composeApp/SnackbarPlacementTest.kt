package com.whatever.caro.composeApp

import androidx.compose.ui.unit.dp
import com.whatever.caro.core.navigator.entries.HomeEntry
import com.whatever.caro.core.navigator.entries.SplashEntry
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import com.whatever.caro.core.navigator.entries.Payload as HomePayload

class SnackbarPlacementTest : FunSpec() {
    init {
        test("Home에서는 플로팅 버튼 위 여백을 반환한다") {
            val home = HomeEntry(payload = HomePayload(id = 1, name = "Tester"))

            snackbarHostBottomPadding(home) shouldBe 88.dp
        }

        test("Home이 아니면 추가 여백을 반환하지 않는다") {
            snackbarHostBottomPadding(SplashEntry) shouldBe 0.dp
        }
    }
}

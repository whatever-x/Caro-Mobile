package com.whatever.caro.core.designsystem.components

import androidx.compose.material3.SnackbarHostState
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class SnackbarTest : FunSpec() {
    init {
        test("액션을 수행하면 onAction을 한 번 호출한다") {
            runTest {
                val hostState = SnackbarHostState()
                var calls = 0

                showSnackbarMessage(
                    coroutineScope = this,
                    snackbarHostState = hostState,
                    message = "완료",
                    actionLabel = "바로가기",
                    onAction = { calls += 1 },
                )
                runCurrent()
                hostState.currentSnackbarData?.performAction()
                advanceUntilIdle()

                calls shouldBe 1
            }
        }

        test("액션 없이 닫히면 onAction을 호출하지 않는다") {
            runTest {
                val hostState = SnackbarHostState()
                var calls = 0

                showSnackbarMessage(
                    coroutineScope = this,
                    snackbarHostState = hostState,
                    message = "완료",
                    actionLabel = "바로가기",
                    onAction = { calls += 1 },
                )
                runCurrent()
                hostState.currentSnackbarData?.dismiss()
                advanceUntilIdle()

                calls shouldBe 0
            }
        }
    }
}

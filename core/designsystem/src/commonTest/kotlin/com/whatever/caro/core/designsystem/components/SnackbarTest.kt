package com.whatever.caro.core.designsystem.components

import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarHostState
import com.whatever.caro.core.designsystem.animation.retainedSnackbarKeys
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class SnackbarTest : FunSpec() {
    init {
        test("스낵바 스타일은 Figma에 surface 토큰이 존재하는 변형만 제공한다") {
            CaroSnackbarStyle.entries shouldContainExactly
                listOf(
                    CaroSnackbarStyle.Normal,
                    CaroSnackbarStyle.Info,
                    CaroSnackbarStyle.Error,
                )
        }

        test("액션을 수행하면 스낵바를 즉시 제거하고 onAction을 한 번 호출한다") {
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
                val snackbarData = requireNotNull(hostState.currentSnackbarData)

                performCaroSnackbarAction(snackbarData)

                retainedSnackbarKeys(
                    keys = listOf(snackbarData, null),
                    current = null,
                    dismissImmediately = ::shouldDismissSnackbarImmediately,
                ) shouldBe emptyList<SnackbarData>()
                advanceUntilIdle()

                calls shouldBe 1
            }
        }

        test("액션 없이 닫히면 종료 애니메이션을 유지하고 onAction을 호출하지 않는다") {
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
                val snackbarData = requireNotNull(hostState.currentSnackbarData)

                snackbarData.dismiss()

                retainedSnackbarKeys(
                    keys = listOf(snackbarData, null),
                    current = null,
                    dismissImmediately = ::shouldDismissSnackbarImmediately,
                ) shouldBe listOf(snackbarData)
                advanceUntilIdle()

                calls shouldBe 0
            }
        }
    }
}

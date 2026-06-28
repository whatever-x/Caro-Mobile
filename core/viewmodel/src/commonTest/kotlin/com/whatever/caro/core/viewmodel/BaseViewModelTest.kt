package com.whatever.caro.core.viewmodel

import com.whatever.caro.core.viewmodel.contract.UiIntent
import com.whatever.caro.core.viewmodel.contract.UiSideEffect
import com.whatever.caro.core.viewmodel.contract.UiState
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class BaseViewModelTest : FunSpec() {
    private data object TestState : UiState

    private sealed interface TestIntent : UiIntent {
        data object Throw : TestIntent
    }

    private sealed interface TestSideEffect : UiSideEffect

    private class TestViewModel(
        exceptionFilter: ExceptionFilter,
    ) : BaseViewModel<TestState, TestIntent, TestSideEffect>(
            initialState = TestState,
            exceptionFilter = exceptionFilter,
        ) {
        var handledThrowable: Throwable? = null

        override suspend fun handleIntent(intent: TestIntent) {
            when (intent) {
                TestIntent.Throw -> throw IllegalStateException("boom")
            }
        }

        override fun handleClientException(throwable: Throwable) {
            handledThrowable = throwable
        }
    }

    init {
        val testDispatcher = StandardTestDispatcher()

        beforeTest {
            Dispatchers.setMain(testDispatcher)
        }

        afterTest {
            Dispatchers.resetMain()
        }

        test("필터가 suppress=true 를 반환하면 handleClientException 이 호출되지 않는다") {
            runTest {
                var filtered: Throwable? = null
                val viewModel =
                    TestViewModel(
                        exceptionFilter =
                            ExceptionFilter { throwable ->
                                filtered = throwable
                                true
                            },
                    )

                viewModel.intent(TestIntent.Throw)
                advanceUntilIdle()

                filtered?.message shouldBe "boom"
                viewModel.handledThrowable shouldBe null
            }
        }

        test("필터가 suppress=false 를 반환하면 handleClientException 으로 예외가 전달된다") {
            runTest {
                val viewModel = TestViewModel(exceptionFilter = ExceptionFilter.None)

                viewModel.intent(TestIntent.Throw)
                advanceUntilIdle()

                viewModel.handledThrowable?.message shouldBe "boom"
            }
        }
    }
}

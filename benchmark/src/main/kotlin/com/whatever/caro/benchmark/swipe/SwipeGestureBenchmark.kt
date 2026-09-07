package com.whatever.caro.benchmark.swipe

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class SwipeGestureBenchmark {
    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    private val config =
        InstrumentationRegistry
            .getArguments()
            .toSwipeBenchmarkConfig()

    @Test
    fun freeSwipeFrameTiming() {
        measureSwipe(scenario = freeSwipeScenario())
    }

    @Test
    fun lockedSwipeFrameTiming() {
        measureSwipe(scenario = lockedSwipeScenario())
    }

    @Test
    fun freeSwipeExitFrameTiming() {
        measureSwipe(scenario = freeSwipeExitScenario())
    }

    @Test
    fun lockedSwipeExitFrameTiming() {
        measureSwipe(scenario = lockedSwipeExitScenario())
    }

    private fun measureSwipe(scenario: SwipeBenchmarkScenario) {
        lateinit var preparedSwipe: PreparedSwipe

        benchmarkRule.measureRepeated(
            packageName = swipeBenchmarkContract.target.packageName,
            metrics = listOf(FrameTimingMetric()),
            compilationMode = CompilationMode.Full(),
            iterations = config.run.iterations,
            setupBlock = {
                killProcess()
                startActivityAndWait(scenario.createIntent(contract = swipeBenchmarkContract))
                preparedSwipe =
                    device.prepareSwipe(
                        contract = swipeBenchmarkContract,
                        timeoutMillis = config.gestureTimeoutMillis,
                    )
            },
        ) {
            repeat(config.swipesPerIteration) { index ->
                preparedSwipe =
                    device.performSwipeAndAwait(
                        contract = swipeBenchmarkContract,
                        preparedSwipe = preparedSwipe,
                        expectedResult = scenario.expectedResult,
                        timeoutMillis = config.gestureTimeoutMillis,
                        input = scenario.inputAt(index),
                    )
            }
        }
    }
}

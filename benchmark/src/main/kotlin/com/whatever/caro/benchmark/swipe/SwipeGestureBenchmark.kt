package com.whatever.caro.benchmark.swipe

import android.os.SystemClock
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

    private fun measureSwipe(scenario: SwipeBenchmarkScenario) {
        benchmarkRule.measureRepeated(
            packageName = swipeBenchmarkContract.target.packageName,
            metrics = listOf(FrameTimingMetric()),
            compilationMode = CompilationMode.Full(),
            iterations = config.run.iterations,
            setupBlock = {
                killProcess()
                startActivityAndWait(scenario.createIntent(contract = swipeBenchmarkContract))
                requireNotNull(device.waitForSwipeCard(contract = swipeBenchmarkContract))
            },
        ) {
            val card = requireNotNull(device.waitForSwipeCard(contract = swipeBenchmarkContract))
            repeat(config.swipesPerIteration) { index ->
                card.performSwipe(
                    device = device,
                    input = scenario.inputAt(index),
                )
                SystemClock.sleep(config.resetSettleMillis)
            }
        }
    }
}

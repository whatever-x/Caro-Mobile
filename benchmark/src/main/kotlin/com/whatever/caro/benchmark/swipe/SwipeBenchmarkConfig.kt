package com.whatever.caro.benchmark.swipe

import android.os.Bundle
import com.whatever.caro.benchmark.common.BenchmarkRunConfig
import com.whatever.caro.benchmark.common.toBenchmarkRunConfig

internal data class SwipeBenchmarkConfig(
    val run: BenchmarkRunConfig,
    val swipesPerIteration: Int = DEFAULT_SWIPES_PER_ITERATION,
    val gestureTimeoutMillis: Long = DEFAULT_GESTURE_TIMEOUT_MILLIS,
) {
    init {
        require(swipesPerIteration > 0) { "$SWIPES_ARGUMENT must be greater than zero." }
        require(gestureTimeoutMillis > 0L) { "$GESTURE_TIMEOUT_ARGUMENT must be greater than zero." }
    }
}

internal fun Bundle.toSwipeBenchmarkConfig(): SwipeBenchmarkConfig =
    SwipeBenchmarkConfig(
        run = toBenchmarkRunConfig(),
        swipesPerIteration = getString(SWIPES_ARGUMENT)?.toIntOrNull() ?: DEFAULT_SWIPES_PER_ITERATION,
        gestureTimeoutMillis =
            getString(GESTURE_TIMEOUT_ARGUMENT)?.toLongOrNull() ?: DEFAULT_GESTURE_TIMEOUT_MILLIS,
    )

private const val SWIPES_ARGUMENT = "swipesPerIteration"
private const val GESTURE_TIMEOUT_ARGUMENT = "gestureTimeoutMillis"
private const val DEFAULT_SWIPES_PER_ITERATION = 6
private const val DEFAULT_GESTURE_TIMEOUT_MILLIS = 5_000L

package com.whatever.caro.benchmark.swipe

import android.os.Bundle
import com.whatever.caro.benchmark.common.BenchmarkRunConfig
import com.whatever.caro.benchmark.common.toBenchmarkRunConfig

internal data class SwipeBenchmarkConfig(
    val run: BenchmarkRunConfig,
    val swipesPerIteration: Int = DEFAULT_SWIPES_PER_ITERATION,
    val resetSettleMillis: Long = DEFAULT_RESET_SETTLE_MILLIS,
) {
    init {
        require(swipesPerIteration > 0) { "$SWIPES_ARGUMENT must be greater than zero." }
        require(resetSettleMillis >= 0L) { "$RESET_SETTLE_ARGUMENT must not be negative." }
    }
}

internal fun Bundle.toSwipeBenchmarkConfig(): SwipeBenchmarkConfig =
    SwipeBenchmarkConfig(
        run = toBenchmarkRunConfig(),
        swipesPerIteration = getString(SWIPES_ARGUMENT)?.toIntOrNull() ?: DEFAULT_SWIPES_PER_ITERATION,
        resetSettleMillis = getString(RESET_SETTLE_ARGUMENT)?.toLongOrNull() ?: DEFAULT_RESET_SETTLE_MILLIS,
    )

private const val SWIPES_ARGUMENT = "swipesPerIteration"
private const val RESET_SETTLE_ARGUMENT = "resetSettleMillis"
private const val DEFAULT_SWIPES_PER_ITERATION = 6
private const val DEFAULT_RESET_SETTLE_MILLIS = 200L

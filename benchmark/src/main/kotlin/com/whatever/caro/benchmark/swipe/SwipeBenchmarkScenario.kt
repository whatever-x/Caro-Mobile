package com.whatever.caro.benchmark.swipe

import android.content.Intent
import com.whatever.caro.benchmark.common.BenchmarkTarget

internal data class SwipeBenchmarkContract(
    val target: BenchmarkTarget,
    val modeExtraName: String,
    val cardResourceId: String,
)

internal data class SwipeBenchmarkScenario(
    val mode: SwipeBenchmarkMode,
    val inputs: List<SwipeInput> = defaultSwipeInputs(),
) {
    init {
        require(inputs.isNotEmpty()) { "Swipe benchmark inputs must not be empty." }
    }
}

internal data class SwipeInput(
    val horizontalDistanceRatio: Float,
    val verticalDistanceRatio: Float,
    val steps: Int = DEFAULT_SWIPE_STEPS,
) {
    init {
        require(horizontalDistanceRatio in -1f..1f) { "Horizontal distance ratio must be between -1 and 1." }
        require(verticalDistanceRatio in -1f..1f) { "Vertical distance ratio must be between -1 and 1." }
        require(steps > 0) { "Swipe steps must be greater than zero." }
    }
}

internal enum class SwipeBenchmarkMode {
    FREE,
    LOCKED,
}

internal val swipeBenchmarkContract =
    SwipeBenchmarkContract(
        target =
            BenchmarkTarget(
                packageName = "com.whatever.caro.benchmark.target",
                activityName = "com.whatever.caro.benchmark.target.swipe.SwipeBenchmarkActivity",
            ),
        modeExtraName = "swipe_mode",
        cardResourceId = "swipe_benchmark_card",
    )

internal fun freeSwipeScenario(): SwipeBenchmarkScenario =
    SwipeBenchmarkScenario(
        mode = SwipeBenchmarkMode.FREE,
    )

internal fun lockedSwipeScenario(): SwipeBenchmarkScenario =
    SwipeBenchmarkScenario(
        mode = SwipeBenchmarkMode.LOCKED,
    )

internal fun SwipeBenchmarkScenario.createIntent(contract: SwipeBenchmarkContract): Intent =
    Intent().apply {
        setClassName(contract.target.packageName, contract.target.activityName)
        putExtra(contract.modeExtraName, mode.name)
    }

internal fun SwipeBenchmarkScenario.inputAt(index: Int): SwipeInput = inputs[index.mod(inputs.size)]

private fun defaultSwipeInputs(): List<SwipeInput> =
    listOf(
        SwipeInput(
            horizontalDistanceRatio = -HORIZONTAL_DISTANCE_RATIO,
            verticalDistanceRatio = 0f,
        ),
        SwipeInput(
            horizontalDistanceRatio = HORIZONTAL_DISTANCE_RATIO,
            verticalDistanceRatio = 0f,
        ),
        SwipeInput(
            horizontalDistanceRatio = 0f,
            verticalDistanceRatio = -VERTICAL_DISTANCE_RATIO,
        ),
    )

private const val HORIZONTAL_DISTANCE_RATIO = 0.25f
private const val VERTICAL_DISTANCE_RATIO = 0.2f
private const val DEFAULT_SWIPE_STEPS = 60

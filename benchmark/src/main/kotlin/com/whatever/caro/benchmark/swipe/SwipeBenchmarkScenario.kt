package com.whatever.caro.benchmark.swipe

import android.content.Intent
import com.whatever.caro.benchmark.common.BenchmarkTarget

internal data class SwipeBenchmarkContract(
    val target: BenchmarkTarget,
    val modeExtraName: String,
    val cardResourceId: String,
    val stateResourceId: String,
    val stateMarkerPrefix: String,
)

internal data class SwipeBenchmarkScenario(
    val mode: SwipeBenchmarkMode,
    val expectedResult: SwipeTerminalResult,
    val inputs: List<SwipeInput>,
) {
    init {
        require(inputs.isNotEmpty()) { "Swipe benchmark inputs must not be empty." }
    }
}

internal data class SwipeInput(
    val horizontalDistanceRatio: Float,
    val verticalDistanceRatio: Float,
    val expectedDirection: SwipeDirection,
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

internal enum class SwipeTerminalResult {
    RESET,
    EXIT,
}

internal enum class SwipeDirection {
    LEFT,
    RIGHT,
    UP,
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
        stateResourceId = "swipe_benchmark_state",
        stateMarkerPrefix = "swipe_benchmark",
    )

internal fun freeSwipeScenario(): SwipeBenchmarkScenario =
    SwipeBenchmarkScenario(
        mode = SwipeBenchmarkMode.FREE,
        expectedResult = SwipeTerminalResult.RESET,
        inputs = defaultResetSwipeInputs(),
    )

internal fun lockedSwipeScenario(): SwipeBenchmarkScenario =
    SwipeBenchmarkScenario(
        mode = SwipeBenchmarkMode.LOCKED,
        expectedResult = SwipeTerminalResult.RESET,
        inputs = defaultResetSwipeInputs(),
    )

internal fun freeSwipeExitScenario(): SwipeBenchmarkScenario =
    SwipeBenchmarkScenario(
        mode = SwipeBenchmarkMode.FREE,
        expectedResult = SwipeTerminalResult.EXIT,
        inputs = defaultExitSwipeInputs(),
    )

internal fun lockedSwipeExitScenario(): SwipeBenchmarkScenario =
    SwipeBenchmarkScenario(
        mode = SwipeBenchmarkMode.LOCKED,
        expectedResult = SwipeTerminalResult.EXIT,
        inputs = defaultExitSwipeInputs(),
    )

internal fun SwipeBenchmarkScenario.createIntent(contract: SwipeBenchmarkContract): Intent =
    Intent().apply {
        setClassName(contract.target.packageName, contract.target.activityName)
        putExtra(contract.modeExtraName, mode.name)
    }

internal fun SwipeBenchmarkScenario.inputAt(index: Int): SwipeInput = inputs[index.mod(inputs.size)]

private fun defaultResetSwipeInputs(): List<SwipeInput> =
    listOf(
        SwipeInput(
            horizontalDistanceRatio = -RESET_HORIZONTAL_DISTANCE_RATIO,
            verticalDistanceRatio = 0f,
            expectedDirection = SwipeDirection.LEFT,
        ),
        SwipeInput(
            horizontalDistanceRatio = RESET_HORIZONTAL_DISTANCE_RATIO,
            verticalDistanceRatio = 0f,
            expectedDirection = SwipeDirection.RIGHT,
        ),
        SwipeInput(
            horizontalDistanceRatio = 0f,
            verticalDistanceRatio = -RESET_VERTICAL_DISTANCE_RATIO,
            expectedDirection = SwipeDirection.UP,
        ),
    )

private fun defaultExitSwipeInputs(): List<SwipeInput> =
    listOf(
        SwipeInput(
            horizontalDistanceRatio = -EXIT_HORIZONTAL_DISTANCE_RATIO,
            verticalDistanceRatio = 0f,
            expectedDirection = SwipeDirection.LEFT,
        ),
        SwipeInput(
            horizontalDistanceRatio = EXIT_HORIZONTAL_DISTANCE_RATIO,
            verticalDistanceRatio = 0f,
            expectedDirection = SwipeDirection.RIGHT,
        ),
        SwipeInput(
            horizontalDistanceRatio = 0f,
            verticalDistanceRatio = -EXIT_VERTICAL_DISTANCE_RATIO,
            expectedDirection = SwipeDirection.UP,
        ),
    )

private const val RESET_HORIZONTAL_DISTANCE_RATIO = 0.25f
private const val RESET_VERTICAL_DISTANCE_RATIO = 0.2f
private const val EXIT_HORIZONTAL_DISTANCE_RATIO = 0.55f
private const val EXIT_VERTICAL_DISTANCE_RATIO = 0.45f
private const val DEFAULT_SWIPE_STEPS = 60

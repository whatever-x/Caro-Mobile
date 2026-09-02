package com.whatever.caro.benchmark.swipe

import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import kotlin.math.roundToInt

internal data class SwipePoint(
    val x: Int,
    val y: Int,
)

internal data class SwipePath(
    val start: SwipePoint,
    val end: SwipePoint,
    val steps: Int,
)

internal fun UiDevice.waitForSwipeCard(contract: SwipeBenchmarkContract): UiObject2? =
    wait(
        Until.findObject(By.res(contract.cardResourceId)),
        FIND_CARD_TIMEOUT_MILLIS,
    )

internal fun UiObject2.performSwipe(
    device: UiDevice,
    input: SwipeInput,
) {
    val path = resolveSwipePath(input = input)

    check(
        device.swipe(
            path.start.x,
            path.start.y,
            path.end.x,
            path.end.y,
            path.steps,
        ),
    )
}

internal fun UiObject2.resolveSwipePath(input: SwipeInput): SwipePath {
    val bounds = visibleBounds
    val start =
        SwipePoint(
            x = bounds.centerX(),
            y = bounds.centerY(),
        )
    val end =
        SwipePoint(
            x = start.x + (bounds.width() * input.horizontalDistanceRatio).roundToInt(),
            y = start.y + (bounds.height() * input.verticalDistanceRatio).roundToInt(),
        )

    return SwipePath(
        start = start,
        end = end,
        steps = input.steps,
    )
}

private const val FIND_CARD_TIMEOUT_MILLIS = 5_000L

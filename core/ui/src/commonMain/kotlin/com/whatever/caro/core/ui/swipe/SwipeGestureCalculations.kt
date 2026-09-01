package com.whatever.caro.core.ui.swipe

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Density
import kotlin.math.abs

internal fun Offset.resolveDirection(
    enabledDirections: Set<SwipeDirection>,
    activationThreshold: Float,
    upToHorizontalSwitchRatio: Float = 1f,
): SwipeDirection? {
    val horizontalDirection =
        when {
            x < -activationThreshold && SwipeDirection.LEFT in enabledDirections -> SwipeDirection.LEFT
            x > activationThreshold && SwipeDirection.RIGHT in enabledDirections -> SwipeDirection.RIGHT
            else -> null
        }
    val horizontalDistance = horizontalDirection?.let { abs(x) } ?: 0f
    val upDistance =
        if (y < -activationThreshold && SwipeDirection.UP in enabledDirections) {
            abs(y)
        } else {
            0f
        }

    return when {
        horizontalDirection == null && upDistance == 0f -> null
        horizontalDirection == null -> SwipeDirection.UP
        upDistance == 0f -> horizontalDirection
        horizontalDistance >= upDistance * upToHorizontalSwitchRatio.coerceIn(0.25f, 1f) -> horizontalDirection
        else -> SwipeDirection.UP
    }
}

internal fun Offset.resolveLockedDirection(
    enabledDirections: Set<SwipeDirection>,
    activationThreshold: Float,
    upToHorizontalSwitchRatio: Float,
): SwipeDirection? =
    resolveDirection(
        enabledDirections = enabledDirections,
        activationThreshold = activationThreshold,
        upToHorizontalSwitchRatio = upToHorizontalSwitchRatio,
    )

internal fun Offset.resolveSwipeGestureSnapshot(
    enabledDirections: Set<SwipeDirection>,
    activationThreshold: Float,
    swipeThreshold: Float,
    upToHorizontalSwitchRatio: Float,
): SwipeGestureSnapshot {
    val direction =
        resolveDirection(
            enabledDirections = enabledDirections,
            activationThreshold = activationThreshold,
            upToHorizontalSwitchRatio = upToHorizontalSwitchRatio,
        )

    return SwipeGestureSnapshot(
        offset = this,
        direction = direction,
        progress =
            resolveProgress(
                direction = direction,
                swipeThreshold = swipeThreshold,
            ),
    )
}

internal fun Offset.resolveLockedSwipeGestureSnapshot(
    enabledDirections: Set<SwipeDirection>,
    activationThreshold: Float,
    swipeThreshold: Float,
    upToHorizontalSwitchRatio: Float,
    projectToDirection: Boolean,
): SwipeGestureSnapshot {
    val direction =
        resolveLockedDirection(
            enabledDirections = enabledDirections,
            activationThreshold = activationThreshold,
            upToHorizontalSwitchRatio = upToHorizontalSwitchRatio,
        )
    val displayedOffset =
        if (projectToDirection) {
            projectTo(direction = direction)
        } else {
            this
        }

    return SwipeGestureSnapshot(
        offset = displayedOffset,
        direction = direction,
        progress =
            displayedOffset.resolveProgress(
                direction = direction,
                swipeThreshold = swipeThreshold,
            ),
    )
}

internal fun Offset.resolveProgress(
    direction: SwipeDirection?,
    swipeThreshold: Float,
): Float {
    if (direction == null || swipeThreshold <= 0f) return 0f

    val distance =
        when (direction) {
            SwipeDirection.LEFT -> -x
            SwipeDirection.RIGHT -> x
            SwipeDirection.UP -> -y
        }
    return (distance / swipeThreshold).coerceIn(0f, 1f)
}

internal fun Offset.projectTo(direction: SwipeDirection?): Offset =
    when (direction) {
        SwipeDirection.LEFT -> {
            Offset(
                x = x.coerceAtMost(0f),
                y = 0f,
            )
        }

        SwipeDirection.RIGHT -> {
            Offset(
                x = x.coerceAtLeast(0f),
                y = 0f,
            )
        }

        SwipeDirection.UP -> {
            Offset(x = 0f, y = y.coerceAtMost(0f))
        }

        null -> {
            Offset.Zero
        }
    }

internal fun Offset.scaleBy(factor: Float): Offset {
    val safeFactor = factor.coerceAtLeast(0.1f)

    return Offset(
        x = x * safeFactor,
        y = y * safeFactor,
    )
}

internal fun SwipeGestureConfig.targetOffset(
    direction: SwipeDirection,
    density: Density,
    currentOffset: Offset,
): Offset =
    with(density) {
        when (direction) {
            SwipeDirection.LEFT -> {
                Offset(
                    x = currentOffset.x + leftExitDistance.toPx(),
                    y = currentOffset.y,
                )
            }

            SwipeDirection.RIGHT -> {
                Offset(
                    x = currentOffset.x + rightExitDistance.toPx(),
                    y = currentOffset.y,
                )
            }

            SwipeDirection.UP -> {
                Offset(
                    x = currentOffset.x,
                    y = currentOffset.y + upExitDistance.toPx(),
                )
            }
        }
    }

internal fun SwipeGestureConfig.resolveAlpha(progress: Float): Float {
    val fadeStart = fadeStartProgress.coerceIn(0f, 1f)
    if (progress <= fadeStart) return 1f

    return lerp(
        start = 1f,
        stop = minAlpha.coerceIn(0f, 1f),
        fraction = ((progress - fadeStart) / (1f - fadeStart)).coerceIn(0f, 1f),
    )
}

internal fun lerp(
    start: Float,
    stop: Float,
    fraction: Float,
): Float = start + (stop - start) * fraction.coerceIn(0f, 1f)

internal fun lerp(
    start: Offset,
    stop: Offset,
    fraction: Float,
): Offset =
    Offset(
        x =
            lerp(
                start = start.x,
                stop = stop.x,
                fraction = fraction,
            ),
        y =
            lerp(
                start = start.y,
                stop = stop.y,
                fraction = fraction,
            ),
    )

internal const val COMPLETE_PROGRESS = 1f
internal const val DEFAULT_CARD_WIDTH = 1

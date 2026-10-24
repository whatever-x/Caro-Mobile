package com.whatever.caro.benchmark.swipe

import android.os.SystemClock
import androidx.test.uiautomator.By
import androidx.test.uiautomator.StaleObjectException
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import kotlin.math.abs
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

internal data class SwipeCardOrigin(
    val center: SwipePoint,
    val width: Int,
    val height: Int,
)

internal data class SwipeObservedState(
    val eventSequence: Int,
    val cardGeneration: Int,
    val isReady: Boolean,
    val terminalResult: SwipeTerminalResult?,
    val terminalDirection: SwipeDirection?,
)

internal data class PreparedSwipe(
    val origin: SwipeCardOrigin,
    val state: SwipeObservedState,
)

internal fun UiDevice.prepareSwipe(
    contract: SwipeBenchmarkContract,
    timeoutMillis: Long,
): PreparedSwipe {
    val card = requireSwipeCard(contract = contract, timeoutMillis = timeoutMillis)
    val state = requireReadySwipeState(contract = contract, timeoutMillis = timeoutMillis)

    check(state.eventSequence == 0) { "Swipe target setup contained an unexpected completed event: $state" }

    return PreparedSwipe(
        origin = card.toOrigin(),
        state = state,
    )
}

internal fun UiDevice.performSwipeAndAwait(
    contract: SwipeBenchmarkContract,
    preparedSwipe: PreparedSwipe,
    input: SwipeInput,
    expectedResult: SwipeTerminalResult,
    timeoutMillis: Long,
): PreparedSwipe {
    val card =
        requirePreparedSwipeCard(
            contract = contract,
            origin = preparedSwipe.origin,
            timeoutMillis = timeoutMillis,
        )
    val path = card.resolveSwipePath(input = input)

    check(
        swipe(
            path.start.x,
            path.start.y,
            path.end.x,
            path.end.y,
            path.steps,
        ),
    ) { "Android rejected swipe input: $path" }

    val completedState =
        awaitNextSwipeState(
            contract = contract,
            previousState = preparedSwipe.state,
            timeoutMillis = timeoutMillis,
        )

    check(completedState.isReady) { "Swipe completed without returning to ready state: $completedState" }
    check(completedState.terminalResult == expectedResult) {
        "Expected $expectedResult but observed ${completedState.terminalResult}: $completedState"
    }
    check(completedState.terminalDirection == input.expectedDirection) {
        "Expected ${input.expectedDirection} but observed ${completedState.terminalDirection}: $completedState"
    }

    val expectedGeneration =
        preparedSwipe.state.cardGeneration +
            if (expectedResult == SwipeTerminalResult.EXIT) 1 else 0
    check(completedState.cardGeneration == expectedGeneration) {
        "Expected card generation $expectedGeneration after $expectedResult but observed $completedState"
    }

    requirePreparedSwipeCard(
        contract = contract,
        origin = preparedSwipe.origin,
        timeoutMillis = timeoutMillis,
    )

    return preparedSwipe.copy(state = completedState)
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

private fun UiDevice.requireSwipeCard(
    contract: SwipeBenchmarkContract,
    timeoutMillis: Long,
): UiObject2 =
    requireNotNull(
        wait(
            Until.findObject(By.res(contract.cardResourceId)),
            timeoutMillis,
        ),
    ) { "Swipe card '${contract.cardResourceId}' was not found within $timeoutMillis ms." }

private fun UiDevice.requireReadySwipeState(
    contract: SwipeBenchmarkContract,
    timeoutMillis: Long,
): SwipeObservedState {
    val deadline = SystemClock.elapsedRealtime() + timeoutMillis
    var latestState: SwipeObservedState? = null

    while (SystemClock.elapsedRealtime() < deadline) {
        readSwipeStateOrNull(contract = contract)?.let { state ->
            latestState = state
            if (state.isReady) return state
        }
        SystemClock.sleep(STATE_POLL_INTERVAL_MILLIS)
    }

    error(
        "Swipe state '${contract.stateResourceId}' was not ready within $timeoutMillis ms. " +
            "Latest=$latestState",
    )
}

private fun UiDevice.awaitNextSwipeState(
    contract: SwipeBenchmarkContract,
    previousState: SwipeObservedState,
    timeoutMillis: Long,
): SwipeObservedState {
    val deadline = SystemClock.elapsedRealtime() + timeoutMillis
    var latestState = previousState

    while (SystemClock.elapsedRealtime() < deadline) {
        readSwipeStateOrNull(contract = contract)?.let { observedState ->
            latestState = observedState
            if (observedState.eventSequence > previousState.eventSequence && observedState.isReady) {
                check(observedState.eventSequence == previousState.eventSequence + 1) {
                    "Expected exactly one swipe event after $previousState but observed $observedState"
                }
                return observedState
            }
        }
        SystemClock.sleep(STATE_POLL_INTERVAL_MILLIS)
    }

    error(
        "Swipe did not reach a terminal state within $timeoutMillis ms. " +
            "Previous=$previousState, latest=$latestState",
    )
}

private fun UiDevice.readSwipeStateOrNull(contract: SwipeBenchmarkContract): SwipeObservedState? =
    findObject(By.res(contract.stateResourceId))?.let { stateNode ->
        try {
            stateNode.contentDescription.toSwipeObservedState(contract = contract)
        } catch (_: StaleObjectException) {
            null
        }
    }

private fun UiDevice.requirePreparedSwipeCard(
    contract: SwipeBenchmarkContract,
    origin: SwipeCardOrigin,
    timeoutMillis: Long,
): UiObject2 {
    val deadline = SystemClock.elapsedRealtime() + timeoutMillis
    var latestOrigin: SwipeCardOrigin? = null

    while (SystemClock.elapsedRealtime() < deadline) {
        findObject(By.res(contract.cardResourceId))?.let { card ->
            latestOrigin = card.toOriginOrNull() ?: return@let
            if (latestOrigin.isAt(origin)) return card
        }
        SystemClock.sleep(STATE_POLL_INTERVAL_MILLIS)
    }

    error(
        "Swipe card did not return to its prepared origin within $timeoutMillis ms. " +
            "Expected=$origin, latest=$latestOrigin",
    )
}

private fun UiObject2.toOrigin(): SwipeCardOrigin {
    val bounds = visibleBounds
    return SwipeCardOrigin(
        center = SwipePoint(x = bounds.centerX(), y = bounds.centerY()),
        width = bounds.width(),
        height = bounds.height(),
    )
}

private fun UiObject2.toOriginOrNull(): SwipeCardOrigin? =
    try {
        toOrigin()
    } catch (_: StaleObjectException) {
        null
    }

private fun SwipeCardOrigin?.isAt(expected: SwipeCardOrigin): Boolean =
    this != null &&
        abs(center.x - expected.center.x) <= CARD_POSITION_TOLERANCE_PX &&
        abs(center.y - expected.center.y) <= CARD_POSITION_TOLERANCE_PX &&
        width == expected.width &&
        height == expected.height

private fun String.toSwipeObservedState(contract: SwipeBenchmarkContract): SwipeObservedState {
    val parts = split(MARKER_SEPARATOR)
    require(parts.firstOrNull() == contract.stateMarkerPrefix) {
        "Unexpected swipe state marker '$this'."
    }
    val values =
        parts
            .drop(1)
            .associate { part ->
                val keyValue = part.split(KEY_VALUE_SEPARATOR, limit = 2)
                require(keyValue.size == 2) { "Malformed swipe state field '$part' in '$this'." }
                keyValue[0] to keyValue[1]
            }

    return SwipeObservedState(
        eventSequence = values.requireInt(STATE_EVENT_KEY, marker = this),
        cardGeneration = values.requireInt(STATE_GENERATION_KEY, marker = this),
        isReady = values.requireValue(STATE_READY_KEY, marker = this).toBooleanStrict(),
        terminalResult = values.requireEnumOrNull(STATE_RESULT_KEY, marker = this),
        terminalDirection = values.requireEnumOrNull(STATE_DIRECTION_KEY, marker = this),
    )
}

private fun Map<String, String>.requireValue(
    key: String,
    marker: String,
): String = requireNotNull(get(key)) { "Missing '$key' in swipe state marker '$marker'." }

private fun Map<String, String>.requireInt(
    key: String,
    marker: String,
): Int = requireValue(key = key, marker = marker).toInt()

private inline fun <reified T : Enum<T>> Map<String, String>.requireEnumOrNull(
    key: String,
    marker: String,
): T? =
    requireValue(key = key, marker = marker)
        .takeUnless { value -> value == NO_VALUE }
        ?.let { value -> enumValueOf<T>(value) }

private const val MARKER_SEPARATOR = ";"
private const val KEY_VALUE_SEPARATOR = "="
private const val NO_VALUE = "NONE"
private const val STATE_EVENT_KEY = "event"
private const val STATE_GENERATION_KEY = "generation"
private const val STATE_READY_KEY = "ready"
private const val STATE_RESULT_KEY = "result"
private const val STATE_DIRECTION_KEY = "direction"
private const val STATE_POLL_INTERVAL_MILLIS = 10L
private const val CARD_POSITION_TOLERANCE_PX = 2

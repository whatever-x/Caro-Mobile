package com.whatever.caro.core.ui.modifier

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import com.whatever.caro.core.ui.swipe.SwipeDirection
import com.whatever.caro.core.ui.swipe.SwipeGestureConfig
import com.whatever.caro.core.ui.swipe.SwipeGestureState
import kotlinx.coroutines.flow.collect

@Composable
internal fun SwipeFeedbackEffect(
    state: SwipeGestureState,
    motionConfig: SwipeGestureConfig,
    hapticFeedback: HapticFeedback,
    onSwipeDirectionChanged: (SwipeDirection?) -> Unit,
) {
    val updatedOnSwipeDirectionChanged by
        rememberUpdatedState(newValue = onSwipeDirectionChanged)
    var feedbackMemory by remember(state) { mutableStateOf(SwipeFeedbackMemory()) }
    val feedbackState =
        remember(state, motionConfig.hapticProgressThreshold) {
            derivedStateOf {
                SwipeFeedbackSnapshot(
                    direction = state.currentDirection,
                    hapticThresholdState =
                        state.progress.resolveHapticThresholdState(
                            threshold = motionConfig.hapticProgressThreshold,
                        ),
                )
            }
        }

    LaunchedEffect(
        state,
        hapticFeedback,
        motionConfig.hapticFeedbackEnabled,
        motionConfig.hapticProgressThreshold,
    ) {
        snapshotFlow { feedbackState.value }.collect { feedback ->
            val transition =
                feedbackMemory.reduce(
                    feedback = feedback,
                    hapticFeedbackEnabled = motionConfig.hapticFeedbackEnabled,
                )
            feedbackMemory = transition.nextMemory

            if (transition.shouldNotifyDirection) {
                updatedOnSwipeDirectionChanged(feedback.direction)
            }
            if (transition.shouldPerformHaptic) {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            }
        }
    }
}

private fun Float.resolveHapticThresholdState(threshold: Float): HapticThresholdState =
    when {
        this >= threshold -> HapticThresholdState.REACHED
        this < threshold / 2f -> HapticThresholdState.RESET
        else -> HapticThresholdState.BETWEEN
    }

private data class SwipeFeedbackMemory(
    val hasNotifiedDirection: Boolean = false,
    val lastNotifiedDirection: SwipeDirection? = null,
    val lastHapticDirection: SwipeDirection? = null,
)

private data class SwipeFeedbackTransition(
    val nextMemory: SwipeFeedbackMemory,
    val shouldNotifyDirection: Boolean,
    val shouldPerformHaptic: Boolean,
)

private data class SwipeFeedbackSnapshot(
    val direction: SwipeDirection?,
    val hapticThresholdState: HapticThresholdState,
)

private fun SwipeFeedbackMemory.reduce(
    feedback: SwipeFeedbackSnapshot,
    hapticFeedbackEnabled: Boolean,
): SwipeFeedbackTransition {
    val shouldNotifyDirection =
        !hasNotifiedDirection || lastNotifiedDirection != feedback.direction
    val shouldResetHaptic =
        feedback.direction == null ||
            feedback.hapticThresholdState == HapticThresholdState.RESET
    val shouldPerformHaptic =
        !shouldResetHaptic &&
            hapticFeedbackEnabled &&
            feedback.hapticThresholdState == HapticThresholdState.REACHED &&
            lastHapticDirection != feedback.direction
    val nextHapticDirection =
        when {
            shouldResetHaptic -> null
            shouldPerformHaptic -> feedback.direction
            else -> lastHapticDirection
        }

    return SwipeFeedbackTransition(
        nextMemory =
            copy(
                hasNotifiedDirection = hasNotifiedDirection || shouldNotifyDirection,
                lastNotifiedDirection =
                    if (shouldNotifyDirection) {
                        feedback.direction
                    } else {
                        lastNotifiedDirection
                    },
                lastHapticDirection = nextHapticDirection,
            ),
        shouldNotifyDirection = shouldNotifyDirection,
        shouldPerformHaptic = shouldPerformHaptic,
    )
}

private enum class HapticThresholdState {
    RESET,
    BETWEEN,
    REACHED,
}

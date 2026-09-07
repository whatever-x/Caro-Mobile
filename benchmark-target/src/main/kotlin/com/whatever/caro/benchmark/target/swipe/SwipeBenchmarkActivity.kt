package com.whatever.caro.benchmark.target.swipe

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import com.whatever.caro.core.ui.modifier.directionLockedSwipeGesture
import com.whatever.caro.core.ui.modifier.swipeGesture
import com.whatever.caro.core.ui.swipe.SwipeDirection
import com.whatever.caro.core.ui.swipe.SwipeGestureState
import com.whatever.caro.core.ui.swipe.rememberSwipeGestureState
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged

class SwipeBenchmarkActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val mode = SwipeBenchmarkMode.from(intent.getStringExtra(EXTRA_SWIPE_MODE))
        setContent {
            SwipeBenchmarkScreen(mode = mode)
        }
    }
}

@Composable
private fun SwipeBenchmarkScreen(mode: SwipeBenchmarkMode) {
    var observation by remember { mutableStateOf(SwipeBenchmarkObservation()) }

    MaterialTheme {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(Color.White)
                    .semantics { testTagsAsResourceId = true },
            contentAlignment = Alignment.Center,
        ) {
            key(observation.cardGeneration) {
                val swipeState = rememberSwipeGestureState()

                LaunchedEffect(swipeState) {
                    snapshotFlow { swipeState.isAnimationRunning }
                        .distinctUntilChanged()
                        .collect { isAnimationRunning ->
                            if (!isAnimationRunning && swipeState.offset == Offset.Zero) {
                                observation = observation.onResetSettled()
                            }
                        }
                }

                Box(
                    modifier =
                        Modifier
                            .size(width = 280.dp, height = 360.dp)
                            .testTag(SWIPE_CARD_TAG)
                            .benchmarkSwipeGesture(
                                mode = mode,
                                state = swipeState,
                                onSwipeDirectionChanged = { direction ->
                                    observation = observation.onDirectionChanged(direction = direction)
                                },
                                onSwiped = { direction ->
                                    observation = observation.onSwiped(direction = direction)
                                },
                            ).background(Color(0xFF4966E9)),
                )
            }

            Box(
                modifier =
                    Modifier
                        .align(Alignment.TopStart)
                        .size(1.dp)
                        .testTag(SWIPE_STATE_TAG)
                        .semantics {
                            contentDescription = observation.toMarker()
                        },
            )
        }
    }
}

private fun Modifier.benchmarkSwipeGesture(
    mode: SwipeBenchmarkMode,
    state: SwipeGestureState,
    onSwipeDirectionChanged: (SwipeDirection?) -> Unit,
    onSwiped: (SwipeDirection) -> Unit,
): Modifier =
    when (mode) {
        SwipeBenchmarkMode.FREE -> {
            swipeGesture(
                state = state,
                onSwipeDirectionChanged = onSwipeDirectionChanged,
                onSwiped = onSwiped,
            )
        }

        SwipeBenchmarkMode.LOCKED -> {
            directionLockedSwipeGesture(
                state = state,
                onSwipeDirectionChanged = onSwipeDirectionChanged,
                onSwiped = onSwiped,
            )
        }
    }

private data class SwipeBenchmarkObservation(
    val cardGeneration: Int = 0,
    val eventSequence: Int = 0,
    val pendingDirection: SwipeDirection? = null,
    val terminalResult: SwipeTerminalResult = SwipeTerminalResult.NONE,
    val terminalDirection: SwipeDirection? = null,
) {
    val isReady: Boolean
        get() = pendingDirection == null

    fun onDirectionChanged(direction: SwipeDirection?): SwipeBenchmarkObservation =
        direction?.let { activeDirection -> copy(pendingDirection = activeDirection) } ?: this

    fun onResetSettled(): SwipeBenchmarkObservation =
        pendingDirection?.let { completedDirection ->
            copy(
                eventSequence = eventSequence + 1,
                pendingDirection = null,
                terminalResult = SwipeTerminalResult.RESET,
                terminalDirection = completedDirection,
            )
        } ?: this

    fun onSwiped(direction: SwipeDirection): SwipeBenchmarkObservation =
        copy(
            cardGeneration = cardGeneration + 1,
            eventSequence = eventSequence + 1,
            pendingDirection = null,
            terminalResult = SwipeTerminalResult.EXIT,
            terminalDirection = direction,
        )

    fun toMarker(): String =
        listOf(
            SWIPE_STATE_MARKER_PREFIX,
            "event=$eventSequence",
            "generation=$cardGeneration",
            "ready=$isReady",
            "result=${terminalResult.name}",
            "direction=${terminalDirection?.name ?: NO_DIRECTION}",
        ).joinToString(separator = MARKER_SEPARATOR)
}

private enum class SwipeTerminalResult {
    NONE,
    RESET,
    EXIT,
}

private enum class SwipeBenchmarkMode {
    FREE,
    LOCKED,
    ;

    companion object {
        fun from(value: String?): SwipeBenchmarkMode = entries.firstOrNull { mode -> mode.name == value } ?: FREE
    }
}

private const val EXTRA_SWIPE_MODE = "swipe_mode"
private const val SWIPE_CARD_TAG = "swipe_benchmark_card"
private const val SWIPE_STATE_TAG = "swipe_benchmark_state"
private const val SWIPE_STATE_MARKER_PREFIX = "swipe_benchmark"
private const val MARKER_SEPARATOR = ";"
private const val NO_DIRECTION = "NONE"

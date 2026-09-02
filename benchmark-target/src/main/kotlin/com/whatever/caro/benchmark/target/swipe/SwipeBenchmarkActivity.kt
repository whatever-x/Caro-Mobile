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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import com.whatever.caro.core.ui.modifier.directionLockedSwipeGesture
import com.whatever.caro.core.ui.modifier.swipeGesture
import com.whatever.caro.core.ui.swipe.SwipeGestureState
import com.whatever.caro.core.ui.swipe.rememberSwipeGestureState

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
    MaterialTheme {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(Color.White)
                    .semantics { testTagsAsResourceId = true },
            contentAlignment = Alignment.Center,
        ) {
            val swipeState = rememberSwipeGestureState()

            Box(
                modifier =
                    Modifier
                        .size(width = 280.dp, height = 360.dp)
                        .testTag(SWIPE_CARD_TAG)
                        .benchmarkSwipeGesture(
                            mode = mode,
                            state = swipeState,
                        ).background(Color(0xFF4966E9)),
            )
        }
    }
}

private fun Modifier.benchmarkSwipeGesture(
    mode: SwipeBenchmarkMode,
    state: SwipeGestureState,
): Modifier =
    when (mode) {
        SwipeBenchmarkMode.FREE -> {
            swipeGesture(
                state = state,
                onSwiped = {},
            )
        }

        SwipeBenchmarkMode.LOCKED -> {
            directionLockedSwipeGesture(
                state = state,
                onSwiped = {},
            )
        }
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

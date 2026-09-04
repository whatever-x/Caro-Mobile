package com.whatever.caro.core.ui.swipe

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.snap
import androidx.compose.runtime.MonotonicFrameClock
import androidx.compose.ui.geometry.Offset
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.withContext

class SwipeGestureStateTest :
    FunSpec({
        test("programmatic animation updates direction and progress") {
            val state = SwipeGestureState(animationProgress = Animatable(initialValue = 0f))
            val resolver: SwipeGestureSnapshotResolver = { offset ->
                offset.resolveSwipeGestureSnapshot(
                    enabledDirections = setOf(SwipeDirection.RIGHT),
                    activationThreshold = 10f,
                    swipeThreshold = 100f,
                    upToHorizontalSwitchRatio = 1f,
                )
            }
            state.attachSnapshotResolver(resolver = resolver)

            withContext(TestFrameClock()) {
                state.animateTo(
                    targetOffset = Offset(x = 100f, y = 0f),
                    animationSpec = snap(),
                )
            }

            state.offset shouldBe Offset(x = 100f, y = 0f)
            state.currentDirection shouldBe SwipeDirection.RIGHT
            state.progress shouldBe 1f
        }
    })

private class TestFrameClock : MonotonicFrameClock {
    private var frameTimeNanos = 0L

    override suspend fun <R> withFrameNanos(onFrame: (frameTimeNanos: Long) -> R): R {
        frameTimeNanos += FRAME_DURATION_NANOS
        return onFrame(frameTimeNanos)
    }

    private companion object {
        const val FRAME_DURATION_NANOS = 16_000_000L
    }
}

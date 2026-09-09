package com.whatever.caro.core.ui.swipe

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.runtime.MonotonicFrameClock
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.snapshots.SnapshotStateObserver
import androidx.compose.ui.geometry.Offset
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext

class SwipeGestureStateTest : FunSpec() {
    init {
        test("programmatic animation updates direction and progress") {
            val state = createState()
            state.attachSnapshotResolver(resolver = createResolver(swipeThreshold = 100f))

            withContext(AdvancingFrameClock()) {
                state.animateTo(
                    targetOffset = Offset(x = 100f, y = 0f),
                    animationSpec = snap(),
                )
            }

            state.offset shouldBe Offset(x = 100f, y = 0f)
            state.currentDirection shouldBe SwipeDirection.RIGHT
            state.progress shouldBe 1f
        }

        test("offset-only updates do not invalidate direction or progress observers") {
            val state = createState()
            state.update(
                SwipeGestureSnapshot(
                    offset = Offset(x = 100f, y = 0f),
                    direction = SwipeDirection.RIGHT,
                    progress = 1f,
                ),
            )
            val observer = SnapshotStateObserver(onChangedExecutor = { callback -> callback() })
            var offsetInvalidations = 0
            var directionInvalidations = 0
            var progressInvalidations = 0

            observer.start()
            try {
                observer.observeReads(
                    scope = "offset",
                    onValueChangedForScope = { offsetInvalidations += 1 },
                    block = { state.offset },
                )
                observer.observeReads(
                    scope = "direction",
                    onValueChangedForScope = { directionInvalidations += 1 },
                    block = { state.currentDirection },
                )
                observer.observeReads(
                    scope = "progress",
                    onValueChangedForScope = { progressInvalidations += 1 },
                    block = { state.progress },
                )

                state.update(
                    SwipeGestureSnapshot(
                        offset = Offset(x = 120f, y = 0f),
                        direction = SwipeDirection.RIGHT,
                        progress = 1f,
                    ),
                )
                Snapshot.sendApplyNotifications()

                state.offset shouldBe Offset(x = 120f, y = 0f)
                state.currentDirection shouldBe SwipeDirection.RIGHT
                state.progress shouldBe 1f
                offsetInvalidations shouldBe 1
                directionInvalidations shouldBe 0
                progressInvalidations shouldBe 0
            } finally {
                observer.stop()
                observer.clear()
            }
        }

        test("direction and progress changes invalidate their observers") {
            val state = createState()
            state.update(
                SwipeGestureSnapshot(
                    offset = Offset(x = 100f, y = 0f),
                    direction = SwipeDirection.RIGHT,
                    progress = 1f,
                ),
            )
            val observer = SnapshotStateObserver(onChangedExecutor = { callback -> callback() })
            var directionInvalidations = 0
            var progressInvalidations = 0

            observer.start()
            try {
                observer.observeReads(
                    scope = "direction",
                    onValueChangedForScope = { directionInvalidations += 1 },
                    block = { state.currentDirection },
                )
                observer.observeReads(
                    scope = "progress",
                    onValueChangedForScope = { progressInvalidations += 1 },
                    block = { state.progress },
                )

                state.update(
                    SwipeGestureSnapshot(
                        offset = Offset(x = -75f, y = 0f),
                        direction = SwipeDirection.LEFT,
                        progress = 0.75f,
                    ),
                )
                Snapshot.sendApplyNotifications()

                state.currentDirection shouldBe SwipeDirection.LEFT
                state.progress shouldBe 0.75f
                directionInvalidations shouldBe 1
                progressInvalidations shouldBe 1
            } finally {
                observer.stop()
                observer.clear()
            }
        }

        test("initial resolver registration reevaluates the current offset") {
            val state = createState()
            state.snapTo(offset = Offset(x = 100f, y = 0f))

            state.attachSnapshotResolver(resolver = createResolver(swipeThreshold = 200f))

            state.offset shouldBe Offset(x = 100f, y = 0f)
            state.currentDirection shouldBe SwipeDirection.RIGHT
            state.progress shouldBe 0.5f
        }

        test("replacement resolver reevaluates the current offset") {
            val state = createState()
            state.attachSnapshotResolver(resolver = createResolver(swipeThreshold = 100f))
            state.snapTo(offset = Offset(x = 100f, y = 0f))
            state.progress shouldBe 1f

            state.attachSnapshotResolver(resolver = createResolver(swipeThreshold = 200f))

            state.offset shouldBe Offset(x = 100f, y = 0f)
            state.currentDirection shouldBe SwipeDirection.RIGHT
            state.progress shouldBe 0.5f
        }

        test("detaching an old registration keeps the replacement resolver active") {
            val state = createState()
            val oldRegistration =
                state.attachSnapshotResolver(resolver = createResolver(swipeThreshold = 100f))
            state.attachSnapshotResolver(resolver = createResolver(swipeThreshold = 200f))

            state.detachSnapshotResolver(registration = oldRegistration)
            state.snapTo(offset = Offset(x = 150f, y = 0f))

            state.progress shouldBe 0.75f
        }

        test("detaching the active registration stops future offset resolution") {
            val state = createState()
            val registration =
                state.attachSnapshotResolver(resolver = createResolver(swipeThreshold = 200f))
            state.snapTo(offset = Offset(x = 100f, y = 0f))

            state.detachSnapshotResolver(registration = registration)
            state.snapTo(offset = Offset(x = 150f, y = 0f))

            state.offset shouldBe Offset(x = 150f, y = 0f)
            state.currentDirection shouldBe SwipeDirection.RIGHT
            state.progress shouldBe 0.5f
        }

        test("animation uses a replacement resolver on subsequent frames") {
            val state = createState()
            val replacementResolver = createResolver(swipeThreshold = 200f)
            val oldSnapshotResolver = createResolver(swipeThreshold = 100f)
            var oldResolverCalls = 0
            val oldResolver: SwipeGestureSnapshotResolver = { offset ->
                oldResolverCalls += 1
                if (oldResolverCalls == 2) {
                    state.attachSnapshotResolver(resolver = replacementResolver)
                }
                oldSnapshotResolver(offset)
            }
            state.attachSnapshotResolver(resolver = oldResolver)

            withContext(AdvancingFrameClock()) {
                state.animateTo(
                    targetOffset = Offset(x = 100f, y = 0f),
                    animationSpec = tween(durationMillis = 64, easing = LinearEasing),
                )
            }

            oldResolverCalls shouldBe 2
            state.offset shouldBe Offset(x = 100f, y = 0f)
            state.progress shouldBe 0.5f
        }

        test("locked reset resolves progress from each animated offset") {
            val state = createState()
            val resolvedSnapshots = mutableListOf<SwipeGestureSnapshot>()
            val resolver: SwipeGestureSnapshotResolver = { offset ->
                offset
                    .resolveLockedSwipeGestureSnapshot(
                        enabledDirections = setOf(SwipeDirection.RIGHT),
                        activationThreshold = 10f,
                        swipeThreshold = 100f,
                        upToHorizontalSwitchRatio = 1f,
                        projectToDirection = false,
                    ).also(resolvedSnapshots::add)
            }
            state.attachSnapshotResolver(resolver = resolver)
            state.snapTo(offset = Offset(x = 100f, y = 0f))

            withContext(AdvancingFrameClock()) {
                state.reset(
                    animationSpec = tween(durationMillis = 64, easing = LinearEasing),
                )
            }

            val middleSnapshot = resolvedSnapshots.first { it.offset == Offset(x = 50f, y = 0f) }
            middleSnapshot.direction shouldBe SwipeDirection.RIGHT
            middleSnapshot.progress shouldBe 0.5f
            SwipeGestureConfig.Default.resolveAlpha(progress = middleSnapshot.progress) shouldBe 1f
            state.offset shouldBe Offset.Zero
            state.currentDirection shouldBe null
            state.progress shouldBe 0f
        }

        test("cancelling reset clears the animation running state") {
            runTest {
                val state = createState()
                state.attachSnapshotResolver(resolver = createResolver(swipeThreshold = 100f))
                state.snapTo(offset = Offset(x = 100f, y = 0f))
                val frameClock = SuspendingFrameClock()
                val resetJob =
                    launch {
                        withContext(frameClock) {
                            state.reset(animationSpec = tween(durationMillis = 100))
                        }
                    }
                frameClock.frameRequested.await()
                state.isAnimationRunning shouldBe true

                resetJob.cancelAndJoin()

                state.isAnimationRunning shouldBe false
            }
        }
    }
}

private fun createState(): SwipeGestureState = SwipeGestureState(animationProgress = Animatable(initialValue = 0f))

private fun createResolver(swipeThreshold: Float): SwipeGestureSnapshotResolver =
    { offset ->
        offset.resolveSwipeGestureSnapshot(
            enabledDirections = setOf(SwipeDirection.LEFT, SwipeDirection.RIGHT),
            activationThreshold = 10f,
            swipeThreshold = swipeThreshold,
            upToHorizontalSwitchRatio = 1f,
        )
    }

private class AdvancingFrameClock : MonotonicFrameClock {
    private var frameTimeNanos = 0L

    override suspend fun <R> withFrameNanos(onFrame: (frameTimeNanos: Long) -> R): R {
        frameTimeNanos += FRAME_DURATION_NANOS
        return onFrame(frameTimeNanos)
    }

    private companion object {
        const val FRAME_DURATION_NANOS = 16_000_000L
    }
}

private class SuspendingFrameClock : MonotonicFrameClock {
    val frameRequested = CompletableDeferred<Unit>()

    override suspend fun <R> withFrameNanos(onFrame: (frameTimeNanos: Long) -> R): R {
        frameRequested.complete(Unit)
        awaitCancellation()
    }
}

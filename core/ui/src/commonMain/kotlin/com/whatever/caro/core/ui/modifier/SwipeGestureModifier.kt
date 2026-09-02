package com.whatever.caro.core.ui.modifier

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.IntSize
import com.whatever.caro.core.ui.swipe.COMPLETE_PROGRESS
import com.whatever.caro.core.ui.swipe.DEFAULT_CARD_WIDTH
import com.whatever.caro.core.ui.swipe.SwipeDirection
import com.whatever.caro.core.ui.swipe.SwipeGestureConfig
import com.whatever.caro.core.ui.swipe.SwipeGestureSnapshot
import com.whatever.caro.core.ui.swipe.SwipeGestureState
import com.whatever.caro.core.ui.swipe.resolveAlpha
import com.whatever.caro.core.ui.swipe.resolveLockedSwipeGestureSnapshot
import com.whatever.caro.core.ui.swipe.resolveSwipeGestureSnapshot
import com.whatever.caro.core.ui.swipe.scaleBy
import com.whatever.caro.core.ui.swipe.targetOffset
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

fun Modifier.swipeGesture(
    state: SwipeGestureState,
    motionConfig: SwipeGestureConfig = SwipeGestureConfig.Default,
    enabled: Boolean = true,
    onSwipeDirectionChanged: (SwipeDirection?) -> Unit = {},
    onSwiped: (SwipeDirection) -> Unit,
): Modifier =
    composed {
        val density = LocalDensity.current
        val hapticFeedback = LocalHapticFeedback.current
        val coroutineScope = rememberCoroutineScope()
        val updatedOnSwiped by rememberUpdatedState(newValue = onSwiped)

        var size by remember { mutableStateOf(IntSize.Zero) }
        var runtime by remember { mutableStateOf(SwipeGestureRuntime()) }

        val activationThresholdPx = with(density) { motionConfig.directionActivationDistance.toPx() }
        val swipeThresholdPx = with(density) { motionConfig.swipeThreshold.toPx() }
        val resolveSwipeSnapshot: (Offset) -> SwipeGestureSnapshot =
            remember(motionConfig, density.density) {
                { offset ->
                    offset.resolveSwipeGestureSnapshot(
                        enabledDirections = motionConfig.enabledDirections,
                        activationThreshold = activationThresholdPx,
                        swipeThreshold = swipeThresholdPx,
                        upToHorizontalSwitchRatio = motionConfig.upToHorizontalSwitchRatio,
                    )
                }
            }

        DisposableEffect(state, resolveSwipeSnapshot) {
            val registration = state.attachSnapshotResolver(resolver = resolveSwipeSnapshot)
            onDispose { state.detachSnapshotResolver(registration = registration) }
        }

        SwipeFeedbackEffect(
            state = state,
            motionConfig = motionConfig,
            hapticFeedback = hapticFeedback,
            onSwipeDirectionChanged = onSwipeDirectionChanged,
        )

        // 호출부가 직접 실행 중인 애니메이션을 reset 이 가로채면 그 애니메이션이 취소되므로 건너뛴다.
        LaunchedEffect(enabled) {
            if (!enabled && !state.isAnimationRunning) {
                runtime.animationJob?.cancel()
                runtime = runtime.copy(animationJob = null)
                state.reset(
                    animationSpec = motionConfig.resetAnimationSpec,
                    resolveSnapshot = resolveSwipeSnapshot,
                )
            }
        }

        onSizeChanged { size = it }
            .pointerInput(
                state,
                enabled,
                motionConfig,
                resolveSwipeSnapshot,
            ) {
                if (!enabled) return@pointerInput

                detectDragGestures(
                    onDragStart = {
                        runtime.animationJob?.cancel()
                        runtime = runtime.copy(animationJob = null)
                    },
                    onDragCancel = {
                        runtime =
                            runtime.copy(
                                animationJob =
                                    coroutineScope.launch {
                                        state.reset(
                                            animationSpec = motionConfig.resetAnimationSpec,
                                            resolveSnapshot = resolveSwipeSnapshot,
                                        )
                                    },
                            )
                    },
                    onDragEnd = {
                        val releasedDirection = state.currentDirection
                        val releasedProgress = state.progress

                        runtime =
                            runtime.copy(
                                animationJob =
                                    coroutineScope.launch {
                                        if (releasedDirection != null && releasedProgress >= COMPLETE_PROGRESS) {
                                            state.animateTo(
                                                targetOffset =
                                                    motionConfig.targetOffset(
                                                        direction = releasedDirection,
                                                        density = density,
                                                        currentOffset = state.offset,
                                                    ),
                                                animationSpec = motionConfig.exitAnimationSpec,
                                                resolveSnapshot = resolveSwipeSnapshot,
                                            )
                                            updatedOnSwiped(releasedDirection)
                                        } else {
                                            state.reset(
                                                animationSpec = motionConfig.resetAnimationSpec,
                                                resolveSnapshot = resolveSwipeSnapshot,
                                            )
                                        }
                                    },
                            )
                    },
                    onDrag = { change, dragAmount ->
                        if (change.positionChange() != Offset.Zero) {
                            change.consume()
                        }
                        state.update(
                            resolveSwipeSnapshot(
                                state.offset + dragAmount.scaleBy(factor = motionConfig.dragSensitivity),
                            ),
                        )
                    },
                )
            }.graphicsLayer {
                val progressFraction = state.progress
                val width = size.width.takeIf { it > 0 } ?: DEFAULT_CARD_WIDTH
                translationX = state.offset.x
                translationY = state.offset.y
                rotationZ =
                    (state.offset.x / width)
                        .coerceIn(-1f, 1f) * motionConfig.maxRotationDegrees
                alpha = motionConfig.resolveAlpha(progress = progressFraction)
            }
    }

fun Modifier.directionLockedSwipeGesture(
    state: SwipeGestureState,
    motionConfig: SwipeGestureConfig = SwipeGestureConfig.Default,
    enabled: Boolean = true,
    onSwipeDirectionChanged: (SwipeDirection?) -> Unit = {},
    onSwiped: (SwipeDirection) -> Unit,
): Modifier =
    composed {
        val density = LocalDensity.current
        val hapticFeedback = LocalHapticFeedback.current
        val coroutineScope = rememberCoroutineScope()
        val updatedOnSwiped by rememberUpdatedState(newValue = onSwiped)

        var size by remember { mutableStateOf(IntSize.Zero) }
        var runtime by remember { mutableStateOf(SwipeGestureRuntime()) }

        val safeLockedSensitivity = motionConfig.lockedDragSensitivity.coerceAtLeast(0.1f)
        val activationThresholdPx = with(density) { motionConfig.lockedDirectionActivationDistance.toPx() }
        val swipeThresholdPx = with(density) { motionConfig.lockedSwipeThreshold.toPx() }
        val resolveDraggedSwipeSnapshot: (Offset) -> SwipeGestureSnapshot =
            remember(motionConfig, density.density) {
                { offset ->
                    offset.resolveLockedSwipeGestureSnapshot(
                        enabledDirections = motionConfig.enabledDirections,
                        activationThreshold = activationThresholdPx,
                        swipeThreshold = swipeThresholdPx,
                        upToHorizontalSwitchRatio = motionConfig.lockedUpToHorizontalSwitchRatio,
                        projectToDirection = true,
                    )
                }
            }
        val resolveAnimatedSwipeSnapshot: (Offset) -> SwipeGestureSnapshot =
            remember(motionConfig, density.density) {
                { offset ->
                    offset.resolveLockedSwipeGestureSnapshot(
                        enabledDirections = motionConfig.enabledDirections,
                        activationThreshold = activationThresholdPx,
                        swipeThreshold = swipeThresholdPx,
                        upToHorizontalSwitchRatio = motionConfig.lockedUpToHorizontalSwitchRatio,
                        projectToDirection = false,
                    )
                }
            }

        DisposableEffect(state, resolveAnimatedSwipeSnapshot) {
            val registration = state.attachSnapshotResolver(resolver = resolveAnimatedSwipeSnapshot)
            onDispose { state.detachSnapshotResolver(registration = registration) }
        }

        SwipeFeedbackEffect(
            state = state,
            motionConfig = motionConfig,
            hapticFeedback = hapticFeedback,
            onSwipeDirectionChanged = onSwipeDirectionChanged,
        )

        LaunchedEffect(enabled) {
            if (!enabled) {
                runtime.animationJob?.cancel()
                runtime = runtime.copy(animationJob = null)
                state.reset(
                    animationSpec = motionConfig.resetAnimationSpec,
                    resolveSnapshot = resolveAnimatedSwipeSnapshot,
                )
                runtime = runtime.copy(dragOffset = Offset.Zero)
            }
        }

        onSizeChanged { size = it }
            .pointerInput(
                state,
                enabled,
                motionConfig,
                resolveDraggedSwipeSnapshot,
            ) {
                if (!enabled) return@pointerInput

                detectDragGestures(
                    onDragStart = {
                        runtime.animationJob?.cancel()
                        runtime =
                            runtime.copy(
                                animationJob = null,
                                dragOffset =
                                    Offset(
                                        x = state.offset.x / safeLockedSensitivity,
                                        y = state.offset.y / safeLockedSensitivity,
                                    ),
                            )
                    },
                    onDragCancel = {
                        runtime =
                            runtime.copy(
                                animationJob =
                                    coroutineScope.launch {
                                        state.reset(
                                            animationSpec = motionConfig.resetAnimationSpec,
                                            resolveSnapshot = resolveAnimatedSwipeSnapshot,
                                        )
                                        runtime = runtime.copy(dragOffset = Offset.Zero)
                                    },
                            )
                    },
                    onDragEnd = {
                        val releasedDirection = state.currentDirection
                        val releasedOffset = state.offset
                        val releasedProgress = state.progress

                        runtime =
                            runtime.copy(
                                animationJob =
                                    coroutineScope.launch {
                                        if (releasedDirection != null && releasedProgress >= COMPLETE_PROGRESS) {
                                            state.animateTo(
                                                targetOffset =
                                                    motionConfig.targetOffset(
                                                        direction = releasedDirection,
                                                        density = density,
                                                        currentOffset = releasedOffset,
                                                    ),
                                                animationSpec = motionConfig.exitAnimationSpec,
                                                resolveSnapshot = resolveAnimatedSwipeSnapshot,
                                            )
                                            updatedOnSwiped(releasedDirection)
                                        } else {
                                            state.reset(
                                                animationSpec = motionConfig.resetAnimationSpec,
                                                resolveSnapshot = resolveAnimatedSwipeSnapshot,
                                            )
                                            runtime = runtime.copy(dragOffset = Offset.Zero)
                                        }
                                    },
                            )
                    },
                    onDrag = { change, dragAmount ->
                        if (change.positionChange() != Offset.Zero) {
                            change.consume()
                        }
                        runtime = runtime.copy(dragOffset = runtime.dragOffset + dragAmount)
                        if (!state.isAnimationRunning) {
                            state.update(
                                resolveDraggedSwipeSnapshot(
                                    runtime.dragOffset.scaleBy(factor = safeLockedSensitivity),
                                ),
                            )
                        }
                    },
                )
            }.graphicsLayer {
                val progressFraction = state.progress
                val width = size.width.takeIf { it > 0 } ?: DEFAULT_CARD_WIDTH
                translationX = state.offset.x
                translationY = state.offset.y
                rotationZ =
                    (state.offset.x / width)
                        .coerceIn(-1f, 1f) * motionConfig.maxRotationDegrees
                alpha = motionConfig.resolveAlpha(progress = progressFraction)
            }
    }

private data class SwipeGestureRuntime(
    val animationJob: Job? = null,
    val dragOffset: Offset = Offset.Zero,
)

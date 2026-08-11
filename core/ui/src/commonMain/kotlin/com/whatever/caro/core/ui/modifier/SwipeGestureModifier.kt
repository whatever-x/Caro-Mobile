package com.whatever.caro.core.ui.modifier

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
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
import com.whatever.caro.core.ui.swipe.SwipeGestureState
import com.whatever.caro.core.ui.swipe.projectTo
import com.whatever.caro.core.ui.swipe.resolveAlpha
import com.whatever.caro.core.ui.swipe.resolveDirection
import com.whatever.caro.core.ui.swipe.resolveLockedDirection
import com.whatever.caro.core.ui.swipe.resolveProgress
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
        val updatedOnSwipeDirectionChanged by rememberUpdatedState(newValue = onSwipeDirectionChanged)

        var size by remember { mutableStateOf(IntSize.Zero) }
        var animationJob by remember { mutableStateOf<Job?>(null) }
        var lastHapticDirection by remember { mutableStateOf<SwipeDirection?>(null) }

        val activationThresholdPx = with(density) { motionConfig.directionActivationDistance.toPx() }
        val swipeThresholdPx = with(density) { motionConfig.swipeThreshold.toPx() }
        val direction =
            state.offset.resolveDirection(
                enabledDirections = motionConfig.enabledDirections,
                activationThreshold = activationThresholdPx,
                upToHorizontalSwitchRatio = motionConfig.upToHorizontalSwitchRatio,
            )
        val progress =
            state.offset.resolveProgress(
                direction = direction,
                swipeThreshold = swipeThresholdPx,
            )

        SideEffect {
            state.updateSwipeInfo(
                direction = direction,
                progress = progress,
            )
        }

        LaunchedEffect(state.currentDirection) {
            updatedOnSwipeDirectionChanged(state.currentDirection)
        }

        LaunchedEffect(
            state.currentDirection,
            state.progress,
            motionConfig.hapticFeedbackEnabled,
        ) {
            val currentDirection = state.currentDirection
            val shouldPerformHaptic =
                motionConfig.hapticFeedbackEnabled &&
                    currentDirection != null &&
                    state.progress >= motionConfig.hapticProgressThreshold &&
                    lastHapticDirection != currentDirection

            if (shouldPerformHaptic) {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                lastHapticDirection = currentDirection
            } else if (
                currentDirection == null ||
                state.progress < motionConfig.hapticProgressThreshold / 2f
            ) {
                lastHapticDirection = null
            }
        }

        // 호출부가 직접 실행 중인 애니메이션을 reset 이 가로채면 그 애니메이션이 취소되므로 건너뛴다.
        LaunchedEffect(enabled) {
            if (!enabled && !state.isAnimationRunning) {
                animationJob?.cancel()
                state.reset(animationSpec = motionConfig.resetAnimationSpec)
            }
        }

        onSizeChanged { size = it }
            .pointerInput(
                enabled,
                motionConfig,
            ) {
                if (!enabled) return@pointerInput

                detectDragGestures(
                    onDragStart = {
                        animationJob?.cancel()
                    },
                    onDragCancel = {
                        animationJob =
                            coroutineScope.launch {
                                state.reset(animationSpec = motionConfig.resetAnimationSpec)
                            }
                    },
                    onDragEnd = {
                        val releasedDirection =
                            state.offset.resolveDirection(
                                enabledDirections = motionConfig.enabledDirections,
                                activationThreshold = activationThresholdPx,
                                upToHorizontalSwitchRatio = motionConfig.upToHorizontalSwitchRatio,
                            )
                        val releasedProgress =
                            state.offset.resolveProgress(
                                direction = releasedDirection,
                                swipeThreshold = swipeThresholdPx,
                            )

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
                                    )
                                    updatedOnSwiped(releasedDirection)
                                } else {
                                    state.reset(animationSpec = motionConfig.resetAnimationSpec)
                                }
                            }
                    },
                    onDrag = { change, dragAmount ->
                        if (change.positionChange() != Offset.Zero) {
                            change.consume()
                        }
                        state.dragBy(
                            delta = dragAmount.scaleBy(factor = motionConfig.dragSensitivity),
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
        val updatedOnSwipeDirectionChanged by rememberUpdatedState(newValue = onSwipeDirectionChanged)

        var size by remember { mutableStateOf(IntSize.Zero) }
        var dragOffset by remember { mutableStateOf(Offset.Zero) }
        var animationJob by remember { mutableStateOf<Job?>(null) }
        var lastHapticDirection by remember { mutableStateOf<SwipeDirection?>(null) }

        val safeLockedSensitivity = motionConfig.lockedDragSensitivity.coerceAtLeast(0.1f)
        val activationThresholdPx = with(density) { motionConfig.lockedDirectionActivationDistance.toPx() }
        val swipeThresholdPx = with(density) { motionConfig.lockedSwipeThreshold.toPx() }
        val effectiveDragOffset = dragOffset.scaleBy(factor = safeLockedSensitivity)
        val direction =
            effectiveDragOffset.resolveLockedDirection(
                enabledDirections = motionConfig.enabledDirections,
                activationThreshold = activationThresholdPx,
                upToHorizontalSwitchRatio = motionConfig.lockedUpToHorizontalSwitchRatio,
            )
        val projectedDragOffset =
            effectiveDragOffset.projectTo(
                direction = direction,
            )
        val progress =
            projectedDragOffset.resolveProgress(
                direction = direction,
                swipeThreshold = swipeThresholdPx,
            )

        SideEffect {
            state.updateSwipeInfo(
                direction = direction,
                progress = progress,
            )
        }

        LaunchedEffect(state.currentDirection) {
            updatedOnSwipeDirectionChanged(state.currentDirection)
        }

        LaunchedEffect(
            state.currentDirection,
            state.progress,
            motionConfig.hapticFeedbackEnabled,
        ) {
            val currentDirection = state.currentDirection
            val shouldPerformHaptic =
                motionConfig.hapticFeedbackEnabled &&
                    currentDirection != null &&
                    state.progress >= motionConfig.hapticProgressThreshold &&
                    lastHapticDirection != currentDirection

            if (shouldPerformHaptic) {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                lastHapticDirection = currentDirection
            } else if (
                currentDirection == null ||
                state.progress < motionConfig.hapticProgressThreshold / 2f
            ) {
                lastHapticDirection = null
            }
        }

        LaunchedEffect(
            direction,
            projectedDragOffset,
        ) {
            if (!state.isAnimationRunning) {
                state.snapTo(projectedDragOffset)
            }
        }

        LaunchedEffect(enabled) {
            if (!enabled) {
                animationJob?.cancel()
                state.reset(animationSpec = motionConfig.resetAnimationSpec)
                dragOffset = Offset.Zero
            }
        }

        onSizeChanged { size = it }
            .pointerInput(
                enabled,
                motionConfig,
            ) {
                if (!enabled) return@pointerInput

                detectDragGestures(
                    onDragStart = {
                        animationJob?.cancel()
                        dragOffset =
                            Offset(
                                x = state.offset.x / safeLockedSensitivity,
                                y = state.offset.y / safeLockedSensitivity,
                            )
                    },
                    onDragCancel = {
                        animationJob =
                            coroutineScope.launch {
                                state.reset(animationSpec = motionConfig.resetAnimationSpec)
                                dragOffset = Offset.Zero
                            }
                    },
                    onDragEnd = {
                        val releasedEffectiveOffset =
                            dragOffset.scaleBy(factor = safeLockedSensitivity)
                        val releasedDirection =
                            releasedEffectiveOffset.resolveLockedDirection(
                                enabledDirections = motionConfig.enabledDirections,
                                activationThreshold = activationThresholdPx,
                                upToHorizontalSwitchRatio = motionConfig.lockedUpToHorizontalSwitchRatio,
                            )
                        val releasedOffset =
                            releasedEffectiveOffset.projectTo(
                                direction = releasedDirection,
                            )
                        val releasedProgress =
                            releasedOffset.resolveProgress(
                                direction = releasedDirection,
                                swipeThreshold = swipeThresholdPx,
                            )

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
                                    )
                                    updatedOnSwiped(releasedDirection)
                                } else {
                                    state.reset(animationSpec = motionConfig.resetAnimationSpec)
                                    dragOffset = Offset.Zero
                                }
                            }
                    },
                    onDrag = { change, dragAmount ->
                        if (change.positionChange() != Offset.Zero) {
                            change.consume()
                        }
                        val nextDragOffset = dragOffset + dragAmount
                        val nextEffectiveOffset =
                            nextDragOffset.scaleBy(factor = safeLockedSensitivity)
                        val nextDirection =
                            nextEffectiveOffset.resolveLockedDirection(
                                enabledDirections = motionConfig.enabledDirections,
                                activationThreshold = activationThresholdPx,
                                upToHorizontalSwitchRatio = motionConfig.lockedUpToHorizontalSwitchRatio,
                            )
                        dragOffset = nextDragOffset
                        if (!state.isAnimationRunning) {
                            state.snapTo(
                                nextEffectiveOffset.projectTo(
                                    direction = nextDirection,
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

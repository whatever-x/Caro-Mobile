package com.whatever.caro.core.ui.swipe

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset

/**
 * 스와이프 제스처의 현재 위치, 방향, 진행률, 애니메이션 상태를 보관하는 상태 객체입니다.
 *
 * [rememberSwipeGestureState]를 통해 생성하며, 제스처 modifier 내부에서 드래그와 애니메이션 상태를 갱신합니다.
 *
 * @author gunhyung
 */
@Stable
class SwipeGestureState internal constructor(
    private val animationProgress: Animatable<Float, AnimationVector1D>,
) {
    /**
     * 현재 composable이 이동한 좌표입니다.
     */
    var offset: Offset by mutableStateOf(Offset.Zero)
        private set

    /**
     * 현재 드래그 방향입니다.
     *
     * 아직 방향 판단 기준을 넘지 않았거나 제스처가 초기화된 경우 `null`입니다.
     */
    var currentDirection: SwipeDirection? by mutableStateOf(null)
        private set

    /**
     * 현재 스와이프 진행률입니다.
     *
     * `0f`는 스와이프가 진행되지 않은 상태이고, `1f`는 완료 기준에 도달한 상태입니다.
     */
    var progress: Float by mutableStateOf(0f)
        private set

    /**
     * 현재 reset 또는 exit 애니메이션이 실행 중인지 여부입니다.
     */
    var isAnimationRunning: Boolean by mutableStateOf(false)
        private set

    /**
     * 애니메이션 없이 위치를 즉시 변경합니다.
     *
     * @param offset 변경할 목표 위치입니다.
     */
    fun snapTo(offset: Offset) {
        this.offset = offset
    }

    /**
     * 현재 위치에 드래그 이동량을 더합니다.
     *
     * @param delta 이번 드래그 이벤트에서 이동한 거리입니다.
     */
    fun dragBy(delta: Offset) {
        offset += delta
    }

    /**
     * 현재 위치를 원점으로 되돌립니다.
     *
     * @param animationSpec 원점으로 돌아갈 때 사용할 애니메이션 설정입니다.
     */
    suspend fun reset(animationSpec: AnimationSpec<Float>) {
        animateTo(
            targetOffset = Offset.Zero,
            animationSpec = animationSpec,
        )
    }

    /**
     * 현재 위치에서 목표 위치까지 애니메이션으로 이동합니다.
     *
     * @param targetOffset 애니메이션이 끝났을 때 도달할 위치입니다.
     * @param animationSpec 이동에 사용할 애니메이션 설정입니다.
     */
    suspend fun animateTo(
        targetOffset: Offset,
        animationSpec: AnimationSpec<Float>,
    ) {
        isAnimationRunning = true
        try {
            val startOffset = offset
            animationProgress.snapTo(targetValue = 0f)
            animationProgress.animateTo(
                targetValue = 1f,
                animationSpec = animationSpec,
            ) {
                offset =
                    lerp(
                        start = startOffset,
                        stop = targetOffset,
                        fraction = value,
                    )
            }
        } finally {
            isAnimationRunning = false
        }
    }

    internal fun updateSwipeInfo(
        direction: SwipeDirection?,
        progress: Float,
    ) {
        currentDirection = direction
        this.progress = progress.coerceIn(0f, 1f)
    }
}

/**
 * recomposition 사이에서 유지되는 [SwipeGestureState]를 생성합니다.
 *
 * @author gunhyung
 */
@Composable
fun rememberSwipeGestureState(): SwipeGestureState {
    val animationProgress = remember { Animatable(initialValue = 0f) }

    return remember(animationProgress) {
        SwipeGestureState(
            animationProgress = animationProgress,
        )
    }
}

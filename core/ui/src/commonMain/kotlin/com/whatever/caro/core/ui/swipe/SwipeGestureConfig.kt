package com.whatever.caro.core.ui.swipe

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 스와이프 제스처의 동작 기준과 애니메이션 값을 정의하는 설정 객체입니다.
 *
 * 생성자는 모듈 내부에서만 사용할 수 있으며, 외부에서는 [Default]를 통해 정해진 설정을 사용합니다.
 *
 * @property enabledDirections 스와이프를 허용할 방향 목록입니다.
 * @property directionActivationDistance 자유 스와이프에서 사용자의 드래그 방향을 판단하기 시작하는 최소 이동 거리입니다.
 * @property swipeThreshold 스와이프 완료 여부를 판단하는 기준 거리입니다.
 * @property dragSensitivity 자유 스와이프에서 드래그 이동량에 곱해지는 민감도입니다.
 * @property upToHorizontalSwitchRatio 위 방향과 좌우 방향이 함께 감지될 때 방향 선택에 사용하는 비율입니다.
 * @property lockedDirectionActivationDistance 방향 고정 스와이프에서 드래그 방향을 확정하기 시작하는 최소 이동 거리입니다.
 * @property lockedSwipeThreshold 방향 고정 스와이프에서 완료 여부를 판단하는 기준 거리입니다.
 * @property lockedDragSensitivity 방향 고정 스와이프에서 드래그 이동량에 곱해지는 민감도입니다.
 * @property lockedUpToHorizontalSwitchRatio 방향 고정 스와이프에서 위 방향과 좌우 방향 중 하나를 선택할 때 사용하는 비율입니다.
 * @property leftExitDistance 왼쪽 스와이프가 완료된 뒤 현재 위치에서 추가로 이동할 거리입니다.
 * @property rightExitDistance 오른쪽 스와이프가 완료된 뒤 현재 위치에서 추가로 이동할 거리입니다.
 * @property upExitDistance 위쪽 스와이프가 완료된 뒤 현재 위치에서 추가로 이동할 거리입니다.
 * @property maxRotationDegrees 스와이프 진행률에 따라 적용되는 최대 회전 각도입니다.
 * @property minAlpha 스와이프가 완료 단계에 가까워졌을 때 적용되는 최소 투명도입니다.
 * @property fadeStartProgress 투명도 감소가 시작되는 스와이프 진행률입니다.
 * @property hapticProgressThreshold 햅틱 피드백을 발생시킬 최소 스와이프 진행률입니다.
 * @property hapticFeedbackEnabled 스와이프 완료 기준을 넘었을 때 햅틱 피드백을 사용할지 여부입니다.
 * @property resetAnimationSpec 스와이프가 완료되지 않았을 때 원래 위치로 돌아가는 애니메이션 설정입니다.
 * @property exitAnimationSpec 스와이프가 완료되었을 때 종료 위치로 이동하는 애니메이션 설정입니다.
 *
 * @author gunhyung
 */
@Immutable
class SwipeGestureConfig internal constructor(
    val enabledDirections: Set<SwipeDirection>,
    val directionActivationDistance: Dp,
    val swipeThreshold: Dp,
    val dragSensitivity: Float,
    val upToHorizontalSwitchRatio: Float,
    val lockedDirectionActivationDistance: Dp,
    val lockedSwipeThreshold: Dp,
    val lockedDragSensitivity: Float,
    val lockedUpToHorizontalSwitchRatio: Float,
    val leftExitDistance: Dp,
    val rightExitDistance: Dp,
    val upExitDistance: Dp,
    val maxRotationDegrees: Float,
    val minAlpha: Float,
    val fadeStartProgress: Float,
    val hapticProgressThreshold: Float,
    val hapticFeedbackEnabled: Boolean,
    val resetAnimationSpec: AnimationSpec<Float>,
    val exitAnimationSpec: AnimationSpec<Float>,
) {
    companion object {
        /**
         * 카드 UI에서 사용하는 기본 스와이프 제스처 설정입니다.
         *
         * @author gunhyung
         */
        val Default: SwipeGestureConfig =
            SwipeGestureConfig(
                enabledDirections =
                    setOf(
                        SwipeDirection.LEFT,
                        SwipeDirection.RIGHT,
                        SwipeDirection.UP,
                    ),
                directionActivationDistance = 10.dp,
                swipeThreshold = 88.dp,
                dragSensitivity = 1.28f,
                upToHorizontalSwitchRatio = 0.62f,
                lockedDirectionActivationDistance = 10.dp,
                lockedSwipeThreshold = 88.dp,
                lockedDragSensitivity = 1.28f,
                lockedUpToHorizontalSwitchRatio = 0.62f,
                leftExitDistance = (-180).dp,
                rightExitDistance = 180.dp,
                upExitDistance = (-220).dp,
                maxRotationDegrees = 7f,
                minAlpha = 0.2f,
                fadeStartProgress = 0.6f,
                hapticProgressThreshold = 0.28f,
                hapticFeedbackEnabled = true,
                resetAnimationSpec =
                    tween(
                        durationMillis = 120,
                        easing = FastOutSlowInEasing,
                    ),
                exitAnimationSpec =
                    tween(
                        durationMillis = 280,
                        easing = FastOutSlowInEasing,
                    ),
            )
    }
}

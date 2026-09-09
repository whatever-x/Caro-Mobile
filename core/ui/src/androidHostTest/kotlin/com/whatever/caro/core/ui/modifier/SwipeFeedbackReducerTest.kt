package com.whatever.caro.core.ui.modifier

import com.whatever.caro.core.ui.swipe.SwipeDirection
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class SwipeFeedbackReducerTest : FunSpec() {
    init {
        test("direction notification is emitted initially and only when direction changes") {
            val initial =
                SwipeFeedbackMemory().reduce(
                    feedback = feedback(direction = null, progress = 0f),
                    hapticFeedbackEnabled = true,
                )
            val enteredRight =
                initial.nextMemory.reduce(
                    feedback = feedback(direction = SwipeDirection.RIGHT, progress = 0.2f),
                    hapticFeedbackEnabled = true,
                )
            val stayedRight =
                enteredRight.nextMemory.reduce(
                    feedback = feedback(direction = SwipeDirection.RIGHT, progress = 0.4f),
                    hapticFeedbackEnabled = true,
                )

            initial.shouldNotifyDirection shouldBe true
            enteredRight.shouldNotifyDirection shouldBe true
            stayedRight.shouldNotifyDirection shouldBe false
        }

        test("haptic feedback is suppressed until progress resets and then reenters") {
            val firstReached =
                SwipeFeedbackMemory().reduce(
                    feedback = feedback(direction = SwipeDirection.RIGHT, progress = 1f),
                    hapticFeedbackEnabled = true,
                )
            val duplicateReached =
                firstReached.nextMemory.reduce(
                    feedback = feedback(direction = SwipeDirection.RIGHT, progress = 0.9f),
                    hapticFeedbackEnabled = true,
                )
            val between =
                duplicateReached.nextMemory.reduce(
                    feedback = feedback(direction = SwipeDirection.RIGHT, progress = 0.6f),
                    hapticFeedbackEnabled = true,
                )
            val reachedFromBetween =
                between.nextMemory.reduce(
                    feedback = feedback(direction = SwipeDirection.RIGHT, progress = 1f),
                    hapticFeedbackEnabled = true,
                )
            val reset =
                reachedFromBetween.nextMemory.reduce(
                    feedback = feedback(direction = SwipeDirection.RIGHT, progress = 0.3f),
                    hapticFeedbackEnabled = true,
                )
            val reentered =
                reset.nextMemory.reduce(
                    feedback = feedback(direction = SwipeDirection.RIGHT, progress = 1f),
                    hapticFeedbackEnabled = true,
                )

            firstReached.shouldPerformHaptic shouldBe true
            duplicateReached.shouldPerformHaptic shouldBe false
            between.shouldPerformHaptic shouldBe false
            reachedFromBetween.shouldPerformHaptic shouldBe false
            reset.shouldPerformHaptic shouldBe false
            reentered.shouldPerformHaptic shouldBe true
        }

        test("direction switch can trigger haptic feedback without a progress reset") {
            val reachedRight =
                SwipeFeedbackMemory().reduce(
                    feedback = feedback(direction = SwipeDirection.RIGHT, progress = 1f),
                    hapticFeedbackEnabled = true,
                )
            val switchedLeft =
                reachedRight.nextMemory.reduce(
                    feedback = feedback(direction = SwipeDirection.LEFT, progress = 1f),
                    hapticFeedbackEnabled = true,
                )

            switchedLeft.shouldNotifyDirection shouldBe true
            switchedLeft.shouldPerformHaptic shouldBe true
        }

        test("disabled haptics do not consume a future enabled threshold entry") {
            val disabled =
                SwipeFeedbackMemory().reduce(
                    feedback = feedback(direction = SwipeDirection.RIGHT, progress = 1f),
                    hapticFeedbackEnabled = false,
                )
            val enabled =
                disabled.nextMemory.reduce(
                    feedback = feedback(direction = SwipeDirection.RIGHT, progress = 1f),
                    hapticFeedbackEnabled = true,
                )

            disabled.shouldPerformHaptic shouldBe false
            enabled.shouldPerformHaptic shouldBe true
        }
    }
}

private fun feedback(
    direction: SwipeDirection?,
    progress: Float,
): SwipeFeedbackSnapshot =
    SwipeFeedbackSnapshot(
        direction = direction,
        hapticThresholdState = progress.resolveHapticThresholdState(threshold = HAPTIC_THRESHOLD),
    )

private const val HAPTIC_THRESHOLD = 0.8f

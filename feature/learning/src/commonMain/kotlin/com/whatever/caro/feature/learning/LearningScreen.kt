package com.whatever.caro.feature.learning

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import caromobile.core.designsystem.generated.resources.Res
import caromobile.core.designsystem.generated.resources.learning_dialog_submit_error
import caromobile.core.designsystem.generated.resources.learning_front_instruction
import caromobile.core.designsystem.generated.resources.learning_swipe_instruction
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.core.model.learning.LearningMode
import com.whatever.caro.core.model.learning.StudyCard
import com.whatever.caro.core.model.learning.StudyEvaluation
import com.whatever.caro.core.model.learning.StudyRating
import com.whatever.caro.core.ui.modifier.swipeGesture
import com.whatever.caro.core.ui.swipe.SwipeDirection
import com.whatever.caro.core.ui.swipe.SwipeGestureConfig
import com.whatever.caro.core.ui.swipe.rememberSwipeGestureState
import com.whatever.caro.feature.learning.components.LearningCard
import com.whatever.caro.feature.learning.components.LearningCompletion
import com.whatever.caro.feature.learning.components.LearningErrorDialog
import com.whatever.caro.feature.learning.components.LearningEvaluationControls
import com.whatever.caro.feature.learning.components.LearningStopDialog
import com.whatever.caro.feature.learning.components.LearningTopBar
import com.whatever.caro.feature.learning.mapper.toRating
import com.whatever.caro.feature.learning.mapper.toSwipeDirection
import com.whatever.caro.feature.learning.mvi.LearningIntent
import com.whatever.caro.feature.learning.mvi.LearningState
import kotlinx.coroutines.withTimeoutOrNull
import org.jetbrains.compose.resources.stringResource

@Composable
fun LearningScreen(
    state: LearningState,
    onIntent: (LearningIntent) -> Unit,
) {
    when {
        state.isLoading -> {
            LoadingContent()
        }

        state.isCompleted -> {
            val ratingCounts = state.ratingCounts
            LearningCompletion(
                total = state.totalCount,
                easy = ratingCounts?.easy ?: state.evaluations.count { it.rating == StudyRating.EASY },
                fair = ratingCounts?.fair ?: state.evaluations.count { it.rating == StudyRating.FAIR },
                again = ratingCounts?.again ?: state.evaluations.count { it.rating == StudyRating.AGAIN },
                onClickBackToHome = { onIntent(LearningIntent.ClickNavigateToHome) },
            )
        }

        else -> {
            LearningContent(state, onIntent)
        }
    }

    if (state.showStopDialog) {
        LearningStopDialog(
            evaluatedCount = state.progress.takeIf { state.mode == LearningMode.DAILY },
            totalCount = state.totalCount,
            onDismiss = { onIntent(LearningIntent.DismissStop) },
            onConfirm = { onIntent(LearningIntent.ConfirmStop) },
        )
    }

    if (state.isShowErrorDialog) {
        LearningErrorDialog(
            message = state.errorMessage ?: stringResource(Res.string.learning_dialog_submit_error),
            onConfirm = { onIntent(LearningIntent.ConfirmError) },
        )
    }
}

@Composable
private fun LearningContent(
    state: LearningState,
    onIntent: (LearningIntent) -> Unit,
) {
    val card = state.currentCard ?: return
    Column(Modifier.fillMaxSize().background(CaroTheme.color.background.primary)) {
        LearningTopBar(
            current = state.progress + 1,
            total = state.totalCount,
            onBack = { onIntent(LearningIntent.RequestStop) },
        )
        LinearProgressIndicator(
            progress = { ((state.progress + 1).toFloat() / state.totalCount.coerceAtLeast(1)).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(LearningProgressHeight),
            color = CaroTheme.color.border.progress,
            trackColor = CaroTheme.color.surface.progress,
        )
        key(card.id) {
            val swipeState = rememberSwipeGestureState()
            var pendingEvaluation by remember { mutableStateOf(PendingEvaluationState()) }
            val pendingRating = pendingEvaluation.rating
            val selectedDirection = pendingRating?.toSwipeDirection() ?: swipeState.currentDirection
            val interactionAvailability =
                learningInteractionAvailability(
                    isSubmitting = state.isSubmitting,
                    hasPendingRating = pendingEvaluation.hasPendingRating,
                )

            LaunchedEffect(state.isSubmitting, state.isShowErrorDialog) {
                pendingEvaluation =
                    pendingEvaluation.onSubmissionStateChanged(
                        isSubmitting = state.isSubmitting,
                        hasSubmissionError = state.isShowErrorDialog,
                    )
            }

            LaunchedEffect(pendingRating) {
                val rating = pendingRating ?: return@LaunchedEffect
                runEvaluationTransition(
                    rating = rating,
                    animate = {
                        swipeState.animateTo(
                            targetOffset = rating.toSwipeDirection().exitOffset,
                            animationSpec = SwipeGestureConfig.Default.exitAnimationSpec,
                        )
                    },
                    onEvaluate = { onIntent(LearningIntent.Evaluate(it)) },
                )
            }

            Box(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(CaroTheme.spacing.xl),
                contentAlignment = Alignment.Center,
            ) {
                LearningCard(
                    frontText = card.front,
                    backText = card.back,
                    isFlipped = state.isFlipped,
                    swipeColorArgb = selectedDirection.feedbackColorArgb(),
                    swipeProgress = if (pendingRating != null) 1f else swipeState.progress,
                    onFlip = { onIntent(LearningIntent.FlipCard) },
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .swipeGesture(
                                state = swipeState,
                                enabled = interactionAvailability.swipeEnabled,
                                onSwiped = { direction -> onIntent(LearningIntent.Evaluate(direction.toRating())) },
                            ),
                )
            }
            LearningEvaluationControls(
                enabled = interactionAvailability.evaluationEnabled,
                onEasy = { pendingEvaluation = pendingEvaluation.select(StudyRating.EASY) },
                onFair = { pendingEvaluation = pendingEvaluation.select(StudyRating.FAIR) },
                onAgain = { pendingEvaluation = pendingEvaluation.select(StudyRating.AGAIN) },
            )
        }
        Text(
            text =
                stringResource(
                    if (state.isFlipped) Res.string.learning_swipe_instruction else Res.string.learning_front_instruction,
                ),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(LearningInstructionHeight)
                    .padding(top = CaroTheme.spacing.m),
            style = CaroTheme.typography.body2.medium,
            color = CaroTheme.color.text.primary,
            textAlign = TextAlign.Center,
        )
    }
}

internal suspend fun runEvaluationTransition(
    rating: StudyRating,
    animate: suspend () -> Unit,
    onEvaluate: (StudyRating) -> Unit,
    timeoutMillis: Long = EVALUATION_ANIMATION_TIMEOUT_MILLIS,
) {
    withTimeoutOrNull(timeoutMillis) { animate() }
    onEvaluate(rating)
}

@Composable
private fun SwipeDirection?.feedbackColorArgb(): Int =
    when (this) {
        SwipeDirection.LEFT -> {
            CaroTheme.color.surface.review
                .toArgb()
        }

        SwipeDirection.UP -> {
            CaroTheme.color.surface.new
                .toArgb()
        }

        SwipeDirection.RIGHT -> {
            CaroTheme.color.surface.dangerous
                .toArgb()
        }

        null -> {
            0x00000000
        }
    }

private val SwipeDirection.exitOffset: Offset
    get() =
        when (this) {
            SwipeDirection.LEFT -> Offset(x = -1_200f, y = 0f)
            SwipeDirection.UP -> Offset(x = 0f, y = -1_400f)
            SwipeDirection.RIGHT -> Offset(x = 1_200f, y = 0f)
        }

private const val EVALUATION_ANIMATION_TIMEOUT_MILLIS = 500L

@Composable
private fun LoadingContent() {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(CaroTheme.color.background.primary),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = CaroTheme.color.icon.primary)
    }
}

private val LearningProgressHeight = 5.dp
private val LearningInstructionHeight = 44.dp

private val learningPreviewState =
    LearningState(
        isLoading = false,
        sessionId = 7L,
        studiedBefore = 10,
        totalCount = 40,
        cards = listOf(StudyCard(id = 11L, front = "Apple", back = "사과")),
    )

@Preview(name = "Learning / Front", showBackground = true)
@Composable
private fun LearningScreenFrontPreview() {
    CaroTheme { LearningScreen(state = learningPreviewState, onIntent = {}) }
}

@Preview(name = "Learning / Back", showBackground = true)
@Composable
private fun LearningScreenBackPreview() {
    CaroTheme { LearningScreen(state = learningPreviewState.copy(isFlipped = true), onIntent = {}) }
}

@Preview(name = "Learning / Stop Dialog", showBackground = true)
@Composable
private fun LearningScreenStopDialogPreview() {
    CaroTheme { LearningScreen(state = learningPreviewState.copy(isFlipped = true, showStopDialog = true), onIntent = {}) }
}

@Preview(name = "Learning / Completed", showBackground = true)
@Composable
private fun LearningScreenCompletedPreview() {
    CaroTheme {
        LearningScreen(
            state =
                learningPreviewState.copy(
                    isCompleted = true,
                    evaluations =
                        listOf(
                            StudyEvaluation(1L, StudyRating.EASY, 800),
                            StudyEvaluation(2L, StudyRating.FAIR, 1_200),
                            StudyEvaluation(3L, StudyRating.AGAIN, 1_500),
                        ),
                ),
            onIntent = {},
        )
    }
}

package com.whatever.caro.feature.learning.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import caromobile.core.designsystem.generated.resources.ic_arrow_left_24
import caromobile.core.designsystem.generated.resources.ic_arrow_right_24
import caromobile.core.designsystem.generated.resources.ic_arrow_up_24
import caromobile.core.designsystem.generated.resources.ic_check_24
import caromobile.feature.learning.generated.resources.Res
import caromobile.feature.learning.generated.resources.learning_again
import caromobile.feature.learning.generated.resources.learning_back
import caromobile.feature.learning.generated.resources.learning_complete_title
import caromobile.feature.learning.generated.resources.learning_continue
import caromobile.feature.learning.generated.resources.learning_easy
import caromobile.feature.learning.generated.resources.learning_evaluated_cards
import caromobile.feature.learning.generated.resources.learning_fair
import caromobile.feature.learning.generated.resources.learning_flip_back_hint
import caromobile.feature.learning.generated.resources.learning_flip_hint
import caromobile.feature.learning.generated.resources.learning_home
import caromobile.feature.learning.generated.resources.learning_stop
import caromobile.feature.learning.generated.resources.learning_stop_body
import caromobile.feature.learning.generated.resources.learning_stop_title
import caromobile.feature.learning.generated.resources.learning_total
import com.whatever.caro.core.designsystem.components.CaroDialog
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.core.model.learning.LearningPolicy
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import caromobile.core.designsystem.generated.resources.Res as DesignRes

@Composable
internal fun LearningTopBar(
    current: Int,
    total: Int,
    onBack: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxWidth().height(LearningTopBarHeight),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(DesignRes.drawable.ic_arrow_left_24),
            contentDescription = stringResource(Res.string.learning_back),
            tint = CaroTheme.color.icon.primary,
            modifier =
                Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = CaroTheme.spacing.xl2)
                    .size(LearningIconSize)
                    .clickable(onClick = onBack),
        )
        Box(
            modifier =
                Modifier
                    .background(CaroTheme.color.surface.tertiary, CaroTheme.shape.xxl)
                    .padding(horizontal = CaroTheme.spacing.l, vertical = LearningProgressPillVerticalPadding),
        ) {
            Text(
                text = "$current / $total",
                style = CaroTheme.typography.caption1.regular,
                color = CaroTheme.color.text.secondary,
            )
        }
    }
}

@Composable
internal fun LearningCard(
    frontText: String,
    backText: String,
    isFlipped: Boolean,
    swipeColorArgb: Int,
    swipeProgress: Float,
    onFlip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expandedText by remember(frontText, backText, isFlipped) { mutableStateOf<String?>(null) }
    val feedbackColor = Color(swipeColorArgb)
    val feedbackAlpha =
        (swipeProgress * LEARNING_SWIPE_FEEDBACK_MAX_ALPHA)
            .coerceIn(0f, LEARNING_SWIPE_FEEDBACK_MAX_ALPHA)
    val borderColor = if (swipeProgress > 0f) feedbackColor else CaroTheme.color.border.secondary
    Surface(
        modifier =
            modifier
                .clickable(onClick = onFlip)
                .border(LearningCardBorderWidth, borderColor, CaroTheme.shape.l),
        shape = CaroTheme.shape.l,
        color = CaroTheme.color.surface.primary,
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(feedbackColor.copy(alpha = feedbackAlpha))
                    .padding(CaroTheme.spacing.s),
            contentAlignment = Alignment.Center,
        ) {
            if (isFlipped) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = frontText,
                        style = CaroTheme.typography.body2.semiBold,
                        color = CaroTheme.color.text.disable,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(CaroTheme.spacing.m))
                    Box(
                        Modifier
                            .width(LearningCardDividerWidth)
                            .height(LearningCardDividerHeight)
                            .background(CaroTheme.color.border.brand),
                    )
                    Spacer(Modifier.height(CaroTheme.spacing.l))
                    LearningCardPrimaryText(
                        text = backText,
                        onShowMore = { expandedText = backText },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            } else {
                LearningCardPrimaryText(
                    text = frontText,
                    onShowMore = { expandedText = frontText },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Text(
                text =
                    stringResource(
                        if (isFlipped) Res.string.learning_flip_back_hint else Res.string.learning_flip_hint,
                    ),
                style = CaroTheme.typography.caption1.regular,
                color = CaroTheme.color.text.tertiary,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
    expandedText?.let { text ->
        LearningCardFullTextDialog(
            text = text,
            onDismissRequest = { expandedText = null },
        )
    }
}

@Composable
internal fun LearningEvaluationControls(
    enabled: Boolean,
    selectedIndex: Int?,
    onEasy: () -> Unit,
    onFair: () -> Unit,
    onAgain: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = CaroTheme.spacing.s, vertical = CaroTheme.spacing.m),
        horizontalArrangement = Arrangement.spacedBy(CaroTheme.spacing.xs),
    ) {
        EvaluationButton(
            label = Res.string.learning_easy,
            icon = DesignRes.drawable.ic_arrow_left_24,
            color = if (selectedIndex == 0) CaroTheme.color.button.pressed.easy else CaroTheme.color.surface.brand,
            selected = selectedIndex == 0,
            enabled = enabled,
            onClick = onEasy,
            modifier = Modifier.weight(1f),
        )
        EvaluationButton(
            label = Res.string.learning_fair,
            icon = DesignRes.drawable.ic_arrow_up_24,
            color = if (selectedIndex == 1) CaroTheme.color.button.pressed.fair else CaroTheme.color.surface.inverse,
            selected = selectedIndex == 1,
            enabled = enabled,
            onClick = onFair,
            modifier = Modifier.weight(1f),
        )
        EvaluationButton(
            label = Res.string.learning_again,
            icon = DesignRes.drawable.ic_arrow_right_24,
            color = if (selectedIndex == 2) CaroTheme.color.button.pressed.hard else CaroTheme.color.surface.accent,
            selected = selectedIndex == 2,
            enabled = enabled,
            onClick = onAgain,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun EvaluationButton(
    label: StringResource,
    icon: DrawableResource,
    color: Color,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .height(LearningEvaluationButtonHeight)
                .clip(CaroTheme.shape.m)
                .background(color)
                .clickable(enabled = enabled, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = if (selected) CaroTheme.color.icon.disable else CaroTheme.color.icon.inverse,
            modifier = Modifier.size(LearningIconSize),
        )
        Spacer(Modifier.height(CaroTheme.spacing.xs))
        Text(
            text = stringResource(label),
            style = CaroTheme.typography.body2.semiBold,
            color = if (selected) CaroTheme.color.text.disable else CaroTheme.color.text.inverse,
        )
    }
}

@Composable
internal fun LearningStopDialog(
    evaluatedCount: Int?,
    totalCount: Int,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    CaroDialog(
        onDismissRequest = onDismiss,
        content = {
            Column(Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(Res.string.learning_stop_title),
                    style = CaroTheme.typography.heading2,
                    color = CaroTheme.color.text.primary,
                )
                Spacer(Modifier.height(CaroTheme.spacing.s))
                Text(
                    text = stringResource(Res.string.learning_stop_body),
                    style = CaroTheme.typography.body3,
                    color = CaroTheme.color.text.secondary,
                )
                if (evaluatedCount != null) {
                    Spacer(Modifier.height(CaroTheme.spacing.m))
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clip(CaroTheme.shape.m)
                                .background(CaroTheme.color.surface.info)
                                .padding(
                                    horizontal = CaroTheme.spacing.l,
                                    vertical = CaroTheme.spacing.s,
                                ),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(Res.string.learning_evaluated_cards),
                            style = CaroTheme.typography.body2.semiBold,
                            color = CaroTheme.color.text.secondary,
                        )
                        Text(
                            text = "$evaluatedCount / $totalCount",
                            style = CaroTheme.typography.body2.semiBold,
                            color = CaroTheme.color.text.info,
                        )
                    }
                }
                Spacer(Modifier.height(CaroTheme.spacing.m))
                DialogAction(
                    text = stringResource(Res.string.learning_continue),
                    backgroundColor = CaroTheme.color.surface.brand,
                    textColor = CaroTheme.color.text.inverse,
                    onClick = onDismiss,
                )
                Spacer(Modifier.height(CaroTheme.spacing.s))
                DialogAction(
                    text = stringResource(Res.string.learning_stop),
                    backgroundColor = CaroTheme.color.surface.primary,
                    textColor = CaroTheme.color.text.brand,
                    borderColor = CaroTheme.color.border.primary,
                    onClick = onConfirm,
                )
            }
        },
    )
}

@Composable
private fun DialogAction(
    text: String,
    backgroundColor: Color,
    textColor: Color,
    borderColor: Color? = null,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(LearningDialogActionHeight)
                .clip(CaroTheme.shape.xxl)
                .background(backgroundColor)
                .then(
                    if (borderColor != null) {
                        Modifier.border(
                            width = LearningDialogBorderWidth,
                            color = borderColor,
                            shape = CaroTheme.shape.xxl,
                        )
                    } else {
                        Modifier
                    },
                ).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, style = CaroTheme.typography.caption1.regular, color = textColor)
    }
}

@Composable
internal fun LearningCompletion(
    total: Int,
    easy: Int,
    fair: Int,
    again: Int,
    onClose: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().background(CaroTheme.color.background.primary),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = CaroTheme.spacing.xl2),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(LearningCompletionAssetSize)
                        .background(CaroTheme.color.surface.info, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(DesignRes.drawable.ic_check_24),
                    contentDescription = null,
                    tint = CaroTheme.color.icon.primary,
                    modifier = Modifier.size(LearningCompletionCheckIconSize),
                )
            }
            Spacer(Modifier.height(CaroTheme.spacing.xl3))
            Text(
                text = stringResource(Res.string.learning_complete_title),
                style = CaroTheme.typography.heading1,
                color = CaroTheme.color.text.primary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(CaroTheme.spacing.xl3))
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(LearningCompletionStatsHeight)
                        .background(CaroTheme.color.surface.primary, CaroTheme.shape.m)
                        .border(
                            LearningCompletionBorderWidth,
                            CaroTheme.color.border.secondary,
                            CaroTheme.shape.m,
                        ).padding(
                            horizontal = CaroTheme.spacing.xl2,
                            vertical = CaroTheme.spacing.xl,
                        ),
                horizontalArrangement = Arrangement.spacedBy(CaroTheme.spacing.s),
            ) {
                CompletionStat(
                    count = total,
                    label = Res.string.learning_total,
                    backgroundColor = Color.Transparent,
                    contentColor = CaroTheme.color.text.primary,
                    isTotal = true,
                    modifier = Modifier.weight(1f),
                )
                CompletionStat(
                    count = easy,
                    label = Res.string.learning_easy,
                    backgroundColor = CaroTheme.color.surface.brand,
                    contentColor = CaroTheme.color.text.inverse,
                    modifier = Modifier.weight(1f),
                )
                CompletionStat(
                    count = fair,
                    label = Res.string.learning_fair,
                    backgroundColor = CaroTheme.color.surface.inverse,
                    contentColor = CaroTheme.color.text.inverse,
                    modifier = Modifier.weight(1f),
                )
                CompletionStat(
                    count = again,
                    label = Res.string.learning_again,
                    backgroundColor = CaroTheme.color.surface.accent,
                    contentColor = CaroTheme.color.text.inverse,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(
                        brush =
                            Brush.verticalGradient(
                                colors =
                                    listOf(
                                        CaroTheme.color.gradient.tertiaryStart,
                                        CaroTheme.color.gradient.tertiaryEnd,
                                    ),
                            ),
                    ).padding(
                        start = CaroTheme.spacing.xl2_2,
                        end = CaroTheme.spacing.xl2_2,
                        bottom = CaroTheme.spacing.l,
                    ),
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(CaroTheme.shape.xxl)
                        .background(CaroTheme.color.surface.brand)
                        .clickable(onClick = onClose)
                        .padding(
                            horizontal = CaroTheme.spacing.xl,
                            vertical = CaroTheme.spacing.l,
                        ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(Res.string.learning_home),
                    style = CaroTheme.typography.label1.regular,
                    color = CaroTheme.color.text.inverse,
                )
            }
        }
    }
}

@Composable
private fun CompletionStat(
    count: Int,
    label: StringResource,
    backgroundColor: Color,
    contentColor: Color,
    isTotal: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .clip(if (isTotal) CaroTheme.shape.m else CaroTheme.shape.l)
                .background(backgroundColor)
                .padding(CaroTheme.spacing.m),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = count.toString(),
            style = if (isTotal) CaroTheme.typography.display else CaroTheme.typography.body2.semiBold,
            color = contentColor,
        )
        Spacer(Modifier.height(CaroTheme.spacing.xs))
        Text(
            text = stringResource(label),
            style = CaroTheme.typography.body2.semiBold,
            color = contentColor,
        )
    }
}

private val LearningTopBarHeight = 56.dp
private val LearningIconSize = 24.dp
private val LearningProgressPillVerticalPadding = 6.dp
private const val LEARNING_SWIPE_FEEDBACK_MAX_ALPHA = 0.12f
private val LearningCardBorderWidth = 1.dp
private val LearningCardDividerWidth = 80.dp
private val LearningCardDividerHeight = 1.dp
private val LearningEvaluationButtonHeight = 68.dp
private val LearningDialogActionHeight = 38.dp
private val LearningDialogBorderWidth = 1.dp
private val LearningCompletionAssetSize = 70.dp
private val LearningCompletionCheckIconSize = 36.dp
private val LearningCompletionStatsHeight = 100.dp
private val LearningCompletionBorderWidth = 1.dp

@Preview(name = "Learning Card / Front", showBackground = true)
@Composable
private fun LearningCardPreview() {
    CaroTheme {
        Box(Modifier.size(width = 362.dp, height = 568.dp).padding(8.dp)) {
            LearningCard(
                frontText = "Apple",
                backText = "사과",
                isFlipped = false,
                swipeColorArgb = 0,
                swipeProgress = 0f,
                onFlip = {},
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Preview(name = "Learning Card / Back", showBackground = true)
@Composable
private fun LearningCardBackPreview() {
    CaroTheme {
        Box(Modifier.size(width = 362.dp, height = 568.dp).padding(8.dp)) {
            LearningCard(
                frontText = "Apple",
                backText = "사과",
                isFlipped = true,
                swipeColorArgb =
                    CaroTheme.color.button.surface.fair
                        .toArgb(),
                swipeProgress = 1f,
                onFlip = {},
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Preview(name = "Learning Evaluation", showBackground = true)
@Composable
private fun LearningEvaluationPreview() {
    CaroTheme {
        LearningEvaluationControls(
            enabled = true,
            selectedIndex = 0,
            onEasy = {},
            onFair = {},
            onAgain = {},
        )
    }
}

@Preview(name = "Daily Learning Stop Dialog", showBackground = true)
@Composable
private fun DailyLearningStopDialogPreview() {
    CaroTheme {
        LearningStopDialog(
            evaluatedCount = 10,
            totalCount = 40,
            onDismiss = {},
            onConfirm = {},
        )
    }
}

@Preview(name = "All Learning Stop Dialog", showBackground = true)
@Composable
private fun AllLearningStopDialogPreview() {
    CaroTheme {
        LearningStopDialog(
            evaluatedCount = null,
            totalCount = 40,
            onDismiss = {},
            onConfirm = {},
        )
    }
}

@Preview(name = "Learning Completion", showBackground = true)
@Composable
private fun LearningCompletionPreview() {
    CaroTheme {
        LearningCompletion(total = 40, easy = 23, fair = 12, again = 5, onClose = {})
    }
}

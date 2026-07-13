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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.style.TextAlign
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
        modifier = Modifier.fillMaxWidth().height(56.dp),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(DesignRes.drawable.ic_arrow_left_24),
            contentDescription = stringResource(Res.string.learning_back),
            tint = CaroTheme.color.icon.primary,
            modifier =
                Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 24.dp)
                    .size(24.dp)
                    .clickable(onClick = onBack),
        )
        Box(
            modifier =
                Modifier
                    .background(CaroTheme.color.surface.info, CaroTheme.shape.xl)
                    .padding(horizontal = 16.dp, vertical = 6.dp),
        ) {
            Text(
                text = "$current / $total",
                style = CaroTheme.typography.caption2.regular,
                color = CaroTheme.color.text.tertiary,
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
    val feedbackColor = Color(swipeColorArgb)
    val feedbackAlpha = (swipeProgress * 0.12f).coerceIn(0f, 0.12f)
    val borderColor = if (swipeProgress > 0f) feedbackColor else CaroTheme.color.border.primary
    Surface(
        modifier = modifier.clickable(onClick = onFlip).border(1.dp, borderColor, CaroTheme.shape.l),
        shape = CaroTheme.shape.l,
        color = CaroTheme.color.surface.primary,
    ) {
        Box(
            modifier = Modifier.fillMaxSize().background(feedbackColor.copy(alpha = feedbackAlpha)).padding(8.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (isFlipped) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = frontText,
                        style = CaroTheme.typography.body2.semiBold,
                        color = CaroTheme.color.text.disable,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(12.dp))
                    Box(Modifier.width(80.dp).height(1.dp).background(CaroTheme.color.border.brand))
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = backText,
                        style = CaroTheme.typography.heading1,
                        color = CaroTheme.color.text.primary,
                        textAlign = TextAlign.Center,
                    )
                }
            } else {
                Text(
                    text = frontText,
                    style = CaroTheme.typography.heading1,
                    color = CaroTheme.color.text.primary,
                    textAlign = TextAlign.Center,
                )
            }
            Text(
                text =
                    stringResource(
                        if (isFlipped) Res.string.learning_flip_back_hint else Res.string.learning_flip_hint,
                    ),
                style = CaroTheme.typography.caption2.regular,
                color = CaroTheme.color.text.disable,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 4.dp),
            )
        }
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
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        EvaluationButton(
            label = Res.string.learning_easy,
            icon = DesignRes.drawable.ic_arrow_left_24,
            color = if (selectedIndex == 0) CaroTheme.color.button.pressed.easy else CaroTheme.color.button.surface.easy,
            enabled = enabled,
            onClick = onEasy,
            modifier = Modifier.weight(1f),
        )
        EvaluationButton(
            label = Res.string.learning_fair,
            icon = DesignRes.drawable.ic_arrow_up_24,
            color = if (selectedIndex == 1) CaroTheme.color.button.pressed.fair else CaroTheme.color.button.surface.fair,
            enabled = enabled,
            onClick = onFair,
            modifier = Modifier.weight(1f),
        )
        EvaluationButton(
            label = Res.string.learning_again,
            icon = DesignRes.drawable.ic_arrow_right_24,
            color = if (selectedIndex == 2) CaroTheme.color.button.pressed.hard else CaroTheme.color.button.surface.hard,
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
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .height(69.dp)
                .clip(CaroTheme.shape.m)
                .background(if (enabled) color else color.copy(alpha = 0.38f))
                .clickable(enabled = enabled, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = CaroTheme.color.icon.inverse,
            modifier = Modifier.size(24.dp),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(label),
            style = CaroTheme.typography.body3,
            color = CaroTheme.color.text.inverse,
        )
    }
}

@Composable
internal fun LearningStopDialog(
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
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(Res.string.learning_stop_body),
                    style = CaroTheme.typography.body3,
                    color = CaroTheme.color.text.tertiary,
                )
                Spacer(Modifier.height(16.dp))
                DialogAction(
                    text = stringResource(Res.string.learning_continue),
                    backgroundColor = CaroTheme.color.surface.brand,
                    textColor = CaroTheme.color.text.inverse,
                    onClick = onDismiss,
                )
                Spacer(Modifier.height(8.dp))
                DialogAction(
                    text = stringResource(Res.string.learning_stop),
                    backgroundColor = CaroTheme.color.surface.error,
                    textColor = CaroTheme.color.text.error,
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
    onClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(38.dp)
                .clip(CaroTheme.shape.xl)
                .background(backgroundColor)
                .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, style = CaroTheme.typography.caption2.regular, color = textColor)
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
        modifier = Modifier.fillMaxSize().background(CaroTheme.color.background.primary).padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))
        Box(
            modifier = Modifier.size(58.dp).background(CaroTheme.color.surface.info, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(DesignRes.drawable.ic_check_24),
                contentDescription = null,
                tint = CaroTheme.color.icon.primary,
                modifier = Modifier.size(36.dp),
            )
        }
        Spacer(Modifier.height(36.dp))
        Text(
            text = stringResource(Res.string.learning_complete_title),
            style = CaroTheme.typography.heading1,
            color = CaroTheme.color.text.primary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(32.dp))
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .background(CaroTheme.color.surface.primary, CaroTheme.shape.m)
                    .border(1.dp, CaroTheme.color.border.primary, CaroTheme.shape.m)
                    .padding(horizontal = 8.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CompletionStat(total, Res.string.learning_total, Color.Transparent, CaroTheme.color.text.primary, Modifier.weight(1f))
            CompletionStat(
                easy,
                Res.string.learning_easy,
                CaroTheme.color.button.surface.easy,
                CaroTheme.color.text.inverse,
                Modifier.weight(1f),
            )
            CompletionStat(
                fair,
                Res.string.learning_fair,
                CaroTheme.color.button.surface.fair,
                CaroTheme.color.text.inverse,
                Modifier.weight(1f),
            )
            CompletionStat(
                again,
                Res.string.learning_again,
                CaroTheme.color.button.surface.hard,
                CaroTheme.color.text.inverse,
                Modifier.weight(1f),
            )
        }
        Spacer(Modifier.weight(1f))
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(CaroTheme.shape.xl)
                    .background(CaroTheme.color.surface.brand)
                    .clickable(onClick = onClose),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(Res.string.learning_home),
                style = CaroTheme.typography.body2.semiBold,
                color = CaroTheme.color.text.inverse,
            )
        }
        Spacer(Modifier.height(50.dp))
    }
}

@Composable
private fun CompletionStat(
    count: Int,
    label: StringResource,
    backgroundColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().clip(CaroTheme.shape.m).background(backgroundColor),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = count.toString(), style = CaroTheme.typography.heading2, color = contentColor)
        Spacer(Modifier.height(4.dp))
        Text(text = stringResource(label), style = CaroTheme.typography.body3, color = contentColor)
    }
}

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

@Preview(name = "Learning Stop Dialog", showBackground = true)
@Composable
private fun LearningStopDialogPreview() {
    CaroTheme { LearningStopDialog(onDismiss = {}, onConfirm = {}) }
}

@Preview(name = "Learning Completion", showBackground = true)
@Composable
private fun LearningCompletionPreview() {
    CaroTheme {
        LearningCompletion(total = 40, easy = 23, fair = 12, again = 5, onClose = {})
    }
}

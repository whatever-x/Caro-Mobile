package com.whatever.caro.feature.learning.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import caromobile.core.designsystem.generated.resources.Res
import caromobile.core.designsystem.generated.resources.learning_more
import com.whatever.caro.core.designsystem.modifier.noRippleClickable
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.core.model.learning.LearningPolicy
import org.jetbrains.compose.resources.stringResource

internal data class LearningCardTextUiState(
    val text: String,
    val showMore: Boolean,
)

internal fun learningCardTextUiState(
    text: String,
    hasVisualOverflow: Boolean,
): LearningCardTextUiState = LearningCardTextUiState(text = text, showMore = hasVisualOverflow)

@Composable
internal fun LearningCardPrimaryText(
    text: String,
    onShowMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var hasVisualOverflow by remember(text) { mutableStateOf(false) }
    val uiState = learningCardTextUiState(text = text, hasVisualOverflow = hasVisualOverflow)

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = uiState.text,
            modifier = Modifier.fillMaxWidth(),
            style = CaroTheme.typography.display,
            color = CaroTheme.color.text.primary,
            textAlign = TextAlign.Center,
            maxLines = LearningPolicy.LEARNING_CARD_COLLAPSED_MAX_LINES,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { hasVisualOverflow = it.hasVisualOverflow },
        )
        if (uiState.showMore) {
            Spacer(Modifier.height(CaroTheme.spacing.l))
            Box(
                modifier =
                    Modifier
                        .background(CaroTheme.color.surface.tertiary, CaroTheme.shape.xxl)
                        .noRippleClickable(onClick = onShowMore)
                        .padding(
                            horizontal = CaroTheme.spacing.xl,
                            vertical = CaroTheme.spacing.s,
                        ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(Res.string.learning_more),
                    style = CaroTheme.typography.caption1.regular,
                    color = CaroTheme.color.text.brand,
                )
            }
        }
    }
}

@Composable
internal fun LearningCardFullTextDialog(
    text: String,
    onDismissRequest: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        BoxWithConstraints(
            modifier =
                Modifier
                    .fillMaxSize()
                    .pointerInput(onDismissRequest) {
                        detectTapGestures { onDismissRequest() }
                    }.padding(CaroTheme.spacing.l),
            contentAlignment = Alignment.Center,
        ) {
            val dialogWidth = minOf(maxWidth, LearningCardDialogWidth)
            val dialogHeight = minOf(maxHeight, LearningCardDialogHeight)

            Surface(
                modifier =
                    Modifier
                        .width(dialogWidth)
                        .height(dialogHeight)
                        .pointerInput(Unit) { detectTapGestures {} },
                shape = CaroTheme.shape.l,
                color = CaroTheme.color.surface.primary,
                border = BorderStroke(LearningCardDialogBorderWidth, CaroTheme.color.border.secondary),
                shadowElevation = LearningCardDialogElevation,
            ) {
                Text(
                    text = text,
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(
                                horizontal = CaroTheme.spacing.s,
                                vertical = CaroTheme.spacing.xl4,
                            ).verticalScroll(rememberScrollState()),
                    style = CaroTheme.typography.body1,
                    color = CaroTheme.color.text.primary,
                )
            }
        }
    }
}

private val LearningCardDialogWidth = 362.dp
private val LearningCardDialogHeight = 642.dp
private val LearningCardDialogBorderWidth = 1.dp
private val LearningCardDialogElevation = 16.dp

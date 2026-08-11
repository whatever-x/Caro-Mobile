package com.whatever.caro.core.ui.card

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.whatever.caro.core.designsystem.modifier.noRippleClickable
import com.whatever.caro.core.designsystem.themes.CaroTheme

@Composable
fun CaroFlashCard(
    frontText: String,
    backText: String,
    isFlipped: Boolean,
    flipHint: String,
    moreLabel: String,
    onFlip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expandedText by remember(frontText, backText, isFlipped) { mutableStateOf<String?>(null) }
    val rotationY =
        animateFloatAsState(
            targetValue = if (isFlipped) FLASH_CARD_HALF_ROTATION_DEGREES else 0f,
            animationSpec =
                tween(
                    durationMillis = FLASH_CARD_FLIP_DURATION_MILLIS,
                    easing = FastOutSlowInEasing,
                ),
            label = "flash_card_rotation_y",
        )
    val showBack by remember { derivedStateOf { rotationY.value >= FLASH_CARD_FACE_SWAP_DEGREES } }
    val cameraDistance = with(LocalDensity.current) { FlashCardCameraDistance.toPx() }

    Surface(
        modifier =
            modifier
                .graphicsLayer {
                    this.rotationY = rotationY.value
                    this.cameraDistance = cameraDistance
                }.noRippleClickable(
                    onClickLabel = flipHint,
                    role = Role.Button,
                    onClick = onFlip,
                ).border(FlashCardBorderWidth, CaroTheme.color.border.secondary, CaroTheme.shape.l),
        shape = CaroTheme.shape.l,
        color = CaroTheme.color.surface.primary,
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(CaroTheme.spacing.s),
            contentAlignment = Alignment.Center,
        ) {
            FlashCardFace(
                frontText = frontText,
                backText = backText,
                isFlipped = showBack,
                flipHint = flipHint,
                moreLabel = moreLabel,
                onShowMore = { text -> expandedText = text },
                modifier =
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            this.rotationY =
                                if (showBack) {
                                    FLASH_CARD_HALF_ROTATION_DEGREES
                                } else {
                                    0f
                                }
                        },
            )
        }
    }

    expandedText?.let { text ->
        FlashCardFullTextDialog(
            text = text,
            onDismissRequest = { expandedText = null },
        )
    }
}

@Composable
private fun FlashCardFace(
    frontText: String,
    backText: String,
    isFlipped: Boolean,
    flipHint: String,
    moreLabel: String,
    onShowMore: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        if (isFlipped) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = frontText,
                    modifier = Modifier.fillMaxWidth(),
                    style = CaroTheme.typography.body1,
                    color = CaroTheme.color.text.disabled,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(CaroTheme.spacing.m))
                Box(
                    Modifier
                        .width(FlashCardDividerWidth)
                        .height(FlashCardDividerHeight)
                        .background(CaroTheme.color.divider.primary),
                )
                Spacer(Modifier.height(CaroTheme.spacing.l))
                FlashCardPrimaryText(
                    text = backText,
                    moreLabel = moreLabel,
                    onShowMore = { onShowMore(backText) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        } else {
            FlashCardPrimaryText(
                text = frontText,
                moreLabel = moreLabel,
                onShowMore = { onShowMore(frontText) },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        FlipHint(
            text = flipHint,
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = FlashCardFlipHintBottomPadding),
        )
    }
}

@Composable
private fun FlashCardPrimaryText(
    text: String,
    moreLabel: String,
    onShowMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var hasVisualOverflow by remember(text) { mutableStateOf(false) }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = text,
            modifier = Modifier.fillMaxWidth(),
            style = CaroTheme.typography.display,
            color = CaroTheme.color.text.primary,
            textAlign = TextAlign.Center,
            maxLines = FLASH_CARD_COLLAPSED_MAX_LINES,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { hasVisualOverflow = it.hasVisualOverflow },
        )
        if (hasVisualOverflow) {
            Spacer(Modifier.height(CaroTheme.spacing.l))
            Box(
                modifier =
                    Modifier
                        .background(CaroTheme.color.surface.tertiary, CaroTheme.shape.xxl)
                        .noRippleClickable(
                            role = Role.Button,
                            onClick = onShowMore,
                        ).padding(
                            horizontal = CaroTheme.spacing.xl,
                            vertical = CaroTheme.spacing.s,
                        ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = moreLabel,
                    style = CaroTheme.typography.caption1.regular,
                    color = CaroTheme.color.text.brand,
                )
            }
        }
    }
}

@Composable
private fun FlipHint(
    text: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(FlashCardFlipIconSize),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier
                        .width(FlashCardFlipLineWidth)
                        .height(FlashCardFlipLineHeight)
                        .background(CaroTheme.color.icon.tertiary, CaroTheme.shape.xxs),
                )
                Spacer(Modifier.height(FlashCardFlipLineSpacing))
                Box(
                    Modifier
                        .width(FlashCardFlipLineWidth)
                        .height(FlashCardFlipLineHeight)
                        .background(CaroTheme.color.icon.tertiary, CaroTheme.shape.xxs),
                )
            }
        }
        Text(
            text = text,
            style = CaroTheme.typography.caption1.regular,
            color = CaroTheme.color.text.tertiary,
        )
    }
}

@Composable
private fun FlashCardFullTextDialog(
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
            val dialogWidth = minOf(maxWidth, FlashCardDialogWidth)
            val dialogHeight = minOf(maxHeight, FlashCardDialogHeight)

            Surface(
                modifier =
                    Modifier
                        .width(dialogWidth)
                        .height(dialogHeight)
                        .pointerInput(Unit) { detectTapGestures {} },
                shape = CaroTheme.shape.l,
                color = CaroTheme.color.surface.primary,
                border = BorderStroke(FlashCardBorderWidth, CaroTheme.color.border.secondary),
                shadowElevation = FlashCardDialogElevation,
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

private const val FLASH_CARD_COLLAPSED_MAX_LINES = 10
private const val FLASH_CARD_FLIP_DURATION_MILLIS = 280
private const val FLASH_CARD_HALF_ROTATION_DEGREES = 180f
private const val FLASH_CARD_FACE_SWAP_DEGREES = 90f
private val FlashCardBorderWidth = 1.dp
private val FlashCardDividerWidth = 80.dp
private val FlashCardDividerHeight = 1.dp
private val FlashCardFlipIconSize = 16.dp
private val FlashCardFlipLineWidth = 12.dp
private val FlashCardFlipLineHeight = 1.5.dp
private val FlashCardFlipLineSpacing = 3.dp
private val FlashCardFlipHintBottomPadding = 7.dp
private val FlashCardDialogWidth = 362.dp
private val FlashCardDialogHeight = 642.dp
private val FlashCardDialogElevation = 16.dp
private val FlashCardCameraDistance = 12.dp

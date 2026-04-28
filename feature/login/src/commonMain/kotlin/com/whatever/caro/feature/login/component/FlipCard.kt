package com.whatever.caro.feature.login.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import caromobile.core.designsystem.generated.resources.Res
import caromobile.core.designsystem.generated.resources.ic_renew_16
import caromobile.core.designsystem.generated.resources.login_card_button
import caromobile.core.designsystem.generated.resources.login_card_text_description
import caromobile.core.designsystem.generated.resources.login_card_text_title
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.core.ui.noRippleClickable
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun FlipCard(
    modifier: Modifier = Modifier,
    isFlipped: Boolean,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (isFlipped) {
            Column(
                modifier =
                    Modifier
                        .graphicsLayer {
                            this.rotationX = 180f
                        },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(space = CaroTheme.spacing.m),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = stringResource(Res.string.login_card_text_title),
                        style = CaroTheme.typography.heading1,
                        color = CaroTheme.color.text.brand,
                    )
                    HorizontalDivider(
                        modifier =
                            Modifier.size(
                                width = 40.dp,
                                height = 2.dp,
                            ),
                        color = CaroTheme.color.divider.primary,
                    )
                    Text(
                        text = stringResource(Res.string.login_card_text_description),
                        style = CaroTheme.typography.heading1,
                        color = CaroTheme.color.text.secondary,
                    )
                }

                Spacer(modifier = Modifier.size(size = CaroTheme.spacing.xl))
                FlipButton(isFlipped = isFlipped, onClick = onClick)
            }
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(Res.string.login_card_text_title),
                    style = CaroTheme.typography.heading1,
                    color = CaroTheme.color.text.brand,
                )
                Spacer(modifier = Modifier.size(size = CaroTheme.spacing.xl))
                FlipButton(isFlipped = isFlipped, onClick = onClick)
            }
        }
    }
}

@Composable
private fun FlipButton(
    isFlipped: Boolean,
    onClick: () -> Unit,
) {
    val (backgroundColor, iconColor, textColor) =
        if (isFlipped) {
            Triple(
                CaroTheme.color.surface.secondary,
                CaroTheme.color.icon.brand,
                CaroTheme.color.text.brand,
            )
        } else {
            Triple(
                CaroTheme.color.surface.tertiary,
                CaroTheme.color.icon.tertiary,
                CaroTheme.color.text.tertiary,
            )
        }

    Row(
        modifier =
            Modifier
                .clip(shape = CaroTheme.shape.xxl)
                .background(color = backgroundColor)
                .noRippleClickable { onClick() }
                .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(
            painter = painterResource(Res.drawable.ic_renew_16),
            contentDescription = null,
            tint = iconColor,
        )
        Spacer(modifier = Modifier.size(size = 4.dp))
        Text(
            text = stringResource(Res.string.login_card_button),
            style = CaroTheme.typography.caption1,
            color = textColor,
        )
    }
}

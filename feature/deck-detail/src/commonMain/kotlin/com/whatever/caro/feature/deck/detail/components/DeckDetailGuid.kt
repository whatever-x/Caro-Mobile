package com.whatever.caro.feature.deck.detail.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import caromobile.core.designsystem.generated.resources.Res
import caromobile.core.designsystem.generated.resources.deck_detail_button_empty_guid
import caromobile.core.designsystem.generated.resources.deck_detail_sub_title_empty_guid
import caromobile.core.designsystem.generated.resources.deck_detail_title_empty_guid
import caromobile.core.designsystem.generated.resources.ic_add_16
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.core.ui.modifier.noRippleClickable
import io.github.alexzhirkevich.compottie.Compottie
import io.github.alexzhirkevich.compottie.LottieCompositionSpec
import io.github.alexzhirkevich.compottie.rememberLottieComposition
import io.github.alexzhirkevich.compottie.rememberLottiePainter
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private const val LOTTIE_EMPTY_CARD_PATH = "files/lottie_empty_card.json"

@Composable
internal fun DeckDetailGuid(
    onAddFirstCard: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val composition by rememberLottieComposition {
        LottieCompositionSpec.JsonString(
            Res.readBytes(path = LOTTIE_EMPTY_CARD_PATH).decodeToString(),
        )
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(height = 62.dp),
                painter =
                    rememberLottiePainter(
                        composition = composition,
                        iterations = Compottie.IterateForever,
                    ),
                contentDescription = null,
                contentScale = ContentScale.Fit,
            )

            Text(
                text =
                    stringResource(
                        resource = Res.string.deck_detail_title_empty_guid,
                    ),
                style = CaroTheme.typography.heading2,
                color = CaroTheme.color.text.primary,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(height = CaroTheme.spacing.l))

            Text(
                text =
                    stringResource(
                        resource = Res.string.deck_detail_sub_title_empty_guid,
                    ),
                style = CaroTheme.typography.body2.medium,
                color = CaroTheme.color.text.secondary,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(height = CaroTheme.spacing.l))

            Row( // TODO : CTA 버튼 컴포넌트 교체
                modifier =
                    Modifier
                        .background(
                            color = CaroTheme.color.surface.brand,
                            shape = CaroTheme.shape.xxl,
                        ).clip(shape = CaroTheme.shape.xxl)
                        .clickable(
                            onClick = onAddFirstCard,
                        ).padding(
                            horizontal = CaroTheme.spacing.l,
                            vertical = CaroTheme.spacing.m,
                        ).noRippleClickable(onClick = onAddFirstCard),
                horizontalArrangement =
                    Arrangement.spacedBy(
                        space = CaroTheme.spacing.xs,
                    ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(resource = Res.drawable.ic_add_16),
                    tint = CaroTheme.color.icon.inverse,
                    contentDescription = null,
                )

                Text(
                    text =
                        stringResource(
                            resource = Res.string.deck_detail_button_empty_guid,
                        ),
                    style = CaroTheme.typography.caption1.regular,
                    color = CaroTheme.color.text.inverse,
                )
            }
        }
    }
}

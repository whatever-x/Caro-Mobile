package com.whatever.caro.feature.deck.detail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import caromobile.core.designsystem.generated.resources.Res
import caromobile.core.designsystem.generated.resources.deck_detail_button_card_load_error_retry
import caromobile.core.designsystem.generated.resources.deck_detail_sub_title_card_load_error
import caromobile.core.designsystem.generated.resources.deck_detail_title_card_load_error
import com.whatever.caro.core.designsystem.modifier.noRippleClickable
import com.whatever.caro.core.designsystem.themes.CaroTheme
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun DeckDetailLoadError(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text =
                    stringResource(
                        resource = Res.string.deck_detail_title_card_load_error,
                    ),
                style = CaroTheme.typography.heading2,
                color = CaroTheme.color.text.primary,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(height = CaroTheme.spacing.l))

            Text(
                text =
                    stringResource(
                        resource = Res.string.deck_detail_sub_title_card_load_error,
                    ),
                style = CaroTheme.typography.body2.medium,
                color = CaroTheme.color.text.secondary,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(height = CaroTheme.spacing.l))

            Text( // TODO : CTA 버튼 컴포넌트 교체
                modifier =
                    Modifier
                        .background(
                            color = CaroTheme.color.surface.brand,
                            shape = CaroTheme.shape.xxl,
                        ).clip(shape = CaroTheme.shape.xxl)
                        .noRippleClickable(
                            onClick = onRetry,
                        ).padding(
                            horizontal = CaroTheme.spacing.l,
                            vertical = CaroTheme.spacing.m,
                        ),
                text =
                    stringResource(
                        resource = Res.string.deck_detail_button_card_load_error_retry,
                    ),
                style = CaroTheme.typography.caption1.regular,
                color = CaroTheme.color.text.inverse,
            )
        }
    }
}

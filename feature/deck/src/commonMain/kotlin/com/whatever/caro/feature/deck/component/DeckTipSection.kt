package com.whatever.caro.feature.deck.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import caromobile.core.designsystem.generated.resources.Res
import caromobile.core.designsystem.generated.resources.deck_tip_label
import caromobile.core.designsystem.generated.resources.deck_tip_max_cards
import com.whatever.caro.core.designsystem.themes.CaroTheme
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun DeckTipSection(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(CaroTheme.spacing.s),
    ) {
        Text(
            text = stringResource(Res.string.deck_tip_label),
            style = CaroTheme.typography.caption1.regular,
            color = CaroTheme.color.text.secondary,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(CaroTheme.spacing.s),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(TipDotSize)
                        .clip(CircleShape)
                        .background(CaroTheme.color.text.tertiary),
            )
            Text(
                text = stringResource(Res.string.deck_tip_max_cards),
                style = CaroTheme.typography.caption1.regular,
                color = CaroTheme.color.text.tertiary,
            )
        }
    }
}
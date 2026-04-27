package com.whatever.caro.feature.deck.detail.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.whatever.caro.core.designsystem.themes.CaroTheme

@Composable
internal fun DeckInfo(
    title: String,
    description: String,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    start = CaroTheme.spacing.l,
                    end = CaroTheme.spacing.l,
                    top = CaroTheme.spacing.s,
                    bottom = CaroTheme.spacing.l,
                ),
        verticalArrangement =
            Arrangement.spacedBy(
                space = CaroTheme.spacing.s,
            ),
    ) {
        Text(
            text = title,
            color = CaroTheme.color.text.primary,
            style = CaroTheme.typography.heading1,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        Text(
            text = description,
            color = CaroTheme.color.text.secondary,
            style = CaroTheme.typography.body2.regular,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

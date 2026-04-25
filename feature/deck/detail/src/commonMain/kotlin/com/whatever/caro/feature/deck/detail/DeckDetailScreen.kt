package com.whatever.caro.feature.deck.detail

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.feature.deck.detail.mvi.DeckDetailIntent
import com.whatever.caro.feature.deck.detail.mvi.DeckDetailState

@Composable
internal fun DeckDetailScreen(
    state: DeckDetailState,
    onIntent: (DeckDetailIntent) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(CaroTheme.spacing.m),
    ) {
        Text(
            text = state.screenName,
        )

        Text(
            text = state.deckId.toString(),
        )
    }
}

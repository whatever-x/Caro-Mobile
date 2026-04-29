package com.whatever.caro.feature.deck.detail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import caromobile.core.designsystem.generated.resources.Res
import caromobile.core.designsystem.generated.resources.deck_detail_button_bottom_sheet_created
import caromobile.core.designsystem.generated.resources.deck_detail_button_bottom_sheet_last_frequency
import caromobile.core.designsystem.generated.resources.deck_detail_button_bottom_sheet_last_reviewed
import caromobile.core.designsystem.generated.resources.deck_detail_button_edit
import caromobile.core.designsystem.generated.resources.deck_detail_label_total_count
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.feature.deck.detail.mvi.DeckDetailSortOption
import org.jetbrains.compose.resources.stringResource

internal fun LazyListScope.FilterAndSortStickyHeader(
    deckCardTotal: Int,
    selectedSortOption: DeckDetailSortOption,
    onSortCardList: () -> Unit,
    onEditCardList: () -> Unit,
    modifier: Modifier = Modifier,
) {
    stickyHeader {
        FilterAndSortSection(
            deckCardTotal = deckCardTotal,
            selectedSortOption = selectedSortOption,
            onSortCardList = onSortCardList,
            onEditCardList = onEditCardList,
        )
    }
}

@Composable
internal fun FilterAndSortSection(
    deckCardTotal: Int,
    selectedSortOption: DeckDetailSortOption,
    onSortCardList: () -> Unit,
    onEditCardList: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .background(color = CaroTheme.color.background.primary)
                .padding(top = CaroTheme.spacing.xs),
    ) {
        Spacer(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(color = CaroTheme.color.divider.primary),
        )

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = CaroTheme.spacing.xl,
                        vertical = CaroTheme.spacing.m,
                    ),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(CaroTheme.spacing.m),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text =
                        stringResource(
                            resource = Res.string.deck_detail_label_total_count,
                            deckCardTotal,
                        ),
                    color = CaroTheme.color.text.primary,
                    style = CaroTheme.typography.label1.bold,
                )

                Row(
                    modifier = Modifier.clickable(onClick = onSortCardList),
                    horizontalArrangement = Arrangement.spacedBy(CaroTheme.spacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text =
                            stringResource(
                                when (selectedSortOption) {
                                    DeckDetailSortOption.CREATED -> {
                                        Res.string.deck_detail_button_bottom_sheet_created
                                    }

                                    DeckDetailSortOption.LAST_REVIEWED -> {
                                        Res.string.deck_detail_button_bottom_sheet_last_reviewed
                                    }

                                    DeckDetailSortOption.FREQUENCY -> {
                                        Res.string.deck_detail_button_bottom_sheet_last_frequency
                                    }
                                },
                            ),
                        color = CaroTheme.color.text.secondary,
                        style = CaroTheme.typography.caption1,
                    )

                    SortIcon(
                        modifier =
                            Modifier
                                .width(15.dp)
                                .height(13.5.dp),
                        color = CaroTheme.color.icon.secondary,
                    )
                }
            }

            Text(
                text = stringResource(Res.string.deck_detail_button_edit),
                modifier = Modifier.clickable(onClick = onEditCardList),
                color = CaroTheme.color.text.secondary,
                style = CaroTheme.typography.caption1,
            )
        }
    }
}

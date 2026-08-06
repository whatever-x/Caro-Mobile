package com.whatever.caro.feature.deck.detail.components.lazycolumn

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
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
import caromobile.core.designsystem.generated.resources.ic_arrow_down_16
import com.whatever.caro.core.designsystem.modifier.noRippleClickable
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.feature.deck.detail.mvi.DeckDetailSortOption
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

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
                .background(color = CaroTheme.color.background.primary),
    ) {
        Spacer(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(color = CaroTheme.color.divider.secondary),
        )

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(47.dp)
                    .padding(
                        horizontal = CaroTheme.spacing.xl,
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
                    style = CaroTheme.typography.label1,
                )

                Row(
                    modifier = Modifier.noRippleClickable(onClick = onSortCardList),
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
                        style = CaroTheme.typography.caption1.regular,
                    )

                    Icon(
                        painter = painterResource(Res.drawable.ic_arrow_down_16),
                        contentDescription = null,
                        modifier =
                            Modifier
                                .width(16.dp)
                                .height(16.dp),
                        tint = CaroTheme.color.icon.secondary,
                    )
                }
            }

            Text(
                text = stringResource(Res.string.deck_detail_button_edit),
                modifier = Modifier.noRippleClickable(onClick = onEditCardList),
                color = CaroTheme.color.text.secondary,
                style = CaroTheme.typography.caption1.regular,
            )
        }
    }
}

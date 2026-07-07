package com.whatever.caro.feature.card.delete

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import caromobile.core.designsystem.generated.resources.Res
import caromobile.core.designsystem.generated.resources.card_content_description_back
import caromobile.core.designsystem.generated.resources.card_title_edit
import caromobile.core.designsystem.generated.resources.ic_chevron_left_24
import com.whatever.caro.core.designsystem.components.CaroTopBar
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.core.ui.modifier.noRippleClickable
import com.whatever.caro.feature.card.delete.components.DeleteBottomBar
import com.whatever.caro.feature.card.delete.components.DeleteCardListItem
import com.whatever.caro.feature.card.delete.components.DeleteConfirmDialog
import com.whatever.caro.feature.card.delete.mvi.DeleteCardsIntent
import com.whatever.caro.feature.card.delete.mvi.DeleteCardsState
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private val TopBarIconSize = 24.dp
private val PageHorizontalPadding = 24.dp
private val BottomBarHeight = 84.dp

@Composable
internal fun DeleteCardsScreen(
    state: DeleteCardsState,
    onIntent: (DeleteCardsIntent) -> Unit,
) {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(CaroTheme.color.background.primary),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            CaroTopBar(
                modifier = Modifier.padding(horizontal = PageHorizontalPadding),
                leadingContent = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(CaroTheme.spacing.s),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            modifier =
                                Modifier
                                    .size(TopBarIconSize)
                                    .noRippleClickable { onIntent(DeleteCardsIntent.ClickBack) },
                            painter = painterResource(Res.drawable.ic_chevron_left_24),
                            contentDescription = stringResource(Res.string.card_content_description_back),
                            tint = CaroTheme.color.icon.brand,
                        )
                        Text(
                            text = stringResource(Res.string.card_title_edit),
                            style = CaroTheme.typography.heading2,
                            color = CaroTheme.color.text.primary,
                        )
                    }
                },
            )

            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentPadding =
                    PaddingValues(
                        start = PageHorizontalPadding,
                        end = PageHorizontalPadding,
                        top = CaroTheme.spacing.s,
                        bottom = BottomBarHeight + CaroTheme.spacing.l,
                    ),
                verticalArrangement = Arrangement.spacedBy(CaroTheme.spacing.m),
            ) {
                items(
                    items = state.cards,
                    key = { item -> item.card.id },
                ) { item ->
                    DeleteCardListItem(
                        item = item,
                        onClick = { onIntent(DeleteCardsIntent.ClickCard(item.card.id)) },
                    )
                }
            }
        }

        DeleteBottomBar(
            modifier = Modifier.align(Alignment.BottomCenter),
            enabled = state.isDeleteEnabled,
            onDelete = { onIntent(DeleteCardsIntent.ClickDeleteSelected) },
            onCancel = { onIntent(DeleteCardsIntent.ClickCancel) },
        )
    }

    if (state.isDeleteConfirmDialogVisible) {
        DeleteConfirmDialog(
            selectedCount = state.selectedCount,
            onDelete = { onIntent(DeleteCardsIntent.ClickDeleteConfirm) },
            onCancel = { onIntent(DeleteCardsIntent.ClickDeleteCancel) },
        )
    }
}

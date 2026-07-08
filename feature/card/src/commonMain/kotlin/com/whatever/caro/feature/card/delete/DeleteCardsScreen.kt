package com.whatever.caro.feature.card.delete

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.whatever.caro.core.designsystem.components.CaroTopBar
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.feature.card.delete.components.DeleteBottomBar
import com.whatever.caro.feature.card.delete.components.DeleteCardListItem
import com.whatever.caro.feature.card.delete.components.DeleteCardsTopBarTitle
import com.whatever.caro.feature.card.delete.components.DeleteConfirmDialog
import com.whatever.caro.feature.card.delete.components.SelectAllTextButton
import com.whatever.caro.feature.card.delete.mvi.DeleteCardsIntent
import com.whatever.caro.feature.card.delete.mvi.DeleteCardsState

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
                    DeleteCardsTopBarTitle(
                        onBackClick = { onIntent(DeleteCardsIntent.ClickBack) },
                    )
                },
                trailingContent = {
                    SelectAllTextButton(
                        onClick = { onIntent(DeleteCardsIntent.ClickSelectAll) },
                    )
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

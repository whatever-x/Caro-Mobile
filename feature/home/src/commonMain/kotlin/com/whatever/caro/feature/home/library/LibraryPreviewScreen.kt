package com.whatever.caro.feature.home.library

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import caromobile.core.designsystem.generated.resources.Res
import caromobile.core.designsystem.generated.resources.library_add
import caromobile.core.designsystem.generated.resources.library_add_error_title
import caromobile.core.designsystem.generated.resources.library_adding
import caromobile.core.designsystem.generated.resources.library_cards_preview
import caromobile.core.designsystem.generated.resources.library_confirm
import caromobile.core.designsystem.generated.resources.library_preview_title
import caromobile.core.designsystem.generated.resources.library_retry_body
import com.whatever.caro.core.designsystem.components.CaroDialog
import com.whatever.caro.core.designsystem.components.CaroDialogButton
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.feature.home.library.mvi.LibraryPreviewIntent
import com.whatever.caro.feature.home.library.mvi.LibraryPreviewState
import org.jetbrains.compose.resources.stringResource

private val LibraryAddButtonHeight = 50.dp
private val LibraryAddBottomSpacing = 30.dp

@Composable
internal fun LibraryPreviewScreen(
    state: LibraryPreviewState,
    onIntent: (LibraryPreviewIntent) -> Unit,
) {
    val scrollState = rememberLazyListState()
    Column(Modifier.fillMaxSize().background(CaroTheme.color.background.primary)) {
        LibraryTopBar(stringResource(Res.string.library_preview_title)) { onIntent(LibraryPreviewIntent.Back) }
        val detail = state.detail
        when {
            state.loading -> {
                LibraryLoading()
            }

            state.failure != null -> {
                LibraryFailureContent(
                    state.failure,
                    true,
                    { onIntent(LibraryPreviewIntent.Retry) },
                    { onIntent(LibraryPreviewIntent.Back) },
                    { onIntent(LibraryPreviewIntent.Login) },
                    { onIntent(LibraryPreviewIntent.Home) },
                )
            }

            detail != null -> {
                LazyColumn(
                    Modifier.weight(1f),
                    state = scrollState,
                    contentPadding = PaddingValues(horizontal = CaroTheme.spacing.xl2, vertical = CaroTheme.spacing.xl),
                    verticalArrangement = Arrangement.spacedBy(CaroTheme.spacing.l),
                ) {
                    item("summary") {
                        Column(verticalArrangement = Arrangement.spacedBy(CaroTheme.spacing.m)) {
                            LibraryDeckHeading(detail.deck, preview = true)
                            LibraryCardCount(detail.deck.cardCount)
                        }
                        Spacer(Modifier.height(CaroTheme.spacing.l))
                    }
                    item("heading") {
                        Text(
                            stringResource(Res.string.library_cards_preview),
                            style = CaroTheme.typography.heading2,
                            color = CaroTheme.color.text.primary,
                        )
                    }
                    items(detail.cards, key = { it.id }) { card ->
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .background(CaroTheme.color.surface.primary, CaroTheme.shape.m)
                                .border(1.dp, CaroTheme.color.border.secondary, CaroTheme.shape.m)
                                .padding(horizontal = CaroTheme.spacing.xl2, vertical = CaroTheme.spacing.l),
                            verticalArrangement = Arrangement.spacedBy(CaroTheme.spacing.s),
                        ) {
                            Text(card.front, style = CaroTheme.typography.heading2, color = CaroTheme.color.text.primary)
                            Text(card.back, style = CaroTheme.typography.body2.medium, color = CaroTheme.color.text.secondary)
                        }
                    }
                }
                Box(
                    Modifier.fillMaxWidth().padding(
                        start = CaroTheme.spacing.xl2,
                        end = CaroTheme.spacing.xl2,
                        top = CaroTheme.spacing.l,
                        bottom = LibraryAddBottomSpacing,
                    ),
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(LibraryAddButtonHeight)
                            .clip(CaroTheme.shape.m)
                            .background(if (state.adding) CaroTheme.color.surface.disabled else CaroTheme.color.surface.brand)
                            .clickable(enabled = !state.adding, role = Role.Button) { onIntent(LibraryPreviewIntent.Add) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            stringResource(if (state.adding) Res.string.library_adding else Res.string.library_add),
                            style = CaroTheme.typography.heading2,
                            color = CaroTheme.color.text.inverse,
                        )
                    }
                }
            }
        }
    }
    if (state.showAddError) {
        CaroDialog(
            modifier = Modifier.padding(horizontal = CaroTheme.spacing.xl2),
            onDismissRequest = { onIntent(LibraryPreviewIntent.DismissError) },
            title = {
                Text(
                    stringResource(Res.string.library_add_error_title),
                    Modifier.fillMaxWidth(),
                    style = CaroTheme.typography.heading2,
                    color = CaroTheme.color.text.primary,
                )
            },
            buttons = {
                Spacer(Modifier.height(CaroTheme.spacing.xl))
                CaroDialogButton(
                    stringResource(Res.string.library_confirm),
                    CaroTheme.color.surface.brand,
                    CaroTheme.color.text.inverse,
                    { onIntent(LibraryPreviewIntent.DismissError) },
                    Modifier.fillMaxWidth(),
                )
            },
        ) {
            Spacer(Modifier.height(CaroTheme.spacing.s))
            Text(
                stringResource(Res.string.library_retry_body),
                Modifier.fillMaxWidth(),
                style = CaroTheme.typography.body2.medium,
                color = CaroTheme.color.text.secondary,
            )
        }
    }
}

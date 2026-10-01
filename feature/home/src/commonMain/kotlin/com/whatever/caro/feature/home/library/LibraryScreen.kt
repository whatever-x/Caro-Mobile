package com.whatever.caro.feature.home.library

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import caromobile.core.designsystem.generated.resources.Res
import caromobile.core.designsystem.generated.resources.ic_arrow_left_24
import caromobile.core.designsystem.generated.resources.library_back
import caromobile.core.designsystem.generated.resources.library_back_list
import caromobile.core.designsystem.generated.resources.library_card_count
import caromobile.core.designsystem.generated.resources.library_check_again
import caromobile.core.designsystem.generated.resources.library_empty_body
import caromobile.core.designsystem.generated.resources.library_empty_title
import caromobile.core.designsystem.generated.resources.library_forbidden_body
import caromobile.core.designsystem.generated.resources.library_forbidden_title
import caromobile.core.designsystem.generated.resources.library_free
import caromobile.core.designsystem.generated.resources.library_home
import caromobile.core.designsystem.generated.resources.library_load_error
import caromobile.core.designsystem.generated.resources.library_login
import caromobile.core.designsystem.generated.resources.library_login_body
import caromobile.core.designsystem.generated.resources.library_login_title
import caromobile.core.designsystem.generated.resources.library_preview_error
import caromobile.core.designsystem.generated.resources.library_refresh_error
import caromobile.core.designsystem.generated.resources.library_retry
import caromobile.core.designsystem.generated.resources.library_retry_body
import caromobile.core.designsystem.generated.resources.library_title
import caromobile.core.designsystem.generated.resources.library_unavailable_body
import caromobile.core.designsystem.generated.resources.library_unavailable_title
import com.whatever.caro.core.designsystem.components.CaroTopBar
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.core.model.library.LibraryDeck
import com.whatever.caro.core.util.NumberFormatter.formatWithComma
import com.whatever.caro.feature.home.library.mvi.LibraryFailure
import com.whatever.caro.feature.home.library.mvi.LibraryIntent
import com.whatever.caro.feature.home.library.mvi.LibraryState
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LibraryScreen(
    state: LibraryState,
    onIntent: (LibraryIntent) -> Unit,
) {
    Column(Modifier.fillMaxSize().background(CaroTheme.color.background.primary)) {
        LibraryTopBar(stringResource(Res.string.library_title)) { onIntent(LibraryIntent.Back) }
        when {
            state.loading && !state.loaded -> {
                LibraryLoading()
            }

            state.failure != null && (!state.loaded || state.failure != LibraryFailure.NETWORK) -> {
                LibraryFailureContent(
                    state.failure,
                    false,
                    { onIntent(LibraryIntent.Retry) },
                    { onIntent(LibraryIntent.Back) },
                    { onIntent(LibraryIntent.Login) },
                    { onIntent(LibraryIntent.Home) },
                )
            }

            state.loaded && state.decks.isEmpty() && state.failure == null -> {
                LibraryMessage(
                    stringResource(Res.string.library_empty_title),
                    stringResource(Res.string.library_empty_body),
                    stringResource(Res.string.library_check_again),
                    { onIntent(LibraryIntent.Retry) },
                )
            }

            else -> {
                if (state.failure != null) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = CaroTheme.spacing.xl2, vertical = CaroTheme.spacing.m),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            stringResource(Res.string.library_refresh_error),
                            Modifier.weight(1f),
                            style = CaroTheme.typography.body2.medium,
                            color = CaroTheme.color.text.secondary,
                        )
                        Text(
                            stringResource(Res.string.library_retry),
                            Modifier.padding(start = CaroTheme.spacing.s).clickable { onIntent(LibraryIntent.Retry) },
                            style = CaroTheme.typography.body2.semiBold,
                            color = CaroTheme.color.text.brand,
                        )
                    }
                }
                PullToRefreshBox(
                    isRefreshing = state.loading,
                    onRefresh = { onIntent(LibraryIntent.Retry) },
                    modifier = Modifier.weight(1f),
                ) {
                    if (state.decks.isEmpty()) {
                        LibraryMessage(
                            stringResource(Res.string.library_empty_title),
                            stringResource(Res.string.library_empty_body),
                            stringResource(Res.string.library_check_again),
                            { onIntent(LibraryIntent.Retry) },
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            state = rememberLazyListState(),
                            contentPadding = PaddingValues(CaroTheme.spacing.xl2),
                            verticalArrangement = Arrangement.spacedBy(CaroTheme.spacing.m),
                        ) {
                            items(state.decks, key = { it.id }) { deck ->
                                LibraryProvidedCard(deck) { onIntent(LibraryIntent.Open(deck.id)) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun LibraryTopBar(
    title: String,
    onBack: () -> Unit,
) {
    CaroTopBar(
        modifier = Modifier.background(CaroTheme.color.background.brand).padding(horizontal = CaroTheme.spacing.xl2),
        leadingContent = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(CaroTheme.spacing.s)) {
                Icon(
                    painterResource(Res.drawable.ic_arrow_left_24),
                    stringResource(Res.string.library_back),
                    Modifier.size(CaroTheme.spacing.xl2).clickable(role = Role.Button, onClick = onBack),
                    tint = CaroTheme.color.icon.inverse,
                )
                Text(title, style = CaroTheme.typography.heading2, color = CaroTheme.color.text.inverse)
            }
        },
    )
}

@Composable
internal fun LibraryProvidedCard(
    deck: LibraryDeck,
    onClick: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(CaroTheme.shape.xl)
            .clickable(role = Role.Button, onClick = onClick)
            .background(CaroTheme.color.surface.primary)
            .border(1.dp, CaroTheme.color.border.secondary, CaroTheme.shape.xl)
            .padding(horizontal = CaroTheme.spacing.xl2, vertical = CaroTheme.spacing.xl),
        verticalArrangement = Arrangement.spacedBy(CaroTheme.spacing.m),
    ) {
        LibraryDeckHeading(deck)
        LibraryCardCount(deck.cardCount)
    }
}

@Composable
internal fun LibraryDeckHeading(
    deck: LibraryDeck,
    preview: Boolean = false,
) {
    Column(verticalArrangement = Arrangement.spacedBy(CaroTheme.spacing.s)) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(CaroTheme.spacing.s),
        ) {
            Text(
                deck.name,
                Modifier.weight(1f),
                style = if (preview) CaroTheme.typography.heading1 else CaroTheme.typography.heading2,
                color = CaroTheme.color.text.primary,
            )
            Text(
                stringResource(Res.string.library_free),
                Modifier
                    .background(CaroTheme.color.surface.ready, CaroTheme.shape.xl)
                    .border(
                        1.dp,
                        CaroTheme.color.border.ready,
                        CaroTheme.shape.xl,
                    ).padding(horizontal = CaroTheme.spacing.s, vertical = CaroTheme.spacing.xs),
                style = CaroTheme.typography.caption1.regular,
                color = CaroTheme.color.text.brand,
            )
        }
        if (deck.description.isNotBlank()) {
            Text(deck.description, style = CaroTheme.typography.body2.medium, color = CaroTheme.color.text.secondary)
        }
    }
}

@Composable
internal fun LibraryCardCount(count: Int) {
    Text(
        stringResource(Res.string.library_card_count, count.formatWithComma()),
        style = CaroTheme.typography.body2.medium,
        color = CaroTheme.color.text.secondary,
    )
}

@Composable
internal fun LibraryLoading() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = CaroTheme.color.surface.brand)
    }
}

@Composable
internal fun LibraryFailureContent(
    failure: LibraryFailure,
    preview: Boolean,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    onLogin: () -> Unit,
    onHome: () -> Unit,
) {
    when (failure) {
        LibraryFailure.LOGIN -> {
            LibraryMessage(
                stringResource(Res.string.library_login_title),
                stringResource(Res.string.library_login_body),
                stringResource(Res.string.library_login),
                onLogin,
            )
        }

        LibraryFailure.FORBIDDEN -> {
            LibraryMessage(
                stringResource(Res.string.library_forbidden_title),
                stringResource(Res.string.library_forbidden_body),
                stringResource(Res.string.library_home),
                onHome,
            )
        }

        LibraryFailure.UNAVAILABLE -> {
            LibraryMessage(
                stringResource(Res.string.library_unavailable_title),
                stringResource(Res.string.library_unavailable_body),
                stringResource(Res.string.library_back_list),
                onBack,
            )
        }

        LibraryFailure.NETWORK -> {
            LibraryMessage(
                stringResource(if (preview) Res.string.library_preview_error else Res.string.library_load_error),
                stringResource(Res.string.library_retry_body),
                stringResource(Res.string.library_retry),
                onRetry,
            )
        }
    }
}

@Composable
private fun LibraryMessage(
    title: String,
    body: String,
    action: String,
    onClick: () -> Unit,
) {
    Box(Modifier.fillMaxSize().padding(CaroTheme.spacing.xl2), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, style = CaroTheme.typography.heading2, color = CaroTheme.color.text.primary, textAlign = TextAlign.Center)
            Spacer(Modifier.height(CaroTheme.spacing.s))
            Text(body, style = CaroTheme.typography.body2.medium, color = CaroTheme.color.text.secondary, textAlign = TextAlign.Center)
            Spacer(Modifier.height(CaroTheme.spacing.xl))
            Text(
                action,
                Modifier
                    .clickable(
                        role = Role.Button,
                        onClick = onClick,
                    ).padding(horizontal = CaroTheme.spacing.l, vertical = CaroTheme.spacing.m),
                style = CaroTheme.typography.body2.semiBold,
                color = CaroTheme.color.text.brand,
            )
        }
    }
}

package com.whatever.caro.feature.home.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import caromobile.core.designsystem.generated.resources.Res
import caromobile.core.designsystem.generated.resources.home_my_decks
import caromobile.core.designsystem.generated.resources.ic_book_open_18
import caromobile.core.designsystem.generated.resources.library_title
import com.whatever.caro.core.designsystem.themes.CaroTheme
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun HomeDeckHeader(onLibraryClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(Res.string.home_my_decks),
            style = CaroTheme.typography.heading2,
            color = CaroTheme.color.text.primary,
        )
        Row(
            modifier =
                Modifier
                    .heightIn(min = 44.dp)
                    .clickable(role = Role.Button, onClick = onLibraryClick)
                    .padding(horizontal = CaroTheme.spacing.m),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(Res.drawable.ic_book_open_18),
                contentDescription = null,
                tint = CaroTheme.color.icon.brand,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = stringResource(Res.string.library_title),
                style = CaroTheme.typography.body2.semiBold,
                color = CaroTheme.color.text.brand,
            )
        }
    }
}

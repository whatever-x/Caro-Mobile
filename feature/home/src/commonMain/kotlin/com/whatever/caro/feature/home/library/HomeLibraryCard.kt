package com.whatever.caro.feature.home.library

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import caromobile.core.designsystem.generated.resources.Res
import caromobile.core.designsystem.generated.resources.library_home_description
import caromobile.core.designsystem.generated.resources.library_title
import com.whatever.caro.core.designsystem.themes.CaroTheme
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun HomeLibraryCard(onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = CaroTheme.spacing.xl2, vertical = CaroTheme.spacing.m)
            .clip(CaroTheme.shape.xl)
            .clickable(role = Role.Button, onClick = onClick)
            .background(CaroTheme.color.surface.primary)
            .border(1.dp, CaroTheme.color.border.secondary, CaroTheme.shape.xl)
            .padding(horizontal = CaroTheme.spacing.xl2, vertical = CaroTheme.spacing.xl),
        verticalArrangement = Arrangement.spacedBy(CaroTheme.spacing.s),
    ) {
        Text(stringResource(Res.string.library_title), style = CaroTheme.typography.heading2, color = CaroTheme.color.text.primary)
        Text(
            stringResource(Res.string.library_home_description),
            style = CaroTheme.typography.body2.medium,
            color = CaroTheme.color.text.secondary,
        )
    }
}

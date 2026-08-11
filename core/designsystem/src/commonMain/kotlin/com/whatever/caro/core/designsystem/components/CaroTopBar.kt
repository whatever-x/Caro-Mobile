package com.whatever.caro.core.designsystem.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import caromobile.core.designsystem.generated.resources.Res
import caromobile.core.designsystem.generated.resources.ic_add_24
import caromobile.core.designsystem.generated.resources.ic_arrow_left_24
import caromobile.core.designsystem.generated.resources.ic_more_vertical_24
import com.whatever.caro.core.designsystem.themes.CaroTheme
import org.jetbrains.compose.resources.painterResource

private val TopBarHeight = 56.dp

@Composable
fun CaroTopBar(
    modifier: Modifier = Modifier,
    leadingContent: (@Composable BoxScope.() -> Unit)? = null,
    centerContent: (@Composable BoxScope.() -> Unit)? = null,
    trailingContent: (@Composable BoxScope.() -> Unit)? = null,
) {
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(height = TopBarHeight),
    ) {
        leadingContent?.let { content ->
            Box(
                modifier = Modifier.align(alignment = Alignment.CenterStart),
                contentAlignment = Alignment.CenterStart,
                content = content,
            )
        }

        centerContent?.let { content ->
            Box(
                modifier = Modifier.align(alignment = Alignment.Center),
                contentAlignment = Alignment.Center,
                content = content,
            )
        }

        trailingContent?.let { content ->
            Box(
                modifier =
                    Modifier.align(alignment = Alignment.CenterEnd),
                contentAlignment = Alignment.CenterEnd,
                content = content,
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 447, name = "Leading Icon / Center Component")
@Composable
private fun CaroTopBarLeadingIconCenterComponentPreview() {
    CaroTheme {
        CaroTopBar(
            modifier = Modifier.width(PreviewTopBarWidth),
            leadingContent = {
                PreviewArrowLeftIcon()
            },
            centerContent = {
                PreviewCenterComponent()
            },
        )
    }
}

@Preview(showBackground = true, widthDp = 447, name = "Leading Icon / Center Component / Trailing 2 Icon")
@Composable
private fun CaroTopBarLeadingIconCenterComponentTrailing2IconPreview() {
    CaroTheme {
        CaroTopBar(
            modifier = Modifier.width(PreviewTopBarWidth),
            leadingContent = {
                PreviewArrowLeftIcon()
            },
            centerContent = {
                PreviewCenterComponent()
            },
            trailingContent = {
                PreviewTrailing2Icon()
            },
        )
    }
}

@Preview(showBackground = true, widthDp = 447, name = "Leading Icon + Text")
@Composable
private fun CaroTopBarLeadingIconTextPreview() {
    CaroTheme {
        CaroTopBar(
            modifier = Modifier.width(PreviewTopBarWidth),
            leadingContent = {
                PreviewLeadingIconText()
            },
        )
    }
}

@Preview(showBackground = true, widthDp = 447, name = "Leading Icon + Text / Trailing Icon")
@Composable
private fun CaroTopBarLeadingIconTextTrailingIconPreview() {
    CaroTheme {
        CaroTopBar(
            modifier = Modifier.width(PreviewTopBarWidth),
            leadingContent = {
                PreviewLeadingIconText()
            },
            trailingContent = {
                PreviewMoreIcon()
            },
        )
    }
}

@Preview(showBackground = true, widthDp = 447, name = "Leading Text / Trailing Icon")
@Composable
private fun CaroTopBarLeadingTextTrailingIconPreview() {
    CaroTheme {
        CaroTopBar(
            modifier = Modifier.width(PreviewTopBarWidth),
            leadingContent = {
                PreviewText()
            },
            trailingContent = {
                PreviewMoreIcon()
            },
        )
    }
}

private val PreviewTopBarWidth = 447.dp
private val PreviewIconSize = 24.dp
private val PreviewCenterComponentBorderWidth = 1.dp
private val PreviewCenterComponentHorizontalPadding = 6.dp
private val PreviewCenterComponentVerticalPadding = 5.dp

@Composable
private fun PreviewLeadingIconText() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(CaroTheme.spacing.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PreviewArrowLeftIcon()
        PreviewText()
    }
}

@Composable
private fun PreviewTrailing2Icon() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(CaroTheme.spacing.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PreviewMoreIcon()
        PreviewAddIcon()
    }
}

@Composable
private fun PreviewArrowLeftIcon() {
    Icon(
        modifier = Modifier.size(PreviewIconSize),
        painter = painterResource(Res.drawable.ic_arrow_left_24),
        contentDescription = null,
        tint = CaroTheme.color.icon.secondary,
    )
}

@Composable
private fun PreviewMoreIcon() {
    Icon(
        modifier = Modifier.size(PreviewIconSize),
        painter = painterResource(Res.drawable.ic_more_vertical_24),
        contentDescription = null,
        tint = CaroTheme.color.icon.secondary,
    )
}

@Composable
private fun PreviewAddIcon() {
    Icon(
        modifier = Modifier.size(PreviewIconSize),
        painter = painterResource(Res.drawable.ic_add_24),
        contentDescription = null,
        tint = CaroTheme.color.icon.secondary,
    )
}

@Composable
private fun PreviewText() {
    Text(
        text = "Text",
        style = CaroTheme.typography.heading2,
        color = CaroTheme.color.text.primary,
    )
}

@Composable
private fun PreviewCenterComponent() {
    Text(
        modifier =
            Modifier
                .border(
                    width = PreviewCenterComponentBorderWidth,
                    color = CaroTheme.color.border.brand,
                    shape = CaroTheme.shape.m,
                ).padding(
                    horizontal = PreviewCenterComponentHorizontalPadding,
                    vertical = PreviewCenterComponentVerticalPadding,
                ),
        text = "Component",
        style = CaroTheme.typography.heading2,
        color = CaroTheme.color.text.primary,
    )
}

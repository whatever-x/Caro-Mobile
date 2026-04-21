package com.whatever.caro.core.designsystem.preview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import caromobile.core.designsystem.generated.resources.Res
import caromobile.core.designsystem.generated.resources.allDrawableResources
import org.jetbrains.compose.resources.painterResource

private val allDrawables = Res.allDrawableResources.values.chunked(10)

@Preview(showBackground = true)
@Composable
fun IconPreviewGrid(
    modifier: Modifier = Modifier,
    iconSize: Dp = 32.dp,
    horizontalSpacing: Dp = 4.dp,
    verticalSpacing: Dp = 4.dp
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        LazyColumn(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(verticalSpacing),
            modifier = Modifier.wrapContentWidth()
        ) {
            items(allDrawables) { rowIcons ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(
                        horizontalSpacing,
                        Alignment.CenterHorizontally
                    )
                ) {
                    rowIcons.forEach { icon ->
                        Box(
                            modifier = Modifier.size(iconSize),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(icon),
                                contentDescription = null
                            )
                        }
                    }
                }
            }
        }
    }
}
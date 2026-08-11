package com.whatever.caro.core.ui.image

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.DefaultAlpha
import androidx.compose.ui.layout.ContentScale
import coil3.SingletonImageLoader
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

@Composable
fun CaroAsyncImage(
    imageUrl: String?,
    contentDescription: String? = null,
    modifier: Modifier = Modifier,
    placeholder: DrawableResource,
    error: DrawableResource,
    fallback: DrawableResource = error,
    contentScale: ContentScale = ContentScale.Fit,
    alignment: Alignment = Alignment.Center,
    alpha: Float = DefaultAlpha,
    colorFilter: ColorFilter? = null,
) {
    val context = LocalPlatformContext.current
    val imageLoader = remember(context) { SingletonImageLoader.get(context) }
    val imageRequest =
        remember(context, imageUrl) {
            ImageRequest
                .Builder(context)
                .data(imageUrl)
                .crossfade(true)
                .build()
        }

    AsyncImage(
        model = imageRequest,
        contentDescription = contentDescription,
        imageLoader = imageLoader,
        modifier = modifier,
        placeholder = painterResource(placeholder),
        error = painterResource(error),
        fallback = painterResource(fallback),
        contentScale = contentScale,
        alignment = alignment,
        alpha = alpha,
        colorFilter = colorFilter,
    )
}

package com.whatever.caro.core.model.banner

import androidx.compose.runtime.Immutable
import kotlin.time.Instant

@Immutable
data class HomeBanner(
    val id: String,
    val revision: Long,
    val imageUrl: String,
    val accessibleLabel: String,
    val libraryDeckId: Long?,
    val expiresAt: Instant,
) {
    val key: String get() = "$id:$revision"
}

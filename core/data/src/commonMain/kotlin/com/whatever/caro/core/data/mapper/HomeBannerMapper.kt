package com.whatever.caro.core.data.mapper

import com.whatever.caro.core.model.banner.HomeBanner
import com.whatever.caro.core.remote.dto.banner.HomeBannerResponse
import com.whatever.caro.core.remote.dto.banner.HomeBannersResponse
import kotlin.math.abs
import kotlin.time.Instant

internal fun HomeBannersResponse.toHomeBanners(requestedAt: Instant): List<HomeBanner> {
    val serverNow = Instant.parse(serverTime)
    return items.mapNotNull { it.toHomeBanner(serverNow, requestedAt) }.distinctBy { it.id }.take(5)
}

private fun HomeBannerResponse.toHomeBanner(
    serverNow: Instant,
    requestedAt: Instant,
): HomeBanner? {
    val bannerId = id?.takeIf { it.isNotBlank() } ?: return null
    val bannerRevision = revision?.takeIf { it > 0 } ?: return null
    val url = imageUrl?.takeIf { it.startsWith("https://") } ?: return null
    val label = accessibleLabel?.takeIf { it.isNotBlank() } ?: return null
    if (aspectRatio?.let { it.isFinite() && abs(it - 3.0) < 0.001 } != true) return null
    val starts = startsAt?.let { runCatching { Instant.parse(it) }.getOrNull() } ?: return null
    val ends = endsAt?.let { runCatching { Instant.parse(it) }.getOrNull() } ?: return null
    if (serverNow < starts || serverNow >= ends) return null
    val target = destination ?: return null
    val deckId =
        when (target.type) {
            "LIBRARY_LIST" -> null
            "LIBRARY_DECK" -> target.id?.toLongOrNull()?.takeIf { it > 0 } ?: return null
            else -> return null
        }
    // 요청 시작 시각을 기준으로 잡아 응답 지연 때문에 노출 기간이 늘어나지 않게 한다.
    return HomeBanner(bannerId, bannerRevision, url, label, deckId, requestedAt + (ends - serverNow))
}

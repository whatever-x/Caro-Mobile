package com.whatever.caro.core.remote.dto.banner

import kotlinx.serialization.Serializable

@Serializable
data class HomeBannersResponse(
    val items: List<HomeBannerResponse>,
    val serverTime: String,
    val configRevision: String,
)

@Serializable
data class HomeBannerResponse(
    val id: String? = null,
    val revision: Long? = null,
    val imageUrl: String? = null,
    val aspectRatio: Double? = null,
    val accessibleLabel: String? = null,
    val destination: BannerDestinationResponse? = null,
    val startsAt: String? = null,
    val endsAt: String? = null,
)

@Serializable
data class BannerDestinationResponse(
    val type: String? = null,
    val id: String? = null,
)

package com.whatever.caro.core.data.repository.banner

import com.whatever.caro.core.model.banner.HomeBanner

interface HomeBannerRepository {
    suspend fun list(): List<HomeBanner>
}

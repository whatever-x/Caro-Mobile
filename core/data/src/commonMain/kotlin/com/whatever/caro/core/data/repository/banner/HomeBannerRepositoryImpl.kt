package com.whatever.caro.core.data.repository.banner

import com.whatever.caro.core.data.mapper.toHomeBanners
import com.whatever.caro.core.model.banner.HomeBanner
import com.whatever.caro.core.remote.datasource.banner.HomeBannerDataSource
import kotlin.time.Clock

internal class HomeBannerRepositoryImpl(
    private val dataSource: HomeBannerDataSource,
) : HomeBannerRepository {
    override suspend fun list(): List<HomeBanner> {
        val requestedAt = Clock.System.now()
        return dataSource.list().toHomeBanners(requestedAt)
    }
}

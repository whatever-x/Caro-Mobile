package com.whatever.caro.core.remote.datasource.banner

import com.whatever.caro.core.remote.api.HomeBannerApi
import com.whatever.caro.core.remote.network.config.CaroNetworkConfig

internal class RemoteHomeBannerDataSourceImpl(
    private val api: HomeBannerApi,
) : HomeBannerDataSource {
    override suspend fun list() = api.list(CaroNetworkConfig.platform, CaroNetworkConfig.appVersion)
}

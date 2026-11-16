package com.whatever.caro.core.remote.datasource.banner

import com.whatever.caro.core.remote.dto.banner.HomeBannersResponse

interface HomeBannerDataSource {
    suspend fun list(): HomeBannersResponse
}

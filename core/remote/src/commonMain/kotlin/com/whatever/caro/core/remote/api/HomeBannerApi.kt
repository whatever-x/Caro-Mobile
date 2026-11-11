package com.whatever.caro.core.remote.api

import com.whatever.caro.core.remote.dto.banner.HomeBannersResponse
import de.jensklingenberg.ktorfit.http.GET
import de.jensklingenberg.ktorfit.http.Headers
import de.jensklingenberg.ktorfit.http.Query

internal interface HomeBannerApi {
    @Headers(ApiVersionHeaders.V1_0)
    @GET("home/banners")
    suspend fun list(
        @Query("platform") platform: String,
        @Query("appVersion") appVersion: String,
    ): HomeBannersResponse
}

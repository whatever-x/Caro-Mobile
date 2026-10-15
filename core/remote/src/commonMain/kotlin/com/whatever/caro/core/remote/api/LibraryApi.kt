package com.whatever.caro.core.remote.api

import com.whatever.caro.core.remote.dto.library.LibraryCopyResponse
import com.whatever.caro.core.remote.dto.library.LibraryDeckResponse
import com.whatever.caro.core.remote.dto.library.LibraryDetailResponse
import de.jensklingenberg.ktorfit.http.GET
import de.jensklingenberg.ktorfit.http.Header
import de.jensklingenberg.ktorfit.http.Headers
import de.jensklingenberg.ktorfit.http.POST
import de.jensklingenberg.ktorfit.http.Path

internal interface LibraryApi {
    @Headers(ApiVersionHeaders.V1_0)
    @GET("library/decks")
    suspend fun list(): List<LibraryDeckResponse>

    @Headers(ApiVersionHeaders.V1_0)
    @GET("library/decks/{libraryDeckId}")
    suspend fun detail(
        @Path("libraryDeckId") libraryDeckId: Long,
    ): LibraryDetailResponse

    @Headers(ApiVersionHeaders.V1_0)
    @POST("library/decks/{libraryDeckId}/copies")
    suspend fun copy(
        @Path("libraryDeckId") libraryDeckId: Long,
        @Header("Idempotency-Key") requestKey: String,
    ): LibraryCopyResponse
}

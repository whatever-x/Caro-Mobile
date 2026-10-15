package com.whatever.caro.core.remote.datasource.library

import com.whatever.caro.core.remote.dto.library.LibraryCopyResponse
import com.whatever.caro.core.remote.dto.library.LibraryDeckResponse
import com.whatever.caro.core.remote.dto.library.LibraryDetailResponse

interface LibraryDataSource {
    suspend fun list(): List<LibraryDeckResponse>

    suspend fun detail(libraryDeckId: Long): LibraryDetailResponse

    suspend fun copy(
        libraryDeckId: Long,
        requestKey: String,
    ): LibraryCopyResponse
}

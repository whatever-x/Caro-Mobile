package com.whatever.caro.core.remote.datasource.library

import com.whatever.caro.core.remote.api.LibraryApi

internal class RemoteLibraryDataSourceImpl(
    private val api: LibraryApi,
) : LibraryDataSource {
    override suspend fun list() = api.list()

    override suspend fun detail(libraryDeckId: Long) = api.detail(libraryDeckId)

    override suspend fun copy(
        libraryDeckId: Long,
        requestKey: String,
    ) = api.copy(libraryDeckId, requestKey)
}

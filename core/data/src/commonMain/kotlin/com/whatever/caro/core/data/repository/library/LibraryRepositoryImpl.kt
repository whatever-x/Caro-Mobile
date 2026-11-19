package com.whatever.caro.core.data.repository.library

import com.whatever.caro.core.data.mapper.toModel
import com.whatever.caro.core.data.mapper.toPersonalDeck
import com.whatever.caro.core.remote.datasource.library.LibraryDataSource

internal class LibraryRepositoryImpl(
    private val dataSource: LibraryDataSource,
) : LibraryRepository {
    override suspend fun list() = dataSource.list().map { it.toModel() }

    override suspend fun detail(libraryDeckId: Long) = dataSource.detail(libraryDeckId).toModel()

    override suspend fun copy(
        libraryDeckId: Long,
        requestKey: String,
    ) = dataSource.copy(libraryDeckId, requestKey).toPersonalDeck()
}

package com.whatever.caro.core.data.repository.demo

import com.whatever.caro.core.data.mapper.toUser
import com.whatever.caro.core.model.User
import com.whatever.caro.core.remote.datasource.demo.DemoDataSource
import com.whatever.caro.core.remote.datasource.sample.SampleDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

internal class DemoRepositoryImpl(
    private val demoDataSource: DemoDataSource,
    private val sampleDataSource: SampleDataSource,
) : DemoRepository {
    override suspend fun getData(id: Long): User {
        val result = demoDataSource.getRestWithQuery(query = id.toInt())

        return result.toUser()
    }

    override fun getDataFlow(id: Long): Flow<User> =
        flow {
            val result = demoDataSource.getRestWithQuery(query = id.toInt())

            emit(value = result.toUser())
        }

    override suspend fun getString(): String = sampleDataSource.getString()
}

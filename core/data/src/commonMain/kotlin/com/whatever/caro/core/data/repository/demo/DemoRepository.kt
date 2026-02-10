package com.whatever.caro.core.data.repository.demo

import com.whatever.caro.core.model.User
import kotlinx.coroutines.flow.Flow

interface DemoRepository {
    suspend fun getData(id: Long): User

    fun getDataFlow(id: Long): Flow<User>
}

package com.whatever.caro.core.remote.datasource.sample

interface SampleDataSource {
    suspend fun getString(): String
}

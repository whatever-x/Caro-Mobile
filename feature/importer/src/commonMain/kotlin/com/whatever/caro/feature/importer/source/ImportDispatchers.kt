package com.whatever.caro.feature.importer.source

import kotlinx.coroutines.CoroutineDispatcher

/** 테스트가 실제 Dispatchers.IO와 Default 대신 테스트 디스패처를 주입해 가상 시간으로 돌리게 한다. */
internal class ImportDispatchers(
    val io: CoroutineDispatcher,
    val default: CoroutineDispatcher,
)

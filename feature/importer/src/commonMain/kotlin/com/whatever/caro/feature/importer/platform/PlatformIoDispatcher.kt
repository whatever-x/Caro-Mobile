package com.whatever.caro.feature.importer.platform

import kotlinx.coroutines.CoroutineDispatcher

// common 소스 세트에는 Dispatchers.IO가 없어 플랫폼마다 구한다.
internal expect fun platformIoDispatcher(): CoroutineDispatcher

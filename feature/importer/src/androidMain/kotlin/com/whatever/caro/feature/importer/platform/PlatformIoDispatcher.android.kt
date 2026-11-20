package com.whatever.caro.feature.importer.platform

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

internal actual fun platformIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

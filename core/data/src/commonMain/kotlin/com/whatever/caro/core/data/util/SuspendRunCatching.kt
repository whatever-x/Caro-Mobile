package com.whatever.caro.core.data.util

import kotlinx.coroutines.CancellationException

/**
 * CancellationException은 흡수하지 않고 rethrow하는 runCatching.
 * coroutine cancellation을 보존해야 하는 suspend 블록에서 사용한다.
 */
suspend inline fun <T> suspendRunCatching(block: suspend () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (c: CancellationException) {
        throw c
    } catch (t: Throwable) {
        Result.failure(t)
    }

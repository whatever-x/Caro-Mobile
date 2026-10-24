package com.whatever.caro.feature.importer.support

/** OutOfMemoryError처럼 Exception이 아닌 Throwable을 공통 코드에서 만들 수 없어 대신 쓰는 오류다. */
internal class ForcedError(
    message: String = "forced",
) : Error(message)

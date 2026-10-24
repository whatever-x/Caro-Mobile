package com.whatever.caro.feature.importer.support

import io.github.aakira.napier.Antilog
import io.github.aakira.napier.LogLevel
import io.github.aakira.napier.Napier

internal class CapturedLog(
    val level: LogLevel,
    val message: String?,
    val throwable: Throwable?,
)

/** Napier는 전역 상태라 [block]이 끝나면 등록한 antilog를 반드시 뺀다. */
internal inline fun <T> captureNapier(block: (List<CapturedLog>) -> T): T {
    val logs = mutableListOf<CapturedLog>()
    Napier.base(
        object : Antilog() {
            override fun performLog(
                priority: LogLevel,
                tag: String?,
                throwable: Throwable?,
                message: String?,
            ) {
                logs += CapturedLog(priority, message, throwable)
            }
        },
    )
    try {
        return block(logs)
    } finally {
        Napier.takeLogarithm()
    }
}

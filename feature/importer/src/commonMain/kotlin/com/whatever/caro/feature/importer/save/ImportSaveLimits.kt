package com.whatever.caro.feature.importer.save

import com.whatever.caro.core.model.importing.ImportLimits
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

internal object ImportSaveLimits {
    /** 묶음 하나의 서버 처리가 앱의 소켓 무응답 한도(10초) 안에 끝나야 한다. staging 측정에서 기준을 넘으면 500으로 낮춘다. */
    const val CHUNK_SIZE = ImportLimits.SERVER_CHUNK_MAX

    /** 최악 1,000장(양면 500자 한글)은 약 3MB라 프록시 본문 한도에 걸린다. 프록시 한도가 확인되면 그 80% 이하로 맞춘다. */
    const val CHUNK_MAX_BYTES = 2L * 1024 * 1024

    const val AUTO_RETRY_COUNT = 3
    val RETRY_BACKOFF: List<Duration> = listOf(2.seconds, 4.seconds, 8.seconds)

    /** 서버의 처리 중 표시는 30초 뒤 사라져 같은 키 재시도가 다시 실행될 수 있으므로 그보다 짧게 끊는다. */
    val PROCESSING_WAIT_LIMIT: Duration = 25.seconds
    val PROCESSING_RETRY_DELAY: Duration = 2.seconds
}

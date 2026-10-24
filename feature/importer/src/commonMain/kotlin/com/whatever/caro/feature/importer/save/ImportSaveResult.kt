package com.whatever.caro.feature.importer.save

import com.whatever.caro.core.model.deck.Deck

internal enum class UserRetry {
    /** 서버가 저장했는지 모르므로 같은 키로 보내 이미 저장됐으면 첫 응답을 재생받는다. */
    SAME_KEY,

    /** 4xx 응답도 24시간 저장되어 같은 키로는 같은 오류만 재생되므로 새 키를 쓴다. 4xx는 저장하지 않았다는 뜻이라 안전하다. */
    NEW_KEY,

    NONE,
}

internal sealed class ChunkFailure(
    val userRetry: UserRetry,
) {
    data object RetryExhausted : ChunkFailure(UserRetry.SAME_KEY)

    data object ProcessingTimeout : ChunkFailure(UserRetry.SAME_KEY)

    data object InvalidResponse : ChunkFailure(UserRetry.SAME_KEY)

    data object Unknown : ChunkFailure(UserRetry.SAME_KEY)

    data class Rejected(
        val code: String,
    ) : ChunkFailure(UserRetry.NEW_KEY)

    /** 같은 키로 다른 본문을 보낸 앱 결함. 새 키로 보내면 이미 저장된 다른 내용과 겹칠 수 있어 다시 시도를 막는다. */
    data object KeyConflict : ChunkFailure(UserRetry.NONE)

    /** 서버는 저장했지만 만든 장수가 보낸 장수와 다르다. 같은 키는 같은 응답만 재생하고 새 키는 중복 저장을 만들 수 있다. */
    data class ResponseCountMismatch(
        val created: Int,
    ) : ChunkFailure(UserRetry.NONE)

    data object DeckUnavailable : ChunkFailure(UserRetry.NONE)

    data object StoppedByUser : ChunkFailure(UserRetry.NONE)
}

internal sealed interface ImportSaveOutcome {
    /** 이번 호출에서 저장한 장수다. 이전 호출 몫은 호출자가 더해 쓰고, 장수가 다른 묶음에서 만들어진 장수는 이미 더해져 있다. */
    val savedCards: Int

    data class Completed(
        override val savedCards: Int,
    ) : ImportSaveOutcome

    /** [chunkIndex] 묶음부터는 저장되지 않았다. */
    data class Stopped(
        val chunkIndex: Int,
        override val savedCards: Int,
        val failure: ChunkFailure,
    ) : ImportSaveOutcome
}

internal sealed interface ImportSaveProgress {
    data class ChunkSaved(
        val chunkIndex: Int,
        val cardCount: Int,
    ) : ImportSaveProgress

    data class Retrying(
        val chunkIndex: Int,
        val attempt: Int,
        val maxAttempts: Int,
    ) : ImportSaveProgress

    data class WaitingForServer(
        val chunkIndex: Int,
    ) : ImportSaveProgress
}

internal sealed interface DeckCreationResult {
    data class Created(
        val deck: Deck,
    ) : DeckCreationResult

    /** 덱이 만들어지지 않은 4xx. 덱 정보 화면에 머물러 입력을 고친다. */
    data class Rejected(
        val code: String,
    ) : DeckCreationResult

    /** 네트워크, 타임아웃, 서버 오류, 해석 실패. 덱이 만들어졌는지 모를 수 있어 자동으로 다시 보내지 않는다. */
    data class Failed(
        val cause: Throwable,
    ) : DeckCreationResult
}

package com.whatever.caro.feature.importer.save

import com.whatever.caro.core.data.repository.card.CardRepository
import com.whatever.caro.core.data.repository.deck.DeckRepository
import com.whatever.caro.core.model.card.CardContent
import com.whatever.caro.core.model.exception.CaroAuthException
import com.whatever.caro.core.model.exception.CaroInvalidResponseException
import com.whatever.caro.core.model.exception.CaroServerException
import com.whatever.caro.core.model.exception.NetworkException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration
import kotlin.time.TimeMark
import kotlin.time.TimeSource
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * 가져오기 저장을 조정한다. 덱 생성(1.0)과 카드 묶음 저장(1.1, 멱등 키)을 함께 다루므로 화면별 저장소 호출과 분리한다.
 * 묶음은 앞 묶음이 성공한 뒤에만 보내 서버의 생성 순서가 원본 순서와 같게 유지된다.
 * 키별 첫 전송 시각을 기억하므로 가져오기 저장 흐름 하나(화면 하나)마다 새로 만들어 쓴다.
 */
internal class ImportSaveCoordinator(
    private val deckRepository: DeckRepository,
    private val cardRepository: CardRepository,
    private val timeSource: TimeSource = TimeSource.Monotonic,
    private val newKey: () -> String = ::randomIdempotencyKey,
) {
    // 사용자 재시도나 자동 재시도로 다시 보내도 C010 대기 상한은 그 키를 처음 보낸 때부터 센다. 서버의 처리 중 표시도 그때부터 흐른다.
    // 같은 키로 다시 보낼 수 있는 동안만 남기고, 저장 호출을 하나씩 실행해 이 맵을 한 코루틴에서만 고친다.
    private val firstSentAt = HashMap<String, TimeMark>()
    private val saving = Mutex()

    private sealed interface Attempt {
        data object Saved : Attempt

        data object Processing : Attempt

        data object Retryable : Attempt

        data class Stop(
            val failure: ChunkFailure,
        ) : Attempt
    }

    /** 불확실한 실패 뒤 앱이 혼자 다시 보내면 사용자가 모르는 빈 덱이 생길 수 있어 자동 재시도하지 않는다. */
    suspend fun createDeck(
        name: String,
        description: String,
    ): DeckCreationResult =
        try {
            DeckCreationResult.Created(deckRepository.createDeck(name, description))
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: CaroAuthException) {
            throw exception
        } catch (exception: CaroServerException) {
            if (exception.code == SERVER_ERROR) DeckCreationResult.Failed(exception) else DeckCreationResult.Rejected(exception.code)
        } catch (exception: Exception) {
            DeckCreationResult.Failed(exception)
        }

    fun planChunks(cards: List<CardContent>): List<ImportChunk> =
        ImportChunker.split(cards, ImportSaveLimits.CHUNK_SIZE, ImportSaveLimits.CHUNK_MAX_BYTES, newKey)

    internal fun trackedKeyCount(): Int = firstSentAt.size

    fun renewKey(chunk: ImportChunk): ImportChunk = chunk.copy(idempotencyKey = newKey())

    /**
     * [fromIndex] 묶음부터 차례로 저장한다. [stopRequested]가 참이 되면 진행 중 묶음은 응답을 기다리되 자동 재시도하지 않고 다음 묶음을 시작하지 않는다.
     */
    suspend fun saveChunks(
        deckId: Long,
        chunks: List<ImportChunk>,
        fromIndex: Int = 0,
        stopRequested: () -> Boolean = { false },
        onProgress: (ImportSaveProgress) -> Unit = {},
    ): ImportSaveOutcome =
        saving.withLock {
            var saved = 0
            for (index in fromIndex until chunks.size) {
                if (stopRequested()) return@withLock ImportSaveOutcome.Stopped(index, saved, ChunkFailure.StoppedByUser)
                val failure = saveChunk(deckId, chunks[index], stopRequested, onProgress)
                // 장수가 다르게 저장된 묶음도 서버에는 남았으므로 저장 장수에 더한다.
                val partial = (failure as? ChunkFailure.ResponseCountMismatch)?.created ?: 0
                if (failure != null) return@withLock ImportSaveOutcome.Stopped(index, saved + partial, failure)
                // 서버가 만든 장수가 보낸 장수와 같을 때만 성공으로 보므로 묶음 장수만큼 저장된 것이다.
                saved += chunks[index].cards.size
                onProgress(ImportSaveProgress.ChunkSaved(index, chunks[index].cards.size))
            }
            ImportSaveOutcome.Completed(saved)
        }

    /** 성공하면 null, 멈추면 그 이유를 돌려준다. */
    private suspend fun saveChunk(
        deckId: Long,
        chunk: ImportChunk,
        stopRequested: () -> Boolean,
        onProgress: (ImportSaveProgress) -> Unit,
    ): ChunkFailure? {
        val failure = sendUntilSettled(deckId, chunk, stopRequested, onProgress)
        if (failure?.userRetry != UserRetry.SAME_KEY) firstSentAt.remove(chunk.idempotencyKey)
        return failure
    }

    private suspend fun sendUntilSettled(
        deckId: Long,
        chunk: ImportChunk,
        stopRequested: () -> Boolean,
        onProgress: (ImportSaveProgress) -> Unit,
    ): ChunkFailure? {
        val firstSent = firstSentAt.getOrPut(chunk.idempotencyKey) { timeSource.markNow() }
        var failures = 0
        while (true) {
            when (val attempt = send(deckId, chunk)) {
                Attempt.Saved -> {
                    return null
                }

                is Attempt.Stop -> {
                    return attempt.failure
                }

                Attempt.Processing -> {
                    val remaining = ImportSaveLimits.PROCESSING_WAIT_LIMIT - firstSent.elapsedNow()
                    if (remaining <= Duration.ZERO) return ChunkFailure.ProcessingTimeout
                    onProgress(ImportSaveProgress.WaitingForServer(chunk.index))
                    delay(minOf(ImportSaveLimits.PROCESSING_RETRY_DELAY, remaining))
                }

                Attempt.Retryable -> {
                    failures++
                    if (stopRequested()) return ChunkFailure.StoppedByUser
                    if (failures > ImportSaveLimits.AUTO_RETRY_COUNT) return ChunkFailure.RetryExhausted
                    onProgress(ImportSaveProgress.Retrying(chunk.index, failures, ImportSaveLimits.AUTO_RETRY_COUNT))
                    delay(ImportSaveLimits.RETRY_BACKOFF[failures - 1])
                    if (stopRequested()) return ChunkFailure.StoppedByUser
                }
            }
        }
    }

    private suspend fun send(
        deckId: Long,
        chunk: ImportChunk,
    ): Attempt =
        try {
            val created = cardRepository.createCardsChunk(deckId, chunk.cards, chunk.idempotencyKey)
            if (created == chunk.cards.size) Attempt.Saved else Attempt.Stop(ChunkFailure.ResponseCountMismatch(created))
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: CaroAuthException) {
            throw exception
        } catch (exception: Exception) {
            classify(exception)
        }

    /** `CaroServerException`에는 HTTP 상태가 없어 코드로만 판단한다. 목록에 없는 코드는 확정 4xx로 본다. */
    private fun classify(exception: Exception): Attempt =
        when (exception) {
            is NetworkException -> {
                Attempt.Retryable
            }

            is CaroInvalidResponseException -> {
                Attempt.Stop(ChunkFailure.InvalidResponse)
            }

            is CaroServerException -> {
                when (exception.code) {
                    PROCESSING -> Attempt.Processing
                    SERVER_ERROR -> Attempt.Retryable
                    KEY_CONFLICT -> Attempt.Stop(ChunkFailure.KeyConflict)
                    in DECK_UNAVAILABLE -> Attempt.Stop(ChunkFailure.DeckUnavailable)
                    else -> Attempt.Stop(ChunkFailure.Rejected(exception.code))
                }
            }

            else -> {
                Attempt.Stop(ChunkFailure.Unknown)
            }
        }

    private companion object {
        const val PROCESSING = "C010"
        const val KEY_CONFLICT = "C009"
        const val SERVER_ERROR = "C999"
        val DECK_UNAVAILABLE = setOf("D001", "D002", "CD002")
    }
}

@OptIn(ExperimentalUuidApi::class)
private fun randomIdempotencyKey(): String = Uuid.random().toString()

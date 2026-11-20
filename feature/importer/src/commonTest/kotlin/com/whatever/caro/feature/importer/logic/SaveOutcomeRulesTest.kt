package com.whatever.caro.feature.importer.logic

import com.whatever.caro.feature.importer.mvi.ImportDialog
import com.whatever.caro.feature.importer.mvi.PauseReason
import com.whatever.caro.feature.importer.save.ChunkFailure
import com.whatever.caro.feature.importer.save.ImportSaveOutcome
import com.whatever.caro.feature.importer.save.isSaveUnconfirmed
import com.whatever.caro.feature.importer.support.CONFIRMED_FAILURE_SAMPLES
import com.whatever.caro.feature.importer.support.UNCONFIRMED_FAILURE_SAMPLES
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe

class SaveOutcomeRulesTest : FunSpec() {
    init {
        context("멈추는 중이 아닐 때의 저장 결과") {
            test("자동 재시도를 다 쓴 실패는 네트워크 원인의 일시 정지 대화상자다") {
                resolve(stopped(ChunkFailure.RetryExhausted, saved = 1_000)) shouldBe
                    dialogAt(ImportDialog.Paused(saved = 1_000, reason = PauseReason.Network), ChunkFailure.RetryExhausted)
            }

            test("처리 시간 초과는 나중에 다시 시도하라는 원인의 일시 정지 대화상자다") {
                resolve(stopped(ChunkFailure.ProcessingTimeout, saved = 1_000)) shouldBe
                    dialogAt(ImportDialog.Paused(saved = 1_000, reason = PauseReason.Later), ChunkFailure.ProcessingTimeout)
            }

            test("응답 해석 실패는 나중에 다시 시도하라는 원인의 일시 정지 대화상자다") {
                resolve(stopped(ChunkFailure.InvalidResponse, saved = 1_000)) shouldBe
                    dialogAt(ImportDialog.Paused(saved = 1_000, reason = PauseReason.Later), ChunkFailure.InvalidResponse)
            }

            test("알 수 없는 실패는 나중에 다시 시도하라는 원인의 일시 정지 대화상자다") {
                resolve(stopped(ChunkFailure.Unknown, saved = 1_000)) shouldBe
                    dialogAt(ImportDialog.Paused(saved = 1_000, reason = PauseReason.Later), ChunkFailure.Unknown)
            }

            test("서버가 거절한 확정 실패는 일부 저장 실패 대화상자다") {
                resolve(stopped(ChunkFailure.Rejected("C001"), saved = 1_000)) shouldBe
                    dialogAt(ImportDialog.PartialFailure(saved = 1_000), ChunkFailure.Rejected("C001"))
            }

            test("키 충돌은 저장 결과를 확인하지 못한 키 충돌 대화상자다") {
                resolve(stopped(ChunkFailure.KeyConflict, saved = 1_000)) shouldBe
                    dialogAt(ImportDialog.KeyConflict(saved = 1_000, isSaveUnconfirmed = true), ChunkFailure.KeyConflict)
            }

            test("장수 불일치는 키 충돌 대화상자이고 서버가 만든 장수까지 저장 장수에 담는다") {
                resolve(stopped(ChunkFailure.ResponseCountMismatch(created = 300), saved = 1_300)) shouldBe
                    dialogAt(
                        ImportDialog.KeyConflict(saved = 1_300, isSaveUnconfirmed = false),
                        ChunkFailure.ResponseCountMismatch(created = 300),
                    )
            }

            test("덱을 찾을 수 없으면 덱 없음 대화상자다") {
                resolve(stopped(ChunkFailure.DeckUnavailable, saved = 1_000)) shouldBe
                    dialogAt(ImportDialog.DeckUnavailable, ChunkFailure.DeckUnavailable)
            }

            test("사용자가 멈춘 결과는 대화상자 없이 부분 저장 덱 상세로 이동한다") {
                resolve(stopped(ChunkFailure.StoppedByUser, saved = 4_000)) shouldBe
                    SaveOutcomeAction.NavigateToDeckDetail(saved = 4_000, completed = false, isSaveUnconfirmed = false)
            }

            test("모두 저장했으면 완료로 덱 상세에 이동한다") {
                resolve(ImportSaveOutcome.Completed(savedCards = 9_940)) shouldBe
                    SaveOutcomeAction.NavigateToDeckDetail(saved = 9_940, completed = true, isSaveUnconfirmed = false)
            }

            test("저장 장수 0장인 일시 정지도 0장으로 대화상자에 담는다") {
                resolve(stopped(ChunkFailure.RetryExhausted, saved = 0)) shouldBe
                    dialogAt(ImportDialog.Paused(saved = 0, reason = PauseReason.Network), ChunkFailure.RetryExhausted)
            }

            test("대화상자 갈래는 멈춘 묶음 번호와 실패를 함께 담는다") {
                val outcome = ImportSaveOutcome.Stopped(chunkIndex = 3, savedCards = 3_000, failure = ChunkFailure.Rejected("C001"))

                resolve(outcome) shouldBe
                    SaveOutcomeAction.ShowDialog(
                        ImportDialog.PartialFailure(saved = 3_000),
                        chunkIndex = 3,
                        failure = ChunkFailure.Rejected("C001"),
                    )
            }

            test("일시 정지 대화상자로 가는 실패는 모두 저장 결과를 확인하지 못한 실패다") {
                val pausedFailures =
                    (UNCONFIRMED_FAILURE_SAMPLES + CONFIRMED_FAILURE_SAMPLES).filter { failure ->
                        (resolve(stopped(failure, saved = 1_000)) as? SaveOutcomeAction.ShowDialog)?.dialog is ImportDialog.Paused
                    }

                pausedFailures.shouldNotBeEmpty()
                pausedFailures.forEach { failure ->
                    withClue(failure) { failure.isSaveUnconfirmed shouldBe true }
                }
            }
        }

        context("이전 호출에서 저장한 장수") {
            test("대화상자의 저장 장수는 이전 호출에서 저장한 장수에 이번 호출의 장수를 더한다") {
                resolve(stopped(ChunkFailure.RetryExhausted, saved = 1_000), alreadySaved = 2_000) shouldBe
                    dialogAt(ImportDialog.Paused(saved = 3_000, reason = PauseReason.Network), ChunkFailure.RetryExhausted)
            }

            test("완료 이동의 저장 장수도 이전 호출의 장수를 더한다") {
                resolve(ImportSaveOutcome.Completed(savedCards = 5_940), alreadySaved = 4_000) shouldBe
                    SaveOutcomeAction.NavigateToDeckDetail(saved = 9_940, completed = true, isSaveUnconfirmed = false)
            }

            test("부분 이동의 저장 장수도 이전 호출의 장수를 더한다") {
                resolve(stopped(ChunkFailure.StoppedByUser, saved = 0), alreadySaved = 4_000) shouldBe
                    SaveOutcomeAction.NavigateToDeckDetail(saved = 4_000, completed = false, isSaveUnconfirmed = false)
            }
        }

        context("멈추는 중의 저장 결과") {
            test("멈추는 중에 마지막 묶음까지 성공하면 완료로 처리해 덱 상세에 이동한다") {
                resolve(ImportSaveOutcome.Completed(savedCards = 9_940), isStopping = true) shouldBe
                    SaveOutcomeAction.NavigateToDeckDetail(saved = 9_940, completed = true, isSaveUnconfirmed = false)
            }

            test("멈추는 중에 덱을 찾을 수 없으면 덱 없음 대화상자다") {
                resolve(stopped(ChunkFailure.DeckUnavailable, saved = 1_000), isStopping = true) shouldBe
                    dialogAt(ImportDialog.DeckUnavailable, ChunkFailure.DeckUnavailable)
            }

            test("멈추는 중에 저장 결과를 확인하지 못한 실패는 대화상자 없이 결과 미확인으로 부분 저장 덱 상세로 이동한다") {
                val failures =
                    listOf(
                        ChunkFailure.RetryExhausted,
                        ChunkFailure.ProcessingTimeout,
                        ChunkFailure.InvalidResponse,
                        ChunkFailure.Unknown,
                        ChunkFailure.KeyConflict,
                        ChunkFailure.StoppedAfterFailedSend,
                    )

                failures.forEach { failure ->
                    withClue(failure) {
                        resolve(stopped(failure, saved = 1_000), isStopping = true) shouldBe
                            SaveOutcomeAction.NavigateToDeckDetail(saved = 1_000, completed = false, isSaveUnconfirmed = true)
                    }
                }
            }

            test("멈추는 중에 저장 결과가 확정된 실패는 대화상자 없이 부분 저장 덱 상세로 이동한다") {
                val failures =
                    listOf(
                        ChunkFailure.Rejected("C001"),
                        ChunkFailure.ResponseCountMismatch(created = 1_000),
                        ChunkFailure.StoppedByUser,
                    )

                failures.forEach { failure ->
                    withClue(failure) {
                        resolve(stopped(failure, saved = 1_000), isStopping = true) shouldBe
                            SaveOutcomeAction.NavigateToDeckDetail(saved = 1_000, completed = false, isSaveUnconfirmed = false)
                    }
                }
            }

            test("멈추는 중의 부분 이동도 이전 호출의 저장 장수를 더한다") {
                resolve(stopped(ChunkFailure.Unknown, saved = 500), isStopping = true, alreadySaved = 1_000) shouldBe
                    SaveOutcomeAction.NavigateToDeckDetail(saved = 1_500, completed = false, isSaveUnconfirmed = true)
            }
        }

        context("저장 중단 대화상자 본문") {
            test("일시 정지는 저장한 장수 뒤에 더 저장됐을 수 있다는 줄과 원인 줄이다") {
                saveDialogBody(ImportDialog.Paused(saved = 1_000, reason = PauseReason.Network)) shouldBe
                    SaveDialogBody(saved = 1_000, SavedCountLine.SavedThenUnconfirmed, CauseLine.CheckNetwork)
                saveDialogBody(ImportDialog.Paused(saved = 1_000, reason = PauseReason.Later)) shouldBe
                    SaveDialogBody(saved = 1_000, SavedCountLine.SavedThenUnconfirmed, CauseLine.TryLater)
            }

            test("저장 0장인 일시 정지는 빈 덱이라 단정하지 않고 저장됐는지 확인하지 못했다는 줄이다") {
                saveDialogBody(ImportDialog.Paused(saved = 0, reason = PauseReason.Network)) shouldBe
                    SaveDialogBody(saved = 0, SavedCountLine.NoneUnconfirmed, CauseLine.CheckNetwork)
                saveDialogBody(ImportDialog.Paused(saved = 0, reason = PauseReason.Later)) shouldBe
                    SaveDialogBody(saved = 0, SavedCountLine.NoneUnconfirmed, CauseLine.TryLater)
            }

            test("일부 저장 실패는 다시 시도하거나 저장한 카드로 덱을 볼 수 있다는 문장이다") {
                saveDialogBody(ImportDialog.PartialFailure(saved = 1_000)) shouldBe
                    SaveDialogBody(saved = 1_000, SavedCountLine.SavedCards, CauseLine.RetryOrViewDeck)
            }

            test("저장 0장인 일부 저장 실패는 다시 시도하거나 빈 덱을 볼 수 있다는 문장이다") {
                saveDialogBody(ImportDialog.PartialFailure(saved = 0)) shouldBe
                    SaveDialogBody(saved = 0, SavedCountLine.NoCardsSaved, CauseLine.RetryOrViewEmptyDeck)
            }

            test("결과를 확인하지 못한 키 충돌은 덱을 열어 저장된 카드를 확인하라는 원인 줄이다") {
                saveDialogBody(ImportDialog.KeyConflict(saved = 1_000, isSaveUnconfirmed = true)) shouldBe
                    SaveDialogBody(saved = 1_000, SavedCountLine.SavedThenUnconfirmed, CauseLine.CheckInDeck)
                saveDialogBody(ImportDialog.KeyConflict(saved = 0, isSaveUnconfirmed = true)) shouldBe
                    SaveDialogBody(saved = 0, SavedCountLine.NoneUnconfirmed, CauseLine.CheckInDeck)
            }

            test("장수가 확정된 키 충돌은 저장한 카드로 덱을 볼 수 있다는 문장이다") {
                saveDialogBody(ImportDialog.KeyConflict(saved = 1_000, isSaveUnconfirmed = false)) shouldBe
                    SaveDialogBody(saved = 1_000, SavedCountLine.SavedCards, CauseLine.ViewDeck)
            }

            test("장수가 확정된 키 충돌에서 저장한 카드가 없으면 원인 줄 없이 저장 줄만 보인다") {
                saveDialogBody(ImportDialog.KeyConflict(saved = 0, isSaveUnconfirmed = false)) shouldBe
                    SaveDialogBody(saved = 0, SavedCountLine.NoCardsSaved, causeLine = null)
            }

            test("저장 장수를 담지 않는 대화상자는 본문 규칙이 없다") {
                listOf(ImportDialog.DeckUnavailable, ImportDialog.StopConfirm, ImportDialog.DiscardConfirm, ImportDialog.DeckCreateFailed)
                    .forEach { dialog -> saveDialogBody(dialog) shouldBe null }
            }

            test("저장 결과를 확인하지 못한 대화상자는 어느 것도 확정 장수 줄을 고르지 않는다") {
                val unconfirmedDialogs =
                    UNCONFIRMED_FAILURE_SAMPLES.mapNotNull { failure ->
                        (resolve(stopped(failure, saved = 1_000)) as? SaveOutcomeAction.ShowDialog)?.dialog
                    }

                unconfirmedDialogs.shouldNotBeEmpty()
                unconfirmedDialogs.forEach { dialog ->
                    withClue(dialog) { saveDialogBody(dialog)?.savedLine shouldBe SavedCountLine.SavedThenUnconfirmed }
                }
            }
        }
    }

    private fun stopped(
        failure: ChunkFailure,
        saved: Int,
    ) = ImportSaveOutcome.Stopped(chunkIndex = 1, savedCards = saved, failure = failure)

    /** [stopped]가 만드는 결과는 1번 묶음에서 멈춘다. */
    private fun dialogAt(
        dialog: ImportDialog,
        failure: ChunkFailure,
    ) = SaveOutcomeAction.ShowDialog(dialog, chunkIndex = 1, failure = failure)

    private fun resolve(
        outcome: ImportSaveOutcome,
        alreadySaved: Int = 0,
        isStopping: Boolean = false,
    ): SaveOutcomeAction = resolveSaveOutcome(outcome, alreadySaved = alreadySaved, isStopping = isStopping)
}

package com.whatever.caro.feature.importer.logic

import com.whatever.caro.feature.importer.mvi.ImportDialog
import com.whatever.caro.feature.importer.mvi.PauseReason
import com.whatever.caro.feature.importer.save.ChunkFailure
import com.whatever.caro.feature.importer.save.ImportSaveOutcome
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class SaveOutcomeRulesTest : FunSpec() {
    init {
        context("멈추는 중이 아닐 때의 저장 결과") {
            test("자동 재시도를 다 쓴 실패는 네트워크 원인의 일시 정지 대화상자다") {
                resolve(stopped(ChunkFailure.RetryExhausted, saved = 1_000)) shouldBe
                    SaveOutcomeAction.ShowDialog(ImportDialog.Paused(saved = 1_000, reason = PauseReason.NETWORK))
            }

            test("처리 시간 초과는 나중에 다시 시도하라는 원인의 일시 정지 대화상자다") {
                resolve(stopped(ChunkFailure.ProcessingTimeout, saved = 1_000)) shouldBe
                    SaveOutcomeAction.ShowDialog(ImportDialog.Paused(saved = 1_000, reason = PauseReason.LATER))
            }

            test("응답 해석 실패는 나중에 다시 시도하라는 원인의 일시 정지 대화상자다") {
                resolve(stopped(ChunkFailure.InvalidResponse, saved = 1_000)) shouldBe
                    SaveOutcomeAction.ShowDialog(ImportDialog.Paused(saved = 1_000, reason = PauseReason.LATER))
            }

            test("알 수 없는 실패는 나중에 다시 시도하라는 원인의 일시 정지 대화상자다") {
                resolve(stopped(ChunkFailure.Unknown, saved = 1_000)) shouldBe
                    SaveOutcomeAction.ShowDialog(ImportDialog.Paused(saved = 1_000, reason = PauseReason.LATER))
            }

            test("서버가 거절한 확정 실패는 일부 저장 실패 대화상자다") {
                resolve(stopped(ChunkFailure.Rejected("C001"), saved = 1_000)) shouldBe
                    SaveOutcomeAction.ShowDialog(ImportDialog.PartialFailure(saved = 1_000))
            }

            test("키 충돌은 키 충돌 대화상자다") {
                resolve(stopped(ChunkFailure.KeyConflict, saved = 1_000)) shouldBe
                    SaveOutcomeAction.ShowDialog(ImportDialog.KeyConflict(saved = 1_000))
            }

            test("장수 불일치는 키 충돌 대화상자이고 서버가 만든 장수까지 저장 장수에 담는다") {
                resolve(stopped(ChunkFailure.ResponseCountMismatch(created = 300), saved = 1_300)) shouldBe
                    SaveOutcomeAction.ShowDialog(ImportDialog.KeyConflict(saved = 1_300))
            }

            test("덱을 찾을 수 없으면 덱 없음 대화상자다") {
                resolve(stopped(ChunkFailure.DeckUnavailable, saved = 1_000)) shouldBe
                    SaveOutcomeAction.ShowDialog(ImportDialog.DeckUnavailable)
            }

            test("사용자가 멈춘 결과는 대화상자 없이 부분 저장 덱 상세로 이동한다") {
                resolve(stopped(ChunkFailure.StoppedByUser, saved = 4_000)) shouldBe
                    SaveOutcomeAction.NavigateToDeckDetail(saved = 4_000, completed = false)
            }

            test("모두 저장했으면 완료로 덱 상세에 이동한다") {
                resolve(ImportSaveOutcome.Completed(savedCards = 9_940)) shouldBe
                    SaveOutcomeAction.NavigateToDeckDetail(saved = 9_940, completed = true)
            }

            test("저장 장수 0장인 일시 정지도 0장으로 대화상자에 담는다") {
                resolve(stopped(ChunkFailure.RetryExhausted, saved = 0)) shouldBe
                    SaveOutcomeAction.ShowDialog(ImportDialog.Paused(saved = 0, reason = PauseReason.NETWORK))
            }
        }

        context("이전 호출에서 저장한 장수") {
            test("대화상자의 저장 장수는 이전 호출에서 저장한 장수에 이번 호출의 장수를 더한다") {
                resolve(stopped(ChunkFailure.RetryExhausted, saved = 1_000), alreadySaved = 2_000) shouldBe
                    SaveOutcomeAction.ShowDialog(ImportDialog.Paused(saved = 3_000, reason = PauseReason.NETWORK))
            }

            test("완료 이동의 저장 장수도 이전 호출의 장수를 더한다") {
                resolve(ImportSaveOutcome.Completed(savedCards = 5_940), alreadySaved = 4_000) shouldBe
                    SaveOutcomeAction.NavigateToDeckDetail(saved = 9_940, completed = true)
            }

            test("부분 이동의 저장 장수도 이전 호출의 장수를 더한다") {
                resolve(stopped(ChunkFailure.StoppedByUser, saved = 0), alreadySaved = 4_000) shouldBe
                    SaveOutcomeAction.NavigateToDeckDetail(saved = 4_000, completed = false)
            }
        }

        context("멈추는 중의 저장 결과") {
            test("멈추는 중에 마지막 묶음까지 성공하면 완료로 처리해 덱 상세에 이동한다") {
                resolve(ImportSaveOutcome.Completed(savedCards = 9_940), isStopping = true) shouldBe
                    SaveOutcomeAction.NavigateToDeckDetail(saved = 9_940, completed = true)
            }

            test("멈추는 중에 덱을 찾을 수 없으면 덱 없음 대화상자다") {
                resolve(stopped(ChunkFailure.DeckUnavailable, saved = 1_000), isStopping = true) shouldBe
                    SaveOutcomeAction.ShowDialog(ImportDialog.DeckUnavailable)
            }

            test("멈추는 중에 덱 없음 외의 모든 실패는 대화상자 없이 부분 저장 덱 상세로 이동한다") {
                val failures =
                    listOf(
                        ChunkFailure.RetryExhausted,
                        ChunkFailure.ProcessingTimeout,
                        ChunkFailure.InvalidResponse,
                        ChunkFailure.Unknown,
                        ChunkFailure.Rejected("C001"),
                        ChunkFailure.KeyConflict,
                        ChunkFailure.ResponseCountMismatch(created = 1_000),
                        ChunkFailure.StoppedByUser,
                    )

                failures.forEach { failure ->
                    resolve(stopped(failure, saved = 1_000), isStopping = true) shouldBe
                        SaveOutcomeAction.NavigateToDeckDetail(saved = 1_000, completed = false)
                }
            }

            test("멈추는 중의 부분 이동도 이전 호출의 저장 장수를 더한다") {
                resolve(stopped(ChunkFailure.Unknown, saved = 500), isStopping = true, alreadySaved = 1_000) shouldBe
                    SaveOutcomeAction.NavigateToDeckDetail(saved = 1_500, completed = false)
            }
        }

        context("저장 중단 대화상자 본문") {
            test("네트워크 일시 정지는 저장한 장수와 네트워크 확인 문장이다") {
                saveDialogBody(ImportDialog.Paused(saved = 1_000, reason = PauseReason.NETWORK)) shouldBe
                    SaveDialogBody(SavedCountLine.SavedCards, CauseLine.CheckNetwork)
            }

            test("처리 시간 초과 같은 일시 정지는 잠시 뒤 다시 시도하라는 문장이다") {
                saveDialogBody(ImportDialog.Paused(saved = 1_000, reason = PauseReason.LATER)) shouldBe
                    SaveDialogBody(SavedCountLine.SavedCards, CauseLine.TryLater)
            }

            test("저장 0장인 네트워크 일시 정지는 저장한 카드가 없다는 문장과 네트워크 확인 문장이다") {
                saveDialogBody(ImportDialog.Paused(saved = 0, reason = PauseReason.NETWORK)) shouldBe
                    SaveDialogBody(SavedCountLine.NoCardsSaved, CauseLine.CheckNetwork)
            }

            test("저장 0장인 잠시 뒤 일시 정지는 저장한 카드가 없다는 문장과 잠시 뒤 문장이다") {
                saveDialogBody(ImportDialog.Paused(saved = 0, reason = PauseReason.LATER)) shouldBe
                    SaveDialogBody(SavedCountLine.NoCardsSaved, CauseLine.TryLater)
            }

            test("일부 저장 실패는 다시 시도하거나 저장한 카드로 덱을 볼 수 있다는 문장이다") {
                saveDialogBody(ImportDialog.PartialFailure(saved = 1_000)) shouldBe
                    SaveDialogBody(SavedCountLine.SavedCards, CauseLine.RetryOrViewDeck)
            }

            test("저장 0장인 일부 저장 실패는 다시 시도하거나 빈 덱을 볼 수 있다는 문장이다") {
                saveDialogBody(ImportDialog.PartialFailure(saved = 0)) shouldBe
                    SaveDialogBody(SavedCountLine.NoCardsSaved, CauseLine.RetryOrViewEmptyDeck)
            }

            test("키 충돌은 저장한 카드로 덱을 볼 수 있다는 문장이다") {
                saveDialogBody(ImportDialog.KeyConflict(saved = 1_000)) shouldBe
                    SaveDialogBody(SavedCountLine.SavedCards, CauseLine.ViewDeck)
            }

            test("저장 0장인 키 충돌은 빈 덱을 덱 상세에서 지울 수 있다는 문장이다") {
                saveDialogBody(ImportDialog.KeyConflict(saved = 0)) shouldBe
                    SaveDialogBody(SavedCountLine.NoCardsSaved, CauseLine.EmptyDeckDeletable)
            }

            test("저장 장수를 담지 않는 대화상자는 본문 규칙이 없다") {
                listOf(ImportDialog.DeckUnavailable, ImportDialog.StopConfirm, ImportDialog.DiscardConfirm, ImportDialog.DeckCreateFailed)
                    .forEach { dialog -> saveDialogBody(dialog) shouldBe null }
            }
        }
    }

    private fun stopped(
        failure: ChunkFailure,
        saved: Int,
    ) = ImportSaveOutcome.Stopped(chunkIndex = 1, savedCards = saved, failure = failure)

    private fun resolve(
        outcome: ImportSaveOutcome,
        alreadySaved: Int = 0,
        isStopping: Boolean = false,
    ): SaveOutcomeAction = resolveSaveOutcome(outcome, alreadySaved = alreadySaved, isStopping = isStopping)
}

package com.whatever.caro.feature.importer.mvi

import com.whatever.caro.core.importparser.ImportParseErrorKind
import com.whatever.caro.feature.importer.transform.ImportCard
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ImportDialogPolicyTest : FunSpec() {
    init {
        test("파일 크기 초과 팝업은 확인 정책이다") {
            ImportDialog.FileTooLarge(extension = "apkg").dismissPolicy shouldBe ImportDismissPolicy.Confirm
        }

        test("모든 오류 종류의 파싱 오류 팝업은 파일이든 붙여넣기든 확인 정책이다") {
            ImportParseErrorKind.entries.forEach { kind ->
                listOf(false, true).forEach { isPaste ->
                    ImportDialog.ParseError(kind = kind, extension = "txt", isPaste = isPaste).dismissPolicy shouldBe
                        ImportDismissPolicy.Confirm
                }
            }
        }

        test("빈 결과 팝업은 확인 정책이다") {
            ImportDialog.EmptyResult(isPaste = false).dismissPolicy shouldBe ImportDismissPolicy.Confirm
        }

        test("가져온 내용을 버릴지 묻는 팝업은 취소 정책이다") {
            ImportDialog.DiscardConfirm.dismissPolicy shouldBe ImportDismissPolicy.Cancel
        }

        test("덱 생성 실패 팝업은 취소 정책이다") {
            ImportDialog.DeckCreateFailed.dismissPolicy shouldBe ImportDismissPolicy.Cancel
        }

        test("가져오기를 멈출지 묻는 팝업은 계속 정책이다") {
            ImportDialog.StopConfirm.dismissPolicy shouldBe ImportDismissPolicy.Continue
        }

        test("저장 일시 정지 팝업은 닫히지 않음 정책이다") {
            ImportDialog.Paused(saved = 1_000, reason = PauseReason.entries.first()).dismissPolicy shouldBe
                ImportDismissPolicy.NotDismissible
        }

        test("일부 저장 실패 팝업은 닫히지 않음 정책이다") {
            ImportDialog.PartialFailure(saved = 1_000).dismissPolicy shouldBe ImportDismissPolicy.NotDismissible
        }

        test("키 충돌 팝업은 닫히지 않음 정책이다") {
            ImportDialog.KeyConflict(saved = 0).dismissPolicy shouldBe ImportDismissPolicy.NotDismissible
        }

        test("덱 없음 팝업은 닫히지 않음 정책이다") {
            ImportDialog.DeckUnavailable.dismissPolicy shouldBe ImportDismissPolicy.NotDismissible
        }

        test("카드 전체 보기는 뒤로 가기와 바깥 탭으로 닫힌다") {
            val policy = ImportDialog.CardDetail(card = card()).dismissPolicy

            policy.dismissOnBackPress shouldBe true
            policy.dismissOnClickOutside shouldBe true
        }

        test("확인, 취소, 계속 정책은 뒤로 가기와 바깥 탭으로 닫힌다") {
            listOf(ImportDismissPolicy.Confirm, ImportDismissPolicy.Cancel, ImportDismissPolicy.Continue).forEach { policy ->
                policy.dismissOnBackPress shouldBe true
                policy.dismissOnClickOutside shouldBe true
            }
        }

        test("닫히지 않음 정책은 뒤로 가기와 바깥 탭을 모두 막는다") {
            ImportDismissPolicy.NotDismissible.dismissOnBackPress shouldBe false
            ImportDismissPolicy.NotDismissible.dismissOnClickOutside shouldBe false
        }
    }

    private fun card() =
        ImportCard(
            sourceIndex = 0,
            front = "앞",
            back = "뒤",
            frontReplaced = false,
            backReplaced = false,
            frontTruncated = false,
            backTruncated = false,
            mediaRemoved = 0,
        )
}

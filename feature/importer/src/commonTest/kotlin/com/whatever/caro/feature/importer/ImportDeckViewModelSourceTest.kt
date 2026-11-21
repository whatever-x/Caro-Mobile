package com.whatever.caro.feature.importer

import app.cash.turbine.test
import com.whatever.caro.core.importparser.ImportParseErrorKind
import com.whatever.caro.feature.importer.logic.ParseErrorTitle
import com.whatever.caro.feature.importer.logic.parseErrorCopy
import com.whatever.caro.feature.importer.mvi.ImportDeckIntent
import com.whatever.caro.feature.importer.mvi.ImportDeckSideEffect
import com.whatever.caro.feature.importer.mvi.ImportDeckState
import com.whatever.caro.feature.importer.mvi.ImportDialog
import com.whatever.caro.feature.importer.mvi.ImportStep
import com.whatever.caro.feature.importer.platform.PickResult
import com.whatever.caro.feature.importer.support.FakeImportSourceReader
import com.whatever.caro.feature.importer.support.FakeImportTempStorage
import com.whatever.caro.feature.importer.support.FakePickedImportFile
import com.whatever.caro.feature.importer.support.ImportTestTables
import com.whatever.caro.feature.importer.support.ImportViewModelRig
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest

/** 가져오기 방법 고르기: 선택기 열기와 결과, 붙여넣기로 가기, 처음 단계의 뒤로 가기를 확인한다. */
class ImportDeckViewModelSourceTest : FunSpec() {
    private val rig = ImportViewModelRig()

    init {
        rig.installOn(this)

        test("Source에서 뒤로 가면 화면을 나가는 NavigateBack을 보낸다") {
            runTest(rig.dispatcher) {
                val harness = rig.create()

                harness.viewModel.sideEffect.test {
                    harness.send(ImportDeckIntent.ClickBack)
                    awaitItem() shouldBe ImportDeckSideEffect.NavigateBack
                }
            }
        }

        test("ClickPickFile을 연달아 보내도 선택기 열기를 한 번만 요청한다") {
            runTest(rig.dispatcher) {
                val harness = rig.create()

                harness.viewModel.sideEffect.test {
                    harness.send(ImportDeckIntent.ClickPickFile)
                    harness.send(ImportDeckIntent.ClickPickFile)
                    awaitItem() shouldBe ImportDeckSideEffect.OpenFilePicker
                    advanceUntilIdle()
                    expectNoEvents()
                }
            }
        }

        test("선택기가 열려 있는 동안 ClickPaste는 버리고 뒤로 가기는 화면을 나간다") {
            runTest(rig.dispatcher) {
                val harness = rig.create()
                harness.send(ImportDeckIntent.ClickPickFile)
                advanceUntilIdle()

                harness.send(ImportDeckIntent.ClickPaste)
                advanceUntilIdle()
                harness.state.step shouldBe ImportStep.Source

                harness.viewModel.sideEffect.test {
                    awaitItem() shouldBe ImportDeckSideEffect.OpenFilePicker
                    harness.send(ImportDeckIntent.ClickBack)
                    awaitItem() shouldBe ImportDeckSideEffect.NavigateBack
                }
            }
        }

        test("선택을 취소하면 선택기만 닫히고 나머지 상태는 처음 그대로다") {
            runTest(rig.dispatcher) {
                val harness = rig.create()
                harness.send(ImportDeckIntent.ClickPickFile)
                advanceUntilIdle()

                harness.pickResult(PickResult.Cancelled)
                advanceUntilIdle()

                harness.state shouldBe ImportDeckState()
            }
        }

        test("지원하지 않는 확장자의 TooLarge 결과는 크기 안내가 아니라 지원하지 않는 파일 대화상자다") {
            runTest(rig.dispatcher) {
                val harness = rig.create()

                harness.pickResult(PickResult.TooLarge(displayName = "big.xls", sizeBytes = 200L * MB))
                advanceUntilIdle()

                harness.state.dialog shouldBe ImportDialog.ParseError(ImportParseErrorKind.UNSUPPORTED_FILE, "xls", isPaste = false)
                harness.state.step shouldBe ImportStep.Source
                harness.reader.calls.shouldBeEmpty()
            }
        }

        test("지원하는 확장자의 TooLarge 결과는 파일 크기 초과 대화상자다") {
            runTest(rig.dispatcher) {
                val harness = rig.create()

                harness.pickResult(PickResult.TooLarge(displayName = "big.apkg", sizeBytes = 200L * MB))
                advanceUntilIdle()

                harness.state.dialog shouldBe ImportDialog.FileTooLarge("apkg")
                harness.state.isPickerOpen shouldBe false
                harness.reader.calls.shouldBeEmpty()
            }
        }

        test("선택기 실패는 파일을 읽지 못했다는 대화상자를 띄우고 Source에 머문다") {
            runTest(rig.dispatcher) {
                val harness = rig.create()

                harness.pickResult(PickResult.Failed(IllegalStateException("copy failed")))
                advanceUntilIdle()

                val dialog = harness.state.dialog.shouldBeInstanceOf<ImportDialog.ParseError>()
                parseErrorCopy(dialog).title shouldBe ParseErrorTitle.ReadFailedFile
                harness.state.step shouldBe ImportStep.Source
            }
        }

        test("ClickPaste는 Paste로 가고 Paste에서 뒤로 가면 화면을 나가지 않고 Source로 간다") {
            runTest(rig.dispatcher) {
                val harness = rig.create()
                harness.send(ImportDeckIntent.ClickPaste)
                advanceUntilIdle()
                harness.state.step shouldBe ImportStep.Paste
                harness.state.isPaste shouldBe true

                harness.viewModel.sideEffect.test {
                    harness.send(ImportDeckIntent.ClickBack)
                    advanceUntilIdle()
                    expectNoEvents()
                }

                harness.state.step shouldBe ImportStep.Source
                harness.state.isPaste shouldBe false
            }
        }

        test("미리보기에 온 선택 결과는 읽지 않고 핸들만 정리하며 미리보기를 유지한다") {
            runTest(rig.dispatcher) {
                val harness = rig.create(reader = FakeImportSourceReader().returns(ImportTestTables.text()))
                harness.pickNamed("words.txt")
                advanceUntilIdle()
                val before = harness.state
                val late = FakePickedImportFile.ofSize("again.txt", size = 100)

                harness.pick(late)
                harness.pickResult(PickResult.Cancelled)
                harness.pickResult(PickResult.TooLarge(displayName = "big.apkg", sizeBytes = 200L * MB))
                advanceUntilIdle()

                harness.state shouldBe before
                harness.state.step shouldBe ImportStep.Preview
                harness.reader.calls.size shouldBe 1
                late.discardCount shouldBe 1
            }
        }

        test("대화상자가 떠 있는 동안 온 파일은 읽지 않고 핸들만 정리한다") {
            runTest(rig.dispatcher) {
                val harness = rig.create()
                harness.pickResult(PickResult.TooLarge(displayName = "big.apkg", sizeBytes = 200L * MB))
                advanceUntilIdle()
                val late = FakePickedImportFile.ofSize("again.txt", size = 100)

                harness.pick(late)
                advanceUntilIdle()

                harness.state.dialog shouldBe ImportDialog.FileTooLarge("apkg")
                harness.state.loading shouldBe null
                harness.reader.calls.shouldBeEmpty()
                late.discardCount shouldBe 1
            }
        }

        test("ViewModel을 만들면 이전 실행이 남긴 다른 화면의 임시 디렉터리를 비운다") {
            runTest(rig.dispatcher) {
                val tempStorage = FakeImportTempStorage.create()
                val previous = tempStorage.plantPreviousSession()

                rig.create(tempStorage = tempStorage)
                advanceUntilIdle()

                tempStorage.exists(previous) shouldBe false
            }
        }
    }

    private companion object {
        const val MB = 1024L * 1024L
    }
}

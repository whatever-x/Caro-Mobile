package com.whatever.caro.feature.importer

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.cash.turbine.test
import com.whatever.caro.feature.importer.mvi.ImportDeckIntent
import com.whatever.caro.feature.importer.mvi.ImportDeckSideEffect
import com.whatever.caro.feature.importer.mvi.ImportDeckState
import com.whatever.caro.feature.importer.mvi.ImportDialog
import com.whatever.caro.feature.importer.mvi.ImportStep
import com.whatever.caro.feature.importer.support.FakeImportSourceReader
import com.whatever.caro.feature.importer.support.FakeImportTempStorage
import com.whatever.caro.feature.importer.support.FakePickedImportFile
import com.whatever.caro.feature.importer.support.GatedDispatcher
import com.whatever.caro.feature.importer.support.ImportTestTables
import com.whatever.caro.feature.importer.support.ImportViewModelHarness
import com.whatever.caro.feature.importer.support.ImportViewModelRig
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

/** 선택 단계의 뒤로 가기와 버림 확인, 중복 입력 무시, 임시 파일 정리 시점을 확인한다. */
class ImportDeckViewModelBackTest : FunSpec() {
    private val rig = ImportViewModelRig()
    private val modernSummary by lazy { ImportTestTables.apkgSummary("modern_anki21b_main.apkg") }

    private class SelectionStage(
        val label: String,
        val step: ImportStep,
        val open: TestScope.() -> ImportViewModelHarness,
    )

    init {
        rig.installOn(this)

        listOf(
            SelectionStage(
                "SubdeckSelect",
                ImportStep.SubdeckSelect,
            ) { picked("deck.apkg", FakeImportSourceReader().returns(modernSummary)) },
            SelectionStage(
                "SheetSelect",
                ImportStep.SheetSelect,
            ) { picked("book.xlsx", FakeImportSourceReader().returns(ImportTestTables.xlsx())) },
            SelectionStage(
                "파일의 TextCheck",
                ImportStep.TextCheck,
            ) { picked("words.txt", FakeImportSourceReader().returns(ImportTestTables.text())) },
        ).forEach { stage ->
            test("${stage.label}에서 뒤로 가면 단계를 유지한 채 DiscardConfirm 대화상자를 띄운다") {
                runTest(rig.dispatcher) {
                    val harness = stage.open(this)

                    harness.send(ImportDeckIntent.ClickBack)
                    advanceUntilIdle()

                    harness.state.dialog shouldBe ImportDialog.DiscardConfirm
                    harness.state.step shouldBe stage.step
                    harness.state.table.shouldNotBeNull()
                }
            }

            test("${stage.label}의 버림 확인을 승인하면 읽은 내용과 임시 파일을 비우고 Source로 간다") {
                runTest(rig.dispatcher) {
                    val harness = stage.open(this)
                    harness.send(ImportDeckIntent.ClickBack)
                    advanceUntilIdle()
                    harness.state.dialog shouldBe ImportDialog.DiscardConfirm

                    harness.send(ImportDeckIntent.ConfirmDiscard)
                    advanceUntilIdle()

                    harness.state shouldBe ImportDeckState()
                    harness.tempStorage.regularFiles().shouldBeEmpty()
                }
            }

            test("${stage.label}의 버림 확인을 취소하면 단계와 읽은 내용을 유지한다") {
                runTest(rig.dispatcher) {
                    val harness = stage.open(this)
                    val tableBefore = harness.state.table
                    harness.send(ImportDeckIntent.ClickBack)
                    advanceUntilIdle()

                    harness.send(ImportDeckIntent.DismissDialog)
                    advanceUntilIdle()

                    harness.state.dialog shouldBe null
                    harness.state.step shouldBe stage.step
                    harness.state.table shouldBe tableBefore
                }
            }
        }

        test("Source에서 뒤로 가면 화면을 나가는 NavigateBack을 보낸다") {
            runTest(rig.dispatcher) {
                val harness = rig.create()

                harness.viewModel.sideEffect.test {
                    harness.send(ImportDeckIntent.ClickBack)
                    awaitItem() shouldBe ImportDeckSideEffect.NavigateBack
                }
            }
        }

        test("읽는 중에는 뒤로 가기와 대화상자 닫기를 뺀 입력을 무시한다") {
            runTest(rig.dispatcher) {
                val reader = FakeImportSourceReader().returns(modernSummary)
                reader.suspends()
                val harness = picked("deck.apkg", reader)
                harness.send(ImportDeckIntent.ClickSelectionNext)
                advanceUntilIdle()
                val stateBefore = harness.state

                harness.viewModel.sideEffect.test {
                    harness.send(ImportDeckIntent.ToggleSubdeck("1791602016834"))
                    harness.send(ImportDeckIntent.ToggleAllSubdecks)
                    harness.send(ImportDeckIntent.ClickSelectionNext)
                    harness.send(ImportDeckIntent.SelectSheet(1))
                    harness.send(ImportDeckIntent.ClickPickFile)
                    harness.send(ImportDeckIntent.ClickPaste)
                    advanceUntilIdle()
                    expectNoEvents()
                }

                harness.state shouldBe stateBefore
                harness.reader.calls.size shouldBe 2
            }
        }

        test("선택기가 열려 있는 동안 ClickPaste는 무시하고 뒤로 가기는 처리한다") {
            runTest(rig.dispatcher) {
                val harness = rig.create()
                harness.send(ImportDeckIntent.ClickPickFile)
                advanceUntilIdle()

                harness.send(ImportDeckIntent.ClickPaste)
                advanceUntilIdle()
                harness.state.step shouldBe ImportStep.Source
                harness.state.isPickerOpen shouldBe true

                harness.viewModel.sideEffect.test {
                    awaitItem() shouldBe ImportDeckSideEffect.OpenFilePicker
                    harness.send(ImportDeckIntent.ClickBack)
                    awaitItem() shouldBe ImportDeckSideEffect.NavigateBack
                }
            }
        }

        test("ViewModel을 만들 때 이전 실행이 남긴 다른 화면의 임시 디렉터리를 비운다") {
            runTest(rig.dispatcher) {
                val tempStorage = FakeImportTempStorage.create()
                val previous = tempStorage.plantPreviousSession()

                rig.create(tempStorage = tempStorage)
                advanceUntilIdle()

                tempStorage.exists(previous) shouldBe false
            }
        }

        test("ViewModel을 만드는 호출에서는 디스크 정리를 하지 않고 io 디스패처가 일할 때 한다") {
            runTest(rig.dispatcher) {
                val io = GatedDispatcher(rig.dispatcher)
                io.hold()
                val tempStorage = FakeImportTempStorage.create()
                val previous = tempStorage.plantPreviousSession()

                rig.create(tempStorage = tempStorage, ioDispatcher = io)
                runCurrent()

                tempStorage.exists(previous) shouldBe true

                io.release()
                advanceUntilIdle()

                tempStorage.exists(previous) shouldBe false
            }
        }

        test("ViewModelStore를 비우면 onCleared가 io 디스패처에서 이 화면의 디렉터리만 비운다") {
            runTest(rig.dispatcher) {
                val io = GatedDispatcher(rig.dispatcher)
                val harness = rig.create(ioDispatcher = io)
                val store = ViewModelStore()
                ViewModelProvider.create(store, viewModelFactory { initializer { harness.viewModel } })[ImportDeckViewModel::class]
                advanceUntilIdle()
                harness.tempStorage.plantLeftover()
                val otherScreen = harness.tempStorage.plantPreviousSession()
                io.hold()

                store.clear()
                runCurrent()

                harness.tempStorage.regularFiles().isEmpty() shouldBe false

                io.release()
                advanceUntilIdle()

                harness.tempStorage.regularFiles().shouldBeEmpty()
                harness.tempStorage.exists(otherScreen) shouldBe true
            }
        }
    }

    private fun TestScope.picked(
        fileName: String,
        reader: FakeImportSourceReader,
    ): ImportViewModelHarness {
        val harness = rig.create(reader = reader)
        harness.pick(FakePickedImportFile.ofSize(fileName, size = 100))
        advanceUntilIdle()
        return harness
    }
}

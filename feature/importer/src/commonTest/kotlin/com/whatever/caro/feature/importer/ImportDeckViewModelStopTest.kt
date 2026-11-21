package com.whatever.caro.feature.importer

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.cash.turbine.test
import com.whatever.caro.feature.importer.mvi.ImportDeckIntent
import com.whatever.caro.feature.importer.mvi.ImportDeckState
import com.whatever.caro.feature.importer.mvi.ImportLoading
import com.whatever.caro.feature.importer.mvi.ImportStep
import com.whatever.caro.feature.importer.support.FakeImportSourceReader
import com.whatever.caro.feature.importer.support.FakePickedImportFile
import com.whatever.caro.feature.importer.support.GatedDispatcher
import com.whatever.caro.feature.importer.support.ImportTestTables
import com.whatever.caro.feature.importer.support.ImportViewModelRig
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

/** 처리 중 뒤로 가기: 그 처리만 멈추고 처음 또는 붙여넣기로 가며, 멈춘 뒤에는 이어 읽거나 미리보기를 만들지 않는다. */
@OptIn(ExperimentalCoroutinesApi::class)
class ImportDeckViewModelStopTest : FunSpec() {
    private val rig = ImportViewModelRig()
    private val ankiSummary by lazy { ImportTestTables.apkgSummary("modern_anki21b_main.apkg") }

    init {
        rig.installOn(this)

        test("파일 첫 읽기 중 뒤로 가기는 읽기를 멈추고 임시 파일을 지운 뒤 Source로 가며 늦은 결과를 버린다") {
            runTest(rig.dispatcher) {
                val reader = FakeImportSourceReader()
                val gate = reader.suspendsIgnoringCancellation()
                val harness = rig.create(reader = reader)
                val file = FakePickedImportFile.ofSize("deck.apkg", size = 20_000)
                harness.pick(file)
                advanceUntilIdle()
                harness.tempStorage.regularFiles().size shouldBe 1

                harness.viewModel.sideEffect.test {
                    harness.send(ImportDeckIntent.ClickBack)
                    advanceUntilIdle()
                    expectNoEvents()
                }
                gate.complete(ankiSummary)
                advanceUntilIdle()

                harness.state shouldBe ImportDeckState()
                harness.reader.calls.size shouldBe 1
                harness.tempStorage.regularFiles().shouldBeEmpty()
                file.discardCount shouldBeGreaterThan 0
            }
        }

        test("파일을 고른 직후 읽기가 시작되기 전에 뒤로 가도 선택기 사본을 정리하고 임시 파일을 남기지 않는다") {
            runTest(rig.dispatcher) {
                val harness = rig.create()
                advanceUntilIdle()
                val file = FakePickedImportFile.ofSize("words.txt", size = 100)

                harness.pick(file)
                harness.send(ImportDeckIntent.ClickBack)
                advanceUntilIdle()

                harness.state shouldBe ImportDeckState()
                file.discardCount shouldBeGreaterThan 0
                harness.tempStorage.regularFiles().shouldBeEmpty()
            }
        }

        test("파일을 읽는 중 들어온 파일 고르기와 붙여넣기 입력은 버린다") {
            runTest(rig.dispatcher) {
                val reader = FakeImportSourceReader()
                val gate = reader.suspends()
                val harness = rig.create(reader = reader)
                harness.pickNamed("words.txt")
                advanceUntilIdle()
                val reading = harness.state

                harness.viewModel.sideEffect.test {
                    harness.send(ImportDeckIntent.ClickPickFile)
                    harness.send(ImportDeckIntent.ClickPaste)
                    advanceUntilIdle()
                    expectNoEvents()
                }

                harness.state shouldBe reading
                gate.complete(ImportTestTables.text())
                advanceUntilIdle()
            }
        }

        test("붙여넣은 글을 읽는 중 들어온 글 수정은 버린다") {
            runTest(rig.dispatcher) {
                val reader = FakeImportSourceReader()
                val gate = reader.suspends()
                val harness = rig.create(reader = reader)
                harness.pasteNext("a\tb")
                advanceUntilIdle()

                harness.send(ImportDeckIntent.UpdatePasteText("changed"))
                advanceUntilIdle()

                harness.state.pasteText shouldBe "a\tb"
                gate.complete(ImportTestTables.text())
                advanceUntilIdle()
            }
        }

        test("파일 이름을 받기 전에 뒤로 가도 화면을 나가지 않고 읽기를 멈춰 Source에 머문다") {
            runTest(rig.dispatcher) {
                val io = GatedDispatcher(rig.dispatcher)
                val harness = rig.create(io = io)
                val file = FakePickedImportFile.ofSize("words.txt", size = 100)
                advanceUntilIdle()
                io.hold()
                harness.pick(file)
                runCurrent()

                harness.viewModel.sideEffect.test {
                    harness.send(ImportDeckIntent.ClickBack)
                    runCurrent()
                    io.release()
                    advanceUntilIdle()
                    expectNoEvents()
                }

                harness.state shouldBe ImportDeckState()
                harness.reader.calls.shouldBeEmpty()
                file.discardCount shouldBe 1
                harness.tempStorage.regularFiles().shouldBeEmpty()
            }
        }

        test("붙여넣기 읽기 중 뒤로 가기는 읽기를 멈추고 입력을 유지한 채 Paste로 간다") {
            runTest(rig.dispatcher) {
                val reader = FakeImportSourceReader()
                val gate = reader.suspends()
                val harness = rig.create(reader = reader)
                harness.pasteNext("a\tb")
                advanceUntilIdle()

                harness.send(ImportDeckIntent.ClickBack)
                advanceUntilIdle()
                gate.complete(ImportTestTables.text())
                advanceUntilIdle()

                harness.state.step shouldBe ImportStep.Paste
                harness.state.loading shouldBe null
                harness.state.pasteText shouldBe "a\tb"
                harness.state.table shouldBe null
                harness.reader.cancelledReads shouldBe 1
            }
        }

        test("Anki 하위 덱 행을 다시 읽는 중 뒤로 가면 늦게 온 행으로 미리보기를 만들지 않는다") {
            runTest(rig.dispatcher) {
                val reader = FakeImportSourceReader().returns(ankiSummary)
                val gate = reader.suspendsIgnoringCancellation()
                val harness = rig.create(reader = reader)
                harness.pickNamed("deck.apkg")
                advanceUntilIdle()

                harness.send(ImportDeckIntent.ClickBack)
                advanceUntilIdle()
                gate.complete(ImportTestTables.apkg("modern_anki21b_main.apkg", "default"))
                advanceUntilIdle()

                harness.state shouldBe ImportDeckState()
                harness.reader.calls.size shouldBe 2
                harness.tempStorage.regularFiles().shouldBeEmpty()
            }
        }

        test("엑셀 후보 시트를 다시 읽는 중 뒤로 가면 늦게 온 결과가 0행이어도 다음 후보를 읽지 않는다") {
            runTest(rig.dispatcher) {
                val reader = FakeImportSourceReader().returns(ImportTestTables.xlsx(sheetRows = listOf(0, 2, 3)))
                val gate = reader.suspendsIgnoringCancellation()
                val harness = rig.create(reader = reader)
                harness.pickNamed("book.xlsx")
                advanceUntilIdle()

                harness.send(ImportDeckIntent.ClickBack)
                advanceUntilIdle()
                gate.complete(ImportTestTables.xlsx(sheetRows = listOf(0, 0, 3), selectedSheet = 1))
                advanceUntilIdle()

                harness.state shouldBe ImportDeckState()
                harness.reader.calls.size shouldBe 2
            }
        }

        test("첫 읽기 결과가 전달되는 사이에 뒤로 가면 하위 덱 행을 이어 읽지 않는다") {
            runTest(rig.dispatcher) {
                // 읽기 결과를 받는 쪽의 재개를 잡아 두어 결과가 도착한 뒤 뒤로 가기가 먼저 처리되는 순간을 만든다.
                val main = GatedDispatcher(rig.dispatcher)
                Dispatchers.setMain(main)
                val reader = FakeImportSourceReader()
                val gate = reader.suspends()
                val harness = rig.create(reader = reader)
                harness.pickNamed("deck.apkg")
                advanceUntilIdle()

                main.hold()
                gate.complete(ankiSummary)
                advanceUntilIdle()
                main.release()
                main.hold()
                runCurrent()
                main.passNewKeepingHeld()
                harness.send(ImportDeckIntent.ClickBack)
                runCurrent()
                main.release()
                advanceUntilIdle()

                harness.state shouldBe ImportDeckState()
                harness.reader.calls.size shouldBe 1
            }
        }

        test("미리보기를 만드는 중 뒤로 가면 미리보기로 가지 않고 Source에 머문다") {
            runTest(rig.dispatcher) {
                val default = GatedDispatcher(rig.dispatcher)
                val harness = rig.create(reader = FakeImportSourceReader().returns(ImportTestTables.text()), default = default)
                default.hold()
                harness.pickNamed("words.txt")
                advanceUntilIdle()
                harness.state.loading shouldBe ImportLoading.ReadingFile("words.txt")

                harness.send(ImportDeckIntent.ClickBack)
                advanceUntilIdle()
                default.release()
                advanceUntilIdle()

                harness.state shouldBe ImportDeckState()
                harness.tempStorage.regularFiles().shouldBeEmpty()
            }
        }

        test("읽기를 멈춘 뒤 새 파일을 고르면 앞 읽기의 늦은 결과가 새 미리보기를 덮지 않는다") {
            runTest(rig.dispatcher) {
                val reader = FakeImportSourceReader()
                val gate = reader.suspendsIgnoringCancellation()
                reader.returns(ImportTestTables.text(sourceName = "second.txt", rows = 2))
                val harness = rig.create(reader = reader)
                harness.pickNamed("first.txt")
                advanceUntilIdle()
                harness.send(ImportDeckIntent.ClickBack)
                advanceUntilIdle()

                harness.pickNamed("second.txt")
                advanceUntilIdle()
                gate.complete(ImportTestTables.text(sourceName = "first.txt", rows = 5))
                advanceUntilIdle()

                harness.state.step shouldBe ImportStep.Preview
                harness.state.table
                    ?.sourceName shouldBe "second.txt"
                harness.state.deckName shouldBe "second"
                harness.reader.calls.size shouldBe 2
            }
        }

        test("ViewModelStore를 비우면 읽기를 멈추고 이 화면의 임시 파일을 지운다") {
            runTest(rig.dispatcher) {
                val reader = FakeImportSourceReader()
                reader.suspends()
                val harness = rig.create(reader = reader)
                val store = ViewModelStore()
                ViewModelProvider.create(store, viewModelFactory { initializer { harness.viewModel } })[ImportDeckViewModel::class]
                harness.pick(FakePickedImportFile.ofSize("deck.apkg", size = 20_000))
                advanceUntilIdle()
                harness.tempStorage.regularFiles().size shouldBe 1

                store.clear()
                advanceUntilIdle()

                harness.reader.cancelledReads shouldBe 1
                harness.tempStorage.regularFiles().shouldBeEmpty()
            }
        }
    }
}

package com.whatever.caro.feature.importer.source

import com.whatever.caro.feature.importer.support.FakeImportSourceReader
import com.whatever.caro.feature.importer.support.FakePickedImportFile
import com.whatever.caro.feature.importer.support.GatedDispatcher
import com.whatever.caro.feature.importer.support.ImportTestTables
import com.whatever.caro.feature.importer.support.ReadCall
import com.whatever.caro.feature.importer.support.ReadControllerHarness
import com.whatever.caro.feature.importer.support.ReadControllerRig
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import okio.ByteString.Companion.encodeUtf8

/** 취소에 바로 응하지 않는 앞선 읽기와 늦게 도는 임시 파일 정리가 있어도 읽기가 겹치지 않고, 새로 고른 파일의 사본이 지워지지 않는다. */
@OptIn(ExperimentalCoroutinesApi::class)
class ImportReadControllerOrderTest : FunSpec() {
    private val rig = ReadControllerRig()

    init {
        rig.installOn(this)

        test("앞선 읽기의 정리가 새 파일의 첫 읽기보다 늦게 돌아도 새 사본이 남고 다시 읽기가 성공한다") {
            runTest(rig.dispatcher) {
                val reader = rig.realParserReader()
                val firstGate = reader.suspendsIgnoringCancellation()
                val io = GatedDispatcher(rig.dispatcher)
                val harness = rig.create(reader = reader, io = io)
                val first = harness.pick(FakePickedImportFile.ofSize("a.txt", size = 100))
                advanceUntilIdle()

                io.hold()
                harness.controller.discard()
                runCurrent()
                io.passNewKeepingHeld()

                val next = splittableTextFile()
                val second = harness.pick(next)
                advanceUntilIdle()

                firstGate.complete(ImportTestTables.text())
                advanceUntilIdle()
                io.release()
                advanceUntilIdle()

                first.await() shouldBe ImportReadResult.Cancelled
                second.await().shouldBeInstanceOf<ImportReadResult.Success>()
                shouldKeepCopyAndReread(harness, next)
            }
        }

        test("정리가 둘 밀려 있어도 새 파일의 사본은 둘 다 끝난 뒤에 만든다") {
            runTest(rig.dispatcher) {
                val reader = rig.realParserReader()
                val firstGate = reader.suspendsIgnoringCancellation()
                val harness = rig.create(reader = reader)
                harness.pick(FakePickedImportFile.ofSize("first.txt", size = 100))
                advanceUntilIdle()
                harness.controller.discard()
                advanceUntilIdle()

                harness.pick(FakePickedImportFile.ofSize("second.txt", size = 100))
                advanceUntilIdle()
                harness.controller.discard()
                advanceUntilIdle()

                val third = splittableTextFile()
                val result = harness.pick(third)
                advanceUntilIdle()

                firstGate.complete(ImportTestTables.text())
                advanceUntilIdle()

                result.await().shouldBeInstanceOf<ImportReadResult.Success>()
                shouldKeepCopyAndReread(harness, third)
            }
        }

        test("앞선 읽기를 기다리다 멈춘 파일 선택도 앞선 읽기가 끝나면 선택기 사본을 정리한다") {
            runTest(rig.dispatcher) {
                val reader = FakeImportSourceReader()
                val firstGate = reader.suspendsIgnoringCancellation()
                val harness = rig.create(reader = reader)
                harness.pick(FakePickedImportFile.ofSize("first.txt", size = 100))
                advanceUntilIdle()
                harness.controller.discard()
                advanceUntilIdle()

                val waiting = FakePickedImportFile.ofSize("second.txt", size = 100)
                val result = harness.pick(waiting)
                advanceUntilIdle()
                harness.controller.discard()
                advanceUntilIdle()
                waiting.discardCount shouldBe 0

                firstGate.complete(ImportTestTables.text())
                advanceUntilIdle()

                waiting.discardCount shouldBe 1
                harness.reader.calls.size shouldBe 1
                result.await() shouldBe ImportReadResult.Cancelled
                harness.controller.source shouldBe null
            }
        }

        test("읽기 작업이 시작하기 전에 멈춘 파일 선택도 앞선 읽기가 끝나면 선택기 사본을 정리한다") {
            runTest(rig.dispatcher) {
                val main = GatedDispatcher(rig.dispatcher)
                val reader = FakeImportSourceReader()
                val firstGate = reader.suspendsIgnoringCancellation()
                val harness = rig.create(reader = reader, scope = rig.newScope(main), callerScope = rig.newScope())
                harness.pick(FakePickedImportFile.ofSize("first.txt", size = 100))
                advanceUntilIdle()

                main.hold()
                val waiting = FakePickedImportFile.ofSize("second.txt", size = 100)
                val result = harness.pick(waiting)
                advanceUntilIdle()
                harness.controller.discard()
                main.release()
                advanceUntilIdle()
                waiting.discardCount shouldBe 0

                firstGate.complete(ImportTestTables.text())
                advanceUntilIdle()

                waiting.discardCount shouldBe 1
                harness.reader.calls.size shouldBe 1
                result.await() shouldBe ImportReadResult.Cancelled
                harness.controller.source shouldBe null
            }
        }

        test("멈춘 뒤 다시 시작한 읽기는 취소에 응하지 않는 앞선 읽기가 끝나야 시작한다") {
            runTest(rig.dispatcher) {
                val pasted = ImportTestTables.text(sourceName = ReadControllerHarness.PASTE_SOURCE_NAME)
                val reader = FakeImportSourceReader()
                val firstGate = reader.suspendsIgnoringCancellation()
                reader.returns(pasted)
                val harness = rig.create(reader = reader)
                val first = harness.paste("a\tb")
                advanceUntilIdle()
                harness.controller.discard()
                advanceUntilIdle()

                val second = harness.paste("a\tb")
                advanceUntilIdle()

                harness.reader.calls.size shouldBe 1

                firstGate.complete(pasted)
                advanceUntilIdle()

                harness.reader.calls.size shouldBe 2
                first.await() shouldBe ImportReadResult.Cancelled
                second.await() shouldBe ImportReadResult.Success(pasted)
            }
        }

        test("설정을 연달아 바꿔 기다리던 다시 읽기가 취소돼도 마지막 다시 읽기는 취소에 응하지 않는 앞선 읽기가 끝나야 시작한다") {
            runTest(rig.dispatcher) {
                val finalTable = ImportTestTables.text(rows = 6, delimiter = ";")
                val reader = FakeImportSourceReader().returns(ImportTestTables.text())
                val blockedGate = reader.suspendsIgnoringCancellation()
                reader.returns(finalTable)
                val harness = rig.create(reader = reader)
                harness.pick(FakePickedImportFile.ofSize("words.txt", size = 100))
                advanceUntilIdle()
                val comma = harness.reread(ImportReadStage.Draft, delimiter = ',')
                runCurrent()
                harness.reader.calls.size shouldBe 2
                val tab = harness.reread(ImportReadStage.Draft, delimiter = '\t')
                runCurrent()

                val semicolon = harness.reread(ImportReadStage.Draft, delimiter = ';')
                advanceUntilIdle()

                harness.reader.calls.size shouldBe 2

                blockedGate.complete(ImportTestTables.text())
                advanceUntilIdle()

                harness.reader.calls.size shouldBe 3
                harness.reader.calls
                    .last()
                    .shouldBeInstanceOf<ReadCall.FileRead>()
                    .delimiter shouldBe ';'
                comma.await() shouldBe ImportReadResult.Cancelled
                tab.await() shouldBe ImportReadResult.Cancelled
                semicolon.await() shouldBe ImportReadResult.Success(finalTable)
            }
        }

        test("설정을 사이 없이 연달아 바꿔 시작하기 전에 취소된 다시 읽기가 있어도 마지막 다시 읽기는 취소에 응하지 않는 앞선 읽기가 끝나야 시작한다") {
            runTest(rig.dispatcher) {
                val finalTable = ImportTestTables.text(rows = 6, delimiter = ";")
                val reader = FakeImportSourceReader().returns(ImportTestTables.text())
                val blockedGate = reader.suspendsIgnoringCancellation()
                reader.returns(finalTable)
                val harness = rig.create(reader = reader)
                harness.pick(FakePickedImportFile.ofSize("words.txt", size = 100))
                advanceUntilIdle()
                harness.reread(ImportReadStage.Draft, delimiter = ',')
                runCurrent()
                harness.reader.calls.size shouldBe 2

                val tab = harness.reread(ImportReadStage.Draft, delimiter = '\t')
                val semicolon = harness.reread(ImportReadStage.Draft, delimiter = ';')
                advanceUntilIdle()

                harness.reader.calls.size shouldBe 2

                blockedGate.complete(ImportTestTables.text())
                advanceUntilIdle()

                harness.reader.calls.size shouldBe 3
                tab.await() shouldBe ImportReadResult.Cancelled
                semicolon.await() shouldBe ImportReadResult.Success(finalTable)
            }
        }

        test("읽기를 멈춰도 그 읽기가 끝나기 전에는 임시 사본을 지우지 않고 끝나면 지운다") {
            runTest(rig.dispatcher) {
                val reader = FakeImportSourceReader()
                val gate = reader.suspendsIgnoringCancellation()
                val harness = rig.create(reader = reader)
                harness.pick(FakePickedImportFile.ofSize("words.txt", size = 100))
                advanceUntilIdle()
                harness.reader.calls.size shouldBe 1
                harness.tempStorage.regularFiles().size shouldBe 1

                harness.controller.discard()
                advanceUntilIdle()

                harness.tempStorage.regularFiles().size shouldBe 1

                gate.complete(ImportTestTables.text())
                advanceUntilIdle()

                harness.tempStorage.regularFiles().shouldBeEmpty()
            }
        }
    }

    // 첫 읽기가 탭과 쉼표 중 어느 구분자를 고르든 두 칸 이상으로 나뉘어야, 다시 읽기가 실패하는 원인이 사본 삭제뿐이다.
    private fun splittableTextFile(): FakePickedImportFile =
        FakePickedImportFile(name = "words.txt", content = "front,back\tmemo\n사과,apple\t과일\n".encodeUtf8())

    private suspend fun TestScope.shouldKeepCopyAndReread(
        harness: ReadControllerHarness,
        picked: FakePickedImportFile,
    ) {
        harness.tempStorage.regularFiles().size shouldBe 1

        val reread = harness.reread(ImportReadStage.Draft, delimiter = ',')
        advanceUntilIdle()

        reread
            .await()
            .shouldBeInstanceOf<ImportReadResult.Success>()
            .table.layout
            .shouldNotBeNull()
            .delimiter shouldBe ","
        harness.reader.calls
            .last()
            .shouldBeInstanceOf<ReadCall.FileRead>()
            .copiedBytes shouldBe picked.content
    }
}

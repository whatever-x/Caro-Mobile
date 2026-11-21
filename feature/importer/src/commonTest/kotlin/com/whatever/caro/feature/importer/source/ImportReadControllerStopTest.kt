package com.whatever.caro.feature.importer.source

import com.whatever.caro.core.importparser.ImportParseErrorKind
import com.whatever.caro.core.importparser.ImportParseException
import com.whatever.caro.feature.importer.support.FakeImportSourceReader
import com.whatever.caro.feature.importer.support.FakeImportTempStorage
import com.whatever.caro.feature.importer.support.FakePickedImportFile
import com.whatever.caro.feature.importer.support.GatedDispatcher
import com.whatever.caro.feature.importer.support.ImportTestTables
import com.whatever.caro.feature.importer.support.ReadControllerHarness
import com.whatever.caro.feature.importer.support.ReadControllerRig
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import okio.FileSystem
import okio.ForwardingFileSystem
import okio.Path

/** 멈추는 시점마다 결과와 원본, 임시 파일이 어떻게 남는지와, 블로킹 작업과 상태 변경이 도는 디스패처를 확인한다. */
@OptIn(ExperimentalCoroutinesApi::class)
class ImportReadControllerStopTest : FunSpec() {
    private val rig = ReadControllerRig()

    init {
        rig.installOn(this)

        test("복사가 끝나 읽기로 넘어가기 전에 원본을 버리면 읽지 않고 사본과 선택기 사본을 정리한다") {
            runTest(rig.dispatcher) {
                val main = GatedDispatcher(rig.dispatcher)
                val harness = rig.create(scope = rig.newScope(main), callerScope = rig.newScope())
                // 블로킹 복사 도중에는 scope의 디스패처에서 공개 함수를 부를 수 없으므로, 복사를 마치고 scope로 돌아오는 작업을 잡아 둔 사이에 부른다.
                val file = FakePickedImportFile.ofSize("deck.apkg", size = 20_000, onRead = { main.hold() })
                val result = harness.pick(file)
                advanceUntilIdle()
                harness.tempStorage.regularFiles().size shouldBe 1

                harness.controller.discard()
                main.release()
                advanceUntilIdle()

                result.await() shouldBe ImportReadResult.Cancelled
                harness.tempStorage.regularFiles().shouldBeEmpty()
                harness.reader.calls.shouldBeEmpty()
                file.discardCount shouldBe 1
                harness.controller.source shouldBe null
            }
        }

        test("복사가 끝나 읽기로 넘어가기 전에 멈추면 그 사본을 원본으로 남기지 않고 지운다") {
            runTest(rig.dispatcher) {
                val main = GatedDispatcher(rig.dispatcher)
                val harness = rig.create(scope = rig.newScope(main), callerScope = rig.newScope())
                val file = FakePickedImportFile.ofSize("words.txt", size = 100, onRead = { main.hold() })
                val result = harness.pick(file)
                advanceUntilIdle()
                harness.tempStorage.regularFiles().size shouldBe 1

                harness.controller.stop()
                main.release()
                advanceUntilIdle()

                result.await() shouldBe ImportReadResult.Cancelled
                harness.controller.source shouldBe null
                harness.tempStorage.regularFiles().shouldBeEmpty()
                harness.reader.calls.shouldBeEmpty()
            }
        }

        test("첫 읽기 중 원본을 버리면 읽기를 취소하고 늦은 결과를 버린 뒤 임시 파일을 지운다") {
            runTest(rig.dispatcher) {
                val reader = FakeImportSourceReader()
                val gate = reader.suspends()
                val harness = rig.create(reader = reader)
                val result = harness.pick(FakePickedImportFile.ofSize("deck.apkg", size = 20_000))
                advanceUntilIdle()
                harness.tempStorage.regularFiles().size shouldBe 1

                harness.controller.discard()
                gate.complete(ImportTestTables.apkgSummary("modern_anki21b_main.apkg"))
                advanceUntilIdle()

                result.await() shouldBe ImportReadResult.Cancelled
                harness.reader.cancelledReads shouldBe 1
                harness.controller.source shouldBe null
                harness.tempStorage.regularFiles().shouldBeEmpty()
            }
        }

        test("다시 읽기를 멈추고 원본을 남기면 사본이 남아 이어서 다시 읽는다") {
            runTest(rig.dispatcher) {
                val reader = FakeImportSourceReader().returns(ImportTestTables.text())
                reader.suspends()
                reader.returns(ImportTestTables.text(delimiter = ";"))
                val harness = rig.create(reader = reader)
                harness.pick(FakePickedImportFile.ofSize("words.txt", size = 100))
                advanceUntilIdle()
                val stopped = harness.reread(ImportReadStage.Draft, delimiter = ',')
                advanceUntilIdle()

                harness.controller.stop()
                advanceUntilIdle()
                val next = harness.reread(ImportReadStage.Draft, delimiter = ';')
                advanceUntilIdle()

                stopped.await() shouldBe ImportReadResult.Cancelled
                harness.reader.cancelledReads shouldBe 1
                next.await().shouldBeInstanceOf<ImportReadResult.Success>()
                harness.tempStorage.regularFiles().size shouldBe 1
            }
        }

        test("읽기가 끝난 뒤 호출한 쪽이 결과를 받기 전에 멈추면 그 결과를 버린다") {
            runTest(rig.dispatcher) {
                val caller = GatedDispatcher(rig.dispatcher)
                val reader = FakeImportSourceReader()
                val gate = reader.suspends()
                val harness = rig.create(reader = reader, callerScope = rig.newScope(caller))
                val result = harness.paste("a\tb")
                advanceUntilIdle()

                caller.hold()
                gate.complete(ImportTestTables.text())
                advanceUntilIdle()
                harness.controller.stop()
                caller.release()
                advanceUntilIdle()

                harness.reader.cancelledReads shouldBe 0
                result.await() shouldBe ImportReadResult.Cancelled
            }
        }

        test("늦게 끝난 옛 읽기의 실패는 그 뒤에 고른 원본을 정리하지 않는다") {
            runTest(rig.dispatcher) {
                val reader = FakeImportSourceReader()
                val gate = reader.suspendsIgnoringCancellation()
                reader.returns(ImportTestTables.text())
                val harness = rig.create(reader = reader)
                val stale = harness.pick(FakePickedImportFile.ofSize("words.txt", size = 100))
                advanceUntilIdle()

                val pasted = harness.paste("a\tb")
                advanceUntilIdle()
                gate.completeExceptionally(ImportParseException(ImportParseErrorKind.CORRUPT_FILE, "forced"))
                advanceUntilIdle()

                stale.await() shouldBe ImportReadResult.Cancelled
                pasted.await().shouldBeInstanceOf<ImportReadResult.Success>()
                harness.controller.source.shouldBeInstanceOf<ImportReadSource.Paste>()
            }
        }

        test("호출한 코루틴이 취소되면 진행 중인 읽기를 멈추고 원본은 남긴다") {
            runTest(rig.dispatcher) {
                val reader = FakeImportSourceReader()
                reader.suspends()
                val scope = rig.newScope()
                val harness = rig.create(reader = reader, scope = scope)
                val caller =
                    scope.launch {
                        harness.controller.readPaste("a\tb", ReadControllerHarness.PASTE_SOURCE_NAME, ReadControllerHarness.DATE_STYLE)
                    }
                advanceUntilIdle()

                caller.cancelAndJoin()
                advanceUntilIdle()

                harness.reader.cancelledReads shouldBe 1
                harness.controller.source.shouldBeInstanceOf<ImportReadSource.Paste>()
            }
        }

        test("파일 이름 확인과 복사, 읽기는 io에서 하므로 io가 멈춰 있으면 진행하지 않는다") {
            runTest(rig.dispatcher) {
                val io = GatedDispatcher(rig.dispatcher)
                val harness = rig.create(reader = FakeImportSourceReader().returns(ImportTestTables.text()), io = io)
                val file = FakePickedImportFile.ofSize("words.txt", size = 100)
                io.hold()

                val result = harness.pick(file)
                advanceUntilIdle()

                harness.reader.calls.shouldBeEmpty()
                harness.tempStorage.regularFiles().shouldBeEmpty()
                file.discardCount shouldBe 0

                io.release()
                advanceUntilIdle()

                result.await().shouldBeInstanceOf<ImportReadResult.Success>()
            }
        }

        test("붙여넣은 글은 default에서 읽으므로 default가 멈춰 있으면 읽지 않는다") {
            runTest(rig.dispatcher) {
                val default = GatedDispatcher(rig.dispatcher)
                val harness = rig.create(reader = FakeImportSourceReader().returns(ImportTestTables.text()), default = default)
                default.hold()

                val result = harness.paste("a\tb")
                advanceUntilIdle()

                harness.reader.calls.shouldBeEmpty()

                default.release()
                advanceUntilIdle()

                result.await().shouldBeInstanceOf<ImportReadResult.Success>()
            }
        }

        test("읽기가 io에서 실패해도 원본 정리는 scope의 디스패처에서 한다") {
            runTest(rig.dispatcher) {
                val main = GatedDispatcher(rig.dispatcher)
                val reader = FakeImportSourceReader()
                val gate = reader.suspends()
                val harness = rig.create(reader = reader, scope = rig.newScope(main), callerScope = rig.newScope())
                val result = harness.pick(FakePickedImportFile.ofSize("words.txt", size = 100))
                advanceUntilIdle()

                main.hold()
                gate.completeExceptionally(ImportParseException(ImportParseErrorKind.CORRUPT_FILE, "forced"))
                advanceUntilIdle()

                harness.controller.source.shouldNotBeNull()

                main.release()
                advanceUntilIdle()

                harness.controller.source shouldBe null
                result.await().shouldBeInstanceOf<ImportReadResult.Failure>()
            }
        }

        test("만들면 이전 실행이 남긴 다른 화면의 임시 디렉터리를 지운다") {
            runTest(rig.dispatcher) {
                val tempStorage = FakeImportTempStorage.create()
                val leftover = tempStorage.plantPreviousSession()

                rig.create(tempStorage = tempStorage)
                advanceUntilIdle()

                tempStorage.exists(leftover) shouldBe false
            }
        }

        test("닫으면 scope가 취소된 뒤에도 진행 중이던 읽기가 끝나기를 기다려 임시 파일을 지운다") {
            runTest(rig.dispatcher) {
                val reader = FakeImportSourceReader()
                val gate = reader.suspendsIgnoringCancellation()
                val scope = rig.newScope()
                val harness = rig.create(reader = reader, scope = scope)
                harness.pick(FakePickedImportFile.ofSize("words.txt", size = 100))
                advanceUntilIdle()

                scope.cancel()
                harness.controller.close()
                advanceUntilIdle()

                harness.tempStorage.regularFiles().size shouldBe 1

                gate.complete(ImportTestTables.text())
                advanceUntilIdle()

                harness.tempStorage.regularFiles().shouldBeEmpty()
                harness.controller.source shouldBe null
            }
        }

        test("임시 파일 정리가 IOException이 아닌 예외로 실패해도 그 예외를 정리 작업 밖으로 내보내지 않고 다음 읽기를 이어 간다") {
            runTest(rig.dispatcher) {
                val fileSystem = ListFailingFileSystem(FileSystem.SYSTEM)
                val reader = FakeImportSourceReader().returns(ImportTestTables.text()).returns(ImportTestTables.text())
                val harness = rig.create(reader = reader, tempStorage = FakeImportTempStorage.create(fileSystem))
                try {
                    harness.pick(FakePickedImportFile.ofSize("first.txt", size = 100))
                    advanceUntilIdle()
                    harness.controller.discard()
                    advanceUntilIdle()

                    val next = harness.pick(FakePickedImportFile.ofSize("second.txt", size = 100))
                    advanceUntilIdle()
                    harness.controller.close()
                    advanceUntilIdle()

                    next.await().shouldBeInstanceOf<ImportReadResult.Success>()
                    // 만들 때, 버릴 때, 닫을 때의 정리가 모두 실패를 겪었다.
                    fileSystem.failedLists shouldBeGreaterThanOrEqual 3
                } finally {
                    fileSystem.failing = false
                }
            }
        }
    }

    // 정리 함수가 삼키는 IOException이 아닌 예외로 디렉터리 목록 읽기를 실패시킨다. 정리는 모두 목록을 읽은 뒤 지운다.
    private class ListFailingFileSystem(
        delegate: FileSystem,
    ) : ForwardingFileSystem(delegate) {
        var failing = true
        var failedLists = 0
            private set

        override fun listOrNull(dir: Path): List<Path>? {
            if (failing) {
                failedLists += 1
                throw IllegalStateException("forced cleanup failure")
            }
            return super.listOrNull(dir)
        }
    }
}

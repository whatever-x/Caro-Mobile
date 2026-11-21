package com.whatever.caro.feature.importer.source

import com.whatever.caro.core.importparser.ImportParseErrorKind
import com.whatever.caro.core.importparser.ImportParseException
import com.whatever.caro.core.model.importing.ImportLimits
import com.whatever.caro.feature.importer.support.FakeImportSourceReader
import com.whatever.caro.feature.importer.support.FakePickedImportFile
import com.whatever.caro.feature.importer.support.ForcedError
import com.whatever.caro.feature.importer.support.ImportTestTables
import com.whatever.caro.feature.importer.support.ReadCall
import com.whatever.caro.feature.importer.support.ReadControllerHarness
import com.whatever.caro.feature.importer.support.ReadControllerRig
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.types.shouldBeSameInstanceAs
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest

/** 첫 읽기와 다시 읽기의 입력, 읽기 전에 막는 파일, 실패 종류에 따른 원본 정리를 확인한다. */
@OptIn(ExperimentalCoroutinesApi::class)
class ImportReadControllerTest : FunSpec() {
    private val rig = ReadControllerRig()

    init {
        rig.installOn(this)

        test("고른 파일은 임시 저장소 사본으로 복사해 그 경로와 파일 이름으로 읽고 선택기 사본은 읽기 전에 정리한다") {
            runTest(rig.dispatcher) {
                val reader = FakeImportSourceReader()
                reader.suspends()
                val harness = rig.create(reader = reader)
                val file = FakePickedImportFile.ofSize("deck.apkg", size = 20_000)

                harness.pick(file)
                advanceUntilIdle()

                val call =
                    harness.reader.calls
                        .single()
                        .shouldBeInstanceOf<ReadCall.FileRead>()
                call.path.parent shouldBe harness.tempStorage.root
                call.copiedBytes shouldBe file.content
                call.sourceName shouldBe "deck.apkg"
                file.discardCount shouldBe 1
                harness.controller.source
                    .shouldBeInstanceOf<ImportReadSource.File>()
                    .path shouldBe call.path
            }
        }

        test("제공자가 준 이름의 방향 제어 문자를 지운 이름으로 형식을 가리고 읽는다") {
            runTest(rig.dispatcher) {
                val reader = FakeImportSourceReader()
                reader.suspends()
                val harness = rig.create(reader = reader)

                harness.pick(FakePickedImportFile.ofSize("deck‮.apkg", size = 100))
                advanceUntilIdle()

                harness.reader.calls
                    .single()
                    .sourceName shouldBe "deck.apkg"
            }
        }

        test("Anki 파일의 첫 읽기는 하위 덱 요약만 읽고 그 밖의 파일은 전체를 읽는다") {
            runTest(rig.dispatcher) {
                val reader = FakeImportSourceReader().returns(ImportTestTables.apkgSummary("modern_anki21b_main.apkg"))
                reader.returns(ImportTestTables.text())
                val harness = rig.create(reader = reader)

                harness.pick(FakePickedImportFile.ofSize("deck.apkg", size = 100))
                advanceUntilIdle()
                harness.pick(FakePickedImportFile.ofSize("words.txt", size = 100))
                advanceUntilIdle()

                val (apkg, text) = harness.reader.calls.map { call -> call.shouldBeInstanceOf<ReadCall.FileRead>() }
                apkg.subdeckIds shouldBe emptySet()
                text.subdeckIds shouldBe null
            }
        }

        test("정확히 상한 크기인 파일은 읽는다") {
            runTest(rig.dispatcher) {
                val table = ImportTestTables.apkgSummary("modern_anki21b_main.apkg")
                val harness = rig.create(reader = FakeImportSourceReader().returns(table))
                val file = FakePickedImportFile.ofSize("deck.apkg", size = 100, reportedSize = ImportLimits.MAX_FILE_BYTES)

                val result = harness.pick(file)
                advanceUntilIdle()

                result.await() shouldBe ImportReadResult.Success(table)
            }
        }

        test("상한보다 1바이트 큰 파일은 읽지 않고 크기 초과 실패를 돌려주며 선택기 사본을 정리한다") {
            runTest(rig.dispatcher) {
                val harness = rig.create()
                val file = FakePickedImportFile.ofSize("deck.apkg", size = 100, reportedSize = ImportLimits.MAX_FILE_BYTES + 1)

                val result = harness.pick(file)
                advanceUntilIdle()

                result.await().shouldBeFailure(ImportParseErrorKind.FILE_TOO_LARGE, extension = "apkg")
                harness.reader.calls.shouldBeEmpty()
                file.discardCount shouldBeGreaterThan 0
                harness.controller.source shouldBe null
            }
        }

        test("지원하지 않는 확장자는 크기와 상관없이 읽지 않고 그 확장자로 지원하지 않는 파일 실패를 돌려준다") {
            runTest(rig.dispatcher) {
                val harness = rig.create()
                val file = FakePickedImportFile.ofSize("notes.XLS", size = 100, reportedSize = ImportLimits.MAX_FILE_BYTES + 1)

                val result = harness.pick(file)
                advanceUntilIdle()

                result.await().shouldBeFailure(ImportParseErrorKind.UNSUPPORTED_FILE, extension = "xls")
                harness.reader.calls.shouldBeEmpty()
                file.discardCount shouldBeGreaterThan 0
            }
        }

        test("크기를 모르는 파일이 복사 중 상한을 넘으면 부분 파일을 지우고 크기 초과 실패를 돌려준다") {
            runTest(rig.dispatcher) {
                val harness = rig.create(limits = ImportLimitsConfig(maxFileBytes = 20_000L))
                val file = FakePickedImportFile.ofSize("deck.apkg", size = 20_001, reportedSize = null)

                val result = harness.pick(file)
                advanceUntilIdle()

                result.await().shouldBeFailure(ImportParseErrorKind.FILE_TOO_LARGE, extension = "apkg")
                harness.tempStorage.regularFiles().shouldBeEmpty()
                harness.reader.calls.shouldBeEmpty()
                file.discardCount shouldBeGreaterThan 0
                harness.controller.source shouldBe null
            }
        }

        val parseError = ImportParseException(ImportParseErrorKind.CORRUPT_FILE, "forced")
        listOf<Triple<String, Throwable, (Throwable) -> Unit>>(
            Triple("파서 오류로 실패하면", parseError) { error -> error shouldBeSameInstanceAs parseError },
            Triple("Exception이 아닌 오류로 실패해도", ForcedError()) { error -> error.shouldBeInstanceOf<ForcedError>() },
            // JVM에서는 코루틴 경계를 넘을 때 stack trace 복구로 같은 종류의 새 예외가 오므로 같은 객체인지 보지 않는다.
            Triple("멈추지 않았는데 리더가 CancellationException을 던지면", CancellationException("forced")) { error ->
                error.shouldBeInstanceOf<CancellationException>().message shouldBe "forced"
            },
        ).forEach { (cause, thrown, verifyError) ->
            test("첫 읽기가 $cause 그 오류와 확장자를 실패로 돌려주고 원본과 임시 파일을 정리한다") {
                runTest(rig.dispatcher) {
                    val harness = rig.create(reader = FakeImportSourceReader().fails(thrown))

                    val result = harness.pick(FakePickedImportFile.ofSize("deck.apkg", size = 20_000))
                    advanceUntilIdle()

                    val failure = result.await().shouldBeInstanceOf<ImportReadResult.Failure>()
                    verifyError(failure.error)
                    failure.extension shouldBe "apkg"
                    harness.controller.source shouldBe null
                    harness.tempStorage.regularFiles().shouldBeEmpty()
                }
            }
        }

        test("초안 다시 읽기가 실패하면 원본과 사본을 남겨 그 화면에서 다시 읽을 수 있다") {
            runTest(rig.dispatcher) {
                val reread = ImportTestTables.text(delimiter = ",")
                val reader =
                    FakeImportSourceReader()
                        .returns(ImportTestTables.text())
                        .fails(ImportParseException(ImportParseErrorKind.TOO_MANY_COLUMNS, "forced"))
                        .returns(reread)
                val harness = rig.create(reader = reader)
                harness.pick(FakePickedImportFile.ofSize("words.txt", size = 100))
                advanceUntilIdle()

                val failed = harness.reread(ImportReadStage.Draft, delimiter = ' ')
                advanceUntilIdle()
                val retried = harness.reread(ImportReadStage.Draft, delimiter = ',')
                advanceUntilIdle()

                failed.await().shouldBeFailure(ImportParseErrorKind.TOO_MANY_COLUMNS, extension = "txt")
                retried.await() shouldBe ImportReadResult.Success(reread)
                harness.controller.source.shouldNotBeNull()
                harness.tempStorage.regularFiles().size shouldBe 1
            }
        }

        test("첫 읽기에 이어 같은 원본을 다시 읽다 실패하면 첫 읽기 실패처럼 원본과 임시 파일을 정리한다") {
            runTest(rig.dispatcher) {
                val reader =
                    FakeImportSourceReader()
                        .returns(ImportTestTables.apkgSummary("modern_anki21b_main.apkg"))
                        .fails(ImportParseException(ImportParseErrorKind.CORRUPT_FILE, "forced"))
                val harness = rig.create(reader = reader)
                harness.pick(FakePickedImportFile.ofSize("deck.apkg", size = 100))
                advanceUntilIdle()

                val rows = harness.reread(ImportReadStage.First)
                advanceUntilIdle()

                rows.await().shouldBeFailure(ImportParseErrorKind.CORRUPT_FILE, extension = "apkg")
                harness.controller.source shouldBe null
                harness.tempStorage.regularFiles().shouldBeEmpty()
            }
        }

        test("Anki 요약을 읽은 뒤 같은 사본에서 모든 하위 덱의 행을 다시 읽는다") {
            runTest(rig.dispatcher) {
                val summary = ImportTestTables.apkgSummary("modern_anki21b_main.apkg")
                val full = ImportTestTables.withOneRow(summary)
                val harness = rig.create(reader = FakeImportSourceReader().returns(summary).returns(full))
                harness.pick(FakePickedImportFile.ofSize("deck.apkg", size = 100))
                advanceUntilIdle()

                val rows = harness.reread(ImportReadStage.First, subdeckIds = null)
                advanceUntilIdle()

                rows.await() shouldBe ImportReadResult.Success(full)
                val (first, second) = harness.reader.calls.map { call -> call.shouldBeInstanceOf<ReadCall.FileRead>() }
                second.path shouldBe first.path
                second.subdeckIds shouldBe null
            }
        }

        test("엑셀 첫 시트가 비면 같은 사본에서 후보 시트를 차례로 다시 읽는다") {
            runTest(rig.dispatcher) {
                val reader =
                    FakeImportSourceReader()
                        .returns(ImportTestTables.xlsx(sheetRows = listOf(0, 1, 3)))
                        .returns(ImportTestTables.xlsx(sheetRows = listOf(0, 0, 3), selectedSheet = 1))
                        .returns(ImportTestTables.xlsx(sheetRows = listOf(0, 1, 3), selectedSheet = 2))
                val harness = rig.create(reader = reader)
                harness.pick(FakePickedImportFile.ofSize("book.xlsx", size = 100))
                advanceUntilIdle()

                harness.reread(ImportReadStage.First, sheetIndex = 1)
                advanceUntilIdle()
                val last = harness.reread(ImportReadStage.First, sheetIndex = 2)
                advanceUntilIdle()

                last
                    .await()
                    .shouldBeInstanceOf<ImportReadResult.Success>()
                    .table.rows.size shouldBe 3
                harness.reader.calls.map { call -> call.shouldBeInstanceOf<ReadCall.FileRead>().sheetIndex } shouldBe listOf(0, 1, 2)
            }
        }

        test("붙여넣은 글은 원본으로 남아 구분 기준과 첫 줄 설정을 바꿔 다시 읽는다") {
            runTest(rig.dispatcher) {
                val reader = FakeImportSourceReader().returns(ImportTestTables.text()).returns(ImportTestTables.text(delimiter = ","))
                val harness = rig.create(reader = reader)
                harness.paste("a,b")
                advanceUntilIdle()

                harness.reread(ImportReadStage.Draft, delimiter = ',', hasHeaderRow = true)
                advanceUntilIdle()

                harness.reader.calls shouldBe
                    listOf(
                        ReadCall.TextRead("a,b", ReadControllerHarness.PASTE_SOURCE_NAME, ReadControllerHarness.DATE_STYLE, null, null),
                        ReadCall.TextRead("a,b", ReadControllerHarness.PASTE_SOURCE_NAME, ReadControllerHarness.DATE_STYLE, ',', true),
                    )
                harness.controller.source.shouldBeInstanceOf<ImportReadSource.Paste>()
            }
        }

        test("버리지 않고 다른 파일을 고르면 앞 사본을 지우고 새 사본만 남긴다") {
            runTest(rig.dispatcher) {
                val harness =
                    rig.create(
                        reader = FakeImportSourceReader().returns(ImportTestTables.text()).returns(ImportTestTables.text()),
                    )
                harness.pick(FakePickedImportFile.ofSize("first.txt", size = 100))
                advanceUntilIdle()

                harness.pick(FakePickedImportFile.ofSize("second.txt", size = 100))
                advanceUntilIdle()

                val copy = harness.controller.source.shouldBeInstanceOf<ImportReadSource.File>()
                copy.name shouldBe "second.txt"
                harness.tempStorage.regularFiles() shouldBe listOf(copy.path)
            }
        }

        test("고른 파일을 버리지 않고 붙여 넣으면 앞 사본을 지운다") {
            runTest(rig.dispatcher) {
                val harness =
                    rig.create(
                        reader = FakeImportSourceReader().returns(ImportTestTables.text()).returns(ImportTestTables.text()),
                    )
                harness.pick(FakePickedImportFile.ofSize("first.txt", size = 100))
                advanceUntilIdle()

                harness.paste("a\tb")
                advanceUntilIdle()

                harness.controller.source.shouldBeInstanceOf<ImportReadSource.Paste>()
                harness.tempStorage.regularFiles().shouldBeEmpty()
            }
        }

        test("원본이 없으면 다시 읽지 않는다") {
            runTest(rig.dispatcher) {
                val harness = rig.create()

                val result = harness.reread(ImportReadStage.Draft, delimiter = ',')
                advanceUntilIdle()

                result.await() shouldBe ImportReadResult.Cancelled
                harness.reader.calls.shouldBeEmpty()
            }
        }
    }

    private fun ImportReadResult.shouldBeFailure(
        kind: ImportParseErrorKind,
        extension: String,
    ) {
        val failure = shouldBeInstanceOf<ImportReadResult.Failure>()
        failure.error.shouldBeInstanceOf<ImportParseException>().kind shouldBe kind
        failure.extension shouldBe extension
    }
}

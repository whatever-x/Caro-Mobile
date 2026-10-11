package com.whatever.caro.feature.importer

import com.whatever.caro.core.importparser.model.ParsedRow
import com.whatever.caro.core.model.importing.FieldMapping
import com.whatever.caro.feature.importer.mvi.ImportDeckIntent
import com.whatever.caro.feature.importer.mvi.ImportDialog
import com.whatever.caro.feature.importer.mvi.ImportStep
import com.whatever.caro.feature.importer.support.EMPTY_SIDE_TEXT
import com.whatever.caro.feature.importer.support.FakeImportSourceReader
import com.whatever.caro.feature.importer.support.ImportTestTables
import com.whatever.caro.feature.importer.support.ImportViewModelHarness
import com.whatever.caro.feature.importer.support.ImportViewModelRig
import com.whatever.caro.feature.importer.support.captureNapier
import com.whatever.caro.feature.importer.support.openMappingFromFile
import io.github.aakira.napier.LogLevel
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest

/** 미리보기 계산이 예상하지 못한 오류로 실패했을 때 안내 대화상자를 띄우고 항목 연결에 머무는지 확인한다. */
class ImportDeckViewModelPreviewFailureTest : FunSpec() {
    private val rig = ImportViewModelRig()

    /** 행을 훑는 순간 던지는 목록이다. 연결 단계까지는 행을 읽지 않으므로 [다음]을 누른 계산에서만 실패한다. */
    private class ExplodingRows : AbstractList<ParsedRow>() {
        override val size: Int = 1

        override fun get(index: Int): ParsedRow = throw IllegalStateException("forced")
    }

    init {
        rig.installOn(this)

        context("미리보기 계산 실패") {
            test("미리보기 계산이 실패하면 미리보기 실패 대화상자를 띄우고 오버레이를 걷는다") {
                runTest(rig.dispatcher) {
                    val harness = failingMapping()

                    harness.send(ImportDeckIntent.ClickMappingNext(EMPTY_SIDE_TEXT))
                    advanceUntilIdle()

                    harness.state.dialog shouldBe ImportDialog.PreviewFailed
                    harness.state.loading.shouldBeNull()
                    harness.state.preview.shouldBeNull()
                }
            }

            test("파일을 다시 읽지 않고 항목 연결에 머물며 고른 항목을 유지한다") {
                runTest(rig.dispatcher) {
                    val harness = failingMapping()
                    harness.send(ImportDeckIntent.ToggleField(TEXT, FieldMapping.BACK, "열 2"))
                    advanceUntilIdle()
                    val mappingsBefore = harness.state.mappings
                    val readsBefore = harness.reader.calls.size

                    harness.send(ImportDeckIntent.ClickMappingNext(EMPTY_SIDE_TEXT))
                    advanceUntilIdle()

                    harness.state.step shouldBe ImportStep.Mapping
                    harness.state.mappings shouldBe mappingsBefore
                    harness.state.table.shouldNotBeNull()
                    harness.reader.calls.size shouldBe readsBefore
                }
            }

            test("확인은 대화상자만 닫고 항목 연결에 머문다") {
                runTest(rig.dispatcher) {
                    val harness = failingMapping()
                    harness.send(ImportDeckIntent.ClickMappingNext(EMPTY_SIDE_TEXT))
                    advanceUntilIdle()

                    harness.send(ImportDeckIntent.DismissDialog)
                    advanceUntilIdle()

                    harness.state.dialog.shouldBeNull()
                    harness.state.step shouldBe ImportStep.Mapping
                    harness.state.table.shouldNotBeNull()
                }
            }

            test("뒤로 가기는 확인과 같아 대화상자만 닫고 이전 선택 단계로 가지 않는다") {
                runTest(rig.dispatcher) {
                    val harness = failingMapping()
                    harness.send(ImportDeckIntent.ClickMappingNext(EMPTY_SIDE_TEXT))
                    advanceUntilIdle()

                    harness.send(ImportDeckIntent.ClickBack)
                    advanceUntilIdle()

                    harness.state.dialog.shouldBeNull()
                    harness.state.step shouldBe ImportStep.Mapping
                }
            }

            test("대화상자를 닫은 뒤 다시 [미리보기]를 누르면 다시 계산한다") {
                runTest(rig.dispatcher) {
                    val harness = failingMapping()
                    harness.send(ImportDeckIntent.ClickMappingNext(EMPTY_SIDE_TEXT))
                    advanceUntilIdle()
                    harness.send(ImportDeckIntent.DismissDialog)
                    advanceUntilIdle()

                    harness.send(ImportDeckIntent.ClickMappingNext(EMPTY_SIDE_TEXT))
                    advanceUntilIdle()

                    harness.state.dialog shouldBe ImportDialog.PreviewFailed
                    harness.state.loading.shouldBeNull()
                }
            }

            test("실패 원인은 오류 로그로 남긴다") {
                runTest(rig.dispatcher) {
                    captureNapier { logs ->
                        val harness = failingMapping()

                        harness.send(ImportDeckIntent.ClickMappingNext(EMPTY_SIDE_TEXT))
                        advanceUntilIdle()

                        val log = logs.single { entry -> entry.message.orEmpty().startsWith("import preview build failed") }
                        log.level shouldBe LogLevel.ERROR
                        log.throwable.shouldBeInstanceOf<IllegalStateException>().message shouldBe "forced"
                    }
                }
            }
        }
    }

    private fun TestScope.failingMapping(): ImportViewModelHarness {
        val table = ImportTestTables.text().copy(rows = ExplodingRows())
        val harness = rig.create(reader = FakeImportSourceReader().returns(table))
        openMappingFromFile(harness, "words.txt")
        return harness
    }

    private companion object {
        const val TEXT = "text"
    }
}

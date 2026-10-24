package com.whatever.caro.feature.importer

import com.whatever.caro.feature.importer.mvi.ImportDeckIntent
import com.whatever.caro.feature.importer.mvi.ImportDialog
import com.whatever.caro.feature.importer.mvi.ImportStep
import com.whatever.caro.feature.importer.support.FakeImportSourceReader
import com.whatever.caro.feature.importer.support.ImportTestTables
import com.whatever.caro.feature.importer.support.ImportViewModelHarness
import com.whatever.caro.feature.importer.support.ImportViewModelRig
import com.whatever.caro.feature.importer.support.openMappingFromFile
import com.whatever.caro.feature.importer.support.openPreview
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest

/** 미리보기 카드를 눌러 전체 내용 팝업을 열고 닫는 흐름을 확인한다. */
class ImportDeckViewModelCardDetailTest : FunSpec() {
    private val rig = ImportViewModelRig()

    init {
        rig.installOn(this)

        test("미리보기에서 카드를 누르면 그 카드의 전체 보기 대화상자를 열고 단계는 그대로다") {
            runTest(rig.dispatcher) {
                val harness = textPreview()
                val card =
                    harness.state.preview
                        .shouldNotBeNull()
                        .cards[1]

                harness.send(ImportDeckIntent.ClickCard(card))
                advanceUntilIdle()

                harness.state.dialog shouldBe ImportDialog.CardDetail(card)
                harness.state.step shouldBe ImportStep.Preview
            }
        }

        test("전체 보기 대화상자는 닫기 Intent로 닫힌다") {
            runTest(rig.dispatcher) {
                val harness = textPreview()
                harness.send(
                    ImportDeckIntent.ClickCard(
                        harness.state.preview
                            .shouldNotBeNull()
                            .cards
                            .first(),
                    ),
                )
                advanceUntilIdle()

                harness.send(ImportDeckIntent.DismissDialog)
                advanceUntilIdle()

                harness.state.dialog shouldBe null
                harness.state.step shouldBe ImportStep.Preview
            }
        }

        test("전체 보기 대화상자가 열린 동안 뒤로 가기는 대화상자만 닫는다") {
            runTest(rig.dispatcher) {
                val harness = textPreview()
                harness.send(
                    ImportDeckIntent.ClickCard(
                        harness.state.preview
                            .shouldNotBeNull()
                            .cards
                            .first(),
                    ),
                )
                advanceUntilIdle()

                harness.send(ImportDeckIntent.ClickBack)
                advanceUntilIdle()

                harness.state.dialog shouldBe null
                harness.state.step shouldBe ImportStep.Preview
            }
        }

        test("다른 대화상자가 열려 있으면 카드를 눌러도 대화상자를 바꾸지 않는다") {
            runTest(rig.dispatcher) {
                val harness = textPreview()
                val cards =
                    harness.state.preview
                        .shouldNotBeNull()
                        .cards
                harness.send(ImportDeckIntent.ClickCard(cards[0]))
                advanceUntilIdle()

                harness.send(ImportDeckIntent.ClickCard(cards[1]))
                advanceUntilIdle()

                harness.state.dialog shouldBe ImportDialog.CardDetail(cards[0])
            }
        }

        test("미리보기가 아닌 단계에서는 카드 Intent를 무시한다") {
            runTest(rig.dispatcher) {
                val harness = textPreview()
                val card =
                    harness.state.preview
                        .shouldNotBeNull()
                        .cards
                        .first()
                harness.send(ImportDeckIntent.ClickBack)
                advanceUntilIdle()
                harness.state.step shouldBe ImportStep.Mapping

                harness.send(ImportDeckIntent.ClickCard(card))
                advanceUntilIdle()

                harness.state.dialog shouldBe null
            }
        }
    }

    private fun TestScope.textPreview(): ImportViewModelHarness {
        val harness = rig.create(reader = FakeImportSourceReader().returns(ImportTestTables.text()))
        openMappingFromFile(harness, "words.txt")
        openPreview(harness)
        harness.state.step shouldBe ImportStep.Preview
        return harness
    }
}

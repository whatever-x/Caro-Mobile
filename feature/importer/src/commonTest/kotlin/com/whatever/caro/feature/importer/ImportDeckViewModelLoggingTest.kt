package com.whatever.caro.feature.importer

import com.whatever.caro.core.data.repository.deck.DeckRepository
import com.whatever.caro.core.importparser.ImportParseErrorKind
import com.whatever.caro.core.importparser.ImportParseException
import com.whatever.caro.core.model.exception.NetworkException
import com.whatever.caro.feature.importer.mvi.ImportDeckIntent
import com.whatever.caro.feature.importer.platform.PickResult
import com.whatever.caro.feature.importer.support.CapturedLog
import com.whatever.caro.feature.importer.support.FakeImportSourceReader
import com.whatever.caro.feature.importer.support.FakePickedImportFile
import com.whatever.caro.feature.importer.support.ForcedError
import com.whatever.caro.feature.importer.support.ImportTestTables
import com.whatever.caro.feature.importer.support.ImportViewModelRig
import com.whatever.caro.feature.importer.support.captureNapier
import com.whatever.caro.feature.importer.support.openDeckInfo
import com.whatever.caro.feature.importer.support.openMappingFromFile
import com.whatever.caro.feature.importer.support.openPreview
import dev.mokkery.answering.throws
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import io.github.aakira.napier.LogLevel
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.types.shouldBeSameInstanceAs
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest

/** 사용자에게는 일반 오류로만 보이는 실패가 원인과 함께 로그에 남는지, 파일과 붙여 넣은 내용은 남지 않는지 확인한다. */
class ImportDeckViewModelLoggingTest : FunSpec() {
    private val rig = ImportViewModelRig()

    init {
        rig.installOn(this)

        test("파서 밖의 예외로 읽기가 실패하면 원인을 경고로 남긴다") {
            runTest(rig.dispatcher) {
                captureNapier { logs ->
                    val harness = rig.create(reader = FakeImportSourceReader().fails(IllegalStateException("boom")))

                    harness.pick(FakePickedImportFile.ofSize("deck.apkg", size = 100))
                    advanceUntilIdle()

                    val log = logs.importLog("import read failed")
                    log.level shouldBe LogLevel.WARNING
                    log.message shouldBe "import read failed ext=apkg paste=false"
                    // 코루틴 경계를 지나며 예외가 복사되므로 같은 객체인지가 아니라 종류와 메시지로 확인한다.
                    log.throwable.shouldBeInstanceOf<IllegalStateException>().message shouldBe "boom"
                }
            }
        }

        test("Exception이 아닌 오류로 읽기가 실패하면 원인을 오류 수준으로 남긴다") {
            runTest(rig.dispatcher) {
                captureNapier { logs ->
                    val harness = rig.create(reader = FakeImportSourceReader().fails(ForcedError()))

                    harness.pick(FakePickedImportFile.ofSize("deck.apkg", size = 100))
                    advanceUntilIdle()

                    val log = logs.importLog("import read failed")
                    log.level shouldBe LogLevel.ERROR
                    log.message shouldBe "import read failed ext=apkg paste=false"
                    log.throwable.shouldBeInstanceOf<ForcedError>().message shouldBe "forced"
                }
            }
        }

        test("붙여넣기 읽기 실패 로그에는 붙여 넣은 내용을 담지 않는다") {
            runTest(rig.dispatcher) {
                captureNapier { logs ->
                    val harness = rig.create(reader = FakeImportSourceReader().fails(IllegalStateException("boom")))

                    harness.pasteNext("비밀단어\t뜻")
                    advanceUntilIdle()

                    logs.importLog("import read failed").message shouldBe "import read failed ext= paste=true"
                    logs.none { log -> log.message.orEmpty().contains("비밀단어") } shouldBe true
                }
            }
        }

        test("파서가 알려 준 오류는 사용자에게 안내하므로 로그를 남기지 않는다") {
            runTest(rig.dispatcher) {
                captureNapier { logs ->
                    val reader = FakeImportSourceReader().fails(ImportParseException(ImportParseErrorKind.CORRUPT_FILE, "forced"))
                    val harness = rig.create(reader = reader)

                    harness.pick(FakePickedImportFile.ofSize("deck.apkg", size = 100))
                    advanceUntilIdle()

                    logs.filter { log -> log.message.orEmpty().startsWith("import read failed") } shouldBe emptyList()
                }
            }
        }

        test("선택기가 실패를 알리면 원인을 경고로 남긴다") {
            runTest(rig.dispatcher) {
                captureNapier { logs ->
                    val cause = IllegalStateException("provider failed")
                    val harness = rig.create()

                    harness.pickResult(PickResult.Failed(cause))
                    advanceUntilIdle()

                    val log = logs.importLog("import file pick failed")
                    log.level shouldBe LogLevel.WARNING
                    log.throwable shouldBeSameInstanceAs cause
                }
            }
        }

        test("덱 생성이 불확실하게 실패하면 원인을 경고로 남긴다") {
            runTest(rig.dispatcher) {
                captureNapier { logs ->
                    val cause = NetworkException.Timeout(debugMessage = "timeout")
                    val repository = mock<DeckRepository> { everySuspend { createDeck(any(), any()) } throws cause }
                    val harness =
                        rig.create(
                            reader = FakeImportSourceReader().returns(ImportTestTables.text()),
                            deckRepository = repository,
                        )
                    openMappingFromFile(harness, "words.txt")
                    openPreview(harness)
                    openDeckInfo(harness)

                    harness.send(ImportDeckIntent.ClickCreateDeck)
                    advanceUntilIdle()

                    val log = logs.importLog("import deck creation failed")
                    log.level shouldBe LogLevel.WARNING
                    log.throwable shouldBeSameInstanceAs cause
                }
            }
        }
    }

    private fun List<CapturedLog>.importLog(prefix: String): CapturedLog = single { log -> log.message.orEmpty().startsWith(prefix) }
}

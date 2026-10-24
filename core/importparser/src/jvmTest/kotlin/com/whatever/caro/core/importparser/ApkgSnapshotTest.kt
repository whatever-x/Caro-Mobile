package com.whatever.caro.core.importparser

import com.whatever.caro.core.importparser.model.ParsedSubdeck
import com.whatever.caro.core.importparser.model.ParsedTable
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json
import okio.Path
import okio.buffer
import okio.gzip
import okio.use

/**
 * 네이티브 SQLite와 zstd를 쓸 수 없는 Android 호스트 테스트와 CI에서도 연결 후 처리를 Anki 자료로 검증하도록 파서 결과를 gzip JSON으로 남긴다.
 * 파서 결과가 바뀌면 이 테스트가 실패해 스냅샷을 함께 갱신하게 한다.
 * 갱신: `CARO_UPDATE_IMPORT_SNAPSHOTS=true ./gradlew :core:importparser:jvmTest`
 */
class ApkgSnapshotTest : FunSpec() {
    private val update = System.getenv(UPDATE_ENV) == "true"

    init {
        Fixtures.expectedFiles("apkg").forEach { expectedPath ->
            val expected = Fixtures.readExpected(expectedPath)
            val fixture = expected.string("fixture") ?: error("fixture missing")
            if (expected.obj("error") != null) return@forEach

            test("$fixture 파서 결과 스냅샷이 최신이다") {
                variants(fixture, expected.objects("selectionExamples").map { it.strings("selectedPaths") }).forEach { (variant, table) ->
                    val path = Fixtures.path("snapshots/apkg/${fixture.substringAfterLast('/')}.$variant.json.gz")
                    if (update) write(path, table)
                    withClue("$variant snapshot") { read(path) shouldBe table }
                }
            }
        }
    }

    private suspend fun variants(
        fixture: String,
        selections: List<List<String?>>,
    ): List<Pair<String, ParsedTable>> {
        val default = Fixtures.parse(fixture)
        val all = Fixtures.parse(fixture, Fixtures.options(subdeckIds = default.subdecks.map { it.id }.toSet() + ParsedSubdeck.NO_DECK_ID))
        val selected =
            selections.mapIndexed { index, paths ->
                val ids =
                    paths
                        .map { path ->
                            path?.let { default.subdecks.first { it.path == path }.id } ?: ParsedSubdeck.NO_DECK_ID
                        }.toSet()
                "selection-$index" to Fixtures.parse(fixture, Fixtures.options(subdeckIds = ids))
            }
        return listOf("default" to default, "all" to all) + selected
    }

    private fun write(
        path: Path,
        table: ParsedTable,
    ) {
        Fixtures.fileSystem.createDirectories(path.parent ?: error("no parent"))
        Fixtures.fileSystem
            .sink(path)
            .gzip()
            .buffer()
            .use { sink -> sink.writeUtf8(json.encodeToString(ParsedTable.serializer(), table)) }
    }

    private fun read(path: Path): ParsedTable =
        Fixtures.fileSystem.source(path).gzip().buffer().use { source ->
            json.decodeFromString(ParsedTable.serializer(), source.readUtf8())
        }

    private companion object {
        const val UPDATE_ENV = "CARO_UPDATE_IMPORT_SNAPSHOTS"
        val json = Json
    }
}

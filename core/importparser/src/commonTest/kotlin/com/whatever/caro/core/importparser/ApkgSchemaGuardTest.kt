package com.whatever.caro.core.importparser

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.shouldBe
import okio.Path
import kotlin.time.Duration.Companion.seconds
import kotlin.time.measureTime

/** 파일 작성자가 스키마로 SQL을 심은 Anki 컬렉션은 고정 SELECT를 실행하기 전에 손상 파일로 거절한다. */
class ApkgSchemaGuardTest : FunSpec() {
    init {
        context("구버전 컬렉션") {
            test("cards를 재귀 CTE view로 정의한 파일은 쿼리를 실행하지 않고 손상 파일로 멈춘다").config(enabled = nativeArchiveLibrariesLoadable) {
                val path = temp("cards-view.apkg")
                TestAnki.legacy(path, fieldCount = 1, notes = emptyList(), notesAndCardsSql = NOTES_TABLE + INFINITE_CARDS_VIEW)

                val elapsed =
                    measureTime {
                        shouldThrow<ImportParseException> { parse(path) }.kind shouldBe
                            ImportParseErrorKind.CORRUPT_FILE
                    }

                elapsed shouldBeLessThan 10.seconds
            }

            listOf(
                "notes.flds를 VIRTUAL 생성 열로 정의한 파일은 값을 만들기 전에" to VIRTUAL_FLDS_NOTES + CARDS_TABLE,
                "cards의 열이 생성 열이어도" to
                    NOTES_TABLE +
                    listOf(
                        "CREATE TABLE cards (nid INTEGER, did INTEGER AS (1) STORED, odid INTEGER, ord INTEGER)",
                        "INSERT INTO cards (nid, odid, ord) VALUES (1, 0, 0)",
                    ),
                "trigger가 있는 파일은" to NOTES_TABLE + CARDS_TABLE + "CREATE TRIGGER t AFTER INSERT ON notes BEGIN SELECT 1; END",
                "가상 테이블이 있는 파일은" to NOTES_TABLE + CARDS_TABLE + "CREATE VIRTUAL TABLE extra USING fts5(body)",
            ).forEachIndexed { index, (case, sql) ->
                test("$case 손상 파일로 멈춘다").config(enabled = nativeArchiveLibrariesLoadable) {
                    val path = temp("legacy-schema-$index.apkg")
                    TestAnki.legacy(path, fieldCount = 1, notes = emptyList(), notesAndCardsSql = sql)

                    shouldThrow<ImportParseException> { parse(path) }.kind shouldBe ImportParseErrorKind.CORRUPT_FILE
                }
            }

            listOf("CREATE  VIRTUAL TABLE", "CREATE /**/ VIRTUAL TABLE").forEach { spelling ->
                test("저장된 SQL 글자를 '$spelling'로 바꿔 숨긴 가상 테이블 notes도 손상 파일로 멈춘다")
                    .config(enabled = nativeArchiveLibrariesLoadable) {
                        val path = temp("disguised-virtual-${spelling.hashCode()}.apkg")
                        TestAnki.legacy(
                            path,
                            fieldCount = 1,
                            notes = emptyList(),
                            notesAndCardsSql =
                                listOf(
                                    "CREATE VIRTUAL TABLE notes USING fts5(id, mid, flds)",
                                    "INSERT INTO notes VALUES (1, 1, 'a')",
                                    "PRAGMA writable_schema=ON",
                                    "UPDATE sqlite_master SET sql = replace(sql, 'CREATE VIRTUAL TABLE', '$spelling') WHERE name = 'notes'",
                                    "PRAGMA writable_schema=OFF",
                                ) + CARDS_TABLE,
                        )

                        shouldThrow<ImportParseException> { parse(path) }.kind shouldBe ImportParseErrorKind.CORRUPT_FILE
                    }
            }
        }

        context("최신 컬렉션") {
            listOf(
                "notes.flds를 VIRTUAL 생성 열로 정의한 파일은 스키마를 고치기 전에" to VIRTUAL_FLDS_NOTES + CARDS_TABLE,
                "쿼리하지 않는 이름의 view가 있어도" to NOTES_TABLE + CARDS_TABLE + "CREATE VIEW extra AS SELECT 1",
            ).forEachIndexed { index, (case, sql) ->
                test("$case 손상 파일로 멈춘다").config(enabled = nativeArchiveLibrariesLoadable) {
                    val path = temp("modern-schema-$index.apkg")
                    TestAnki.modern(path, fieldCount = 1, notes = emptyList(), notesAndCardsSql = sql)

                    shouldThrow<ImportParseException> { parse(path) }.kind shouldBe ImportParseErrorKind.CORRUPT_FILE
                }
            }
        }
    }

    private fun temp(name: String): Path {
        Fixtures.fileSystem.createDirectories(Fixtures.tempRoot)
        return Fixtures.tempRoot / "input-$name"
    }

    private suspend fun parse(path: Path) =
        ImportParser(Fixtures.fileSystem).parseFile(path, path.name.removePrefix("input-"), Fixtures.options())

    private companion object {
        val NOTES_TABLE =
            listOf(
                "CREATE TABLE notes (id INTEGER, mid INTEGER, flds TEXT)",
                "INSERT INTO notes VALUES (1, 1, 'a')",
            )
        val CARDS_TABLE =
            listOf(
                "CREATE TABLE cards (nid INTEGER, did INTEGER, odid INTEGER, ord INTEGER)",
                "INSERT INTO cards VALUES (1, 1, 0, 0)",
            )
        val VIRTUAL_FLDS_NOTES =
            listOf(
                "CREATE TABLE notes (id INTEGER, mid INTEGER, flds TEXT AS (printf('%.*c', 2000000, 'x')) VIRTUAL)",
                "INSERT INTO notes (id, mid) VALUES (1, 1)",
            )
        val INFINITE_CARDS_VIEW =
            listOf(
                "CREATE VIEW cards AS WITH RECURSIVE c(x) AS (SELECT 1 UNION ALL SELECT x + 1 FROM c) " +
                    "SELECT x AS nid, 1 AS did, 0 AS odid, 0 AS ord FROM c WHERE x < 0",
            )
    }
}

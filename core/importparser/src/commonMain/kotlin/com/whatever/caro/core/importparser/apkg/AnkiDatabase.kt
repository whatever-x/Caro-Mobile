package com.whatever.caro.core.importparser.apkg

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteDriver
import androidx.sqlite.SQLiteException
import androidx.sqlite.SQLiteStatement
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.driver.bundled.SQLITE_OPEN_READONLY
import androidx.sqlite.driver.bundled.SQLITE_OPEN_URI
import androidx.sqlite.execSQL
import com.whatever.caro.core.importparser.ImportParseErrorKind
import com.whatever.caro.core.importparser.ImportParseException
import okio.Path

/**
 * 파서가 푼 임시 사본 위의 Anki 컬렉션 DB. 정해진 테이블과 열만 SELECT 한다.
 */
internal class AnkiDatabase private constructor(
    @PublishedApi internal val connection: SQLiteConnection,
) : AutoCloseable {
    inline fun <T> query(
        sql: String,
        block: (SQLiteStatement) -> T,
    ): T = connection.prepare(sql).use(block)

    override fun close() = connection.close()

    companion object {
        private const val UNICASE_SCHEMA_FILTER = "WHERE sql LIKE '%COLLATE unicase%'"
        private const val URI_SAFE_CHARACTERS = "/-._~"

        // 가상 테이블은 sqlite_master.type이 table이고 저장된 SQL 글자는 바꿀 수 있으므로, 글자 대신 SQLite가 해석한 type으로 거른다.
        private const val EXECUTABLE_SCHEMA_QUERY =
            "SELECT (SELECT count(*) FROM sqlite_master WHERE type IN ('view', 'trigger')) + " +
                "(SELECT count(*) FROM pragma_table_list WHERE type = 'virtual')"

        // quick_check 메시지에는 스키마의 테이블, 인덱스 이름이 들어가 길이를 알 수 없으므로 문자열로 읽지 않고 SQLite 안에서 'ok'와 비교한다.
        private const val QUICK_CHECK_OK_QUERY = "SELECT quick_check = 'ok' FROM pragma_quick_check LIMIT 1"
        private const val GENERATED_COLUMN_QUERY = "SELECT count(*) FROM pragma_table_xinfo(?) WHERE hidden IN (2, 3)"
        private val QUERIED_TABLES = listOf("col", "notes", "cards", "notetypes", "fields", "decks")

        /**
         * 최신 형식 컬렉션은 Anki 전용 `unicase` 콜레이션을 선언해 등록하지 않으면 `fields` 조회와 무결성 검사가 실패한다.
         * 드라이버에 콜레이션 등록 API가 없어 임시 사본의 스키마에서 콜레이션 선언을 지우고, WAL 헤더를 일반 저널로 바꾼다.
         */
        fun open(
            driver: SQLiteDriver,
            path: Path,
            rewriteModernSchema: Boolean,
            verifyIntegrity: Boolean,
        ): AnkiDatabase {
            if (rewriteModernSchema) rewriteSchema(driver, path)
            val connection = openReadOnly(driver, path)
            try {
                applySafetyPragmas(connection)
                verifySchema(connection)
                if (verifyIntegrity) checkIntegrity(connection)
            } catch (exception: Exception) {
                connection.close()
                throw exception
            }
            return AnkiDatabase(connection)
        }

        private fun rewriteSchema(
            driver: SQLiteDriver,
            path: Path,
        ) {
            driver.open(path.toString()).use { connection ->
                applySafetyPragmas(connection)
                verifySchema(connection)
                connection.execSQL("PRAGMA journal_mode=DELETE")
                val declared =
                    connection.prepare("SELECT count(*) FROM sqlite_master $UNICASE_SCHEMA_FILTER").use {
                        it.step() &&
                            it.getLong(0) > 0
                    }
                if (!declared) return
                connection.execSQL("PRAGMA writable_schema=ON")
                connection.execSQL("UPDATE sqlite_master SET sql = replace(sql, ' COLLATE unicase', '') $UNICASE_SCHEMA_FILTER")
                connection.execSQL("PRAGMA writable_schema=OFF")
            }
        }

        /** 번들 드라이버면 `immutable=1` 읽기 전용으로 열고, 실패하거나 다른 드라이버면 임시 사본을 쓰기 금지 상태로 연다. */
        private fun openReadOnly(
            driver: SQLiteDriver,
            path: Path,
        ): SQLiteConnection {
            if (driver is BundledSQLiteDriver) {
                try {
                    return driver.open("file:${uriPath(path)}?immutable=1", SQLITE_OPEN_READONLY or SQLITE_OPEN_URI)
                } catch (_: SQLiteException) {
                    // 읽기 전용 열기를 지원하지 않는 환경이면 아래 쓰기 금지 연결로 읽는다.
                }
            }
            return driver.open(path.toString()).also { connection -> connection.execSQL("PRAGMA query_only=ON") }
        }

        /** 악성 DB의 셀 크기 조작은 읽기 중에 잡고, 메모리 매핑은 꺼서 손상 파일이 프로세스 메모리를 직접 건드리지 않게 한다. */
        private fun applySafetyPragmas(connection: SQLiteConnection) {
            connection.execSQL("PRAGMA trusted_schema=OFF")
            connection.execSQL("PRAGMA cell_size_check=ON")
            connection.execSQL("PRAGMA mmap_size=0")
        }

        /**
         * view, trigger, 가상 테이블, 생성 열은 고정 SELECT 안에서 파일 작성자의 SQL을 실행시키므로 데이터 쿼리 전에 거절한다.
         * 안전 pragma는 재귀 CTE와 printf 같은 일반 함수를 막지 못해 무한 반복과 거대 메모리 할당이 가능하다. 실제 Anki 컬렉션에는 이 객체들이 없다.
         */
        private fun verifySchema(connection: SQLiteConnection) {
            val hasExecutableObject = connection.prepare(EXECUTABLE_SCHEMA_QUERY).use { it.step() && it.getLong(0) > 0 }
            if (hasExecutableObject) throw ImportParseException(ImportParseErrorKind.CORRUPT_FILE, "unexpected schema object")
            QUERIED_TABLES.forEach { table ->
                val hasGeneratedColumn =
                    connection.prepare(GENERATED_COLUMN_QUERY).use { statement ->
                        statement.bindText(1, table)
                        statement.step() && statement.getLong(0) > 0
                    }
                if (hasGeneratedColumn) throw ImportParseException(ImportParseErrorKind.CORRUPT_FILE, "generated column in $table")
            }
        }

        private fun checkIntegrity(connection: SQLiteConnection) {
            val passed = connection.prepare(QUICK_CHECK_OK_QUERY).use { it.step() && it.getLong(0) == 1L }
            if (!passed) throw ImportParseException(ImportParseErrorKind.CORRUPT_FILE, "quick_check failed")
        }

        private fun uriPath(path: Path): String =
            buildString {
                path.toString().encodeToByteArray().forEach { byte ->
                    val char = byte.toInt().toChar()
                    if (byte >= 0 && (char.isLetterOrDigit() || char in URI_SAFE_CHARACTERS)) {
                        append(char)
                    } else {
                        append('%')
                        append(
                            byte
                                .toUByte()
                                .toString(radix = 16)
                                .padStart(2, '0')
                                .uppercase(),
                        )
                    }
                }
            }
    }
}

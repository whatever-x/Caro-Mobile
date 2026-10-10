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
                connection.execSQL("PRAGMA trusted_schema=OFF")
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
                connection.execSQL("PRAGMA trusted_schema=OFF")
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

        private fun checkIntegrity(connection: SQLiteConnection) {
            val result = connection.prepare("PRAGMA quick_check").use { if (it.step()) it.getText(0) else "" }
            if (result != "ok") throw ImportParseException(ImportParseErrorKind.CORRUPT_FILE, "quick_check failed")
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

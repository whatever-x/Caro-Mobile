package com.whatever.caro.core.importparser

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import com.squareup.zstd.okio.zstdCompress
import okio.Buffer
import okio.Path
import okio.buffer
import okio.use

/** 노트 유형 항목 수와 노트 값 길이를 마음대로 정한 작은 Anki 패키지를 만든다. 실제 Anki 내보내기에 없는 악성 구조를 재현하려고 쓴다. */
internal object TestAnki {
    private const val NOTE_TYPE_ID = 1L
    private const val DECK_ID = 1L

    fun legacy(
        path: Path,
        fieldCount: Int,
        notes: List<String>,
    ) {
        val database =
            buildDatabase(path) { connection ->
                val fields = List(fieldCount) { index -> """{"name":"f$index","ord":$index}""" }.joinToString(",")
                connection.execSQL("CREATE TABLE col (models TEXT, decks TEXT)")
                insert(
                    connection,
                    "INSERT INTO col VALUES (?, ?)",
                    """{"$NOTE_TYPE_ID":{"name":"T","type":0,"flds":[$fields]}}""",
                    """{"$DECK_ID":{"name":"D"}}""",
                )
                insertNotesAndCards(connection, notes)
            }
        TestZip.write(path, listOf("collection.anki2" to database))
    }

    fun modern(
        path: Path,
        fieldCount: Int,
        notes: List<String>,
    ) {
        val database =
            buildDatabase(path) { connection ->
                connection.execSQL("CREATE TABLE notetypes (id INTEGER, name TEXT, config BLOB)")
                connection.execSQL("CREATE TABLE fields (ntid INTEGER, ord INTEGER, name TEXT)")
                connection.execSQL("CREATE TABLE decks (id INTEGER, name TEXT)")
                connection.execSQL("INSERT INTO notetypes VALUES ($NOTE_TYPE_ID, 'T', x'')")
                connection.execSQL("INSERT INTO decks VALUES ($DECK_ID, 'D')")
                repeat(fieldCount) { index -> connection.execSQL("INSERT INTO fields VALUES ($NOTE_TYPE_ID, $index, 'f$index')") }
                insertNotesAndCards(connection, notes)
            }
        val compressed = Buffer().also { buffer -> buffer.zstdCompress().buffer().use { it.write(database) } }.readByteArray()
        TestZip.write(path, listOf("meta" to byteArrayOf(META_VERSION_KEY, META_VERSION_LATEST), "collection.anki21b" to compressed))
    }

    private const val META_VERSION_KEY: Byte = 0x08
    private const val META_VERSION_LATEST: Byte = 0x03

    private fun buildDatabase(
        target: Path,
        build: (SQLiteConnection) -> Unit,
    ): ByteArray {
        val databasePath = target.parent!! / "${target.name}.sqlite"
        Fixtures.fileSystem.delete(databasePath, mustExist = false)
        BundledSQLiteDriver().open(databasePath.toString()).use(build)
        return Fixtures.fileSystem.read(databasePath) { readByteArray() }.also { Fixtures.fileSystem.delete(databasePath) }
    }

    private fun insertNotesAndCards(
        connection: SQLiteConnection,
        notes: List<String>,
    ) {
        connection.execSQL("CREATE TABLE notes (id INTEGER, mid INTEGER, flds TEXT)")
        connection.execSQL("CREATE TABLE cards (nid INTEGER, did INTEGER, odid INTEGER, ord INTEGER)")
        notes.forEachIndexed { index, flds ->
            insert(connection, "INSERT INTO notes VALUES (${index + 1}, $NOTE_TYPE_ID, ?)", flds)
            connection.execSQL("INSERT INTO cards VALUES (${index + 1}, $DECK_ID, 0, 0)")
        }
    }

    private fun insert(
        connection: SQLiteConnection,
        sql: String,
        vararg values: String,
    ) {
        connection.prepare(sql).use { statement ->
            values.forEachIndexed { index, value -> statement.bindText(index + 1, value) }
            statement.step()
        }
    }
}

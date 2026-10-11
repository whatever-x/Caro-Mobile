package com.whatever.caro.core.importparser

import com.whatever.caro.core.importparser.model.ParsedTable
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.JsonObject

internal fun ParsedTable.valuesAt(index: Int): List<String> {
    val row = rows[index]
    val fieldNames = noteTypes.first { type -> type.id == row.noteTypeId }.fieldNames
    return fieldNames.map { name -> row.fields.getValue(name) }
}

internal fun ParsedTable.shouldMatchHeader(expected: JsonObject) {
    expected.string("format")?.let { value -> format.name shouldBe value }
    expected.string("htmlMode")?.let { value -> htmlMode.name shouldBe value }
    expected.string("sourceName")?.let { value -> sourceName shouldBe value }
}

internal fun ParsedTable.shouldMatchNoteTypes(expectedTypes: List<JsonObject>) {
    val actual = noteTypes.map { type -> listOf(type.name, type.fieldNames, type.isCloze, type.noteCount) }
    val wanted =
        expectedTypes.map { type ->
            listOf(
                type.string("name"),
                type.strings("fieldNames"),
                type.boolean("isCloze"),
                type.int("noteCount"),
            )
        }
    withClue("noteTypes") { actual shouldContainExactlyInAnyOrder wanted }
    expectedTypes.forEach { type ->
        val id = type.string("id") ?: return@forEach
        withClue("noteType id of ${type.string("name")}") {
            noteTypes.first { it.name == type.string("name") }.id shouldBe id
        }
    }
    expectedTypes.forEach { type ->
        withClue("isImageOcclusion of ${type.string("name")}") {
            noteTypes.first { it.name == type.string("name") }.isImageOcclusion shouldBe (type.boolean("isImageOcclusion") ?: false)
        }
    }
}

internal fun ParsedTable.shouldMatchWarnings(expectedWarnings: List<JsonObject>) {
    val actual = warnings.map { warning -> warning.code to warning.count }
    val wanted = expectedWarnings.map { warning -> warning.string("code") to warning.int("count") }
    withClue("warnings") { actual shouldContainExactlyInAnyOrder wanted }
}

internal fun ParsedTable.shouldMatchListedRows(
    expectedRows: List<JsonObject>,
    indexKey: String,
) {
    expectedRows.forEach { row ->
        val index = row.int(indexKey) ?: error("row without $indexKey")
        withClue("row $index") { valuesAt(index) shouldBe row.strings("fieldValues") }
    }
}

internal fun ParsedTable.shouldMatchSubdecks(expectedSubdecks: List<JsonObject>) {
    val actual = subdecks.map { subdeck -> subdeck.path to subdeck.noteCount }
    val wanted = expectedSubdecks.map { subdeck -> subdeck.string("path") to subdeck.int("noteCount") }
    withClue("subdecks in path order") { actual shouldBe wanted }
    expectedSubdecks.forEach { subdeck ->
        val id = subdeck.string("id") ?: return@forEach
        withClue("subdeck id of ${subdeck.string("path")}") {
            subdecks.first { it.path == subdeck.string("path") }.id shouldBe id
        }
    }
}

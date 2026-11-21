package com.whatever.caro.feature.importer.logic

import com.whatever.caro.core.importparser.model.ParsedLayout
import com.whatever.caro.core.importparser.model.ParsedTable
import com.whatever.caro.core.model.importing.FieldMapping
import com.whatever.caro.feature.importer.mvi.ImportSelection
import com.whatever.caro.feature.importer.support.PreviewSamples
import com.whatever.caro.feature.importer.transform.ImportPreview
import com.whatever.caro.feature.importer.transform.defaultFieldMappings
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableSet

class PreviewSettingsTest : FunSpec() {
    init {
        context("Anki 설정 박스") {
            test("하위 덱, 카드 구성, 제외 줄을 태그로 만든다") {
                rowsOf(PreviewSamples.ankiTable, PreviewSamples.ankiPreview) shouldBe
                    listOf(
                        row(SettingRowKind.Subdeck, SettingTag.SubdeckAll(2), SettingTag.SubdeckMerged),
                        row(SettingRowKind.CardLayout, SettingTag.NoteTypeCount(2), SettingTag.SidesAuto),
                        row(
                            SettingRowKind.Excluded,
                            SettingTag.EmptyCards(2),
                            SettingTag.ImageOcclusionCards(PreviewSamples.IMAGE_OCCLUSION_CARDS),
                        ),
                    )
            }

            test("하위 덱이 하나면 합칠 덱이 없어 한 덱으로 합침 태그를 숨긴다") {
                val table = PreviewSamples.ankiTable.copy(subdecks = PreviewSamples.ankiTable.subdecks.take(1))

                rowsOf(table, PreviewSamples.ankiPreview).first() shouldBe row(SettingRowKind.Subdeck, SettingTag.SubdeckAll(1))
            }

            test("빈 카드와 Image Occlusion이 없으면 제외 줄을 숨긴다") {
                val table = PreviewSamples.ankiTable.copy(warnings = emptyList())
                val preview = PreviewSamples.preview(PreviewSamples.ankiCards)

                rowsOf(table, preview).map { row -> row.kind } shouldBe listOf(SettingRowKind.Subdeck, SettingRowKind.CardLayout)
            }
        }

        context("엑셀 설정 박스") {
            test("시트 이름과 제목 행, 앞뒷면 항목, 빈 카드와 다른 시트 수를 보인다") {
                rowsOf(PreviewSamples.xlsxTable, PreviewSamples.xlsxPreview) shouldBe
                    listOf(
                        row(SettingRowKind.Sheet, SettingTag.SheetName("Day1"), SettingTag.HeaderRow(isOn = true)),
                        row(
                            SettingRowKind.CardLayout,
                            SettingTag.FrontFields(named("단어"), moreCount = 0),
                            SettingTag.BackFields(named("뜻", "예문"), moreCount = 0),
                        ),
                        row(SettingRowKind.Excluded, SettingTag.EmptyCards(3), SettingTag.OtherSheets(2)),
                    )
            }

            test("뒷면 항목이 셋을 넘으면 셋만 보이고 나머지는 수로 센다") {
                val fields = listOf("단어", "뜻", "예문", "발음", "품사", "메모")
                val table = PreviewSamples.xlsxTable.let { it.copy(noteTypes = listOf(it.noteTypes.single().copy(fieldNames = fields))) }

                rowsOf(table, PreviewSamples.xlsxPreview)[1].tags[1] shouldBe
                    SettingTag.BackFields(named("뜻", "예문", "발음"), moreCount = 2)
            }
        }

        context("텍스트와 붙여넣기 설정 박스") {
            test("구분 기준과 첫 줄 판정, 이름 없는 열은 번호로 보이고 제외할 카드가 없으면 제외 줄이 없다") {
                rowsOf(PreviewSamples.textTable, PreviewSamples.textPreview) shouldBe
                    listOf(
                        row(SettingRowKind.Delimiter, SettingTag.Delimiter(DelimiterKind.COMMA, ","), SettingTag.HeaderRow(isOn = false)),
                        row(
                            SettingRowKind.CardLayout,
                            SettingTag.FrontFields(numbered(1), moreCount = 0),
                            SettingTag.BackFields(numbered(2), moreCount = 0),
                        ),
                    )
            }

            test("뒷면에 넣은 항목이 없으면 뒷면 없음이다") {
                rowsOf(PreviewSamples.singleColumnTable, PreviewSamples.singleColumnPreview)[1] shouldBe
                    row(SettingRowKind.CardLayout, SettingTag.FrontFields(numbered(1), moreCount = 0), SettingTag.BackNone)
            }

            test("이름이 없는 구분 문자는 그 문자를 그대로 보인다") {
                val table = PreviewSamples.textTable.copy(layout = ParsedLayout(delimiter = "#", hasHeaderRow = true))

                rowsOf(table, PreviewSamples.textPreview).first() shouldBe
                    row(SettingRowKind.Delimiter, SettingTag.Delimiter(kind = null, text = "#"), SettingTag.HeaderRow(isOn = true))
            }

            test("사용자가 쓴 열 이름은 이름 그대로 보인다") {
                val names = listOf("Word", "Meaning")
                val table = PreviewSamples.textTable.let { it.copy(noteTypes = listOf(it.noteTypes.single().copy(fieldNames = names))) }
                val mapping = FieldMapping("text", mapOf(FieldMapping.FRONT to listOf("Word"), FieldMapping.BACK to listOf("Meaning")))

                previewSettingRows(table, ImportSelection(), mapOf("text" to mapping), PreviewSamples.textPreview)[1].tags shouldBe
                    listOf(SettingTag.FrontFields(named("Word"), moreCount = 0), SettingTag.BackFields(named("Meaning"), moreCount = 0))
            }
        }
    }

    private fun rowsOf(
        table: ParsedTable,
        preview: ImportPreview,
    ): List<SettingRow> {
        val selection = ImportSelection(subdeckIds = table.subdecks.map { subdeck -> subdeck.id }.toImmutableSet())
        return previewSettingRows(table, selection, defaultFieldMappings(table), preview)
    }

    private fun named(vararg names: String): ImmutableList<TextCheckHeader> =
        names.map { name -> TextCheckHeader.Named(name) }.toImmutableList()

    private fun numbered(number: Int): ImmutableList<TextCheckHeader> = persistentListOf(TextCheckHeader.Numbered(number))

    private fun row(
        kind: SettingRowKind,
        vararg tags: SettingTag,
    ): SettingRow = SettingRow(kind, tags.toList().toImmutableList())
}

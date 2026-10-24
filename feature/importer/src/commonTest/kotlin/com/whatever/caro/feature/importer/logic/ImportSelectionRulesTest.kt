package com.whatever.caro.feature.importer.logic

import androidx.compose.ui.state.ToggleableState
import com.whatever.caro.core.importparser.model.HtmlMode
import com.whatever.caro.core.importparser.model.ImportFormat
import com.whatever.caro.core.importparser.model.ParsedNoteType
import com.whatever.caro.core.importparser.model.ParsedSheet
import com.whatever.caro.core.importparser.model.ParsedSubdeck
import com.whatever.caro.core.importparser.model.ParsedTable
import com.whatever.caro.core.model.importing.ImportLimits
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ImportSelectionRulesTest : FunSpec() {
    init {
        context("공통 상위 경로") {
            test("부모 경로가 같은 두 덱의 공통 상위는 그 부모다") {
                commonParentPath(listOf("TOEIC::Day 01", "TOEIC::Day 02")) shouldBe "TOEIC"
            }

            test("단일 최상위 덱은 공통 상위가 없다") {
                commonParentPath(listOf("JLPT N5")) shouldBe ""
            }

            test("상위 덱과 그 하위 덱을 함께 고르면 공통 상위가 없다") {
                commonParentPath(listOf("JLPT N5", "JLPT N5::단어")) shouldBe ""
            }

            test("경로 조각 단위로 비교하므로 글자만 겹치는 최상위 덱은 공통 상위가 아니다") {
                commonParentPath(listOf("TOEIC::Day 01", "TOEFL::Day 01")) shouldBe ""
            }

            test("깊이가 2 이상인 공통 부모는 전체 경로를 돌려준다") {
                commonParentPath(listOf("Lang::JP::단어", "Lang::JP::문법")) shouldBe "Lang::JP"
            }

            test("경로 깊이가 서로 달라도 부모 경로의 공통 앞부분을 돌려준다") {
                commonParentPath(listOf("Lang::JP::단어::N5", "Lang::JP::문법")) shouldBe "Lang::JP"
            }

            test("고른 덱이 없으면 공통 상위가 없다") {
                commonParentPath(emptyList()) shouldBe ""
            }
        }

        context("하위 덱 표시 이름") {
            test("공통 상위를 뺀 나머지 경로를 보여준다") {
                val paths = listOf("TOEIC::Day 01", "TOEIC::Day 02")

                paths.map { path -> subdeckDisplayName(path, commonParent = "TOEIC") } shouldBe listOf("Day 01", "Day 02")
            }

            test("단일 덱은 공통 상위가 없어 이름이 비지 않는다") {
                subdeckDisplayName("JLPT N5", commonParent = "") shouldBe "JLPT N5"
            }

            test("상위 덱과 하위 덱 쌍은 둘 다 비어 있지 않은 이름을 준다") {
                val paths = listOf("JLPT N5", "JLPT N5::단어")

                paths.map { path -> subdeckDisplayName(path, commonParent = commonParentPath(paths)) } shouldBe
                    listOf("JLPT N5", "JLPT N5::단어")
            }

            test("이름이 비게 되면 마지막 경로 조각으로 대신한다") {
                subdeckDisplayName("Lang::JP", commonParent = "Lang::JP") shouldBe "JP"
            }

            test("공통 상위 아래에 단계가 더 있으면 남은 경로를 모두 보여준다") {
                subdeckDisplayName("Lang::JP::단어::N5", commonParent = "Lang::JP") shouldBe "단어::N5"
            }
        }

        context("덱 없음 수") {
            test("노트 유형별 노트 합계에서 하위 덱 노트 합계를 뺀 값이다") {
                val table =
                    apkgTable(
                        subdecks = listOf(subdeck("1", "A", 600), subdeck("2", "B", 370)),
                        noteTypes = listOf(noteType("n1", noteCount = 700), noteType("n2", noteCount = 300)),
                    )

                noDeckCount(table) shouldBe 30
            }

            test("모든 노트가 하위 덱에 있으면 0이다") {
                val table =
                    apkgTable(
                        subdecks = listOf(subdeck("1", "A", 600), subdeck("2", "B", 400)),
                        noteTypes = listOf(noteType("n1", noteCount = 1_000)),
                    )

                noDeckCount(table) shouldBe 0
            }
        }

        context("하위 덱 선택 합계와 상한") {
            // a 5,000 + b 5,001 + c 5,000 + 덱 없음 7
            val table =
                apkgTable(
                    subdecks = listOf(subdeck("a", "A", 5_000), subdeck("b", "B", 5_001), subdeck("c", "C", 5_000)),
                    noteTypes = listOf(noteType("n1", noteCount = 15_008)),
                )

            test("선택한 하위 덱의 노트 수를 더한다") {
                summarizeSubdeckSelection(table, setOf("a", "b"), maxCards = 10_000).selectedTotal shouldBe 10_001
            }

            test("덱 없음을 고르면 그 노트 수도 합계에 더한다") {
                summarizeSubdeckSelection(table, setOf("a", ParsedSubdeck.NO_DECK_ID), maxCards = 10_000).selectedTotal shouldBe 5_007
            }

            test("아무것도 고르지 않으면 합계가 0이고 상한을 넘지 않는다") {
                val summary = summarizeSubdeckSelection(table, emptySet(), maxCards = 10_000)

                summary.selectedTotal shouldBe 0
                summary.isOverLimit shouldBe false
            }

            test("상한 10,000에서 정확히 10,000장은 넘지 않는다") {
                summarizeSubdeckSelection(table, setOf("a", "c"), maxCards = 10_000).isOverLimit shouldBe false
            }

            test("상한 10,000에서 10,001장은 넘는다") {
                summarizeSubdeckSelection(table, setOf("a", "b"), maxCards = 10_000).isOverLimit shouldBe true
            }

            test("상한 5,000에서 정확히 5,000장은 넘지 않는다") {
                summarizeSubdeckSelection(table, setOf("a"), maxCards = 5_000).isOverLimit shouldBe false
            }

            test("상한 5,000에서 5,001장은 넘는다") {
                summarizeSubdeckSelection(table, setOf("b"), maxCards = 5_000).isOverLimit shouldBe true
            }

            test("덱 없음을 더해 상한 5,000을 넘으면 넘는다") {
                summarizeSubdeckSelection(table, setOf("a", ParsedSubdeck.NO_DECK_ID), maxCards = 5_000).isOverLimit shouldBe true
            }
        }

        context("전체 선택 상태") {
            val table =
                apkgTable(
                    subdecks = listOf(subdeck("a", "A", 10), subdeck("b", "B", 10)),
                    noteTypes = listOf(noteType("n1", noteCount = 25)),
                )

            test("하위 덱을 모두 고르면 선택됨이다") {
                selectAllState(table, setOf("a", "b")) shouldBe ToggleableState.On
            }

            test("하위 덱을 일부만 고르면 일부 선택이다") {
                selectAllState(table, setOf("a")) shouldBe ToggleableState.Indeterminate
            }

            test("하위 덱을 하나도 고르지 않으면 선택 안 됨이다") {
                selectAllState(table, emptySet()) shouldBe ToggleableState.Off
            }

            test("덱 없음만 골라도 선택 안 됨이다") {
                selectAllState(table, setOf(ParsedSubdeck.NO_DECK_ID)) shouldBe ToggleableState.Off
            }

            test("하위 덱을 모두 고르고 덱 없음도 골라도 선택됨이다") {
                selectAllState(table, setOf("a", "b", ParsedSubdeck.NO_DECK_ID)) shouldBe ToggleableState.On
            }

            test("하위 덱 일부와 덱 없음을 고르면 일부 선택이다") {
                selectAllState(table, setOf("a", ParsedSubdeck.NO_DECK_ID)) shouldBe ToggleableState.Indeterminate
            }
        }

        context("시트 줄 라벨") {
            test("0행이면 비어 있음이다") {
                sheetRowLabel(ParsedSheet(0, "메모", rowCount = 0), maxCards = 10_000) shouldBe SheetRowLabel.Empty
            }

            test("행이 있으면 행 수를 보여준다") {
                sheetRowLabel(ParsedSheet(0, "Sheet1", rowCount = 320), maxCards = 10_000) shouldBe SheetRowLabel.Rows(320)
            }

            test("상한과 같은 행 수는 상한 초과가 아니다") {
                sheetRowLabel(ParsedSheet(0, "원본", rowCount = 10_000), maxCards = 10_000) shouldBe SheetRowLabel.Rows(10_000)
            }

            test("상한보다 1 많은 행 수는 상한 초과다") {
                sheetRowLabel(ParsedSheet(0, "원본", rowCount = 10_001), maxCards = 10_000) shouldBe SheetRowLabel.OverLimit
            }

            test("상한 5,000에서 5,000행은 행 수를 보여준다") {
                sheetRowLabel(ParsedSheet(0, "원본", rowCount = 5_000), maxCards = 5_000) shouldBe SheetRowLabel.Rows(5_000)
            }

            test("상한 5,000에서 5,001행은 상한 초과다") {
                sheetRowLabel(ParsedSheet(0, "원본", rowCount = 5_001), maxCards = 5_000) shouldBe SheetRowLabel.OverLimit
            }
        }

        context("붙여넣기 입력 검사") {
            test("비어 있으면 다음을 막는다") {
                pasteBlockReason("", maxPasteChars = 10, isOverflowed = false) shouldBe PasteBlockReason.Empty
            }

            test("상한 글자 수까지는 통과한다") {
                pasteBlockReason("a".repeat(10), maxPasteChars = 10, isOverflowed = false) shouldBe null
            }

            test("상한보다 한 글자 많으면 막는다") {
                pasteBlockReason("a".repeat(11), maxPasteChars = 10, isOverflowed = false) shouldBe PasteBlockReason.TooLong
            }

            test("기본 상한 1,000,000자는 통과한다") {
                pasteBlockReason("a".repeat(ImportLimits.MAX_PASTE_CHARS), ImportLimits.MAX_PASTE_CHARS, isOverflowed = false) shouldBe null
            }

            test("기본 상한보다 한 글자 많은 1,000,001자는 막는다") {
                pasteBlockReason("a".repeat(ImportLimits.MAX_PASTE_CHARS + 1), ImportLimits.MAX_PASTE_CHARS, isOverflowed = false) shouldBe
                    PasteBlockReason.TooLong
            }

            test("넘친 적이 있으면 상한 이하로 돌아와도 막는다") {
                pasteBlockReason("a".repeat(10), maxPasteChars = 10, isOverflowed = true) shouldBe PasteBlockReason.TooLong
            }

            test("비어 있으면 넘친 적이 있어도 비어 있음이 먼저다") {
                pasteBlockReason("", maxPasteChars = 10, isOverflowed = true) shouldBe PasteBlockReason.Empty
            }
        }

        context("넘친 붙여넣기 유지 판정") {
            val overflowed = "0123456789A"

            test("상한을 넘는 입력은 넘친 것이다") {
                nextPasteOverflowed(previous = "", wasOverflowed = false, edited = overflowed, maxPasteChars = 10) shouldBe true
            }

            test("넘친 적 없고 상한 이하면 넘치지 않은 것이다") {
                nextPasteOverflowed(previous = "abc", wasOverflowed = false, edited = "abcd", maxPasteChars = 10) shouldBe false
            }

            test("넘친 뒤 끝 글자를 하나 지우면 계속 넘친 것이다") {
                nextPasteOverflowed(overflowed, wasOverflowed = true, edited = "0123456789", maxPasteChars = 10) shouldBe true
            }

            test("넘친 뒤 첫 글자를 하나 지우면 계속 넘친 것이다") {
                nextPasteOverflowed(overflowed, wasOverflowed = true, edited = "123456789A", maxPasteChars = 10) shouldBe true
            }

            test("넘친 뒤 가운데 글자를 하나 지우면 계속 넘친 것이다") {
                nextPasteOverflowed(overflowed, wasOverflowed = true, edited = "012346789A", maxPasteChars = 10) shouldBe true
            }

            test("넘친 뒤 연속한 구간을 지워도 계속 넘친 것이다") {
                nextPasteOverflowed(overflowed, wasOverflowed = true, edited = "01A", maxPasteChars = 10) shouldBe true
            }

            test("같은 글자가 이어진 곳에서 하나를 지워도 계속 넘친 것이다") {
                nextPasteOverflowed("aaaaaaaaaaa", wasOverflowed = true, edited = "aaaaaaaaaa", maxPasteChars = 10) shouldBe true
            }

            test("넘친 뒤 비우면 풀린다") {
                nextPasteOverflowed(overflowed, wasOverflowed = true, edited = "", maxPasteChars = 10) shouldBe false
            }

            test("넘친 뒤 다른 글로 바꾸면 풀린다") {
                nextPasteOverflowed(overflowed, wasOverflowed = true, edited = "새 글", maxPasteChars = 10) shouldBe false
            }

            test("넘친 뒤 지우고 새 글자를 넣은 입력은 지우기만 한 것이 아니므로 풀린다") {
                nextPasteOverflowed(overflowed, wasOverflowed = true, edited = "01Z", maxPasteChars = 10) shouldBe false
            }

            test("넘친 뒤 다시 상한을 넘기면 계속 넘친 것이다") {
                nextPasteOverflowed(overflowed, wasOverflowed = true, edited = "새 글을 아주 길게 붙여 넣은 입력", maxPasteChars = 10) shouldBe true
            }
        }
    }

    private fun subdeck(
        id: String,
        path: String,
        noteCount: Int,
    ) = ParsedSubdeck(id = id, path = path, noteCount = noteCount)

    private fun noteType(
        id: String,
        noteCount: Int,
        isImageOcclusion: Boolean = false,
        selectedNoteCount: Int? = null,
    ) = ParsedNoteType(
        id = id,
        name = id,
        fieldNames = listOf("앞면", "뒷면"),
        isCloze = false,
        noteCount = noteCount,
        isImageOcclusion = isImageOcclusion,
        selectedNoteCount = selectedNoteCount,
    )

    private fun apkgTable(
        subdecks: List<ParsedSubdeck> = emptyList(),
        noteTypes: List<ParsedNoteType>,
    ) = ParsedTable(
        format = ImportFormat.APKG,
        sourceName = "deck.apkg",
        htmlMode = HtmlMode.HTML,
        subdecks = subdecks,
        noteTypes = noteTypes,
        rows = emptyList(),
    )
}

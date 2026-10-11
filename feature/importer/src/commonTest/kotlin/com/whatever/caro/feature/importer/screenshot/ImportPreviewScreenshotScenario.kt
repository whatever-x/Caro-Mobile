package com.whatever.caro.feature.importer.screenshot

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import com.whatever.caro.core.importparser.model.HtmlMode
import com.whatever.caro.core.importparser.model.ImportFormat
import com.whatever.caro.core.importparser.model.ParsedNoteType
import com.whatever.caro.core.importparser.model.ParsedTable
import com.whatever.caro.feature.importer.mvi.ImportDeckState
import com.whatever.caro.feature.importer.mvi.ImportPreviewFilter
import com.whatever.caro.feature.importer.mvi.ImportStep
import com.whatever.caro.feature.importer.transform.ImportCard
import com.whatever.caro.feature.importer.transform.ImportPreview
import com.whatever.caro.feature.importer.transform.ImportPreviewStats
import kotlinx.collections.immutable.persistentListOf

internal object ImportPreviewScreenshotScenario {
    const val FLAGS_ID = "importer.preview.flags"
    const val FILTERED_ID = "importer.preview.filtered"

    private const val EMPTY_SIDE = "빈 항목입니다"
    private const val LAYOUT_FRAMES = 2

    private val CARDS =
        persistentListOf(
            card(index = 0, front = "学校", back = "학교\n学校に行きます。\nがっこう"),
            card(index = 1, front = "読む", back = EMPTY_SIDE, backReplaced = true),
            card(
                index = 2,
                front = "日本",
                back = "일본\n日本は東アジアにある島国で、北海道、本州、四国、九州の四つの大きな島と六千を超える小さな島からなります。",
                backTruncated = true,
            ),
            card(
                index = 3,
                front = EMPTY_SIDE,
                back = "富士山\n富士山は日本で一番高い山で、高さは三千七百七十六メートルです。静岡県と山梨県にまたがり、昔から多くの絵や詩に描かれてきました。",
                frontReplaced = true,
                backTruncated = true,
                mediaRemoved = 2,
            ),
            card(index = 4, front = "食べる", back = "먹다\n朝ご飯を食べます。\nたべる", mediaRemoved = 1),
            card(index = 5, front = "友達", back = EMPTY_SIDE, backReplaced = true),
            card(index = 6, front = "先生", back = "선생님\n先生に聞きます。\nせんせい"),
            card(index = 7, front = "学生", back = "학생\n学生です。\nがくせい"),
        )

    // 요약 수치는 Figma 프레임 값이고 목록은 앞쪽 카드 여덟 장만 담는다.
    private val PREVIEW =
        ImportPreview(
            cards = CARDS,
            stats =
                ImportPreviewStats(
                    cardCount = 1_512,
                    excludedCount = 2,
                    emptyReplacedCount = 5,
                    truncatedCount = 2,
                    mediaRemovedCount = 386,
                ),
            warnings = emptyList(),
            limitExceeded = false,
        )

    private val TABLE =
        ParsedTable(
            format = ImportFormat.APKG,
            sourceName = "일본어.apkg",
            htmlMode = HtmlMode.HTML,
            noteTypes =
                listOf(
                    ParsedNoteType(
                        id = "vocab",
                        name = "일본어 단어",
                        fieldNames = listOf("단어", "뜻"),
                        isCloze = false,
                        noteCount = 15,
                    ),
                ),
            rows = emptyList(),
        )

    @Composable
    fun RenderFlags() {
        ImportScreenshotScenario.Render(ImportDeckState(step = ImportStep.Preview, table = TABLE, preview = PREVIEW))
    }

    // 목록 머리로 올리는 스크롤은 필터를 바꿨을 때만 일어나고, 머리를 맨 위에 붙이는 채움 높이는 첫 배치 뒤에야 정해진다.
    // 그래서 두 프레임을 지난 뒤 필터를 한 번 바꾼다. 먼저 바꾸면 채움이 없는 채로 스크롤이 처음 위치로 되돌려진다.
    @Composable
    fun RenderFiltered() {
        var filter by remember { mutableStateOf(ImportPreviewFilter.All) }
        LaunchedEffect(Unit) {
            repeat(LAYOUT_FRAMES) { withFrameNanos { } }
            filter = ImportPreviewFilter.Truncated
        }
        ImportScreenshotScenario.Render(
            ImportDeckState(step = ImportStep.Preview, table = TABLE, preview = PREVIEW, previewFilter = filter),
        )
    }

    private fun card(
        index: Int,
        front: String,
        back: String,
        frontReplaced: Boolean = false,
        backReplaced: Boolean = false,
        backTruncated: Boolean = false,
        mediaRemoved: Int = 0,
    ) = ImportCard(
        sourceIndex = index,
        front = front,
        back = back,
        frontReplaced = frontReplaced,
        backReplaced = backReplaced,
        frontTruncated = false,
        backTruncated = backTruncated,
        mediaRemoved = mediaRemoved,
    )
}

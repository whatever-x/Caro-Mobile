package com.whatever.caro.feature.importer.step

import com.whatever.caro.core.importparser.model.ParseWarning
import com.whatever.caro.feature.importer.transform.ImportCard
import com.whatever.caro.feature.importer.transform.ImportPreview
import com.whatever.caro.feature.importer.transform.ImportPreviewStats
import kotlinx.collections.immutable.toImmutableList

/**
 * 미리보기 화면 테스트가 쓰는 카드와 미리보기다. 카드 값은 Figma 프레임의 표시 값이다.
 * 요약 수치가 서로 다른 글자(8장, 7장, 3장, 2장, 3개)로 나오게 맞춰 어느 줄의 수치인지 글자만으로 찾을 수 있다.
 */
internal object PreviewStepTables {
    const val EMPTY_SIDE = "빈 항목입니다"
    const val EXCLUDED = 7

    private const val JAPAN_BACK = "일본\n日本は東アジアにある島国で、北海道、本州、四国、九州の四つの大きな島と六千を超える小さな島からなります。"
    private const val FUJI_BACK = "富士山\n富士山は日本で一番高い山です。"

    /** 화면 줄 수를 넘기는 긴 글이다. 앞면 두 줄, 뒷면 세 줄 제한을 확인하는 데 쓴다. */
    val LONG_FRONT = "긴앞면".repeat(200)
    val LONG_BACK = List(12) { line -> "뒷면 ${line + 1}번째 줄" }.joinToString(separator = "\n")

    val SCHOOL = card(index = 0, front = "学校", back = "학교\n学校に行きます。\nがっこう")
    val READ = card(index = 1, front = "読む", back = EMPTY_SIDE, backReplaced = true)
    val JAPAN = card(index = 2, front = "日本", back = JAPAN_BACK, backTruncated = true)
    val MOUNT_FUJI =
        card(index = 3, front = EMPTY_SIDE, back = FUJI_BACK, frontReplaced = true, backTruncated = true, mediaRemoved = 2)
    val EAT = card(index = 4, front = "食べる", back = "먹다\n朝ご飯を食べます。\nたべる", mediaRemoved = 1)
    val FRIEND = card(index = 5, front = "友達", back = EMPTY_SIDE, backReplaced = true)
    val TEACHER = card(index = 6, front = "先生", back = "선생님\n先生に聞きます。\nせんせい")
    val STUDENT = card(index = 7, front = "学生", back = "학생\n学生です。\nがくせい")

    /** 빈 면 채움은 읽다, 후지산, 친구 셋이고 잘린 내용은 일본, 후지산 둘이며 지운 미디어는 후지산 둘과 먹다 하나다. */
    val FIGMA_CARDS = listOf(SCHOOL, READ, JAPAN, MOUNT_FUJI, EAT, FRIEND, TEACHER, STUDENT)

    fun card(
        index: Int,
        front: String = "앞$index",
        back: String = "뒤$index",
        frontReplaced: Boolean = false,
        backReplaced: Boolean = false,
        frontTruncated: Boolean = false,
        backTruncated: Boolean = false,
        mediaRemoved: Int = 0,
    ) = ImportCard(
        sourceIndex = index,
        front = front,
        back = back,
        frontReplaced = frontReplaced,
        backReplaced = backReplaced,
        frontTruncated = frontTruncated,
        backTruncated = backTruncated,
        mediaRemoved = mediaRemoved,
    )

    /** 바뀐 것이 없는 카드 [count]장이다. 스크롤이 필요한 목록과 큰 장수를 만든다. */
    fun plainCards(
        count: Int,
        firstIndex: Int = 0,
    ): List<ImportCard> =
        List(count) { offset ->
            val index = firstIndex + offset
            card(index = index, front = "단어 $index", back = "뜻 $index")
        }

    fun preview(
        cards: List<ImportCard> = FIGMA_CARDS,
        excluded: Int = EXCLUDED,
        warnings: List<ParseWarning> = emptyList(),
        limitExceeded: Boolean = false,
    ) = ImportPreview(
        cards = cards.toImmutableList(),
        stats =
            ImportPreviewStats(
                cardCount = cards.size,
                excludedCount = excluded,
                emptyReplacedCount = cards.count { card -> card.frontReplaced || card.backReplaced },
                truncatedCount = cards.count { card -> card.frontTruncated || card.backTruncated },
                mediaRemovedCount = cards.sumOf { card -> card.mediaRemoved },
            ),
        warnings = warnings,
        limitExceeded = limitExceeded,
    )

    /** 목록이 화면보다 길어 스크롤된다. 앞 여덟 장은 Figma 카드이고 뒤는 바뀐 것이 없는 카드다. */
    fun longPreview(extra: Int = 40) = preview(cards = FIGMA_CARDS + plainCards(count = extra, firstIndex = FIGMA_CARDS.size))
}
